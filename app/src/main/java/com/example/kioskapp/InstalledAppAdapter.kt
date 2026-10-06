package com.example.kioskapp

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class InstalledAppRow(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable?,
    var checked: Boolean
)

class InstalledAppAdapter(
    private val items: MutableList<InstalledAppRow>
) : RecyclerView.Adapter<InstalledAppAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.rowIcon)
        val label: TextView = view.findViewById(R.id.rowLabel)
        val checkBox: CheckBox = view.findViewById(R.id.rowCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_installed_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.icon.setImageDrawable(item.icon)
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.checked
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.checked = isChecked
        }
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount() = items.size

    fun getSelectedPackages(): Set<String> = items.filter { it.checked }.map { it.packageName }.toSet()
}

/**
 * חבילות שאסור שיהיו אף פעם ברשימת האפליקציות המורשות בקיוסק, גם אם המנהל
 * מנסה לסמן אותן בטעות. חסימת com.android.settings היא הקריטית ביותר: אם
 * ה-Settings תיכנס לרשימת lockTaskPackages, המשתמש יוכל לצאת ממצב הקיוסק
 * (לשנות הרשאות, Wi-Fi, אפליקציית בית וכו') בלי לדעת PIN בכלל.
 */
val PERMANENTLY_BLOCKED_PACKAGES = setOf(
    "com.android.settings",
    "com.android.settings.intelligence",
    "com.android.launcher",
    "com.android.launcher3"
)

/** שולף את כל האפליקציות שניתן להפעיל (יש להן launcher activity), חוץ מהקיוסק עצמו וחבילות חסומות. */
fun listLaunchableApps(pm: PackageManager, ownPackage: String, preselected: Set<String>): List<InstalledAppRow> {
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    return resolved
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != ownPackage }
        .filter { it.packageName !in PERMANENTLY_BLOCKED_PACKAGES }
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || preselected.contains(it.packageName) }
        .map { info ->
            InstalledAppRow(
                packageName = info.packageName,
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info),
                checked = preselected.contains(info.packageName)
            )
        }
        .sortedBy { it.label.lowercase() }
}

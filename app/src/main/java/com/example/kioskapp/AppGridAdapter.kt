package com.example.kioskapp

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BankAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isInstalled: Boolean
)

class AppGridAdapter(
    private val items: List<BankAppEntry>,
    private val onClick: (BankAppEntry) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.icon.setImageDrawable(item.icon)
        holder.icon.alpha = if (item.isInstalled) 1.0f else 0.35f
        holder.itemView.setOnClickListener {
            if (item.isInstalled) onClick(item)
        }
    }

    override fun getItemCount() = items.size
}

fun resolveBankApps(pm: PackageManager, packages: Set<String>): List<BankAppEntry> {
    return packages.sorted().map { pkg ->
        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            BankAppEntry(
                packageName = pkg,
                label = pm.getApplicationLabel(appInfo).toString(),
                icon = pm.getApplicationIcon(appInfo),
                isInstalled = true
            )
        } catch (e: PackageManager.NameNotFoundException) {
            BankAppEntry(packageName = pkg, label = pkg, icon = null, isInstalled = false)
        }
    }
}

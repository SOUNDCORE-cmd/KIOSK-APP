package com.example.kioskapp

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.Toast

object PinPrompt {

    /**
     * מציג דיאלוג הזנת PIN. אם הקוד נכון, מריץ onSuccess.
     * לא ניתן לסגור את הדיאלוג בלחיצה מחוץ לו - רק ע"י ביטול מפורש.
     */
    fun show(context: Context, title: String = "הזן קוד PIN", onSuccess: () -> Unit) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
        }

        val padding = (16 * context.resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(context).apply {
            setPadding(padding, padding, padding, padding)
            addView(input)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setCancelable(true)
            .setPositiveButton("אישור", null) // נבטל את הסגירה האוטומטית למטה
            .setNegativeButton("ביטול", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val entered = input.text.toString()
                if (PinManager.verifyPin(entered)) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    input.error = "קוד שגוי"
                    Toast.makeText(context, "קוד PIN שגוי", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}

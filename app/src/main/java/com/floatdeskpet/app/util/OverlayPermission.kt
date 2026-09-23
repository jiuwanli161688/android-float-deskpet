package com.floatdeskpet.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import com.floatdeskpet.app.R

object OverlayPermission {
    fun granted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    }

    fun toastNeed(context: Context) {
        Toast.makeText(context, context.getString(R.string.toast_need_overlay), Toast.LENGTH_SHORT).show()
    }
}

fun Context.dp(value: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics,
    ).toInt()
}

fun Context.dpSize(value: Int): Int = dp(value).coerceAtLeast(1)

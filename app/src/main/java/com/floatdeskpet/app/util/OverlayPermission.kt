package com.floatdeskpet.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.floatdeskpet.app.R

object OverlayPermission {
    fun granted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    }

    fun fallbackIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    fun open(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        val first = settingsIntent(activity)
        try {
            launcher.launch(first)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
        try {
            launcher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            return
        } catch (_: Exception) {
        }
        try {
            activity.startActivity(fallbackIntent(activity))
        } catch (_: Exception) {
            toastNeed(activity)
        }
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

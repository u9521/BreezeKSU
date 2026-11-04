package me.weishu.kernelsu.ui.webui.util

import android.app.Activity
import android.app.ActivityManager
import android.graphics.Bitmap
import android.os.Build

fun Activity.setTaskDescription(label: String, icon: Bitmap? = null) {
    if (icon != null) {
        @Suppress("DEPRECATION")
        setTaskDescription(ActivityManager.TaskDescription(label, icon))
    } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription(label))
        } else {
            val taskDescription = ActivityManager.TaskDescription.Builder()
                .setLabel(label)
                .build()
            setTaskDescription(taskDescription)
        }
    }
}


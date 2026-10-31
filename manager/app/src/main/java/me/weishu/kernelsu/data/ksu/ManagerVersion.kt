package me.weishu.kernelsu.data.ksu

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/** The manager package's own version, read from the installed package rather than BuildConfig. */
data class ManagerVersion(
    val versionName: String,
    val versionCode: Long
)

fun getManagerVersion(context: Context): ManagerVersion {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return ManagerVersion(
        versionName = packageInfo.versionName!!,
        versionCode = versionCode
    )
}

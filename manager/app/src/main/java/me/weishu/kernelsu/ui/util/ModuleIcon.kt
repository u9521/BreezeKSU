package me.weishu.kernelsu.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import me.weishu.kernelsu.data.ksu.getRootShell

private const val TAG = "ModuleIcon"

/**
 * Decodes a module icon, either from the app itself or from a `su:` path under `/data/adb`.
 * Lives in `ui/util` because the shortcut builder and the WebUI window both need it and may
 * not reach into each other. Returns a square bitmap of at most 512px, or null.
 */
fun loadModuleIconBitmap(context: Context, iconUri: String?): Bitmap? {
    if (iconUri.isNullOrBlank()) return null
    return try {
        val uri = iconUri.toUri()
        val rawBitmap = if (uri.scheme.equals("su", ignoreCase = true)) {
            val path = uri.path ?: ""
            if (path.isNotBlank()) {
                val shell = getRootShell(true)
                val suFile = SuFile(path)
                suFile.shell = shell
                SuFileInputStream.open(suFile).use { input -> BitmapFactory.decodeStream(input) }
            } else null
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }

        if (rawBitmap == null) {
            Log.w(TAG, "icon could not be decoded: $iconUri")
            return null
        }

        val side = minOf(rawBitmap.width, rawBitmap.height)
        val x = (rawBitmap.width - side) / 2
        val y = (rawBitmap.height - side) / 2
        val square = runCatching {
            Bitmap.createBitmap(rawBitmap, x, y, side, side)
        }.getOrDefault(rawBitmap)
        if (square !== rawBitmap && !rawBitmap.isRecycled) rawBitmap.recycle()

        if (side > 512) {
            runCatching { square.scale(512, 512) }.getOrElse { square }.also { scaled ->
                if (scaled !== square && !square.isRecycled) square.recycle()
            }
        } else {
            square
        }
    } catch (t: Throwable) {
        Log.w(TAG, "failed to load icon: $iconUri", t)
        null
    }
}

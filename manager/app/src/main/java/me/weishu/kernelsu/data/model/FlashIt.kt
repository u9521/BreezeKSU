package me.weishu.kernelsu.data.model

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.ksu.LkmSelection

/**
 * A flash the UI asked for, carried across process death by the navigation back stack.
 *
 * Lives in Layer 1 because it is a payload, not a screen: ui/nav/ serialises it and
 * data/ksu/ executes it, so neither side has to reach into the other.
 */
@Parcelize
sealed class FlashIt : Parcelable {
    @Parcelize
    data class FlashBoot(
        val boot: Uri? = null,
        val lkm: LkmSelection,
        val ota: Boolean,
        val partition: String? = null,
        val allowShell: Boolean = false,
        val enableAdb: Boolean = false,
        val backup: Boolean = false,
    ) : FlashIt()

    @Parcelize
    data class DownloadBoot(
        val url: String,
        val partition: String,
        val lkm: LkmSelection,
        val allowShell: Boolean = false,
        val enableAdb: Boolean = false,
        val backup: Boolean = false,
    ) : FlashIt()

    @Parcelize
    data class FlashModules(val uris: List<Uri>) : FlashIt()

    @Parcelize
    data object FlashRestore : FlashIt()

    @Parcelize
    data object FlashUninstall : FlashIt()
}

/**
 * What "uninstall" means to the user.
 *
 * The icon is a drawing concern and is mapped back in UninstallDialogBreeze, so this
 * enum keeps only the two string resources and stays free of Compose.
 */
enum class UninstallType(@param:StringRes val title: Int, @param:StringRes val message: Int) {
    TEMPORARY(R.string.settings_uninstall_temporary, R.string.settings_uninstall_temporary_message),
    PERMANENT(R.string.settings_uninstall_permanent, R.string.settings_uninstall_permanent_message),
    RESTORE_STOCK_IMAGE(
        R.string.settings_restore_stock_image,
        R.string.settings_restore_stock_image_message
    ),
    NONE(0, 0)
}

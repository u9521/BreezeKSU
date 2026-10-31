package me.weishu.kernelsu.ui.nav

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import me.weishu.kernelsu.R
import me.weishu.kernelsu.data.model.FlashIt
import me.weishu.kernelsu.data.model.TemplateInfo
import me.weishu.kernelsu.ui.icons.MaterialSymbols
import me.weishu.kernelsu.ui.util.FlashItSerializer
import me.weishu.kernelsu.ui.util.TemplateInfoSerializer

/**
 * Breeze's own navigation keys.
 *
 * Adding a main tab requires updating [TabRoute] only: the tab order, labels and icons all derive
 * from the enum, so the pager page count, the bar entries and the rail entries cannot drift.
 *
 * Deliberately independent from upstream navigation: Breeze owns its back stack and must keep
 * working when upstream reshapes its navigation graph. Only the argument payloads (`FlashIt`,
 * `TemplateInfo`) are shared.
 */
sealed interface BreezeRoute : NavKey, Parcelable {

    /** One main tab; declaration order is the pager order. */
    @Parcelize
    @Serializable
    enum class TabRoute : BreezeRoute {
        Home,
        SuperUser,
        Module,
        Settings;

        /** Label resource of this tab. */
        @get:StringRes
        val labelRes: Int
            get() = when (this) {
                Home -> R.string.home
                SuperUser -> R.string.superuser
                Module -> R.string.module
                Settings -> R.string.settings
            }

        val selectedIcon: ImageVector
            get() = when (this) {
                Home -> MaterialSymbols.Filled.Rounded.Cottage
                SuperUser -> MaterialSymbols.Rounded.Security
                Module -> MaterialSymbols.Filled.Rounded.Extension
                Settings -> MaterialSymbols.Filled.Rounded.Settings
            }

        val unselectedIcon: ImageVector
            get() = when (this) {
                Home -> MaterialSymbols.Rounded.Cottage
                SuperUser -> MaterialSymbols.Rounded.Shield
                Module -> MaterialSymbols.Rounded.Extension
                Settings -> MaterialSymbols.Rounded.Settings
            }
    }

    @Parcelize
    @Serializable
    data object About : BreezeRoute

    @Parcelize
    @Serializable
    data object Sulog : BreezeRoute

    @Parcelize
    @Serializable
    data object ColorPalette : BreezeRoute

    @Parcelize
    @Serializable
    data object AppProfileTemplate : BreezeRoute

    @Parcelize
    @Serializable
    data class TemplateEditor(
        @Serializable(with = TemplateInfoSerializer::class) val template: TemplateInfo,
        val readOnly: Boolean
    ) : BreezeRoute

    @Parcelize
    @Serializable
    data class AppProfile(val uid: Int) : BreezeRoute

    @Parcelize
    @Serializable
    data object Install : BreezeRoute

    @Parcelize
    @Serializable
    data class Flash(@Serializable(with = FlashItSerializer::class) val flashIt: FlashIt) :
        BreezeRoute

    @Parcelize
    @Serializable
    data class ExecuteModuleAction(val moduleId: String, val fromShortcut: Boolean = false) :
        BreezeRoute
}

package me.weishu.kernelsu.data.model

/**
 * Pending update parsed from a module's `updateJson`. [changelog] is fetched on demand when the
 * update dialog opens.
 */
data class ModuleUpdateInfo(
    val downloadUrl: String,
    val version: String,
    val changelog: ChangelogUrl,
    val versionCode: Int = 0,
) {
    /** Whether `updateJson` declares a changelog URL, kept distinct from a fetch that later fails or returns blank. */
    sealed interface ChangelogUrl {
        data object NotConfigured : ChangelogUrl
        data class Configured(val url: String) : ChangelogUrl
    }

    companion object {
        val Empty = ModuleUpdateInfo("", "", ChangelogUrl.NotConfigured)
    }
}

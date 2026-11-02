plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 1
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21

/**
 * Version numbers come from git, read through [Provider]s so the configuration cache can reuse an
 * entry instead of re-running `git` on every build.
 *
 * `rev-list --count HEAD` counts every commit, so the checkout must be full: CI runs with
 * `fetch-depth: 0`. A shallow clone would report 1 and silently mint a version code lower than
 * every published build, which no device would ever update to — so that case fails instead.
 */
val gitCommitCount: Provider<Int> = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.map { it.trim().toInt() }

val gitDescribe: Provider<String> = providers.exec {
    commandLine("git", "describe", "--tags", "--always")
}.standardOutput.asText.map { it.trim() }

extra["managerVersionCode"] = gitCommitCount.map { count ->
    check(count > 1) {
        "git rev-list --count HEAD returned $count: the repository is a shallow clone, so the " +
            "version code would collide with already published builds. Fetch full history " +
            "(fetch-depth: 0) before building."
    }
    30000 + count
}.get()
extra["managerVersionName"] = gitDescribe.get()

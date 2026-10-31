# Manager build

Covers: manager/build.gradle.kts, manager/settings.gradle.kts, manager/gradle/, manager/app/build.gradle.kts, manager/app/src/main/cpp/

## Toolchain is pinned, and the wrapper fetches it

The app is built with the Gradle wrapper the repository pins. `gradle/gradle-daemon-jvm.properties` pins the daemon to Oracle JDK 23 and `settings.gradle.kts` applies the foojay resolver, so Gradle downloads that JDK itself instead of inheriting whatever `JAVA_HOME` points at. Do not delete either file to "fix" a local JDK problem — that reintroduces a build that depends on the machine it runs on.

Building from WSL fails because the wrapper's Gradle distribution is not cached on the Linux side and `.gradle/` is locked by the Windows daemon. Build through the Windows wrapper (`cmd.exe /c "gradlew.bat ..."`) or Android Studio at the Windows path.

`jniLibs` must already contain `libksud.so` for every ABI in `abiFilters` before any Gradle command — see [userspace](../userspace.md).

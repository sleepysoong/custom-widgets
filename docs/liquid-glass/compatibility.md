# Liquid Glass build compatibility

Updated: 2026-09-25. See [the implementation plan](../../LIQUID_GLASS_PLAN.md).

## Selected versions

| Layer | Version | Evidence |
|---|---|---|
| Gradle | 9.7.1, checked wrapper checksum | `./gradlew --version` |
| AGP | 9.3.2, built-in Kotlin | plugin configuration succeeds |
| Kotlin / Compose compiler / serialization plugin | 2.4.10 | version catalog; Kotlin compilation succeeds |
| KSP | 2.3.12 | `kspDebugKotlin` succeeds |
| Hilt plugin/runtime/compiler | 2.60.1 | generated components compile |
| Room | 2.8.5 | DAO code generation and compilation succeed |
| Compose BOM | 2026.09.00 | official BOM maps UI/foundation/runtime to 1.12.1 and Material3 to 1.4.0 |
| Backdrop / Shapes | 2.0.1 / 1.2.1 | published Maven dependencies, real Backdrop API compiled |
| Activity Compose | 1.13.0 | metadata and compilation succeed |
| SDK | compile 37, target 35, min 31 | AAR metadata validation enabled and passes |
| JVM | OpenJDK 17.0.20.1, target 17 | local Gradle and Kotlin compile |
| Build tools | AGP-selected 36.0.0 | Gradle installed required package; not forced to upstream sample version |

Navigation 2.8.4, Glance 1.1.1, Hilt Work 1.2.0 and WorkManager 2.10.0 remain in use. Compilation is evidence for source compatibility, not evidence for every runtime interaction. Device verification is tracked separately.

## Resolved Compose graph

`debugRuntimeClasspath` was inspected. The AndroidX BOM selects AndroidX runtime/UI/foundation 1.12.1. The Backdrop Android variant declares JetBrains Compose Foundation 1.12.0; its Android Gradle metadata exposes AndroidX foundation/layout and those requests resolve to 1.12.1 through the BOM. This graph was exercised by debug/release compilation and duplicate-class checking. Keep `androidx.compose:compose-bom` separate from JetBrains Compose versions; the BOM does not version every arbitrary group.

## Intentional migration decisions

- Removed `org.jetbrains.kotlin.android` application with AGP built-in Kotlin.
- Kept Compose compiler and serialization plugins, and moved JVM options to `kotlin.compilerOptions`.
- Removed task-level suppression of AAR metadata checks and unsupported SDK warning suppression.
- Kept this an Android application; no KMP module conversion.
- Upgraded Hilt and Room for the new processing toolchain. No entity/schema migration was introduced.
- The library's JetBrains Compose dependencies and AndroidX BOM must be inspected in the resolved dependency report; no blanket version forcing or exclusions were added.
- Local SDK paths are in ignored `local.properties`, never in the versioned build.

## Reproduction

```bash
./gradlew --version
./gradlew :app:checkDebugAarMetadata :app:compileDebugKotlin
./gradlew :app:dependencies --configuration debugRuntimeClasspath
./gradlew :app:assembleDebug :app:assembleRelease
```

On memory-limited Linux containers, build variants separately to keep dex merging within the process limit:

```bash
./gradlew :app:assembleDebug --no-daemon --max-workers=1
./gradlew :app:assembleRelease --no-daemon --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8'
```

Sources: [Hilt Gradle setup](https://dagger.dev/hilt/gradle-setup.html), [KSP releases](https://github.com/google/ksp/releases), [Compose BOM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom), [Backdrop POM](https://repo.maven.apache.org/maven2/io/github/kyant0/backdrop/2.0.1/backdrop-2.0.1.pom).

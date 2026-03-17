# Munchkin Resource

Gradle plugin and runtime for generating strongly typed Compose Multiplatform resources.

## Modules

- `runtime`: runtime APIs such as `stringResource`, `colorResource`, `painterResource`, `toDp`, `toSp`
- `gradle-plugin`: `cn.szkug.munchkin.resources`, which generates `Res` accessors and syncs iOS resources

## Coordinates

```kotlin
plugins {
    id("cn.szkug.munchkin.resources") version "<version>"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:runtime:<version>")
        }
    }
}
```

## Capabilities

- Generates `Res.strings`, `Res.colors`, `Res.dimens`, `Res.drawable`, `Res.raw`
- Supports Android/iOS runtime resolution for strings, plurals, colors, painters, and dimens
- Syncs compose resources into the iOS bundle
- Supports optional iOS unused-resource pruning through KLIB IR scanning

## Publish

```bash
./gradlew :runtime:publishToMavenLocal :gradle-plugin:publishToMavenLocal
./gradlew :runtime:publishToMavenCentral
./gradlew :runtime:publishAndReleaseToMavenCentral
./gradlew :gradle-plugin:publishPlugins
```

`runtime` publishes to Maven Central. `gradle-plugin` publishes to the Gradle Plugin Portal and requires `GRADLE_PUBLISH_KEY` plus `GRADLE_PUBLISH_SECRET`.

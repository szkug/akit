# Munchkin Image

Compose Multiplatform async image loading with a shared API and pluggable engines.

## Modules

- `image`: core async image API and Compose modifiers
- `engine-coil`: Coil 3 engine for Android and iOS
- `engine-glide`: Glide engine for Android

## Coordinates

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cn.szkug.munchkin:image:<version>")
        }
        androidMain.dependencies {
            implementation("cn.szkug.munchkin:engine-glide:<version>")
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
        iosMain.dependencies {
            implementation("cn.szkug.munchkin:engine-coil:<version>")
        }
    }
}
```

## Features

- `MunchkinAsyncImage` and `Modifier.munchkinAsyncBackground`
- Glide and Coil engine backends
- NinePatch, GIF, Lottie, video first-frame thumbnails
- Optional blur and request size limiting
- Engine-agnostic request pipeline

## Publish

```bash
./gradlew :image:publishToMavenLocal :engine-coil:publishToMavenLocal :engine-glide:publishToMavenLocal
./gradlew :image:publishToMavenCentral :engine-coil:publishToMavenCentral :engine-glide:publishToMavenCentral
./gradlew :image:publishAndReleaseToMavenCentral :engine-coil:publishAndReleaseToMavenCentral :engine-glide:publishAndReleaseToMavenCentral
```

Publish after `munchkin-graph` and `munchkin-resource:runtime` for the same version are already available.

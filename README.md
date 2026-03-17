# Munchkin Cats

[中文文档](./README_CN.md)

Munchkin Cats is the sample app and reference integration workspace for the Munchkin Compose Multiplatform libraries.

## Start Here

Use this repository when you want to:

- try the libraries before integrating them into your app
- copy working sample code for Android or Compose Multiplatform
- verify behavior such as resources, async image loading, NinePatch, Lottie, blur, and shadows

## Run The Demos

- `apps/android`: Android sample app with image and resource demos
- `apps/cmp`: Compose Multiplatform sample app for Android and iOS

Open the project in Android Studio or IntelliJ IDEA, then run either sample app from the IDE.

## Pick The Guide You Need

- [Munchkin Graph](./libs/graph/README.md): NinePatch, Lottie painter, blur helpers, and shadow rendering
- [Munchkin Image](./libs/image/README.md): `MunchkinAsyncImage`, background image loading, Coil, and Glide
- [Munchkin Resource](./libs/resource/README.md): generated `Res` accessors and runtime resource APIs

## What You Can Learn From The Samples

- how to load remote and local images with one Compose API
- how to choose Coil on Android/iOS or Glide on Android
- how to generate strongly typed resource accessors from Android-style resources
- how to render NinePatch, Lottie, blur, and soft shadows in shared UI

If you are integrating a library directly, start with the README in `libs/graph`, `libs/image`, or `libs/resource`.

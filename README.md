# Munchkin Cats

[中文文档](./README_CN.md)

Sample workspace for the Munchkin Compose Multiplatform libraries.

This repository keeps the demo apps, benchmark target, and local build logic. Library source code is split into Git submodules under `libs/`.

## Initialize submodules

```bash
git submodule update --init --recursive
```

## Repository layout

- `apps/android`: Android sample app host
- `apps/cmp`: Compose Multiplatform demo host
- `apps/cmp-lib`, `apps/cmp-lib2`: shared demo/resource modules
- `benchmark`: macrobenchmark project for `apps/android`
- `plugins`: local Gradle build logic used by the sample workspace
- `libs/graph`: [munchkin-graph](./libs/graph)
- `libs/image`: [munchkin-image](./libs/image)
- `libs/resource`: [munchkin-resource](./libs/resource)

## Documentation

Library-specific documentation now lives with each extracted repository:

- `libs/graph/README.md`
- `libs/image/README.md`
- `libs/resource/README.md`

Use this root repository for sample integration, local composite-build development, and end-to-end verification across the submodules.

## Publishing

Publish the extracted libraries from this workspace root with the aggregate script:

```bash
./scripts/publish-all.sh --version 0.1.0
```

Useful variants:

```bash
./scripts/publish-all.sh --local
./scripts/publish-all.sh --version 0.1.0 --skip-plugin-portal
```

The script publishes in dependency order:

1. `libs/graph`
2. `libs/resource:runtime`
3. `libs/image:image`
4. `libs/image:engine-coil`
5. `libs/image:engine-glide`
6. `libs/resource:gradle-plugin`

Remote publishing requires these environment variables:

- Maven Central: `ORG_GRADLE_PROJECT_mavenCentralUsername`, `ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`
- Gradle Plugin Portal: `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`

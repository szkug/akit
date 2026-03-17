# Munchkin Sample

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

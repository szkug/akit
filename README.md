# Munchkin Sample

[中文文档](./README_CN.md)

Sample workspace for the Munchkin Compose Multiplatform libraries.

This repository is being split into a sample host plus three library submodules under `libs/`:

- `libs/graph`: [munchkin-graph](./libs/graph)
- `libs/image`: [munchkin-image](./libs/image)
- `libs/resource`: [munchkin-resource](./libs/resource)

## What stays here

- `apps/*`: Android and Compose Multiplatform demo applications
- `benchmark`: app benchmark verification
- `plugins/*`: local sample build logic
- repository integration glue for developing the sample against local library sources

## Split plan

The library sources currently still exist in this repository while the extraction is in progress.
They will be moved to the submodule repositories in follow-up commits, then referenced from this
sample workspace through `libs/`.

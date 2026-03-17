# Munchkin Cats

[中文文档](./README_CN.md)

Sample workspace and source monorepo for the Munchkin Compose Multiplatform libraries.

## Repository layout

- `apps/android`: Android sample app host
- `apps/cmp`: Compose Multiplatform demo host
- `apps/cmp-lib`, `apps/cmp-lib2`: shared demo/resource modules
- `benchmark`: macrobenchmark project for `apps/android`
- `plugins`: local Gradle build logic used by the workspace
- `libs/graph`: graphics toolkit module
- `libs/image`: image modules (`image`, `engine-coil`, `engine-glide`)
- `libs/resource`: resource modules (`runtime`, `gradle-plugin`)

## Documentation

Library-specific documentation lives with each module directory:

- `libs/graph/README.md`
- `libs/image/README.md`
- `libs/resource/README.md`

Use this root repository for source development, sample integration, publishing, and end-to-end verification.

## Publishing

Publish the libraries from this workspace root with the aggregate script:

```bash
./scripts/publish-all.sh --version 0.1.0
```

Useful variants:

```bash
./scripts/publish-all.sh --local
./scripts/publish-all.sh --version 0.1.0 --skip-plugin-portal
```

The script publishes in dependency order:

1. `libs:graph`
2. `libs:resource:runtime`
3. `libs:image:image`
4. `libs:image:engine-coil`
5. `libs:image:engine-glide`
6. `libs/resource/gradle-plugin`

Remote publishing requires these environment variables:

- Maven Central: `ORG_GRADLE_PROJECT_mavenCentralUsername`, `ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`
- Gradle Plugin Portal: `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET`

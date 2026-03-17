# Munchkin Cats

Munchkin Compose Multiplatform 组件库的示例工作区。

当前仓库只保留 Demo、Benchmark 和本地构建逻辑，库源码已经拆分为 `libs/` 下的 Git Submodule。

## 初始化子模块

```bash
git submodule update --init --recursive
```

## 仓库结构

- `apps/android`：Android 示例宿主
- `apps/cmp`：Compose Multiplatform Demo 宿主
- `apps/cmp-lib`、`apps/cmp-lib2`：共享 Demo / 资源模块
- `benchmark`：面向 `apps/android` 的 Macrobenchmark 工程
- `plugins`：sample 使用的本地 Gradle 构建逻辑
- `libs/graph`：[munchkin-graph](./libs/graph)
- `libs/image`：[munchkin-image](./libs/image)
- `libs/resource`：[munchkin-resource](./libs/resource)

## 文档位置

库能力相关文档已经跟随拆分后的仓库维护：

- `libs/graph/README.md`
- `libs/image/README.md`
- `libs/resource/README.md`

此仓库主要用于 sample 集成、submodule 本地联调，以及跨仓的端到端验证。

## 发布

在根仓使用聚合脚本发布拆分后的库：

```bash
./scripts/publish-all.sh --version 0.1.0
```

常用变体：

```bash
./scripts/publish-all.sh --local
./scripts/publish-all.sh --version 0.1.0 --skip-plugin-portal
```

脚本会按依赖顺序发布：

1. `libs/graph`
2. `libs/resource:runtime`
3. `libs/image:image`
4. `libs/image:engine-coil`
5. `libs/image:engine-glide`
6. `libs/resource:gradle-plugin`

远端发布依赖这些环境变量：

- Maven Central：`ORG_GRADLE_PROJECT_mavenCentralUsername`、`ORG_GRADLE_PROJECT_mavenCentralPassword`、`ORG_GRADLE_PROJECT_signingInMemoryKey`、`ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`
- Gradle Plugin Portal：`GRADLE_PUBLISH_KEY`、`GRADLE_PUBLISH_SECRET`

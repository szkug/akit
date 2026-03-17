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

根仓库主要用于 sample 集成、submodule 本地联调，以及跨仓的端到端验证。

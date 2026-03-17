# Munchkin Sample

Munchkin Compose Multiplatform 组件库的示例工作区。

当前仓库会逐步拆分为一个 sample 宿主仓库，以及 `libs/` 下的三个子模块仓库：

- `libs/graph`：[munchkin-graph](./libs/graph)
- `libs/image`：[munchkin-image](./libs/image)
- `libs/resource`：[munchkin-resource](./libs/resource)

## 当前仓库保留内容

- `apps/*`：Android 与 Compose Multiplatform Demo
- `benchmark`：基准测试工程
- `plugins/*`：sample 本地构建逻辑
- 供 sample 与本地库源码联调的仓库级集成配置

## 拆分计划

当前库源码还暂时保留在本仓内，后续提交会逐步迁移到对应的子仓库中，
再由当前 sample 工作区通过 `libs/` 进行引用。

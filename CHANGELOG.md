# 更新日志

## [1.0.0] - 2025-11-15

首个正式版本。

### 新增

- 假人连锁破坏：地毯（Carpet）/ GCA 假人挖方块时，自动连带破坏周围同类方块。
- 指令 `/fakeultimine`（别名 `/ful`）：
  - `on` / `off` / `toggle` / `all` / `list` / `status`
  - `shape` / `max` / `drop` / `durability` / `tool`
  - `only` / `except` / `onlytag` / `excepttag` / `filter`
  - `shapes` / `reload` / `prune` / `help`
- 7 种连锁形状：`cube3`、`cube5`、`plane3`、`plane5`、`tunnel`、`line`、`vein`。
- 方块过滤（两种模式）：
  - `only`：只挖、只连锁指定方块，列表外的方块连挖都挖不动；
  - `except`：除指定方块外都能连锁，用于保护某些方块；
  - 支持方块标签版本 `onlytag` / `excepttag`，例如 `minecraft:logs` 可让不同木种一起连锁。
- 配置持久化：`config/fakeultimine.json`，假人设置按名字保存，重启不丢失。
- 防护机制：全服每刻破坏预算、单假人冷却、世界边界与高度限制、硬度 -1 方块保护、
  `fakeultimine:blacklist` 方块标签、兼容 FTB Ultimine 的 `ftbultimine:excluded_blocks` 标签。
- 工具耐久保护：`durability=false` 时该假人挖方块完全不磨损工具。
- 空手连锁：假人手上没有工具时也能连锁（原先主手为空会直接停止）。
- 调试日志：`debug=true` 时每次连锁输出一行日志。

### 兼容

- Minecraft 1.21.1 / Fabric Loader 0.16.x
- Carpet 1.4.x、GCA 2.12.x、FTB Ultimine 2101.x（可选）

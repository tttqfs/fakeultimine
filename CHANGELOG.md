# 更新日志

## [1.1.0] - 2025-11-15

### 新增

- **方块过滤（两种模式，全指令控制）**
  - `/ful only <假人> <方块>`：只挖、只连锁指定方块，列表外的方块连挖都挖不动；
  - `/ful except <假人> <方块>`：除指定方块外都能连锁，用于保护某些方块；
  - 支持方块标签版本 `/ful onlytag|excepttag <假人> <标签>`，例如 `minecraft:logs`
    可让橡木、云杉、白桦等不同木种一起连锁；
  - `/ful filter <假人> list|clear` 查看与取消限制。
- **空手连锁**：假人手上没有工具也能连锁，只有「原本拿着工具、挖到一半工具断了」才会停止；
  想强制要求手持工具可用 `/ful tool <假人> true`。

### 变更

- 指令列表新增 `only` / `except` / `onlytag` / `excepttag` / `filter`。
- 配置文件 `bots` 节点新增 `filterMode`（off/only/except）与 `filterBlocks`。

### 兼容

- Minecraft 1.21.1 / Fabric Loader 0.16.x
- Carpet 1.4.x、GCA 2.12.x、FTB Ultimine 2101.x（可选）

## [1.0.0] - 2025-11-15

首个正式版本。

### 新增

- 假人连锁破坏：地毯（Carpet）/ GCA 假人挖方块时，自动连带破坏周围同类方块。
- 指令 `/fakeultimine`（别名 `/ful`）：
  - `on` / `off` / `toggle` / `all` / `list` / `status`
  - `shape` / `max` / `drop` / `durability` / `tool`
  - `shapes` / `reload` / `prune` / `help`
- 7 种连锁形状：`cube3`、`cube5`、`plane3`、`plane5`、`tunnel`、`line`、`vein`。
- 配置持久化：`config/fakeultimine.json`，假人设置按名字保存，重启不丢失。
- 防护机制：全服每刻破坏预算、单假人冷却、世界边界与高度限制、硬度 -1 方块保护、
  `fakeultimine:blacklist` 方块标签、兼容 FTB Ultimine 的 `ftbultimine:excluded_blocks` 标签。
- 工具耐久保护：`durability=false` 时该假人挖方块完全不磨损工具。
- 调试日志：`debug=true` 时每次连锁输出一行日志。

### 兼容

- Minecraft 1.21.1 / Fabric Loader 0.16.x
- Carpet 1.4.x、GCA 2.12.x、FTB Ultimine 2101.x（可选）

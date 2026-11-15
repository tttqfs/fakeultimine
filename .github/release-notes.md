## 假人连锁破坏 v1.0.0

给**地毯（Carpet）/ GCA 假人**加上 FTB Ultimine 式连锁破坏能力的 Fabric 附属模组。
服务端安装即可，客户端不需要装。

### 功能

- 假人挖方块时，自动把周围同类方块一起挖掉，形状可选（7 种）
- **方块过滤**：`only` 只挖指定方块（其它方块连挖都不挖）、`except` 保护指定方块；支持 `#标签`
- **空手也能连锁**，只有工具挖到一半断了才会停止
- 每个假人独立设置：形状、单次上限、掉落、耐久、是否要求正确工具
- 设置按假人名字保存在 `config/fakeultimine.json`，重启服务器、假人重进都不会丢
- 防卡服：单次上限 + 单假人冷却 + 全服每刻破坏预算 + 世界边界/高度检查
- 装了 FTB Ultimine 时自动遵守它的 `ftbultimine:excluded_blocks` 标签

### 快速开始

```mcfunction
/player Steve spawn
/item replace entity Steve weapon.mainhand with minecraft:diamond_pickaxe
/fakeultimine on Steve
/player Steve attack continuous
```

### 常用指令

| 指令 | 作用 |
| --- | --- |
| `/ful on\|off <假人>` | 开关连锁 |
| `/ful shape <假人> <形状>` | 设置形状（`/ful shapes` 查看） |
| `/ful max <假人> 64` | 单次连锁上限 |
| `/ful only <假人> minecraft:oak_log` | 只挖、只连锁橡木原木 |
| `/ful except <假人> minecraft:oak_log` | 除橡木原木外都能连锁 |
| `/ful onlytag <假人> minecraft:logs` | 按标签限制（所有原木） |
| `/ful filter <假人> list\|clear` | 查看 / 取消方块限制 |
| `/ful status [假人]` | 查看配置 |

### 安装

1. 安装 Fabric Loader（1.21.1）与 Fabric API
2. 把 `fakeultimine-1.0.0.jar` 放进 `mods/`
3. 安装 Carpet 或 GCA 提供假人，启动后即可使用

### 兼容

- Minecraft **1.21.1**（仅此版本）
- Fabric Loader 0.16+
- Carpet 1.4.x / GCA 2.12.x / FTB Ultimine 2101.x（可选）

### 已知说明

- 空手也能连锁，但**第一块是按徒手速度挖的**（泥土约 0.75 秒、石头约 7.5 秒），挖掉第一块后其余方块才会瞬间连锁
- 默认只连锁同一种方块；基岩、屏障、刷怪笼、命令方块等默认不连锁

### 反馈

问题与建议请到 [Issues](https://github.com/tttqfs/fakeultimine/issues)。

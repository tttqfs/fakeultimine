# FakePlayer Ultimine

**Chain breaking for Carpet / GCA fake players — fully command-driven, server-side only.**

FakePlayer Ultimine is a small addon for Minecraft 1.21.1 (Fabric) that gives fake players
from [Carpet](https://modrinth.com/mod/carpet) or [GCA](https://modrinth.com/mod/gugle-carpet-addition)
FTB Ultimine-style chain breaking. Toggle it per bot with one command, choose one of 7 shapes,
and let your quarry bot clear 3×3 tunnels without a client mod.

> 中文说明在同页面下方。Client installation is **not** required.

---

## Features

- Per-bot toggle: only bots you enable will chain-break. Real players keep using FTB Ultimine.
- **Block filter (only / except)**: restrict a bot to specific blocks (e.g. only oak logs — it won't even
  mine anything else), or protect blocks from being chain-broken. Supports block ids and tags (`#minecraft:logs`).
- 7 shapes: `cube3`, `cube5`, `plane3`, `plane5`, `tunnel`, `line`, `vein` (connected ore veins).
- **Works with empty hands**: a bot with no tool still chain-breaks (the first block just takes as long
  as mining it by hand). Set `/ful tool <bot> true` if you want to require a proper tool.
- Everything configurable in game: shape, max blocks, drops, tool durability, correct-tool requirement.
- Settings persist per bot name in `config/fakeultimine.json` (survives restarts / respawns).
- Lag guards: per-break cap, per-bot cooldown, global per-tick budget, world border / height checks.
- Soft integration: honors `ftbultimine:excluded_blocks` when FTB Ultimine is installed,
  plus its own `fakeultimine:blacklist` block tag (bedrock, barriers, spawners, command blocks…).
- No compile-time dependency on Carpet / GCA / Ultimine — they can be updated or missing without crashing.

## Requirements

| Required | |
| --- | --- |
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16+ (Java 21) |
| Fabric API | 0.100+ |
| Carpet **or** GCA | provides the fake players |

Optional: FTB Ultimine (its block exclusion tag is then respected automatically).

## Usage

```mcfunction
/player Steve spawn
/gamemode survival Steve
/item replace entity Steve weapon.mainhand with minecraft:diamond_pickaxe
/tp Steve 0 -60 0 270 0
/fakeultimine on Steve
/player Steve attack continuous
```

- Carpet's mining action is `attack` (left click); `use` is right click.
- The `<player>` argument is an entity selector, so the bot must be online.
- `/fakeultimine` has the short alias `/ful`.

| Command | Description |
| --- | --- |
| `/ful on\|off\|toggle <bot>` | enable / disable / toggle one bot |
| `/ful all <true\|false>` | enable / disable every online fake player |
| `/ful list` | list enabled bots |
| `/ful status [bot]` | show global or per-bot settings |
| `/ful shape <bot> <shape>` | choose the chain shape |
| `/ful only <bot> <block>` | **only chain-break this block** (and stop the bot from mining anything else) |
| `/ful except <bot> <block>` | chain-break everything **except** this block |
| `/ful onlytag <bot> <tag>` | same as `only`, using a block tag (`minecraft:logs`) |
| `/ful excepttag <bot> <tag>` | same as `except`, using a block tag |
| `/ful filter <bot> list\|clear` | show / clear the block filter |
| `/ful max <bot> <1-4096>` | max blocks per break |
| `/ful drop <bot> <true\|false>` | drop items from chain-broken blocks |
| `/ful durability <bot> <true\|false>` | consume tool durability or not |
| `/ful tool <bot> <true\|false>` | require a suitable tool |
| `/ful shapes` | list shapes |
| `/ful reload` | reload `config/fakeultimine.json` |
| `/ful prune` | drop records of disabled bots |

## Shapes

| id | Shape | Typical use |
| --- | --- | --- |
| `cube3` | 3×3×3 cube (default) | general mining |
| `cube5` | 5×5×5 cube | bulk clearing |
| `plane3` | 3×3 plane facing the mining direction | walls, flattening |
| `plane5` | 5×5 plane | large flattening |
| `tunnel` | 3×3 tunnel along the mining direction | mining corridors |
| `line` | 1×1 line | drilling |
| `vein` | connected same-block vein | ores, trees |

The direction is taken from the bot's look vector.

## Configuration

`config/fakeultimine.json` (generated on first launch, hot-reload with `/ful reload`):

```json
{
  "permissionLevel": 2,
  "maxBlocks": 64,
  "maxBlocksPerTick": 512,
  "cooldownTicks": 4,
  "defaultShape": "cube3",
  "requireCorrectTool": false,
  "dropItems": true,
  "damageTool": true,
  "allowRealPlayers": false,
  "respectBlacklist": true,
  "respectUltimineTags": true,
  "debug": false,
  "bots": { "Steve": { "enabled": true, "shape": "tunnel", "maxBlocks": 64 } }
}
```

---

# 中文说明

**让地毯（Carpet）/ GCA 假人像 FTB Ultimine 一样连锁破坏方块，服务端安装即可，客户端不需要装。**

## 功能

- 按假人单独开关：只有被 `/fakeultimine on` 的假人才会连锁，真实玩家照旧用 Ultimine 的按键。
- 方块过滤（only / except）：让假人只挖指定方块（如只砍橡木原木，别的方块连挖都不挖），
  或反过来保护某些方块不参与连锁；支持方块 id 与标签（`minecraft:logs`）。
- 7 种形状：立方体、平面、隧道、直线、连通矿脉。
- 全部可指令调节：形状、上限、掉落、耐久、是否要求正确工具。
- 设置按假人名字保存在 `config/fakeultimine.json`，重启 / 重新 spawn 都不丢。
- 防卡服：单次上限、单假人冷却、全服每刻预算、世界边界与高度检查。
- 装了 FTB Ultimine 时自动遵守它的 `ftbultimine:excluded_blocks` 标签；另有自己的黑名单标签。
- 不依赖地毯 / GCA / Ultimine 的编译期 API，它们换版本或缺失都不会崩服。

## 依赖

必需：Minecraft 1.21.1、Fabric Loader 0.16+、Fabric API 0.100+、Carpet 或 GCA。
可选：FTB Ultimine。

## 快速使用

```mcfunction
/player Steve spawn
/item replace entity Steve weapon.mainhand with minecraft:diamond_pickaxe
/fakeultimine on Steve
/player Steve attack continuous
```

> 地毯的挖掘动作是 `attack`（左键）；指令里的假人需要在线。

## 指令

`/fakeultimine` 与 `/ful` 等价，需要权限等级 ≥ 2。

- `/ful on|off|toggle <假人>` —— 开关
- `/ful all <true|false>` —— 一键开关所有在线假人
- `/ful list` / `/ful status [假人]` —— 查看状态
- `/ful shape <假人> <形状>` / `/ful max <假人> <数量>` —— 形状与上限
- `/ful drop` / `/ful durability` / `/ful tool <假人> <true|false>` —— 掉落、耐久、工具要求
- `/ful shapes` / `/ful reload` / `/ful prune` / `/ful help`

## 形状

`cube3`（3×3×3，默认）、`cube5`、`plane3`（3×3 平面）、`plane5`、`tunnel`（3×3 隧道）、
`line`（1×1 直线）、`vein`（同类连通矿脉）。朝向取假人视线方向。

## 注意事项

- 只连锁**同一种方块**；基岩、屏障、刷怪笼、命令方块等默认不连锁（见 `fakeultimine:blacklist` 标签）。
- 原方块的掉落、经验、工具消耗仍由原版处理；`dropItems=false` 只影响连锁出来的那部分。
- `durability=false` 时该假人挖方块完全不掉耐久。
- 服务器假人较多时，建议把 `cooldownTicks` 调到 10 以上。

## 实测

Fabric 1.21.1 + Carpet 1.4.147 + GCA 2.12.10：

- 开启连锁挖 3×3×3 石头：约 2 秒全部消失；关闭连锁同条件只挖掉 3 个。
- `tunnel` + 上限 64：一次挖掉 63 个额外方块。
- `drop=true/false`、`durability=true/false`、`tool=true`、重启恢复设置：均通过。

假人连锁破坏（FakePlayer Ultimine）

【模组简介】
给地毯（Carpet）/ GCA 假人加上 FTB Ultimine 式连锁破坏能力的 Fabric 附属模组。
用一条指令开关，服务端安装即可，客户端不需要装。

【它能做什么】
· 假人挖方块时，自动把周围同类方块一起挖掉，形状可选。
· 支持 7 种形状：3×3×3 立方体、5×5×5 立方体、3×3 平面、5×5 平面、3×3 隧道、1×1 直线、同类连通矿脉。
· 每个假人单独开关，可分别设置形状、单次上限、是否掉落、是否消耗耐久、是否要求正确工具。
· 设置按假人名字保存在 config/fakeultimine.json，重启服务器、假人重新上线都不会丢。
· 装了 FTB Ultimine 时，自动遵守它的 ftbultimine:excluded_blocks 方块标签。
· 自带黑名单标签 fakeultimine:blacklist，默认不连锁基岩、屏障、命令方块、刷怪笼等。

【使用方法】
1. /player Steve spawn（召唤假人）
2. /item replace entity Steve weapon.mainhand with minecraft:diamond_pickaxe（给工具）
3. /fakeultimine on Steve（开启连锁，短指令为 /ful on Steve）
4. /player Steve attack continuous（开始挖，attack 是左键）
5. /player Steve stop（停止）

【常用指令】
/fakeultimine on|off|toggle <假人>   开关
/fakeultimine all <true|false>       一键开关所有在线假人
/fakeultimine list                  查看已开启的假人
/fakeultimine status [假人]          查看配置
/fakeultimine shape <假人> <形状>     设置形状
/fakeultimine max <假人> <数量>       设置单次连锁上限
/fakeultimine drop <假人> <true|false>       连锁是否掉落物品
/fakeultimine durability <假人> <true|false> 是否消耗工具耐久
/fakeultimine tool <假人> <true|false>       是否要求手持正确工具
/fakeultimine shapes                查看所有形状
/fakeultimine reload                重载配置文件
/fakeultimine prune                 清理已关闭的假人记录

指令需要权限等级 2（OP），可在配置文件里修改。

【依赖与兼容】
必需：Minecraft 1.21.1、Fabric Loader 0.16+、Fabric API 0.100+、Carpet 或 GCA（提供假人）。
可选：FTB Ultimine（装了会同步它的方块排除标签）。
模组不依赖地毯 / GCA / Ultimine 的编译期 API，它们升级或缺失都不会导致崩服。

【注意事项】
· 只连锁同一种方块，不会误伤旁边的矿石或木头。
· 原方块的掉落、经验、工具消耗由原版处理；关闭掉落后只影响连锁出来的那部分方块。
· 单次上限 64、冷却 4 刻、全服每刻最多 512 格的默认值已比较保守；假人多时建议把冷却调大。
· 假人需要在线才能用指令指定（指令参数是实体选择器）。

【实测】
Fabric 1.21.1 + Carpet 1.4.147 + GCA 2.12.10 环境下：
开启连锁挖 3×3×3 石头约 2 秒全部消失，同条件关闭连锁只挖掉 3 个；
tunnel 形状 + 上限 64 一次挖掉 63 个额外方块；
掉落、耐久、工具要求、重启恢复设置均测试通过。

【开源】
MIT 协议，源码与问题反馈见 GitHub 仓库（见本页“相关链接”）。

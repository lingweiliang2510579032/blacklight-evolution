# 黑光猎杀者与吸收 R9

本文记录 R8/R9 的感染与猎手实现；当前 R10 已扩展普通生物及多形态吸收，范围和时长以 [R10 吸收说明](CONSUMPTION_R10.md) 为准。

第一只样例现在是独立怪物 `blacklight:blacklight_hunter`，具有自己的全身模型、骨骼、移动和蓄力挥爪 AI。不是普通僵尸附带永久增益。暂不添加自然生成，方便先测试。

- 普通僵尸感染病毒后经历 48 tick 异化，随后由服务端替换为猎杀者；继承名称、生命比例、持久化标记和装备。其他种类暂沿用感染状态。幼年僵尸也转化为成年尺寸的新物种。
- 新物种基础生命 48、近战伤害 7；追击玩家和未感染动物/村民。右爪蓄力后命中一次，再恢复。
- `/blacklight infected_sample` 或创造栏“黑光猎杀者刷怪蛋”直接生成完整新生物。
- 原型模式下把它打到 **30% 生命以下**，近距离面对它按 **G** 发动抓取吸收。按键可在控制设置中改绑。
- 46 tick（2.3 秒）内完成压制、前刺、牵拉、分部位瓦解和黑红生物质回流。完成一次回复 8 点生命（4 心）、120 生物质、30 进化点；目标直接被吸收，不再重复掉落普通战利品。
- 服务端负责选择近距离、视线可达且位于面前的目标，以及排他占用和一次性结算。受伤、目标死亡、离开范围、退出原型模式、换维度或下线会取消，不给奖励并释放 AI。主角不获得额外无敌。
- 存档不会保留临时抓取锁；保存期间被抓住的怪物恢复原来的 AI 设置。
- R9 最初仅支持猎杀者吸收；R10 已扩展普通生物。不包含人类身份/记忆复制。数值和 30% 门槛属于 Mod 第一版调校，不能称为原作数值复刻。
- V 的 R8 利爪变身与 R7 贴墙跑/爬/跃逻辑保持现有实现。

## 原版参考与适配

参考《虐杀原形》一代的[吸收动作实机展示](https://www.youtube.com/watch?v=LsTL2ZGtHrw)与[官方 PC 手册](https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/10150/manuals/Manual_english.pdf?t=1655760765)。该展示作者注明使用 FMV Alex 外观替换，故仅作为吸收动作参考，不据此复制角色材质。网络限制下本轮成功查看的是视频分镜与吸收 CG 短片，没有取得完整连续实机视频；精确原作帧节奏尚未逐帧匹配。

采用近身接触、利爪形态保留爪刃、局部黑色生物质聚合与回收的表现。模型、动作和实现均为自制适配；不是导入游戏原始资源，也不宣称一比一还原。

## 可选跑酷兼容

验证组合：Minecraft 1.21.1 / NeoForge 21.1.244 / Epic Fight 21.17.3.1 / ParCool 3.4.3.3 / Epic ParCool 21.0.0。

Epic ParCool 单独安装，资源不打包进 Blacklight。原型模式由 Blacklight 控制速度、贴墙、墙上停止和跳跃；普通模式允许 ParCool 运行。墙跑采用 Epic ParCool 的下肢及根骨动作，上半身保留适合加长爪刃的自制动作，防止空手动作把爪刃抬到脸前。其余墙上攀爬、停留与跳跃仍使用 R7 专用片段。

开发运行：把 [Epic ParCool 21.0.0](https://www.curseforge.com/minecraft/mc-mods/official-epic-fight-x-parcool/files/7490217) 的 jar 放到忽略目录 `art-work/compat/epicparcool-21.0.0.jar`，使用 `gradlew runClient -PwithEpicParCool`。ParCool 3.4.3.3 由 Gradle 下载。常规发行安装则把这两个官方 mod 放入游戏 mods 文件夹。未安装时使用原有墙跑动画，不强制增加依赖。

## 资产制作与验证

- `tools/build_hunter.py` 生成独立全身的 GeckoLib 模型和动作 JSON，可用 Blockbench/GeckoLib 工作流继续编辑。
- `tools/author_consume_r9.py -- WORK R7_AUTHORING_DIR OUTPUT` 用 Blender IK 制作近身吸收动作，导出 Epic Fight 片段。
- `-PcaptureHunterPreview=OUTPUT -PquickPlayWorld="Blacklight Hunter QA r9"` 在独立存档验证感染实体替换、吸收视觉和回报。
- GameTest 包括实体替换、取消感染、吸收独占与一次结算、距离中断与 AI 恢复，并保留墙跑测试。

下列 R8 工具用于变身和感染过渡：

- `tools/build_infected_zombie.py -- WORK OUTPUT` 在 Blender 中制作感染覆盖模型，导出原始三角面至 `assets/blacklight/models/infected_zombie.json`。
- `tools/author_morph_r8.py -- WORK R7_AUTHORING_DIR OUTPUT` 在 R7 可编辑 IK 工程上重做变身动作并导出 JSON。
- Blender 的 Epic Fight 参考骨架仅用于本地制作，不随开源项目提交。
- `gradlew test build`：感染时间/分阶段生长，动作时长、骨架与长爪头部间隙等测试。
- `gradlew runGameTestServer -PwallTraversalTest -PwithEpicParCool`：实际方块碰撞的墙跑回归。
- R8 的 `captureInfectedPreview` 拍摄脚本属于历史覆盖模型流程；独立实体的当前验收使用上面的 `captureHunterPreview`。截图不代替实际位移测试。

已排除 ParCool 3.4.3.0：该版在独立服务器加载 LocalStamina 客户端类时崩溃。3.4.3.3 与不安装可选跑酷模组两种配置均通过 3 项实际墙面碰撞 GameTest。

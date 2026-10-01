# Blacklight: Ascension 0.2.0-rc.53

Minecraft 1.21.1 / NeoForge 的黑光病毒同人 Mod。本轮是本地试玩候选版：固定当前实现，集中修复问题，等待用户验收；不是完整复刻或已验收的正式版。

RC53 新增巨型组织蜂巢、感染地貌和 7 种组织方块；巢内的吞噬者通过真实进食积累生物质，逐步成长为巨构。玩家可以破坏营养节点、清理猎物或摧毁核心干预成长。新结构自然生成于新区域，见 [巨巢、吞噬成长与组织地形](docs/RC53-devourer-and-organic-hive.md)。

RC52 降低接触传播，限制安瓿感染半径与穿墙传播，分离普通感染与巨构形态。普通感染不会自动造出巨构；管理员可用 `/blacklight spawn_colossus <物种>` 查看巨构。见 [传播平衡与巨构门槛](docs/RC52-infection-balance.md)。

RC51 接入 82 类感染模型（78 类普通生物与 4 类巨构首领），按物种绑定基础动作、受击与吸收表现，并接入巨构首领的感染转换、分段受击和持久化。见 [感染模型与巨构首领](docs/RC51-infected-models-and-bosses.md)。Boss 专属战斗招式仍留待后续制作。

RC50 接入吸收身份、X 键切换本体与伪装、外观保存及阵营识别。见 [吸收身份与切换](docs/RC50-disguise.md)。

RC49 修复抓取、持有阶段受伤即取消，补上近地受击抓取容差与提前右键吸收输入。见 [战斗中抓取修复](docs/RC49-combat-grab.md)。

RC48 补接肌肉撕右臂、徒手胸部吸收，并修正利爪吸收时的手腕与双手接触。见 [肌肉、徒手与利爪对位](docs/RC48-muscle-bare-and-claw-grip.md)。

RC47 对齐利爪、拳套、鞭拳当前吸收片段的受害者动作和效果时序，修复拳套效果出现前就结算的问题。见 [三种武器受害者表现](docs/RC47-claw-hammer-whip-receivers.md)。

RC46 补接受害者物种骨架，修正动画绑定成功判定，加入刀锋吸收的躯干分体、断面与两半回收。见 [受害者动作与刀锋分体](docs/RC46-receiver-animation-and-sever.md)。

RC45 修复空中左键误触发远距离追击与攻击结束后的动作交接，分离鞭拳空中重击悬停；修复冲刺加速被摩擦抵消，并支持落水后按前进与奔跑键上浮、接续水面奔跑。见 [空中攻击、冲刺与水面起跑](docs/RC45-air-and-sprint.md)。

RC44 统一镜头与移动方向，修复鼠标被目标构图覆盖、观察时自动换目标、墙面/落地角度突变和高速跟随过度滞后；加入变形镜头鼠标接管与物品瞄准交接。见 [镜头控制与跟随修复](docs/RC44-camera-control.md)。

RC43 修正左上角原版头像的上下方向；血条布局、跳跃和承伤沿用 RC42。

RC42 使用本地解包原版 HUD 素材替换生命条，降低并限制跳跃力度，修复空中片段停帧，并按普通怪、精英和首领重设感染者承伤。见 [原版血条、跳跃与战斗数值](docs/RC42-native-hud-and-combat.md)。

RC41 将生命数字移入 U 键面板的属性页，左上角仅保留血量比例条与受伤变化。见 [纯血条与属性页](docs/RC41-health-bar-and-attributes.md)。

RC40 增加左上角数字生命条，接入感染后 100 点基础生命与四级生命成长，统一敌我生命/伤害/治疗单位，并小幅提高移动与连续攻击节奏。见 [生命 HUD 与数值体系](docs/RC40-health-hud-and-balance.md)。

RC39 修复目标镜头吞掉鼠标输入、失效目标残留锁定，以及移动/受击状态切换抢回手动镜头的问题。见 [镜头侧面锁死与控制衔接](docs/RC39-camera-input-ownership.md)。

RC38 修复重生/重进世界后动画模式未恢复，以及 E 抓取的镜头方向、距离和按键响应。见 [重生与抓取修复](docs/RC38-respawn-and-grab.md)。

RC37 修正步态时钟反复重置、移动时误切站立、镜头与方向输入参照不一致，并完善 F8 的暂停调参、分页、撤销和可靠保存。见 [动作、镜头与 F8 修正](docs/RC37-locomotion-camera-tuner.md)。

RC36 接入分状态旋转、动作提示、六组长变形身体与镜头、热能变形景深，共 36 条动作时间线、78 个事件。实现范围和仍有差异的部分见 [镜头与长变形](docs/RC36-source-camera-runtime.md)。

RC35 接入原版镜头逼近/滤波、十种吸收的推近与缩放、拳套事件及源震屏参数，共 18 条动作路由、45 条事件，并去掉重复跑步晃动。该版未包含后续 RC36 的镜头旋转、电影镜头和景深，历史说明见 [镜头事件移植与剩余范围](docs/RC35-source-camera-events.md)。

RC34 恢复未感染及普通模式下的 Minecraft 生存动作与模型，并接入本地解包追随镜头的分状态距离和平滑设置，见 [镜头与原版生存表现](docs/RC34-camera-and-vanilla-mode.md)。

RC33 修复黑光模式饥饿限制，并分离手动锁定与准星关注目标，统一攻击、抓取和扑击的选敌校验。见 [饥饿修复与目标判定审计](docs/RC33-hunger-targeting.md)。

RC32 修正护盾横在肩颈的握持错误：直接绑定手腕，并补入漏掉的原版独立护盾挂点轨道。见 [护盾握持修复](docs/RC32-shield-grip.md)。

RC31 替换护盾为新方块甲壳，补入三档实际破损、碎片、前臂重建组织及手腕绑定；同时补充装甲受击反馈、感知渐变和热能身体颜色层次。见 [护盾重建与感知更新](docs/RC31-shield-and-senses.md)。

RC30 接入确认后的第四版方块装甲、自制像素贴图和原版甲片生长时序，修正身体缩隐缩放对骨架的影响；见 [装甲模型、变形与还原边界](docs/RC30-block-armor.md)。

RC29 接入护盾原始参数、装甲伤害倍率、六条解包防御动作和感知视觉适配；详见 [原版依据及尚未还原的部分](docs/RC29-utility-source.md)。军事科技和实验室尚未完成。

RC28 修复贴地离地时蓄力跳被误判为滑翔，统一客户端与服务端的空格输入归属和离地条件，见 [蓄力跳与滑翔修复](docs/RC28-charge-glide-input.md)。

RC27 区分低障碍短翻越、疾跑墙顶出口与慢爬上沿；修复落脚后强制下落及丢失疾跑的问题，见 [墙顶交接与原版动作依据](docs/RC27-wall-top-handoff.md)。

RC26 加入水面疾跑并修正普通地形上的步态切换抖动，见 [水面疾跑与地形跑步动画](docs/RC26-water-running-and-terrain-gait.md)。实际效果待用户试玩。

RC25 修复拳套重砸缩放和后退转身，见 [拳套膨胀与朝向](docs/RC25-hammer-scale-and-facing.md)；RC24 对齐 [猎手吸收](docs/RC24-hunter-consume.md)，RC23 完善 [攻击转向与吸收收尾移动](docs/RC23-direction-and-consume.md)。

RC22 扩展刀锋、重锤、鞭拳 9 个接招点，并核对全套动作系统，见 [其他武器与全套还原现状](docs/RC22-full-action-audit.md)。

RC21 接入 7 个已核对的原版接招时刻，替换通用等待公式；见 [原版时序接入与还原边界](docs/RC21-source-combo-timing.md)。

RC20 修复轮盘收起武器被拒绝、自由移动朝向与收招取消，见 [动作衔接与原版数据对照](docs/RC20-action-transitions.md)。实际手感待试玩。

RC19 修复黑光模式在水中无法上浮：流体中恢复原版跳跃输入，停用陆地蓄力/墙跑预测，保留游泳动作。见 [水中上浮修复说明](docs/RC19-swimming-fix.md)。

RC18 接入冰下研究站生存开局，见 [研究站、地图和主动觉醒说明](docs/RC18-survival-opening.md)。雪地学徒制图师提供地图，玩家探索研究站取得样本，按住使用键 2 秒觉醒；腐肉、红石合成病毒的配方已移除。感染者攻击和样本溅射不会强制感染玩家。

新增 [RC6 吸收方向与模型连接修正](docs/CONSUME_RC8.md)，此前见 [RC5 普通生物接收动作与双方位移](docs/CONSUME_RC5.md)、[RC4 大型吸收与释放阶段](docs/CONSUME_RC4.md)、[RC3 对位修复](docs/CONSUME_RC3.md) 和 [RC2 吸收接入说明](docs/CONSUME_RC2.md)。

## 当前入口

- [候选版更新说明与边界](docs/RELEASE_RC1.md)
- [用户试玩清单](docs/ACCEPTANCE_RC1.md)
- [抓取和吸收规则](docs/grab-consume-rules.md)
- [攻击选敌与追击](docs/combat-targeting.md)
- [攻击距离升级与环境反馈](docs/attack-environment-and-reach.md)
- [当前已知问题](docs/KNOWN_ISSUES_AND_SLOTS_ZH.md)

## 操作

| 默认按键 | 用途 |
| --- | --- |
| B | 原版模式 / 黑光模式 |
| Z | 武器能力圆盘 |
| U | 升级界面 |
| Tab | 目标锁定 |
| 左键 / 右键 | 当前形态的普通 / 特殊攻击，地面、空中和组合输入有不同分支 |
| E | 抓取；持有时再次按下投掷 |
| X | 主角本体 / 最近吸收的身份切换 |
| 持有时右键 / 左键 | 吸收 / 投掷 |

病毒安瓿是感染入口；生存模式在研究站冷藏箱获得，创造模式也可取得，调试可使用 `/blacklight infect`。未感染时按住使用键 2 秒主动觉醒；已感染时潜行并使用可投掷感染生物。原版模式恢复 Minecraft 生存交互。研究站生成、成长节奏和蜂巢奖励仍需用户实玩验收，不能用调试解锁替代。

可吸收名单中的生物：最大生命值低于 50 可以直接抓取，达到 50 后必须先降至剩余 15% 或以下。铁傀儡、凋灵、末影龙已加入名单和大型攀附分支；Boss 使用原生死亡结算，细节待用户验收。

## 名称与打包约定

英文名为 **Blacklight: Ascension**，中文展示名为“黑光：进化”。存档使用的 Mod ID 仍为 `blacklight`。

JAR 与安装 ZIP 统一使用 `Blacklight-Ascension-MC<我的世界版本>-NeoForge-<Mod版本>`：当前为 `Blacklight-Ascension-MC1.21.1-NeoForge-0.2.0-rc.53.jar` / `.zip`。

名称与两个版本分别从 `gradle.properties` 的 `mod_archive_name`、`minecraft_version`、`mod_version` 自动生成。以后更改版本只改配置，不手动省略或缩写版本号。`./gradlew assemble` 同时生成 `build/libs/` 下的 JAR 和 `build/distributions/` 下的安装 ZIP；ZIP 的 `mods/` 中只有本次构建的 JAR，不收集旧产物。需更新的发布说明仍随版本维护。

## 依赖与安装

当前构建使用 Minecraft 1.21.1、NeoForge 21.1.244、GeckoLib 4.9.2、Epic Fight 21.17.3.1，Java 21。前置 Mod 单独安装，JAR 不内置它们。可选 ParCool / Epic ParCool / Weapons of Miracles 的兼容范围以 JAR 元数据为准。

本地候选目录为 `release/local-0.2.0-rc.53/`。安装该目录的 Mod JAR 前移除实例中旧版 Blacklight JAR，保留其他依赖；先退出游戏并备份存档。旧世界需要探索新区域生成研究站和巨型蜂巢；已生成交易的旧制图师不会自动改卖新地图。

**RC53 本地候选 JAR 保留 213 个动作文件及 11 份配套数据，共 224 个文件。** 无需额外动作目录或 JVM 动作路径参数；已有 `-Dblacklight.localWeaponMotions` 参数的玩家应移除它。该参数仍可用于开发时替换整套资料。普通源码构建不会自动包含本地原作解码资源；构建方式与范围见 [RC17 动作内置说明](docs/RC17-bundled-motions.md)。此包未公开上传，原作解码资料不属于原创 GPL 素材。

## 构建和验收

```text
gradlew.bat clean assemble --no-daemon
```

当前默认只执行 `assemble` 编译打包，由用户试玩；单元测试、碰撞测试、GameTest 和游戏内验收仅在用户明确要求时运行。早期版本的截图检查记录不代表当前版本已验收。上述普通源码构建不内置动作；本地内置版构建需增加 `-PbundleMotionDirectory=<完整动作目录>`，详见 RC17 说明。

## 协作

公开仓库：[mengyuyuan/blacklight-evolution](https://github.com/mengyuyuan/blacklight-evolution)。原创代码和原创素材采用 [GPL-3.0-only](LICENSE)，见 [贡献指南](CONTRIBUTING.md)。公开 PR 只提交源码、自制资源和说明；含原版派生动作的本地试玩包不公开发布。

早期说明保留在 [历史 README](docs/history/pre-rc1-README.md)，其中旧按键、血量规则及“已完成/未完成”描述不作为当前验收依据。

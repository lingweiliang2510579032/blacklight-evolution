# 现成动作资源评估（2026-09-20）

用户指出 R19/R20 自编攻击缺少全身协调，要求寻找现成动作包。自编动作不作为还原成品发布。

当前项目是 Minecraft 1.21.1、NeoForge 21.1.244、Epic Fight 21.17.3.1。

| 候选 | 官方版本信息 | 本轮决定 |
| --- | --- | --- |
| Weapons of Miracles | 2.0.178，1.21.1 NeoForge；文件页注明测试 Epic Fight 21.17.3.1 | 已下载检查，放入独立 QA 实例，先预览 Agony/Torment 动作与黑光模型的关系。 |
| Epic Fight 内置动作 | 已是项目依赖，包含 sword/greatsword/fist 连击与冲刺、空中动作 | 同步预览徒手动作，作为全身发力基础候选。 |
| Battle Arts | 21.17.1.2，1.21.1 NeoForge；有徒手、重武器与不同战斗风格 | 备用；尚未在本机验证。 |
| Epic Fight Animations 旧资源包 | 检索到主文件为 1.16.5 的 EF_anmation_1.16_v1.6.zip | 不直接安装到当前 1.21.1 实例。 |

来源：

- https://www.curseforge.com/minecraft/mc-mods/weapons-of-miracles-epicfight/files/8829395
- https://www.curseforge.com/minecraft/mc-mods/battle-arts
- https://www.curseforge.com/minecraft/texture-packs/epic-fight-animations
- https://github.com/Forixaim/Battle-Arts/blob/1.21.1/LICENSE

WOM 下载来源是文件页提供的 Curse Maven 坐标 `curse.maven:weapons-of-miracles-epicfight-918614:8829395`。本机 JAR SHA-256 为 `0c36ba17bc812c65f5e37f9227d8b6a5185aac70b82a88d1bf52ee13103592cc`。

WOM 的 JAR 只用于本机独立测试实例；未加入本项目发布包或公开仓库。其素材为保留所有权利，正式接入优先通过独立依赖在运行时调用，不能把素材复制过来标为本项目原创。Battle Arts 的源码为 MIT、作者素材为 CC-BY-SA，社区素材按各自来源处理，不能只看源码许可证。

现成动作仍需检查：单手/双手习惯是否适合刀臂、拳肘是否穿头、脚步与实际位移是否一致、膝髋转动是否保留、接触帧与伤害是否一致。普通刀剑动作不能直接代表原版刀臂，现成重拳也不能直接代表原版肘击与吸收。

## 本机实测与选择

已在上述项目版本下成功加载 WOM 2.0.178，并通过已注册动画键实际播放以下 12 段动作，分别录制两个角度：

- `wom:biped/combat/agony_auto_1` 至 `_4`，绑定当前刀臂。
- `wom:biped/combat/torment_auto_1` 至 `_3` 及 `torment_charged_attack_1`，绑定当前锤拳。
- `epicfight:biped/combat/fist_auto1` 至 `fist_auto3` 及 `fist_dash`，绑定当前肌肉形态。

初步观察：Torment 有明显屈膝、躯干倾斜及双臂配合，适合优先评估锤拳的发力基础，但现有大型拳套在部分蓄力姿势中遮挡或穿过上身。Agony 有低姿态和腾空转动，不能把整套连击等同于 Prototype 1 的刀臂攻击；武器朝向和接触轨迹仍需适配。Epic Fight 徒手可作为普通拳击的对照，尚不足以替代黑光重击与专用吸收动作。

这些是候选素材的兼容性和视觉评估，不是已经接入的正式战斗方案。QA 直接播放依赖的攻击类；正式接入前必须分离其伤害事件、校准根位移与接触时间，避免本项目和依赖重复结算攻击。没有复制依赖的动画 JSON 到本项目。

复现入口：`runClient -PcaptureGameplayPreview=<输出目录> -PpackReview -PquickPlayWorld="Blacklight Consume QA r10"`，配合本机原有预览 init script。仅允许专用 QA 存档，依赖 JAR 仅在该运行目录的 mods 下。`GameplayPreview` 受测试属性控制，构建发布 JAR 时排除。第一次录制的铁傀儡遮挡了发力过程，第二次录制移除目标，仅检查空挥动作；它不验证命中、联网或正常游戏完整兼容性。

验证：普通 `test build` 成功，93 个单元测试通过；R20 战斗逻辑的 34 个 GameTest 通过。上述检查不能替代动作质量评估。

## R21 实际适配

现已将以下 9 个动作接入正常 `WeaponCombatSystem.request` 路径，使用 Blacklight 自己注册的 `AdaptedWeaponAnimation`，不再直接播放 WOM 的攻击类：

| 动作 | 来源 |
| --- | --- |
| 刀臂两段普攻 | Epic Fight sword_auto1 / sword_auto2，采用地面斩击而非 Agony 翻腾 |
| 锤拳两段普攻 | WOM torment_auto_1 / torment_auto_2 |
| 肌肉形态直拳、交叉拳 | Epic Fight fist_auto1 / fist_auto2 |
| 肌肉形态上挑 | WOM torment_auto_3 |
| 肌肉形态冲刺拳、空中拳 | Epic Fight fist_dash / fist_airslash |

保留来源的躯干、髋部、膝和腿部旋转，读取来源第一攻击阶段的接触时间（只读时间，不执行事件），按服务端蓄力/接触/收招时序重采样；无有效阶段时间时使用显式配置，一帧过渡算入总时间。锤拳两段分别延长至 26/28 ticks，接触窗口为 7–14 / 9–15 ticks，避免把 Torment 的完整重拳压成两三帧；未装 WOM 的旧动作也同步调整时间。脚步由独立 COORD 曲线负责，去掉来源重复的水平 Root 平移，肌肉冲刺不再叠加旧服务端推进。取消适配攻击时恢复闲置动作和操作状态。

对左右臂分别做固定关节角度修正，不改变武器尺寸。检查采用当前实际加载的骨架和武器网格，60 Hz 采样，逐招间隙写入 `adapted-clearance.json`。此检查覆盖动作轨迹内的网格顶点；不等同于所有三角面、过渡混合姿势或所有皮肤模型均无穿模。双视角实机已检查，命中目标时机仍需持续结合玩家手感评估。

WOM 2.0.178 已安装在本机开发 `run/mods`、专用预览和 GameTest 运行目录；未纳入发布 JAR。缺少 WOM 时对应三招退回此前自制动作，其余六招照常使用已有 Epic Fight。

本轮范围不含鞭拳、利爪、刀臂三连/空降和锤拳砸地等专用动作；它们继续使用现有实现，不能称为所有武器动作已完成还原。

最终验证：95 个单元测试通过；装有 WOM / 未装 WOM 的专用服务端均通过 35 项 GameTest，覆盖九招的选择、伤害窗口、单次伤害与收招，并实际加载服务端动画资源。最终 60 Hz 手部顶点/头部检查最小间隙 0.0567 格；九招两个角度重新录制完成。发布 JAR 检查确认不含预览/GameTest 类或 Epic Fight/WOM 动画素材。

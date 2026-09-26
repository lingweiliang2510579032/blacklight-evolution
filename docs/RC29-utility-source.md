# RC29：防御与感知能力的原版依据及实现边界

Minecraft 1.21.1 / NeoForge / Blacklight: Ascension 0.2.0-rc.29。网络协议 26，客户端与服务端须一起更新。

## 本轮接入

- 防御与感知各自独立于攻击能力，同组互斥。轮盘新增可见分页按钮，仍支持 Tab；G 快速开关上次使用的防御能力，H 开关感知能力，可在按键设置修改。原作 PC 的数字键与 Minecraft 快捷栏冲突，因此默认键是适配。
- 护盾使用独立盾值，完整吸收可防御的正面攻击；破盾一击超过剩余盾值的部分穿透。开关不补满，盾值及受击、重建时间存档。收起后逐渐恢复；破裂后等待重建。新增百分比和重建倒计时。
- 装甲以明确的受伤倍率替代旧版 +12 护甲、+4 韧性，避免叠加旧减伤。禁用滑翔和低障碍自动翻越，保留墙体移动。增加手臂与手部外壳，并避开武器占用的手。
- 感染视觉和热能视觉使用世界颜色处理及实体实际姿态的着色轮廓，移除调试包围盒。感染视觉区分感染与普通目标；热能视觉把铁傀儡等非生物目标标为冷色。关闭、切换和资源重载清理效果，保留世界深度遮挡。

## 已核对的原版参数

本地解包 alex_tod.p3d SHA-256：`383b26f83fcc87cf5146604dddd28219a520a33129ea6510484e1d36e9e9ed78`。
`tools/extract_utility_settings.py` 可重新读取数值，输入必须是用户本地解包文件，不分发原始游戏文件。

| 对象 / 字段 | 原始值 | 当前接入 |
| --- | --- | --- |
| ShieldPower / health | 250 | 盾值 250，直接以 Minecraft 伤害数值扣除 |
| deployedScale / deploySpeed / retractSpeed | 1 / 7 / 3 | 展开及收起的模型比例变化速度 |
| healingTime | 10 秒 | 收起状态每秒恢复最大盾值的 1/10 |
| undamagedTimeBeforeHealing | 1 秒 | 距上次受击至少 1 秒才开始逐步恢复 |
| autoHealFromBrokenTime | 0.25 秒 | 破裂等待 0.25 秒后进入 10 秒重建，共 205 tick |
| AlexArmourTransformationDescription / healthDamageScale | 0.525 | 可被装甲减免的伤害乘以 0.525，即减伤 47.5% |

ShieldPower 配置对象位于 TOD `0x6455`，装甲对象位于 `0x758e`。字段顺序交叉核对了本地 prototypeenginef.dll 的元数据注册：ShieldPower `VA 0x1008bac0`；装甲 healthDamageScale 注册附近 `VA 0x10087c93`，对象偏移 `0x58`。地址仅适用于核对时的本地 DLL，首选基址 `0x10000000`。

同时检查了 ShieldPower 原生伤害处理（约 `VA 0x1008b730–0x1008b82d`）和更新状态机（`VA 0x1008bd90`）：吸收量从来袭伤害中扣除；状态 0/1 收起及收起中可以回血，状态 3 展开时不回血；状态 4 等待后转入状态 5 重建，完成时补满。没有采用“破盾一击无条件全部吸收”或“展开时持续恢复”的自定规则。

## 解包动作

新增六条原版身体动作，使用原始帧率和完整片段重定向到 Epic Fight 骨架。关闭能力只淡出覆盖，不把倒放展开片段冒充原版收起动作。

| 私有动作 ID | 解包源片段 |
| --- | --- |
| utility_shield_deploy | alex_power__transform_shield_come_around_left_003_Brian_1 |
| utility_shield_hit | alex_shield_impact |
| utility_shield_run | alex_shield_run_n |
| utility_shield_sprint | alex_shield_sprint_n |
| utility_armor_transform | alex_act_armor_transformation |
| utility_armor_idle | alex_armor_amb_fight |

导出脚本：`tools/export_utility_motions.py`。每条动作保存源 SHA-256。移除根位移，由 Minecraft 控制实际移动；攻击、抓取、吸收、受击、跳跃与墙体动作优先。盾牌覆盖左臂，装甲移动中仅叠加变身上身动作；不能据此声称原版所有 FIG 状态切换条件已还原。212 个私有动作/数据文件随本地包提供，原有 206 个文件保持不变。

## 尚未等同原版的部分

- 盾值 250 是读到的原始数值；原作生命及武器伤害与 Minecraft 不同，尚未建立伤害单位换算，不能声称抗打次数与原作一致。正面夹角阈值、Minecraft 特殊伤害豁免、粒子与声音是适配。
- 装甲移速 -25%、击退抗性 +0.6 暂留已有 Minecraft 适配；没有将它们标为解包确认参数。装甲外壳是现有自制模型，模型变形过渡、动作混合窗口及 FIG 片段裁切还没有完整复刻。
- 感知颜色处理是 Minecraft 着色器适配，未移植原游戏着色器；仅处理已加载同步目标，感染 32 格、热能 24 格、最近最多 48 个为已有性能限制。不是整张地图透视，不能声称所有原作热源材质、感染伪装或场景效果已复刻。
- 当前没有经确认的感知第三人称变身片段，未把解包的第一人称 hands/cam 文件套到全身骨架冒充。轮盘快捷切换沿用服务端忙碌检查。
- 军方科技、实验室及超级士兵仍是后续工作；生存研究流程是用户要求的 Minecraft 扩展，应与原作兵种、设备及解包动作的还原分别注明。

## 验证与安装

执行 `assemble` 编译打包、JSON 与包内资源完整性核对及差异检查；遵照项目约定没有执行自动测试、GameTest 或自动游戏验收。实际动画混合、视效、联机表现和难度待用户试玩。

退出游戏并备份存档后，移走旧版 Blacklight JAR，放入本包 `mods/` 中的 JAR。前置：Java 21、NeoForge 21.1.244、Epic Fight 21.17.3.1、GeckoLib 4.9.2。当前运行游戏不会热更新。含原作派生动作的完整包仅供本地私人使用。

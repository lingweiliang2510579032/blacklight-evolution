# RC24：按解包数据补回猎手吸收

本次依据本机 Prototype 1 的 FIG 状态树和 P3D 动画解码结果修正普通猎手吸收。原版按目标类型分流：人形目标有武器分支，普通猎手进入专用共用分支。因此没有给猎手新增六种凭空设计的武器处决。

## 原始数据与实现

| 已核对的数据 | RC24 的处理 |
| --- | --- |
| `prototype_consume` 的 `/consume/grapple/` 武器分支含 `Holding != brawler` 条件 | 保留人形目标各武器动作；猎手始终选择共用猎手动作 |
| `prototype/opening/surface/ground/holding/grabbing/brawler/0xa2dd99e48b1f5b11/variations/consume` 下，猎手 giver 使用 `alex_brawl_consume_2_gv` | 新增独立同步阶段 `HUNTER_CONSUME`，姿态直接采样经过的秒数，不套用通用两 tick 停顿及动画百分比时间扭曲 |
| giver 的 `consume` 与 `grabSlotDetach` 均在 2.5 秒 | 第 50 个服务器 tick 完成结算、释放玩家；剩余上身动作可跟随移动播放 |
| actor 头部元数据为 106 帧、30 fps | 同步动作阶段长度为向上取整的 71 tick；原有 retarget 文件末端填充不再决定猎手结算时点 |
| receiver 为 `alex_brawl_consume_2_rcv`，FIG `endFrame=94` | 释放后继续播放相同 receiver 至 94/30 秒并保持倒地姿态，不立即删除整个实体 |
| receiver 的 `Jaw/SCL` 有 25 个缩放关键帧 | 补充私有数据文件并插值应用到自制猎手下颚；此前 receiver 导出只保留了旋转和根位移 |
| Brawler 的 `brawler_hunter/effects/consume/consume` 中，`effectConsume` 起点为 `Head`，begin=0.666667 秒、reverse=1.5 秒 | 特效起点限制到头部，身体不再执行通用四肢碎裂及全身溶解；1.5–2.5 秒用模组渲染完成头部回流 |

武器分支原始条件为 `GrabSlotClassNameCondition`：Holding 哈希 `0x9139b991c1dd9d0d`，比较值 `2164770`（解析器枚举 `NotEqual`），brawler 哈希 `0x435f19d7b2b9fa71`。这比“当前导出列表只有一组动作”更能确定分支适用范围。

原始 giver 完成/解除轨道的 blob offset 分别为 12332、12372；Brawler `effectConsume` offset 为 65556。其效果结束值是 4.666667 秒，不能把 2.5 秒误称为效果轨道的结束值。2.5 秒来自 giver 的 consume/detach 事件。

人形既有映射保留：无能力 `alex_grap_punch_chest_gv`、利爪 `alex_claw_grap_crush_head_gv`、重锤 `alex_hammer_grap_headdrop_gv`、刀锋 `alex_grap_blade_spin_and_slice_gv`、肌肉强化 `alex_grap_muscle_arm_rip_gv`、鞭拳 `alex_whipfist_grap_lash_gv`。本次没有把这些人形动作套到普通猎手上，也没有宣称已覆盖每种武器在原版中的全部随机变体。

## 适配边界

- 原始效果着色器算法和完整 ragdoll 系统尚未复现。头部几何缩小、回流到玩家、完成后隐藏头部属于依据 Head 起点和事件时序制作的 Minecraft 表现，不是已解出的原版“砍头回调”。下颚轨道应用到自制骨架的局部轴，也属于骨架适配。
- 残躯保留 100 tick（5 秒）后清理是模组策略。残躯健康值为零，不可再次抓取、不攻击、不再掉落战利品或经验；保存其残躯时间，避免重载后恢复成活体。恢复姿态使用同一 receiver 后半段，没有拼接其他死亡动画。
- 源 receiver 还引用 `//hitreaction/common/hunter/ragdoll/enable`；这里仅确认引用存在，不声称重现其全部运行行为。局部 `grabSlotExecute` 的数字引用没有完全解析，不用遍历序号冒充目标节点索引。
- Leader Hunter 有另一套 `alex_grap_leader_consume_*` 分支，本模组当前普通猎手不混用它。Minecraft 巨型生物及 Boss 继续走现有适配，不能据此称它们等同原版猎手。
- 普通人形的收尾时点和通用碎裂表现仍使用原有模组适配。本次恢复的精确 2.5 秒完成事件仅用于普通猎手。

## 数据、构建与安装

`tools/export_hunter_consume.py` 从本地解码后的 `Brawler-alex_brawl_consume_2_rcv.json` 提取遗漏的 Jaw/SCL，写入私有动作目录的 `hunter-consume.data`。源文件及曲线数据不进 Git。既有 202 个动作/数据文件保留，打包额外加入该文件；Gradle 显式校验其存在，避免编译成功却漏装缩放轨道。

只运行 `assemble` 编译打包，并核对包内类、版本及动作文件散列；未运行自动测试、GameTest 或自动游戏验收。当前运行的游戏不会热更新。退出游戏、备份存档、移走旧版 Blacklight JAR、放入 RC24 后重启；多人客户端和服务端均需更新。

依赖仍为 Minecraft 1.21.1、NeoForge 21.1.244、Java 21、Epic Fight 21.17.3.1、GeckoLib 4.9.2。

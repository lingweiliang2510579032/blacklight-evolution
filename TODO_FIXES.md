# 未修复问题备忘(2026-08-19 鸿钧接手后更新)

> 旧记录(2026-08-16)的两个"都没修好"问题,本轮处理状态如下。

## 1. 空中左键扑击(飞扑) —— 已重写,待游戏内确认

- 已按备忘方向改为弹道式:扑击开始捕获目标,每 tick 步长 clamp 到剩余距离
  (`min(speed, distance)`),数学上不可能穿过目标,振荡病根已除。
- 接触包围盒当 tick 结算斩击并原地收势(不再悬停 2.5 格外)。
- 无锁定目标时保持原视角直冲逻辑不变。
- 位置:`PrototypeMovementSystem` dash 分支;3D 追踪(含垂直分量,clamp -0.55~+0.30)。

## 2. 蓄力跳跃下蹲动画 —— 已修复并游戏内确认(2026-08-19)

用户确认"总算对了"。最终方案与备忘建议一致(照搬地刺模式),但实际踩了四层坑才通,
完整教训已写入 `docs/KNOWN_ISSUES_AND_SLOTS_ZH.md` 第 E 节。

最终形态:
- `CHARGED_JUMP` 是 `TunableActionAnimation`(正牌 ActionAnimation),服务端
  `playAnimationSynchronized` + `ClawAnimationPayload` 自确认包双发。
- 客户端按蓄力比例驱动 elapsed,渐入期不冻结,满蓄力才 hardPause 定格;
  时间钳制在 totalTime-1ms 防 isEnd 弹回。
- 服务端 animator 同步驱动+定格;所有蓄力出口(松手/坠台/入水/墙跑/模式关闭)
  显式 unpause 并播 JUMP_FALL/BIPED_JUMP 接管。

## 新增待确认项(本轮已实现,等游戏内验收)

- 落地 3D 方块震起(LandingDebrisSystem,BlockDisplay 抛物线,不落块不掉物)。
- 疾跑体撞:≥0.45 格/tick 触发,伤害+击飞,12 tick 冷却。
- 地刺利爪插地:plant 帧 root -0.56/torso 40°/arm 38°/手腕+30°下扎。
- 滑翔动作:prototype_glide 循环剪辑,FLY living-motion 槽。

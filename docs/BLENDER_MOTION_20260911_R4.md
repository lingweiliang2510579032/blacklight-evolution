# 利爪模型与全部 20 段动作 · Blender 版本

本版已将项目中的全部 20 段动作换成 Blender 制作、烘焙的动画，并接入现有 Epic Fight 动作注册。较大的前臂和掌部、三根主爪与独立拇爪替代了原来的条纹套筒。网格共 2,128 个三角面，左右臂各自绑定到实际肩、肘关节。

这是按《虐杀原形》一代动作特点进行的重建，不是提取原版模型或原版动画数据，也未达到逐帧 1:1 复刻。Minecraft 的身体比例、关节结构和当前模组技能机制仍然构成差异。吞噬和变身尤其需要结合目标、粒子与实际交互继续评估，不能仅凭角色独立回放判断完整效果。

## 查看和使用

- `claw-study.blend`：独立可编辑工程。选中 `Blacklight_EpicFight` 骨架，在 Action Editor 中直接选择 20 段动作；无需安装导出插件即可播放、改骨骼和网格。
- `claw-study-authoring.blend`：保留 IK 控制器、原始关键帧和曲线的制作工程。每段骨架 Action 对应同名控制器 Actions，需要一起切换。
- `all-motions-normal.mp4`、`all-motions-slow.mp4`：全部 20 段的 Blender 正常速度、慢速回放。
- `motions/`：20 个独立视频；`exports/`：对应游戏动画 JSON。
- `game-all-motions.mp4`：游戏中固定镜头的逐帧姿势回放，用来核对实际骨架与模型显示；不是完整战斗过程录像。
- `blacklight-0.2.0-mvp.5-blender-r4.jar`：NeoForge 1.21.1 测试包，继续使用项目的 Epic Fight、GeckoLib 依赖。替换旧版 Blacklight JAR 使用，勿同时放入两份。

## 动作覆盖

| 类别 | 已重做 |
|---|---|
| 基础 | 待机、行走、奔跑 |
| 战斗 | 三连击、奔跑攻击、冲刺切割、地刺 |
| 变异 | 变身、吞噬 |
| 跳跃与空中 | 蓄力跳跃、起跳、下落、空中冲刺、滑翔 |
| 墙面与落地 | 爬墙、轻落地、重落地、猛烈落地 |

## 验证

- Blender 5.2.1 已实际执行建模、IK 求解、动画烘焙、渲染与文件保存。
- 全部 20 段导出时长与现有游戏动作契约一致；保留服务器伤害与移动逻辑。
- `gradlew test build --offline` 通过，72 项测试通过；资源测试覆盖全部 20 段骨架、时间与单位四元数，并通过完整骨架正向运动学检查地面动作脚部位置。
- Epic Fight 的加载器取反 XYZ 与 OpenMatrix 旋转约定会相互抵消；导出保留 Blender WXYZ，避免额外反转。已将游戏实际骨架与 Blender 同时间采样的 23 个姿势进行坐标核对，覆盖 20 段动作，手腕误差小于 0.001 格。
- 站立连击使用完整身体动作；走跑期间保留步伐，叠加上身攻击。
- 网格已检查有限坐标、朝外表面与四个关节分组。测试截图代码不包含在交付 JAR 中。
- 游戏回放包含完整 20 段、494 帧；在复制的 `Blacklight Motion QA r3` 测试存档执行。只验证姿势回放，不宣称已经完成所有实战碰撞、手感和性能场景的验证。

JAR SHA-256：`39cbcd913bf599170b927e41d4e0d4d9a9245be35a71d75169d21a4be7880963`

## 参考与制作来源

- [Dejan Ostojic 一代概念设计](https://d-conceptart.blogspot.com/2012/07/prototype.html)：形体与发力方向参考，属于早期设定，不等同于最终实机。
- [利爪与地刺演示](https://www.youtube.com/watch?v=qnbyGiuyh1k)、[利爪战斗片段](https://www.youtube.com/watch?v=k57rsOsSojo)、[墙面跳跃片段](https://www.youtube.com/watch?v=pOc_N7FnwNA)、[跳跃与冲刺片段](https://www.youtube.com/watch?v=iNVj0zWhK6s)：姿势和动作阶段参考；未据此声称每段动作都已逐帧测量。
- [Epic Fight Blender 工作流](https://epicfight-docs.readthedocs.io/Guides/page2/)；使用其链接的 [Blender 导入/导出工具](https://github.com/Antikythera-Studios/blender-json-addon)，版本提交 `b9c6844193074f8c21b35513052d61c82cc2c207`。工具仅在制作脚本进程中加载，未全局安装其更新服务。
- 灰色比例参考人体和骨架来自项目已有 Epic Fight 依赖。新爪网格由本项目制作；工程中的灰色人体只作比例参考。

制作脚本位于原项目 `tools/`：`build_claw_blender.py`、`bake_claw_playback.py`、`render_claw_blender.py`。制作时先建立带 IK 的工程，再保存可独立切换的烘焙工程。

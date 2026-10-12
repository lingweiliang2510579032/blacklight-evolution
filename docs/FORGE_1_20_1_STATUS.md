# Forge 1.20.1 维护与验收状态

更新时间：2026-10-12。状态：Forge 源码移植候选已构建，完整试玩被动作资料缺失阻塞。

本分支独立维护 Minecraft 1.20.1 Forge 版本。原作者末者继续维护上游 1.21.1 NeoForge。移植基于上游提交 `d43aa4bf4263ad5ebf1de799cee06f48a0f8c018`，保留 GPL、作者署名及第三方资源声明。

## 目标环境

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Java | 17 |
| Forge | 47.4.10 |
| GeckoLib | 4.8.4，Forge 1.20.1 版 |
| ParCool | 1.20.1-3.4.3.3，可选 |
| Gradle | 8.9 |
| ForgeGradle | 6.0.54 |
| MixinExtras | Forge 0.4.1，内嵌 |

## 验证状态

| 验证 | 实际结果 |
| --- | --- |
| Java 17 主源码、Mixin 注解处理 | 通过 |
| 独立运行依赖边界检查 | 通过；没有 Epic Fight / Weapons of Miracles 字节码依赖 |
| `jar` / `jarJar` 与两种 Forge 重混淆 | 通过 |
| `assemble` 分发压缩包 | 通过 |
| 独立动作解析工具编译 | 通过 |
| 可选 GameTest 与视觉探针 | 全部 23 个探针通过 Forge / Java 17 编译；未运行游戏场景 |
| 新增 Forge 单元测试 | 9/9 通过：状态持久化、死亡/非死亡复制、可变 NBT 深复制、capability 生命周期、网络往返及输入边界 |
| 完整单元测试 | 192 项：180 通过、1 跳过、11 失败；细分如下 |
| 独立开发客户端，Java 17 | 实际进入模组加载，因 `Missing Blacklight motion: basic_idle` 停止 |
| 专用服务端 | 已尝试启动，仅到 EULA 检查；未接受协议、未完成模组/玩法验收 |
| 多人、生存、动作手感与最终可玩验收 | 未完成 |

启动日志未出现 Mixin 注入错误，只能说明已加载类的启动过程；不能替代伤害、吸收、吞噬冻结和渲染等玩法场景验证。没有改动用户正在运行的整合包实例。

## 完整测试的 11 项失败

- 7 项需要缺失的动作资料：`NativeLandingPoseTest` 2 项、`WeaponMotionClockTest` 3 项、`FirstPersonPoseTest` 2 项；包括 `basic_land_moving`、`basic_sprint`、`utility_armor_transform` 等。
- 4 项上游旧断言与当前实现不同：移动配置预设 1 项、移动升级 2 项、警戒/伪装 1 项。移动升级与警戒状态源码未改动；配置预设与编译默认值也保留原数值。未为了通过测试修改这些玩法规则。
- 观察到的差异：跳跃预设 `1.2407766990291262` 对编译默认 `0.55`；未升级冲刺倍率 `1.08` 对旧断言 `1.0`；一级跳跃倍率 `1.1` 对旧断言 `1.2`；警戒测试仍期待立即解除伪装。

因此不能宣称完整测试通过。Windows 中文路径下 Java 17 的测试参数编码已用 URI manifest JAR 处理；capability 测试复用 Forge 本身的 `CapabilityTokenSubclass` 转换器，未替换为假的存档或 capability 实现。

## 本地候选

版本 `0.3.0-forge-1.20.1-dev.1`，配置 `source-only`。本地收集器验证 Java class 版本不高于 61、Forge 元数据与原作者署名、GPL/第三方声明、重混淆输出一致、Mixin 与 refmap、语言资源以及分发包一致。候选不含私有动作资料，`publicReleaseReady=false`。

维护分支为 `forge-1.20.1`；未推送移植改动或创建公开 Release。源码、原资源声明与独立维护边界保留；不向上游提交整套移植 PR。

## 完整动作资料

公开仓库缺少完整的动作、HUD 和镜头资料。保留上游 source-only / local-preview 打包边界，以及缺失动作时的明确失败行为。源码构建成功不能作为可玩成品的证明。原 README 中的启动及人工验收记录来自 1.21.1，不作为 Forge 1.20.1 的证据。

适配依据见 [移植研究](FORGE_1_20_1_RESEARCH.md)。

# Minecraft 1.20.1 Forge 移植调查

> 历史调查：下文描述移植基线 dev.5。上游 9ba910f/dev.7 已公开并补齐224资料；本分支 dev.2 默认 bundled、显式外部资料 external-profile，原 source-only/local-preview 与缺资料阻塞已关闭。当前目标 Minecraft 1.20.1 Forge，实际验证见 [维护状态](FORGE_1_20_1_STATUS.md)。

调查日期：2026-10-12。源码基线：上游 `main` 提交 `d43aa4bf4263ad5ebf1de799cee06f48a0f8c018`，版本 `0.3.0-standalone-dev.5`。本文是移植前调查，不代表 Forge 版已经编译、启动或通过试玩。用户已确定：我们独立维护 1.20.1 Forge，原作者继续维护 1.21.1 NeoForge，项目范围只包括这个模组，不向上游提交完整移植 PR。

## 可执行结论

1. 在 fork 的 `forge-1.20.1` 分支独立维护，保留上游提交血缘、作者和许可。先完成平台适配，再验证玩法；不能仅替换版本号和包名。
2. 主要工作集中在状态持久化/同步、网络、伤害阶段、客户端渲染与 Mixin。建议保留现有动作/玩法代码，在 Forge 边界重建这些平台服务。
3. 公开仓库确实缺少完整动作资料。原版普通构建是 `source-only`，初始化会检查全部动作；构建成功不能直接当作可玩包。按用户要求先基于公开源码推进，试玩缺口需单独解决。
4. 保留 `source-only` / `local-preview` 区分、第三方声明和私有资源排除规则。原作者的源码移植许可并不改变仓库对商业游戏派生素材的分发声明。

以上建议为**非原文：根据以下代码与官方接口归纳**。[上游 README](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/README.md)、[上游构建文件](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/build.gradle)、[资源声明](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/THIRD_PARTY_ASSETS.md)。

## 范围与依赖

该提交共有 372 个 Java 文件：`src/main/java` 300；`src/test/java` 48；独立版服务端/视觉测试 4；墙面测试 18；蜂巢测试 1；`tools` 探针 1。主源码中，77 个文件引用 NeoForge，92 个文件使用 `getData` / `setData` / `syncData` / `hasData`，28 个文件使用 StreamCodec 系列，90 个文件使用 `Math.clamp`。这些集合有重叠，不能相加成为改动总量。统计来自基线源码文件清单及文本检索。[基线源码树](https://github.com/mengyuyuan/blacklight-evolution/tree/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src)。

当前依赖为 Java 21、Minecraft 1.21.1、NeoForge 21.1.244、GeckoLib 4.9.2；ParCool 3.4.3.3 仅为可选兼容。Epic Fight / Epic ParCool / Weapons of Miracles 已从独立版运行依赖中移除，不能按历史文档重新引入。[版本配置](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/gradle.properties)、[第三方声明](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/THIRD_PARTY_NOTICES.md)。

Forge 1.20.1 官方开发要求为 Java 17。应采用对应版本的 ForgeGradle/MDK，重新配置映射、运行目录、资源处理、Mixin 和 reobfuscation，而非保留 NeoForge ModDevGradle 的任务假设。官方仓库 `1.20.x` 分支已包含较新游戏版本配置，调查时需使用明确的 `1.20.1` 分支/固定提交。[Forge 1.20.1 开发说明](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/)、[Forge 1.20.1 MDK](https://github.com/MinecraftForge/MinecraftForge/blob/0ec923d7307eb15bef70d6916329642b733a21ed/mdk/build.gradle)。

GeckoLib 的 Forge 坐标为 `software.bernie.geckolib:geckolib-forge-1.20.1:<版本>`，ForgeGradle 使用 `fg.deobf(...)`；其官方 1.20.1 分支在调查时声明 4.8.4。实际锁定版本仍须经过 Maven 解析、编译和运行验证，不能直接使用 NeoForge 1.21.1 的 4.9.2 JAR。[GeckoLib 安装文档](https://github.com/bernie-g/geckolib/wiki/Installation-(Geckolib4))、[GeckoLib 1.20.1 版本配置](https://github.com/bernie-g/geckolib/blob/1.20.1/gradle.properties)。

## 主要 API 风险及适配方向

| 层面 | 基线事实与风险 | Forge 1.20.1 方向及验收重点 |
| --- | --- | --- |
| Java | 大量 `Math.clamp`；部分 List 使用 `getFirst/getLast/removeFirst` | 降为 Java 17 可用实现。逐项核实容器类型，Deque 上同名方法不需要删除；检查空容器行为、数值边界和 NaN 语义。 |
| 入口/元数据 | 构造器注入 `IEventBus, ModContainer`；`neoforge.mods.toml` 使用 `type="required"`、`[[mixins]]` | Forge 入口取得 `FMLJavaModLoadingContext`；改为 `META-INF/mods.toml` 与 `mandatory=true/false`，按 Forge 机制注册 Mixin；依赖范围固定为 1.20.1/Forge 47 系列。 |
| 注册 | `DeferredHolder`、`DeferredItem`、`DeferredBlock`、`DeferredRegister.Items` 等 NeoForge便利接口 | 转为 Forge `DeferredRegister` / `RegistryObject`；保留已有注册 ID，检查生成实体、属性、药效、世界生成注册时机。 |
| Entity 状态 | 20 个 attachment，有序列化、自动同步和死亡复制语义，92 个使用文件 | 用 Forge capabilities 或明确的实体状态服务实现相同语义。登录、复活、换维度、开始跟踪必须补同步；临时动作锁不能误持久化。 |
| 网络 | `RegisterPayloadHandlersEvent` 注册 17 条消息；payload依赖 `CustomPacketPayload` 与 StreamCodec | Forge `SimpleChannel` + `FriendlyByteBuf`。固定消息 ID/方向/协议版本；保留服务器对身份、动作、目标、距离与升级的校验，主线程执行。 |
| Tick | `PlayerTickEvent.Pre/Post`、`ClientTickEvent`、`EntityTickEvent.Pre/Post`；部分 Pre 会取消整个实体 tick | 玩家/客户端改为 `TickEvent` 的 START/END；非玩家实体须核对可替代的 LivingTick/Mixin。仅跳过自己的处理不能代替取消猎物 AI/重力更新。 |
| 伤害 | Incoming、Damage.Pre、Damage.Post 三种阶段；生命单位五倍换算 | 按 Forge 实际 damage pipeline选择钩子。护甲减伤、护盾容量、吸收生命和实际伤害通知分别回归；不能因事件名字相似就直接映射。 |
| 原版属性/药效 | ResourceLocation 属性修饰符、Holder属性、`ADD_VALUE/ADD_MULTIPLIED_TOTAL`；药效 tick 返回 boolean | 回移 UUID/原版 1.20.1 操作枚举、Attribute/MobEffect 类型及方法签名；特别检查五倍生命与吸收生命上限机制。 |
| 渲染 | `addVertex/setColor/setUv/setOverlay/setLight/setNormal`，`Tesselator.begin/buildOrThrow`，整型颜色渲染参数 | 回移 1.20.1 顶点管线及 RGBA 参数，核对每个 VertexConsumer 包装器，防止模型能编译但颜色/法线/截面失效。 |
| GUI/镜头 | `RenderGuiLayerEvent` / `VanillaGuiLayers`、RenderFrame/DeltaTracker、PostChain | 使用 Forge GUI overlay/RenderTick接口，适配插值和后处理生命周期；验证关闭效果后的状态恢复及资源重载。 |
| Mixin | 10 个公共 + 17 个客户端 Mixin，配置 `JAVA_21`、`required=true`、`defaultRequire=1` | 全部核对 1.20.1 方法/字段/描述符；配置 Java 17 和 refmap。注入失败可能直到实际启动才暴露。 |
| 数据资源 | `pack_format=34`，1.21.1 数据文件和结构 NBT | 回移资源/数据目录和 schema；验证 recipes/loot/tags/worldgen实际加载、研究站可定位及存档读写。 |

表内适配方案为**非原文：工程推断**。基线事实来源：[入口](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/BlacklightNeoMod.java)、[注册](https://github.com/mengyuyuan/blacklight-evolution/tree/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/registry)、[网络](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/network/ModNetwork.java)、[伤害换算](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/power/CombatScale.java)、[渲染入口](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/client/renderer/AnatomicalGeoRenderer.java)、[Mixin配置](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/resources/blacklight.mixins.json)、[资源版本](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/resources/pack.mcmeta)。

目标平台依据：[Forge capabilities](https://docs.minecraftforge.net/en/1.20.1/datastorage/capabilities/)、[Forge SimpleImpl](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/)、[Forge registries](https://docs.minecraftforge.net/en/1.20.1/concepts/registries/)、[Forge mod files](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/modfiles/)、[Forge TickEvent 源码](https://github.com/MinecraftForge/MinecraftForge/blob/0ec923d7307eb15bef70d6916329642b733a21ed/src/main/java/net/minecraftforge/event/TickEvent.java)、[GeckoLib 1.20.1 GeoRenderer](https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/renderer/GeoRenderer.java)。

### 状态与网络不能只做编译替换

`PLAYER_STATE`、`GROWTH`、`DISGUISE_IDENTITY` 使用 `copyOnDeath()`；`INFECTION_START` 和 `UTILITY_STATE` 持久化但未声明死亡复制；`MOVEMENT_STATE` 与 `CLAW_COMBAT` 为临时对象。适配应以这些原有标记为准，不能统一把 20 个状态全部保存/复制。`PLAYER_ACTION` 的注释还要求新观察者开始跟踪时收到动作，以免其他玩家看到错误姿态。[ModAttachments](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/registry/ModAttachments.java)。

Forge capabilities 默认不自动同步，也不自动保留死亡状态；需在 Clone 和跟踪生命周期明确实现。**非原文：建议**先用本地状态访问接口集中替换直接 attachment 调用，再用同一套包编码覆盖临时状态和初始全量快照。[Forge capabilities](https://docs.minecraftforge.net/en/1.20.1/datastorage/capabilities/)。

五倍生命转换原意位于护甲/抗性处理之后、吸收之前；Forge最终 `LivingDamageEvent` 的阶段不可未经核对就视作等价。护盾以显示生命计量，溢出先还原单位再交回管线。**非原文：建议**把无甲、有甲、抗性、吸收、护盾破裂溢出列为单独数值验收项。[CombatScale](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/power/CombatScale.java)、[UtilityPowerSystem](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/power/UtilityPowerSystem.java)。

## 公开资源缺口与能否运行

本地核查该提交的 `src/main/resources`，以下文件均无同名资源；它们不是“克隆遗漏”，与上游明确的私有资料排除政策相符。[资源树](https://github.com/mengyuyuan/blacklight-evolution/tree/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/resources)、[私有资源声明](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/THIRD_PARTY_ASSETS.md)。

构建器完整性检查要求的 15 个 JSON：

```text
morph_claws.json                 morph_blade.json
morph_hammerfists.json           morph_muscle_mass.json
morph_whipfist.json              morph_thermal_vision.json
utility_shield_deploy.json       utility_shield_hit.json
utility_shield_run.json          utility_shield_sprint.json
utility_armor_transform.json     utility_armor_idle.json
basic_idle.json                  basic_wall_top.json
basic_parkour_small.json
```

检查要求的 10 个 `.data`：

```text
health-hud.data                  camera-film.data
combat-motion-tuning.data        consume-placement.data
receiver-poses.data              hunter-consume.data
hammer-scales.data               armor-growth.data
camera-profile.data             camera-events.data
```

另有 `consume-receiver-events.data` 被资源 staging 规则接收，但未列入上述构建前检查。staging 也接收动作目录内全部 `*.json`，所以这个清单只是构建器最低检查集合，不是完整动作包清单。[stageBundledMotions](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/build.gradle)。

运行时 `MotionLibrary.requireInstalled()` 遍历全部 `PlayerAction`、`WeaponMove`及明确的附加动作；`clip()` 缺数据就抛出 `Missing Blacklight motion: <id>`。模组公共初始化会直接调用该检查。公开目录包含少量原创动作（例如 `claw_pounce.json`、`claw_pounce_charge.json`）和既有公开资源，但不足以通过完整安装检查。[MotionLibrary](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/animation/MotionLibrary.java)、[原创动作目录](https://github.com/mengyuyuan/blacklight-evolution/tree/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/resources/assets/blacklight/authored_motions)、[初始化调用](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/BlacklightNeoMod.java)。

资源读取支持系统属性 `blacklight.localWeaponMotions` 指定外部目录，外部目录整体替代 bundled profile，禁止逐文件混用不同 profile。构建可用 `-PbundleMotionDirectory=<目录>` 显式生成 `local-preview`。**非原文：判断**没有外部完整资料时，可推进编译移植；完整动作试玩须补获许可的资料或完成原创替代动作。移除检查、吞掉异常或统一静止姿态不能据此宣称玩法移植完成。[MotionResources](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/combat/MotionResources.java)、[LocalWeaponMotion](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/src/main/java/com/codex/blacklight/combat/LocalWeaponMotion.java)。

## 许可和包边界

- 原创代码、文档、模型和原创动画声明为 GPL-3.0-only；保留 `LICENSE`、`COPYRIGHT`、原作者与贡献者记录。[COPYRIGHT](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/COPYRIGHT)、[LICENSE](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/LICENSE)。
- Kenney 粒子三张贴图为 CC0；AnimeSpeedLines 的设计衍生代码保留 MIT 声明；GeckoLib MIT、ParCool LGPL-3.0均为单独安装依赖，不把其 JAR随意合并进模组。[资源声明](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/THIRD_PARTY_ASSETS.md)、[第三方 notices](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/THIRD_PARTY_NOTICES.md)。
- 仓库声明私有 Prototype 动作、镜头、HUD和受害者编舞资料没有取得公开分发授权；源码 GPL 不覆盖这些资料。历史本地包资源计数也不能当作当前公开包清单。[资源声明](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/THIRD_PARTY_ASSETS.md)。
- 移植应保留 source-only 排除 `blacklight/bundled_motions/**`、产物分类、JAR内第三方 notices 和“尚非公开发行成品”标记。完成平台适配不会自动消除上游资源与验收阻塞。[构建边界](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/build.gradle)。

## 独立维护策略与验证顺序

按用户已明确的分工，fork 的 `forge-1.20.1` 是独立维护入口。保留上游 main参考分支及 `upstream` remote；以后按需要审查/cherry-pick通用修复，再进行平台适配，不直接把整个 NeoForge main合并进 Forge分支。README、issue链接、构建产物与版本号应明确 Forge 1.20.1身份，同时保留原作者出处。以上为**非原文：维护建议**。

完整降版不向上游提交 PR；将来如果发现纯算法/动作数据读取等可共享修复，可以独立形成小改动并先讨论。原作者贡献说明要求大型改动先讨论设计分工，PR还必须交代验证缺口与素材来源。GitHub的跨 fork PR本身是往选定上游 base分支合并差异，并不是单纯登记一个独立维护分支。[CONTRIBUTING](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/CONTRIBUTING.md)、[GitHub fork PR说明](https://docs.github.com/en/pull-requests/how-tos/create-pull-requests/creating-a-pull-request-from-a-fork)。

**非原文：建议验证顺序**：

1. 锁定 Java 17、Forge、GeckoLib依赖，编译主源码与 source-only包。
2. 检查成品元数据、Mixin/refmap、私有目录排除及 notices；测试 reobfuscated JAR，不能仅依赖开发环境。
3. 验证状态持久化、死亡复制、初始跟踪同步与消息编码；保留上游测试目录及可选 GameTest设置，不把历史测试通过记录搬作当前结果。
4. 在完整资源或原创替代动作具备后，启动客户端及专用服务端，确认 Mixin实际注入和资源加载。
5. 试玩觉醒、模式切换、全部武器、抓取/吸收、伪装、护盾/装甲、墙跑/攀爬/滑翔及重登录/换维度/死亡；多人特别验证旁观者动作与受害者配对。

## 实施补记：共同端适配与待验收项

以下为 **非原文：2026-10-12 本地移植记录**，不是原作者的验证报告。目标依赖已解析为 Forge `47.4.10`、GeckoLib `4.8.4`。检查依据包括本地映射后的 Forge JAR 字节码；仍须完成全量编译和实际运行。

- 注册对象已改为 `RegistryObject`；实体同步数据改为旧版 `defineSynchedData()`；属性修饰符以 `namespace:path` 的 UTF-8 字节生成稳定 UUID，避免重进世界后叠加重复修饰符。`STEP_HEIGHT_ADDITION` 是原版 0.6 之上的加值，因此原总高度 1 / 2 对应新增 0.4 / 1.4。
- 玩家和服务器 Tick 按 Forge 的 START / END 映射。服务器非玩家 tick 与乘客 tick 用 `ServerEntityTickMixin` 提供可取消的 Pre / Post 桥接；捕获冻结需要实际跳过实体更新。运行时应核对乘客、被抓取猎物与冻结解除。
- `LivingEntity.actuallyHurt` 和 `Player.actuallyHurt` 具有各自实现，必须分别注入。Forge 字节码第三次写回伤害参数发生在护甲和魔法减伤后、吸收生命扣除前；此处执行五倍伤害换算，不能改用吸收生命已经扣除后的 `LivingDamageEvent`。`setHealth` 后提供实际正生命损失通知。待验证 Mixin 命中、护盾溢出、吸收生命、致死伤害及取消事件顺序。
- 1.20.1 的效果类名为 `AbsoptionMobEffect`，且无 `MAX_ABSORPTION` 属性。开始效果后保证五倍吸收生命，移除效果时同步换算扣回数量；否则吸收 I 到期仅扣 4，会留下 16 点。应测试效果升级、刷新、受伤及到期。
- GeckoLib 旧版 `GeoAnimatable`、实例缓存和动画控制器在 `core` 子包，`GeoEntity` / `GeoItem` 仍在原 `animatable` 包。样本物品保留右键投掷与发射器行为，采用旧版物品和发射器接口。
- 1.21 新增原版粒子/锤声在目标游戏内不存在：共同端把 `DUST_PLUME` / `SMALL_GUST` / `GUST_EMITTER_LARGE` 分别映射为 `POOF` / `CLOUD` / `EXPLOSION_EMITTER`，重/轻锤击声采用 `GENERIC_EXPLODE` / `IRON_GOLEM_ATTACK`。这是版本兼容替代，视觉和音色存在差异，需要试玩判断效果。
- 1.20.1 马的命令物品槽为 400 鞍、401 马铠、500 起仓储；感染转换按旧版槽协议释放物品，直到 `SlotAccess.NULL`，最后移除箱子。不可直接沿用 1.21 库存偏移，否则会漏掉马铠或错位。
- 上述代码适配不补齐缺失动作资料，也不代表完整动作试玩、专用服务端或多人同步已通过。公开资源缺口继续遵循前文清单。
6. 最后验证自然研究站开局、感染体/巨巢及持续成长。未完成项在维护文档标注，不以编译完成替代可玩验收。

当前报告未执行上述构建或试玩；实际结果应由移植记录另行记载。上游默认 `assemble` 不运行单测、碰撞测试或 GameTest，历史成功数据也不是当前 Forge版测试结果。[上游构建与贡献规则](https://github.com/mengyuyuan/blacklight-evolution/blob/d43aa4bf4263ad5ebf1de799cee06f48a0f8c018/CONTRIBUTING.md)。

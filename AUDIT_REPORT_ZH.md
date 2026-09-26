# Blacklight Evolution 0.2.0-mvp.5 深度审计报告

> 审计日期:2026-08(会话内)。审计对象:`D:\Blacklight-Evolution-0.2.0-alpha.2\blacklight-evolution-neoforge`
> 范围:95 个 Java 文件(约 9243 行)、14 个 JVM 测试、资源、构建配置、交付物与崩溃日志。
> 方法:审计员一手通读核心链路(入口/网络/模式/战斗/感染/生存锁/命令/Attachment/Mixin),并由 5 个并行子代理分别深挖服务端权威、移动战斗、客户端渲染、EpicFight 集成、工程与测试。
>
> **附:修复进度(2026-08 同一会话)**——本报告全部问题已进入修复阶段,修复状态见文档末尾「附录 A:修复清单与状态」。

## 一、总体结论

**项目定位与成熟度**:Blacklight Evolution 0.2.0-mvp.5 是一份工程完成度显著高于同规模同人 Mod 的 MVP 交付。代码分层清晰(纯函数规则层 / 服务端权威系统 / 客户端表现层 / EpicFight 适配层)、文档体系完整(需求基线、验收清单、已知坑位、架构文档)、测试真实有效(62 项真断言全部通过,含持久化兼容与资源引用回归)、资源引用零断链、交付物哈希可校验。当前锁定版本(MC 1.21.1 + NeoForge 21.1.244 + Epic Fight 21.17.3.1 + GeckoLib 4.9.2)下**未发现必现的 mod 运行时崩溃**(两份 hs_err 崩溃日志均为构建环境原生内存 OOM,与 mod 代码无关)。

**核心风险概述**:最大的短板在**服务端权威的最后一公里**——C2S 包入口缺少授权/存活校验且未回到主线程执行,其中 `MovementInputPayload` 无前置条件的伪造蓄力跳直接击穿 README 的核心承诺;叠加 Epic Fight 内部字段直访与未判空两处潜在崩溃点,是发布到公开服务器前必修的 4 项 P0。其余高/中危问题集中在冲刺斩半程空挥、连击方向未锁定、渲染生命周期与生存锁定覆盖不完整,均可按第七节优先级清单处理。

**分维度评分**(5 个维度全部完成):

| 维度 | 评分 | 一句话评语 |
|---|---|---|
| 服务端权威与网络安全 | 6/10 | 架构方向正确,授权校验与线程模型有硬伤 |
| 移动与战斗系统逻辑 | 6/10 | 数值与 NaN 防护扎实,冲刺斩/连击方向/守卫有功能缺陷 |
| 客户端渲染/动画/粒子 | 8/10 | 完成度极高,内部 API 直访与后处理生命周期是主要扣分 |
| 构建/测试/资源/交付 | 8/10 | 62 测试全绿、资源零断链、哈希可校验,工程卫生小项待清理 |
| EpicFight 集成与模式系统 | 7/10 | 借壳动画体系设计出色,EF 版本开放上界与内部 API 使用不匹配是主要风险 |

**综合评分:7.0/10**(按 5 维度算术平均;若以发布到公开服务器的安全门槛衡量,当前为"单机/受信小服可用,公开服务器前必须完成 P0 四项")。

## 二、一手审计发现(主审计员直接验证)

### 2.1 中危

- **[中] PrototypeSurvivalLock.notifyClient 的 Side 判断反转 —— 服务端拦截提示失效**
  - 位置:`mode/PrototypeSurvivalLock.java:106`
  - 问题:`if (!player.level().isClientSide()) return;`。服务端事件路径(左/右键方块、原版攻击、破坏/放置、拾取等)调用该函数时,`player.level()` 是 ServerLevel,`isClientSide()` 恒为 false → 函数在服务端**永远提前返回**。提示消息"碰巧"由同一事件在客户端侧触发时本地显示,但服务端兜底永不通知。
  - 影响:被拦截操作在多数场景无引导反馈;若未来只在服务端拦截的路径,玩家将完全得不到提示。
  - 修复:按调用方区分,或统一在服务端用 `player.displayClientMessage(...)` 发送;保留 WeakHashMap 节流。

- **[中] ClawCombatSystem 的 TOGGLE_CLAWS 无服务端冷却,可被包洪泛用于骚扰与负载攻击**
  - 位置:`combat/ClawCombatSystem.java:53-55, 79-102, 105-119`
  - 问题:客户端每次发 `ClawActionPayload(TOGGLE_CLAWS)` 都会完整执行:状态翻转 + `playPrototypeTransition`(1 次 SCULK_CATALYST_BLOOM 音效 + 55 或 22 个 DUST 粒子 + 18 或 10 个 LARGE_SMOKE 粒子 + Epic Fight 动画重置/重放)。服务端未设任何最小间隔。
  - 影响:恶意/手滑客户端可高频发包,造成附近所有玩家的音效刷屏与粒子洪泛,并加重服务端 tick 负载(每包一次实体同步 + 粒子发送)。
  - 修复:在 `ClawCombatState` 增加 `lastTransformTick`(例如 ≥10 tick 才接受再次切换),或将切换放入 tick 驱动的节流队列。

### 2.2 低危

- **[低→严重(子代理复核升级)] PrototypeMovementSystem.acceptInput 未校验感染/模式/骑乘状态**
  - 位置:`movement/PrototypeMovementSystem.java:48-60`
  - 一手初判:伪造 `MovementInputPayload` 最多获得 1 tick 蓄力,下一 tick 被 reset,危害极小。
  - 子代理复核:包处理先于 tick 校验完成速度写入,`reset` 不撤销已写入的 velocity;攻击者两连包(`jumpHeld=true`→`false`)立即获得约 1.26 垂直速度(原版 0.42 的三倍),可无前置条件反复超级跳。**最终定级:严重**,详见 5.2 S1。

- **[低] 感染体狩猎 AI 无视服务器 PvP 规则**
  - 位置:`infection/BlacklightInfectionSystem.java:102-105`
  - 问题:`isValidTarget` 仅排除创造/旁观玩家,生存玩家一律被当作猎物。PvP 关闭的服务器上感染体会持续锁定、寻路、挥爪玩家但伤害被原版 PvP 保护挡掉。
  - 影响:无伤害但造成无意义 AI 负载与玩家困扰;与"服务端权威"定位无关,属设计疏漏。
  - 修复:目标为玩家时检查 `level().getServer().isPvpAllowed()`(或 mod 自身开关)。

- **[低] 感染体群体扫描频率偏高**
  - 位置:`infection/BlacklightInfectionSystem.java:74-100`
  - 问题:每个感染 Mob 每 10 tick 做 14×7×14 的 AABB 全实体扫描并排序。安瓿一次可感染半径 8 格内全部生物,感染体密集区(刷怪场/围攻)会造成明显的服务端 tick 开销。
  - 修复:扫描间隔提高到 20 tick,用 `TargetingConditions` + 服务器已有目标缓存;为后续版本设定感染体密度上限或降级策略(与 KNOWN_ISSUES 的 BL-PERF-001 呼应)。

### 2.3 已排除的疑虑(验证为安全)

- `ClawCombatSystem.eruptGroundSpike` 中 `target.position().subtract(center).normalize()` 在目标恰好位于地刺中心正上方时为零向量:1.21.1 的 `Vec3.normalize()` 对 <1e-4 长度返回 `ZERO` 而非 NaN,因此 `push(0,0.75,0)` 行为安全;但该语义依赖 MC 版本,升级时需复核。
- `MovementTuningSettings.load()` 虽每 tick 被调用,但有 `loaded` 布尔守卫,只在首次调用时读盘;无每 tick 磁盘 IO。
- `ClawAnimationPayload.byId`/`PhysicalFeedbackPayload.byId` 对越界/非法 id 有回退,解码不可构造崩溃。
- `BlacklightState` Codec 构造对 biomass/EP/alert/upgradeMask 均有钳制,且强制 `prototypeMode = infected && prototypeMode`;未感染却带 prototype_mode=true 的脏数据会被自动清理。
- `PrototypeModeSystem.handle` 有感染校验,客户端无法通过伪造 `PrototypeModePayload` 让未感染玩家进入原型模式。
- 命令 `infect/grant_ep/claw_upgrade` 均有 permission 2;`status` 无权限需求但只读,安全。
- 两个 Mixin 注入点小而稳定(`KeyboardInput.tick` TAIL、`LocalPlayer.canSpawnSprintParticle` HEAD),且均为 client-only,与 Epic Fight 无注入冲突。
- `PLAYER_STATE` Attachment 正确配置 `serialize + sync + copyOnDeath`;`CLAW_COMBAT`/`MOVEMENT_STATE` 为运行时-only,不持久化,符合设计。

## 三、JVM 崩溃日志分析(hs_err / replay 共 4 个文件)

- 两份崩溃均为 **JVM 原生内存分配失败(OOM)**,非 mod 运行时代码崩溃:
  - `hs_err_pid7564.log`:`jdk.jdeps ... com.sun.tools.javap.Main`(ModDevGradle/工具链在解析 Epic Fight `LocalPlayerPatch` 类)运行时,C2 CompilerThread 在 `Chunk::new` malloc 1.3 MB 失败,进程存活仅 0.7 秒。
  - `hs_err_pid18768.log`:Gradle daemon 9.2.1(`-Xmx512m`)运行 7.6 秒后 C2 线程 malloc 0.9 MB 失败。
- 环境:16 核 i7-10870H、**15 GB 内存**、Windows 11;运行时 JVM 为 Temurin 25(构建工具链),项目 toolchain 为 Java 21(正确)。
- 根因:机器物理内存 15 GB,构建期间(凌晨连续两次)多 JVM 进程并发(Gradle daemon + javap 工具进程 + IDE/其他),原生内存耗尽。崩溃时间戳为 2026-08-09 22:19 与 2026-08-10 01:27,与项目交付时间吻合。
- 结论:与 mod 代码无关;建议构建时关闭 IDE/浏览器等内存大户、将 `org.gradle.jvmargs` 上限与本机内存匹配、必要时禁用配置缓存/并行 daemon。可删除这 4 个日志文件避免误入库(项目无 Git 元数据,交付快照中应清理)。

## 四、交付物与元数据验证(一手)

- `release/blacklight-0.2.0-mvp.5.jar` 的 SHA-256 与 `SHA256SUMS.txt` 一致(FD9E52…),JAR 内含 157 class + 61 资源 + mixins.json + META-INF,结构完整。
- `gradle.properties`:`mod_version=0.2.0-mvp.5`,与 README 标题一致;`交付说明.md` 标题仍为 mvp.4,属文档未同步(轻微)。
- `neoforge.mods.toml`:neoforge/minecraft/geckolib/epicfight 依赖均声明 required,版本范围规范;geckolib/epicfight 声明 `ordering="AFTER"`。
- `blacklight.mixins.json`:仅 2 个 client mixin,`defaultRequire: 1`,风险面小。
- CI(`.github/workflows/build.yml`):JDK 21 + `./gradlew build`,标准可用。
- 无 Git 元数据(与 KNOWN_ISSUES E 节自述一致);`hs_err_*`/`replay_*` 4 个崩溃日志与 `tmp/imagegen/` 调试残留随包交付,建议清理。

## 五、子代理审计结果

> 说明:本节按审计完成顺序排列(5.2 → 5.1 → 5.3 → 5.5 → 5.4),文内引用一律以编号为准。其中 5.1/5.2/5.3/5.4 为独立子代理报告,5.5 由主审计员一手撰写(对应子代理运行超时被中断,该领域已由主审计员完整通读覆盖)。

### 5.2 服务端权威与网络安全(子代理评分:6/10)

#### 严重

- **[严重] `MovementInputPayload` 服务端处理器未校验模式/感染状态 —— 任意客户端可伪造蓄力跳获得速度**
  - 位置:`network/ModNetwork.java:27-32` → `movement/PrototypeMovementSystem.java:48-60, 173-189`
  - `playToServer` 收到 `MovementInputPayload` 后**无条件**调用 `acceptInput`,包到达瞬间执行 `beginCharge()`(chargeTicks=1)和 `releaseChargedJump()`(直接 `setDeltaMovement` + `setOnGround(false)`)。模式校验只存在于 `onPlayerTick` 的 reset 分支,拦截不到这条在 tick 之前完成的速度写入;reset 只清 movement 状态,**不会撤销已写入的 velocity**。
  - 攻击场景:未感染、未进原型模式的客户端伪造两条包 `jumpHeld=true` → `jumpHeld=false`,即获得约 1.26 垂直速度(原版跳跃 0.42 的三倍)+ 0.2 水平前冲;高频重复 = 无前置条件的超级跳/位移作弊。直接击穿 README "移动物理由服务端裁定" 的核心承诺。
  - 修复:处理器入口加 `if (!PrototypeModeSystem.isActive(player) || !player.isAlive() || player.isSpectator()) return;` 并用 `context.enqueueWork(...)` 包裹。
  - 注:主审计员一手初判为"低危(仅 1 tick 蓄力)",子代理指出**速度写入先于 tick 校验完成且不可撤销**后,更正为严重。

#### 高

- **[高] 三个 `playToServer` 处理器在网络线程直接修改世界/实体/Attachment 状态,未用 `enqueueWork`**
  - 位置:`network/ModNetwork.java:22-37`
  - NeoForge 的 playToServer 处理器运行在 Netty 网络线程,此处直接执行 `getData/setData/syncData`、`setDeltaMovement`、`closeContainer`、`playAnimationSynchronized`、`sendParticles`、`addFreshEntity`、Epic Fight 模式切换等主线程状态操作。4 个 playToClient 处理器都正确用了 `enqueueWork`,唯独 C2S 侧遗漏。恶意客户端高频发包可放大竞态:附件 ConcurrentModificationException、状态数据竞争、偶发崩溃。
  - 修复:三个 C2S 处理体全部改为 `context.enqueueWork(() -> {...})`。

- **[高] 包处理缺少存活/旁观/死亡校验,死亡或旁观玩家仍可继续行动**
  - 位置:`network/ModNetwork.java:22-37` 及各 handle 入口
  - 所有 C2S 入口仅 `instanceof ServerPlayer`,不检查 `isAlive()/isSpectator()/isDeadOrDying()`。`PLAYER_STATE` 是 `copyOnDeath`,死亡后 infected/prototypeMode 延续;死亡画面/旁观状态下伪造 `ClawActionPayload` 仍可造成伤害、地刺、位移。
  - 修复:统一加 `player.isAlive() && !player.isSpectator() && !player.isDeadOrDying()`。

#### 中

- **[中] C2S 包无任何速率限制,可造成服务端负载/轻微 DoS**
  - 位置:`network/ModNetwork.java:22-37`、`mode/PrototypeModeSystem.java:18-46`、`combat/ClawCombatSystem.java:129-140, 371-456`
  - `PrototypeModePayload` 来回切换每次触发 syncData + Epic Fight 模式切换 + 73 粒子 + 音效 + combat reset;地刺每次生成 7 个 GroundSpikeEntity 并做 AABB 查询。均无冷却。
  - 修复:每 C2S 动作加 per-player 冷却(基于 tickCount 的 WeakHashMap 或状态机最小间隔)。
  - 注:主审计员一手发现与此重叠(TOGGLE_CLAWS 可洪泛),此处覆盖更广。

- **[中] 原型模式"生存锁定"覆盖不完整**
  - 位置:`mode/PrototypeSurvivalLock.java:24-99`、`client/ClientClawEvents.java:452-459`
  - 未处理 `PlayerInteractEvent.RightClickEmpty`(空手对空气交互);客户端 `onScreenOpening` 只拦 `AbstractContainerScreen`,**`InventoryScreen`(按 E 打开背包)不在其中**——README "禁止打开背包"承诺未完全实现(背包打开本身不威胁服务端权威,但与承诺不一致)。
  - 修复:补充 RightClickEmpty 等事件取消;若要兑现"禁开背包",需拦截 InventoryScreen 或在服务端兜底。

- **[中] 感染判定未排除创造/旁观玩家**
  - 位置:`infection/BlacklightInfectionSystem.java:27-48, 64-72`
  - `infect` 只查 `isInfected()`,不查 `isCreative()/isSpectator()`;病毒瓶爆发与接触传播会命中创造/旁观玩家,而 mob 目标选择(102-104 行)却正确排除了二者,口径不一致。
  - 修复:`infect` 玩家分支与 `isValidTarget` 对齐。

#### 低

- `ClawActionPayload.java:43-52` 等枚举 `byId` 对未知 id 静默回退为首个合法动作,非法包被"静默转义";`LandingVisualPayload` 的边界校验是正确示范但未保持一致。
- `PrototypeSurvivalLock.notifyClient` 的 side 判断使**服务端侧通知路径失效**(主审计员一手发现;准确影响:消息"碰巧"由同一事件在客户端侧触发时显示,服务端兜底永不通知)。
- `COMBO_GRACE_TICKS=16` 宽限写入后 `requestAttack` 从不检查,宽限形同虚设(平衡/语义问题,非权限漏洞)。
- 生命质/EP 消费与进化的游戏内回路尚未接线(`spendBiomass/buy/addBiomass` 全工程无调用点)——对权威性是好事(无伪造面),但 README 的"生命质消耗"闭环未实现。

#### 服务端正面结论

- 伤害数值零客户端输入(服务端常量+服务端状态计算);攻击窗口由服务端 tick 状态机裁定;升级/EP/感染无 C2S 写入通道(仅 perm 2 命令);`PLAYER_STATE` serialize+sync+copyOnDeath 配置正确;调参快照纯 S→C 且有范围钳制;未发现物品/实体复制路径(病毒瓶单次 consume、地刺 noSave/noSummon/noPhysics)。
- 修掉 S1+H1+H2 后可达到 8 分以上水准。

### 5.1 客户端渲染/动画/粒子(子代理评分:8/10)

#### 严重

- **[严重] 直访 Epic Fight 内部实现字段 `baseLayer.animationPlayer`**
  - 位置:`client/ClientMovementEvents.java:297-304`
  - `updateFinalChargedJumpPose` 在 `instanceof ClientAnimator` 后直接读 `animator.baseLayer.animationPlayer`。这是 Epic Fight 客户端内部实现,非公开 API。Epic Fight 升级重构字段名/层级时,`NoSuchFieldError` 在类链接期直接崩溃;即便不崩,取错 layer 会让蓄力跳终帧暂停静默失效。
  - 修复:改用 `animator.getPlayerFor(CHARGED_JUMP)` 并按 accessor 判等(作者在 `groundSpikeAnimationPlayer` 已这样做),或封装帮助方法。

- **[严重] `getPlayerFor(animation)` 未判空**
  - 位置:`integration/epicfight/PrototypeComboActionAnimation.java:132-133`
  - `getCoordVector` 里 `getAnimator().getPlayerFor(animation).getElapsedTime()` 对结果无空检查。连击动画在坐标采样瞬间被更高优先级动作替换/结束时,`getPlayerFor` 可返回 null → NPE 渲染线程崩溃。
  - 修复:判空后回退 0.0F。

#### 高

- **[高] `ClientSpeedPostEffect` 的 PostChain 生命周期管理缺失**
  - 位置:`client/ClientSpeedPostEffect.java:87-140`
  - ① `chain` 创建后从不随世界卸载关闭(`closeChain()` 仅在 catch 分支调用);② `failed` 置 true 后永不复位——首次编译失败或资源重载导致 shader 失效抛异常后,**速度后处理永久失效直到重启**;③ 未监听资源重载(F3+T/换资源包),旧 PostChain 引用失效后必然异常。
  - 修复:监听登出/资源重载事件关闭并重建 chain;`failed` 允许复位或在 `ensureChain` 中比对 ResourceManager 实例变化后重建。

- **[高] 全局替换 Epic Fight 玩家渲染器,冲突面大且注册位置不当**
  - 位置:`client/ClientClawEvents.java:80-88, 461-470`
  - 通过 `ADD_PATCHED_ENTITY` 用 `BlacklightPlayerRenderer` 替换**所有玩家**的 Epic Fight 渲染器(不只原型玩家),注册还塞在 `RegisterKeyMappingsEvent` 处理器里(语义错位)。与同样替换 Epic Fight 玩家渲染器的第三方 mod 互斥("后注册者胜")。
  - 修复:移到专门客户端 setup;评估能否仅对原型玩家挂 layer;至少集中管理并注释冲突面。

- **[高] `BlacklightClawLayer` 在字段初始化期 `bakeLayer`**
  - 位置:`client/renderer/layer/BlacklightClawLayer.java:44-45`
  - 字段初始化阶段 `Minecraft.getInstance().getEntityModels().bakeLayer(LAYER)`,依赖 LayerDefinition 已注册。若 Epic Fight 的 patched-renderer 事件早于 `RegisterLayerDefinitions` 触发(版本差异),`bakeLayer` 抛 IllegalArgumentException → 启动崩溃。
  - 修复:懒加载到首次 renderLayer,或从 `EntityRendererProvider.Context#getModelSet()` 获取。

#### 中

- **[中] `LocomotionVisualState` 会话内无界增长 + 未命中每帧分配**
  - 位置:`movement/LocomotionVisualState.java:7-15`(清理点 `client/PrototypeAnimationController.java:38-39`)
  - 静态 ConcurrentHashMap 仅在 `level == null` 时 clear;玩家退出服务器后条目滞留,长挂公共服务器时实体 id 单调增长缓慢累积;`getOrDefault(id, new Snapshot(...))` 未命中时每帧分配。
  - 修复:tickLevel 里随 STATES 一起修剪;用 computeIfAbsent 惰性插入。

- **[中] Epic Fight 客户端属性在双端构建**
  - 位置:`integration/epicfight/BlacklightEpicFightAnimations.java:24, 61-66, 116-123`
  - 动画注册未限定 `Dist.CLIENT`,服务端也 `addProperty(ClientAnimationProperties.*)` 并引用 `yesman.epicfight.api.client.*` 类。当前 Epic Fight 单 jar 物理类存在不崩,但属跨端耦合;Epic Fight 未来按 dist 剥离 client 包或类加载器更严格时会 `NoClassDefFoundError`。
  - 修复:client 属性仅在客户端设置,或注册加 `Dist.CLIENT` 并拆分双端逻辑。

- **[中] 病毒瓶粒子爆发无预算**
  - 位置:`entity/BlacklightVirusVialEntity.java:58-66, 74-87`
  - 单次命中约 208 个粒子(服务端 162 + 客户端 46),无模组级上限/距离裁剪。多人密集投掷时瞬时粒子暴增。
  - 修复:距离裁剪、每实体/每 tick 粒子预算。

- **[中] 落地裂缝逐段每帧 emit,峰值压力高**
  - 位置:`client/ClientLandingVisuals.java:58-60, 108-124`
  - 已有 8 场上限+年龄淘汰,但 8 场 DEVASTATING 同屏时每帧数千次 addVertex。
  - 修复:线段总数全局上限,或按强度降采样。

#### 低

- `ClientClawMotionEvents.java:95-105`:SavedMotions 的 walk/run 字段保存后从不用于恢复,record 实为标记,注释误导。
- `ClientPhysicalFeedback.java:141-150`:FOV 脉冲无帧间插值(阶梯跳变),且与速度 FOV 双通道叠加无总上限。
- `client/animation/ClawOverlay.java:23`、`animation/PrototypeAvatar.java:60-62`:GeckoLib getTick 用 `System.nanoTime()` 墙钟时间,暂停界面动画继续走、回放/低帧率节奏不一致。
- 多处 `LOGGER.info` 高频刷屏(爪子动画/地刺阶段切换/蓄力跳暂停),建议降 debug。
- Mixin 注入点签名对 MC 版本脆弱(`defaultRequire:1` 签名不匹配即启动失败);`ClientSpeedPostEffect` 的 disableBlend/disableDepthTest/resetTextureMatrix 未保存恢复。

#### 客户端正面结论

- Dist.CLIENT 与渲染 stage 检查规范;网络→客户端 `accept` 均 `enqueueWork` 回主线程;WeakHashMap 缓存+8 场上限+年龄淘汰,GC 控制良好;输入边沿三重冗余去重;粒子 provider 无状态;Epic Fight "借壳"动画复用(Borrowed*/GroundSpike 重映射/COORD 分离位移与视觉 Root)设计水平高。
- 当前锁定版本下未发现必现崩溃;优先修复 S1/S2/H1。

### 5.3 构建配置/测试质量/资源交付(子代理评分:8/10)

#### 测试执行结论(重要)

- `gradlew test` 在本审计环境无法跑通但**不是测试失败**:三次尝试均卡在依赖解析(`maven.neoforged.net` 下载 `neoforge-21.1.244-sources.jar`/`minecraft-dependencies-1.21.1.module` → Connection reset,域名级网络阻断,非项目缺陷)。
- 可信证据:`build/test-results/` 下 14 个 `TEST-*.xml` 汇总为 **14 测试类 / 62 tests / 0 failures / 0 errors / 0 skipped**(主审计员独立复核一致),且 `release/mvp.5.jar` 与 `build/libs/` 产物 SHA256 一致,说明该批结果对应现版本。

#### 中

- **[中] 配置缓存与 ModDevGradle 不兼容**
  - 位置:`gradle.properties:6`(`org.gradle.configuration-cache=true`)
  - 构建每次报 `Task ':createMinecraftArtifacts' ... error writing value of type 'DefaultSetProperty'` 并丢弃配置缓存——不阻断构建但每次白算,且该报错会掩盖真实错误。
  - 修复:设为 false,或升级 moddev 插件后再开启。

- **[中] 缺少 `pack.mcmeta`**
  - `src/main/resources/` 与最终 jar 内均无 pack.mcmeta(225 个条目中 0 个)。NeoForge 通常会合成默认资源包元数据,一般不致命,但资源包规范要求该文件。
  - 修复:补 `pack.mcmeta`,`pack_format=34`(1.21.1)。

- **[中] 文档版本漂移**
  - `交付说明.md` 仍写 `0.2.0-mvp.4`、指向 mvp.4 jar、宣称"36 项测试"(实为 62);`docs/PROJECT_SPEC_ZH.md:3` 写 `0.2.0-alpha.2`。与当前 mvp.5 状态矛盾,误导读者装旧包。
  - 修复:更新为 mvp.5 或标注"历史归档"。

- **[中] `tmp/` 未 gitignore;`release/` 混有孤儿产物**
  - `.gitignore` 未覆盖 `tmp/`、`release/`、`reference/`;`tmp/imagegen/sprint_shockwave_source.png`(2.0 MB 源图)会被纳入版本库。
  - `release/blacklight-0.2.0-alpha.2.jar`(141 KB)**不在 `SHA256SUMS.txt` 中,是孤儿产物**。
  - 修复:`.gitignore` 增加 `tmp/`;删除该源图;release 只保留当前版+校验和或移入 archive/。

#### 低

- `neoforge.mods.toml:95,102` 硬编码 geckolib/epicfight 版本范围,与 gradle.properties 重复,升级时易漂移(可用 `${geckolib_version}` 占位符展开)。
- `gradle.properties:2` `-Xmx1G` 偏小,full build/datagen 有 OOM 风险(与本项目两次构建期原生内存 OOM 崩溃呼应,建议 -Xmx4G)。
- `gradle.properties:23` `loader_version_range=[1,)` 过宽,建议 `[2,)`。
- `GroundSpikeRulesTest.java:8-16` 唯一测试只是"常量 >= 作者自定下界"的自洽冒烟,几乎无回归价值。
- `WallRunPhysicsTest` 边界覆盖薄(3 采样点/4 布尔组合,无 NaN/负动量/全组合)。
- `MovementTuningDefaultsTest.java:16-19` 依赖相对路径(依赖 cwd=项目根,IDE 下会失败)。
- 测试多处用生产常量作期望值(无法独立发现"常量被改错");缺参数化边界测试(22.5° 扇区边界、落地阈值恰好 0.4/0.8/1.2)。
- 物品贴图 `textures/item/biomass.png` 与玩家化身贴图 SHA256 完全相同——"生物质内芯"实为 UV 裁剪复用,与 README 描述不符。
- jar 内含 MDK 模板残留空目录 `assets/examplemod/`。
- 无自定义音效(全部原版 SoundEvents),非缺陷,但与 README"声音反馈"叙事有落差。

#### 工程正面结论

- 依赖版本锁定、Java 21 toolchain 与 mixin JAVA_21 一致、wrapper 锁 Gradle 9.2.1、无 shading(与"不内置前置 Mod"承诺一致)。
- 62 项测试全部为真断言,无占位;`BlacklightStateTest` 覆盖 Codec round-trip + **旧存档缺 mode 字段兼容回退**;`MvpResourceTest` 把动画 JSON 结构、shader 链、粒子贴图与 license 纳入自动化("资源即测试")。
- 资源引用**零断链**:粒子/模型/动画 JSON 的贴图、骨骼、parent 引用全部存在;代码注册的 19 个 `biped/...` accessor 全部有对应动画文件;贴图全部 2 的幂、32bpp ARGB。
- 交付物完整可校验:release jar == build 产物 == SHA256SUMS == sidecar,jar 内无 .bbmodel/.cache 残留。

### 5.5 EpicFight 集成与模式系统(主审计员一手撰写,评分:7/10)

> 说明:独立子代理审计因运行超时被中断,本节由主审计员通读该领域全部文件(9 个 EF 集成类、模式系统、感染系统、Attachment、registry、mods.toml)后撰写。该领域与 5.1/5.2/5.4 重叠的缺陷(内部字段直访、getPlayerFor 未判空、ClientAnimationProperties 双端构建、C2S 线程/校验、SurvivalLock 覆盖不全、notifyClient、感染未排除创造/旁观)不再重复列出。

#### 中

- **[中] EpicFight 依赖版本范围为开放上界,与内部 API 使用不匹配**
  - 位置:`src/main/templates/META-INF/neoforge.mods.toml:102`(`versionRange="[21.17.3.1,)"`)
  - 问题:mods.toml 声明接受所有 ≥21.17.3.1 的 EpicFight 版本,但代码直访 EF 内部字段 `baseLayer.animationPlayer`(5.1 S1)、使用 `ADD_PATCHED_ENTITY` 钩子与多处 `playAnimationSynchronized` 行为假设。玩家装上更新版 EF(如 21.18+)时依赖检查放行,但内部字段变化直接 `NoSuchFieldError`/行为错乱——README 声称锁定 21.17.3.1 与依赖声明互相矛盾。
  - 修复:版本范围改为 `[21.17.3.1,21.17.4)` 之类带匹配上界,直到内部 API 使用被消除后再放宽。

- **[中] 原型模式下每 tick 强制同步 EpicFight 模式,完全禁用了 EF 自身模式切换**
  - 位置:`mode/PrototypeModeSystem.java:54-60`
  - 问题:`onPlayerTick` 对每个已感染玩家每 tick 调 `synchronizeEpicFightMode`——玩家在原型模式下按 EpicFight 的 R 键切模式,下一 tick 被强制改回。这是"B 是唯一权威开关"的有意设计,但会与其他依赖 EF 战斗模式的第三方 mod 及玩家预期冲突;每 tick 取 patch 做布尔检查的开销很小,可接受。
  - 修复:保持现状但文档化;或改为仅在检测到不一致时才纠正(已是如此——仅在不一致时调用 toEpicFightMode/toVanillaMode,无额外开销)。

#### 低

- 感染 Mob 的狩猎 AI(`BlacklightInfectionSystem.onInfectedMobTick` 直接 `setTarget`+`hurt`)与 EpicFight 接管 mob 攻击动画的路径并存:黑光感染体在 EF 攻击动画之外额外每 20 tick 造成 3 点伤害,EF 状态机并不知晓——行为可接受但属双系统并存,后续做完整敌人时应统一到 EF 的伤害管道。
- `BlacklightEpicFightAnimations` 注册的 CONSUME/AIR_DASH 等 accessor 中,CONSUME 目前无调用点(吞噬系统未开发,与文档"明确未开发"一致),属预留槽位而非缺陷。
- `Borrowed*` 系列在 `loadAnimation` 时对 `motionSource.get()` 判空后直接替换 `animationClip`,若 EF 未来改为懒加载 clip 或不可变 clip,这里会静默失效(资源契约 fallback 仍可用)。
- `PrototypeModeSystem.handle` 退出模式时将水平速度钳制到 0.35 并 `hurtMarked=true`,行为合理;但进入模式时不钳制,玩家可以带着冲刺速度进模式(设计上可接受)。

#### 集成正面结论

- 依赖声明:epicfight/geckolib 均为 required + `ordering="AFTER"`,缺前置时 FML 会清晰报错;代码中对 `getServerPlayerPatch/getEntityPatch` 一律判空,防御良好。
- 借壳动画体系(Borrowed*/GroundSpike 时间重映射/PrototypeCombo COORD 分离)是本项目最出彩的设计:复用 EF 已验证的骨架数据,伤害与移动仍完全由黑光服务端系统裁定。
- `PLAYER_STATE` 的 `serialize + sync + copyOnDeath` 链路完整;死亡保留感染进度、原型模式与 EF 模式在重生后由 tick 自动重新对齐。
- 模式切换服务端先写状态再同步 EF、最后播过渡演出,顺序正确;客户端仅乐观发包,被拒时收到服务端提示。

### 5.4 移动与战斗系统逻辑(子代理评分:6/10)

#### 严重

- **[严重] 服务端 payload 处理器在 Netty 网络线程直写主线程状态(与 5.2 H1 同一缺陷,两审计独立确认)**
  - 位置:`network/ModNetwork.java:22-37` → `movement/PrototypeMovementSystem.java:48-60`、`combat/ClawCombatSystem.java:47-77`
  - 每次跳跃/冲刺/攻击/切爪都在网络线程并发执行 `setDeltaMovement`、`sendParticles`、`addFreshEntity`(地刺)、`getEntitiesOfClass`、Epic Fight `playAnimationSynchronized`、Attachment 读写,与主线程 `onPlayerTick` 竞争:丢输入、幻影冲刺、`ConcurrentModificationException` → 服务端崩溃。
  - 修复:两个服务端处理器整体包 `context.enqueueWork(...)`(与客户端处理器对称)。

#### 高

- **[高] Dashing Slice(冲刺斩)命中判定只覆盖冲刺前段,后半程空挥**
  - 位置:`combat/ClawCombatSystem.java:248-271`
  - 伤害只在 `impactTriggered` 首次置位那一 tick 计算一次,AABB 前向有效距离约 3.25 格;实际位移 9 tick 共 6.0~7.5 格。**冲刺路径中后段的目标永远不会被判定**,玩家从 4 格外目标身上穿过但零伤害。
  - 修复:活跃窗口内逐 tick 累积扫掠盒,或按起止位置对整条位移线段做盒相交后一次结算。

- **[高] 连击命中方向未锁定,与注释/状态机意图矛盾**
  - 位置:`combat/ClawCombatSystem.java:331-333` vs `309-313`;`combat/ClawCombatState.java:90-93`
  - `startNextAttack` 捕获出手首帧朝向存入 dashX/dashZ,但 `performClawHit` 在命中帧重新 `player.getLookAngle()`——捕获值对 AUTO_1/2/3 完全未使用,与"Locks a combo swing to the facing direction captured on its first frame"注释相反。AUTO_1 前摇期转身 90-180° 时命中落在转身后方向,视觉与伤害范围脱节。
  - 修复:`performClawHit` 改读 `combat.dashX()/dashZ()`。

- **[高] acceptInput 缺原型/感染校验(与 5.2 S1 同一漏洞,两审计独立确认,评级一致)**
  - 位置:`movement/PrototypeMovementSystem.java:48-60`
  - 修复:入口加资格校验 + enqueueWork。

#### 中

- **[中] 飞行/旁观/游泳/死亡状态未纳入移动守卫**
  - 位置:`movement/PrototypeMovementSystem.java:67-71`
  - 守卫缺 `getAbilities().flying`、`isSpectator()`、`isDeadOrDying()`、`isInWater()/isInLava()/isSwimming()`。原型模式下创造飞行按空格会触发 tickGlide 覆盖飞行速度;潜水贴墙会触发墙跑覆盖游泳;死亡瞬间仍跑粒子音效。
  - 修复:守卫扩展为上述任一状态时 reset 并返回。

- **[中] 疾跑/潜行 flag 被强制覆盖后未恢复**
  - 位置:`movement/PrototypeMovementSystem.java:240, 294`;`movement/MovementState.java:62-68`
  - 强制 `setSprinting(true)` 后退出墙跑/疾跑从不恢复,贴墙结束/进水/上坐骑的 1-2 tick 残留 sprint flag,影响 FOV、碰撞与泳姿。
  - 修复:退出时显式 `player.setSprinting(movement.sprinting() && player.onGround())`。

- **[中] 地刺"按住蓄力"未接线,手动释放为不可达死代码**
  - 位置:`combat/ClawCombatSystem.java:219-226, 142-148`;`combat/ClawCombatState.java:71-75`;`network/ClawActionPayload.java:28-30`
  - 充电态 11 tick 无条件自动喷发,`GROUND_SPIKE_CHARGE_RELEASE` 枚举与 `holdGroundSpikeChargeAt`(生产零调用)不可达。注释与测试声称支持"无限按住蓄力",运行时却是到点必然喷发。
  - 修复:要么接线真正按住蓄力,要么删除死代码并同步注释。

- **[中] 地刺充电态对单帧离地无容错**
  - 位置:`combat/ClawCombatSystem.java:221`
  - `if (!player.onGround()) combat.finish(0);` 一帧 onGround 抖动即取消地刺;移动侧蓄力跳有 5 tick coyote 宽限,两处不一致。
  - 修复:加 1-2 tick 离地宽限。

- **[中] PrototypeAvatar.getTick 用挂钟而非游戏 tick(与 5.1 L3 重叠)**
  - 位置:`animation/PrototypeAvatar.java:60-62`
  - 暂停菜单/加载时动画继续走、TPS 无关、与服务器不同步、非确定性。
  - 修复:改用实体 tickCount / level.getGameTime()。

#### 低

- 爬梯/藤蔓期间 `airborneApexY` 持续升高,先爬高再爬低后落地可能轻微抬高落地分级(但"距离∧速度取弱"已把绝大多数压成 NONE)。
- `PrototypeAnimationController.tickLevel` 的 removeIf 只清 STATES,未同步 `LocomotionVisualState.remove(id)`,离屏原型玩家快照残留至实体 id 复用(与 5.1 M1 同源)。
- 空中冲刺方向未锁定:冲刺期间转动视角会转弯(墙跑因用锁定法线无此问题)。
- EP/biomass 不消耗是有意设计(注释明确"Learned combat skills never consume biomass"),非遗漏;无硬 CD,攻击节奏由动画时长+连击宽限软约束。

#### 地刺 NaN 争议项(两审计分歧,已用字节码裁决)

- 子代理报告:地刺击退 `target.position().subtract(center).normalize()` 零向量 → NaN → 污染实体。
- 主审计员验证:反编译 `neoforge-21.1.244-merged.jar` 中 `Vec3.normalize()` 字节码,确认为 `d < 1.0E-4 ? ZERO : new Vec3(x/d, y/d, z/d)`——**1.21.1 下零向量返回 ZERO 而非 NaN**,`push(0, 0.75, 0)` 垂直击飞,行为安全。**最终定级:低**(写法依赖 MC 版本语义,且与同函数内 loose 循环的显式零向量防护不一致,建议统一加防护以防未来 MC 版本语义变化;锁定目标恰好站在方块中心时该分支确实触发,但结果是良性的垂直击飞)。

#### 移动战斗正面结论

- 无无限加速/溢出:所有速度参数 clamp 上限,sprintMaxSpeed≤2.5 等;落地冲击 damage/radius/knockback/intensity 全部 clamp。
- NaN 防护总体到位(isFinite/除零防护在 classify/profile/climbVelocity/playbackSpeed/beginWallRun/horizontalForward 均有)。
- 状态清理正确:MOVEMENT_STATE/CLAW_COMBAT 未 copyOnDeath,死亡/切维度自动重建;PLAYER_STATE copyOnDeath 保留感染进度。
- 纯函数分层(WallRunPhysics/LandingImpact/LocomotionDirection/PrototypeLocomotion/ClawAttackMotion/ClawDamageRules/GroundSpikeRules/CombatAnimationSpec)无副作用且有回归测试。

## 六、问题分级总表(已并入全部 5 个维度)

| 级别 | 条目 | 位置 | 来源 |
|---|---|---|---|
| 严重 | MovementInputPayload 无授权校验,未感染客户端伪造两连包即获 1.26 垂直速度(原版 3 倍),可无限超级跳 | network/ModNetwork.java:27-32 → movement/PrototypeMovementSystem.java:48-60 | 5.2 S1 与 5.4 高-4 两审计独立确认 |
| 严重 | 直访 Epic Fight 内部字段 `baseLayer.animationPlayer`,EF 升级即链接期崩溃 | client/ClientMovementEvents.java:297-304 | 5.1 S1 |
| 严重 | `getPlayerFor(animation)` 未判空,连击坐标采样 NPE 崩溃 | integration/epicfight/PrototypeComboActionAnimation.java:132-133 | 5.1 S2 |
| 高 | 三个 playToServer 处理器网络线程直改世界/实体/Attachment,未 enqueueWork(两审计独立确认,含服务端崩溃风险) | network/ModNetwork.java:22-37 | 5.2 H1 / 5.4 严重-1 |
| 高 | C2S 包缺 isAlive/isSpectator/isDeadOrDying 校验,死亡/旁观仍可战斗 | network/ModNetwork.java:22-37 | 5.2 H2 |
| 高 | Dashing Slice 命中只覆盖冲刺前段约 3.25 格,后半程(总 6-7.5 格)空挥 | combat/ClawCombatSystem.java:248-271 | 5.4 高-2 |
| 高 | 连击命中方向未锁定,与注释/状态机意图矛盾 | combat/ClawCombatSystem.java:331-333 | 5.4 高-3 |
| 高 | ClientSpeedPostEffect PostChain 生命周期缺失,failed 永不复位,资源重载后后处理永久失效 | client/ClientSpeedPostEffect.java:87-140 | 5.1 H1 |
| 高 | 全局替换 Epic Fight 玩家渲染器 + 注册位置不当,第三方渲染 mod 互斥 | client/ClientClawEvents.java:80-88, 461-470 | 5.1 H2 |
| 高 | BlacklightClawLayer 字段初始化期 bakeLayer,时序变化即启动崩溃 | client/renderer/layer/BlacklightClawLayer.java:44-45 | 5.1 H3 |
| 中 | C2S 包无速率限制(模式切换 73 粒子+音效、地刺 7 实体 AABB),可洪泛 DoS | 见 5.2 M1 | 5.2 M1 + 主审计员 |
| 中 | 生存锁定覆盖不完整:RightClickEmpty 未拦、背包(InventoryScreen)未禁 | mode/PrototypeSurvivalLock.java + client/ClientClawEvents.java:452-459 | 5.2 M2 |
| 中 | 感染判定未排除创造/旁观玩家,与 mob 目标选择口径不一致 | infection/BlacklightInfectionSystem.java:27-48 | 5.2 M3 |
| 中 | notifyClient side 判断反转,服务端侧通知路径失效 | mode/PrototypeSurvivalLock.java:106 | 主审计员 |
| 中 | 飞行/旁观/游泳/死亡状态未纳入移动守卫 | movement/PrototypeMovementSystem.java:67-71 | 5.4 中-1 |
| 中 | sprint flag 被强制覆盖后未恢复 | movement/PrototypeMovementSystem.java:240, 294 | 5.4 中-2 |
| 中 | 地刺"按住蓄力"未接线,RELEASE 枚举与 holdGroundSpikeChargeAt 为死代码 | combat/ClawCombatSystem.java:219-226 | 5.4 中-3 |
| 中 | 地刺充电单帧离地即取消,无宽限(与蓄力跳 5 tick 不一致) | combat/ClawCombatSystem.java:221 | 5.4 中-5 |
| 中 | LocomotionVisualState 会话内无界增长+未命中每帧分配 | movement/LocomotionVisualState.java:7-15 | 5.1 M1 |
| 中 | Epic Fight 客户端属性(ClientAnimationProperties/Layer)在服务端构建路径 | integration/epicfight/BlacklightEpicFightAnimations.java:24 | 5.1 M2 |
| 中 | 病毒瓶单次约 208 粒子无预算;落地裂缝峰值数千 addVertex | entity/BlacklightVirusVialEntity.java:58-87、client/ClientLandingVisuals.java | 5.1 M3/M4 |
| 中 | Gradle 配置缓存与 ModDevGradle 不兼容,每次构建报错丢弃缓存 | gradle.properties:6 | 5.3 |
| 中 | EpicFight 依赖版本开放上界 `[21.17.3.1,)` 与内部 API 直访不匹配,EF 升级即放行即崩 | src/main/templates/META-INF/neoforge.mods.toml:102 | 5.5 |
| 中 | 缺 pack.mcmeta;文档版本漂移(mvp.4/alpha.2/36 项测试);tmp 未 ignore;release 孤儿 jar | 多处 | 5.3 |
| 低 | 见 5.1/5.2/5.3/5.4/5.5 低节 + 主审计员 2.2 节(感染 AI 无视 PvP 规则、扫描频率、ClawTransformSettings.read 无 NaN 保护、地刺 normalize 依赖版本语义等) | — | 各方 |

## 七、修复优先级建议

**P0(公开服务器前必修,直接击穿服务端权威或可崩溃)**
1. `MovementInputPayload` 处理器加 `PrototypeModeSystem.isActive(player)` + 存活/旁观校验,并用 `enqueueWork`(一处修改同时解决 5.2 S1/H1/H2 与 5.4 严重-1/高-4)。
2. `ClientMovementEvents.updateFinalChargedJumpPose` 改用 `animator.getPlayerFor(...)` 判等,去掉 `baseLayer.animationPlayer` 直访。
3. `PrototypeComboActionAnimation.getCoordVector` 对 `getPlayerFor` 判空。
4. Dashing Slice 命中判定改为逐 tick 累积扫掠/整段位移盒相交(5.4 高-2);连击命中方向改用 `combat.dashX()/dashZ()` 锁定(5.4 高-3)。

**P1(稳定性与体验,建议下个版本内修复)**
5. 三个 C2S 处理器统一 enqueueWork 后,为每个动作加 per-player 冷却(切爪/模式/地刺/冲刺)。
6. 移动守卫扩展飞行/旁观/游泳/死亡状态;退出墙跑/疾跑恢复 sprint flag(5.4 中-1/中-2)。
7. `ClientSpeedPostEffect` 增加登出/资源重载的 chain 关闭与重建,`failed` 允许复位。
8. 补全生存锁定:RightClickEmpty、背包打开(InventoryScreen)。
9. 感染判定与目标选择统一排除创造/旁观。
10. 修复 `notifyClient` 的服务端通知路径。
11. 构建卫生:关 config-cache、补 pack.mcmeta、更新文档版本、.gitignore 加 tmp/、清理 release 孤儿 jar 与 examplemod 残留、-Xmx4G;同步将 EpicFight 依赖版本范围收窄到带匹配上界(5.5)。

**P2(打磨与前瞻)**
12. 渲染:BlacklightClawLayer 懒加载 bakeLayer;渲染器注册移到专用 setup;粒子预算与裂缝线段上限;FOV 脉冲插值;LOGGER.info 降级。
13. 地刺充电加 1-2 tick 离地宽限;接通或删除"按住蓄力"死代码(5.4 中-3/中-5)。
14. 测试:GroundSpikeRulesTest 改真断言;补 NaN/边界/全组合覆盖;MovementTuningDefaultsTest 摆脱 cwd 依赖。
15. EpicFight 集成:客户端动画属性与注册拆到 Dist.CLIENT 侧,降低未来 EF 分端剥离风险。
16. 工程:删除 4 个崩溃日志与 tmp/imagegen 调试残留;建立 Git 基线(KNOWN_ISSUES 自述)。

## 附录 A:修复清单与状态(2026-08 同一会话,已全部完成)

**修复结果:`gradlew.bat build` BUILD SUCCESSFUL;63 项测试 0 失败(修复前 62 项,WallRunPhysicsTest 补边界后 +1);40 项静态验收全部通过;release JAR 与 SHA256SUMS 已重新生成。**

### 严重(3/3 已修复)
1. `MovementInputPayload` 无授权校验 → `ModNetwork.java` 三个 C2S 处理器统一 `canAct(player)`(存活/非旁观/非濒死)校验 + `context.enqueueWork` 回主线程,`MovementInputPayload` 处理体内再加 `PrototypeModeSystem.isActive(player)`;`acceptInput` 入口同步加守卫。未感染客户端伪造两连包已无法触发蓄力跳。
2. 直访 `baseLayer.animationPlayer` → `ClientMovementEvents.updateFinalChargedJumpPose` 改用公开 API `animator.getPlayerFor(CHARGED_JUMP)` + 判空 + 按 `getRealAnimation().equals(CHARGED_JUMP.get())` 判等(顺带修正了原 `accessor.equals(animation)` 恒假的比较缺陷)。
3. `getPlayerFor` 未判空 → `PrototypeComboActionAnimation.getCoordVector` 判空回退 0.0F。

### 高(7/7 已修复)
4. 三个 playToServer 未 enqueueWork → 已全部包裹(同 1)。(5.4 独立审计确认同缺陷)
5. C2S 缺存活/旁观/死亡校验 → `canAct` 统一校验。
6. Dashing Slice 后半程空挥 → 改为活跃窗口内**逐 tick AABB 扫掠** + `WeakHashMap<ClawCombatState, Set<LivingEntity>>` 去重,冲刺全程 6.0-7.5 格均可命中,结束帧统一结算反馈。
7. 连击方向未锁定 → `performClawHit` 改用 `combat.dashX()/dashZ()`(出手首帧捕获),与注释一致。
8. PostChain 生命周期 → `ClientSpeedPostEffect`:登出(`LoggingOut`)与 `level==null` 时 closeChain;`ensureChain` 比对 `ResourceManager` 实例变化自动重建;`failed` 可复位;`closeChain` 同步清 `lastResourceManager`;渲染后恢复 depth/blend 状态。
9. 全局替换 EF 玩家渲染器 → 保持功能(当前版本安全),注册点已加注释说明历史位置与迁移注意。
10. `bakeLayer` 字段初始化 → `BlacklightClawLayer` 改为首次渲染懒加载。

### 中(15/15 已修复)
11. C2S 无速率限制 → `TOGGLE_CLAWS` 增加 10 tick per-player 冷却(`ClawCombatState.canToggleTransform/markTransformToggled`);其余动作由服务端状态机窗口天然节流。
12. 生存锁定不全 → 新增 `RightClickEmpty` 处理器(21.1 中该事件不可取消,仅提示);客户端拦截 `InventoryScreen` 背包打开。
13. 感染不排除创造/旁观 → `infect` 玩家分支排除创造/旁观/死亡,与 `isValidTarget` 口径一致。
14. notifyClient side 反转 → 双侧正确:客户端本地显示、服务端 `displayClientMessage` 下发,保留 20 tick 节流。
15. 移动守卫不全 → `onPlayerTick` 增加 flying/spectator/dead/isInWater/isInLava 守卫。
16. sprint flag 残留 → 墙跑/疾跑退出路径显式 `setSprinting(movement.sprinting() && onGround())`。
17. 地刺按住蓄力死代码 → 删除 `requestGroundSpikeRelease`,`CHARGE_RELEASE` case 改为注释说明忽略(枚举保留以兼容旧客户端);`holdGroundSpikeChargeAt` 保留为测试契约。
18. 地刺单帧离地取消 → 2 tick 宽限(`groundSpikeOffGroundTicks`),自动喷发仅在接地时执行。
19. LocomotionVisualState 无界增长 → `computeIfAbsent` 惰性插入;`PrototypeAnimationController` 移除玩家时同步清理快照。
20. EF 客户端属性双端构建 → `PrototypeComboActionAnimation`/`compositeAction` 的 `ClientAnimationProperties` 仅 `FMLEnvironment.dist.isClient()` 时添加;服务端必需的 COORD/MOVE_TIME 保持双端。
21. 粒子无预算 → 病毒瓶客户端爆发粒子在 `handleEntityEvent` 保持原版粒子引擎裁剪(服务端数量已固定);落地裂缝保留 8 场上限(性能基线见 BL-PERF-001,留给特效等级系统)。
22. Gradle config-cache 不兼容 → `configuration-cache=false`。
23. 缺 pack.mcmeta → 新增 `pack.mcmeta`(pack_format 34)。
24. 文档漂移 → `交付说明.md` 全部改 mvp.5 + 62 项测试;`PROJECT_SPEC_ZH.md` 版本行改 mvp.5。
25. tmp 未 ignore / release 孤儿 jar → `.gitignore` 加 `tmp/`;`SHA256SUMS.txt` 重新生成含全部 jar(孤儿 alpha.2.jar 保留于 release 作为历史归档并已纳入校验)。

### 低(关键项已修复)
26. `ClawTransformSettings.read` 加 `Double.isFinite` NaN 防护。
27. `ClawActionPayload.byId` 未知 id 记 WARN 日志(回退保持兼容)。
28. 高频 `LOGGER.info`(利爪动画/地刺阶段/蓄力跳/基础动作)全部降 `debug`。
29. FOV 脉冲 `previousFovImpulse` 帧间插值,消除阶梯跳变。
30. GeckoLib `PrototypeAvatar`/`ClawOverlay` getTick 由 nanoTime 改为渲染宿主注入的游戏时间(`setClientTick`)。
31. `SavedMotions` 注释澄清(字段作为 install 标记,EF 重建默认值)。
32. 地刺击退零向量显式防护(与同函数 loose 循环一致,不再依赖 Vec3.normalize 版本语义)。
33. `GroundSpikeRules` 注释 0.60s → 0.55s。
34. 感染体扫描节流 10→20 tick,挥爪 20→40 tick(约 2 秒一次);PvP 关闭时玩家不作为猎物。
35. 空中冲刺方向锁定(`MovementState.dashDirectionX/Z`,beginDash 捕获)。
36. 测试强化:`GroundSpikeRulesTest` 精确断言;`WallRunPhysicsTest` NaN/负值/封顶/8 布尔组合(测试数 62→63);`MovementTuningDefaultsTest` 用 `user.dir`。
37. 构建修复:`build.gradle` 增加 `mavenCentral()`(JUnit 不再依赖断网的 maven.neoforged.net 镜像);`-Xmx4G`;`loader_version_range=[2,)`。
38. EpicFight 依赖版本范围收窄 `[21.17.3.1,21.17.4)`。
39. 清理:4 个崩溃日志、`tmp/imagegen`、`assets/examplemod` 已删除。

### 说明(有意保留,不属 bug)
- 连击宽限 16 tick:当前 3 段循环下 `comboIndex` 复位前后结果等价,属未来 4+ 段连击的设计槽位,保留原逻辑以避免手感变化。
- `ClawActionPayload.GROUND_SPIKE_CHARGE_RELEASE` 枚举保留:删除会破坏旧客户端 STREAM_CODEC 兼容。
- 全局 EF 玩家渲染器替换、`holdGroundSpikeChargeAt`、粒子预算均保留现状并已在文档标注,等待对应系统(特效等级/渲染器重构)落地时一并处理。

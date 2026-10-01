# 安装、升级与故障排除

适用：Blacklight: Ascension 0.2.0-rc.57 · Minecraft 1.21.1 · 作者：末者。当前是发布准备阶段，尚无已放行的公开正式游戏包。

## 先确认拿到什么文件

| 文件后缀 | 用途 | 当前能否直接安装试玩 |
| --- | --- | --- |
| -local-preview.jar / .zip | 含本地私有动作、镜头等资料的预览包 | 配齐依赖后用于本地试玩；未作为公开发行物 |
| -source-only.jar / .zip | 默认源码编译结果，不含私有资料 | **不能视为可运行的完整 Mod**；当前初始化仍要求完整动作资料 |
| -sources.zip | 原始源码快照 | 不是 Mod JAR，不能放入 mods |

完整文件名包含 Minecraft 与 Mod 版本，例如 `Blacklight-Ascension-MC1.21.1-NeoForge-0.2.0-rc.57-local-preview.jar`。私有资源授权尚未明确，请勿把 local-preview 上传到公开下载平台或公共整合包；来源与范围见 [资源说明](../THIRD_PARTY_ASSETS.md)。

普通构建缺资料时会出现 `Incomplete motion profile` 或 `Missing original traversal motion`，不是安装 Java 或更新前置就能解决。正式公开版本仍需解决可分发资源和独立运行方案。

## 必需环境与依赖

| 项目 | 本项目当前构建版本 | 安装说明 |
| --- | --- | --- |
| Minecraft Java 版 | 1.21.1 | 不是基岩版，也不是 Forge / Fabric 实例 |
| Java | 21 | 在启动器中选择对应 Java 21 运行时 |
| NeoForge | 21.1.244 | 先建立该版本的游戏实例 |
| GeckoLib | 4.9.2 | 单独安装面向 Minecraft 1.21.1 的 NeoForge JAR |
| Epic Fight | 21.17.3.1 | 单独安装面向 Minecraft 1.21.1 的 NeoForge JAR |
| Blacklight: Ascension | 0.2.0-rc.57 | 当前本地试玩使用资料完整的 local-preview 包 |

Blacklight 不内嵌这些前置。所核对的 Epic Fight 与 GeckoLib JAR 没有额外的必需 Mod；GeckoLib 4.9.2 明确声明与 `geckoanimfix` 不兼容。

当前元数据允许 NeoForge 21.1.244 及以上、GeckoLib 4.9.2 及以上，Epic Fight 范围为 21.17.3.1（含）至 21.17.4（不含）。这不等于所有后续版本都通过验收；首次安装优先使用上表的构建版本。

## 可选联动

| Mod | 当前使用版本 | 当前接受范围 | 缺少时的边界 |
| --- | --- | --- | --- |
| ParCool | 3.4.3.3 | 3.4.3.3（含）～3.5（不含） | 不要求安装；Blacklight 原生移动仍是自身系统 |
| Epic ParCool | 21.0.0 | 21.0.0（含）～21.1（不含） | 不要求安装；联动需要匹配的 ParCool 和 Epic Fight |
| Weapons of Miracles | 2.0.178 | 2.0.178（含）～2.1（不含） | 可选依赖动作退回项目已有片段，不补齐缺失的私有动作资料 |

可选表示可以不安装，**不表示安装任意版本都兼容**。不要把 ParCool 4 放入当前 3.4 联动方案。第三方 JAR 应从其官方发布页单独取得；不会随 Blacklight 安装 ZIP 附送。首次定位问题可只保留必需依赖，再逐个加入可选联动。

## 首次安装

1. 退出游戏，建立 Minecraft 1.21.1 / NeoForge 21.1.244 实例，并确认启动器使用 Java 21。
2. 将 GeckoLib、Epic Fight 和当前 local-preview JAR 放入该实例的 mods 文件夹。若拿到 ZIP，只取其 mods 中的 JAR 放入游戏实例；README、docs 和校验文件用于查阅。
3. 保证 mods 中只有一个 Blacklight 版本。不要把 source-only 和 local-preview 两个 JAR 同时放入。
4. 删除旧的 `-Dblacklight.localWeaponMotions=...` 启动参数，除非你正在有意使用一套完整的开发资料。显式目录会替代整套包内资料；某个文件缺失时不会自动借用包内同名文件。
5. 使用新建的试玩世界或存档副本进入游戏，核对 Mod 菜单版本和控制设置。当前安装路径未作为 RC57 完整人工验收通过，发现问题请保留日志。

Gradle 的标准输出位于 build/libs 与 build/distributions；本机整理的本地预览目录为 release/local-0.2.0-rc.57。不要把其他历史 release 或 build 目录中的旧 JAR 混装。

## 升级与回退

1. 完全退出客户端和正在使用该存档的服务器。
2. 备份整个世界目录及实例 config；保留旧 JAR、依赖版本和启动参数。多人由服主在服务器停止后备份世界。
3. 移除旧 Blacklight JAR，安装新版本，并按此次安装说明核对前置。客户端与服务端应更新为相同的 Mod 版本和资料方案。
4. 在存档副本中检查重进、重生、升级状态、正在成长的宿主与身份保存，再决定是否替换常用实例。
5. 回退时一起恢复备份世界、配置和旧 Mod；不要假定新版本写入后的存档能直接交给旧版读取。

研究站和巨巢不会自动补建到已生成区域；旧世界需要探索新区域。旧制图师已保存的交易不会自动更新，应寻找尚未生成相应交易的雪地制图师。不要为了调参删除整个世界或全部配置。

## 多人和服务端

Blacklight 及其必需依赖需要在客户端与服务端同时安装。保持精确 Mod 版本和动作资料一致；只让一端使用私有资料会使动画时序、判定或资源要求不一致。当前不能用 source-only 服务端与 local-preview 客户端拼接成已支持方案。

专用服务器启动、远程玩家动画、重连与高延迟行为仍待 [人工验收](ACCEPTANCE_0.2.0.md)。旧版本曾成功启动或单人可以游玩，不代表本版多人已经通过。local-preview 含私有资料，当前不作为公共服务器或公开整合包的分发方案。

## 常见问题

| 表现 | 先核对 |
| --- | --- |
| 启动提示缺 GeckoLib / Epic Fight | 是否装入正确实例，是否为 NeoForge 1.21.1 对应版本，是否混入 Forge/Fabric JAR |
| 提示依赖版本不匹配 | 使用本文构建版本；可选模组存在时也要匹配接受范围 |
| Incomplete motion profile / Missing original traversal motion | 是否误装 source-only，或启动参数指向旧、不完整的动作目录；不要只补一个文件后声称整套资料完整 |
| 镜头或血条与本地预览不同 | 镜头依赖 camera-profile / camera-events 等资料；HUD 缺失会退到自制比例条。完整动作缺失仍可能直接阻止启动 |
| 调整 F8 后方向或动作异常 | 先记录改动和当前配置，使用面板已有的恢复默认/撤销操作，再从默认值复查；不要混入旧版本的调参快照 |
| E 没反应或打开背包 | 核对黑光模式与控制冲突；确认目标属于可吸收名单、视线/距离/削弱条件满足 |
| 重生或重进后模型/动作异常 | 记录世界、Mod 与依赖版本，保留 latest.log；按当前验收项复现，不用清空存档掩盖问题 |
| 巨巢定位久、放置失败 | 用 /blacklight hive locate 与 status 查看分段任务；放置需完整范围加载、干地和安全空间，错误信息会指出原因 |
| 吞噬者巨大却未完成 | 用 /blacklight colossus_status 查看阶段、生物质、节点和核心；完成前拆核心会衰退，体型不是完成凭据 |
| 怪物不攻击 | 核对创造/旁观模式、难度、伪装及世界规则；旧测试世界设置可能不同于新生存世界 |

蜂巢搜索只返回位置，可能标为尚未加载验证的候选，不自动传送。人工放置会改造 113×113 格范围；任务取消不会撤销已有方块，请只在已备份的空地使用。加速只影响指定巨巢，不提高世界 tick 速度，也不补送生物质。

报告问题时提供：完整文件名、Minecraft/NeoForge/前置版本、单人或服务端、可复现步骤、截图以及 latest.log 或 crash-reports 中对应报告。发布日志前去除聊天、地址等不希望公开的信息。

## 本轮验证边界

本轮维护安装与发布说明，不自动启动游戏、测试、GameTest 或碰撞测试。编译结果、游戏验收和公开资源授权分别记录；任何一项通过都不代替另外两项。

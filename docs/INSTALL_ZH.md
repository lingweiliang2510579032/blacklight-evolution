# Forge 1.20.1 开发版安装边界

适用：Blacklight: Ascension 0.3.0-forge-1.20.1-dev.1 · Minecraft 1.20.1 · Forge 47.4.10 · Java 17。

当前只有 source-only 源码候选。公开仓库没有完整动作、HUD 和镜头资料；独立客户端实际加载后以 `Missing Blacklight motion: basic_idle` 停止。不能把它当作可玩版本放进整合包。结果见 [移植状态](FORGE_1_20_1_STATUS.md)。Epic Fight、Epic ParCool 和 Weapons of Miracles 不是本分支前置。

## 安装文件

- `Blacklight-Ascension-MC1.20.1-Forge-0.3.0-forge-1.20.1-dev.1-source-only.jar`：已重混淆的源码候选，不含完整动作资料。
- `geckolib-forge-1.20.1-4.8.4.jar`：需要单独安装的 GeckoLib。
- MixinExtras Forge 0.4.1：已内嵌，由 Forge 自动加载。
- `source-only`：不含完整私有动作资料，仅用于源码编译，不是完整试玩包。

## 获得完整且获准使用的资料后

1. 在本地以 `-PbundleMotionDirectory=<完整资料目录>` 构建 local-preview。保留私有资料标记，不公开上传。
2. 建立独立 Minecraft 1.20.1 / Forge 47.4.10 实例并使用 Java 17，放入一个 Blacklight local-preview JAR 和对应 GeckoLib。
3. 首次验证先使用独立实例或已有存档副本。不要在 RC59 常用实例中混装旧黑光、新黑光与旧动作前置。
4. 删除旧的 `-Dblacklight.localWeaponMotions=...` 参数，除非正在有意使用完整的外部开发资料目录。该目录会替代包内资料。
5. 检查模组菜单中的版本和控制设置，先确认普通模式、B 黑光模式、五种攻击形态、装甲、盾牌、抓取吞噬和墙面动作。

这些是待执行的验收条件，不表示当前 source-only 候选可玩。ParCool 1.20.1-3.4.3.3 可选；黑光原生移动不要求安装。额外 Mod 尚未通过兼容验收。

## 升级与回退

先备份整个世界和 config，并保留 RC59 旧包。试用独立版时使用副本；回退时一起恢复旧世界、配置和旧包。新的动作附件不表示旧存档中的动作前置内容可以由黑光替代。

客户端与服务端必须使用相同黑光版本和完整动作资料。Forge 专用服务端、远程玩家、高延迟、重连、多人观战和完整生存流程尚未通过玩法验收。上游的启动记录不能替代本分支验收。

## 故障排查

| 表现 | 检查 |
| --- | --- |
| 提示缺 GeckoLib | 必须使用 Forge 1.20.1 的 4.8.4 版本 |
| 提示缺 Epic Fight | 是否仍在运行 RC59 旧 JAR，或存在另一个附属模组 |
| Missing Blacklight motion / Incomplete motion profile | 是否误装 source-only，或外部动作目录不完整 |
| E 打开背包 | 抓取与背包绑定冲突，检查控制设置 |
| 装甲、墙跑或跑步画面异常 | 保存截图、具体动作、完整文件名及 latest.log，以同一状态复现 |

独立版 local-preview 仍包含私人 Prototype 派生资料，发行边界没有因框架独立化而改变，见 [资源说明](../THIRD_PARTY_ASSETS.md)。本地修复与公开发布是分别记录的状态。

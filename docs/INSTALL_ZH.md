# 安装 Blacklight: Ascension — Forge 1.20.1

适用版本：0.3.0-forge-1.20.1-dev.3，Minecraft Java 1.20.1、Java 17、Forge 47.4.10。

## 文件和依赖

- 安装 `Blacklight-Ascension-MC1.20.1-Forge-0.3.0-forge-1.20.1-dev.3-bundled.jar`；默认包含 224 个动作、镜头和 HUD 资料文件。
- 单独安装 GeckoLib 4.8.4 的 Forge 1.20.1 版。
- ParCool 1.20.1-3.4.3.3 可选。Epic Fight、Epic ParCool、Weapons of Miracles 均不是前置。
- MixinExtras Forge 0.4.1 内嵌，不需要另外放入 mods。
- `bundled-slim.jar` 是构建中间文件；旧 dev.1 source-only 包不可代替本版。

## 独立实例验证

1. 建立 Minecraft 1.20.1 / Forge 47.4.10 独立实例，选择 Java 17。
2. 把一个本版 bundled JAR 和 GeckoLib 放入 mods，移走旧 Blacklight 版本，避免重复模组。
3. 先验证菜单与独立测试存档，再检查 B 模式、武器切换、吞噬、移动、HUD、镜头、感染体和存档重载。
4. 多人需客户端与服务端使用相同模组版本、依赖和资料；联机仍需专门验证。不要直接用现有整合包存档测试开发版。

Forge 编译、启动与玩法结果见 [验收状态](FORGE_1_20_1_STATUS.md)。以上是安装与验证步骤，不表示全部场景已经通过。

## 外部整套资料

源码构建默认使用仓库 `art/motion-profile/`。开发者可用 `-PbundleMotionDirectory=<目录>` 替换完整资料，生成 `external-profile`；此选项不混合两个资料目录。游戏运行时的 `blacklight.localWeaponMotions` 仍是显式开发覆盖。

## 常见问题

| 现象 | 检查 |
| --- | --- |
| 缺 GeckoLib、无效模组文件 | 是否选择 Forge 1.20.1 版 GeckoLib 和本版完整 JAR |
| 重复模组 | mods 仅保留一份 Blacklight |
| Missing Blacklight motion / Incomplete motion profile | 是否误装旧 source-only；外部完整资料是否缺文件；成品是否通过打包清单校验 |
| 联机或额外模组冲突 | 先在最小独立实例复现，保留脱敏日志与准确版本 |

资源出处与许可见 [资源说明](../THIRD_PARTY_ASSETS.md)和[第三方声明](../THIRD_PARTY_NOTICES.md)。本地开发候选与正式发布分别记录。

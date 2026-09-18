# Blacklight Evolution · 黑光进化

受 Prototype 启发的 Minecraft 1.21.1 / NeoForge 模组，欢迎共同开发。

**当前为实验性原型**：可以体验利爪战斗、蓄力跳跃、墙面移动和滑翔；模型、动画、手感及多人同步仍在迭代。这不是官方 Prototype 项目，也不承诺逐帧复刻原作。

**Prototype-inspired Minecraft mod for NeoForge. Experimental; code, animation, modeling and testing contributions are welcome.**

## 一起开发

欢迎程序员、模型/动画作者、特效作者和测试玩家参与。先阅读 [贡献指南](CONTRIBUTING.md)，通过 Issue 讨论一个具体问题，再提交 PR。

目前优先方向：

- 攀爬、墙跑、沿墙跳跃和翻上屋顶之间的动作衔接。
- 利爪与头部、身体的空间关系，以及多个视角下的表现。
- 凹凸墙面、转角、战斗打断和联网延迟下的移动行为。
- 战斗命中反馈、复现步骤和中英文文档。

## 环境与构建

| 组件 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.244 |
| Java | 21 |
| Epic Fight | 21.17.3.1 |
| GeckoLib | 4.9.2 |

Windows：

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
```

Linux / macOS：

```sh
bash ./gradlew test build
bash ./gradlew runClient
```

首次构建需联网获取依赖。安装包输出到 `build/libs/`；正式游戏实例的客户端和服务器都需要安装 Blacklight Evolution、Epic Fight 与 GeckoLib。不要同时安装两份 Blacklight JAR。

## 抓取与吸收

E 只抓取，目标会保持完整并挣扎，等待右键吸收或左键/再次 E 投掷。持有时可步行转身；详细流程和限制见 [R11 抓取说明](docs/GRAB_R11.md)。

## 试玩操作

- `U` 解锁武器形态，`Z` 切换徒手、利爪、锤拳、刀刃和鞭拳，面对目标按 `E` 抓取，再右键吸收；左键或再次 `E` 投掷。
- 锤拳将目标完整砸倒后吸收；鞭拳挥臂甩索、缠住后拉回。人形、四足与猎手有对应身体反应。
- 普通动物可直接吸收；支持的敌对生物与猎手需生命不超过 30%。完整规则、限制与开发入口见 [R10 吸收说明](docs/CONSUMPTION_R10.md)，[实机录像](docs/media/grab-r11.mp4)。
- 感染僵尸会异化为独立黑光猎手；分阶段感染、利爪变形及可选跑酷兼容见 [感染体说明](docs/INFECTED_PROTOTYPE.md)。
- 创造模式取得黑光病毒安瓿并投掷；也可使用调试命令 `/blacklight infect`。
- `B` 切换原版/Prototype 模式，`V` 展开或收回利爪。
- 左键利爪连击；长按右键使用地刺。部分技能需先解锁。
- 地面按住空格蓄力，松开起跳。
- 加速贴墙进入墙跑；停止移动时抓墙停住。
- 墙上 `W/S` 上下攀爬、`A/D` 横移；加速恢复墙跑，空格沿墙向上跳，`Shift` 离墙。

Prototype 模式会限制普通挖掘、放置和物品交互；按 `B` 返回原版模式恢复这些操作。

## 验证与素材工作流

当前 R11 通过 85 项单元测试和 20 项 Minecraft GameTest，并实机录制七组吸收场景。特殊模组生物、所有相机角度和多人延迟场景仍需持续测试。网络协议为 10，联机双方需安装相同版本。

墙面物理测试使用独立的测试世界：

```powershell
.\gradlew.bat runGameTestServer -PwallTraversalTest
```

利爪模型和动画创作流程见 [Blender 工作流](docs/ASSET_WORKFLOW.md)。正常编译与运行不需要 Blender。项目保留创作脚本和运行资源；第三方参考骨架、比例模型及其导出工具由开发者在本机准备。

更多资料：[动画架构](docs/ANIMATION_ARCHITECTURE.md)、[已知问题](docs/KNOWN_ISSUES_AND_SLOTS_ZH.md)、[规划](docs/ROADMAP_ZH.md)。历史设计文档中的目标不等于当前已实现功能。

## 许可证

项目原创代码、文档、模型和制作的动画采用 [GPL-3.0-only](LICENSE)。贡献者提交的改动按同一许可证提供；第三方内容保留其原许可与声明，见 [素材来源](THIRD_PARTY_ASSETS.md)、[第三方声明](THIRD_PARTY_NOTICES.md) 和 `TEMPLATE_LICENSE.txt`。

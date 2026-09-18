# 参与 Blacklight Evolution

欢迎参与代码、建模、动画、特效和测试。项目目前处于原型迭代阶段，目标是制作受 Prototype 启发的 Minecraft 模组。提交功能前请先说明你要解决的问题；大型改动先讨论设计和分工，避免多人同时重做同一套动作。

项目原创内容采用 [GPL-3.0-only](LICENSE)。提交贡献时，请确认你有权按此许可证提供这些内容；第三方文件保留原有许可和署名。

## 开发环境

- Java 21，使用仓库内的 Gradle Wrapper。
- Minecraft 1.21.1、NeoForge 21.1.244；GeckoLib 和 Epic Fight 由 Gradle 获取。
- Windows：`./gradlew.bat test build`；Linux/macOS：`bash ./gradlew test build`。
- 启动开发客户端：`./gradlew.bat runClient`；Linux/macOS 替换为 `bash ./gradlew`。
- 第一轮构建需要网络下载依赖。不要提交 `.gradle/`、`build/`、`run/`、存档或个人账户配置。

## 目前适合参与的工作

- **动画与建模**：墙跑、慢速攀爬、静止抓墙和沿墙跳跃之间的衔接；利爪与头部、身体的空间关系；多个观察角度下的动作表现。
- **移动与战斗**：联网延迟下的客户端预测和服务端状态同步；转角、凹凸墙体、墙顶及战斗打断。
- **测试**：复现手脚反向、模型穿插、动画重播和卡墙问题，提供操作步骤及短视频。
- **文档与翻译**：安装说明、操作说明及中文/英文文本。

## 代码与素材入口

- `src/main/java/com/codex/blacklight/movement/`：移动逻辑；`WallTraversal` 为客户端和服务器共用的墙面状态机，`WallContacts` 负责方块接触检测。
- `src/main/java/com/codex/blacklight/client/`：客户端预测、动作选择及渲染。
- `src/main/java/com/codex/blacklight/integration/epicfight/`：Epic Fight 动画适配。
- `src/main/resources/assets/blacklight/animmodels/animations/biped/`：游戏使用的烘焙动画 JSON。
- `tools/`：Blender 创作、烘焙及预览脚本。脚本需要额外的骨架、插件源码或 `.blend` 输入，目前尚不是新克隆仓库即可完整重建美术资产的流程；开始美术工作前请向维护者确认源文件。
- 现有第三方声明见 `THIRD_PARTY_ASSETS.md`、`THIRD_PARTY_NOTICES.md` 和资源目录内的许可文件。

## 提交改动

1. 从主分支建立自己的功能分支；一个改动集中解决一个问题。
2. 描述修改前后的行为，附复现步骤。动作和模型改动附正面、侧面及运动中的视频，并提供可编辑源文件和导出结果。
3. 运行 `test build`。改动墙面物理时，额外运行 `runGameTestServer -PwallTraversalTest`；测试使用独立的 `run-wall-gametest/` 世界。
4. 在 PR 中填写验证结果、未验证项和素材来源。服务端裁定伤害与移动规则，不能只改客户端表现而遗漏同步。
5. 不上传从商业游戏提取的模型、纹理、音频或完整参考视频。原创或第三方资产应注明作者、来源和许可证，保留要求的声明。

当前基线：R11 有 85 项单元测试和 20 项 GameTest 通过；七组实机录像见 [吸收说明](docs/GRAB_R11.md)。这不等于所有动作、联网场景与真实手感均已验收。

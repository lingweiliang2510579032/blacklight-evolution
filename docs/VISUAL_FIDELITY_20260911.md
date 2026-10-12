# Blacklight：原版风格视觉预览

日期：2026-09-11。源码基线：`8243221`。本次改动已经写入 D 盘的 Blacklight 项目。

## 对照依据

实际查看了 [《Prototype》Claws & Ground Spike 高清演示](https://www.youtube.com/watch?v=qnbyGiuyh1k)，以及 [同段演示](https://www.youtube.com/watch?v=4Nelt-hUIm8) 的连续帧。重点是约 7–9 秒的抬臂、单爪插地、另一只手平衡、地刺破土与持有姿态。

据画面观察，本轮针对黑灰生物组织、灰白棱边、稀疏暗红缝隙、不规则地刺倒刺进行修改。原版影像只用于观察，没有把视频、截图或原游戏模型打进模组。

## 已改动

- 前臂：方盒替换为八面、六截面的体积，保留原先调整好的左右手绑定位置。
- 爪指：三段菱形截面，尖端收为点；各指节错峰展开，腕部旋转以实际爪根为支点。
- 地刺动作：蓄力标记从 0.18 秒移到 0.24 秒，近接地标记从 0.45 秒移到 0.48 秒；真正接触保持 0.55 秒。缩小提前捕获定格的窗口，减少下刺尚未结束就跳到接地帧的现象。
- 地刺模型：保留高大轮廓，改为六级收尖，并加入稀疏侧面倒刺；各主刺错开少量时间破土，22 tick 内结束。
- 材质：独立的 `claw_biomass.png` 用于利爪与地刺；使用内置 imagegen 生成黑灰纤维、细微暗红缝隙的生物质贴图。原有角色通用贴图保留。
- 特效：削减白色剑气、风团和亮点，碎屑向外、向上飞散；修正烟雾透明度在第一 tick 突然增亮的问题。

墙跑实现、左右手调试配置、攻击伤害与范围未改动。

## 验证

- Gradle `build` 通过，69 项测试，0 失败、0 错误。
- 分节地刺测试现在检查整体高度、每根主刺从地下出现及在生命周期结束前收回；保留原有单右爪、下蹲和身体方向检查。
- 静态资源检查通过：骨骼父子引用、关键帧顺序、有效数值、UV 边界和 1.1 秒生命周期。
- `模型轮廓对比.png` 是根据模型几何绘制的示意图，使用统一材质，**不是游戏截图**。

## 仍待实机验收

- 右爪实际接地角度与手指穿地深度，正面、侧面和背面都要看。
- 新材质在不同光照和第三人称距离下的观感、游戏帧率。
- 联机远端玩家的定格/拔爪同步。本轮没有重做原有本地暂停状态机。
- 生物手臂仍沿用项目现有的上臂绑定，尚未改成完整的肘部蒙皮。

这是向原版靠近的预览版，不是已获视觉验收的最终还原版。

## 文件

- `blacklight-0.2.0-mvp.5-visual-preview-20260911.jar`：独立预览包。
- `模型轮廓对比.png`：修改前后几何对比。
- `claw_biomass.png`：已接入项目的生成贴图。

包内模组 ID 和版本仍是 `blacklight / 0.2.0-mvp.5`。手动安装时替换旧 Blacklight JAR，不要同时放两个 Blacklight 包。原有回退包未覆盖。

## 贴图生成记录

方式：内置 imagegen。输入截图仅作为材质与配色参考。

提示词：

> Use case: stylized-concept. Asset type: production seamless albedo texture for a Minecraft NeoForge biological claw-arm and groundspike model, not a concept image. Generate one square 1024x1024 opaque edge-to-edge seamless material tile. Subject: dense layered black organic keratin and sinewy biomass, inspired by the black claw-arm material in the supplied Prototype 2009 gameplay reference image. The supplied image is ONLY a material/color reference, do not copy its composition, character, HUD, environment or watermark. Material consists of mostly longitudinal irregular intertwined ridges and dense fibrous grooves, slightly diagonal and flowing, like black tendon tissue hardening into keratin. Palette: charcoal black, graphite gray highlights on raised ridges, extremely subtle deep oxblood/maroon in sparse crevices, less than 5% red; no emissive red, no glowing veins, no lava cracks, no rock cobblestones, no neon. Legible medium-scale ridges so it still reads well on low-poly 3D geometry. Texture must have mid-dark gray raised areas (not all pure black) for actual in-game lighting to remain legible. Flat orthographic material texture without perspective, objects, lighting hotspots, shadows, text, borders or logos. Seamless repeating full-bleed tile. This image will be used directly as an albedo map on existing UVs.

实际生成尺寸为 1254×1254；以原始 PNG 接入，未额外修改图像。

## 客户端启动检查

2026-09-11 17:43，开发客户端已进入“新的世界”，日志记录了利爪变形的正常触发；未发现本次新贴图或模型的加载错误。日志另有感染效果图标缺失及 Epic Fight 字幕、可选披风层、远程服务提示，不属于本轮新资产。尚未完成本次地刺的实际释放、三视角录像与双客户端验收。

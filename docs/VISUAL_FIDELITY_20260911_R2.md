# 利爪视觉预览 r2

2026-09-11。针对实机截图中手臂与肩膀断开、管状端面外露、写实贴图与 Minecraft 不协调的问题修正。此版本替代此前的 visual-preview 包。

## 本轮修正

- 上臂直接绑定 Epic Fight 的 Arm_R/L 肩关节，前臂绑定 Hand_R/L 肘关节，分别跟随动作。移除旧的逆绑定偏移和手动整臂平移；肩部与肘部的截面相互覆盖。
- 前臂长度按当前依赖骨架的肘到腕距离调整，变形缩放从肘部开始，避免整段缩放把连接点拉走。
- 材质替换为较大的黑灰像素色块和稀疏暗红接缝，减少细碎写实纹理。利爪和地刺共用新材质。
- K 调试器改为左右手独立的“手臂粗细”和“爪刃长度”，保持关节位置固定。旧平移、旋转和整臂缩放参数仍留在配置文件中，供回退旧实现；分节模型不再使用它们。
- 爪腕转向时间与新的下刺关键帧 0.24–0.48 秒保持一致。

前一轮的尖爪、分节地刺、破土节奏及碎屑调整继续保留。参考过《Prototype》的 [Claws & Ground Spike 演示](https://www.youtube.com/watch?v=qnbyGiuyh1k)，没有把原游戏资产放入模组。

## 验证与限制

- Gradle build 通过，69 项测试，0 失败、0 错误。
- 查验所安装 Epic Fight 骨架及矩阵计算：Arm_R/L 是肩部，Hand_R/L 是肘部，Tool_R/L 是腕部；姿态矩阵已经包含父子关节变换。
- 上述是编译、资源及绑定计算的检查，不能代替实机外观验收。仍需观察站立、弯肘、挥爪、插地时的连接和穿插情况，以及不同光照下的材质。
- 本轮没有完成双客户端动作同步验收。

## 交付文件

- `blacklight-0.2.0-mvp.5-visual-preview-20260911-r2.jar`
- `claw_biomass-pixel.png`

安装时用 r2 替换旧 Blacklight 包，避免同时放入两个同 ID 模组。此前的模型对比图来自旧版本示意，不代表 r2 实机效果。

## 贴图生成记录

工具：内置 imagegen。以上一版材质为输入，生成新 PNG；直接接入原始结果。

提示词：

> Edit this material tile into a Minecraft-compatible low-resolution pixel-art game texture. Keep charcoal black / muted graphite / sparse very dark maroon palette. REMOVE ALL tiny fibrous lines and photorealistic detail. Use a 32 by 32 logical pixel grid, with large clearly square pixels, as if an actual 32x32 Minecraft texture enlarged with nearest-neighbor. Only 6 flat colors: dark charcoal, medium charcoal, muted cool gray, dark burgundy, near-black, gray highlight. Broad simple diagonal keratin plates and a few dark maroon seams. Matte diffuse surface. Each plate should span several large square pixels. No anti-aliasing, no gradients, no microscopic linework, no noisy dithering, no smooth rendered surfaces, no lighting hotspots. Must blend with ordinary Minecraft block and player textures. Square seamless tile, fully opaque edge to edge. Do not draw a character, a 3D model, borders, labels or text. The purpose is to replace the visually noisy realistic texture on a low-poly claw arm.
客户端启动记录：17:56，r2 开发客户端成功进入存档。此为启动检查，尚未取得 r2 实机外观截图。

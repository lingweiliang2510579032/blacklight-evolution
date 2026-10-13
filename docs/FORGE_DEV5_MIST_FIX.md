# Forge 1.20.1 dev.5：修正红雾变黄绿色面片

## 问题与原因

dev.4 的实际截图中，俯冲红簇/红雾变成了黄绿色不透明矩形。此前技能流程与 Shader ready 检查通过，但未发现这处画面错误；不能用这些通过项代替颜色/透明度验收。

1.20.1 ShaderInstance 只有在 JSON 存在 attributes 数组时，才会在链接前按 VertexFormat 绑定属性槽。impact_mist 缺失该字段，GL 自动分配的 Color/UV0 顺序与 POSITION_TEX_COLOR 不同，导致 UV 被当作颜色、颜色被当作 UV。相同缺项也存在于 liquid_biomass 和 rigid_biomass，全部补齐各自格式声明，避免依赖驱动分配顺序。

第二处差异是 1.20.1 ShaderInstance.apply() 会应用 JSON 的混合模式；缺少 blend 时会覆盖 RenderType 的透明状态，形成不透明红片。三个透明材质同时补齐 srcalpha / 1-srcalpha 混合，使烟雾渐隐和液体变形透明度生效。

## 修正及验证方法

- 不改技能威力、50 格红雾阈值或地形算法。
- 为三个核心 Shader 补充属性声明；增加配置回归。
- 实际客户端检查 glGetAttribLocation：红雾 UV0 槽 1、Color 槽 2。
- 重跑 20 格红簇、80 格红雾及地面触须，并直接检查实际落地截图。
- 可玩包升级到 dev.5；dev.4 仅留作历史记录，不再作为推荐安装包。

测试工具另修正一处等待条件：实际地形测试需等第 16 块触发开关注入，不能在首个受耗时预算限制的 tick 就提前断言已取消；取消语义、阈值和生产逻辑未改变。

## 最终结果

`forge-dev5-mist-final/native-skills-report.json`：complete/passed，ground_barrage、bulletdive_20、bulletdive_80 三场景通过；GPU 属性槽断言无异常。已直接检查 80 格正面和近景落地截图，颜色恢复红色，背景可透过烟雾，矩形硬边和不透明红片消失。此前仅颜色修复的 v2 截图不计作最终视觉通过。

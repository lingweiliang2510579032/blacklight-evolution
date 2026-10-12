# dev.5 目标红色轮廓与试玩视野

有效锁定目标重新显示红色实体外轮廓。通过 Minecraft 原生 outline pass 绘制，仅在本地目标对应的渲染调用中选择红色；不写实体发光标记或队伍颜色。取消锁定、目标死亡或换世界时，轮廓随有效目标解除。其他实体原有发光效果保留。

普通试玩目录的 options.txt 曾为 fov:-0.75（40°），原项目为 fov:0.0（70°）。本次正常保存退出后，仅将试玩配置的这一个选项恢复为 70°，其余设置保留；备份位于 D:/Blacklight-Standalone/tmp/options-before-fov-fix-20261012-183001.txt。安装包不包含 options.txt，也不在运行时强制修改玩家的视野设置。镜头朝向控制未在本次改动。

实机验证在独立的 run-native-qa 存档副本进行：真实 Tab、服务器 ACK、连续取消、服务器拒绝、F5 三种视角、死亡重生与原生描边均通过，0 异常。锁定实体的原始发光标志仍为 false，旁边普通实体无轮廓，原版发光实体保留白色。相同第一人称相机下，目标头部轮廓区域（排除 HUD）锁定时有 386 个红色像素，取消后为 0。

本次聚焦 TargetLockRequestStateTest、WorldHudProjectionTest 共 5 项通过，assemble 和 verifyStandaloneBoundary 通过。dev.4 的完整单元测试为 183 项、176 通过、7 个 RC59 既有失败；本次未重复整套测试或旧 GameTest，不将该历史结果计作 dev.5 全量验收。

# 模型与动画创作

游戏直接使用 `src/main/resources/assets/blacklight/` 内的模型、纹理和烘焙动画；安装和编译 Mod 不需要 Blender。

利爪与动作可通过仓库里的 Python 脚本生成 Blender 工程。制作流程验证环境为 Blender 5.2.1；导入/导出工具使用 [Antikythera Studios Blender JSON Add-on](https://github.com/Antikythera-Studios/blender-json-addon) 的提交 `b9c6844193074f8c21b35513052d61c82cc2c207`。

## 本地准备

1. 构建项目以下载声明的 Epic Fight 依赖，或从其官方发布页获取对应版本。
2. 将上面的导出工具克隆到任意本地目录并切换到指定提交。
3. 执行 `python tools/prepare_blender_inputs.py <Epic-Fight-JAR路径> <导出工具目录>`。

这一步仅从开发者本机的依赖中准备参考骨架与比例模型，保存在已忽略的 `art-work/`。Epic Fight 的代码与资产采用不同许可；这些参考文件、导出工具副本及含第三方参考模型的 `.blend` 不作为本项目原创素材公开分发。

## 生成可编辑工程

以下命令中的 `blender` 应替换为本机 Blender 可执行文件。运行会重新生成仓库中的运行模型与动画，建议在单独的工作分支操作。

```sh
blender -b --python tools/build_claw_blender.py -- art-work art-work/base
blender -b art-work/base/claw-study.blend --python tools/bake_claw_playback.py -- art-work/base
blender -b --python tools/author_wall_traversal.py -- art-work art-work/base art-work/wall
blender -b art-work/wall/claw-study.blend --python tools/bake_claw_playback.py -- art-work/wall
blender -b art-work/wall/claw-study.blend --python tools/render_claw_blender.py -- art-work/preview living/wall_
```

`claw-study-authoring.blend` 保留 IK；`claw-study.blend` 将 23 段动画烘焙为可独立选择的 Actions。导出采用 100 FPS 的时间基准，保持服务器既有动作时长。

## 验证约定

- 正面为 Blender +Y；不要凭没有纹理的人体判断正反面。
- 保持右/左手拇爪的正确位置。手爪必须避开头部和躯干，并在多个视角下检查。
- 导出四元数保留 Blender WXYZ，避免重复反转坐标。
- 执行 `test build`，检查骨架完整性、关键帧时长、单位四元数、落脚和墙面动作的头部间距。
- 目前脚本在已有编辑工程上完成过验证；从全新环境完整重建资产还应单独核对输出差异。不要把通过数据测试等同于已完成实战动作验收。

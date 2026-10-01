# RC57 原动作包发布记录

2026-10-01，按作者末者明确要求，将现有 RC57 完整包发布至 [GitHub Release](https://github.com/mengyuyuan/blacklight-evolution/releases/tag/v0.2.0-rc.57)。保留原有动作、镜头、HUD 和机制，没有使用已撤回的 RC58 替代动作方案。本轮发布不修改 JAR 或安装 ZIP 的内容。

## 必需前置

Minecraft Java 1.21.1、Java 21、NeoForge 21.1.244。

- Epic Fight 21.17.3.1：NeoForge / Minecraft 1.21.1 版本，必需。
- GeckoLib 4.9.2：NeoForge / Minecraft 1.21.1 版本，必需。
- ParCool 3.4.3.3、Epic ParCool 21.0.0、Weapons of Miracles 2.0.178：可选联动。

将本模组 JAR 和两个必需前置一起放入 mods，只保留一份本模组；前置 JAR 未打入本包。完整包已含 224 个运行时动作/表现资料文件，无需另设本地资料目录。

## 文件与核对

文件名同时包含 Minecraft 1.21.1 和模组 0.2.0-rc.57。`local-preview` 为原构建标记；此次原样上传，没有借改文件名替换内容。三个文件已对照本地 SHA256SUMS 和 GitHub 返回的摘要核实：

| 文件 | SHA-256 |
| --- | --- |
| local-preview.jar | ac81f97ba47c20a9cfeddc3e99d93c868de4dd2334926ef71a37372781402f26 |
| local-preview.zip | 6618b8189c02ad5a4a50c0fa0958620198998fb9c5b9f740f6b92115b6b24353 |
| sources.zip | ca8d618f4ed657ed26a91f5790c91e5904bf0e210faf457ce0fe68e1781b52e0 |

源码快照是机制代码和工程源码，不是安装文件，也不含整套运行时动作资料。RC57 编译打包与 466 个类一致性核对通过，完整生存、专服、多人及性能仍待验收。

## 来源说明更正

作者于 2026-10-01 说明：动作是逐帧参考重制，代码机制由项目编写。此前文档把动作目录统一称作 private/解包资料，并据此作出一概不得发布的结论，这种分类不足以说明各文件的具体制作方式，应予更正。

项目代码、重制动作和引用素材需分别记录来源；本记录不把代码 GPL 许可自动扩展到所有素材，也不对商业授权作未经核实的肯定或否定结论。此次发布保留原包，未擅自替换任何动作。

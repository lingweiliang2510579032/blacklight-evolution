# 液态武器变形的本地原版依据

核查日期：2026-10-12。研究对象为用户本机的 2009 年《Prototype》Alex Mercer。下列内容区分直接读到的源记录、Minecraft 中重新实现的效果，以及尚未解析的部分。

本轮 fluid 效果是依据原版变形的组织传播、分部位软组织特效和材质特征重新实现的程序化效果，不是原游戏 Shader 的一比一移植。原始网格、贴图、完整动画曲线和解包 JSON 不随本文提交公共仓库。

## 本地输入与核查方法

- 游戏输入：`E:/虐杀原形/Prototype-1/Prototype/art` 下的 `alex/alex.p3d.rz`、`alex/alex_tod.p3d.rz`、`startup_effects.p3d.rz`，以及 `packages/powers/alex_*` 武器包。只读解压 RZ 的 zlib 数据到内存，没有改动游戏文件。
- 已有私有研究：`C:/Users/Administrator/Documents/Codex/2026-09-11/hua/work/prototype-research/local-private`。长变身记录来自 `fig-r25-basic/prototype-owners.json`；已解码身体动作位于 `decoded-all-r25`。
- 通用变形与组织特效记录：`C:/Users/Administrator/Documents/Codex/2026-09-26/ji/work/consume-r24-private/prototype_consume-owners.json`。
- 本地身体动作配置：`D:/Blacklight-Standalone/motion-profile/morph_*.json` 与 `armor-growth.data`。它们含原版派生数据，继续保留在仓库外。
- `effect` 轨道的 `EffectName`、`EmitJoint` 布局参考本地 EditP3D 源码的 `Prototype1/Track/EffectTrack.cs`。名称哈希按已核查的 `hash64` 算法，与用户原始包内的效果、骨骼名称交叉匹配。这里只记录核查结果，没有复制原始特效资产。

## 长变身动作中的启动时刻

五种攻击形态的长变身轨道都有共同准备动作 `alex_act_power_pain_arms`，在所属节点的 0 至 2.433330 秒播放。专属动作从 2.433330 秒开始。真正启动攻击形态转化的是独立 `spawn` 轨道，其目标路径是：

```text
//prototype_consume/transform/transform/attack/on/loaded/execute/still_loaded/transform
```

| 形态 | 拥有身体动作与 spawn 的节点 offset | 变形 spawn 开始，秒 | 后续专属身体动作 |
| --- | ---: | ---: | --- |
| 利爪 | 85724 | 2.500000 | `alex_power_transform_claws_scratch_ground` |
| 利刃 | 88928 | 1.966670 | `alex_power_transform_blades_fancy` |
| 锤拳 | 92348 | 2.833330 | `alex_power_transform_hammerfist_punch_clap` |
| 肌肉 | 96228 | 2.433330 | `alex_power_transform_musclemass_double_bicep` |
| 鞭拳 | 98532 | 2.433330 | `alex_power_transform_whipfist_crack` |

这些值相对各自的长变身拥有节点；同一节点中的身体轨道也以 0 秒起播，因此可以对齐当前对应的完整 `morph_*` 身体动作。它们不是专属身体动作自身的起点，也不是武器已完全塑形的时刻。不能将五种形态都统一为 2.433330 秒，或一开始便显示完整武器后让它整体放大。

进入被 spawn 的通用变形节点后，`transformStart` 的本地时间为 0 秒。例如 `prototype_consume` 节点 206704 中，轨道 206828 标记 `Attack`。这里的 0 秒须加上父节点的实际启动时间；不能覆盖上表长变身时间。

## 组织传播与局部软组织特效

通用变形 owner 树具有以下命名分支：

```text
transform/transform/stuff/effects/type_effects/chest_out
transform/transform/stuff/effects/type_effects/toe_to_head
transform/transform/stuff/effects/type_effects/head_to_toe
```

这些名称直接支持从胸部向外、从脚到头、从头到脚的传播方式存在。分支有条件选择和子轨道组；目前没有完整解析子轨道组的每个回调。因此它们是空间传播的源依据，不是已经还原的完整执行状态机。

直接 `effect` 轨道确认了以下软组织特效。起止时间相对各自特效节点，属于局部塑形的短窗口，不能直接当作完整身体动作的全局秒数。

| 分支 | 效果与源骨骼挂点 | 局部时序 |
| --- | --- | --- |
| 利刃，节点 217528 | 9 条 `debrisTransformSoft001`，`Elbow_R` | 0、0.066667、0.133333、0.200000、0.266667 秒分批开始；各约 0.2 秒，整体覆盖 0 至 0.466667 秒 |
| 双手分支，节点 221964 | 同特效，`Wrist_L`、`Wrist_R`、`Middle_L_End`、`Middle_R_End` | 两手腕 0 至 0.4 秒；两指尖 0.133333 至 0.6 秒 |
| 鞭拳，节点 223980 | 同特效，`Wrist_R`、`Middle_R_End` | 右手腕 0 至 0.4 秒；右指尖 0.133333 至 0.6 秒 |

双手分支的形态名称仍为未解哈希 `0x6a926ce9182bdc57`，不能仅因为有双手挂点就确定它是利爪或锤拳。上表挂点是源骨架名称，绑定到 Mod 的骨架时需要使用当前手臂与腕部映射。

短窗口、肘腕与指尖分批发射，支持制作附着手臂并推进到武器末端的塑形，以及伴随的少量软组织碎片。`debrisTransformSoft001` 的名称和轨道只证明该软组织效果存在；它本身不能证明液体模拟、真实流体求解或完整 Shader 算法。

## 独立的长变身附加特效

长变身还会启动准备、冲击或拖尾效果，独立于上面的实际 `transformStart`：

| 形态 | 直接读取的 long_transform 特效开始时间，秒 |
| --- | --- |
| 利爪 | 共用 `charge` 为 0 至 3；`long_transform/claws` 从 3 开始 |
| 利刃 | `charge/blades`、`motiontrails` 从 0 开始；`long_transform/blades` 从 4 开始 |
| 锤拳 | 共用 `charge` 为 0 至 4；`charge/hammerfist`、`long_transform/hammerfist` 从 0 开始 |
| 肌肉 | `charge/musclemass` 从 0 开始；`musclemass/motiontrails` 从 3.233330 开始；`long_transform/musclemass` 从 3.433330 开始 |
| 鞭拳 | `long_transform/whipfist` 从 3.200000 开始；`motiontrails/whipfist/sweep_charge`、`sweep_transform` 分别从 3.033330、3.066670 开始 |

这些时间相对对应长变身节点，不应拿冲击/拖尾的起点代替模型变形起点。源数据含 `-1` 结束标记；肌肉的部分轨道还出现结束字段 3 秒小于开始字段 3.433330 秒。其语义尚未解析，保持未知，不能擅自计算为负时长、倒放或以整段动画时长替换。

## 材质与网格形变的解析边界

`startup_effects.p3d.rz` 可直接读到 `char_consume`、`SHAD_consume`、`consume_colour`、`consume_normal`、`consume_refl`、`consumeSpecParams`、`normalMixPower`，以及 diffuse、normal、reflection 纹理引用。`alex.p3d.rz` 也含 consume 颜色、法线、反射与 spec 参数名称。这支持把组织颜色、表面扰动和湿润反光作为重建效果的参考。

本次没有解析并验证这些材质的实际 GPU 程序、UV 动画、法线混合、反射系数或完整参数曲线。名称存在不等于知道渲染算法，更不能把消耗/吸收材质的所有参数直接当成已确认的武器变形专属参数。

补查 `alex_tod.p3d.rz` 的 `AlexBladesTransformationDescription`、`AlexClawsTransformationDescription`、`AlexHammerFistTransformationDescription`、`AlexMuscleMassTransformationDescription`、`AlexWhipFistTransformationDescription`，确认了各自对象/手臂资源引用，例如 `alex_blade_arms`、`alex_claw_arms`、`alex_hammer_arms`、`alex_muscle_arms`、`alex_whip_arms`。这次窄范围核查没有得到可直接用于液态武器网格的、已经验证语义的顶点形变或 Shader 阶段参数。不能把这些对象的未解浮点字段猜成流速、融化程度或膨胀量。

装甲已有独立依据：`alex_act_armor_transformation_armor` 的甲片累计缩放，被既有私有 `armor-growth.data` 映射为 30 条部位曲线、30 FPS、121 帧，约 4.03 秒。它只证明装甲部位生长时序，不能当成所有攻击武器通用的液态形变曲线。装甲曲线的来源与适配边界另见 [RC30-block-armor.md](RC30-block-armor.md)。

## 本轮适配范围

本轮已接入利爪、利刃、锤拳、肌肉和鞭拳五种攻击武器，以第三人称变形为主要展示。武器表面使用共享波形驱动鼓动、轻微切向流动与逐段塑形；四股具有厚度的组织流附着当前手臂关节，沿覆盖前沿推进。湿材质在同一实体绘制管线中增加随观察角度变化的克制反光，继续接受环境光照、受伤覆盖与雾；液态强度归零后使用原来的稳定武器材质。

组织推进与模型切换共用形态变换时间；完整长变身采用上表核对的启动点，普通切换、收起和被动作打断的过渡属于 Minecraft 适配。装甲与护盾保留各自已有生长流程，本轮五种攻击武器的效果不代表已重新实现它们全部液态表现。

本轮没有完整复制原版的软组织碎片、charge 准备、burst 爆发和所有拖尾分支，也没有把回缩细丝列为单独完成的原版系统。前文有关这些资源与时序的核查用于明确来源及后续工作，不是当前效果的完成清单。

效果附着当前骨骼和现有第一人称手部绘制位置，没有为液态效果另加相机偏移或把全身 Root 位移带入第一人称。空间推进、网格鼓动、组织流的幅度、速度、扰动、湿亮系数及收起时长是本轮自制表现，没有伪装成解包确认值。

保留全部私有输入在上述研究目录；公共提交只包含本说明、程序化渲染代码和项目自制资源。本文是源记录核查，不是最终客户端视觉验收或完整原版效果还原声明。

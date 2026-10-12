# R22 P1 P3D 动画通道：源码与本地交叉验证

核查日期：2026-09-20。只读取开源解码器源码和本机既有资源，未运行上游 EXE，未复制原版动画到 Mod 生产目录。

开源依据：[Elan-S/EditP3D](https://github.com/Elan-S/EditP3D)，`master` 核查提交 `6966de776f7650e4f5a9150ebe4cf9377f7933d3`。本地仅源码副本位于 `work/prototype-research/tools-r22/elan`，上游采用 zlib 许可，许可文本保留在源码副本中。

## 核心结果

独立审计脚本 `work/prototype-research/validate_elan_channels_r22.py` 对 `alex.p3d` 与 blades/claws/hammerfist/whipfist/musclemass 六包的 PTRN 动画，按下列规则解码了 **37,637 条目标类型通道、1,314,019 个关键帧**。缓冲区边界、帧号单调、数值有限性均通过，重建旋转的 xyz 平方和没有超过 1。结果仅保存在私有研究目录 `local-private/elan-layout-validation-r22.json`。

该审计只覆盖下表的两种压缩四元数与三维 half 通道；其他通道由另一份通用解码器检查，不能把这里的数量当作全部文件类型的覆盖率。

## Chunk 与层级

所有以下数字按 little-endian 读取。每个 chunk 先有三个 `uint32`：`type, headerSize, totalSize`。`headerSize` 包含 12 字节 chunk 头，子块从 `chunkOffset + headerSize` 开始，当前块结束于 `chunkOffset + totalSize`。不要假设所有 chunk 起点按 4 字节对齐。

- `0x121000 Animation`：所属动画，携带版本、带长度的名称、FourCC 动画类型、float32 总帧数、float32 帧率、uint32 循环标志。
- `0x121002 AnimationGroup`：版本、组数；其子树包含骨骼组。
- `0x121001 Bone`：版本、带长度的骨骼名、GroupId、通道数。通道通过这个父节点的**骨骼名**对应骨架，不应把通道遍历序号误当骨架索引。
- `0x02f00000 AnimationData`：当前动画子树的外置通道数据块。其 payload 是 `uint32 version, char[4] compression, uint32 uncompressedSize, uint32 compressedSize, byte[compressedSize] data`。
- `compression == b'ZLIB'` 时 Python 可直接 `zlib.decompress(data)`，并核查解压长度。C# 跳过 zlib 的两字节头后用 DeflateStream，是 API 差异；Python 不需要手工去头。

源码：[Animation.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/Animation.cs)、[Bones.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/Bones.cs)、[AnimationData.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/AnimationData.cs)。

## 三种核心数值通道

下表中的偏移全部相对于 chunk 的 **payload 开始**，即排除 12 字节 chunk 头。

共同 payload 开头：`uint32 version`（偏移 0）、`char[4] tag`（偏移 4，如 `ROT\0`、`TRAN`）、`uint32 inlineFrameCount`（偏移 8）。不要根据 type 就把所有向量都当成 TRAN；本地还出现 `SCL\0`、`SHER`、`SCLP`、`SCLT`。

| type | 值布局 | 解码 |
| --- | --- | --- |
| `0x121112` Quaternion6Compressed | 每关键帧 3×signed int16，6 字节 | x/y/z 分别除以 32767；w 取正平方根 `sqrt(max(0, 1-x*x-y*y-z*z))` |
| `0x121114` Quaternion3Compressed | 每关键帧 3×signed int8，3 字节 | x/y/z 分别除以 127；w 同上 |
| `0x121119` Vector3DOFCompressed | 每关键帧 3×IEEE754 binary16，6 字节 | Python `struct.unpack_from('<eee', ...)`，顺序 x/y/z |

ROT 的值是单位四元数分量，不是欧拉角，不以度或弧度存储。原始三分量顺序是 xyz，补出的 w 为第四分量；传给 Blender 的 `Quaternion` 时要改成 `(w,x,y,z)`，传给需要 xyzw 的库时保持 `(x,y,z,w)`。采样时使用最短弧 SLERP：若前后四元数点积负，将后者整组取负后插值，随后归一化。

源码：[Quaternion6CompressedChannel.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/BoneData/Quaternion6CompressedChannel.cs)、[Quaternion3CompressedChannel.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/BoneData/Quaternion3CompressedChannel.cs)、[Vector3DOFCompressedChannel.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/BoneData/Vector3DOFCompressedChannel.cs)。

## 内联与外置：最容易错的两字节差异

### 内联：inlineFrameCount > 0

设 N 为 inlineFrameCount：

```python
keys_offset = 12
values_offset = 12 + 2 * N
keys = struct.unpack_from('<' + 'H' * N, payload, keys_offset)
```

帧表是连续的 `uint16[N]`，随后是连续的 values 表。**即使 N 为奇数，内联帧表后也不加两字节 padding。** 先完整读取帧表，再完整读取值表；不是 key/value 交错结构。

### 外置：inlineFrameCount == 0

通道的子块 `0x121120`（源码也接受 `0x121121`）为 AnimationDataReference。其 payload：

```text
uint32 version
uint32 numberOfKeys
uint32 byteOffset
byte[] extraData     # P1 本次目标文件未需要非零 segment
```

在所属 **Animation 子树** 的 AnimationData 解压块中，从 byteOffset 开始读取帧表；N 为奇数时帧表后有 **2 字节 padding**：

```python
keys_offset = byteOffset
values_offset = byteOffset + 2*N + (2 if N & 1 else 0)
```

不能把 byteOffset 当成 `.p3d` 文件偏移，也不能拿前一个/全局动画的压缩块。内联通道已有 N 个值时忽略外置读取路径。

上游支持 extraData 首字节表示 segment index；只有单个物理块却引用多个 segment 时，会按每段引用的最大末尾估算长度、16 字节对齐后切段。本次私有 P1 审计中没有非零 segment，**该分段策略尚未被这里的数据验证**。

源码：[AnimationChannel.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/AnimationChannel.cs)、[AnimationDataReference.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/AnimationDataReference.cs)。

## 0x121110 与时间单位

`0x121110` 是 BoneInterpolation 元数据，不是另一个关键帧通道。payload 是 `uint32 version, uint32 interpolate`；本机常见 interpolate 为 `0xffffffff`。源码未提供该值的完整语义枚举，不应猜它等于某种曲线模式；预览器仍为向量执行线性插值、旋转执行 SLERP。

帧表的 `uint16` 是动画帧号，转换秒数用对应 Animation 的 FrameRate。不要把“稀疏关键帧数量”当动画总帧数，也不要按列表下标均匀采样。骨骼不同通道的关键帧数量和时间可以不同。没有通道的骨骼保留 bind pose 对应分量。

源码：[BoneInterpolation.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.FileFormats/Pure3D/BoneData/BoneInterpolation.cs)。

## TRAN 标定与骨架应用

EditP3D 的 `ToPreviewPosition` 原样保留 x/y/z；本次解码也直接使用 half 值，**不乘 100、不除 100，也不把 half 位型当作 int16 除以 32767**。文件存储的物理单位没有在该源代码里明文注明，因此“原始坐标单位”比未经验证的“厘米/米”更准确。重定向到 Minecraft 时，应通过实际骨架身高比统一标定；此标定是目标骨架适配，不是文件解压公式。

预览器 `ApplyAnimationChannelsToPose` 的行为：

- ROT 样本直接构造该骨骼的 local 旋转基，覆盖对应 bind local 旋转；不是再乘一次 bind local 旋转。
- 非根 TRAN 样本直接覆盖 local position；不是在 bind position 上叠加一遍。
- root 可选择归零：`bindRootPosition + currentRootSample - firstRootSample`。这只针对根节点，不应对每根骨骼减首帧，否则会消掉蹲伏与骨长关系。
- 最后沿 parent 链组合 local 变换得到全局姿势。转换坐标轴要同时作用于骨架、旋转与位置；不要只翻 TRAN 的某一个轴。

源码位置：[ModelPreviewBuilder.cs](https://github.com/Elan-S/EditP3D/blob/6966de776f7650e4f5a9150ebe4cf9377f7933d3/Gibbed.Prototype.Edit3D/ModelPreviewBuilder.cs)，特别是 `CreateAnimation`、`ApplyAnimationChannelsToPose`、`ComposeBones`、`ToPreviewQuat`、`ToPreviewPosition`、`RotateLocalBasis`。

这是可靠读取原始姿势的格式结论；骨架可视化、左右方向、接地及重定向后的武器穿模仍需另外验证。解码数值合法并不自动证明游戏动作已正确还原。

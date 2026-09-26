# R15 武器重建

本轮以静态原作概念图为造型参考，重建四种武器。用户纠正刀臂必须保留两端长刃，因此 R15 的刀臂包含前向刃与反向刃，反向刃不是短倒刺。生成的三视图只是工作参考，不是官方定稿或游戏模型。

## 结构

- 利爪：保留已经验证的掌心和拇指方向，重做长短错落的弯爪、前臂外壳和纵向筋束。
- 刀臂：双端刃面、中部镂空连接、窄刃口与抬起的组织脊。
- 锤拳：较窄的前臂连接、三块闭合的撞击指节和不规则硬壳结节。
- 鞭拳：钩形主刃、反钩和连续的背侧骨节；绳索连接点更新到新模型末端。
- 生物组织使用平滑法线，刀面保留刃口折线。所有运行时网格由本项目控制曲线生成，没有导入原作资产。

## 绑定与动作

Epic Fight 的 `Hand_R/L` 是肘部骨骼。肩、肘原点不作任意平移。利爪掌心保持 R13 的位置，抓取 IK 与渲染接触点同步恢复到该位置。

双端刀臂暴露了旧 `consume_blade` 在跟随动作时的头部相交。保持抓取起始姿态、攻击时间和手臂长度，仅在右前臂局部轴上平滑向外滚转 15 度，让反向刀刃从头部外侧经过。

刀臂站立待机时抬起右臂，避免长刃自然下垂插入地面；起步后平滑退出，攻击、吸收、受伤和使用物品时由原动作接管。

## 验证

88 项单元测试和 23 项游戏测试通过，包括武器接触、刀刃避头、12 格鞭拳抓取及墙体遮挡。游戏内录制七组抓取/吸收/投掷场景，运行结果全部通过；受害生物渲染后的共享姿态恢复检查通过。另检查了六组鸡、幼鸡、猪、村民及转向抓握场景，最大掌心到身体边界间隙为 0.00239 格。检查了双端刀的待机与吸收画面。自动检查不等于所有视角和地形都已经人工验收。

## 再生成

```text
python tools/build_reference_weapons.py
blender -b --python tools/clear_reverse_blade.py -- PATH_TO_EPIC_BIPED_JSON
blender -b --python tools/render_power_forms.py -- BEFORE_MESH_DIRECTORY OUTPUT_DIRECTORY
```

`tools/data/claw_contact_r13.json` 是本项目已有抓取网格的接触部位，作为稳定的解剖绑定基准。`clear_reverse_blade.py` 读取依赖的真实骨架并保留动画第一个抓取姿态；重复执行会先撤销此前的角度修正，避免累计旋转。

## 参考来源

- [一代概念设计与设计师说明的社区汇编](https://steamcommunity.com/sharedfiles/filedetails/?id=2900892589)
- [刀臂轮廓方案](https://images.steamusercontent.com/ugc/5933000025553876249/8BC20029256DB18FD578E2F9BAFC16F031445029/)
- [锤拳概念图](https://prototype.fandom.com/wiki/Hammerfists)
- [利爪概念图](https://prototype.fandom.com/wiki/Claws)
- [Dejan Ostojic 原作概念图与鞭拳分镜](https://d-conceptart.blogspot.com/2012/07/prototype.html)

概念方案与原作最终实现可能有差异，R15 是据此制作的 Minecraft 适配版本，不宣称一比一还原。

# 02 — 改性矩阵 modified matrix（坐在 vat 顶部的矩阵）

本文分析 Isorropia 的 **modified matrix**（1.7.10 Thaumic Horizons 里叫 `TileVatMatrix` / “vat matrix”）的
全部客户端渲染与效果：8 个旋转立方体、发光 overlay 层、halo 光芒、expunge（排 flux）特效、护目镜文字。
它是 TC4 **runic matrix（注魔矩阵）** 的一个变体——同一份 `ModelCube(0/32)`、同一套 startUp 旋转 / instability
抖动 / halo 公式，只是把数据源从 `TileInfusionMatrix` 换成“下面的 vat”，并把贴图换成 Isorropia 自己的
`matrix_vat.png`。因此 **TC4R 自带的 `InfusionMatrixRenderer`（20732）是本区域最重要、最省事的参考与复用对象**。

引用记号约定（与 `00-api-reference.md` 一致）：
- `1.12:` = `H:/MinecraftMods/Isorropia_kedition/src/main/java/fr/wind_blade/isorropia/`
- `1.7.10:` = `H:/MinecraftMods/ThaumicHorizons/src/main/java/com/kentington/thaumichorizons/`
- `TC4:` = `H:/MinecraftMods/TC4/thaumcraft/`（1.7.10 Thaumcraft 4.2.3.5 反编译）
- `TC4R:` = `H:/MinecraftMods/Isorropia-1.20.1/reference/tc4r-20721-sources/dev/tc4port/thaumcraft/`（20721 源码）
- `port:` = `H:/MinecraftMods/Isorropia-1.20.1/src/main/`

---

## 0. 结论速览

| 项 | 结论 |
|---|---|
| 1.7.10 vs 1.12 谁更准 | **立方体几何/抖动/halo 用 1.7.10 + TC4**；**三轴旋转、贴图、expunge、startUp 语义用 1.12（Isorropia 实际发行版）**；**1.20.1 落地代码直接用 TC4R `InfusionMatrixRenderer` 的算法**（它已经把 TC4 逐项复刻并对齐到 1.20.1）。1.12 的 renderer 有 3 处明显“劣化/改写”，不要照抄（详见 §1.4）。 |
| port 现状 | **缺失**：没有 BER、没有模型类、没有发光 RenderType；方块静态模型目前在世界里可见（应为 `INVISIBLE`）；`TileVat` 没有 `startUp` / `craftCount` 两个动画量，也没有把 instability 明确暴露给客户端。贴图与两个 JSON 已存在且正确。 |
| 最大复用 | `TC4R: api/client/ThaumcraftClientVisualsApi.renderInfusionHalo(pose, buffers, craftCount)`（D:51，20732 新增，20721 源码中不存在但 20732 jar 已 `javap` 确认存在）——halo 一行搞定，内部自带 `translate(0.5,0.5,0.5)`。 |
| 需要新增的共享件 | 一个自建 additive glow RenderType（`NativeRenderTypes.create` + eyes shader + LIGHTNING_TRANSPARENCY + COLOR_WRITE），因为 TC4R 的 `TCClientRenderTypes.infusionMatrixGlow` 是**包私有**。 |

---

## 1. 上游行为（逐文件 + 行号）

### 1.1 1.12 Isorropia

- **BER**：`1.12: client/renderer/tiles/RenderModifiedMatrix.java`（225 行）
  - `:29-30` `model = new ModelCube(0)`、`model_over = new ModelCube(32)`；`ModelCube` 来自
    `thaumcraft.client.renderers.models.ModelCube`（TC6/TC 共用类）。
  - `:31` `tex1 = isorropia:textures/blocks/matrix_vat.png`（注意是 Isorropia **自己的** 64×64 贴图，
    不是 TC 的 `infuser.png`）。
  - `render()` `:221-224` → `renderInfusionMatrix()`。
  - 无 `renderWorldLast`/无阶段排队：直接画，halo 也直接画在同一 TESR 里。
- **TE（客户端相关部分）**：`1.12: common/tiles/TileModifiedMatrix.java`
  - `:91-94` `getMaster()`：取 `world.getTileEntity(pos.down())`，`instanceof TileVat` 才算。
  - `:96-117` `getIGogglesText()`（护目镜文字）。
  - `:24` 实现 `IAspectContainer` + `IGogglesDisplayExtended` + `IInteractWithCaster`。
- **方块**：`1.12: common/blocks/BlockModifiedMatrix.java`
  - `:47-49` `getRenderType()` 返回 `EnumBlockRenderType.INVISIBLE` —— **世界里不画 block model**，
    模型只用于物品栏（`blockstates/modified_matrix.json` 同时给 `inventory` 和 `normal` 变体）。
  - `:39-46` `breakBlock`：破坏时把 vat 置 `setActive(false)`、`setStartUp(0.0f)`、`syncTile(false)`，
    正在注魔则 `destroyMultiBlock()`。
- **方块模型 JSON**：`1.12: assets/.../models/block/modified_matrix.json`（8 个 `7×7×7` 角块，见 §2.6）。
- **数据源**（`1.12: common/tiles/TileVat.java`）：`getRawStability() :1470`、`getCraftCount() :1458`、
  `getStartUp() :1426`、`isActive() :1442`、`isInfusing() :1462`、`isExpunging() :1531`、`setExpunge() :1547`。
  `update() :173` 中 `:248` 是 startUp 的缓动公式（见 §2.2.1）。

### 1.2 1.7.10 Thaumic Horizons

- **BER**：`1.7.10: client/renderer/tile/TileVatMatrixRender.java`（214 行）
  - `:31-40` `model = new ModelCube(0)`、`model_over = new ModelCube(32)`；构造函数带一个 `type` 参数
    （`RendererVatMatrix` 的遗留物，实际恒为 0；`:218-223` 的 `type==1` 分支递归调用自己 = 死代码，勿移植）。
  - `:99` `UtilsFX.bindTexture("textures/models/infuser.png")` —— **用 TC4 原版贴图**。
  - `renderTileEntityAt()` `:210-213` → `renderInfusionMatrix()`。
- **ISBRH**：`1.7.10: client/renderer/block/BlockVatMatrixRender.java`
  - `:35-38` `renderWorldBlock` 恒返回 `false`（世界里什么都不画，全靠 TESR）。
  - `:23-33` `renderInventoryBlock`：`rotateY(90)` → `translate(-0.5,-0.5,-0.5)` → 用假 TE 跑 `renderTileEntityAt`。
    即 1.7.10 的**物品栏里显示的是真正的旋转立方体 TESR**，而不是烘焙静态模型。
- **TE**：`1.7.10: common/tiles/TileVatMatrix.java`
  - `:20-26` `getVat()`：`world.getTileEntity(x, y-1, z) instanceof TileVat`。
  - `:83-89` `onWandRightClick`：`t.isValidInfusionTarget() && t.mode==0` 或 `t.mode==4` → `t.startInfusion(player)`。
  - 实现 `IWandable` + `IAspectContainer` + `TileVisNode`；**没有护目镜文字**（TC4 的 `IGoggles` 只显示要素图标，
    `TileVat` / `TileVatMatrix` 都没实现）。
- **数据源**：`1.7.10: common/tiles/TileVat.java`
  - 字段：`mode :120`（int）、`instability :134`（int，0..25）、`craftCount :144`（int）、`startUp :145`（float）。
  - `:290-297` startUp 衰减（`mode != 2` 时 `startUp -= startUp/10`，<0.001 归零）。
  - `:785-818` `doEffects()`：注魔时 `craftCount++`、`startUp += max(startUp/10,0.001)`（到 1 封顶）；
    不注魔时 `craftCount -= 2`（地板 0，上限 `CRAFTCOUNT_WIND_DOWN_MAX=50 :89`）。
  - `:699` `instability = symmetry + recipeInstability`（`MAX_INSTABILITY=25 :93`）。

### 1.3 TC4 原版 runic matrix 与 TC4R 实现（本区域的“真身”）

- **TC4**：`TC4: client/renderers/tile/TileRunicMatrixRenderer.java`
  - `:82-171` `renderInfusionMatrix`；`:30-80` `drawHalo`。
  - 三轴旋转：`rotatef(ticks%360*startUp,0,1,0)` → `rotatef(35*startUp,1,0,0)` → `rotatef(45*startUp,0,0,1)`。
  - 抖动分母 **含** `instability/2`：`15-inst/2`、`14-inst/2`、`13-inst/2`。
  - `instability = min(6, 1 + instability*0.66*(min(craftCount,50)/50))`。
  - 模型类 `ModelCube`：`TC4: client/renderers/models/ModelCube.java`
    - `ModelCube()` `:9-16`：texSize **64×32**、`texOffs(0,0)`、box `(-8,-8,-8,16,16,16)`、`field_78809_i = true`（**mirror**）。
    - `ModelCube(int shift)` `:18-25`：texSize **64×64**、`texOffs(0, shift)`、同 box、同 mirror。
    - （1.12 的 `RenderModifiedMatrix` 用的就是这两个构造器，行为与 TC4 相同。）
- **TC4R**：`TC4R: client/InfusionMatrixRenderer.java`（20732 起还有 `api/client/ThaumcraftClientVisualsApi.renderInfusionHalo` 的公开入口）
  - `:198-224` `poseMatrix` / `poseCube`：把上述变换完整搬到 `PoseStack`，**数值与 TC4 一字不差**。
  - `:163-179` `createFrame`：overlay 颜色 `argb(1, 0.8*a, 0.1*a, a)`，`a=(sin((ticks+a*2+b*3+c*4)/4)*0.1+0.2)*startUp`。
  - `:226-251` `renderHalo`：`rays = FAST?10:20`、`growth=min(craftCount,50)/50`、`Random(245L)`、
    白色中心 `(255,255,255,255)`、外点 `(204,0,255,0)`、`len=(r*20+5)/20*growth`、`w=(r*2+1)/20*growth`。
  - `:39/92-98` 排队到 `AFTER_BLOCK_ENTITIES` 回放（TAA/发光相位考虑，见 §5 说明为什么 Isorropia 可以简化）。
  - 模型类 `TC4R: client/InfusionMatrixModel.java`：`LayerDefinition 64×64`，`base texOffs(0,0)`、
    `overlay texOffs(0,32)`，box `(-8,-8,-8,16,16,16)`，`PartPose.ZERO`（**没有 mirror**，见 §6 风险 1）。

### 1.4 逐项对照与参考选取（每个元素“跟谁”）

| # | 元素 | 1.12 | 1.7.10 | TC4 / TC4R | 采用 | 理由 |
|---|---|---|---|---|---|---|
| 1 | 8 立方体几何 | box 同 | box 同 | 同 | TC4R `InfusionMatrixModel` | 1.20.1 已验证实现，且 1.12 的 `ModelCube` 来自 TC6（构造器 body 未核对） |
| 2 | 三轴旋转 | rotY+rotX(35)+rotZ(45) `:101-103` | **只有 rotY** `:111` | 三轴 | **1.12 / TC4** | 1.7.10 漏了 X/Z 倾斜（劣化）；1.12 恢复了 TC4 的三轴 |
| 3 | 抖动分母 | 固定 `15/14/13` `:143-145` | `15-inst/2` 等 `:123-131` | `15-inst/2` 等 | **1.7.10 / TC4** | 1.12 把 `instability/2` 丢了（劣化） |
| 4 | instability 公式 | `1 + (inst<0 ? -inst*0.66 : 1)*k` `:132` | `1 + inst*0.66*k` `:113` | `1 + inst*0.66*k` | **1.7.10 / TC4** | 1.12 的 `: 1.0f` 分支是改写/退化，语义不清 |
| 5 | halo 几何 | 同 | 同 | 同 | TC4R `renderInfusionHalo` | 只差一处见 #6 |
| 6 | halo 顶点色 | 白 alpha `255*(1-f1)`，`f1=count/500` `:40,61` | 白 alpha `255*(1-f3)`，`f3=0` → 255 `:51,73` | 白 255 / `(204,0,255,0)` | **1.7.10 / TC4** | 1.12 误用 `f1` 让 alpha 随 craftCount 下降（劣化） |
| 7 | halo 触发条件 | `isInfusing()` `:215` | `mode==2` `:205` | `crafting` | 任一 | 三者等价（注魔中） |
| 8 | 贴图 | `matrix_vat.png` 64×64 | `infuser.png` 64×64 | `infuser.png` | **1.12** | Isorropia 实际发行版的自有贴图，port 已有 |
| 9 | startUp 缓动 | “active 就涨、否则落” `:248` | “仅 mode==2 涨” `:812-817` | — | **1.12** | Isorropia 的矩阵是“被法杖打开后常亮自转”，不是只在注魔时转 |
| 10 | craftCount 语义 | vat 上，halo 成长 | vat 上，注魔 +1/tick、闲置 −2/tick、上限 50 | 同 1.7.10 | **1.7.10** | 1.12 同义，常数在 1.7.10 有（`CRAFTCOUNT_WIND_DOWN_MAX=50`） |
| 11 | expunge 特效 | 有，1.12 独有 `:104-121` | 无 | 无 | **1.12** | Isorropia 新增的“排 flux”表现 |
| 12 | 护目镜文字 | 有（TC6 API） `:96-117` | 无 | 无（TC4 只显示要素图标） | 见 §5.6 | TC6 独有，TC4R 无对应 API |

---

## 2. 精确常量

### 2.1 立方体模型（ModelCube / ModelPart）

- 层定义：`LayerDefinition 64 × 64`（TC4 的 base 用 64×32，但 box 只吃 `u 0..64 / v 0..32`，与 64×64 下
  `texOffs(0,0)` 的 UV 展开完全一致，故统一 64×64 无副作用）。
- `base`：`texOffs(0, 0).addBox(-8, -8, -8, 16, 16, 16)`，`PartPose.ZERO`。
- `overlay`：`texOffs(0, 32).addBox(-8, -8, -8, 16, 16, 16)`，`PartPose.ZERO`。
- 单位：ModelPart 内部 × 1/16；`PartPose.ZERO`。所以两立方体各自在**模型局部空间**占据 `-0.5..+0.5`。
- TC4 的 `mirror = true`（`ModelCube.java:15,24` `field_78809_i = true`）；TC4R 未设 mirror。→ §6 风险 1。

### 2.2 变换（顺序不可乱；所有角度单位 = 度）

**2.2.1 矩阵整体（在 `translate(0.5,0.5,0.5)` 之后）**

```
ticks = cameraEntity.tickCount + partialTick        // 1.12 :88；TC4R :63
startUp ∈ [0,1]                                       // 缓动，见下
if (vat 存在):
    rotateY( (ticks % 360) * startUp )                // 1.12 :101 / TC4 :95 / TC4R :199
    rotateX( 35 * startUp )                           // 1.12 :102 / TC4 :96 / TC4R :200
    rotateZ( 45 * startUp )                           // 1.12 :103 / TC4 :97 / TC4R :201
if (vat.isExpunging()):                               // 1.12 :104-105 独有
    rotateY( (ticks + partialTick) * 30 )             // 注意 1.12 把 partialTick 又加了一次（小瑕疵，照抄即原样）
```

startUp 缓动（1.12 `TileVat.java:248`，唯一权威公式）：

```
startUp = (!active || (!infusing && expunge))
    ? (startUp > 0 ? startUp - max(startUp / 10, 0.001) : 0)
    : (startUp < 1 ? startUp + max(startUp / 10, 0.001) : 1);
```
（每 tick 执行一次；从 0 起首 tick 只能 +0.001，之后按 ×1.1 几何增长，约 72 tick 到 1。）

**2.2.2 每个立方体（8 次，a,b,c ∈ {0,1}）**

```
instability = min(6, 1 + instabilityValue * 0.66 * (min(craftCount, 50) / 50))   // 1.7.10 :113 / TC4 / TC4R :205
// 抖动（仅 active 时非零；1.7.10/TC4 分母含 -instability/2）
b1 = sin((ticks + a*10) / (15 - instability/2)) * 0.01 * startUp * instability
b2 = sin((ticks + b*10) / (14 - instability/2)) * 0.01 * startUp * instability
b3 = sin((ticks + c*10) / (13 - instability/2)) * 0.01 * startUp * instability
push
translate( b1 + (a==0 ? -0.25 : +0.25),
           b2 + (b==0 ? -0.25 : +0.25),
           b3 + (c==0 ? -0.25 : +0.25) )            // 1.7.10 :137 / 1.12 :151 / TC4R :213
if (a>0) rotateX(90)                                // 轴 (a,0,0) 且 a∈{0,1}，即 X
if (b>0) rotateY(90)                                // 轴 (0,b,0)
if (c>0) rotateZ(90)                                // 轴 (0,0,c)
scale(0.45, 0.45, 0.45)                             // 3 个轴同值；旋转+缩放顺序必须先 rotate 后 scale
draw base 立方体                                    // model.render()
pop
```
- **base 层**（8 个立方体）：恒画（不论 active）。用普通光照：`RenderType.entityCutout(matrix_vat)` 或
  `RenderType.entitySolid`，顶点色 `(1,1,1,1)`，light = BE 的 `packedLight`，overlay = `packedOverlay`。
  （1.12/1.7.10 是在标准 TESR 光照状态下 `model.render()`，即“随方块亮度”。）
- **overlay 层**（8 个立方体，同 8 个 pose，**只在 active 时画**——1.12 `:167`；1.7.10 恒画但 startUp=0 时
  alpha=0 故不可见，等价）：
  - GL 状态（1.7.10 `:154-156` / 1.12 `:169-171`）：`alphaTest` 阈值 **0.003921569**（≈1/255，几乎不裁剪）、
    `enableBlend`、`blendFunc(SRC_ALPHA, ONE)` = TC4 `(770,1)`。
  - 亮度：**FULL_BRIGHT**（`15728880 = 0xF000F0`，1.7.10 `:185-188` / 1.12 `:193-196`）。
  - 颜色：`glColor4f(0.8, 0.1, 1.0, (sin((ticks + a*2 + b*3 + c*4)/4) * 0.1 + 0.2) * startUp)`
    （1.7.10 `:189-194` / 1.12 `:197`）。→ 顶点色直接给 `(0.8, 0.1, 1.0, a)`，或用 TC4R 的预乘形式
    `argb(1, 0.8a, 0.1a, a)`（等价，因为 `blendFunc(SRC_ALPHA,ONE)` 下两者结果相同）。
  - 画完把顶点色复位 `(1,1,1,1)`（1.12 `:199`），最后恢复 `blendFunc(770,771)` + 阈值 0.1（1.12 `:204-206`）。

### 2.3 halo 光芒（`drawHalo`）

1.7.10 `:42-93` / 1.12 `:34-79`；触发 = 注魔中。几何与 TC4R `renderHalo` 完全一致，直接复用
`ThaumcraftClientVisualsApi.renderInfusionHalo(pose, buffers, craftCount)`。逐项常量（供核对）：

- 起点：`translate(pos + 0.5, pos + 0.5, pos + 0.5)`（1.7.10 `:45` 在 renderer 里 push 后 translate；
  TC4R 的公开 API **内部自带** `translate(0.5,0.5,0.5)`，所以传入的是**方块角 pose**）。
- 条数 `q`：`fancyGraphics ? 20 : 10`（1.7.10 `:46` / 1.12 `:37`）≈ TC4R `FAST ? 10 : 20`。
- `growth = min(craftCount, 50) / 50`；`growth <= 0` 不画。
- `Random(245L)`（固定种子，每帧重建 → 图案稳定、随 craftCount 缩放）。
- 每条：连续 **6 次** `rotate(rand*360)`：X,Y,Z,X,Y,Z，最后一次 Z 额外加 `f1*360`，其中 `f1 = count/500`
  （1.7.10 `:49,67`）。**不 push/pop**，所以旋转累乘（TC4R `:237-242` 同样累乘）。
- `fa = (rand*20 + 5 + f3*10) / (20 / growth)`，`f4 = (rand*2 + 1 + f3*2) / (20 / growth)`，`f3 = 0`
  （1.7.10 `:69-72`）→ 等价 `len = (rand*20+5)/20*growth`、`w = (rand*2+1)/20*growth`。
- 顶点（三角扇 `startDrawing(6)` → 1.20.1 拆 `Mode.TRIANGLES`，TC4R 用 3 个三角形 = 1 个扇）：
  - 中心 `(0,0,0)` 色 `0xFFFFFF`(16777215) alpha `255*(1-f3)=255`
  - `(-0.866*w, fa, -0.5*w)`、`(0.866*w, fa, -0.5*w)`、`(0, fa, w)`、`(-0.866*w, fa, -0.5*w)` 色
    `13369599 = 0xCC00FF` = `(204,0,255)` alpha **0**（1.7.10 `:73-79`）
- GL 状态：关 2D 纹理、`ShadeModel(SMOOTH)`、`enableBlend`、`blendFunc(770,1)`、**关 alpha test**、
  `enableCull`、`depthMask(false)`；结束恢复 `depthMask(true)`、关 cull/blend、`ShadeModel(FLAT)`、
  `blendFunc(770,771)`、开 2D 纹理与 alpha test（1.7.10 `:53-91`）。
  → 1.20.1：TRIANGLES + POSITION_COLOR + LIGHTNING blend + COLOR_WRITE + CULL，即 TC4R 的 `infusionHalo`
  或公开 `ThaumcraftClientRenderApi.eldritchOrbRays()`。

### 2.4 破坏贴花（destroyStage，选做）

1.12 `:124-131`：`bindTexture(DESTROY_STAGES[stage])`，纹理矩阵 `scale(4,4,1)` + `translate(0.0625,0.0625,0.0625)`；
对应 TC4R 用 `SheetedDecalTextureGenerator` + `ModelBakery.DESTROY_TYPES`（见 `InfusionMatrixRenderer:133-147`）。
**优先级低**，可后补。

### 2.5 expunge（排 flux）特效 —— 1.12 独有

`1.12: RenderModifiedMatrix.java:104-121`，条件 `vat.isExpunging()`：

- 先 `rotateY((ticks + partialTick) * 30)`（§2.2.1）。
- 每帧生成 **1** 颗粒子（注意是在 a/b/c 循环**之外**）：
  - 位置 = `vat 方块坐标 + 0.2 + rand*0.6`（x、y、z 各一次，`te.getWorld().rand`）。
  - `FXGeneric`：`maxAge = 100 + rand.nextInt(60)`、`RGB = (1.0, 0.3, 0.9)`、`alphaF(0.5, 0.0)`、
    `gridSize = 16`、`setParticles(56, 1, 1)`、`scale(2.0, 5.0)`、`setLayer(1)`（加色）、
    `setSlowDown(1.0)`、`setWind(0.001)`、`setRotationSpeed(randF, randBool ? -1 : +1)`。
- 1.20.1 映射：`ThaumcraftClientEffectsApi.spawnGenericParticle(level, pos, Vec3 velocity, 1.0F, 0.3F, 0.9F,
  0.5F, false, 56, 1, 1, maxAge, 0, 2.0F)`。**注意 TC4R 的 API 只有单值 alpha / scale**（`ChampionGenericParticle`
  的 `quadSize = 0.1*scale`、`alpha=sourceAlpha`），无法表达 `alpha 0.5→0.0` 与 `scale 2→5` 的双值插值，
  只能取初值（0.5 / 2.0）。见 §6 风险 3。

### 2.6 方块静态模型 JSON（`models/block/modified_matrix.json`）

8 个元素、每个 `7×7×7`，贴图 `#side = isorropia:block/matrix_vat`，`ambientocclusion=false`；
port 版本额外加了 `"parent": "minecraft:block/block"`（给物品栏正确的 display 变换，可保留）。
UV 是 16 单位空间里 6 个 `4×4` 子矩形：`[0,4,4,8] [4,0,8,4] [8,0,12,4] [4,4,8,8] [8,4,12,8] [12,4,16,8]`
（贴着 64×64 贴图的 4×4 网格，即每个 4 单位 = 16px）。

| # | from→to | up | down | west | east | north | south |
|---|---|---|---|---|---|---|---|
| 1 | `[0,0,0]→[7,7,7]` | 0,4,4,8 | 8,0,12,4 | 0,4,4,8 | 8,0,12,4 | 8,0,12,4 | 4,0,8,4 |
| 2 | `[9,0,9]→[16,7,16]` | 0,4,4,8 | 4,0,8,4 | 8,4,12,8 | 8,4,12,8 | 4,4,8,8 | 4,4,8,8 |
| 3 | `[9,9,0]→[16,16,7]` | 4,4,8,8 | 12,4,16,8 | 8,4,12,8 | 4,4,8,8 | 8,4,12,8 | 4,0,8,4 |
| 4 | `[0,9,9]→[7,16,16]` | 8,4,12,8 | 12,4,16,8 | 8,0,12,4 | 8,0,12,4 | 4,4,8,8 | 4,0,8,4 |
| 5 | `[9,9,9]→[16,16,16]` | 12,4,16,8 | 12,4,16,8 | 8,4,12,8 | 0,4,4,8 | 4,4,8,8 | 8,0,12,4 |
| 6 | `[0,0,9]→[7,7,16]` | 0,4,4,8 | 12,4,16,8 | 4,4,8,8 | 8,0,12,4 | 4,4,8,8 | 0,4,4,8 |
| 7 | `[0,9,0]→[7,16,7]` | 0,4,4,8 | 12,4,16,8 | 4,0,8,4 | 8,0,12,4 | 12,4,16,8 | 4,0,8,4 |
| 8 | `[9,0,0]→[16,7,7]` | 0,4,4,8 | 0,4,4,8 | 8,4,12,8 | 12,4,16,8 | 4,0,8,4 | 4,0,8,4 |

> 该文件在 1.12 与 port 中**逐字节同构**（port 仅多 `parent`），无需改动，只需改方块 render shape（§5.2）。

---

## 3. 贴图 / 资源

| 资源 | 1.12 路径 | port 是否存在 | 尺寸 | 说明 |
|---|---|---|---|---|
| `matrix_vat.png` | `assets/isorropia/textures/blocks/matrix_vat.png` | **是**：`port: assets/isorropia/textures/block/matrix_vat.png`（sha256 前 16 位 `f0c503ccd35408f1`，与 1.12 完全一致） | 64×64 | 上半 32 行 = base 立方体贴图，下半 32 行 = overlay（发光）贴图。Isorropia 自有，与 TC4R `models/infuser.png`(`4f11241a…`) **不同**。 |
| 方块模型 | `assets/isorropia/models/block/modified_matrix.json` | **是** | — | 与 1.12 同构 |
| 物品模型 | 1.12 无独立 item json（用 blockstate inventory 变体） | **是**：`models/item/modified_matrix.json` = `parent isorropia:block/modified_matrix` | — | 保留 |
| blockstate | `blockstates/modified_matrix.json` | **是** | — | 保留 |
| halo 贴图 | 无（纯顶点色三角扇） | 不需要 | — | — |
| expunge 粒子贴图 | Thaumcraft `misc/particles.png`（TC6 名字；TC4R 中为打包后的 200×440） | TC4R 自带 | — | 经 `spawnGenericParticle` 走 `particleAtlasUv`，无需自带 |

**无需复制任何贴图**。唯一注意：`matrix_vat.png` 同时被“方块图集精灵”（`isorropia:block/matrix_vat`，静态模型用）
和“独立实体贴图”（BER 的 `RenderType.entityCutout`）引用是允许的（TC4R 对 `textures/models/*` 就是这么干的）。
若坚持决策 7（渲染器皮肤放 `textures/entity/`），可另存一份到 `textures/entity/matrix_vat.png` 供 BER 用，
但**没有必要**。

---

## 4. 当前 port 状态（缺失 / partial / wrong）

| 位置 | 状态 | 证据 |
|---|---|---|
| BER / 模型类 / 发光 RT | **missing** | `port: client/renderer/tiles/` 只有 `RenderVat.java`；`grep -rn "modified_matrix\|ModifiedMatrix" client/` 只有实体渲染器；`ClientSetup` 只注册了 `TilesIS.VAT`（`client/ClientSetup.java:73`）。 |
| 注册 | **missing** | `ClientSetup.onRegisterRenderers`（`:68-84`）里没有 `registerBlockEntityRenderer(TilesIS.MODIFIED_MATRIX.get(), …)`。 |
| 方块 render shape | **wrong** | `port: common/blocks/BlockModifiedMatrix.java` 未覆写 `getRenderShape` → 默认 `MODEL`，世界里会画出静态 8 角块模型；1.12 是 `INVISIBLE`（`1.12: BlockModifiedMatrix.java:47-49`）。加了 BER 后会与旋转立方体重叠。 |
| `TileVat` 动画量 | **missing** | `grep -n "startUp\|craftCount" port: common/tiles/TileVat.java` 无结果；`getStability()` 有（`:885`）、`isActive() :873`、`isInfusing() :877`、`isExpunging() :881`。 |
| 同步 | **partial** | `getUpdateTag()`（`:1042-1054`）与 `readSync()`（`:1073-1083`）含 Mode/Active/Stability/Flux/Aura/AuraNeeded/Expunge/Effigy/EffigyEssentia；缺 startUp / craftCount / 明确 instability。 |
| `TileModifiedMatrix` | **partial** | 已实现 `AspectContainerView.visibleAspects()`（`:59-63`，委托给 vat）→ TC4R 护目镜**自动**画要素图标；`getVat()`（`:25-29`）可用。无护目镜文字，无渲染相关字段。 |
| 护目镜文字 | **missing** | TC4R 无对应 API（§5.6）。 |
| 贴图 / 两个 JSON | **ok** | 见 §3。 |

---

## 5. 1.20.1 目标设计

### 5.1 新增类（建议）

| 类 | 位置 | 职责 |
|---|---|---|
| `ModifiedMatrixModel` | `port: client/model/ModifiedMatrixModel.java` | 照抄 TC4R `InfusionMatrixModel`（包私有，必须自写）：构造函数里 `createLayer().bakeRoot()`，两个 `ModelPart` `base`(texOffs 0,0) / `overlay`(texOffs 0,32)；暴露 `renderBase(pose, vc, light, overlay)` 与 `renderOverlay(pose, vc, light, overlay, argb)`。 |
| `ModifiedMatrixRenderTypes` | `port: client/renderer/MatrixRenderTypes.java`（或 renderer 内联 `Util.memoize`） | 自建 glow RT（见下），因为 `TCClientRenderTypes` 包私有。 |
| `RenderModifiedMatrix` | `port: client/renderer/tiles/RenderModifiedMatrix.java` | `BlockEntityRenderer<TileModifiedMatrix>`，实现 §5.3 的 render。 |

**glow RenderType**（等价 TC4R `infusionMatrixGlow`；`D.5` 要求用 `NativeRenderTypes.create` 以避免混合被 shader JSON 覆盖）：

```java
static final Function<ResourceLocation, RenderType> MATRIX_GLOW = Util.memoize(tex -> NativeRenderTypes.create(
    "isorropia_matrix_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
    RenderType.CompositeState.builder()
        .setShaderState(NativeRenderStates.RENDERTYPE_EYES_SHADER)      // 无光照、不裁剪、不丢 alpha
        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))  // blur=false, mipmap=false
        .setTransparencyState(NativeRenderStates.LIGHTNING_TRANSPARENCY) // (SRC_ALPHA, ONE) = (770,1)
        .setWriteMaskState(NativeRenderStates.COLOR_WRITE)               // 不写深度 = depthMask(false)
        .setCullState(NativeRenderStates.CULL)                           // TC4 旋转立方体是 CULL
        .createCompositeState(false)));                                  // 默认 LEQUAL + NO_LIGHTMAP
```

base 层直接用现成 `RenderType.entityCutout(MATRIX_TEX)`（TC4R 同款）。

### 5.2 方块改动（`BlockModifiedMatrix`）

- 覆写 `getRenderShape(BlockState) → RenderShape.INVISIBLE`（对齐 1.12），物品仍用 `models/item/modified_matrix.json`。
- 保留 `getLightEmission = 7`（`port: :62-65`，1.12 也有发光语义）。
- `onRemove` 里补 `vat.setStartUp(0)` 语义（对齐 `1.12: BlockModifiedMatrix.java:39-46`：破坏矩阵即让 vat 的
  startUp 落回 0），若采纳 §5.4 的“server 端 startUp”则一并重置。

### 5.3 BER 渲染流程（`RenderModifiedMatrix.render`）

```
ticks = cameraEntity.tickCount + partialTick            // 与 TC4R InfusionMatrixRenderer:63 一致
vat   = matrix.getVat()                                 // 客户端查 level.getBlockEntity(below())
poseStack.pushPose()
  poseStack.translate(0.5, 0.5, 0.5)
  if (vat != null):
      poseStack.mulPose(Axis.YP.rotationDegrees((ticks % 360) * startUp))
      poseStack.mulPose(Axis.XP.rotationDegrees(35 * startUp))
      poseStack.mulPose(Axis.ZP.rotationDegrees(45 * startUp))
      if (vat.isExpunging()) poseStack.mulPose(Axis.YP.rotationDegrees((ticks + partialTick) * 30))
  instability = min(6, 1 + instabilityValue * 0.66 * (min(craftCount, 50) / 50))
  // 两遍：base(始终) 与 overlay(仅 active)
  for a,b,c in {0,1}³:
      push; 按 §2.2.2 translate / rotate90 / scale(0.45)
      matrixModel.renderBase(poseStack, buffers.getBuffer(RenderType.entityCutout(TEX)), packedLight, packedOverlay)
      pop
  if (active):
      for a,b,c: push; 同上 pose; matrixModel.renderOverlay(poseStack, buffers.getBuffer(MATRIX_GLOW.apply(TEX)),
                        LightTexture.FULL_BRIGHT, packedOverlay, argb(1, 0.8f*a, 0.1f*a, a)); pop
poseStack.popPose()
if (vat != null && vat.isInfusing())
    ThaumcraftClientVisualsApi.renderInfusionHalo(poseStack, buffers, vat.getCraftCount())  // 内部自带 translate(0.5,0.5,0.5)
if (vat != null && vat.isExpunging())
    ThaumcraftClientEffectsApi.spawnGenericParticle(Minecraft.getInstance().level, Vec3.atCenterOf(pos)... 见 §2.5)
```

注意：halo 必须在**外层的 `popPose()` 之后**、用**方块角 pose** 调用（1.12/1.7.10 也是先 pop 再 drawHalo）。
`argb()` 用 TC4R `InfusionMatrixRenderer:292-294` 的写法（clamp 到 0..255）。

`shouldRenderOffScreen(T) → true`；`getRenderBoundingBox(T)` → `new AABB(pos).inflate(1.0, 1.0, 1.0)`
（对齐 TC4R `:302-306`；halo 最长 `(20*1+5)/20*1 = 1.25`，如发现被裁可放到 `inflate(1.5)`）。

### 5.4 客户端/服务端同步（`TileVat` 需要发出的数据）

BER 需要：`active`、`isInfusing()`、`isExpunging()`、`startUp`、`instabilityValue`、`craftCount`。
现状已有前者三项 + `Stability`（`getStability()`），缺后三项。推荐（最小改动，与 1.12 一致）：

1. `TileVat` 新增字段 `private float startUp;`、`private int craftCount;`。
2. 在 `tickMaster` 末尾按 1.12 `:248` 缓动 `startUp`；按 1.7.10 `:785-818` 维护 `craftCount`
   （注魔中 `craftCount++`，否则 `craftCount = max(0, craftCount - 2)`，上限 50）。
3. instability：**不新增字段**，直接用已同步的 `Stability`：`instabilityValue = Mth.clamp(-stability, 0, 25)`
   （port 的 stability 随注魔递减、可转负，正好对应 TC4 的 instability 增长；见 `port: TileVat.java:345-349`）。
   若要更贴 TC4，可另加 `int instability`（cap 25）并同步。
4. `getUpdateTag()` 增加 `startUp`(Float) 与 `craftCount`(Int)；`readSync()` 增加对应读取。
   `TileModifiedMatrix` 本身**无需**新同步（它通过 `getVat()` 读 vat 的已同步字段）。

> 备选（零服务端改动）：在 `TileModifiedMatrix` 上挂客户端 `BlockEntityTicker`，用已同步的
> `active/infusing/expunge` 本地缓动出 `startUp`/`craftCount`。缺点是 craftCount 只能近似，动画会与 halo 成长
> 不完全同步；**不推荐**。

### 5.5 复用 TC4R 清单（不要重写）

| 需求 | 用 TC4R |
|---|---|
| halo 几何 / 颜色 / Random(245L) | `api/client/ThaumcraftClientVisualsApi.renderInfusionHalo(pose, buffers, craftCount)`（20732；20721 源码没有，编译期用 20732 jar，`javap` 已确认签名） |
| 发光层 RT 状态 | 照抄 `NativeRenderStates`(eyes shader + LIGHTNING + COLOR_WRITE) + `NativeRenderTypes.create` |
| 顶点色 → argb | 照抄 `InfusionMatrixRenderer.argb` |
| 护目镜要素图标 | **已自动**：`TileModifiedMatrix implements AspectContainerView`（`GogglesAspectView.resolve` 会读 `visibleAspects()`） |
| 粒子 | `ThaumcraftClientEffectsApi.spawnGenericParticle`（打包图集 UV 自动处理） |

### 5.6 护目镜文字（1.12 独有，TC6 API）

`1.12: TileModifiedMatrix.java:96-117` 用 TC6 的 `IGogglesDisplayExtended.getIGogglesText()` 输出 5 行
（`getStability().name()` 粗体；`Flux : <n>` 深紫；`Celestia Aura <a>/<b>` 灰蓝 `rgb(0.49019608,0.5568628,0.63529414)`
→ `0x7D8EA2`；`<gain> stability.gain` 金色斜体；`<loss> stability.range<loss> stability.loss` 红色）。
**TC4 与 1.7.10 TH 都没有这个功能**（TH 的矩阵/vat 只实现 `IAspectContainer` → 只显示要素图标），TC4R 也
没有文本 API（只有要素图标）。两个选项：

- **A（推荐，低成本）**：不做文字。用户看到的是 TC4 风格的要素图标浮标（已由 `AspectContainerView` 带来），
  与“1.7.10 原版行为”一致。
- **B（保 1.12 观感）**：自建 `RenderLevelStageEvent` 的 `AFTER_LEVEL` 世界内文本渲染器，条件 = 玩家戴护目镜
  （可用 `RevealingEquipment.hasGogglesOverlay(player)`，或物品实现 `GogglesOverlayGear`）且准星指向该矩阵方块；
  字号/阴影照抄 `00-api-reference.md` §B.9 `renderAmount` 的数值（`translate(0,0.24,-0.1)`、`scale(0.04)`、
  阴影 `0xBF111111` 于 `(14-w,1)`、白字 `-1` 于 `(13-w,0)`、`Font.DisplayMode.SEE_THROUGH`、FULL_BRIGHT）。
  文本本身用 `getIGogglesText()` 等价的本地化键（`stability.<name>`、`stability.gain`、`stability.loss`、`stability.range`）。

### 5.7 注册点

`port: client/ClientSetup.java` 的 `onRegisterRenderers`（`:68` 事件）内加一行：

```java
event.registerBlockEntityRenderer(TilesIS.MODIFIED_MATRIX.get(), RenderModifiedMatrix::new);
```

若采纳 §5.6-B，另在 `ClientSetup` 里订阅 `RenderLevelStageEvent`（Forge 总线，注意是 `MinecraftForge.EVENT_BUS`
而非 MOD 总线）。

---

## 6. 风险 / 只能实机确认

1. **`mirror` 缺失**：TC4 `ModelCube` 的两个构造器都设 `field_78809_i = true`（mirror），TC4R 的
   `InfusionMatrixModel` 没有 mirror。若 `matrix_vat.png` 左右不对称，base/overlay 的 UV 会左右镜像错。
   首个落地版本按 TC4R 写（不 mirror），实机与 1.12 截图比对后决定是否给 `CubeListBuilder` 加 `.mirror()`。
2. **发光层混合被覆盖**（`D.5`）：若图省事用原版 `RenderType.create` 而不是 `NativeRenderTypes.create`，
   1.20.1 的 shader JSON 普通混合可能覆盖 `LIGHTNING_TRANSPARENCY`，导致 overlay 偶发变成普通 alpha 混合。
   只能多场景（开物品栏、同屏多个发光物、粒子多）实机确认。
3. **expunge 粒子双值插值丢失**：TC4R `ChampionGenericParticle` 只有单 alpha/scale，1.12 的
   `alpha 0.5→0.0`、`scale 2.0→5.0` 无法表达，只能取初值。观感（紫色烟雾由小变大、渐隐）会打折，实机微调
   `start/num/inc/scale` 或改用 `spawnMovingWisp` / `spawnSmokeSpiral` 逼近。
4. **startUp 来源**：port 目前无 `startUp`，若只做“客户端本地缓动”，与 1.12 的服务器缓动在断线/重连后相位会
   不同步。推荐 §5.4 的服务端字段 + 同步。
5. **instability 语义**：TC4 的 instability 是 `symmetry + recipeInstability`（0..25 的“配方不稳度”），port 没有
   这个量；本文用 `-stability` 近似。视觉上（抖动幅度 0.01*instability，≤0.06 格）差异很小，但若将来 port 细化
   了稳定度系统，需要重新对齐。
6. **halo 裁切**：halo 最长伸到 ~1.25 格，TC4R 用 ±1 的包围盒。实机看 halo 尖端是否在方块边界被裁，必要时
   `inflate(1.5)`。
7. **`ThaumcraftClientVisualsApi` 的版本**：它不在 20721 源码里（20732 新增）。工程编译用 20732 jar，
   `javap` 已确认 `renderInfusionHalo(PoseStack, MultiBufferSource, int)` 存在；若 TC4R 升级需复查。
8. **物品栏表现**：1.7.10 的物品栏显示的是真 TESR（旋转立方体），1.12 显示的是烘焙静态模型。port 沿用 1.12
   （静态模型，已存在）即可；若想还原 1.7.10 的旋转物品，需要额外做 BEWLR（`builtin/entity` 或
   `thaumcraft:item/tc4_block_entity` 父级），非必须。
9. **startUp 的“active 常亮自转”是 Isorropia 独有**：1.7.10 只在注魔时转。若希望更“TC4 原味”，可改成只有
   `isInfusing()` 时 startUp 才涨。本文按 1.12（Isorropia 发行版）选择。

---

## 7. 源码索引（供实现者定点查阅）

- `1.12: client/renderer/tiles/RenderModifiedMatrix.java:29-31, 34-79, 81-219`（模型/贴图、drawHalo、主体）
- `1.12: common/tiles/TileModifiedMatrix.java:91-117`（getMaster、护目镜文字）
- `1.12: common/tiles/TileVat.java:248`（startUp 缓动）、`:1426-1531`（getter 群）
- `1.12: common/blocks/BlockModifiedMatrix.java:39-49`（INVISIBLE、breakBlock）
- `1.12: assets/isorropia/models/block/modified_matrix.json`（8 角块）
- `1.7.10: client/renderer/tile/TileVatMatrixRender.java:42-93, 95-208`（drawHalo、主体）
- `1.7.10: client/renderer/block/BlockVatMatrixRender.java:23-45`（物品 TESR、renderWorldBlock=false）
- `1.7.10: common/tiles/TileVatMatrix.java:20-89`（getVat、onWandRightClick）
- `1.7.10: common/tiles/TileVat.java:120,134,144,145,290-297,699,785-818`（mode/instability/craftCount/startUp 语义）
- `TC4: client/renderers/tile/TileRunicMatrixRenderer.java:30-171`
- `TC4: client/renderers/models/ModelCube.java:9-25`（mirror！）
- `TC4R: client/InfusionMatrixRenderer.java:163-179,186-224,226-294,296-306`
- `TC4R: client/InfusionMatrixModel.java:27-53`
- `TC4R(20732 jar): api/client/ThaumcraftClientVisualsApi.renderInfusionHalo(PoseStack, MultiBufferSource, int)`
- `port: common/tiles/TileVat.java:864-931,936-1090`（getter / 持久化 / 同步）
- `port: common/tiles/TileModifiedMatrix.java:25-63`、`common/blocks/BlockModifiedMatrix.java`
- `port: client/renderer/tiles/RenderVat.java`（本工程 BER 书写范式）、`client/ClientSetup.java:68-84`

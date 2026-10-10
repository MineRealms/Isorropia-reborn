# 01 — 治疗缸（curative vat）渲染与特效移植规格

引用记号沿用 `00-api-reference.md`：`D:` = TC4R 20732 反编译、`S:` = 20721 源码、`TC4:` = TC4 1.7.10、`F47:` = Forge 47。上游两条线：

| 记号 | 树 | 关键文件 |
|---|---|---|
| `1.12` | `H:/MinecraftMods/Isorropia_kedition/src/main/java/fr/wind_blade/isorropia`（assets 同级 `resources/assets/isorropia`） | `client/renderer/tiles/RenderTileVat.java`、`RenderVatSlave.java`、`common/tiles/TileVat.java`、`common/blocks/BlockCurativeVat.java`、`BlockLiquidVat.java`、`common/network/ISPacketFXInfusionSource.java` |
| `1.7.10` | `H:/MinecraftMods/ThaumicHorizons/src/main/java/com/kentington/thaumichorizons` | `client/renderer/block/BlockVatRender.java`、`BlockVatSolidRender.java`、`BlockVatInteriorRender.java`、`client/renderer/tile/TileVatRender.java`、`TileVatSlaveRender.java`、`client/renderer/model/ModelVat.java`、`common/tiles/TileVat.java`、`common/items/WandManagerTH.java` |
| port | `H:/MinecraftMods/Isorropia-1.20.1/src/main/java/fr/wind_blade/isorropia` | `common/tiles/TileVat.java`、`common/vat/VatStructure.java`、`common/blocks/BlockCurativeVat.java`、`BlockLiquidVat.java`、`client/ClientSetup.java` |

## 0. 总体结论（决定“以哪条上游为准”）

1. **缸体几何以 1.12 为准**。1.12 实际出货的渲染是**一个 OBJ**（`models/block/vat_redo.obj`），由 `RenderTileVat` 绑在 `TileVatBottom` 上一次画完整套 3×3×4（`1.12:client/renderer/tiles/RenderTileVat.java:11-46`、`1.12:client/Client.java:45`）。它不是 1.7.10 的降级复制，而是重新建模的成品；配套贴图 `curative_vat.png`(128×128)、`water_still.png`(16×16) 在 port 里**已存在且 sha1 与 1.12 完全一致**（见 §1.3）。1.7.10 的缸体是逐方块 `ISimpleBlockRenderingHandler`（`BlockVatRender` 画玻璃环、`BlockVatSolidRender` 画木台/盖），语义等价但用 16×16 单面贴图拼装。
2. **1.7.10 仍然更好用的两处**：(a) 缸内生物的渲染（`TileVatSlaveRender` 真正渲染活体实体 + 尸体替身），1.12 把它退化成“只画一个 `ModelBiped` 假人”；(b) `WandManagerTH.replaceVat` 的方块元数据布局，是理解“哪些方块是盖、哪些是玻璃环”的权威。
3. **1.12 的 `RenderVatSlave` 是退化复制**：它用固定的 `ModelBiped` + 两张替身贴图代替真实实体；port 里“缸内生物是真实实体”（`port:common/tiles/TileVat.java:775-798` `setContained` 会 `level.addFreshEntity`），所以**生物本体不需要 BER**，只有尸体替身（mode 3/4）需要补一个渲染。
4. **1.12 的两个 JSON 模型集（`curative_vat_*.json`、`classic vurative/`）都是死资产**：`blockstates/curative_vat.json` 所有 variant 指向 `thaumcraft:empty`（`1.12:resources/assets/isorropia/blockstates/curative_vat.json`），没有任何 Java 引用它们（全树 grep `classic` 无命中）。`classic vurative/*` 复刻的是 **1.7.10 原版外观**，且它引用的 20 张 TH 贴图（`vatinner*`/`vatbottom*`/`vattop*`/`vatlid*`）**port 里没有**。因此不作为主方案，只在 §12 作为备选记档。
5. **1.7.10 的 `TileVatRender` + `ModelVat` 是死代码**：`ClientProxy` 从未绑定 `TileVat→TileVatRender`（`1.7.10:client/ClientProxy.java:212-213` 只绑 `TileVatSlave` 与 `TileVatMatrix`）。不要照抄 `ModelVat`。

---

## 1. 缸体外壳（frame / glass / lid）与液面

### 1.1 上游行为

**1.12（出货版，主参考）** — `1.12:client/renderer/tiles/RenderTileVat.java:11-46`，TESR 绑 `TileVatBottom`，`isGlobalRenderer=true`：

```java
GlStateManager.pushMatrix();
GlStateManager.enableBlend();
GlStateManager.blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA);       // glBlendFunc(770,771)
GlStateManager.translate(x + 0.5f, y, z + 0.5f);
GlStateManager.depthMask(false);
bindTexture(isorropia:models/block/water_still.png);
model.renderPart("Water");
GlStateManager.depthMask(true);
bindTexture(isorropia:models/block/curative_vat.png);
model.renderAllExcept("Water");
GlStateManager.disableBlend();
GlStateManager.popMatrix();
```

- 变换只有一个：`translate(x+0.5, y, z+0.5)`，其中 `y` 是**最底方块**的方块角 y。OBJ 自身已经以“底方块中心”为原点（OBJ 坐标 x,z ∈ [-1.5,1.5]，y ∈ [0,4]）。
- 两条绘制顺序：先 `Water`（关深度写、开混色），后其余全部（开深度写）。cull face、lighting、lightmap **都不动**。
- 贴图走的是 `assets/isorropia/models/block/*.png`（注意不是 `textures/`，见 §1.3）。
- OBJ 用 TC6 的 `thaumcraft.client.lib.obj.AdvancedModelLoader`（`RenderTileVat.java:12`），**TC4R 没有这个类，必须改 `forge:obj`**。

OBJ 结构（`1.12:resources/assets/isorropia/models/block/vat_redo.obj`，576 顶点 / 296 面 / 218 vt / 52 vn；`vat_redo.mtl` 两个材质）：

| group | faces | 材质 | 包围盒 (x,y,z)，单位=方块 |
|---|---|---|---|
| `Bottom` | 6 | Main_Side | [-1.375, 0, -1.375] → [1.375, 1, 1.375] |
| `Pipe_Lower` | 108 | Main_Side | [-1.5, 0, -1.5] → [1.5, 0.75, 1.5] |
| `Water` | 24 | Water | [-1.125, 1, -1.125] → [1.125, 3, 1.125] |
| `Glass` | 8 | Main_Side | [-1.375, 0.9943, -1.375] → [1.375, 3.0057, 1.375] |
| `Top` | 6 | Main_Side | [-1.375, 3, -1.375] → [1.375, 4, 1.375] |
| `Pipe_Top` | 108 | Main_Side | [-1.5, 3.125, -1.5] → [1.5, 3.875, 1.5] |
| `Dial_W` / `_N` / `_E` / `_S` | 各 9 | Main_Side | y ∈ [3.4375, 3.75]，贴在各面外侧 ±1.4375…±1.5 |

整体 [-1.5, 0, -1.5] → [1.5, 4, 1.5]。`vt` 范围 0..1（整张贴图）。`mtl`：`Main_Side.map_Kd = curative_vat.png`，`Water.map_Kd = water_still.png`（`vat_redo.mtl:13-14,25-26`）。PNG 尺寸：`curative_vat.png` 128×128、`water_still.png` 16×16。

**1.7.10（TC4 原版语义，备参考）**：

- `1.7.10:client/renderer/block/BlockVatRender.java:20-213`：只处理玻璃块（meta 10）。`tess.setColorRGBA(255,255,255,192)`（玻璃半透明）、`tess.setBrightness(241)`、`renderer.enableAO=false`。按 master 相对偏移 `(dx,dy,dz)` 选贴图：`dy==1` 是上排玻璃（`iconGlassTL/T/TR` = `vattopleft/vattop/vattopright`），`dy==2` 是下排（`iconGlassBL/B/BR` = `vatbottomleft/vatbottom/vatbottomright`）；转角块用 TL/TR 组合，边中块用 `iconGlassT`。随后把方块 bounds 缩到 `(0,0,0)-(0.75,1,0.75)` 之类，再以 `tess.setColorRGBA(255,255,255,255)` 在同方向面画 **`Blocks.water.getBlockTextureFromSide(0)`** —— 这就是 **1.7.10 液面**：水贴图平面贴在玻璃内 0.25 格处，白色、alpha 255。
- `1.7.10:client/renderer/block/BlockVatSolidRender.java:19-324`：木台与盖。`tess.setColorOpaque_F(1,1,1)`、`tess.setBrightness(150)`、AO off（meta 5 与 meta 7 分支把 `enableAO` 复位为 true）。meta 6=底座中心（`iconBaseCenter/iconInnerCenter`），meta 4=底排木块（`iconBaseSide/iconBaseSideBottom` + 顶面按相对方向取 `iconInnerPosZ/NegZ/PosX/NegX`），meta 5=盖/侧板（`iconLidSideCenter`/`iconLidLeftRight`/`iconGreatwood`；`dy==3` 时顶面取 `iconInnerCornerA..D`），meta 7=盖中心（`iconLidCenterTop` + 底面 `iconInnerCenter`）。
- 结构权威：`1.7.10:common/items/WandManagerTH.java:64-66` blueprint = 顶层 `{p,p,p}/{p,a,p}/{p,p,p}`（木板环 + 奥术构造体中心 = master）、中间两层 `{g,g,g}/{g,w,g}/{g,g,g}`（玻璃环 + 水中心）、底层同顶层；`replaceVat:85-119` 把水中心块换成 `blockVatInterior`（`BlockVatInteriorRender.renderWorldBlock` 直接 `return false`，**不画任何东西**，`1.7.10:client/renderer/block/BlockVatInteriorRender.java:21-24`）。

### 1.2 精确常量

| 量 | 1.12 值 | 1.7.10 值 |
|---|---|---|
| 变换 | `translate(x+0.5, y, z+0.5)`（y = 底方块） | 无 TESR；逐方块面绘制 |
| blend | `SRC_ALPHA, ONE_MINUS_SRC_ALPHA`（770/771）全程开 | 玻璃 `setColorRGBA(255,255,255,192)` |
| depth mask | 仅 Water 段 `false` | 无 |
| cull / lighting | 不修改（默认开启） | AO off（`enableAO=false`） |
| 亮度 | 不改（继承方块 pass lightmap） | 玻璃 `setBrightness(241)`；木台/盖 `setBrightness(150)` |
| 颜色 | 白（顶点色不设） | 水 `(255,255,255,255)`；木台 `(1,1,1)` |
| 液面 | OBJ `Water` 盒，用 `water_still.png` 全贴图 | `Blocks.water` still 图标，平面在玻璃内 0.25 |
| 体积 | 3×3，y 0→4 | 3×3×4 |

### 1.3 贴图/资产

| 文件 | 尺寸 | port 现状 |
|---|---|---|
| `isorropia:models/block/vat_redo.obj` + `.mtl` | 576v/296f | **缺**（需从 `1.12:resources/assets/isorropia/models/block/` 复制；1.20.1 应放到 `assets/isorropia/models/block/` 下，OBJ 走 `forge:obj` 不进方块图集） |
| `models/block/curative_vat.png`（1.12 的 OBJ 皮肤） | 128×128，sha1 `bd232efdad` | port `textures/block/curative_vat.png` 同名同尺寸同 sha1 ✅（但路径要符合 `forge:obj` 的方块图集要求，见 1.5） |
| `models/block/water_still.png` | 16×16，sha1 `b96b1a88a1` | port `textures/block/water_still.png` 同 sha1 ✅；另建议直接用 `minecraft:block/water_still`（见 1.5） |
| `textures/blocks/vat_side.png` | 64×64 | port `textures/block/vat_side.png` ✅ |
| `textures/blocks/vat_misc.png` | 64×64 | port `textures/block/vat_misc.png` ✅ |
| `textures/blocks/vat_top.png` / `vat_under.png` | 64×64 | port 同名 ✅ |
| `textures/blocks/matrix_vat.png` | 64×64 | port ✅（供 S6 矩阵用） |
| 1.7.10 `blocks/vat*.png`（25 张） | 多为 16×16（`vat_side.png` 48×64） | port **缺**（只在选 `classic vurative` 方案时需要） |
| 1.7.10 `models/vat.png`（死代码 ModelVat 皮肤） | 448×208 | port 缺，不需要 |

`vat_side/vat_misc/vat_top/vat_under` 这 4 张**只被死资产 `curative_vat_*.json` 引用**，OBJ 并不用它们。

### 1.4 port 现状（缺失）

- `port:src/main/resources/assets/isorropia/models/block/curative_vat.json` 只有 `{"textures":{"particle":"isorropia:block/vat_side"}}` —— **空模型**；`blockstates/curative_vat.json` 5 个 variant 全部指向它。
- 没有任何缸体渲染器：`port:client/ClientSetup.java:37-73` 只注册实体渲染器与模型层，**没有 `registerBlockEntityRenderer`**。
- 行为侧已就绪：`port:common/vat/VatStructure.java:54-84`（3×3×4 布局、master=TOP 中心）、`port:common/tiles/TileVat.java:144-155`（`isMaster()`/`master()`）、`setContained` 会把实体放进缸内（`:787` `setPos(x+0.5, y-1.6, z+0.5)`，注意 port 用的是 master（顶层）坐标）。
- **坐标口径差异**：1.12 的 TESR 锚在**底方块**、`translate(+0.5, +0.0, +0.5)`；port 的 BER 锚在 BE（= master，顶层中心）。换算：原点 = `master + (0.5, -3, 0.5)`。

### 1.5 目标 1.20.1 设计

1. **新建 `client/renderer/VatRenderer implements BlockEntityRenderer<TileVat>`**，在 `port:client/ClientSetup.java` 的 `onRegisterRenderers` 里 `event.registerBlockEntityRenderer(TilesIS.VAT.get(), VatRenderer::new)`（`F47:client/event/EntityRenderersEvent.java:126`）。`TilesIS.VAT` 覆盖所有 `curative_vat` 方块（`port:common/tiles/TilesIS.java:18-19`），所以 renderer 内先判 `if (!be.isMaster()) return;`，并覆写 `shouldRenderOffScreen → true`（等价 1.12 `isGlobalRenderer`）。
2. **OBJ 走 `forge:obj`**（`F47:net/minecraftforge/client/model/obj/ObjLoader.java:48-61`，键见 `00-api-reference.md §D.1`）。在模组总线 `ModelEvent.RegisterAdditional` 注册 `isorropia:block/vat_redo_obj`（不带 `models/` 与 `.json`），JSON：
   ```json
   { "loader": "forge:obj", "model": "isorropia:models/block/vat_redo.obj",
     "flip_v": true, "automatic_culling": true, "shade_quads": true, "emissive_ambient": false,
     "render_type": "minecraft:cutout",
     "textures": { "Main_Side": "isorropia:block/curative_vat", "Water": "isorropia:block/water_still",
                   "particle": "isorropia:block/curative_vat" } }
   ```
   `Main_Side`/`Water` 是 `.mtl` 的 `newmtl` 名，Forge 用它们去 `textures` 里取精灵；**两者都必须在方块图集**：`isorropia:block/curative_vat`、`isorropia:block/water_still` 现在都在 `textures/block/` ✅（`water_still` 若想动起来可换 `minecraft:block/water_still`，那是原版动画精灵）。
3. **BER 内绘制**（顺序照抄 1.12）：`pose.pushPose(); pose.translate(0.5, -3.0, 0.5);` → 先 `Water` 组、后其余。用 `Minecraft.getInstance().getModelManager().getModel(VAT_OBJ)` 取模型，两种写法二选一：
   - 简单：直接 `BlockRenderDispatcher.renderSingleBlock` 不适用（这不是 BlockState），用 `ModelBlockRenderer.renderModel(pose.last(), buffers.getBuffer(rt), null, model, 1,1,1, packedLight, packedOverlay, ModelData.EMPTY, rt)`（签名见 `00 §D.2`），并用 `visibility` 拆成“只画 Water”“只画非 Water”两遍（同一个 OBJ 两个 JSON：一个 `visibility:{Main_Side:false, Glass:false, ...}` 只留 Water，另一个反之）。
   - 或按 group 拆分：`visibility` 逐组关掉。
4. **RenderType**：外壳用 `RenderType.cutout()`（半透明玻璃靠贴图 alpha）或 `RenderType.translucent()`；**液面（Water 组）必须用“写深度=关”的半透明 RT**，否则会挡住缸内实体（1.12 这里 `depthMask(false)`）。建议照抄 `00 §D.5` 的配方建一个 `VAT_WATER` RT：`TRANSLUCENT_TRANSPARENCY` + `COLOR_WRITE`（不写深度）+ `NO_CULL`，shader 用 `RENDERTYPE_ENTITY_TRANSLUCENT_SHADER` 或 `entity_translucent_emissive`（想全亮）。**首版可直接用 `RenderType.translucent()` 试，实机看是否挡实体**。
5. **光照**：外壳用 BER 传入的 `packedLight`（master 处）。液面按 `00 §B.6`（坩埚）做法取 `max(200, packedLight)`，顶点色白、alpha 255；1.12 不染色所以**不要**加 0x4361FF 那套蓝色（那是 TC4R 坩埚自己的水色）。
6. **`flip_v`**：1.12 的 OBJ 是给 TC6 `AdvancedModelLoader` 用的，翻转规则未在源码中核实 → 首落地时必须实机比对（`00 §E` 风险 #9）。若上下颠倒就把 `flip_v` 取反。
7. 备选方案（可省 OBJ/flip_v 风险，观感略糙）：把 OBJ 的几何手工拆成**多方块 JSON 模型**——即把死资产 `curative_vat_base/bottom_solid/top_solid/bottom_translucent/top_translucent.json` 清理后启用（它们用的正是 port 已有的 `vat_side/vat_misc/vat_top/vat_under`）。代价：需要按层/按渲染层拆 5 个模型 + 重写 blockstate，且是 1.12 未出货版本。

### 1.6 风险 / 只能实机确认

- `forge:obj` 的 `visibility` 能否按 `.mtl` 材质名分别过滤（`00 §D.1` 只保证按 `o`/`g` 组名）→ Water 与外壳是两个材质、但 group 名是 `Water`/`Glass`…，**优先用 group 名做 visibility**（`Water` 组只见于液面，正好）。
- `automatic_culling=true` 对非立方体 OBJ 的剔除是否正确（可能漏画内壁）；不行就设 `false`。
- 液面不写深度 + 半透明时与缸内实体、粒子、水雾的叠放顺序只能实机看。
- `flip_v`（见上）。

---

## 2. 缸内生物（contained creature）

### 2.1 上游行为

- **1.7.10（主参考，真正渲染实体）** `1.7.10:client/renderer/tile/TileVatSlaveRender.java:35-105`：绑在 `TileVatSlave`。取正上方 master（`te.getBlockMetadata()==0 && 上方==7`），分支：
  - `entityContained != null && !(instanceof EntityPlayer)`：`mode==1` → 用 `RenderManager.func_147939_a(...)` 带插值渲染、`glColor4f(1,1,1, (800-progress)/800)` 淡入；其余 → `translate(0, 0.1*cos(rad(player.ticksExisted)), 0)` 后 `renderEntitySimple`（上下浮动 ±0.1 格，周期 2π≈6.28 s）。
  - `mode==3`：`rotatef(180, 0,0,1)` + `translate(-x-0.5, -y-1.5+0.1*cos(...), z+0.5)` + `corpse.render(..., 0.1f)`，贴图 `corpseeffigy.png`。
  - `mode==4 || (mode==2 && recipeType==1)`：同上但贴图 `corpseeffigyrevived.png`。
  - `sample != null`：造一个 `EntityItem` 在缸内渲染（血样）。
- **1.12（退化）** `1.12:client/renderer/tiles/RenderVatSlave.java:25-46`：`TileVat` 的 TESR 只做 mode 3 → tx1、mode 4/2&recipeType1 → tx2 两种 `ModelBiped` 假人，`corpse.render(null,0,0,0,0,0,0.1f)`；**不再渲染真实活体**（活体交给原版实体渲染）。

### 2.2 精确常量

- 浮动：`0.1f * cos(toRadians(player.ticksExisted))`（1.7.10/1.12 一致）→ 世界时钟。
- 假人缩放 `0.1f`（`ModelBiped.render(..., 0.1f)`），`rotatef(180, 0,0,1)`，贴图 64×32。
- 1.7.10 起始淡入：alpha = `(800 - progress)/800`（`progress` 到 800 = `CLONE_PROGRESS`，`1.7.10:common/tiles/TileVat.java:82`）。

### 2.3 资产

`corpseeffigy.png` / `corpseeffigyrevived.png`（64×32）：port 已有 `textures/entity/corpseeffigy(.revived).png`，sha1 `766fb08a55` 与 1.12 `textures/models/corpseeffigy.png` 一致 ✅（port 决策 7 把渲染器皮肤放 `textures/entity/`）。

### 2.4 port 现状

- 活体：**ok（无需渲染代码）**。`port:common/tiles/TileVat.java:775-798` `setContained` 会 `level.addFreshEntity(entity)` 并 `setPos` 进缸；实体是真实被追踪实体，客户端由原版实体渲染器画。位置 `(x+0.5, y-1.6, z+0.5)` 是 **master（顶层）** 坐标（1.12 是底方块坐标 `y-2`，语义相同：缸内中段）。
- 尸体替身：**missing**。port 没有 mode 3/4 的假人渲染；`TileVat.MODE_EFFIGY=3`/`MODE_SELF_READY=4` 已定义（`port:.../TileVat.java:74-75`），`hasEffigy`/`effigyEssentia` 已同步。

### 2.5 目标 1.20.1 设计

- 活体：**不做任何事**（别重复渲染；1.12 的 `RenderVatSlave` 活体分支在 1.12 本就不存在）。
- 尸体替身：在 `VatRenderer` 里加一段——`be.getMode()==3` → 画 `HumanoidModel` 用 `textures/entity/corpseeffigy.png`；`==4` → `corpseeffigyrevived.png`。做法照 `00 §D.9` 的 `ModelRenderer→ModelPart` 对照：`RegisterLayerDefinitions` 里 bake 一个 64×64 的 `LayerDefinition`（1.7.10 `ModelBiped` 的 UV 布局），BER 内 `translate(0.5, -3+?, 0.5); rotY(180); rotZ(180)` + `translate(0, 0.1*cos(playerTickCount), 0)`，`scale(0.1)`（模型单位 1/16 → `ModelPart` 已按 1/16，实际 `scale` 需实机调）。RT `RenderType.entityCutoutNoCull(tex)`，light = `LightTexture.FULL_BRIGHT`（1.12 未改光；假人在暗处也看得见，建议 FULL_BRIGHT）。
- 也可更简单：为尸体替身注册一个**自定义实体**（和别的移植做法一致），让原版渲染它。但 port 现在没有载具实体，BER 更省事。
- 位置：1.12/1.7.10 都画在“水柱中段”；port 用 `master + (0.5, -1.6, 0.5)`（与 `setContained` 一致）。

### 2.6 风险

- `ModelBiped`（1.7.10，模型单位是 1/16 且 Y 向下）与 1.20.1 `HumanoidModel` 的姿势/UV 不一，直接照抄数字可能错位 → 实机。
- 假人与缸内实体同时存在（mode 3 时 `contained==null`，无冲突）。

---

## 3. 缸底注魔符文（runes）

### 3.1 上游行为

- **1.7.10** `1.7.10:common/tiles/TileVat.java:785-802` `doEffects()`：`mode==2` 时，`craftCount==0` → `world.playSound(...,"thaumcraft:infuserstart",0.5,1.0,false)`；否则 `craftCount % 65 == 0` → `"thaumcraft:infuser",0.5,1.0`；`++craftCount`；每 tick 调 `Thaumcraft.proxy.blockRunes(world, x, y-2, z, 0.5f+rand*0.2f, 0.1f, 0.7f+rand*0.3f, 25, -0.03f)`。
- **1.12** `1.12:common/tiles/TileVat.java:321-331`：条件换成 `this.infusing`；`SoundsIR.curative_infusion_start`（`craftCount==0`）/ `curative_infusion_loop`（`craftCount % 65 == 0`）；`FXDispatcher.INSTANCE.blockRunes(x, y-2.5, z, rand*0.2f, 0.1f, 0.7f+rand*0.3f, 25, -0.03f)`。`craftCount` 收尾 wind-down（`:332-340`）：每 tick −2，下限 0，上限 50（`CRAFTCOUNT_WIND_DOWN_MAX`）。
- 常量：1.7.10 `INFUSER_SOUND_INTERVAL=65`、`INFUSER_PARTICLE_COUNT=25`（`1.7.10:.../TileVat.java:88,118`）。

### 3.2 目标 1.20.1 设计

- 用 `ThaumcraftClientEffectsApi.spawnBlockRune(level, Vec3 pos, r, g, b, duration, gravity)`（`00 §A.1`，内部 +0.5，`duration ≤ 400`）：`spawnBlockRune(clientLevel, new Vec3(mx, my-2.5, mz), rand*0.2f, 0.1f, 0.7f+rand*0.3f, 25, -0.03f)`。**以 1.12 数值为准**（Isorropia 出货值），括号里注明 1.7.10 的 `r=0.5+rand*0.2`、`y-2`。
- 触发点：客户端 tick。因为 `craftCount` 只是本地动画计数，**建议客户端自管**（`ClientTickEvent` 或 BER 内累加 + `Mode` 同步判断 `isInfusing()`）。port 的 `Mode` 已同步，`isInfusing()` 可直接判定。
- 声音：`level.playLocalSound` 用 `SoundsIS.CURATIVE_INFUSION_START/LOOP`（port 已注册，`port:common/sounds/SoundsIS.java:16-17`），`SoundSource.BLOCKS`，volume 0.5，pitch 1.0，位置 = master。

### 3.3 port 现状 / 资产

- **missing**：port 没有任何 vat 客户端特效；`common/sounds/SoundsIS.java` 5 个音效已注册、`sounds.json` 与 5 个 `.ogg` 都在 ✅（`port:src/main/resources/assets/isorropia/sounds.json`、`sounds/curative_*.ogg`）。
- 符文用 TC4R 打包的 `misc/particles.png` 中的 `RUNED_STONE_RUNE` 精灵 → **不要自己写 UV**（`00 §A.1` `spawnBlockRune` 已封装）。

---

## 4. 天体灵气吸收（celestial aura absorption）

### 4.1 上游行为

**1.12** `1.12:common/tiles/TileVat.java:322-324`（在 `doEffects`）：

```java
if (this.celestialAuraNeeded > 0.0f && this.canDrawnCelestialAura())
    ISFXDispatcher.fxAbsorption(x+0.5, y+1.5 + (celestialAuraNeeded > celestialAura ? 3.0 : -3.5), z+0.5,
                                0.49019608f, 0.5568628f, 0.63529414f, celestialAuraNeeded > celestialAura);
```

即颜色 `(0.49019608, 0.5568628, 0.63529414)` ≈ `(125,142,162)`，呈 **上/下二态**（需要更多灵气 → 起点在缸上方 +3.0 格、往下吸；已采够 → −3.5 格、往上吐）。`FXCelestialAbsorption`（`1.12:client/fx/FXCelestialAbsorption.java`）在构造里一次性生成 **50 个 `FXGeneric`**：

- `absorb=true`：`fb.setLoop(true)`、`setGravity(sp?0:0.125f)`、`setLayer(0)`、`setSlowDown(0.995)`；
- `absorb=false`：起点加 `(rand-0.5, 0, rand-0.5)` 抖动、`setParticles(sp?320:512, 16, 1)`、`setGravity(-0.1f)`、`setSlowDown(0.95)`；
- 公共：`age = 30 + rand(20)`；`setRBGColorF(red, blue, green)`（**注意源码把 blue/green 传反**，`:51`）；`alphas[]` 长度 `6 + rand(age/3)`，内部随机、`alphas[0]=1`；`setScale({0.5f, 0.125f})`；`setRandomMovementScale(0.0025f, 0.001f, 0.0025f)`；`ParticleEngine.addEffectWithDelay(world, fb, 2 + rand(3))`；父粒子 `particleMaxAge=3`、`setSize(0.02,0.02)`。
- `sp` = `rand < 0.2`。

### 4.2 目标 1.20.1 设计

- TC4R **有 FCGeneric 等价**：`ThaumcraftClientEffectsApi.spawnGenericParticle(level, pos, Vec3 velocity, r, g, b, alpha, loop, start, num, inc, maxAge, delay, scale)`（`00 §A.1`）。FXCelestialAbsorption 的 `setParticles(320/512, 16, 1)` ↔ `start=320/512, num=16, inc=1`；但 API 约束 `start 0..255`、`start+num ≤ 256` → **320/512 越界**。两条路：
  1. 用 `spawnGenericParticle` 但把帧起点改到打包图集允许的区间（观感近似）；
  2. 自写一个 `ParticleProvider`（`00 §D.6`）照抄上面的 50 个粒子的参数（推荐，忠于原样；`00 §A.10` 明确说 `drawGenericParticles` 才是一一对应）。
- 触发位置/二态条件照抄 1.12；但 port 的灵气逻辑不同：`port:common/tiles/TileVat.java:368-383` `celestialStep` 是“每周期采 1 点”。建议：**只要 `celestialAuraNeeded > 0` 且 celestial 可采**就在 master 上方播放“吸入”态。
- 客户端需要知道 `celestialAuraNeeded`（已同步）、`celestialAura`（已同步）、`celestialBody`（**未同步**）。`canDrawnCelestialAura()` 的判断（白昼/晴空/天体可采）客户端做得到（`level.canSeeSky` + 本地时间），或干脆**由服务端广播一次开始/结束**。

### 4.3 风险

- `spawnGenericParticle` 的帧号上限（上）；`setRBGColorF(red, blue, green)` 的蓝绿互换是上游 bug，**照抄即忠实，修掉即更好看**——必须写清选了哪个。
- 颜色是灰蓝 `(125,142,162)`；在 TC4R 打包图集下（`WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`）观感可能偏亮/偏白，实机比。

---

## 5. flux 排放（expunge）FX + 音效

### 5.1 上游行为

- **音效（1.12）** `1.12:common/tiles/TileVat.java:341-358`，条件 `active && !infusing && expunge`：
  - `soundExpunge==0 && fluxStocked*2 > 5.821f` → `curative_expunge_flux_start`（0.5,1.0），`soundExpunge=-1`；
  - `fluxStocked*2 > 5.821f && (player.ticksExisted % floor(116.42) == 0 || soundExpunge==-1)` → `curative_expunge_flux_loop`，`soundExpunge = floor(116.42)+1 = 117`；
  - `fluxStocked*2 <= 5.821f && soundExpunge == 1` → `curative_expunge_flux_end`，`soundExpunge=0`；
  - `if (soundExpunge > 1) soundExpunge -= 1`。位置 `(x, y+1, z)`。
  - 常量：`SoundsIR.CURATIVE_EXPUNGE_FLUX_LOOP_LENGTH = 5.821F`（`1.12:common/events/SoundsIR.java`）→ 阈值 `flux > 2.9105`；循环周期 `floor(116.42) = 116` tick。
- **爆炸 FX（1.12）** `killSubject()` 客户端分支 `1.12:.../TileVat.java:1157-1159`：`ISFXDispatcher.fluxExplosion(world, x+0.5, y+1.5, z+0.5)`；`FXFluxExplosion`（`1.12:client/fx/FXFluxExplosion.java:18-42`）：**3 层 × 180 个** `FXGeneric`，初速 `(getRandomSign, getRandomSign, getRandomSign)` 各为 `±rand/1.5`；`setRBGColorF(clamp(384,0,1), 0, clamp(384,0,1), rand, rand, rand)`（即红蓝取 1.0，实际是紫）；`setLoop(true)`、`setLayer(0)`、`setSlowDown(0.955)`；`age=30+rand(20)`；`setParticles(sp?320:512,16,1)`；`setScale(0.5,0.125)`；`setRandomMovementScale(0.0025,0.001,0.0025)`；`addEffectWithDelay(2+rand(3))`。
- 1.7.10 的 flux 排放走 `Thaumcraft.proxy`/`fx`，未见独立 flux 爆炸粒子（`killSubject` 在 1.7.10 里是服务端爆炸）。

### 5.2 目标 1.20.1 设计

- **音效**：客户端听 `Mode`/`Expunge`/`Flux`（都已同步，`port:.../TileVat.java:1042-1053`）驱动。`soundExpunge` 状态**放客户端本地**（1.12 就是 `@SideOnly(CLIENT)` 的字段）。用 `SoundsIS.CURATIVE_EXPUNGE_FLUX_START/LOOP/END`，`SoundSource.BLOCKS`，0.5/1.0，位置 master 上方 1 格。
- **flux 爆炸**：`killSubject` 现在是 `destroyStructure`（`port:.../TileVat.java:836-860`），只在服务端跑，没有客户端粒子分支。**推荐**：服务端在拆结构前发一次演出（`ActionPresentationApi.presentAt(level, isorropia:vat_flux_burst, master+0.5, …)`，`00 §A.8`），客户端 handler 里跑粒子。粒子本身用 \(3×180\) 个 `spawnGenericParticle` 或自写 particle——**成本高，建议降级为 `spawnBurst`/`spawnSparkle` 组合**，并在文档里标注这是 1.12 专属美化的近似。

### 5.3 风险

- 3×180 个长寿命 `FXGeneric` × 每次爆炸 → 1.12 就是性能地雷；TC4R 侧建议按画质采样（`qualityScaledParticleCount`）。
- `MathHelper.clamp(384, 0, 1)` 恒为 0 或 1（384 先被 clamp 到 1）→ 颜色分量取值荒谬，**忠实复刻会得到奇怪颜色**；建议按“紫红”取 `(1,0,1)`。

---

## 6. 基座 → 缸 的“材料吸取”粒子（sourceFX / ISPacketFXInfusionSource）

### 6.1 上游行为

**服务端触发（1.12）**：

- 吸 XP 时 `1.12:.../TileVat.java:752`：`sendToAllAround(new ISPacketFXInfusionSource(this.pos, this.pos, target.getEntityId()), 32 格)`。
- 取基座物品时 `1.12:.../TileVat.java:792`：`sendToAllAround(new ISPacketFXInfusionSource(this.pos, pedestal.getPos(), 0), 32 格)`。
- **1.7.10** 等价物是 `PacketInfusionFX`/`PacketFXEssentiaBubble`（`1.7.10:common/tiles/TileVat.java:1840+` 的 `SourceFX`）。

**客户端 handler** `1.12:common/network/ISPacketFXInfusionSource.java:50-76`：`key = "px:py:pz:color"`；`count = 15`，若 `p2` 是 `TilePedestal` 则 `count = 60`；把 `SourceFX(p2, count, color)` 放进 master 的 `sourceFX` map。

**客户端逐帧消费** `1.12:.../TileVat.java:372-417` `drawnIngredientParticles()`：

- 每项 `--fx.ticks`，`ticks <= 0` 删除。
- `fx.loc == vat 位置`（XP 源）：`player = world.getEntityByID(fx.color)`，对玩家身上随机取 `rand-of-width/height`，**4 次** `drawInfusionParticles4(player.pos, vat(x, y+1, z))`。
- `fx.loc` 是基座：取 `getSyncedStackInSlot(0)`；`1/3` 概率 → `drawInfusionParticles3(ped+rand, ped.y+rand+1, ped.z+rand, vat(x,y+1,z))`；否则若是 `ItemBlock` → **4 次** `drawInfusionParticles2(ped+rand, ped.y+rand+1, ped.z+rand, target=v( x,y+1,z), state, md)`，否则 **4 次** `drawInfusionParticles1(ped.x+0.4+rand*0.2, ped.y+1.23+rand*0.2, ped.z+0.4+rand*0.2, vat.up(), is)`。
- 基座不再是基座 → `fx.ticks = 0`。

### 6.2 目标 1.20.1 设计（关键：**新同步/演出**）

- 三条 `drawInfusionParticles*` 在 TC4R 有直接等价（`00 §A.10`）：`1` → `spawnInfusionItem(level, src, tgt, stack)`；`2` → `spawnInfusionBlock(level, src, tgt, state, pos, face)`；`3` → `spawnAttractedSparkle(level, src, tgt, ()->tgt, 1)`（或 `spawnParticle(INFUSION_SPARKLE, src, tgt)`）；`4` → 同 mode 2。
- 由于这些是**每帧**的调用（不是“调用一次自走”），必须**在客户端循环里调用**：把 `sourceFX` 表搬到**客户端**（`port:common/tiles/TileVat.java` 目前没有该字段），由服务端触发同步。
- 触发通道二选一：
  1. **`ActionPresentationApi.presentAt(level, isorropia:vat_src, master+0.5, pedestal+0.5, null, detail=0/1, value=color)`**（`00 §A.8`，服务端触发、客户端 handler，无需自建包）——**首选**，与 port 现有 `WorldEffectsApi.blockSparkle` 用法同源（`port:common/tiles/TileVat.java:241,321,376,416,449,540`）。
  2. 自建包（照 `port:common/network/IsorropiaNetwork.java:18-31` 的注册模式，注意"id 按注册顺序，追加在末尾"），负载同 1.12。
- port 现在 `ingredientStep`（`port:.../TileVat.java:404-418`）**每周期一次性** consume 一件物品并 `blockSparkle`；1.12 是"先发包（count=60）→ 60 tick 内每帧喷粒子 → 到点再真正消耗"（`itemCount` 倒计时，`1.12:.../TileVat.java:790-801`）。**若想忠实，需要把 port 的 `ingredientStep` 改成 60 tick 的拉取状态机**；否则只能是"瞬时闪光"的近似（当前 port 的行为）。
- port 没有 XP 步骤（`1.12` 的 `xpStep`/`recipeXP` 未移植），所以 XP 吸取粒子（`drawInfusionParticles4`）**没有触发点**——可在 §7 的 instability/harm 事件里复用，或明确记为"不移植"。

### 6.3 风险

- distances: `00 §A.1` Effects API 要求位置 ≤256 格（基座在 12 格内，无问题）。
- 服务端与客户端的基座物品必须一致（`InfusionPedestal.item()`）；1.12 用 `getSyncedStackInSlot(0)`，注意 TC4R 的 `InfusionPedestal` 是否同步物品（`port:common/tiles/TileVat.java:411-413` 已在用 `pedestal.item()`）。

---

## 7. 不稳定：电弧 + 随机伤害演出

### 7.1 上游行为

- **1.12** `1.12:.../TileVat.java:360-368`：`infusing && stability < 0 && rand(200) <= |stability|` → 终点 `xx = x+0.5+(rand-rand)*4`、`zz` 同理、`yy = y+1` 且 `++yy` 直到 `isAirBlock`；`FXDispatcher.INSTANCE.arcLightning(x+0.5, y+1.5, z+0.5, xx, yy, zz, 0.8f, 0.1f, 1.0f, 0.1f)`。
- **1.7.10** `1.7.10:.../TileVat.java:895-905`：`mode==2 && instability>0 && rand(200) <= instability` → `Thaumcraft.proxy.nodeBolt(world, x+0.5,y+0.5,z+0.5, x+0.5+(rand-rand)*2, y+0.5+(rand-rand)*2, z+0.5+(rand-rand)*2)`（**半径 2 不是 4**，且 y 也从中心抖，不是朝上的空气柱）。
- 常量：1.7.10 `INSTABILITY_EFFECT_CHANCE=200`、`MAX_INSTABILITY=25`。
- 服务端还有 13 类事件（`inEvZap` → `PacketFXBlockArc`；`inEvEjectItem` → `PacketFXBlockArc`；`inEvWarp/Harm`），port 已按类别移植（`port:.../TileVat.java:483-517`）但**没有对应粒子包**，目前只调 `WorldEffectsApi.blockSparkle`（`zap()` `:540`）。

### 7.2 目标 1.20.1 设计

- 电弧：`ThaumcraftClientEffectsApi.spawnArc(level, Vec3 start, Vec3 end, EffectColor.rgb(rgb), float height)`（`00 §A.1`，终点自带 sparkle）。1.12 参数 → `start=(mx+0.5, my+1.5, mz+0.5)`，`end=(xx, yy, zz)`，`EffectColor(0.8, 0.1, 1.0)`，`height = 0.1f`。
- 触发：**客户端**（只需 `stability`，已同步）→ 可在 BER/tick 里做，或服务端在 `instabilityEvent` 里 `presentAt`。1.12 是纯客户端条件（用 `world.rand`），**客户端做更省**。
- 1.7.10 的 `nodeBolt` 半径 2 → 若想要"贴着缸内壁"的电弧，用 1.7.10 版；1.12 的 4 格 + 空气柱更像"打到附近的东西"。**以 1.12 为准**（出货版）。
- `inEvZap` 的 `PacketFXBlockArc` → `spawnArc(level, master+0.5, target.pos, EffectColor(rgb), height)`；port 目前只在 `zap()` 里 `blockSparkle(0x9B00FF)`（`port:.../TileVat.java:540`）→ **可替换成 `spawnArc`**（`00 §A.10` 明确 `arcLightning → spawnArc`）。

### 7.3 风险

- `spawnArc` 的参数是 `EffectColor`（记录，分量 ∈[0,1]）；1.12 传的是 float 三元组，直接对应。
- 客户端条件需用**本地** `level.random`（1.12 用 `world.rand`，服务端/客户端各一份 RNG，行为本就不一致）→ 只求观感。

---

## 8. 注魔音效与 `craftCount` 收尾

- 见 §3：`curative_infusion_start`（craftCount==0）/`curative_infusion_loop`（每 65 tick），音量 0.5、音高 1.0、`SoundSource.BLOCKS`、位置 master。
- `craftCount` 在 1.12 同时供 `RenderModifiedMatrix` 的 halo 用（`1.12:client/renderer/tiles/RenderModifiedMatrix.java:97` `craftcount = curative.getCraftCount()`；halo 长度/亮度见 `:40-66`、`drawHalo`）。**port 没有 `craftCount` 字段**（`port:.../TileVat.java` 无）；若 S6 的矩阵渲染要照抄 halo，需要在客户端复现同一计数（本地自增 + wind-down −2/0..50）或服务端同步。
- `RenderModifiedMatrix` 还会读 `getRawStability()`、`getStartUp()`、`isActive()`、`isInfusing()`、`isExpunging()`（`1.12:.../RenderModifiedMatrix.java:94-105`）——见 §9。

---

## 9. `startUp` 动画（供改造矩阵）

### 9.1 上游行为

- 1.12 `updateStartUp()` `1.12:.../TileVat.java:247-249`（每 tick，双端跑）：目标态 = `active && (infusing || expunge)`；向 1 上升 / 向 0 下降，步长 `max(startUp/10, 0.001)`（指数趋近，约 10% 每 tick）。1.7.10 同名逻辑在 `doEffects` 里（`1.7.10:.../TileVat.java:812-819`），只在 `mode==2` 时上升。
- 用途：`RenderModifiedMatrix` 用它做旋转/抖动/发光的"启动系数"（`1.12:.../RenderModifiedMatrix.java:101-103`：`rotY(ticks%360*startup)`、`rotX(35*startup)`、`rotZ(45*startup)`；`:143-145,175-177` 抖动也乘 `startup`；`:197` 发光 alpha 乘 `startup`）。
- 同步：1.12 把 `startUp` 放进 `readSyncNBT/writeSyncNBT`（`1.12:.../TileVat.java:1220,1246`）。

### 9.2 port 现状 / 目标

- **missing**：`port:common/tiles/TileVat.java` 没有 `startUp` 字段，`getUpdateTag()`（`:1042-1053`）、`saveAdditional`（`:937-973`）里都没有。
- 改造矩阵是 S6；**若 S6 要照抄 1.12 的矩阵外观，就必须补 `startUp`**（建议：客户端本地按 `Mode`+`Expunge`+`Active` 复现 `max(x/10, 0.001)` 的指数曲线，或直接同步一个 float）。
- 本阶段结论：**先在客户端本地复现**（`Mode`/`Expunge`/`Active` 已同步，够算目标态）；不占同步带宽。若实机发现启动抖动与 1.12 不同步再改同步。

---

## 10. blockstate / block model / render layer

### 10.1 上游行为

- **1.12 `BlockCurativeVat`**（`1.12:common/blocks/BlockCurativeVat.java:113-130`）：`shouldSideBeRendered = (meta == BOTTOM)`；`canRenderInLayer = TRANSLUCENT || SOLID`；`getRenderType = ENTITYBLOCK_ANIMATED`；`getRenderLayer = TRANSLUCENT`；`isOpaqueCube/isFullCube/isFullBlock = false`。variant 枚举 `PLACEHOLDER(0)/TOP(1)/BOTTOM(2)/CONNECTOR(3)`（`:191-196`）。
- **1.12 `BlockLiquidVat`**（`1.12:common/blocks/BlockLiquidVat.java:65-72`）：`getRenderLayer = TRANSLUCENT`、`getRenderType = INVISIBLE`、全部 `is*Cube=false`。
- **1.7.10**：`blockVat`（meta 10 玻璃）、`blockVatSolid`（meta 4/5/6/7）、`blockVatInterior`（无渲染）三个块，配 3 个 RI（`1.7.10:client/ClientProxy.java:317-324`）。
- 1.12 的 `blockstates/curative_vat.json` variants 全 `thaumcraft:empty`；`vat_interior.json` 有 `level=0..15` 变体（实际不按 level 渲染）。

### 10.2 port 现状

- `port:common/blocks/BlockCurativeVat.java:142-159`：`Type` 枚举 `PLACEHOLDER/GLASS/TOP/BOTTOM/CONNECTOR`（比 1.12 多一个 GLASS，少 0 号语义差）。`BlockLiquidVat` `:13-16` `RenderShape.INVISIBLE` ✅（正确，1.12 的 INVISIBLE 等价物）。
- `blockstates/curative_vat.json` 5 个 variant 指向**空模型** `models/block/curative_vat.json`。→ **若采用 OBJ+BER 方案，这个 blockstate 要改成 `minecraft:air`/`builtin/entity`（`RenderShape.INVISIBLE` 更直接）**：给 `BlockCurativeVat` 覆写 `getRenderShape` 返回 `RenderShape.ENTITYBLOCK_ANIMATED`（当 `VARIANT != PLACEHOLDER`）或统一 `INVISIBLE`，让原版不再画空模型。
- `vat_interior` 的 blockstate 仍指向 `models/block/vat_interior.json`（只有 particle），`BlockLiquidVat.getRenderShape → INVISIBLE` 已经让该模型不画 ✅（但要保留 particle 贴图供破坏粒子）。

### 10.3 目标 1.20.1 设计

- `BlockCurativeVat.getRenderShape(state)` → `RenderShape.ENTITYBLOCK_ANIMATED`（1.12 的做法）或 `INVISIBLE`（更省，无 BE 的部分由 BER 画）。
- 保留 `BlockLiquidVat.getRenderShape → INVISIBLE`。
- `canRenderInLayer` 用 `BlockBehaviour#getVisualShape`/`supportsExternalFaceCulling` 的 1.20.1 对应物不必照搬（OBJ 路线下无所谓）；`shouldSideBeRendered = (meta == BOTTOM)` 对应 port 的 `Type.BOTTOM`（`1.12:...:114`）——**注意 port 的枚举多了 GLASS，语义要重新映射**。
- `models/block/curative_vat.json` / `vat_interior.json` 保留但简化（particle 用 `isorropia:block/vat_side` / `water_still`）。

---

## 11. 渲染器注册、包围盒与同步清单

### 11.1 包围盒

- 1.12：`TileVat.getRenderBoundingBox = pos+(-3,-3,-3) .. pos+(4,4,4)`（`1.12:.../TileVat.java:135-137`）；`TileVatBottom` 同值（`1.12:common/tiles/TileVatBottom.java:12-14`）。
- port 目标：master 是 TOP 中心；OBJ 展开后世界范围 = `master + (-1,-3,-1) .. master + (2,1,2)`（3×3 水平、4 竖直，与 `VatStructure` 一致）。BER 在 **BE 类**里覆写 `getRenderBoundingBox()`（`00 §D.3`；TC4R 的 `NativeBlockEntityRenderer.getRenderBoundingBox` 不会被 Forge 调用）：
  `new AABB(worldPosition.offset(-1,-3,-1), worldPosition.offset(2,1,2))`。

### 11.2 同步：master 现在同步什么、还缺什么

`port:common/tiles/TileVat.java:1042-1053` `getUpdateTag()` 同步：`Mode, Active, Stability, Flux, Aura, AuraNeeded, Expunge, Effigy, EffigyEssentia`。

本 spec 用到的每一项：

| 用途 | 需要的量 | port 现状 |
|---|---|---|
| 外壳/液面 | 无（恒画） | ✅ |
| 符文（§3） | `Mode`(→isInfusing)、`Active` | ✅ |
| 启动/收尾声音（§3） | `Mode`、`Active` | ✅ |
| 天体吸收（§4） | `AuraNeeded`、`Aura`、`celestialBody` | AuraNeeded/Aura ✅；**`celestialBody` 未同步**（客户端可用天空/时间自己判定，或补同步） |
| flux 排放音效（§5） | `Expunge`、`Flux`、`Active` | ✅ |
| flux 爆炸 FX（§5） | 事件（拆结构时） | **未广播**（建议 `presentAt`） |
| 电弧（§7） | `Stability` | ✅ |
| 基座吸料粒子（§6） | 事件（pedestal 位置 + 颜色/次数） | **未广播**（建议 `presentAt` 或新包） |
| 尸体替身（§2） | `Mode`、`Effigy` | ✅ |
| 矩阵 `startUp`（§9） | `Active`+`Mode`+`Expunge` 可推导 | ✅（本地复现） |
| 矩阵 halo（§8） | `craftCount` | **未同步/未实现**（S6） |
| 缸内生物 | 真实被追踪实体 | ✅（`setContained` addFreshEntity） |
| 缸内要素（护目镜） | `stored` | ✅（`visibleAspects()` `:715-718`，BE 实现 `AspectContainerView` → TC4R 自动画浮标，`00 §B.9`） |

**结论**：本 spec 只需新增两类"事件广播"（吸料、flux 爆炸），其余状态量已同步；`startUp`/`craftCount` 建议客户端本地复现。

### 11.3 注册点

- `port:client/ClientSetup.java:49-67` `onRegisterRenderers`：加 `event.registerBlockEntityRenderer(TilesIS.VAT.get(), VatRenderer::new)`。
- 若 `VatRenderer` 用 `ModelPart`（尸体替身）→ `onRegisterLayers`（`ClientSetup.java:44-47`）加 `registerLayerDefinition`。
- 粒子（§4/§5 若自写）：公共侧 `DeferredRegister<ParticleType<?>>` + 客户端 `RegisterParticleProvidersEvent`（`00 §D.6`）；`ThaumcraftClientEffectsApi` 只能在客户端线程调 → 用 `client/fx/*Fx` 这类"仅客户端辅助类"分发（照 `port:client/fx/CreatureFx.java:8-24` 的写法：`if (!(x.level() instanceof ClientLevel level)) return;`）。

---

## 附：1.12 死资产清单（供选型时明确取舍）

| 资产 | 内容 | 结论 |
|---|---|---|
| `models/block/curative_vat_base.json` | 40 元素，(-16,0,-16)→(32,32,32)，贴图 `vat_side/vat_misc/vat_under/water_still`；组 `north/south/west/east_pipes/glass/water` | 1.12 未出货的新外观 JSON 版（缸的下两层）；**死资产** |
| `models/block/curative_vat_bottom_solid.json` | 29 元素，y 0..16，组 pipes | 死资产 |
| `models/block/curative_vat_top_solid.json` | 37 元素，y 0..16，贴图含 `vat_top` | 死资产 |
| `models/block/curative_vat_bottom_translucent.json` | 11 元素，y 16..32，组 `water/glass` | 死资产 |
| `models/block/curative_vat_top_translucent.json` | 12 元素，y -16..0，组 `glass/water` | 死资产 |
| `models/block/classic vurative/vat_bottom.json` | 29 元素，y 0..32，贴图 = **1.7.10 原版** `vatbasesidecenter/vatinner/vatbottom*/vatbaseleftright/vatinnercorner/greatwood_plank` | 1.7.10 外观的 JSON 复刻；**死资产**，且其 20 张贴图 port 里没有 |
| `models/block/classic vurative/vat_top.json` | 29 元素，y -16..16，贴图 = 1.7.10 `vattop*`/`vatlid*`/`vatinner*` | 同上 |
| 根目录 `assets/isorropia/curative_vat_base.json`、`curative_vat_top.json` | 与 `models/block/` 下同名文件的散落副本 | 垃圾，可忽略 |

（另：`assets/isorropia/blockstates/vat_interior.json` 的 `level=0..15` 变体是 1.12 液位残留，port 已简化为单一 `""` 变体。）

---

## 附：元素速查（用于结构化返回）

| # | 元素 | kind | port 现状 | 难度 |
|---|---|---|---|---|
| 1 | 缸体外壳（OBJ `vat_redo.obj`） | BER + forge:obj | missing | L |
| 2 | 液面（water plane） | BER 内 Water 组 | missing | M |
| 3 | 缸内生物 | 实体（无需渲染） | ok | S |
| 4 | 尸体替身假人 | BER + HumanoidModel | missing | M |
| 5 | 缸底符文 | particle（spawnBlockRune） | missing | S |
| 6 | 天体灵气吸收 | particle（自写/FXGeneric） | missing | M |
| 7 | flux 排放音效 | sound（客户端循环） | missing | S |
| 8 | flux 爆炸 FX | particle | missing | M |
| 9 | 基座吸料粒子（sourceFX） | sync + particle | missing | L |
| 10 | 不稳定电弧 / zap | particle（spawnArc） | partial（仅 blockSparkle） | S |
| 11 | 注魔音效 + craftCount | sound + 客户端状态 | missing | S |
| 12 | startUp 动画 | sync/客户端状态 | missing | S |
| 13 | blockstate/model/renderShape | block model JSON | wrong（空模型 + 未改 renderShape） | S |
| 14 | BER 注册 + 包围盒 | BER registration | missing | S |

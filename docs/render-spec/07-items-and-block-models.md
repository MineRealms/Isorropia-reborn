# 07 — 物品渲染与静态方块/物品模型

范围：Isorropia 里所有**静态** JSON 模型（方块模型 + 物品模型 + Blockbench 模型）、物品形态的渲染
（`TileEntityItemStackRenderer` / `DynamicStaticModel` / `IItemRenderer`）、模型注册（变体、state mapper）、
物品染色（jelly）。不含 BER/实体渲染器（各自文档）、不含透镜 HUD。

引用约定同 `00-api-reference.md`。缩写：
- `IS12:` = `H:/MinecraftMods/Isorropia_kedition/src/main/java/fr/wind_blade/isorropia`
- `ISA12:` = `H:/MinecraftMods/Isorropia_kedition/src/main/resources/assets/isorropia`
- `TH:` = `H:/MinecraftMods/ThaumicHorizons/src/main/java/com/kentington/thaumichorizons`
- `THA:` = `H:/MinecraftMods/ThaumicHorizons/src/main/resources/assets/thaumichorizons`
- `PORT:` = `H:/MinecraftMods/Isorropia-1.20.1/src/main/java/fr/wind_blade/isorropia`
- `PORTA:` = `H:/MinecraftMods/Isorropia-1.20.1/src/main/resources/assets/isorropia`
- `JAR:` = `H:/MinecraftMods/Isorropia-1.20.1/local-repo/dev/tc4port/thaumcraft-forge/0.1.0-20732/thaumcraft-forge-0.1.0-20732.jar`

---

## 0. 结论摘要

1. 本区**几乎全部已落地且正确**：31 个注册物品 + 6 个方块全部有 `models/item/*.json` / `blockstates/*.json`，
   所有 `isorropia:` 贴图引用都存在，父链全部可解析（`minecraft:` / `thaumcraft:`）。
   机械核对见 §5，**没有发现失配/丢失引用**。
2. 唯一的“上游有、移植没有”的东西是有意为之：`lens_gluttony`（决策 6 砍掉）、`soul_jar` 物品
   （1.12 只有幽灵资产 `item/soul_jar.json`，没有对应物品）、`models/block/placeholder.json`
   （1.12 引用不存在的 `isorropia:blocks/transparent`，是死模型）。
3. 需要补的**只有一处结构性问题**：`soul_beacon.json`（以及 `soul_beacon` 方块）是纯静态模型，
   1.12 的 `TileSoulBeacon` + `RenderSoulBeacon` 光束 BER 未移植（属 S6）；本区只需确认静态外壳。
4. 需要实机确认的：`corpse_effigy` 的粒子贴图语义、partial `display` 的父链回退、cat 变体谓词浮点阈值。

---

## 1. 资产总表（1.12 → 1.20 路径与存在性）

### 1.1 物品模型（`models/item/*.json`）

| 物品（registry name） | 1.12 模型 → 贴图 | 1.20 目标 | 贴图位置（1.20） | 状态 |
|---|---|---|---|---|
| `lens` | `item/generated` ← `isorropia:items/base_lens` | `models/item/lens.json` ← `item/generated` ← `isorropia:item/base_lens` | `textures/item/base_lens.png` 16² | OK |
| `lens_air` | ← `items/air_lens` | ← `item/air_lens` | `textures/item/air_lens.png` | OK |
| `lens_ordo` | ← `items/ordo_lens` | ← `item/ordo_lens` | `textures/item/ordo_lens.png` | OK |
| `lens_fire` | ← `items/fire_lens` | ← `item/fire_lens` | 16² OK |
| `lens_envy` | ← `items/envy_lens` | ← `item/envy_lens` | OK |
| `lens_gluttony` | ← `items/gluttony_lens` | **不注册**（决策 6） | `textures/item/gluttony_lens.png` 仍是死资源 | 有意缺失 |
| `jelly` | ← `items/jelly` | ← `item/jelly` | `textures/item/jelly.png` | OK |
| `gold_egg` | ← `items/gold_egg` | ← `item/gold_egg` | OK |
| `incubated_egg` | ← `items/incubated_egg` | ← `item/incubated_egg` | OK |
| `ink_egg` | ← `items/ink_egg` | ← `item/ink_egg` | OK |
| `flesh` | ← `items/flesh` | ← `item/flesh` | OK |
| `somatic_brain` | ← `items/somatic_brain` | ← `item/somatic_brain` | OK |
| `chocolate_bar` | ← `items/chocolatebar` | ← `item/chocolatebar` | OK |
| `chocolate_cream` | ← `items/icecream` | ← `item/icecream` | OK |
| `chocolate_milk_bucket` | ← `items/chocolate_milk_bucket` | ← `item/chocolate_milk_bucket` | OK |
| `corpse_effigy` | Blockbench 内联模型 ← `items/corpseeffigy_x64` | 内联模型 ← `item/corpseeffigy_x64` | `textures/item/corpseeffigy_x64.png` 64² | OK（§2.1） |
| `cat`（18 变体） | 18 个 `cat_*.json` + `cat_steve.json`，贴图 `research/*` | `cat.json` + 18 个 `cat_*.json`（overrides） | `textures/item/cat/*.png`（steve 留 `research/steve.png`） | OK（§2.2） |
| `chocolate`（block item） | `models/block/chocolate.json`（cube_all） | `models/item/chocolate.json` → `block/chocolate` | `textures/block/chocolate.png` 16² | OK |
| `modified_matrix`（block item） | 内联模型 | `models/item/modified_matrix.json` → `block/modified_matrix` | `textures/block/matrix_vat.png` 64² | OK |
| `soul_beacon`（block item） | 内联模型 | → `block/soul_beacon` | `textures/block/soulbeacon.png` 64² | OK |
| `jar_soul`（block item） | `parent thaumcraft:block/jar_normal` + 5 条 `fill` overrides | → `block/jar_soul` → `thaumcraft:block/warded_jar` | `thaumcraft:block/jar_side/top/bottom` | OK（§2.7） |
| `*_spawn_egg` ×11 | 1.12 用 Forge 生成 | `item/template_spawn_egg` | 原版 | OK |
| （幽灵）`soul_jar` | `parent isorropia:block/jar_soul` | **无对应物品，不移植** | — | 有意缺失 |

**1.12 里额外的死资源（不必移植）**：`models/block/placeholder.json`（`block/cube_all` + 不存在的
`isorropia:blocks/transparent`）；`models/item/soul_jar.json`（无物品）；`items/gluttony_lens.png`。

### 1.2 方块模型 / blockstate

| 方块 | 1.12 blockstate | 1.12 model | 1.20 blockstate | 1.20 model | 状态 |
|---|---|---|---|---|---|
| `chocolate` | variants `normal`/`golden`/`inventory`/`facing{n,s,w,e}` | `isorropia:chocolate`（cube_all） | 单 `""` 变体 | `block/chocolate`（cube_all，`isorropia:block/chocolate`） | OK（丢 golden/facing，视觉等价，§2.4） |
| `modified_matrix` | `inventory`/`normal`，`forge:default-block` | `isorropia:modified_matrix`（8 元素，无 parent） | 单 `""` | `block/modified_matrix`（8 元素 + `parent minecraft:block/block`） | OK（§2.5） |
| `soul_beacon` | `inventory`/`normal` | `isorropia:soul_beacon`（14 元素 + 自定义 display） | 单 `""` | `block/soul_beacon`（同 14 元素 + 同 display + parent `block/block`） | OK（§2.6） |
| `jar_soul` | 单 `normal`，model `thaumcraft:jar_normal`，state mapper 忽略 FACING | `thaumcraft:jar_normal` | `facing{n,e,s,w}`（y 旋转） | `block/jar_soul` → `thaumcraft:block/warded_jar` | OK（§2.7） |
| `curative_vat` | `variant` = placeholder/top/bottom/connector + inventory | 4 项均 `thaumcraft:empty` | `variant` = placeholder/top/bottom/connector/**glass** | 全部 → `block/curative_vat`（只有 particle，无元素） | OK，但见 §2.8 |
| `vat_interior` | `normal`/`inventory`/`level=0..15` | `thaumcraft:empty` | 单 `""` | `block/vat_interior` | 死资产（`getRenderShape()=INVISIBLE`），无害 |
| （1.12 专用）`curative_vat_base/bottom_solid/bottom_translucent/top_solid/top_translucent.json`、`classic vurative/vat_top.json`/`vat_bottom.json` | — | 大件 Blockbench 几何 | **未移植为静态 JSON** | `block/vat_redo.obj` + `RenderVat` BER | 属 vat BER 文档；§2.8 |

---

## 2. 逐模型规格

### 2.1 `corpse_effigy`（Blockbench 静态模型，首选 1.12）

**1.12 上游**：`ISA12/models/item/corpse_effigy.json`（完整数值见下）；物品类
`IS12/common/items/misc/ItemCorpseEffigy.java:5-10`（只是一个 `setMaxStackSize(1)` 的 `Item`，**没有任何客户端代码**）。

**1.7.10 上游**：`TH:client/renderer/item/ItemCorpseEffigyRender.java`——用 `ModelBiped` + 贴图
`textures/models/corpseeffigy.png`（64×32 双足皮肤），注册于 `TH:client/ClientProxy.java:253`
（`MinecraftForgeClient.registerItemRenderer(itemCorpseEffigy, new ItemCorpseEffigyRender())`）。
GL 序列（`ItemCorpseEffigyRender.java:37-56`）：`rotZ(180) → rotY(180)`；
INVENTORY：`trans(0,-0.9,0), scale(0.5)`；ENTITY：`trans(0,-3,0)`；其他（EQ/FP）：`scale(0.5) → trans(-1,-4,-1)`；
然后 `corpse.render(null,0,0,0,0,0,0.125f)`。

**用哪个 / 为什么**：**用 1.12**。1.12 把 1.7.10 的双足模型换成了 Blockbench 静态 JSON（8 面立方体拼的人形），
数据自洽、贴图是 64² 的 `corpseeffigy_x64`。1.7.10 的 64×32 双足皮肤在 1.20.1 已无对应（且要写 BEWLR 才能复现）。
移植现状已经是 1.12 版，正确。

**1.12 精确数值**（`ISA12/models/item/corpse_effigy.json`；`texture_size:[64,64]`，贴图槽 `#2 = isorropia:items/corpseeffigy_x64`）：

| 元素 | from | to | 面 UV（north/east/south/west/up/down，单位 1/16 贴图） |
|---|---|---|---|
| Head | [4,24,4] | [12,32,12] | north[2,2,4,4] east[0,2,2,4] south[6,2,8,4] west[4,2,6,4] up[4,2,2,0] down[6,0,4,2] |
| Body | [4,12,6] | [12,24,10] | north[5,5,7,8] east[4,5,5,8] south[8,5,10,8] west[7,5,8,8] up[7,5,5,4] down[9,4,7,5] |
| Right Arm | [12,12,6] | [16,24,10] | north[11,5,12,8] east[10,5,11,8] south[13,5,14,8] west[12,5,13,8] up[12,5,11,4] down[13,4,12,5] |
| Left Arm | [0,12,6] | [4,24,10] | north[12,5,11,8] east[13,5,12,8] south[14,5,13,8] west[11,5,10,8] up[11,5,12,4] down[12,4,13,5] |
| Right Leg | [7.9,0,6] | [11.9,12,10] | north[1,5,2,8] east[0,5,1,8] south[3,5,4,8] west[2,5,3,8] up[2,5,1,4] down[3,4,2,5] |
| Left Leg | [4.1,0,6] | [8.1,12,10] | north[2,5,1,8] east[3,5,2,8] south[4,5,3,8] west[1,5,0,8] up[1,5,2,4] down[2,4,3,5] |

所有元素 `rotation:{angle:0,axis:y,origin:[8,0,8]}`（= 无旋转）。`groups` 只影响 Blockbench 编辑。

**display（1.12 原值，1.20 已原样保留）**：
- `thirdperson_righthand` / `thirdperson_lefthand` / `firstperson_righthand` / `firstperson_lefthand`：仅 `scale 0.3125`
- `ground`：`translate [0,2,0]`，`scale 0.3125`
- `gui`：`rot [30,135,0]`，`translate [0,-2.25,0]`，`scale 0.3125`
- `head`：`translate [0,3.25,0]`，`scale 0.3125`
- `fixed`：`translate [0,-2,0]`，`scale 0.3125`

**贴图差异（1.12 → 1.20）**：1.12 的 `particle` 指向 `isorropia:models/corpseeffigy`（= `textures/models/corpseeffigy.png`，
存在，64×32）；1.20 已改为 `isorropia:item/corpseeffigy_x64`（更合理，`models/` 命名空间被 TC4R 拼进方块图集，见决策 7）。

**移植现状（PORTA）**：`models/item/corpse_effigy.json` 与 1.12 逐元素一致，`texture_size` 保留，
`parent` 无（正确），`textures.2 = isorropia:item/corpseeffigy_x64`、`particle = isorropia:item/corpseeffigy_x64`；
贴图 `textures/item/corpseeffigy_x64.png` 存在且为 64×64。**无客户端代码，无需 BEWLR**。

**目标设计**：纯 JSON，物品类 `PORT:common/items/ItemsIS.java:47` 已是普通 `Item`。**无需改动**。
可选项：若希望未附魔/未使用的 effigy 在手上更好看，可给模型加 `"gui_light": "front"`（1.12 无，保持原样）。

---

### 2.2 `cat` 变体（18 模型 + item property）

**1.12 上游**：`IS12/common/items/misc/ItemCat.java`——`setHasSubtypes(true)`，`EnumCat` 18 项
（`:46-65`：PIG0…STEVE17）；`getSubItems` 只对 `recipe != null` 的项产出（`:25-28`）。
模型注册在 `IS12/common/events/RegistryEventHandler.java:84-86`：
```java
for (int i = 0; i < ItemCat.EnumCat.values().length; i++)
    ModelLoader.setCustomModelResourceLocation(ItemsIS.itemCat, i,
        new ModelResourceLocation(ItemsIS.itemCat.getRegistryName()+"_"+EnumCat.values()[i].getName(), "inventory"));
```
即每 metadata 一个独立模型文件 `isorropia:cat_<name>`。18 个 `models/item/cat_*.json` + `cat_steve.json` 全部
`parent item/generated`，贴图 `isorropia:research/cat_*`（`cat_steve` → `research/steve`、`cat_love` →
`research/cat_instilledloyalty`）。

**1.7.10 上游**：TH **没有** cat 物品（`TH:common/items/` 无 `ItemCat`）；cats 是 Isorropia 相对 TH 的新增。
→ 本元素**只能以 1.12 为参照**。

**移植现状（PORT）**：
- `PORT:common/items/misc/ItemCat.java`：metadata → NBT `Cat` 字符串，`EnumCat` 同名同序（PIG…STEVE，18 项）。
- `PORTA/models/item/cat.json`：`parent isorropia:item/cat_pig` + 18 条 `overrides`，阈值
  `0.00,0.01,…,0.17` 依次对应 pig…steve。
- `PORT:client/ClientSetup.java:41-42`：
  `ItemProperties.register(ItemsIS.CAT.get(), Isorropia.id("cat"), (stack,level,entity,seed)-> ItemCat.getType(stack).ordinal()/100.0F)`。
- 18 个 `models/item/cat_*.json` 全部存在，`parent minecraft:item/generated`；贴图
  `isorropia:item/cat/cat_*.png`（`cat_steve` 例外仍指 `isorropia:research/steve`），全部存在。

**注意点（实机）**：谓词返回 `ordinal/100.0F`，JSON 阈值是 `0.01,0.02,…`。Java 里 `1/100.0F == 0.01f`，
Gson 解析 `0.01` → `asFloat()`，两者应逐位相等；但 `ItemOverrides` 取“最后一个 predicate ≤ 值”的规则
对边界格外敏感，建议实机逐项检视 18 个变体（`cat_pig` 变体在 GUI/手中/掉落物三处）。

**目标设计**：无需新增。若要更稳，可把谓词改为 `(ordinal+0.5)/100.0F`、阈值改为 `0.005,0.015,…`（避免边界），
但这会偏离“1.12 对等”，不建议。

---

### 2.3 普通 `item/generated` 物品

**1.12 / 1.7.10**：全部是单层图标物品。1.7.10 侧对应物用 `registerIcons(...registerIcon("thaumichorizons:<tex>"))`
+ `getIconFromDamage(int)` 返回同一 icon，例：
- `TH:common/items/ItemGoldEgg.java:28-33` → `thaumichorizons:goldegg`
- `TH:common/items/ItemInkEgg.java:28-33` → `thaumichorizons:tenebrousegg`
- `TH:common/items/ItemEggIncubated.java:47-52` → `thaumichorizons:eggincubated`
- `TH:common/items/lenses/ItemLensOrderEntropy.java:322-327` → `thaumichorizons:lensorderentropy`（单一 icon，无子类型）
- 巧克力：`TH:common/items/ItemBarChocolate.java` / `ItemBucketChocolate.java`（贴图 `chocolatebar.png` / `bucket_chocolatemilk.png`）
→ **1.7.10 的 IIcon 在这里没有独立几何**（就是一张 16² 图标），1.12/1.20 的 `item/generated` 与它等价，
**用 1.12 为参照即可**（贴图命名以 1.12 为准：`chocolate_milk_bucket` 而非 TH 的 `bucket_chocolatemilk`）。

**精确规格**：`parent minecraft:item/generated`（`gui_light front`，`layer0` 平铺到 16×16），display 由
`item/generated` 提供（gui/ground/head/thirdperson/firstperson/fixed，见 `client-extra.jar:assets/minecraft/models/item/generated.json`）。
移植现状：全部存在且贴图齐全（见 §1.1）。

**注意**：`chocolate_cream`（贴图 `icecream`）、`chocolate_bar`（贴图 `chocolatebar`）——名字与贴图名不一致是 1.12 原样，保留。

---

### 2.4 `chocolate` 方块

**1.12**：`ISA12/models/block/chocolate.json` = `{"parent":"block/cube_all","textures":{"all":"isorropia:blocks/chocolate"}}`；
`ISA12/blockstates/chocolate.json` 用 `forge_marker:1`，`defaults.model = isorropia:chocolate`，
变体 `normal`/`golden`/`inventory`/`facing{north,south(y180),west(y270),east(y90)}`——**所有变体同一模型**，
即 `golden` 与 `facing` 在视觉上无差别（模型里没有旋转/多贴图）。

**1.7.10**：TH 无 `chocolate` **方块**（只有 `ItemBarChocolate` 物品 + 一个 `chocolate.png` 方块贴图）。
→ 本元素以 **1.12** 为准。

**移植现状（PORTA）**：`blockstates/chocolate.json` 只有 `""` 变体 → `models/block/chocolate.json`
（`parent minecraft:block/cube_all`，`isorropia:block/chocolate`，16² 存在）。丢掉了 `golden`/`facing` 变体，
但 1.12 也只是同模型映射，**视觉等价**。方块类 `PORT:common/blocks/BlocksIS.java:26-28` 是普通 `Block`（无 `variant` 属性）。

**display**：1.12 通过 blockstate 的 `transform: forge:default-block`；1.20 通过 `minecraft:block/cube_all`
→ `minecraft:block/block` 的 display（gui `rot[30,225,0] scale .625`），等价。**Ok**。

---

### 2.5 `modified_matrix` 方块

**1.12**：`ISA12/models/block/modified_matrix.json`——`ambientocclusion:false`，**无 parent**，
贴图槽 `side = isorropia:blocks/matrix_vat`，8 个 7×7×7/7×7×7 立方体拼成 2×2×2 缺中心的对角矩阵（`__createdwith: opl's Model Creator`）。
8 个盒子的 from/to 与逐面 UV：

| # | from | to | 面 UV（up / down / west / east / north / south） |
|---|---|---|---|
| 1 | [0,0,0] | [7,7,7] | up[0,4,4,8] down[8,0,12,4] west[0,4,4,8] east[8,0,12,4] north[8,0,12,4] south[4,0,8,4] |
| 2 | [9,0,9] | [16,7,16] | up[0,4,4,8] down[4,0,8,4] west[8,4,12,8] east[8,4,12,8] north[4,4,8,8] south[4,4,8,8] |
| 3 | [9,9,0] | [16,16,7] | up[4,4,8,8] down[12,4,16,8] west[8,4,12,8] east[4,4,8,8] north[8,4,12,8] south[4,0,8,4] |
| 4 | [0,9,9] | [7,16,16] | up[8,4,12,8] down[12,4,16,8] west[8,0,12,4] east[8,0,12,4] north[4,4,8,8] south[4,0,8,4] |
| 5 | [9,9,9] | [16,16,16] | up[12,4,16,8] down[12,4,16,8] west[8,4,12,8] east[0,4,4,8] north[4,4,8,8] south[8,0,12,4] |
| 6 | [0,0,9] | [7,7,16] | up[0,4,4,8] down[12,4,16,8] west[4,4,8,8] east[8,0,12,4] north[4,4,8,8] south[0,4,4,8] |
| 7 | [0,9,0] | [7,16,7] | up[0,4,4,8] down[12,4,16,8] west[4,0,8,4] east[8,0,12,4] north[12,4,16,8] south[4,0,8,4] |
| 8 | [9,0,0] | [16,7,7] | up[0,4,4,8] down[0,4,4,8] west[8,4,12,8] east[12,4,16,8] north[4,0,8,4] south[4,0,8,4] |

**1.7.10**：TH 无此方块。→ 以 **1.12** 为准。

**移植现状（PORTA）**：`models/block/modified_matrix.json` 与 1.12 **逐元素相等**（脚本比对），
`isorropia:blocks/matrix_vat` → `isorropia:block/matrix_vat`（64² 存在），**新增 `parent minecraft:block/block`**。
`blockstates/modified_matrix.json` 单 `""` 变体。方块类 `BlockModifiedMatrix` 是普通 `Block implements EntityBlock`
（`getLightEmission → 7`），BER `TileModifiedMatrix` 属 S6。

**display**：1.12 靠 `forge:default-block`；1.20 靠 `minecraft:block/block`（等价）。**Ok**。

---

### 2.6 `soul_beacon` 方块

**1.12**：`ISA12/models/block/soul_beacon.json`——Blockbench，`texture_size:[64,64]`，贴图
`0 = isorropia:blocks/soulbeacon`，**14 个元素**（底座 + 中央柱 + 四角小塔饰），每元素 ≤ 6 面，UV 用 1/16 单位。
自定义 `display`（见 §1.2）：`gui rot[30,135,0] trans[0,1,0] scale .75`、`fixed rot[0,90,0] scale .625`、
`ground trans[0,2,0] scale .625`、`thirdperson_l/rhand scale .625`。
（完整 14 元素 from/to/UV 已核对与 1.20 版逐元素相等，此处不再逐行抄；若要复刻请直接 diff
`ISA12` vs `PORTA` 两份 `models/block/soul_beacon.json`，脚本 `json.load(...)['elements']==` 返回 `True`。）

**1.7.10**：TH 有 **soul beacon 的前身**（`TileSoulBeacon` + `RenderSoulBeacon`，光束 BER），方块本体在
`TH:common/ThaumicHorizons.java` 注册；1.7.10 无 JSON 模型，靠 `BlockRenderer`/TESR。1.12 把它固化成静态模型。
→ 静态外壳以 **1.12** 为准；BER（光束）以 **1.7.10/1.12 的 RenderSoulBeacon** 为准（属 S6）。

**移植现状（PORTA）**：元素与 display 与 1.12 逐项相等，纹理 `isorropia:blocks/soulbeacon → isorropia:block/soulbeacon`
（64² 存在），**新增 `parent minecraft:block/block`**。方块类 `BlockSoulBeacon` 是普通 `Block`，
**无 BER**（1.12 的 `RenderSoulBeacon` 未移植）。

**风险（实机）**：`soul_beacon` 只声明了 5 个 display key（gui/fixed/ground/thirdperson_l/rhand），
缺 `firstperson_righthand/lefthand/head`。1.12 无 parent、缺失 key = identity；1.20 有 parent `block/block`
（含 firstperson 的 `rot[0,45,0] scale .4`）。**1.20.1 的 `BlockModel` 是否逐 key 回退到 parent 的 display 需实机确认**：
若逐 key 回退，则第一人称与 1.12 不同（更接近原版方块）；若不回退，则缺失 key = identity（与 1.12 同）。
建议实机在三处检视 `soul_beacon` 物品（GUI、第一人称、掉落物）。

---

### 2.7 `jar_soul`（方块 + 物品形态）

**1.12**：
- 方块：`ISA12/blockstates/jar_soul.json` —— `defaults.model = thaumcraft:jar_normal`，单 `normal` 变体；
  `RegistryEventHandler.java:89` 注册 `new StateMap.Builder().ignore(BlockJarSoul.FACING).build()`
  → **FACING 不进模型**（罐子对称）。方块类 `IS12/common/blocks/BlockJarSoul.java` 是 `BlockContainer`
  + `BlockRenderLayer.TRANSLUCENT` + 碰撞箱 `[0.1875,0,0.1875 → 0.8125,0.75,0.8125]`；BER `RenderJarSoul` 画罐内生物。
- 物品（ItemBlock）：`ISA12/models/item/jar_soul.json` = `parent thaumcraft:block/jar_normal` + 5 条
  `fill` 覆盖（`fill 0→jar_normal`, `1→jar_normal_25`, `2→jar_normal_50`, `3→jar_normal_75`, `4→jar_normal_100`）。
- 自定义物品渲染（本区核心）：`IS12/client/libs/RenderEventHandler.java:68-73`
  `onModelBakeEvent` 把 `isorropia:jar_soul#inventory` 换成 `DynamicStaticModel(jar_soul, ItemBlock)`
  （`IS12/client/model/DynamicStaticModel.java`）——`isBuiltInRenderer()=true`、
  `handlePerspective(type)` 里把 `type` 写进 `RenderCustomItem.transform` 再返回原模型；
  `IS12/client/renderer/RenderCustomItem.java:20-29`（`TileEntityItemStackRenderer`）只对
  `BlocksIS.blockJarSoul` 做 `translate(0.5,0.5,0.4)` 后 `renderItem(stack, RenderEventHandler.jar_soul)`
  并绑方块图集。注册于 `IS12/client/Client.java:72-73`（对实现 `IItemStackRenderProvider` 的物品/方块 item 挂 `RenderCustomItem`）。
- 另有 1.12 死资产 `ISA12/models/block/jar_soul.json`（`ambientocclusion:false` + 两元素 3,0,3→13,12,13 与
  5,12,5→11,14,11 + 自定义 display）和一死物品 `soul_jar`（`models/item/soul_jar.json` → `isorropia:block/jar_soul`）。

**1.7.10**：`TH:client/renderer/tile/ItemJarTHRenderer.java`（`Item.getItemFromBlock(blockJar)`，注册于
`TH:client/ClientProxy.java:298-300`）——`trans(0.5,0,0.5)`；ENTITY 再 `trans(-0.5,-0.25,-0.5)`，
EQUIPPED+player 再 `trans(0,0,-0.5)`；`isSoul` 时画动画灵魂 quad（RT `SRC_ALPHA,ONE_MINUS_SRC_ALPHA`、不写深度、
不裁剪、`renderAnimatedQuad(0.3,0.9,16,(nanoTime/40ms)%16,0,0xFFFFFF)`）；否则把 `TileSoulJar` 交给
`TileEntityRendererDispatcher.instance.renderTileEntityAt`；最后绑方块图集用 `RenderBlocks.renderBlockAsItem(blockJar,0,1)`。
方块/罐体模型见 `TH:client/renderer/tile/TileJarTHRenderer.java`（贴图 `thaumcraft:textures/models/jar.png`）。

**用哪个 / 为什么**：静态外壳与 display → **1.12（也等于 TC4R `warded_jar`）**；罐内生物/灵魂的绘制 →
**1.7.10 `ItemJarTHRenderer`**（1.12 的 `RenderJarSoul` BER 与它同源）。对 1.20.1 而言，**罐体直接用 TC4R
`thaumcraft:block/warded_jar`**（`JAR` 内已确认存在，`parent minecraft:block/block`，`render_type: translucent`，
几何 3..13 罐身 + 5..11 盖，与 1.12 `models/block/jar_soul.json` 完全一致），罐内生物交给 S6 的 BER。

**移植现状（PORTA）**：
- `blockstates/jar_soul.json`：`facing{north,east(y90),south(y180),west(y270)}` → `models/block/jar_soul.json`
  （`parent thaumcraft:block/warded_jar`）。方块类 `PORT:common/blocks/BlockJarSoul.java` 是
  `HorizontalDirectionalBlock`，`SHAPE = box(3,0,3,13,12,13)`（与 1.12 碰撞箱 0.1875..0.8125 → 3..13 / 0..12 一致，**1.12 高度是 0.75=12/16**）。
- `models/item/jar_soul.json`：`parent isorropia:block/jar_soul`（**丢掉了 1.12 的 5 条 `fill` 覆盖**）。
- **无 `RenderCustomItem` / `DynamicStaticModel`**：`PORT` 里既没有这两个类，也没有 `IItemStackRenderProvider`；
  罐内生物渲染完全未做（S6）。
- 1.12 的 `models/block/jar_soul.json`（自定义 display）与 `soul_jar` 死物品**未移植**。

**display 对齐**：1.12 物品模型继承 TC6 `jar_normal` 的 display；1.20 继承 `block/jar_soul` →
`thaumcraft:block/warded_jar` → `minecraft:block/block`，即
`gui rot[30,225,0] scale .625 / ground trans[0,3,0] scale .25 / fixed scale .5 /
thirdperson_r rot[75,45,0] trans[0,2.5,0] scale .375 / firstperson scale .4`。
TC4R 的 `item/warded_jar.json`（`builtin/entity`）用的是同一组数值 → 观感一致。

**目标设计（本区只到静态模型）**：
- 保持现有 `blockstates/jar_soul.json` + `models/block/jar_soul.json`（父 `thaumcraft:block/warded_jar`）。
  1.12 忽略 FACING，1.20 保留 y 旋转——因 `warded_jar` 四面同贴图，**视觉等价，可保留**（FACING 仍供未来 BER 使用）。
- 若需 1.20.1 的“罐内生物在物品里显示”，走 **BEWLR**（`IClientItemExtensions.getCustomRenderer`），
  在 `renderByItem` 里：(a) 先 `renderSingleBlock(warded_jar state, …, RenderType.translucent()/cutout)`，
  (b) 再用 B.3 的盐水/脑罐数值画罐内生物。属 S6，不在本区。
- 1.12 的 `fill` 覆盖**不需要**：`jar_soul` 在移植里不是源质容器（它装生物实体，用 NBT `ENTITY_DATA`）。

---

### 2.8 `curative_vat` / `vat_interior` / 1.12 vat 静态模型

**1.12**：所有 `curative_vat` 变体与 `vat_interior` 都映射到 `thaumcraft:empty`（不可见），
实际几何由 `RenderTileVat`/`RenderVatSlave` 等 BER + 一组 Blockbench 模型
（`curative_vat_base.json` 大件 39 元素、`curative_vat_top_solid.json` 37 元素、`*_translucent.json`、
`classic vurative/vat_top.json`、`vat_bottom.json`）拼出。

**移植现状（PORTA）**：`blockstates/curative_vat.json`（5 个 `variant` 值，含新增 `glass`）→
`models/block/curative_vat.json`（**只有 `particle: isorropia:block/vat_side`，零元素**，即不可见，等价 1.12 `thaumcraft:empty`）；
`vat_interior` 有 blockstate + `models/block/vat_interior.json`，但方块 `PORT:common/blocks/BlockLiquidVat.java:14-16`
`getRenderShape()=INVISIBLE` → **该 blockstate/model 是死资产**（无害）。
正方块几何改由 `models/block/vat_redo.obj` + `PORT:client/renderer/tiles/RenderVat.java:31`（`Isorropia.id("models/block/vat_redo.obj")`）绘制。

**判断**：本区**只需确认上述“不可见占位”，不需要移植 1.12 的 5 个 vat 静态模型**——它们与 vat BER 是同一套东西，
归入 vat BER 文档。`vat_redo.obj`/`.mtl` 的 UV/材质规格见该文档，不在本区复查。
`curative_vat` 的 `variant` 属性覆盖已核对：`PORT:common/blocks/BlockCurativeVat.java:143-147`
（PLACEHOLDER/GLASS/TOP/BOTTOM/CONNECTOR）与 blockstate 的 5 个变体一一对应。

---

### 2.9 Spawn eggs

**1.12**：用 Forge 的 `ItemSpawnEgg`（无 JSON 模型，由 Forge 生成）。**1.7.10**：TH 用 `ItemSpawnEgg` +
`getIconFromDamage` 双色叠加。1.20.1 用原版 `template_spawn_egg`（双 layer，`ItemColors` 自动上色）。
移植现状：11 个 `*_spawn_egg.json` 全为 `parent minecraft:item/template_spawn_egg`，
颜色在 `PORT:common/items/ItemsIS.java:55-65`（`ForgeSpawnEggItem(type, base, 0xA500FF)`，注释说明取自 TH 底色 + 统一紫斑）。
**无需改动**。

---

## 3. 1.12 客户端代码（本区涉及类，逐行）

- **`IS12/common/events/RegistryEventHandler.java`**（131 行）：
  - `:79-91` `@SideOnly(CLIENT) onModelRegistry(ModelRegistryEvent)`：
    对每个 item `registerItemModel`（`resourceName = registryName`，metadata>0 → `+ "_m"+meta`，`:111-122`）；
    cat 走 `:84-86` 逐 metadata 注册；`:89` jar_soul 的 state mapper 忽略 FACING；`:90` 对每个 block 的 item 注册
    单模型（`registerItemRenders`，`:126-131`）。
  - 1.20 对应：ModelResourceLocation 机制被 **`models/item/<registryName>.json` 文件约定 + `ItemProperties` 覆盖** 取代，
    **无 Java 注册代码**（正确）。
- **`IS12/client/Client.java`**：
  - `:72-73` 给 `IItemStackRenderProvider` 的 item/blockitem 挂 `RenderCustomItem`（→ §2.7）。
  - `:79-86` 物品染色：`IItemColor` 对 `itemJelly` 返回 `getAspects(stack).getAspects()[0].getColor()`，
    无要素则 `Aspect.SENSES.getColor()`，注册 `registerItemColorHandler(handler, itemJelly)`（**只对 tintIndex 无判定的整体染色**）。
    1.20 对应：`PORT:client/ClientSetup.java:76-80` `RegisterColorHandlersEvent.Item`，
    `tintIndex==0 ? 0xFF000000 | JellyColors.of(aspect) : -1`（`JellyColors.of(null) → SENSUS 色`，与 1.12 的 SENSES 缺省等价）。
- **`IS12/client/renderer/RenderCustomItem.java`**（30 行）：见 §2.7。1.20 用 BEWLR 取代（S6）。
- **`IS12/client/model/DynamicStaticModel.java`**（72 行）：IBakedModel 包装器，把 `handlePerspective` 的
  transform 透给 `RenderCustomItem`，并强制 `isBuiltInRenderer()=true`、`getOverrides()=NONE`。
  1.20 无等价物（BEWLR 直接拿 `ItemDisplayContext`，不需要这个 hack）。
- **`ItemCat`/`ItemJelly`/`ItemLens`**：见 §2.2/§2.3。
  `ItemLens`（`IS12/common/items/misc/ItemLens.java:5-17`）只有一个 `Lens` 字段 + `setMaxStackSize(1)`；
  1.20 `PORT:common/items/misc/ItemLens.java` 更简（无字段，`stacksTo(1)`）——**无渲染影响**。

---

## 4. 1.7.10 参照（本区）

- **`TH:client/renderer/item/ItemCorpseEffigyRender.java`**（§2.1）。
- **IIcon 用法**：TH 物品 `registerIcons(IIconRegister)` + `getIconFromDamage(int)`（§2.3 例）。
  1.20.1 无 IIcon；`item/generated` + 贴图文件即等价。
- **`TH:client/ClientProxy.java`**：`:249-255` `MinecraftForgeClient.registerItemRenderer(...)`
  （syringe×4 / corpseEffigy / injector / wandCastingDisposable）；`:298-300`
  `Item.getItemFromBlock(blockJar)` → `ItemJarTHRenderer` + `ClientRegistry.bindTileEntitySpecialRenderer(TileSoulJar, …)`
  + `BlockJarTHRenderer`。1.20.1 对应：物品侧 `initializeClient`/BEWLR（D.7），blockitem 侧模型 JSON，BER 侧
  `registerBlockEntityRenderer`。
- **`TH:client/renderer/tile/ItemJarTHRenderer.java`**（§2.7）。

---

## 5. 移植现状机械核对（脚本，2026-10）

方法：解析 `PORT:common/items/ItemsIS.java` 与 `PORT:common/blocks/BlocksIS.java` 的注册名，
逐个核对 `PORTA/models/item/<name>.json`、`PORTA/blockstates/<name>.json`，
再对每个模型解析 `textures`（含 `#ref` 展开）与 `parent`，`isorropia:` 贴图查本地文件，
非 `minecraft:` 的 parent 查 `JAR`。

结果：

| 检查 | 结果 |
|---|---|
| 注册物品（31）→ `models/item/*.json` | **全部存在**，0 缺失 |
| 注册方块（6）→ `blockstates/*.json` | **全部存在**，0 缺失 |
| 有 BlockItem 的方块（`chocolate`/`modified_matrix`/`jar_soul`/`soul_beacon`）→ 物品模型 | **全部存在** |
| `curative_vat`/`vat_interior` | 无 BlockItem（正确，1.12 同） |
| 物品模型里的 `isorropia:` 贴图 | **0 缺失**（含 `corpseeffigy_x64`、`cat/*`、`research/steve`） |
| 方块模型里的 `isorropia:` 贴图 | **0 缺失**（`chocolate` 16²、`matrix_vat`/`vat_side`/`vat_misc`/`vat_top`/`vat_under`/`soulbeacon` 64²、`vat_side` 16²） |
| 父模型 | `minecraft:block/cube_all`、`minecraft:block/block`、`minecraft:item/generated`、`minecraft:item/template_spawn_egg`（原版，已核实存在于 `client-extra.jar`）；`thaumcraft:block/warded_jar`（`JAR` 内存在）；`isorropia:*` 内部父（`cat_pig`、`block/chocolate`、`block/jar_soul`、`block/modified_matrix`、`block/soul_beacon`）**全部存在** |
| `blockstates` 属性覆盖 | `curative_vat` 的 5 个 `variant` 值 ↔ `BlockCurativeVat.Type` 5 值 ✔；`jar_soul` 的 4 个 `facing` ↔ `HorizontalDirectionalBlock.FACING` ✔；其余单 `""` ↔ 方块无属性 ✔ |

**发现的问题**：**无失配、无丢失引用**。仅以下“非错误”项：
1. `soul_beacon` 的 partial `display`（缺 firstperson/head）——见 §2.6 风险。
2. `models/item/jar_soul.json` 丢掉了 1.12 的 5 条 `fill` 覆盖——有意（§2.7）。
3. 死资产（无害）：`blockstates/vat_interior.json`+`models/block/vat_interior.json`（方块 `INVISIBLE`）、
   `textures/item/gluttony_lens.png`、`textures/item/chocolate.png`（若无人引用）、`models/block/curative_vat.json`（仅 particle）。
4. `chocolate` 块丢了 `golden`/`facing` 变体（视觉等价，§2.4）。

---

## 6. 目标 1.20.1 设计（本区）

**本区需要写的 Java：无**（除下述可选增强）。所有物品/方块形态由 **JSON 模型 + ItemProperties** 表达，
这是 1.20.1 的正确做法；1.12 的 `ModelLoader.setCustomModelResourceLocation` / `DynamicStaticModel` 是旧 API，
**不移植**。

清单（全部已就位，无需改动）：
- `PORTA/models/item/cat.json` + 18 个 `cat_*.json` + `ClientSetup.onClientSetup` 的 `ItemProperties.register(CAT, "isorropia:cat", …)`。
- `PORTA/models/item/{lens,lens_air,lens_ordo,lens_fire,lens_envy,jelly,gold_egg,incubated_egg,ink_egg,flesh,somatic_brain,chocolate_bar,chocolate_cream,chocolate_milk_bucket,corpse_effigy}.json`。
- `PORTA/models/item/{chocolate,modified_matrix,jar_soul,soul_beacon}.json` → 对应 `block/*`。
- `PORTA/models/item/*_spawn_egg.json`（11）+ `ForgeSpawnEggItem` 配色。
- `PORTA/models/block/{chocolate,modified_matrix,soul_beacon,jar_soul,curative_vat,vat_interior}.json` + `blockstates/*`。
- `ClientSetup.onItemColors` 的 jelly 染色。

**注册点**：物品/方块在 `ItemsIS.ITEMS`/`BlocksIS.BLOCKS`（`DeferredRegister`，双端）；客户端专属的
`ItemProperties`/`RegisterColorHandlersEvent` 在 `ClientSetup`（`MOD` 总线，`Dist.CLIENT`）。
`ItemProperties` 必须放在 `event.enqueueWork(...)`（模型烘焙线程安全，`ClientSetup.java:41` 已如此）。

**客户端/服务端同步**：静态模型 **不需要** 任何同步。唯一与同步相关的形态是
`corpse_effigy`（无 NBT）、`cat`（NBT `Cat` 字符串，双端各算，无需同步）、`jelly`（NBT 要素，用于染色与
tooltip，双端各算）。`jar_soul` 的罐内生物需要同步（S6 BER）。

**建议（可选）**：
- 给 `jar_soul` 物品加 BEWLR 之前，可先保留纯模型；不要照抄 1.12 的 `DynamicStaticModel`。
- 若要 1.12 的 `chocolate` `golden` 变体，可加 `PropertyBool GOLDEN` + 两条 blockstate 变体
  （当前非损失项，优先级低）。

---

## 7. 风险与仅实机可确认项

| # | 内容 |
|---|---|
| 1 | **`soul_beacon` partial display**：只声明 5/8 个 display key，缺 firstperson_r/lhand/head。1.20.1 `BlockModel` 是否逐 key 回退到 `minecraft:block/block` 的 display 需实机确认（影响第一人称观感）。 |
| 2 | **cat 变体谓词浮点阈值**：`ordinal/100.0F` vs JSON `0.01…0.17`，`ItemOverrides` 的“最后一个 ≤ 值”规则在边界处需实机逐项检视 18 个变体。 |
| 3 | **`corpse_effigy` 的 `particle`**：1.12 用 `isorropia:models/corpseeffigy`（64×32 双足皮肤），1.20 改成 `isorropia:item/corpseeffigy_x64`（64² effigy 贴图）。破坏粒子贴图随之改变——是否可接受需实机看（1.12 的“粒子”其实用错了贴图，1.20 更对）。 |
| 4 | **`jar_soul` 罐内生物**：S6 未做，物品/方块目前只显示空罐。实机确认空罐 + `warded_jar` 贴图观感。 |
| 5 | **`warded_jar` 的 `render_type: translucent`** 会被 `jar_soul` 继承：方块态是半透明玻璃（正确）；但作为**物品**在 GUI 里是否因 translucent 出现排序/z-fighting 需实机看（TC4R 自己这样用，风险低）。 |
| 6 | **`curative_vat` 的 `variant=glass`**（1.12 无此值）在 `BlockCurativeVat` 里存在但 1.12 的 5 个 vat 静态模型未移植；成型后 vat 的可见几何完全依赖 `RenderVat`+`vat_redo.obj`，需 vat BER 文档 + 实机（本区只确认占位不可见）。 |
| 7 | **`modified_matrix`/`soul_beacon` 新增 `parent minecraft:block/block`**：会给模型加原版 block 的标准 display 与 `gui_light: side`。1.12 靠 `forge:default-block`，值一致；但 `soul_beacon` 自带 display 覆盖了大部分 key，剩余 key 的回退见 #1。 |
| 8 | **`vat_interior` 的死 blockstate/model**：`getRenderShape()=INVISIVE`，模型永不绘制——确认 `RenderShape` 拼写（移植代码写作 `INVISIBLE`，正确）后无害。 |

---

## 8. 附：贴图来源对照（1.12/1.7.10 → 1.20 缺失时从哪拷）

以下 1.20 需要的贴图**都已存在**，仅记录来源以备重做：

| 1.20 路径 | 1.12 源 | 1.7.10 源 | 尺寸 |
|---|---|---|---|
| `textures/item/*_lens.png`、`base_lens.png` | `ISA12/textures/items/{air,fire,envy,ordo,gluttony,base}_lens.png` | `THA/textures/items/lens{air,fire,orderentropy,water,earth}.png` | 16² |
| `textures/item/{jelly,flesh,gold_egg,incubated_egg,ink_egg,chocolatebar,icecream,chocolate_milk_bucket,somatic_brain}.png` | `ISA12/textures/items/…` | `THA/textures/items/{chocolatebar,icecream,goldegg,eggincubated,tenebrousegg,…}.png` | 16² |
| `textures/item/cat/*.png`（含 `cat_instilledloyalty`） | `ISA12/textures/research/cat_*.png` | `THA/textures/items/cat.png` 等 | 16² |
| `textures/research/steve.png` | `ISA12/textures/research/steve.png` | 无 | — |
| `textures/item/corpseeffigy_x64.png` | `ISA12/textures/items/corpseeffigy_x64.png` | `THA/textures/models/corpseeffigy.png`（64×32，**不通用**） | 64² |
| `textures/block/{chocolate,matrix_vat,vat_side,vat_misc,vat_top,vat_under,soulbeacon}.png` | `ISA12/textures/blocks/…` | `THA/textures/blocks/{chocolate,…}.png` | 16²（矩阵/vat/soulbeacon 64²） |
| `textures/item/{lens_inner,lens_outer,Len_Inner_Take_3,mirror_amulet,broken_mirror_pane}.png` | 同名（HUD 用，S5） | — | — |

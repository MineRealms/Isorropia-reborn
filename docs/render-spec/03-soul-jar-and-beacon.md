# 03 — 灵魂罐 (soul jar) 与灵魂信标 (soul beacon) 渲染 spec

引用约定同 [00-api-reference.md](00-api-reference.md)：`D:` = 20732 编译期反编译，`S:` = 20721 源码，
`TC4:` = TC4 1.7.10 4.2.3.5，`JAR` = `_tc4_1201/thaumcraft-forge-...-20732.jar`。
上游路径简写：
- **IS12** = `Isorropia_kedition/src/main/java/fr/wind_blade/isorropia`
- **TH17** = `ThaumicHorizons/src/main/java/com/kentington/thaumichorizons`
- **PORT** = `Isorropia-1.20.1/src/main/java/fr/wind_blade/isorropia`

结论速览：

| 元素 | 1.12 (IS12) | 1.7.10 (TH17) | 1.20.1 PORT 现状 | 建议参考 |
|---|---|---|---|---|
| 罐外壳 (jar glass + lid) | 内联 JSON，几何同 TC4R `warded_jar` | `BlockJarTHRenderer` 硬编码 bounds | ✅ 已有（`models/block/jar_soul.json` → `thaumcraft:block/warded_jar`） | 都用，几何完全一致 |
| 罐内生物 (block) | `RenderJarSoul`（有 facing 旋转） | `TileJarTHRenderer`（无旋转） | ❌ 完全没有（无 BE、无 BER） | **1.12**（Isorropia 实际出货版，且带 facing 旋转） |
| 罐内生物 (item) | `RenderCustomItem` **只画空罐** | `ItemJarTHRenderer` **画罐 + 生物** | ❌ 现在只有静态罐模型 | **1.7.10**（1.12 是退化副本） |
| `isSoul` 16 帧灵魂精灵 | 已删除（`fx3` 声明但从不使用） | `TileJarTHRenderer` / `ItemJarTHRenderer` | ❌（也不应做） | 不实现 |
| 信标模型 | 静态 JSON（= 1.7.10 `ModelSoulBeacon` 的逐元素转写） | `ModelSoulBeacon` (Java ModelBase) | ✅ 已有（`models/block/soul_beacon.json`） | 1.12（已是 JSON） |
| 信标光柱 | 委托原版 `TileEntityBeaconRenderer.renderBeamSegment` | 手写 Tessellator（旋转内柱） | ❌ 无 BE、无 BER | **1.12**（1:1 映到 1.20.1 `BeaconRenderer.renderBeaconBeam`） |

---

## 1. 灵魂罐 soul jar

### 1.1 上游行为

**1.7.10 `TileJarTHRenderer`**（TH17 `client/renderer/tile/TileJarTHRenderer.java:32-82`）：

```
if (jarTag.getBoolean("isSoul")) { ...灵魂精灵路径，见 §1.6... ; return; }
glPushMatrix();
glDisable(GL_CULL_FACE);
glTranslatef(x + 0.5f, y + 0.01f, z + 0.5f);     // 方块角 → 罐底中心，抬高 0.01
glColor4f(1, 1, 1, 1);
bindTexture(th.getTexture());                     // "thaumcraft:textures/models/jar.png"（绑定后并未用来画东西）
if (entity != null) {
    glScalef(0.25f, 0.25f, 0.25f);
    entity.setLocationAndAngles(x + 0.5, y + 0.5, z + 0.5, 0, 0);
    RenderManager.instance.getEntityRenderObject(entity).doRender(entity, 0, 0, 0, 0, f);
}
glEnable(GL_CULL_FACE);
glPopMatrix();
```
关键点：**只画实体，不画罐**。罐壳由 `BlockJarTHRenderer.renderWorldBlock`（`ISimpleBlockRenderingHandler`）用两个
`renderStandardBlock` 分段画（bounds `W3,W13,W12,W14` = 3/16,13/16,12/16,14/16）。罐 Body `[3,0,3]→[13,12,13]`、盖 `[5,12,5]→[11,14,11]`。

**1.12 `RenderJarSoul`**（IS12 `client/renderer/tiles/RenderJarSoul.java:19-41`，`TileEntitySpecialRenderer<TileJarSoul>`）：

```java
if (te.entity != null) {
    glPushMatrix();
    glDisable(2884);                                   // GL_CULL_FACE
    glTranslatef((float)x + 0.5F, (float)y + 0.01F, (float)z + 0.5F);
    glRotated(te.getWorld().getBlockState(te.getPos()).getValue(BlockJarSoul.FACING)
                .getHorizontalIndex() * -90 + 180, 0, 1, 0);
    glColor4f(1, 1, 1, 1);
    glScalef(0.25F, 0.25F, 0.25F);
    te.entity.setLocationAndAngles(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ(), 0, 0);
    Render<Entity> r = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(te.entity);
    if (r != null) r.doRender(te.entity, 0, 0, 0, 0, 0);   // 注意：partialTicks 传 0
    glEnable(2884);
    glPopMatrix();
}
```
与 1.7.10 的区别：① 增加了 FACING 的 Y 旋转；② `setLocationAndAngles` 的 z 少写了 `+0.5`（3 处坐标里唯一不一致，但因
`doRender(...,0,0,0)` 只取 pose 原点，实际无影响）；③ 罐壳改由 `models/block/jar_soul.json` 的静态模型绘制（`EnumBlockRenderType.MODEL`，
`BlockJarSoul.java:110-112`）；④ `fx3 = isorropia:textures/misc/soul.png` 被声明（`:16`）但**从不使用**——1.12 删除了整个 `isSoul` 精灵路径。

> **参考取舍**：几何/数值用 **1.12**（Isorropia 真正出货的版本），因为 1.12 才把罐壳交给 JSON、并给罐内生物加了 facing；
> 但 **item 形态用 1.7.10**——1.12 的 `RenderCustomItem` 只画空罐（见 §1.3），把 1.7.10「罐里看得见生物」的能力弄丢了，属退化。

### 1.2 精确常量 — 方块形态（BER）

`glTranslated(x,y,z)` → 1.20.1 的 `poseStack` 已在方块角，**去掉 (x,y,z)**，只保留其后偏移（00-api D.9）。等价 PoseStack 序列：

```java
poseStack.pushPose();
// 无 cull 需要：1.20.1 BER 用 buffer，不需要 glDisable(GL_CULL_FACE)
poseStack.translate(0.5F, 0.01F, 0.5F);
poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - 90.0F * facing.get2DDataValue()));
poseStack.scale(0.25F, 0.25F, 0.25F);
// r,g,b,a = 1,1,1,1（顶点色默认即 white；无需显式）
dispatcher.render(entity, 0, 0, 0, 0.0F, partialTick, poseStack, buffers, packedLight);
poseStack.popPose();
```

- **旋转映射**：1.12 `EnumFacing.getHorizontalIndex()` = SOUTH 0 / WEST 1 / NORTH 2 / EAST 3；1.20.1
  `Direction.get2DDataValue()` **同序**。故 `angle = 180 - 90*idx`：
  SOUTH→180°，WEST→90°，NORTH→0°，EAST→-90°（270°）。
- **缩放**：0.25（1.12 与 1.7.10 均为 `0.25F`，三轴等比）。原版 1.8 高的生物 → 0.45 高，脚在 y=0.01，头 ~0.46，落在 0.75 高的罐体内。
- **颜色/alpha**：`glColor4f(1,1,1,1)` → 顶点色留白，alpha 255。
- **GL 状态**：`glDisable(GL_CULL_FACE)` 一并由 RT 决定（生物自身 RT 已 NO_CULL，无需额外处理）；不关光照（沿用方块的 `packedLight`，1.12 未 `disableLightmap`）。
- **partialTick**：1.12 传 0；1.20.1 传真实 `partialTick` 更佳（无副作用）。

### 1.3 精确常量 — item 形态

**1.12 `RenderCustomItem`**（IS12 `client/renderer/RenderCustomItem.java:20-29`，`TileEntityItemStackRenderer`）：

```java
pushMatrix();
translate(0.5, 0.5, 0.4);
bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
Minecraft.getMinecraft().getRenderItem().renderItem(stack, RenderEventHandler.jar_soul);
popMatrix();
```
`RenderEventHandler.jar_soul`（`client/libs/RenderEventHandler.java:52,68-73`）在 `ModelBakeEvent` 里保存**未包装的**罐方块模型，
再把 `isorropia:jar_soul#inventory` 换成 `DynamicStaticModel(jar_soul, item)`。`DynamicStaticModel`（`client/model/DynamicStaticModel.java`）
把 `isBuiltInRenderer()` 恒置 `true`（`:53-55`）→ 走 TEISR；`handlePerspective` 把相机视角写进 `RenderCustomItem.transform` 并转发给原模型
（`:68-71`）。`IItemStackRenderProvider`（`common/blocks/IItemStackRenderProvider.java`，空标记接口）由 `BlockJarSoulItem` 实现，
`Client.init` 里对实现者批量 `setTileEntityItemStackRenderer(new RenderCustomItem())`（`client/Client.java:72-73`）。
**结论：1.12 的物品形态 = 一个没有生物的静态罐子**（`models/item/jar_soul.json` 甚至 parent 到 `thaumcraft:block/jar_normal` + 5 个 fill override）。

**1.7.10 `ItemJarTHRenderer`**（TH17 `client/renderer/tile/ItemJarTHRenderer.java:39-89`，`IItemRenderer`）：

```
glTranslated(0.5, 0.0, 0.5);
ENTITY 形态   → glTranslatef(-0.5f, -0.25f, -0.5f);
EQUIPPED(玩家) → glTranslatef(0.0f, 0.0f, -0.5f);
if (isSoul)        → 灵魂精灵（§1.6）
else if (hasTag)   → pushMatrix; glRotatef(90,0,1,0); glTranslatef(-0.5,0,-0.5);
                     new TileSoulJar(); th.entity = EntityList.createEntityFromNBT(tag, viewer.world);
                     TileEntityRendererDispatcher.instance.renderTileEntityAt(th, 0, 0, 0, 0);   // 复用罐 TESR 画生物
                     glBlendFunc(770,771); entityRenderer.disableLightmap(0.0);               // 全亮
                     popMatrix;
pushMatrix; glTranslatef(0, 0.5, 0); glEnable(GL_BLEND); glBlendFunc(770,771); glColor4f(1,1,1,1);
bindTexture(TextureMap.locationBlocksTexture); renderBlocks.renderBlockAsItem(ConfigBlocks.blockJar, 0, 1.0f);   // 罐壳
popMatrix;
```
净位移：罐壳 = (0.5, 0.5, 0.5)（居中，与 1.12 的水平居中一致）；生物 = 经 TESR 的 (x+0.5, y+0.01, z+0.5) 后同样落在罐底中心。

### 1.4 罐壳 (jar glass + lid) 与资源

| 资源 | TC4R | PORT | 来源 |
|---|---|---|---|
| `thaumcraft:block/jar_side.png` / `jar_top.png` / `jar_bottom.png`（16²） | ✅ `JAR assets/thaumcraft/textures/block/` | 引用 TC4R，无需自带 | TC4R |
| 方块模型 `warded_jar.json` | ✅ | PORT `models/block/jar_soul.json` = `{"parent":"thaumcraft:block/warded_jar"}` | TC4R |
| 物品模型 | — | PORT `models/item/jar_soul.json` = `{"parent":"isorropia:block/jar_soul"}` | 已有 |
| `isorropia:textures/misc/soul.png`（256×16，16 帧×16²） | 无（TH 自带） | ✅ 已存在（7315 B，= IS12 版本；TH 版 6723 B）；**无 `.mcmeta`** | 仅 isSoul 用，本项目不用 |
| `thaumcraft:textures/models/jar.png`（1.7.10 绑定的罐皮肤） | 无（TC4R 用方块图集） | — | 不需要 |

TC4R `warded_jar.json`（`reference/tc4r-20721-sources/assets/thaumcraft/models/block/warded_jar.json`）：
`render_type: minecraft:translucent`、`ambientocclusion: false`；元素 `[3,0,3]→[13,12,13]`（罐身，down→bottom、up→top、四面→side）
与 `[5,12,5]→[11,14,11]`（盖，down→bottom、up→top、四面→side）。与 1.7.10 `BlockJarTHRenderer.renderWorldBlock`
（`W3=0.1875, W13=0.8125, W12=0.75, W14=0.875`）逐值一致，也与 1.12 内联 JSON 一致 → **罐壳无需再做任何事**。

### 1.5 PORT 现状（missing）

- `common/blocks/BlockJarSoul.java`（36 行）：只有 `FACING` + `SHAPE`，**无 `EntityBlock`、无 `newBlockEntity`**。
- `common/tiles/`：**没有 `TileJarSoul`**；`TilesIS.java` 只注册了 `VAT`、`MODIFIED_MATRIX`。
- `common/blocks/BlockJarSoulItem.java` 不存在；`ItemsIS.JAR_SOUL` 是通用 `BlockItem`（`ItemsIS.java:52`）。
- `client/renderer/tiles/`：只有 `RenderVat`；`ClientSetup.onRegisterBlockEntityRenderers`（`ClientSetup.java:71-74`）只注册 VAT。
- `data/isorropia/loot_tables/blocks/jar_soul.json`：普通掉落，**不保留 NBT**。
- 物品形态：静态罐模型（无生物）。
- `common/infusion/SoulJar.java` 已在**物品**上存好 NBT：`ENTITY_TAG = "EntityData"`（`:24`），`capture()` 用 `target.save(data)` 写入（`:46-48`）。

### 1.6 `isSoul` 灵魂精灵（1.7.10 有、1.12 已删、**不要移植**）

TH17 `TileJarTHRenderer.java:37-64`：`jarTag.getBoolean("isSoul")` 时画一个面向相机的动画四边形，而非生物：
`bindTexture(textures/misc/soul.png)` → `enable(GL_BLEND)`、`alphaFunc(GL_GREATER, 0.003921569F)`、关深度/关裁剪、
`blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)`、`UtilsFX.renderFacingQuad(x+0.5, y+0.4, z+0.5, 0.0f, 0.1f, 0.9f, 16,
(int)(nanoTime/40000000L % 16L), f, 16777215)` → 尺寸 0.9、帧号 = 40 ms/帧（16 帧 640 ms 循环）、满白。
item 版 `ItemJarTHRenderer.java:49-65`：`UtilsFX.renderAnimatedQuad(0.3f, 0.9f, 16, frame, 0, 0xFFFFFF)` 于 `translate(0,0.4,0)`。
**1.12 已彻底删除**（`fx3` 未被引用；1.12 全树无 `isSoul`），PORT 的 `SoulJar` 也不区分，故**不实现**；`textures/misc/soul.png` 保留不用。

### 1.7 目标 1.20.1 设计

**新增/改动（全部新文件，除注明外）**

1. `common/tiles/TileJarSoul.java` — `BlockEntity implements EntityBlock` 配套：
   ```java
   public class TileJarSoul extends BlockEntity {
       @Nullable private CompoundTag entityData;              // NBT key = "EntityData"（复用 SoulJar.ENTITY_TAG）
       @Nullable private LivingEntity cached;                 // 仅客户端：实体缓存
       @Nullable private CompoundTag cachedTag;               // 仅客户端：缓存对应的 tag 快照
       public TileJarSoul(BlockPos pos, BlockState state) { super(TilesIS.JAR_SOUL.get(), pos, state); }
       // saveAdditional/load: tag.put("EntityData", entityData)
       // getUpdateTag(): 只放 "EntityData"（精简；有 tag 才放）
       // getUpdatePacket(): ClientboundBlockEntityDataPacket.create(this)
       // handleUpdateTag(tag)/onDataPacket(conn,packet): readSync(tag) → entityData = tag.getCompound("EntityData"); cachedTag=null;
       // sync(): setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
       @Nullable public LivingEntity clientEntity() { /* 懒建：cachedTag != entityData 时重建 */ }
   }
   ```
   **同步字段清单**：仅 `EntityData`（一个 CompoundTag）。无动画状态、无需客户端 ticker。
   客户端建实体：`EntityType.create(entityData, level)`，失败/非 `LivingEntity` → null。
2. `TilesIS.java` 增 `JAR_SOUL = TILES.register("jar_soul", () -> BlockEntityType.Builder.of(TileJarSoul::new, BlocksIS.JAR_SOUL.get()).build(null))`。
3. `BlockJarSoul` → 实现 `EntityBlock`，`newBlockEntity` 返回 `TileJarSoul`；补 `setPlacedBy`（把 stack 的 `EntityData` 拷进 BE，
   然后 `be.sync()`）与 `playerWillDestroy`（掉落带 `EntityData` 的 `jar_soul` 物品；`Block.popResource` 或
   `Block.dropResources`+自定义）。**这是渲染能出东西的前提**，否则 BE 永远空。
4. `client/renderer/tiles/RenderJarSoul.java` — `BlockEntityRenderer<TileJarSoul>`，`render(...)` 按 §1.2 的序列；
   `getRenderBoundingBox` 用默认（实体 0.25 缩放不出方块）。
   ```java
   LivingEntity e = be.clientEntity();
   if (e == null) return;
   poseStack.pushPose();
   poseStack.translate(0.5, 0.01, 0.5);
   poseStack.mulPose(Axis.YP.rotationDegrees(180F - 90F * state.getValue(BlockJarSoul.FACING).get2DDataValue()));
   poseStack.scale(0.25F, 0.25F, 0.25F);
   Minecraft.getInstance().getEntityRenderDispatcher().render(e, 0, 0, 0, 0F, partialTick, poseStack, buffers, light);
   poseStack.popPose();
   ```
   注册：`ClientSetup.onRegisterBlockEntityRenderers` 增 `event.registerBlockEntityRenderer(TilesIS.JAR_SOUL.get(), RenderJarSoul::new)`。
5. **item 形态（推荐，取 1.7.10 行为）**：新建 `JarSoulItem extends BlockItem` 覆写
   `initializeClient(c -> c.accept(new IClientItemExtensions(){ ... getCustomRenderer() -> new JarSoulItemRenderer(); }))`，
   `ItemsIS.JAR_SOUL` 改用它；把 `models/item/jar_soul.json` 改成 `{"parent": "thaumcraft:item/tc4_block_entity"}`（display 见 00-api B.11）
   或 `builtin/entity` + 自备 display。
   `JarSoulItemRenderer extends BlockEntityWithoutLevelRenderer`：
   ```java
   public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
       // 生物（先画，后画罐让它盖在外面）
       LivingEntity e = SoulJar.peek(stack, Minecraft.getInstance().level);   // level 为 null 则跳过
       if (e != null) {
           pose.pushPose();
           pose.translate(0.5, 0.01, 0.5);
           pose.mulPose(Axis.YP.rotationDegrees(180F));
           pose.scale(0.25F, 0.25F, 0.25F);
           Minecraft.getInstance().getEntityRenderDispatcher().render(e, 0, 0, 0, 0F, Minecraft.getInstance().getFrameTime(), pose, buffers, LightTexture.FULL_BRIGHT);
           pose.popPose();
       }
       // 罐壳：不额外 translate —— 引擎已在调用前 pose.translate(-0.5,-0.5,-0.5)（见下）
       Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
           BlocksIS.JAR_SOUL.get().defaultBlockState().setValue(BlockJarSoul.FACING, Direction.EAST),
           pose, buffers, light, overlay, ModelData.EMPTY, RenderType.translucent());
   }
   ```
   - **为什么罐壳不加 translate**：1.20.1 `ItemRenderer` 在进 custom renderer 之前统一做了
     `poseStack.translate(-0.5F,-0.5F,-0.5F)`；TC4R 自己的 `WardedJarItemRenderer`（`D:client/WardedJarItemRenderer.java`）
     也是 pushPose 后不做 translate 直接 `blocks.renderSingleBlock(state(FACING=EAST), pose, buffer, light, overlay, ModelData.EMPTY, rt)`。
     1.12 的 `translate(0.5,0.5,0.4)` 是 `renderItem` 路径的补偿，BEWLR 里**不需要**（净效果同为水平居中）。
   - **item 光照**：1.7.10 明确 `disableLightmap(0.0)`＝全亮 → 用 `LightTexture.FULL_BRIGHT`(0xF000F0) 画生物；罐壳用传入 `light`。

**可复用的 TC4R 件**：`thaumcraft:item/tc4_block_entity`（display，00-api B.11）；`RenderType.translucent()` 画罐壳；
生物渲染直接走原版 `EntityRenderDispatcher`（无需 TC4R API）。**不要**复用 TC4R `WardedJarRenderer` / `JarClientExtensions`（包私有）。

**注意 `BlockJarSoul` 的 FACING 放置约定**：1.12 用 `placer.getHorizontalFacing()`；PORT 现用
`context.getHorizontalDirection().getOpposite()`（`PORT BlockJarSoul.java:29`）。二者**相反**。若沿用 PORT 现约定，则上述
`180 - 90*idx` 会让罐内生物相对 1.12 反转 180°（朝向玩家，观感其实更好）；若想严格复刻 1.12，改放置为
`context.getHorizontalDirection()`。二选一，勿混。

### 1.8 风险

1. **客户端实体必须不入世界**（不 `addFreshEntity`），只用于渲染；`EntityType.create` 返回的实体 `level()` 已是 ClientLevel，`dispatcher.render` 可用。
2. **不要每帧重建实体**：BEWLR/BER 都按 tag 快照缓存（`cachedTag`）。`SoulJar.peek` 每次新建，必须自己缓存。
3. `EntityRenderDispatcher.render` 对某些实体（船/矿车/玩家渲染器）会加 `getRenderOffset`，会破坏居中——`LivingEntity` 默认 `Vec3.ZERO`，可接受。
4. 罐体 `render_type: translucent` 的方块模型在 translucent pass 绘制，BER 的生物在实体 pass——**玻璃应盖在生物前**，与 1.12/1.7.10 一致；实机确认有没有深度排序毛刺。
5. `handleUpdateTag` 会丢 `EntityData`（若 `getUpdateTag` 写漏）：空罐是常见现象，务必核对。
6. `soul.png` 无 `.mcmeta`：本项目不使用它，别误当动画精灵引用。
7. 1.12 的生物 `partialTicks=0`；PORT 传真实 partialTick，插值会“更顺”，属有意改善。

---

## 2. 灵魂信标 soul beacon

### 2.1 上游行为

**1.7.10 `TileSoulBeaconRender`**（TH17 `client/renderer/tile/TileSoulBeaconRender.java:27-126`）：
1. 模型：`glTranslatef(x+0.5, y+1.5, z+0.5); glRotatef(180, 1,0,0); bindTexture("thaumichorizons:textures/models/soulbeacon.png");
   base.render(null,0,0,0,0,0,0.0625f);` → `ModelSoulBeacon`（§2.4），1/16 缩放、绕 X 翻 180°、抬到方块顶。
2. 光柱：**手写 Tessellator**（`canBlockSeeTheSky` 才画）：
   - `f1 = tco.func_146002_i()` = 0..1 动画缩放；`f2 = worldTime + partialTicks`；`f3 = -f2*0.2 - floor(-f2*0.1)`（V 滚动）；
   - 内柱：4 个角在半径 0.2、角度 135°/45°/225°/315°（2.356194490192345 / 0.7853981633974483 / 3.9269908169872414 / 5.497787143782138），
     绕 Y 按 `d3 = f2*0.025*(1 - 2.5) = -0.0375*f2` **旋转**；高 `d13 = 256*f1`；`u∈[0,1]`，`v ∈ [-1+f3, 256*f1*(0.5/0.2)+(-1+f3)]`；
     颜色 `RGBA(255,255,255,32)`；`glDisable(LIGHTING/CULL/BLEND)`、`glDepthMask(true)`、`glBlendFunc(770,1)`（加色）。
   - 外辉光：固定四角 `0.2/0.2/0.8/0.8`（半宽 0.3，不旋转）；同色；`glEnable(GL_BLEND)`、`glBlendFunc(770,771)`、`glDepthMask(false)`；
     `v ∈ [-1+f3, 256*f1+(-1+f3)]`。
   - 贴图 `textures/entity/beacon_beam.png`（原版），`WRAP_S/T = 10497`。

**1.12 `RenderSoulBeacon`**（IS12 `client/renderer/tiles/RenderSoulBeacon.java:19-37`）：
```java
GlStateManager.disableFog();
if (te.getWorld() == null || !te.getWorld().canBlockSeeSky(te.getPos())) return;
GlStateManager.alphaFunc(516, 0.1F);
bindTexture(TEXTURE_BEACON_BEAM);   // new ResourceLocation("textures/entity/beacon_beam.png")
TileEntityBeaconRenderer.renderBeamSegment(x, y, z, partialTicks, te.shouldBeamRender(),
        (double) te.getWorld().getTotalWorldTime(), 0, (int)(256 - y),
        EnumDyeColor.WHITE.getColorComponentValues(), 0.2, 0.25);
super.render(...); GlStateManager.enableFog();
```
`isGlobalRenderer → true`。**模型**改由 `blockstates/soul_beacon.json` + `models/block/soul_beacon.json` 静态绘制（`EnumBlockRenderType.MODEL`），
BER 只画光柱。

`TileSoulBeacon.shouldBeamRender()`（IS12 `common/tiles/TileSoulBeacon.java:47-62`，客户端有状态）：
```java
int i = (int)(world.getTotalWorldTime() - field_146016_i);   // 距上次调用的 tick 间隔
field_146016_i = world.getTotalWorldTime();
if (i > 1) { scale -= i / 40.0f; if (scale < 0) scale = 0; }  // 跳帧衰减
scale += 0.025f; if (scale > 1) scale = 1;                    // 每帧 +0.025，~40 帧填满
return scale;
```

> **参考取舍**：光柱用 **1.12**（`renderBeamSegment` ↔ 1.20.1 `BeaconRenderer.renderBeaconBeam` 一一对应，且 `0.2/0.25` 与原版信标完全相同）；
> 模型两边都对，且 PORT 已有 JSON。1.7.10 的手写旋转内柱只有在想逐帧复刻 TC4 观感时才需要（1.20.1 原版光柱内柱不旋转）。

### 2.2 精确常量 — 1.12 → 1.20.1 签名映射

1.20.1（javap 核实）：
```java
public static void BeaconRenderer.renderBeaconBeam(PoseStack poseStack, MultiBufferSource bufferSource,
    ResourceLocation location, float partialTick, float textureScale, long gameTime,
    int yOffset, int height, float[] colors, float beamRadius, float glowRadius);
public static final ResourceLocation BeaconRenderer.BEAM_LOCATION;   // "textures/entity/beacon_beam.png"
public boolean shouldRenderOffScreen(...);  public int getViewDistance();
```
- 内部已 `poseStack.translate(0.5, 0.0, 0.5)`（javap 常量 `ldc2_w 0.5d ×2`）→ **BER 的 pose 传方块角即可，不要再 translate**。
- 1.12 的前 3 参 `x,y,z`（相机相对的方块位置）**不要**再传——1.20.1 由 pose 承担。
- `yOffset=0`、`height = 256 - 方块 y`；`colors = DyeColor.WHITE.getTextureDiffuseColors()`（= `{1,1,1}`，对应 1.12 `getColorComponentValues()`）；
  `beamRadius=0.2F`、`glowRadius=0.25F`（与原版信标一致，javap 亦见 `ldc 0.2f`/`0.25f`）。

```java
public void render(TileSoulBeacon be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    Level level = be.getLevel();
    if (level == null || !level.canSeeSky(be.getBlockPos())) return;
    BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, be.beamScale(),
        level.getGameTime(), 0, 256 - be.getBlockPos().getY(),
        DyeColor.WHITE.getTextureDiffuseColors(), 0.2F, 0.25F);
}
```
- `beamScale()`：把 1.12 的有状态逻辑原样搬进 BE（客户端字段，`i>1` 时衰减、每调用 +0.025、上限 1）。BER 每帧只调一次即可。
- 覆盖：`shouldRenderOffScreen(be) -> true`；`getViewDistance() -> 256`（对应 1.12 `getMaxRenderDistanceSquared()=65536`）；
  `getRenderBoundingBox(be) -> INFINITE_EXTENT_AABB`（1.12 `getRenderBoundingBox = INFINITE_EXTENT_AABB`）。
- **不需要** `disableFog`：1.20.1 `RenderType.beaconBeam` 自带雾行为；1.12 的 `disableFog` 是原版 TESR 的习惯写法。

### 2.3 PORT 现状（missing）

- `common/blocks/BlockSoulBeacon.java`（9 行）：只有构造，**无 `EntityBlock`、无 `newBlockEntity`、无 `use`**。
- `TilesIS` 无 SOUL_BEACON；`ClientSetup` 无 `RenderSoulBeacon`。
- 方块模型/blockstate **已有**：`models/block/soul_beacon.json`（= 1.12 版，64×64 Blockbench，13 个 paste）、
  `models/item/soul_beacon.json`、`blockstates/soul_beacon.json`（`""` → `isorropia:block/soul_beacon`）。
- 贴图 **已有**：`assets/isorropia/textures/block/soulbeacon.png`（64×64）。
- `Isorropia` 有 `isorropia.setBeacon` 语言键；`TileSoulBeacon.activate()`（写玩家 `soulBeacon` 坐标 + `arcLightning` + `SoundsTC.zap`）尚未移植（`BlockSoulBeacon` 无右击）。

### 2.4 信标模型（**PORT 已有，无需改动**）

`ModelSoulBeacon`（TH17 `client/renderer/model/ModelSoulBeacon.java:28-101`）64×64，`0.0625` 缩放、`rotX 180`、`translate(+0.5, +1.5, +0.5)`；
元素映射 `y_block = 1.5 − y_model/16`、`x_block = 0.5 + x_model/16`、`z_block = 0.5 − z_model/16`：

| part | texOffs | box(w,h,d) | rotPoint | → 像素 from→to (16³) |
|---|---|---|---|---|
| Base | 0,23 | 16,1,16 | −8,23,−8 | `[0,0,0]→[16,1,16]` |
| Core | 0,0 | 12,11,12 | −6,12,−6 | `[2,1,2]→[14,12,14]` |
| Claw1A | 48,0 | 2,1,2 | 6,12,−1 | `[14,11,7]→[16,12,9]` |
| Claw2A | 48,0 | 2,1,2 | −8,12,−1 | `[0,11,7]→[2,12,9]` |
| Claw3A | 48,0 | 2,1,2 | −1,12,−8 | `[7,11,14]→[9,12,16]` |
| Claw4A | 48,0 | 2,1,2 | −1,12,6 | `[7,11,0]→[9,12,2]` |
| Claw3B | 48,8 | 2,4,1 | −1,8,−8 | `[7,12,15]→[9,16,16]` |
| Claw4B | 48,8 | 2,4,1 | −1,8,7 | `[7,12,0]→[9,16,1]` |
| Claw1B | 48,16 | 1,4,2 | 7,8,−1 | `[15,12,7]→[16,16,9]` |
| Claw2B | 48,16 | 1,4,2 | −8,8,−1 | `[0,12,7]→[1,16,9]` |
| Claw1C | 48,48 | 4,1,2 | 3,8,−1 | `[11,15,7]→[15,16,9]` |
| Claw2C | 48,48 | 4,1,2 | −7,8,−1 | `[1,15,7]→[5,16,9]` |
| Claw3C | 0,48 | 2,1,4 | −1,8,−7 | `[7,15,11]→[9,16,15]` |
| Claw4C | 0,48 | 2,1,4 | −1,8,3 | `[7,15,1]→[9,16,5]` |

PORT `models/block/soul_beacon.json` 的 13 个 element 与该映射**逐值吻合**（已核对 Base/Core/Claw1A/Claw3A/Claw1B 等），
即 PORT 的 JSON 就是 TC4 `ModelSoulBeacon` 的忠实转写 → **模型无需重建**。贴图 `isorropia:block/soulbeacon`（64×64）已存在。

### 2.5 目标 1.20.1 设计

1. `common/tiles/TileSoulBeacon.java` — `BlockEntity`：
   - 客户端字段 `float beamScale; long lastGameTime;` + `float beamScale()` 按 §2.1 逻辑。
   - 持久化/同步：**无需同步**（光柱纯客户端时钟）。`activate(Player)` 写玩家持久数据（PORT 可放 capability / persistent data）+ 播放 `arcLightning`（00-api A.1 `spawnArc`）+ `SoundsTC.zap`；这在服务端或双端 `BlockSoulBeacon.use` 里做。
2. `TilesIS` 增 `SOUL_BEACON` 类型；`BlockSoulBeacon` 实现 `EntityBlock` + `use`（转发 `activate`）。
3. `client/renderer/tiles/RenderSoulBeacon.java` — 按 §2.2；注册进 `ClientSetup.onRegisterBlockEntityRenderers`。
4. 复用 TC4R：**无**专门 API；直接用原版 `BeaconRenderer.renderBeaconBeam` + `BeaconRenderer.BEAM_LOCATION`。
   若要 TC4 的「旋转内柱」观感，则只能用 1.7.10 的 Tessellator 几何（自建 `VertexConsumer`，加色 RT 见 00-api D.5），
   **不建议**（收益低、与 1.20.1 原版信标观感不一致）。

### 2.6 风险

1. `height = 256 - blockY` **不是** 1.12 的原始式（1.12 传 `(int)(256 - y)`，`y` 是相机相对 y → 随玩家高度漂移，是 1.12 的 bug）。PORT 取「光柱到世界 y=256」的意图值；差异只能实机比对。
2. `renderBeaconBeam` 内部 `translate(0.5,0,0.5)`：若外面再加 translate 会双倍偏移。
3. `beamScale` 依赖「每帧调用一次」——若 BER 被调用多次/帧会加速填满；1.12 同病。若要稳，改成由 `gameTime` 推导的纯函数（会牺牲 1.12 的跳帧衰减手惑）。
4. `getViewDistance()` 若留默认 64，256 高的光柱远端会被剔除 → 必须 256（或 `INFINITE_EXTENT_AABB` + `shouldRenderOffScreen`）。
5. `RenderType.beaconBeam` 需要玩家在光束内/外正确排序：与瓶子、染色玻璃同屏的观感实机确认。
6. `canSeeSky`：1.12 用 `world.canBlockSeeSky(pos)`；1.20.1 `Level.canSeeSky(BlockPos)` 语义一致（天空可见性），可放心对齐。

---

## 3. 共享需求 / 注册点

- **BER 注册**：`client/ClientSetup.java:71-74` `onRegisterBlockEntityRenderers`（已有 VAT 一处），新增 JAR_SOUL、SOUL_BEACON。
- **BE 类型注册**：`common/tiles/TilesIS.java`（`DeferredRegister<BlockEntityType<?>>`，已被 `Isorropia.java:29` 挂到 mod bus）。
- **方块注册**：`common/blocks/BlocksIS.java:23-34`（JAR_SOUL、SOUL_BEACON 已有，只需让方块实现 `EntityBlock`）。
- **物品**：`common/items/ItemsIS.java:52` 把 `JAR_SOUL` 从通用 `BlockItem` 换成自定义 `JarSoulItem`（实现 `initializeClient`）。
- **同步**：仅灵魂罐需要（`EntityData`）；信标零同步。模式照抄 `TileVat`（`getUpdateTag`/`getUpdatePacket`/`onDataPacket`/`handleUpdateTag`/`sync()`，`PORT common/tiles/TileVat.java:1040-1095`）。
- **物品模型 JSON**：灵魂罐 `models/item/jar_soul.json` 改为 `builtin/entity` / `thaumcraft:item/tc4_block_entity`（若采用 BEWLR）。
- **不可复用（包私有）**：TC4R `WardedJarRenderer`、`JarClientExtensions`、`NamedGroupObjMesh`、`TCClientRenderTypes`。

## 4. 只能实机确认

1. 罐内生物在罐玻璃前后的深度排序（translucent 方块 vs 实体 pass）。
2. 生物缩放/朝向：0.25 缩放 + facing 旋转是否在罐内居中（尤其 `BlockJarSoul` 放置约定了反方向）。
3. BEWLR 里 `translate(0.5,0.01,0.5)` 的净位移是否与 TC4R `WardedJarItemRenderer` 无 translate 的约定一致（引擎会先 `-0.5`）。
4. 信标光柱高度 `256 - blockY` 与 1.12 的观感差；`beamScale` 填满速度（~40 帧）。
5. 客户端实体（不 tick、无 AI）的渲染是否有姿态/贴图异常（如 `attackAnim`、`isBaby`、光照图）。

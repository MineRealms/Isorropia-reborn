# 05 — S3 已移植实体渲染器保真度审计

作用：逐实体核对 `fr/wind_blade/isorropia/client/renderer/entities/*`、`client/model/*`、`client/fx/CreatureFx.java`、
`client/ClientSetup.java` 与 `textures/entity/*`，对照 **1.12 Isorropia（实际发布版）** 与 **1.7.10 Thaumic Horizons（TC4 原型）**，
给出每个差异的 **MISSING / WRONG / OK** 判定和可直接落地的修法。常量一律照抄，不做“差不多”表述。

引用记号：`IS:` = `../Isorropia_kedition/src/main/java/fr/wind_blade/isorropia`；`TH:` = `../ThaumicHorizons/src/main/java/com/kentington/thaumichorizons`；
`TC4:` = `H:/MinecraftMods/TC4/thaumcraft`；`P:` = 本工程 `src/main/java/fr/wind_blade/isorropia`。

**上游优选原则（本文件统一适用）**：1.12 是 Isorropia 真正发布的样子，默认以它为准；但 1.12 里存在**明显退化/失效**者
（该层贴图从未被绑定、字面丢失一个可见特效），则以 1.7.10 为准并注明。下面每处都写明取哪一份及理由。

---

## 0. 结论摘要

| 实体 | 判定 | 主要问题（详见各节） |
|---|---|---|
| taint_pig | partial | 边框层 `isInvisible()` 早退（上游仍画）；边框层 RT 不写深度 vs 上游 `depthMask(true)`；节点 sprite 用 `RenderType.eyes`(1,1) vs 上游 `(770,1)` |
| gravekeeper | partial | 圣光束用 `spawnEssentiaTrail` 顶替（S3 已记）；半速率（每 2 tick）；缩放/阴影/贴图 OK |
| sheeder | partial | 多了一层 vanilla `SpiderEyesLayer`（1.7.10 用 overlay 顶替眼睛；1.12 是“眼睛+失效 overlay”）；overlay 贴图用对了（修好了 1.12 的失效） |
| chromatic_sheep | OK | 30 tick 循环 16 色 + 直接 `SheepRenderer`，与 1.12 一致 |
| scholar_chicken | OK | 与 1.12/1.7.10 一致 |
| golden_chicken | partial | 1.7.10 有金粉 sparkle、1.12 删了、端口也没有（判定见 2.5） |
| ore_pig | OK（题面前提有误） | **上游根本不存在“矿物 overlay”**；端口额外加了 `SaddleLayer`（无害，见 2.6） |
| jelly_rabbit | **WRONG（模型走样）** | `ModelJelly`/`ModelJellyRabbit` **丢了 1.12 全体的 `mirror=true`**；其余缩放/颜色/alpha/层结构 OK |
| hanging_label | **WRONG（变换+图标色）** | 变换链被简化（缺 `Rz(180)`，两个 Y 旋转被合并为 `180-yaw`，朝向一般不等价）；aspect 图标应为**单色** `0.1,0.1,0.1` α0.8，端口画成了要素彩色 |
| chocolate_cow | OK | 一致 |
| guardian_panther | partial（含 1 项既定偏差） | `ModelGuardianPanther` 上游确未使用（两树均无实例化，S3 note 正确）；阴影 0.8 与 1.12 的 0.4/1.7.10 的 1.0 都不同；随机皮肤是 S3 记下的既定偏差 |
| nether_hound | OK | 贴图/阴影/摆动与 1.12 一致（1.12 名为 hellhound.png，端口沿用） |
| ember | partial | 帧/网格/尺寸 OK；混合用 `eyes`(1,1) vs 上游 `(770,1)`；`getBbHeight()` 依赖实体碰撞箱 |
| incubated_egg | OK | `ThrownItemRenderer` ≈ 1.12 `RenderSnowball`；尺度可能略不同 |

共享件：`Billboards` 与 `ClientSetup` 见 §1、§3。

---

## 1. 共享件

### 1.1 `P:client/renderer/entities/Billboards.java`（相机朝向 sprite，taint_pig 节点、ember 火花共用）

**上游**
- 1.12 `IS:client/renderer/entities/RenderTaintPig.java:35-52` 与 `RenderEmber.java:26-44`：手写 `glPushMatrix` + `glAlphaFunc(516,0.003921569F)`
  + `glEnable(3042)` + `glBlendFunc(770,1)` + `glDepthMask(false)` + `glDisable(2884)`，再 `UtilsFX.renderFacingQuad(...)`。
- 1.7.10 `TH:.../RenderTaintfeeder.java:39-65` 同款 GL 状态，但用 `UtilsFX.renderFacingStrip(..., frames, strip, frame, ...)`。
- TC4 `TC4:client/lib/UtilsFX.java:119-159`（renderFacingQuad）：`f2=cframe/frames, f3=(cframe+1)/frames, f4=0, f5=1`，
  即帧号沿 **U** 轴、V 取满行；`TC4:...UtilsFX.java:161-201`（renderFacingStrip）：`f2=frame/frames, f3=(frame+1)/frames, f4=strip/frames, f5=(strip+1)/frames`
  （帧号 U、strip V）。两者四边形顶点用 `ActiveRenderInfo` 的相机基向量生成（billboard），顶点亮度 `func_78380_c(220)`。

**端口 `P:.../Billboards.java:20-52`**
- `translate(0, height, 0)` → `mulPose(dispatcher.cameraOrientation())` → `mulPose(Axis.YP.rotationDegrees(180))`；
- UV：`u0=(frame%grid)/grid, v0=(frame/grid)/grid, u1=u0+1/grid, v1=v0+1/grid`（`:29-32`）；
- RT `RenderType.eyes(sheet)`（`:36`）；顶点 `uv2(LightTexture.FULL_BRIGHT).color(r,g,b,255)`；画正反两遍（`:37-45`，因为 `eyes` 有背面剔除）。
- **数值核对 OK**：grid+frame 的取格算法与上游完全一致（见 §2.1 与 §2.12 的帧号验证）。

**判定：WRONG（混合状态）**
- 上游 `glBlendFunc(770,1)` = `SRC_ALPHA,ONE` = **`LIGHTNING_TRANSPARENCY`**；端口 `RenderType.eyes` = `ADDITIVE_TRANSPARENCY` = `blendFunc(ONE,ONE)`（javap 核实
  `RenderType`：`RENDERTYPE_EYES_SHADER` + `ADDITIVE_TRANSPARENCY`，且 `createCompositeState(false)` → **保留默认 CULL**）。对 α<1 的火花/节点边缘，`(1,1)` 与 `(770,1)` 结果不同。
- 亮度：上游 `func_78380_c(220)`（旧光照单位），端口 `FULL_BRIGHT`(240)。轻微偏亮。
- 修正：自建 RT（`NativeRenderTypes.create(...)` + `NativeRenderStates.LIGHTNING_TRANSPARENCY` + `NO_CULL` + `RENDERTYPE_EYES_SHADER` + `COLOR_WRITE`，
  见 `00-api-reference.md` D.5 配方），顶点亮度可保留 FULL_BRIGHT（可接受）。若维持 `eyes()`，则必须保留现在“正反两遍”的写法。
- 风险：`eyes` shader 有 `linear_fog_fade`（远处整体变淡，`00-api-reference.md` E#4），TC4 的 `(770,1)` 全亮不裁剪；只能实机比对。

### 1.2 `P:client/fx/CreatureFx.java`

- `gravekeeperBeam`：见 §2.2。
- 该文件是纯客户端（`ClientLevel` 判定）——与 `00-api-reference.md` 的“需 ClientLevel”约束一致。

### 1.3 `P:client/model/JellyColors.java`

- `ALPHA = 0.7F`（`:12`），与 1.12 `RenderJellyRabbit.java:40,83` 的 `0.7F` 一致 ✓。
- 回退到 `IsorropiaAspects.SENSUS`（`:23`）= 1.12 `Aspect.SENSES`（TC6 名）✓。
- 颜色取 `AspectApi.get(...).color()` = TC4 `aspect.getColor()` ✓。

---

## 2. 逐实体

### 2.1 taint_pig（污化猪）

**上游**
- 1.12 `IS:.../RenderTaintPig.java`：`extends RenderLiving<EntityTaintPig>`，`new ModelPig()`（无膨胀），**阴影 0.5F**（`:23`）；
  贴图 `isorropia:textures/entity/taintfeeder.png`（`:18`）。`doRender`（`:33-52`）在 `super.doRender` 之后画节点 sprite：
  `i = ticksExisted % 32`；`UtilsFX.renderFacingQuad(posX, posY + height/1.75, posZ, 32, 32, 192 + i, 2.0F, 11197951, 1.0F, 1, partialTicks)`
  → texWidth=texHeight=**32**，frame=**192+i**，scale=**2.0F**，color=**11197951 = 0xAADDFF**，alpha 1.0，blendmode **1**（加色）。
- 1.7.10 `TH:.../RenderTaintfeeder.java`：`extends RenderPig(model1, model2=ModelPig(0.5F), 0.5f)`（`ClientProxy.java:233`）；
  `renderFacingStrip(x, y+height/1.75, z, 0.0f, 2.0f, 1.0f, 32, 6, i, partialTicks, 11197951)` → frames=**32**、strip=**6**、frame=**i**、scale 2.0、color 0xAADDFF。
  边框走 `shouldRenderPass(pass 0)`（`:87-103`）：`f = sin(toRadians(ticksExisted % 45) * 8.0)`，`glBlendFunc(770,771)`，`glDisable(GL_ALPHA_TEST)`，
  `glDepthMask(!isInvisible())`，`glColor4f(1,1,1, 0.32f + 0.32f*f)`。
- **两者帧号一致**：`renderFacingStrip(frames=32, strip=6, frame=i)` → U=[i/32,(i+1)/32]，V=[6/32,7/32]；
  1.12 的 `renderFacingQuad(texW=32,texH=32,frame=192+i)` → U=[i/32,(i+1)/32]，V=[6/32,7/32]。**同一个 (col=i, row=6)**。⇒ 用 32×32 网格。
- **优选 1.12**：发布版；且 taintfeeder_eyes 边框在 1.12 从 `shouldRenderPass` 升级为独立 `LayerRenderer`（`LayerTaintPigBorder`），结构更贴近 1.20.1 的 RenderLayer。

**端口 `P:.../RenderTaintPig.java`**
- `:24` 阴影 0.5F、`PigModel`（`ModelLayers.PIG`）✓；`:25` 边框层用 `ModelLayers.PIG_SADDLE` 模型（= `PigModel.createBodyLayer(new CubeDeformation(0.5F))`，等价上游 `ModelPig(0.5F)`）✓。
- `:36` sprite：`Billboards.drawFrame(..., NODES=isorropia:textures/misc/nodes.png, bbHeight/1.75, 2.0F, 32, 192 + tickCount%32, 0xAADDFF)` ✓ 网格/帧号/尺寸/颜色/高度全对。
- `:54` `pulse = sin(toRadians(tickCount % 45) * 8.0)` ✓；`:59` alpha `0.32 + 0.32*pulse` ✓；`:58` `entityTranslucent(EYES)` → blend 770/771 ✓、`EYES=taintfeeder_eyes.png` ✓。

**判定**
1. **WRONG（轻微）** 边框层 `:51-53` `if (entity.isInvisible()) return;` —— 上游**隐形时仍然画**（只是 `depthMask(false)`）。修正：改成上游语义
   （隐形时不早退，改为不写深度；端口用 `entityTranslucent` 本就 `COLOR_WRITE` 不写深度，所以隐形/可见差异其实只是“画/不画”）。
2. **WRONG（轻微）** 上游可见时 `depthMask(true)`（边框会写深度），端口 `entityTranslucent` 不写深度 → 边框可能透过别的实体显示。若在意，用自建 RT 加 `COLOR_DEPTH_WRITE`。
3. **WRONG（轻微）** sprite 混合：(1,1) vs (770,1)，见 §1.1。
4. **OK**：`isorropia:textures/misc/nodes.png` 端口实际为 **2048×2048**（= TC4 原图，非 TC4R 重打包的 1088²），所以 32×32 取格算法成立。
   ⚠ 若将来替换成 TC4R 的 `thaumcraft:textures/misc/nodes.png`（1088²，需 `effectAtlasUv`），本算法立刻失效。

**色值**：`0xAADDFF` = 11197951 ✓（两端一致）。

---

### 2.2 gravekeeper（守墓猫）

**上游**
- 1.12 `IS:.../RenderGraveKeeper.java`：`extends RenderLiving<EntityGravekeeper>`，`new ModelOcelot()`，**阴影 0.4F**（`:18`）；
  贴图 `gravekeeper.png`（`:15`）；`preRenderCallback`：`isTamed()` 时 `scale(0.8,0.8,0.8)`（`:25-31`）。
- 1.7.10 `TH:.../RenderGravekeeper.java`：`extends RenderOcelot`，`new RenderGravekeeper(new ModelOcelot(), 0.5f)`（`ClientProxy.java:230`，**阴影 0.5**），贴图 `gravekeeper.png`，无额外缩放。
- 圣光束**不在渲染器里**：1.12 `IS:.../common/entities/EntityGravekeeper.java:45-58` 在 `onUpdate()`（每 tick，双端）里对 5 格内每个亡灵
  `FXDispatcher.INSTANCE.beamBore(posX, posY+height/2, posZ, ent.posX, ent.posY+ent.height/2, ent.posZ, 0, 16773444, false, 2.5F, 1, 1)`；
  1.7.10 `TH:.../EntityGravekeeper.java:58-87` 同参数调 `Thaumcraft.proxy.beam(world, x, y+h/2, z, tx, ty+th/2, tz, 0, 16773444, false, 2.5f, 1)`。
  ⇒ type 0（默认 beam）、色 **16773444 = 0xFFEE44**、reverse=false、width **2.5**、endmod/endScale 1。**两树一致**。
- 优选：渲染器本身取 1.12（阴影 0.4、tamed 0.8 与 1.20.1 的 Cat 更接近）；光束参数两树一致，取哪份都行。

**端口 `P:.../RenderGravekeeper.java`**
- `:17` 阴影 0.4F ✓（对齐 1.12）；`:18` `CatModel`（`ModelLayers.CAT`）；`:26-31` `isTame()` 时 `scale(0.8)` ✓。
- 贴图 `gravekeeper.png` ✓（64×32，与 1.12 字节相同）。
- 光束：`P:.../EntityGravekeeper.java:63-77` 在客户端每 **2 tick** 调 `CreatureFx.gravekeeperBeam(this, entity)`；
  `P:.../CreatureFx.java:16-23`：`from = position + (0, bbHeight/2, 0)`，`to = target.position + (0, bbHeight/2, 0)`，
  `spawnEssentiaTrail(level, from, to, source.tickCount, 0xFFEE44, 1.0F)`。

**判定**
1. **WRONG（既定偏差）** 光束用 essentia trail 顶替 bore beam：端点/颜色对，但**外观是点串不是 2.5 宽的实心光束**。
   更好的替代（`00-api-reference.md` A.4/A.10）：在 `EntityRenderer` 里每帧
   `ThaumcraftClientVisualsApi.renderBoreBeam(pose, buffers, start, end, 0xFFEE44, false, animation)`（`beam_primary/secondary`）；
   或 `ThaumcraftClientEffectsApi.spawnBeam(level, start, end, new BeamSettings(BeamTexture.DEFAULT, EffectColor.rgb(0xFFEE44), lifetime, width, 1.0F, false, true, 5, true))`
   （width 取值见 `00-api-reference.md` E#6）。**建议 S7 改为每一帧画一次**（与上游每 tick 生成一致，视觉连续）。
2. **WRONG（轻微）** 端口每 2 tick 才发一次（`P:.../EntityGravekeeper.java:66-68`），上游每 tick；若保留自走粒子方案，去掉半速率判断。
3. `Cat` 与 1.12 ocelot：`CatModel extends OcelotModel`（javap 核实），几何就是 `ModelOcelot`，**OK**；Cat 额外的“趴/放松”姿势属于 1.20.1 行为，可接受。
4. **OK**：阴影 0.4、tamed 0.8、贴图。
5. 阴影值对照：1.7.10 用 0.5，1.12 用 0.4 —— 端口取 1.12，符合“发布版优先”。

---

### 2.3 sheeder（羊蛛）

**上游**
- 1.12 `IS:.../RenderSheeder.java`：`extends RenderSpider<EntitySheeder>`，贴图 `sheeder.png`（`:14`）；构造只 `addLayer(new LayerSheederWool(this))`（`:18`）。
  `RenderSpider` 自带 vanilla `LayerSpiderEyes` ⇒ **1.12 的羊蛛有红色蜘蛛眼**。
  `LayerSheederWool`（`IS:.../layers/LayerSheederWool.java:20-28`）：`if (!sheeder.getSheared()) { depthMask(!invisible); translate(0,-0.01,-0.01); scale(1.01,1.01,1.02); getMainModel().render(...) }`
  —— ⚠ **该层从不 `bindTexture`**，且 `shouldCombineTextures()=true`，所以它其实用主贴图 `sheeder.png` 重画一遍，**`sheederoverlay.png` 从未被使用**（1.12 的失效）。
- 1.7.10 `TH:.../RenderSheeder.java`：`extends RenderSpider`，`shouldRenderPass(pass 0)`：`if (pass != 0) return -1; if (!getSheared()) { bindTexture(spiderEyesTextures=sheederoverlay.png); return 1; }`
  ⇒ **用 `sheederoverlay.png` 顶替 vanilla 蜘蛛眼 pass**（整只蜘蛛模型叠一层 overlay），**没有红眼**。条件：未剪毛。
- **优选 1.7.10**：1.12 的 overlay 失效（贴图从未绑定），1.7.10 才是“羊毛 overlay”真正生效的版本。
- 共同：贴图 `sheeder.png`（64×32）、`sheederoverlay.png`（64×32）；蜘蛛阴影 0.8（vanilla `RenderSpider` 默认）。

**端口 `P:.../RenderSheeder.java`**
- `:24` 阴影 0.8F ✓；`:25` `addLayer(new SpiderEyesLayer<>(this))` ← **vanilla 红眼**；`:26` `addLayer(new Wool(this))`；
- `:30-32` `getFlipDegrees()=180`（= vanilla 默认，无害）；`:47` `isSheared()||isInvisible()` 早退（合理）；`:51-52` `translate(0,-0.01,-0.01); scale(1.01,1.01,1.02)`（照抄 1.12）；`:53` **`entityCutoutNoCull(WOOL=sheederoverlay.png)`** ← 用对了 overlay 贴图。

**判定：partial**
1. **WRONG（对 1.7.10）** 多出 `SpiderEyesLayer`（`:25`）：1.7.10 用 overlay 顶替眼睛，端口却“眼睛 + overlay”并存 ⇒ 会看到红色蜘蛛眼，1.7.10 没有。
   修法二选一：(a) 忠实 1.7.10 → **删掉 `:25`**，保留 `Wool` 层（其效果≈1.7.10 的整模型 overlay）；(b) 忠实 1.12 → 保留眼睛，接受 overlay。
   鉴于 1.12 的 overlay 是失效的，**建议 (a)**（并保留端口已修好的 overlay 贴图绑定）。
2. **WRONG（轻微）** 缩放/平移 `(1.01,1.01,1.02)`、`translate(0,-0.01,-0.01)` 只存在于 1.12 的失效层里；若按 1.7.10 复刻 overlay，应**去掉这组偏移/缩放**（1.7.10 的 overlay 就是原尺寸整模型叠加）。
3. RT：1.7.10 是在实体渲染 pass 内、由主渲染器上下文绑定的（等同 cutout）；端口 `entityCutoutNoCull` 可接受（保留双面）。
4. **OK**：贴图两份都存在且与 1.12 字节相同。

---

### 2.4 chromatic_sheep（变色羊）

**上游**
- 1.12 `IS:.../common/entities/EntityChromaticSheep.java:13-23`：每 30 tick `setFleeceColor(EnumDyeColor.byMetadata((meta+1)%16))`；`IS:.../client/Client.java:42-64` **未注册专用渲染器** ⇒ 用 vanilla `RenderSheep` + fleece 颜色。
- 1.7.10 无此实体（Isorropia 新增）。
- 优选 1.12。

**端口**：`P:.../common/entities/EntityChromaticSheep.java:14-20` 每 30 tick `setColor(DyeColor.byId((id+1)%16))`（服务端）；
`P:.../ClientSetup.java:57` `registerEntityRenderer(CHROMATIC_SHEEP, SheepRenderer::new)`。
`SheepRenderer` 构造里 `addLayer(new SheepFurLayer(...))`，`SheepFurLayer` 用 `Sheep.getColor()` + `Sheep.getColorArray(...)` 给羊毛上色（javap 核实）⇒ 变色可见。

**判定：OK**。（唯一细节：`getColor()` 走 synched data，服务端变色会自动同步——与 1.12 一致。）

---

### 2.5 scholar_chicken / golden_chicken

**上游**
- 1.12：`IS:.../RenderScholarChicken.java:13-32`、`RenderGoldChicken.java:16-34`：均 `RenderLiving` + `new ModelChicken()`，**阴影 0.5F**；
  `handleRotationFloat` = `(sin(lerp(oFlap,wingRotation,pt)) + 1) * lerp(oFlapSpeed,destPos,pt)`（扇翅 bob）。贴图 `scholar_chicken.png` / `golden_chicken.png`。
  **1.12 的 golden chicken 无 sparkle**。
- 1.7.10：`TH:.../RenderScholarChicken.java`（`scholarchicken.png`）、`RenderGoldChicken.java`（`goldchicken.png`）。
  1.7.10 的 `RenderGoldChicken.doRender`（`:32-45`）**额外**在 5% 概率/帧发金色 sparkle：
  `Thaumcraft.proxy.sparkle(posX + width*cos(angle), posY + height*(rand-0.1)*1.2, posZ + width*sin(angle), 2.0f, 1, 0.0f)`，angle=rand*2π。
- 优选：渲染器结构取 1.12；**但 goldchicken 的金粉特效只在 1.7.10 有 → 1.12 是退化**。按“退化则取 1.7.10”原则，金粉应补。

**端口** `P:.../RenderChickenVariant.java`
- `:16` 阴影 0.5F ✓；`:26-30` `getBob` 复刻 1.12 的 `handleRotationFloat` ✓（`lerp(oFlap,flap)`/`lerp(oFlapSpeed,flapSpeed)`）。
- 贴图：`ClientSetup.java:58-65` 分别用 `scholar_chicken.png` / `golden_chicken.png` ✓。

**判定**
- scholar_chicken：**OK**。
- golden_chicken：**partial / MISSING** 金粉 sparkle。修法（S7）：在 `EntityGoldenChicken`（或其渲染器）每 tick 按 1.7.10 公式发
  `ThaumcraftClientEffectsApi.spawnQualitySparkle(level, Vec3, 2.0F, 1, 0.0F)`（`00-api-reference.md` A.10：`sparkle(x,y,z,size,color,grav)` → `spawnQualitySparkle(...,2.0F,color,0F)`）。
  注：1.7.10 的概率是“每次 doRender”，与帧率相关；移植时改用“每 tick 5%”更稳定（建议 value=0.05）。
- 该判定属于“1.12 退化 → 补 1.7.10”，需与美术确认是否保留金粉（若严格照 1.12 发布版则可判定为 OK 但记录差异）。

---

### 2.6 ore_pig（矿猪）

**题面前提有误：上游两个版本都**没有**“矿物 overlay”。**
- 1.12 `IS:.../RenderOrePig.java:11-22`：`extends RenderPig`，只覆写 `getEntityTexture → oreboar.png`，无任何层/overlay。
- 1.7.10 `TH:.../RenderOreBoar.java`：`extends RenderPig`，`new RenderOreBoar(new ModelPig(), new ModelPig(0.5f), 0.7f)`（`ClientProxy.java:227`），只有贴图 `oreboar.png`。
  ⚠ 1.7.10 传了第二个模型 `ModelPig(0.5f)`（saddle pass 用的膨胀模型），但 `RenderPig` 只在猪被鞍时用 pass；矿猪贴图本身含“矿物图案”。
- 优选：1.12（发布版，结构更简）。

**端口** `P:.../RenderOrePig.java:12-25`：`PigModel`（`ModelLayers.PIG`），阴影 **0.7F** ✓，贴图 `oreboar.png` ✓；
`:18` **额外加了 `SaddleLayer(this, PIG_SADDLE 模型, vanilla pig_saddle.png)`**。

**判定：OK（+1 处无碍的额外层）**
1. 端口额外 `SaddleLayer`（`:18`）：1.7.10 的 `RenderPig`（`model2=ModelPig(0.5f)`）在鞍时也会画鞍 pass，**行为等价**；1.12 `RenderPig` 同样有鞍 pass。⇒ 不算偏差，但**若矿猪在 1.20.1 可被上鞍**，会显示原版鞍贴图（上游也是这个行为）。可保留。
2. **OK 的核对**：贴图 `oreboar.png`（64×32）与 1.12 字节相同；阴影 0.7 与 1.7.10 显式值一致（1.12 `RenderPig` 默认亦为 0.7）。
3. 结论：**“矿猪的矿物 overlay”不存在**，不需要实现；题面若指“矿石图案”= 已烘焙在 `oreboar.png` 里。

---

### 2.7 jelly_rabbit（果冻兔）——**主要缺陷**

**上游**
- 1.12 `IS:.../RenderJellyRabbit.java`：
  - `extends RenderLiving<EntityJellyRabbit>`，主模型 `new ModelJellyRabbit()`（自定义），**阴影 0.3F**（`:25`）；主贴图 `minecraft:textures/entity/rabbit/white.png`（`:21`）。
  - **核心**：`renderModel`（`:30-50`）把整只核心模型染色 + 缩放：`color = aspect.getColor()`（空则 `Aspect.SENSES.getColor()`）；`glColor4f(r/255,g/255,b/255, 0.7F)`；
    `size = 0.55F`；`offset = rabbit.height*(size-1)/2 + 0.08F`；`translate(0, 1.5+offset, 0); scale(size); translate(0,-1.5,0)`；再 `super.renderModel(...)`；最后 `color(1,1,1)`。
  - **外壳**：内部类 `LayerRabbitJelly`（`:57-108`）：`if(!invisible)`：同样 aspect 色 + α0.7；`jellySize = rabbit.getJellySize()/25 + 1`；`offset = rabbit.height*(jellySize-1)/2`；
    `translate(0,1.5+offset,0); scale(jellySize); translate(0,-1.5,0); scale(0.6,0.6,0.6)`；`blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)`；`bindTexture(jelly_rabbit.png)`；用 `new ModelJelly()` 渲染。
  - 模型：
    - `ModelJellyRabbit`（核心）：`rabbitBody(0,0) box(-3,-2,-10,6,5,10) @(0,19,8) rotX -0.34906584`；`rabbitHead(32,0) box(-2.5,-4,-5,5,4,5) @(0,16,-1)`；耳（未渲染）；`slimeRightEye/slimeLeftEye(58,26) box(±1,1,1) @(0,16,-1)`。
      `render()` **只画 body/head/左右眼**（耳朵建了但不画，`:62-68`）。**所有 body/head/耳 均 `mirror=true`，眼睛不 mirror**。
    - `ModelJelly`（外壳）：= 完整兔子模型，**所有 pivot 比核心高 16px**（body @(0,35,8)、head @(0,32,-1)、tail @(0,36,7)…），
      `render()` 依次画 leftFoot,rightFoot,leftThigh,rightThigh,body,leftArm,rightArm,head,rightEar,leftEar,tail,nose。**12 个 part 全部 `mirror=true`**。
- 1.7.10：无果冻兔（Isorropia 新增）。⇒ 只能以 1.12 为准。
- 优选：1.12（唯一来源）。

**端口**
- `P:.../RenderJellyRabbit.java`：`:24` 阴影 0.3F ✓；`:20` 主贴图 `minecraft:rabbit/white.png` ✓；`:25` `new ModelJelly(...)` 外壳层 ✓；
- `:47-49` `color=JellyColors.of(rabbit)`、`size=getJellySize()/25+1`、`offset=getBbHeight()*(size-1)/2`；`:51-54` `translate(0,1.5+offset,0); scale(size); translate(0,-1.5,0); scale(0.6)` ✓；
  `:57-59` `entityTranslucent(JELLY)` + 颜色/α0.7 ✓。
- `P:.../model/ModelJellyRabbit.java`：`SIZE=0.55`；`renderToBuffer:75-78` `offset = entityHeight*(SIZE-1)/2 + 0.08`；`translate/scale` ✓；
  几何 UV/box 与 1.12 逐项一致（body/head/眼）；**只建 body/head/左右眼并只画这 4 个** ✓（对齐 1.12 不画耳朵）。
- `P:.../model/ModelJelly.java`：几何 UV/box/pivot/初始 rotX 与 1.12 `ModelJelly` **逐项一致**；`parts` 渲染顺序与 1.12 一致 ✓。

**判定**
1. **WRONG（模型走样，必须修）**：两个模型**全部漏了 `.mirror()`**。
   - 1.12 `ModelJelly`：12 个 part 全 `mirror = true`（`IS:.../model/ModelJelly.java:38,43,48,53,58,63,68,73,78,83,88,93`）。
   - 1.12 `ModelJellyRabbit`：`rabbitBody.mirror=true`（`:27`）、`rabbitHead.mirror=true`（`:32`）、耳 `:37,:42` 也 true；**只有左右眼不 mirror**（`:44-51`）。
   - 端口 `ModelJelly.java:59-82`、`ModelJellyRabbit.java:46-53` 的 `CubeListBuilder` **全部没有 `.mirror()`** ⇒ 每个 box 的贴图 UV 走向与 1.12 相反（对 `jelly_rabbit.png` 这类非对称贴图肉眼可见；对白色兔子贴图影响小）。
   - **修法**：给 `ModelJelly` 全部 12 个 `CubeListBuilder` 加 `.mirror()`；给 `ModelJellyRabbit` 的 `body`、`head` 加 `.mirror()`（两眼不加）。
2. **WRONG（轻微，数值）**：`height` 来源。1.12 用 `rabbit.height`；端口用 `rabbit.getBbHeight()`。1.12 `EntityRabbit.setSize(0.4,0.7)`，1.20.1 `Rabbit.setSize(0.4,0.5)`（待实机确认）⇒ 核心/外壳的竖直 offset 会差约 `(0.7-0.5)*0.225 ≈ 0.045`。修法：在 `EntityJellyRabbit` 覆写 `setSize(0.4F, 0.7F)` 以对齐 1.12，或把 `0.08` 常量按 1.20.1 高度重标定。**只能实机确认到底哪个高度**。
3. **WRONG（轻微）**：RT 深度写。1.12 外壳用 `blendFunc` 但未动 depth mask（默认 true ⇒ **写深度**）；端口 `entityTranslucent` 不写深度。视觉差异小，记录即可。
4. **WRONG（轻微）**：端口外壳传了 `getOverlayCoords(rabbit, 0.0F)`（受击闪红会作用于外壳），1.12 层未处理 overlay。
5. **OK**：α=0.7、aspect 色、`jelly_rabbit.png`（64×32，字节同 1.12）、阴影 0.3、`getJellySize()/25+1` 尺寸公式、`0.6` 外壳缩比、`0.55` 核心缩比、核心 `+0.08`、`1.5` 旋转中心。

---

### 2.8 hanging_label（悬挂标签）——**变换与图标色双缺陷**

**上游**
- 1.12 `IS:.../RenderHangingLabel.java`：
  - `LABEL_TEXTURE = thaumcraft:textures/models/label.png`（`:16`）。
  - `doRender`（`:28-60`）变换顺序（**全部累乘**）：
    1. `color(1,1,1,1)`；`pushMatrix`；`disableLighting`；`translate(x,y,z)`；
    2. `rotate(180, 0,0,1)` → `Rz(180)`；
    3. `rotate(yawInterp, 0,1,0)` → `Ry(yaw)`；
    4. `rotate(pitchInterp, 0,0,1)` → `Rz(pitch)`（`EntityHanging` 的 pitch=0）；
    5. `rotate(180, 0,1,0)` → `Ry(180)`；
    6. `translate(0,0,0.03)`；`enableRescaleNormal`；
    7. `renderEntity`：`push` → `renderQuadCentered(LABEL, 0.5F, 1,1,1, -99, 771, 1.0)`（**前面**）→ `rotate(180,0,1,0)`（`Ry180`）→ `renderQuadCentered(...)`（**背面**）→ `pop`；
       若 `aspect != null`：`push; translate(0,0,-0.001); scale(0.021); drawTag(-8,-8,aspect); pop`；
    8. `translate(-7,-8,-0.01)`；`popMatrix`。
  - `TC4:.../UtilsFX.java:325-342`（`renderQuadCenteredFromTexture`）：`glScalef(scale)` 后画 `±0.5`（scale=0.5 ⇒ **半宽 0.25**）四边形，UV 0..1，法线 -Z，blend=`(770, blend)`=**(770,771)**。
  - `TC4:.../UtilsFX.java:522-524` → `drawTag(x,y,aspect)` = `drawTag(..., 771, 1.0f, bw=true)`；`TC4:.../UtilsFX.java:530-561`：`bw=true` 时颜色 **`glColor4f(0.1,0.1,0.1, alpha*0.8)`**（即 **0x1A1A1A，α=204/255**），UV 0..1 覆盖整张 aspect 图标（32×32），quad 尺寸 16 texel。
- 1.7.10：无悬挂标签实体（Isorropia 新增）。⇒ 只能以 1.12 为准。
- **优选 1.12**。注：TC4R 的源质罐标签也用同一色（`00-api-reference.md` B.2：0x191919/α204），互为印证。

**端口 `P:.../RenderHangingLabel.java`**
- `:21` `LABEL = thaumcraft:textures/models/label.png` ✓；`:23` `LABEL_HALF=0.25` ✓（对应 scale 0.5）；`:23`? `ICON_HALF = 8*0.021 = 0.168` ✓（对应 drawTag 的 16 texel × 0.021 的一半）。
- `:32` **只有** `mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()))` 一条旋转（缺 `Rz(180)`、缺 `Rz(pitch)`、把 `Ry(yaw)` 与 `Ry(180)` 合并成 `180-yaw`）。
- `:33` 标签 quad：`RenderType.entityCutoutNoCull(LABEL)`，`z=-0.03`，`half=0.25`，`rgb=0xFFFFFF`，α=1（`:53-56` 顶点 UV：u 向左增、v 上 0 下 1）。
- `:34-39` 图标：`RenderType.entityTranslucent(definition.texture())`，`z=-0.031`，`half=ICON_HALF`，**`rgb=definition.color()`**（要素本色），α=1，`light`。

**判定**
1. **WRONG（变换链不等价）**：
   - 上游净变换（顶点 p）：`M_u = Rz(180)·Ry(yaw)·Ry(180)`；端口 `M_p = Ry(180-yaw)`。
   - 二者**不相等**：例如 yaw=0（南面）`M_u=diag(1,-1,-1)`（面向 +Z、上下翻转），`M_p=diag(-1,1,-1)`（面向 +Z、左右翻转）——相差一个 `Rz(180)`（图纸面内滚 180°）。yaw=90/270 时 Y 分量也差 180°。
   - 后果：标签与其 aspect 图标**朝向/上下翻转**，至少部分朝向（东/西/南/北）与 1.12 不一致。**必须实机逐朝向比对**。
   - **修法**：直接照抄上游链（1.20.1 对应）：
     ```java
     poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));                     // rotate(180,0,0,1)
     poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));           // rotate(yaw,0,1,0)  [getXRot()==0, 可省 Rz(pitch)]
     poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));                     // rotate(180,0,1,0)
     poseStack.translate(0.0D, 0.0D, 0.03D);                                 // 注意是 +0.03，不是 -0.03
     ```
     然后标签 quad 画在 z=0（正反两面），图标画在 z=-0.001（用 `scale` 而非直接 half）。
   - 尚存不确定：1.12 `EntityHanging.updateFacingWithBoundingBox` 是否把 `rotationYaw` 设为 `horizontalIndex*90`（与 1.20.1 `get2DDataValue()*90` 同约定）。**这一条只能实机对照截图确认**。
2. **WRONG（图标色）**：上游 `drawTag(...,bw=true)` = **单色 0.1/0.1/0.1，α0.8 → 0xFF1A1A1A**；端口用 `definition.color()`（要素彩色）+ α1.0。
   **修法**：把 `:37-38` 的 `definition.color(), 1.0F` 改成 `0x1A1A1A, 0.8F`（与 TC4R 源质罐标签一致）。
3. **WRONG（轻微）** 标签混合：上游 `(770,771)` translucent；端口 `entityCutoutNoCull`（cutout，α<0.1 丢弃、无混合）。若 `label.png` 有半透明像素会不一致。修法：改 `RenderType.entityTranslucent(LABEL)`（或保持 cutout 并实机确认 label.png 无软边）。
4. **OK**：贴图 RL、半宽 0.25、图标半宽 0.168、双面（端口用 no-cull 单 quad 顶替上游“正反两 quad”，视觉等价但背面 UV 会镜像）。
5. **OK**：端口把标签画在 `z=-0.03`、图标 `z=-0.031`（相对 z 轴负向），上游是 `+0.03`；两者都在“墙外/墙内”上可能反号——与 (1) 一起实机校正。

---

### 2.9 chocolate_cow（巧克力牛）

**上游**
- 1.12 `IS:.../RenderChocolateCow.java:13-24`：`RenderLiving` + `new ModelCow()`，**阴影 0.7F**，贴图 `isorropia:chocow.png`。
- 1.7.10 `TH:.../RenderChocolateCow.java`：`RenderCow` + `new RenderChocolateCow(new ModelCow(), 0.7f)`（`ClientProxy.java:226`），贴图 `thaumichorizons:chocolatecow.png`。
- 优选 1.12（Isorropia 发布版；贴图名 `chocow.png`）。

**端口** `P:.../RenderChocolateCow.java:14-21`：阴影 0.7F ✓、`CowModel`（`ModelLayers.COW`）✓、贴图 `chocow.png` ✓（64×32，与 1.12 同字节）。

**判定：OK。**

---

### 2.10 guardian_panther（守护豹）

**关于 `ModelGuardianPanther`（题面点名）——S3 note 正确**：
`grep "ModelGuardianPanther"` 只命中两处**类声明本身**：`IS:.../client/model/ModelGuardianPanther.java:18,30` 与 `TH:.../client/renderer/model/ModelGuardianPanther.java:20,32`，**两树都没有任何 `new ModelGuardianPanther()`**。
1.12 `RenderGuardianPanther extends RenderOcelot`（用 `ModelOcelot`），1.7.10 由 `ClientProxy.java:228` 传入 `new ModelOcelot()`。⇒ **该模型在两端都是死代码，端口不移植它是对的（OK）**。

**上游**
- 1.12 `IS:.../RenderGuardianPanther.java:10-33`：`extends RenderOcelot`，`getEntityTexture` 按 `getTameSkin()` switch：
  `0→guardian1, 1→guardian1, 2→guardian2, 3→guardian3`（`:17-27`，**0 与 1 都映射 guardian1**）；
  `preRenderCallback`：`super` 后 `glScalef(2.0f, 2.0f, 2.0f)`（`:30-33`）。
  `IS:.../common/entities/EntityGuardianPanther.java:81-90`：`entityInit` 把自有 data 置 `|0x4`、`isTamed()=true`；但**从不设置 `getTameSkin`** ⇒ 实际**恒为 skin 0 → guardian1**。
- 1.7.10 `TH:.../RenderGuardianPanther.java:13-23`：`extends RenderOcelot`，`ClientProxy.java:228` 传 `new ModelOcelot(), 1.0f`（**阴影 1.0**），`preRenderCallback` `glScalef(2.0f)`；**不覆写贴图**（用 ocelot 默认贴图）。
- 阴影对照：1.12 = `RenderOcelot` 默认（0.4）；1.7.10 = 1.0。缩放两树都是 2.0（叠在 `RenderOcelot` 的 tamed 0.8 之上 ⇒ 有效 1.6；旁证：1.12 `RenderGraveKeeper` 不继承 `RenderOcelot` 却手写 `scale(0.8)` 补 tamed 缩放）。
- 优选：贴图映射取 1.12（guardian1–3 是 Isorropia 的资产）；缩放取两树一致的 2.0×0.8=1.6。

**端口 `P:.../RenderGuardianPanther.java`**
- `:13` `MobRenderer<EntityGuardianPanther, CatModel<...>>`（`ModelLayers.CAT`）；`:20` **阴影 0.8F**；`:29-31` `super.scale` 后 `scale(1.6)`；
- `:24-25` `SKINS[Math.floorMod(getSkin(), 3)]` → skin 0→guardian1, 1→guardian2, 2→guardian3。
- `P:.../common/entities/EntityGuardianPanther.java:78` `finalizeSpawn` 随机 `skin∈[0,2]`；`isTame()` 恒 true（`:63-65`）。

**判定：partial**
1. **偏差（S3 已记，OK-per-deviation）**：端口随机 0–2 → guardian1/2/3；1.12 实际恒 guardian1。若追求“发布版外观”，可让 `finalizeSpawn` 恒 `setSkin(0)`。
2. **WRONG（阴影）**：端口 0.8F；1.12 = 0.4（`RenderOcelot` 默认），1.7.10 = 1.0。**修法**：`super(context, model, 0.8F)` → 若照 1.12 应为 `0.4F`；若想按 2× 缩放放大阴影则 0.8 可自洽，但需注明偏差。**建议 0.4 并对齐 1.12**（或实机确认视觉）。
3. **OK**：缩放 1.6（= tamed 0.8 × 2.0）与两树一致；贴图 `guardian1-3.png`（各 64×32，与 1.12 同字节）；`CatModel extends OcelotModel` ⇒ 几何 = 1.12 `ModelOcelot` ✓。
4. **OK**：无 collar 层（`MobRenderer` 直用 `CatModel`），与 1.12 ocelot 无 collar 一致。

---

### 2.11 nether_hound（地狱犬）

**上游**
- 1.12 `IS:.../RenderHellHound.java:12-26`：`RenderLiving` + `new ModelWolf()`，**阴影 0.5F**；贴图 `isorropia:hellhound.png`；
  `handleRotationFloat → getTailRotation()`（尾巴摆动）。
- 1.7.10 `TH:.../RenderNetherHound.java:15-35`：`RenderWolf`，`ClientProxy.java:234` 传 `new ModelWolf(), new ModelWolf(), 0.5f`；贴图 `thaumichorizons:netherhound.png`。
- 优选 1.12（发布版；贴图名 hellhound.png，端口沿用）。

**端口** `P:.../RenderHellHound.java:11-27`：`MobRenderer` + `WolfModel`（`ModelLayers.WOLF`）✓、阴影 0.5F ✓、贴图 `hellhound.png` ✓；
`:19-21` `getBob → entity.getTailAngle()`（1.20.1 `Wolf.getTailAngle()`，等价 1.12 `getTailRotation()`）✓。

**判定：OK。**（细节：端口用 `MobRenderer` 而非 `WolfRenderer`，**无 collar 层**，与 1.12 `RenderLiving` 一致 ✓；`getBob` 用于尾摆与 vanilla wolf 一致。）

---

### 2.12 ember（余烬弹）

**上游**
- 1.12 `IS:.../RenderEmber.java:14-54`：`extends Render<EntityEmber>`，**阴影 0.0F**（`:23`）；`getEntityTexture` 返回方块图集（`:52`，实际不用于 sprite）。
  `renderEntityAt`（`:26-44`）：与 taint_pig 同款 GL 状态；`i = ticksExisted % 7`；
  `UtilsFX.renderFacingQuad(posX, posY + height/1.75, posZ, 16, 16, 144 + i + 7, 0.08F, 11197951, 1.0F, 1, pticks)`
  → texW=texH=**16**，frame=**151+i**（=144+i+7），scale=**0.08F**，color=**0xAADDFF**，blendmode **1**。
- 1.7.10：无（Isorropia 新增）。
- 优选 1.12。

**端口** `P:.../RenderEmber.java:12-31`：`:17` `shadowRadius = 0.0F` ✓；
`:22-23` `Billboards.drawFrame(..., PARTICLES=isorropia:textures/misc/particles.png, getBbHeight()/1.75, 0.08F, 16, 151 + tickCount%7, 0xAADDFF)` ✓ 网格 16、帧 151+i、尺寸 0.08、颜色全对。

**判定**
1. **部分 WRONG（依赖实体尺寸）** `getBbHeight()/1.75`：1.12 用 `entity.height/1.75`。`EntityEmber extends ThrowableProjectile`（`P:.../projectile/EntityEmber.java:24`），其碰撞箱需核对是否等于 1.12 的 `EntityEmber.height`。若不等，sprite 会偏。**实机确认高度**。
2. **WRONG（轻微）** sprite 混合 (1,1) vs (770,1)，同 §1.1。
3. **OK**：贴图 `isorropia:textures/misc/particles.png` 端口为 **256×256**（= TC4 原图），16×16 取格成立；帧 151..157 → (col 7..13, row 9)。

---

### 2.13 incubated_egg（孵化蛋）

**上游**
- 1.12 `IS:.../client/Client.java:42`：`registerEntityRenderingHandler(EntityIncubatedEgg.class, rm -> new RenderSnowball(rm, ItemsIS.itemIncubatedEgg, Minecraft.getMinecraft().getRenderItem()))` ⇒ 用物品图标渲染抛掷物。
- 1.7.10：无（Isorropia 新增）。优选 1.12。

**端口** `P:.../ClientSetup.java:53`：`registerEntityRenderer(INCUBATED_EGG, ThrownItemRenderer::new)`；`EntityIncubatedEgg extends ThrowableItemProjectile`（`P:.../projectile/EntityIncubatedEgg.java:19`）⇒ 与 `ThrownItemRenderer` 兼容 ✓。

**判定：OK（1 处待确认）**：`ThrownItemRenderer` 渲染的物品图标比例/自旋与 1.12 `RenderSnowball` 略有差异（1.12 snowball 通常 0.5 缩放 + 自旋），核对待实机。功能等价。

---

## 3. 共享基础设施 / 注册

`P:.../ClientSetup.java`
- `:45-49` `onRegisterLayers`：注册 `ModelJellyRabbit.LAYER`、`ModelJelly.LAYER` 两个 layer definition，BER/实体渲染器构造里 `context.bakeLayer(...)` ✓（Forge 47 `RegisterLayerDefinitions`，`00-api-reference.md` D.3）。
- `:51-69` `onRegisterRenderers`（`EntityRenderersEvent.RegisterRenderers`）：14 个实体全部注册 ✓（`00-api-reference.md` B.11/D.3 的注册点）。
- `:76-80` `onItemColors`：`ItemJelly` 按 aspect 着色（`JellyColors.of`）——与 1.12 `Client.java:79-86`（`itemEssentiaColourHandler` 取 `getAspects()[0].getColor()`，空则 `Aspect.SENSES.getColor()`）语义一致 ✓。
- `:72-74` BER：`TilesIS.VAT → RenderVat`（属 01 文档范围）。

**共性建议**：`Billboards` 的 RT（§1.1）与 `hanging_label` 的填充/图标色（§2.8）是两处最值得在 S7 一次性统一修的点；两者都只涉及客户端，不需要新增网络同步（实体状态已由现有 synched data / NBT 同步：jelly 的 `ASPECT/JELLY`、panther 的 `SKIN`、sheeder 的 `SHEARED`、hanging label 的 spawn data 里的 aspect/facing）。

**服务端必须提供的数据（客户端只读）**
- `EntityJellyRabbit`：`ASPECT`(String)、`JELLY`(int) —— 已 `defineSynchedData` ✓。
- `EntityGuardianPanther`：`SKIN`(int) —— ✓。
- `EntitySheeder`：`SHEARED`(bool) —— ✓。
- `EntityHangingLabel`：spawn data（pos / facing / aspect 字符串）—— ✓（`EntityHangingLabel.java:105-118`）。
- `EntityChromaticSheep`：走 vanilla `Sheep` 的羊毛色 synched data —— ✓。
- 其余实体渲染无额外同步需求。

---

## 4. 资产清单（`assets/isorropia/textures/...`）

| 文件 | 端口状态 | 来源/尺寸 |
|---|---|---|
| `entity/taintfeeder.png` | 有，与 1.12 同字节 | 64×32 |
| `entity/taintfeeder_eyes.png` | 有，同字节 | 64×32 |
| `entity/sheeder.png` / `sheederoverlay.png` | 有，同字节 | 64×32 |
| `entity/jelly_rabbit.png` | 有，同字节 | 64×32 |
| `entity/gravekeeper.png` | 有，同字节 | 64×32 |
| `entity/guardian1/2/3.png` | 有，同字节 | 64×32 |
| `entity/oreboar.png` | 有，同字节 | 64×32 |
| `entity/scholar_chicken.png` / `golden_chicken.png` | 有，同字节 | 64×32 |
| `entity/chocow.png` | 有，同字节 | 64×32 |
| `entity/hellhound.png` | 有，同字节 | 64×32 |
| `misc/nodes.png` | 有（**2048×2048**，TC4 原图） | 供 taint_pig 节点 sprite |
| `misc/particles.png` | 有（**256×256**，TC4 原图） | 供 ember 火花 |
| `thaumcraft:textures/models/label.png` | TC4R 内已有（16×16，`00-api-reference.md` C.2） | 供 hanging_label |

不需要新增贴图；均在位。**注意**（§2.1/§2.12）：端口用的是自己命名空间的 TC4 **原始尺寸** 图（2048²/256²），所以裸 32×32 / 16×16 取格成立。**若日后改为引用 TC4R 打包图，必须换成 `particleAtlasUv/effectAtlasUv`。**

---

## 5. 风险 / 只能实机确认

1. **hanging_label 朝向**：端口单 Y 旋转 vs 上游 4 段旋转链（缺 `Rz(180)`），只在 S/N 两面“Y 分量”相等；E/W/S/N 四朝向的最终朝向与图标上下方向**必须逐朝向截图比对**。同时确认 1.12 `EntityHanging.rotationYaw` 约定（本文件按 `horizontalIndex*90` 推断）。风险最高。
2. **hanging_label 图标色**：本文件推断 1.12 的 3 参 `drawTag` 为 `bw=true`（单色）；TC6 的 `UtilsFX` 若签名/默认与 TC4 不同，需以实机/TC6 反编译复核。
3. **jelly_rabbit `.mirror()`**：加 `.mirror()` 后外观变化幅度需实机看（白色兔子底图影响小，`jelly_rabbit.png` 外壳可能明显）。
4. **jelly_rabbit 竖直 offset**：依赖 1.20.1 `Rabbit.getBbHeight()`（可能 0.5 vs 1.12 的 0.7）。
5. **Billboards 混合**：`eyes`(1,1) vs TC4 `(770,1)`；且 `eyes` shader 受雾影响（`00-api-reference.md` E#4）。实机在多场景（远处/雾中）比对。
6. **gravekeeper 光束**：`spawnEssentiaTrail` 顶替 bore beam，与上游 2.5 宽实心束差异明显；且端口每 2 tick 一次。
7. **golden_chicken 金粉**：是否补 sparkle 属于“1.12 退化 vs 1.7.10 原型”的取舍，需与美术/需求确认。
8. **guardian_panther 阴影 0.8**：与 1.12(0.4)/1.7.10(1.0) 均不同，需定值。
9. **ember/egg 尺寸**：`getBbHeight()` 与 1.12 `height` 是否一致；`ThrownItemRenderer` 与 `RenderSnowball` 的物品比例。
10. **`entityTranslucent` 不写深度**（边框层/jelly 外壳）：相对上游 `depthMask(true)` 的差异只在多层叠加时可见。

---

## 6. 建议的 S7 修复顺序（按性价比）

1. `hanging_label`：照抄 §2.8 的 4 段旋转链 + z=+0.03 + 图标改 `0x1A1A1A/0.8` + 标签改 `entityTranslucent`。
2. `jelly_rabbit`：`ModelJelly` 全 12 个 + `ModelJellyRabbit` body/head 加 `.mirror()`；核对 `getBbHeight()`。
3. `sheeder`：删 `SpiderEyesLayer`（按 1.7.10），overlay 去掉 1.01/1.02 偏移。
4. `billboards`：自建 RT（LIGHTNING + NO_CULL + eyes shader）。
5. `gravekeeper`：改 `renderBoreBeam`（每帧）或 `spawnBeam`，去半速率。
6. `taint_pig` 边框：去掉 `isInvisible()` 早退；如需写深度用自建 RT。
7. `golden_chicken` 金粉（视取舍）；`guardian_panther` 阴影定值。

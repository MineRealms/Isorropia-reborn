# 04 — 粒子与世界特效（particles and world FX）

本文覆盖 Isorropia/ThaumicHorizons 在 1.12 / 1.7.10 里通过“粒子”与“服务端触发→客户端播放”实现的所有视觉特效，
并给出 1.20.1（Forge 47 + TC4R 20732）的可落地方案。阅读前先读 `00-api-reference.md`（下称“共享参考”）。

约定：`D:` = 20732 反编译行号（见共享参考），`S:` = 20721 源码；引用上游一律给 `文件:行`。

---

## 0. 结论速览

| # | 特效 | 1.12 载体 | 1.7.10 载体 | 1.20.1 首选实现 | 需自定义粒子？ |
|---|---|---|---|---|---|
| 1 | 天穹吸收（vat 吸收日月精华） | `FXCelestialAbsorption`→TC6 `FXGeneric` | 无 | 自定义 `ParticleProvider` | **是** |
| 2 | 元素光团爆发 | `FXElementalWisp`（**死代码**） | 无 | 不实现 / 自定义 | 否（可略） |
| 3 | 通量爆炸 | `FXFluxExplosion`→`FXGeneric` | 无 | 自定义 `ParticleProvider` | **是** |
| 4 | 声波（sonic） | `FXSonic`（hemis.obj + rippleN） | `FXSonic`（同） | `ThaumcraftClientEffectsApi.spawnSonicWave` | 否 |
| 5 | vat 注入符文 | `FXDispatcher.blockRunes` | `Thaumcraft.proxy.blockRunes` | `ThaumcraftClientEffectsApi.spawnBlockRune` | 否 |
| 6 | vat 不稳定电弧 | `FXDispatcher.arcLightning` | `proxy.nodeBolt` | `spawnArc` / `spawnNodeLightning` | 否 |
| 7 | 注魔原料飞入（玩家/基座） | `ISPacketFXInfusionSource`+`drawInfusionParticles1..4` | `PacketInfusionFX`+`drawInfusionParticles1..4` | `spawnAttractedSparkle`/`spawnInfusionItem`/`spawnInfusionBlock` + 自建包 | 否 |
| 8 | 基座原料被消耗的碎屑 | `ParticuleDestroyMessage` | `PacketFXInfusionDone` | `spawnBlockSparkle`/原版 destroy 粒子 | 否 |
| 9 | 矩阵排气羽流 | `RenderModifiedMatrix:109` TC6 `FXGeneric` | 无 | 自定义 `ParticleProvider` | **是** |
| 10 | 矩阵光芒 halo | `RenderModifiedMatrix.drawHalo` | 无（TC4 `TileRunicMatrixRenderer`） | `ThaumcraftClientVisualsApi.renderInfusionHalo` | 否 |
| 11 | 灵魂信标电弧 | `TileSoulBeacon:33` `arcLightning` | 同 `proxy.arcLightning` | `spawnArc` + `presentAt` | 否 |
| 12 | 雷电皮肤反伤弧 | `EntityEventHandler:442` `arcLightning` | `EventHandlerEntity:395…` `arcLightning` | `spawnArc` | 否 |
| 13 | 透镜扫描高亮 | `OrdoLens:71/83/92` `FXDispatcher.scanHighlight` | `ItemLensOrderEntropy` `blockRunes` | `spawnParticle(SCAN_RUNE)`（近似） | 否 |
| 14 | 守墓者圣光 | `EntityGravekeeper:54` `beamBore` | `EntityGravekeeper:72` `proxy.beam` | `spawnBeam(...)`/`showFocusBeam` | 否 |
| 15 | 禁闭（containment）爆发 | 无（TC6 用别的） | `FXContainment`+`PacketFXContainment` | 自定义 `ParticleProvider` | **是** |
| 16 | 精华气泡 | 无 | `FXEssentiaBubble`+`PacketFXEssentiaBubble` | 自定义 `ParticleProvider` | **是** |
| 17 | 精华流光 | 无 | `FXEssentiaTrail` | `spawnEssentiaTrail` | 否 |
| 18 | 漩涡/饥饿节点 | 无 | `TileVortex/TileVortexRender`+`proxy.burst/nodeBolt` | `ThaumcraftClientVisualsApi.renderNode*` + `spawnBurst` | 部分 |
| 19 | ember 火球贴图 | `RenderEmber` + `UtilsFX.renderFacingQuad` | — | 现有 `Billboards`（已做） | 否 |
| 20 | 服务端 `blockSparkle` 占位 | — | — | 保留 `WorldEffectsApi.blockSparkle`（已做） | 否 |

> 关键结论：**TC4R 的 `spawnGenericParticle`（后端 `ChampionGenericParticle`）只实现 TC6 `FXGeneric` 的“单色、单缩放、单帧序列”子集**，
> 不支持 `setAlphaF(float[])`、`setScale(float[])`、`setRandomMovementScale`、`setWind`、`setRotationSpeed`、逐粒子随机速度。
> 因此 #1、#3、#9 这三个 Isorropia 原版特效**必须自写粒子**才能还原。

---

## 1. 上游行为

### 1.1 1.12 Isorropia（TC6 载体）

#### 1) `FXCelestialAbsorption`（天穹吸收）
文件：`Isorropia_kedition/.../client/fx/FXCelestialAbsorption.java`。

- 外层 `Particle`（第 13-14 行）`blendmode=1`、`length=1.0F`、`particle=16`；构造 `:20-29`：
  `setSize(0.02F,0.02F)`、`motionXYZ=0`、`particleMaxAge=3`；`renderParticle` 是空覆写（`:87`）——本粒子只当“发号器”。
- 构造里立刻生成 **50** 个内层 `FXGeneric`（`:31-68`），每颗 `sp = rand<0.2`：
  - `absorb=true`：位置 `(x,y,z)`；`setLoop(true)`、`setGravity(sp?0.0f:0.125f)`、`setLayer(0)`、`setSlowDown(0.995)`（`:36-40`）。
  - `absorb=false`：位置 `(x+rand-0.5, y, z-rand+0.5)`；`setParticles(sp?320:512, 16, 1)`、`setGravity(-0.1f)`、`setSlowDown(0.95)`（`:42-46`）。
  - 公共：`age = 30 + rand.nextInt(20)`（`:49`）；**`setRBGColorF(red, blue, green)`**（`:51`，注意 G/B 写反，实际传入 r=0.49019608,b=0.63529414,g=0.5568628 → 参数顺序为 (r,b,g)）。
  - `alphas = new float[6 + rand.nextInt(age/3)]`；`alphas[0]=1.0F`，`alphas[1..len-2]=rand()`（`:57-61`）。
  - `setParticles(...)` 再设一次（`:62`）、`setScale(new float[]{0.5F,0.125F})`、`setRandomMovementScale(0.0025F,0.001F,0.0025F)`（`:63-64`）。
  - `ParticleEngine.addEffectWithDelay(fb, 2 + rand.nextInt(3))`（`:66`）。
- 调用点：`common/tiles/TileVat.java:323`，仅当 `infusing && celestialAuraNeeded>0 && canDrawnCelestialAura()`：
  `fxAbsorption(x+0.5, y+1.5 + (needed>have ? 3.0 : -3.5), z+0.5, 0.49019608f, 0.5568628f, 0.63529414f, needed>have)`。
  即吸收时粒子在方块上方 +4.5 格、排出时在下方 −2.0 格。

#### 2) `FXElementalWisp`（元素光团，1.12 为死代码）
文件：`client/fx/FXElementalWisp.java:17-34`。10 颗 `FXGeneric`，位置 `(x+a, y+b, z+c)`，`a,b,c ∈ ±rand[0,1]`；
初速 `(-a/20, -b/20, -c/20)`（`slow=20.0F`）；`setRBGColorF(clamp(384,0,1)=1.0, 0.0, 1.0)`（品红）；`age=30+rand(20)`；
`setParticles(sp?320:512,16,1)`；`setScale({0.5,0.125})`。
**`ISFXDispatcher.elemental(...)`（`ISFXDispatcher.java:11`）在整个 1.12 树里没有任何调用点** → 死代码，1.20.1 可不实现。

#### 3) `FXFluxExplosion`（通量爆炸）
文件：`client/fx/FXFluxExplosion.java:18-42`。**3×180=540** 颗 `FXGeneric`；
位置 = `(x,y,z)`，初速 = `(randomSign/1.5, …, …)`（`getRandomSign :52-54`，`±rand/1.5`）；
`setRBGColorF(1.0f, 0.0f, 1.0f, rand, rand, rand)`（6 参版，`:24`）；`setLoop(true)`、`setLayer(0)`、`setSlowDown(0.955)`；
`age=30+rand(20)`；alpha 数组同 #1；`setParticles(sp?320:512,16,1)`；`setScale(0.5f,0.125f)`；
`setRandomMovementScale(0.0025f,0.001f,0.0025f)`；`addEffectWithDelay(fb, 2+rand(3))`。
调用点：`TileVat.java:1158`（仅客户端分支），位置 `(x+0.5, y+1.5, z+0.5)`。

#### 4) `FXSonic`（声波，1.12 版）
文件：`client/fx/FXSonic.java:21-89`。
- 构造 `:30-50`：`particleMaxAge = age + rand.nextInt(age/2)`；`canCollide=false`→`true`；`setSize(0.01,0.01)`；`particleScale=1.0`；
  `yaw = target.getRotationYawHead()`、`pitch = target.rotationPitch`；位置被强行设为 `target.pos` + `eyeHeight`（`:47-49`）。颜色 `(0,1,1)`。
- `renderParticle :54-89`：`blend (770,771)`=SRC_ALPHA/ONE_MINUS_SRC_ALPHA，`depthMask(false)`，**无 cull 开关**；
  `fade = (age+pt)/maxAge`；`frame = min(15, (int)(14*fade)+1)` → 贴图 `thaumcraft:textures/models/ripple<frame>.png`（帧 1..15，**无 ripple0**）；
  `lightmap = 220`；`rotY(-yaw)` → `rotX(pitch)` → `translate(0,0, 2*target.height + target.width/2)` →
  `scale(0.25*target.height, 0.25*target.height, -1*target.height)` → 画 `hemis.obj` → `glColor4f(0, 0.5, 0.5, 1)`。
  贴图路径 `thaumcraft:models/obj/hemis.obj`（`:28`）。

#### 5) `ISFXDispatcher`（1.12 的派发器，`:11-24`）
`elemental / fxAbsorption / fluxExplosion` 三个静态方法，全部 `Minecraft.getMinecraft().effectRenderer.addEffect(...)`。

#### 6) 其它 1.12 调用点（TC6 `FXDispatcher` / `ParticleEngine`）
- `TileVat.java:331`：`FXDispatcher.INSTANCE.blockRunes(x, y-2.5, z, rand*0.2f, 0.1f, 0.7f+rand*0.3f, 25, -0.03f)`（注入时每 tick）。
- `TileVat.java:367`：不稳定随机电弧 `arcLightning(x+0.5, y+1.5, z+0.5, xx, yy, zz, 0.8f, 0.1f, 1.0f, 0.1f)`，
  触发条件 `infusing && stability<0 && rand.nextInt(200) <= |stability|`（`:360`），终点 `xx,zz` 在 ±4 格内、`yy` 抬到空气处。
- `TileVat.java:385/394/401/405`：**注魔原料飞入**，由 `sourceFX` map（`drawnIngredientParticles :372-417`）驱动：
  - `fx.loc == vat.pos`（玩家供料）：4×`drawInfusionParticles4(玩家包围盒内随机点, vat.x, vat.y+1, vat.z)`。
  - 基座且 `rand.nextInt(3)==0`：`drawInfusionParticles3`；`ItemBlock`：4×`drawInfusionParticles2(…, pos.up(), blockstate, md)`；否则 4×`drawInfusionParticles1(…, pos.up(), stack)`。
- `EntityGravekeeper.java:53-55`（仅 `isRemote`）：`beamBore(posX, posY+height/2, posZ, ent.posX, ent.posY+height/2, ent.posZ, 0, 16773444(0xFFEE44), false, 2.5F, 1, 1)`。
- `EntityEventHandler.java:442`：雷电皮肤反伤 `arcLightning(base.pos, target.pos, 0.2F, 0.8F, 0.8F, 1.0F)`（**服务端事件里直接调客户端 API**，1.12 靠 `@SideOnly` 外的反射容错）。
- `OrdoLens.java:71/83/92`：`FXDispatcher.INSTANCE.scanHighlight(target/item/blockPos)`。
- `RenderModifiedMatrix.java:109-120`：排气羽流（`isExpunging` 时每帧）：`FXGeneric`；`setMaxAge(100+rand(60))`、`setRBGColorF(1,0.3,0.9)`、
  `setAlphaF(0.5f,0.0f)`、`setGridSize(16)`、`setParticles(56,1,1)`、`setScale(2.0f,5.0f)`、`setLayer(1)`、`setSlowDown(1.0)`、
  `setWind(0.001)`、`setRotationSpeed(rand, rand?−1:1)`；位置 `(x+0.2+rand*0.6, …)`。
- `RenderModifiedMatrix.java:34-79 drawHalo`：光芒三角扇（见 §5 #10）。
- `RenderEmber.java:26-44`：`renderFacingQuad(..., 16, 16, 144+i+7, 0.08F, 11197951, 1.0F, 1, pticks)`，`i=ticksExisted%7` → 帧 151..157；贴图 `isorropia:textures/misc/particles.png`（256²）。
- `RenderTaintPig.java:44`：`renderFacingQuad(..., 32, 32, 192+i, 2.0F, 11197951, 1.0F, 1, pticks)`。
- `AirLens.java:74/78/94/98/116`：`UtilsFX.renderQuadCentered(TEX_*, size, 1,1,1, -99, 771, alpha)`（世界内叠加四边形）。

#### 7) 1.12 触发客户端特效的网络包
- `ISPacketFXInfusionSource`（`common/network/ISPacketFXInfusionSource.java`）：服务端 `TileVat.java:752`（玩家供料，`color=entityId`）、
  `:792`（基座供料，`color=0`）发出。载荷 = `p1(长期)`+ `p2(长期)`+`color(int)`+`index(int)`。
  客户端 `onMessage :50-76`：把 `(p2, ticks, color)` 存进 `TileVat.sourceFX`（基座=60 tick、玩家=15 tick），不直接画粒子。
- `ParticuleDestroyMessage`（`common/network/ParticuleDestroyMessage.java`）：服务端 `EntityAIEatTaint.java:160`（10 格范围）发出；
  客户端 `:36` → `effectRenderer.addBlockDestroyEffects(pos, world.getBlockState(pos))`（原版碎块粒子）。
- `thaumcraft.common.lib.network.fx.PacketFXWispZap`（TC6 自带，`EntityAIZapAttack.java:93`）：直接播 wisp zap（TC4R 无对应包 → 用 `spawnArc`/`spawnTargetedWisp` 替代）。

### 1.2 1.7.10 Thaumic Horizons（TC4 原型，**首选参考**）

#### A) `FXContainment`（禁闭爆发）— `client/fx/FXContainment.java`
- 构造 `:21-38`：`particleMaxAge=40`；`particleAlpha=0.5`（后被逐顶点 0.1 覆盖）；`particleScale=1.0`；`noClip=true`；颜色最终 `(1,1,1)`。
- `renderParticle :40-89`：`glDepthMask(false)`，`blend (770,771)`；贴图 `thaumcraft:textures/misc/eldritch_portal.png`（**2048×128，16 帧 ×128px**）；
  `frame = (int)(System.nanoTime()/50_000_000L) % 16`；`f2=frame/16`、`f3=f2+0.0625`、`f4=0`、`f5=1`；
  `f6 = 3.0f * particleScale`；四边形用 `p_3..p_7` 相机基向量展开（±f6）；顶点色 alpha **0.1**。
- `onUpdate :95-101`：每 tick `particleScale /= 1.15f`；寿命到即死。
- 触发：`PacketFXContainment(x,y,z)`（`common/lib/networking/PacketFXContainment.java`），由 `ItemFocusContainment.java:244/322`（家畜收容/释放时）发出，32 格范围。

#### B) `FXEssentiaBubble`（精华气泡）— `client/fx/FXEssentiaBubble.java`
- 构造 `:22-49`：`particleMaxAge = 20 + rand.nextInt(20)`；`particleScale=(sin(count/2)*0.1+1)*scale`（`count` = 玩家 `ticksExisted`）；
  `motionY = 0.025 + sin(count/3)*0.002`，`motionXYZ` 其余 0；颜色 = 要素色 ±20% 抖动；`particleGravity=0.2f`。
- `renderParticle :51-95`：`setBrightness(240)`；UV 固定 `u∈[0.5625,0.625]`、`v∈[0.0625,0.125]`（256² 图 → 像素 x144..160,y16..32）；
  alpha **0.5**；`s = sin((age-count)/5)*0.25 + 1`；`var12 = 0.1f*scale*s`；四边形 ±var12。
- `onUpdate :101-117`：`delay` 递减（网络包里 `delay=i*2+2`）；`motionY += 0.00125`；`scale *= 1.05`；`motionY *= 0.985`。
- 触发：`PacketFXEssentiaBubble(x+0.5, y-2, z+0.5, aspectColor)`（`TileVat.java:1795-1804`），客户端一次生成 **10** 颗，
  位置 `x + 1.5*(rand-0.5)`，`scale=0.3`，`delay=i*2+2`（`PacketFXEssentiaBubble.java:37-49`）。

#### C) `FXEssentiaTrail`（精华流光）— `client/fx/FXEssentiaTrail.java`
- 构造 `:24-54`：`particleMaxAge = 20+rand(20)`；`particleScale=(sin(count/2)*0.1+1)*scale`；
  初速 `x=sin(count/4)*0.003+gauss*0.002`、`y=0.1+sin(count/3)*0.002`、`z=sin(count/2)*0.003+gauss*0.002`；颜色 = 要素色 ±20%；gravity 0.2。
- `renderParticle :56-97`：`setBrightness(240)`；与气泡同 UV（x144..160,y16..32）；alpha 0.5；`s=sin((age-count)/5)*0.25+1`；`var12=0.1*scale*s`。
- `onUpdate :103-140`：摩擦 0.985 且 clamp ±0.05；朝目标加速 `0.01/min(1,dist)`；`dist<2` 时 `scale*=0.98`；`scale<0.2` 即死。
- 触发：`TileEssentiaDynamoRender.java:40-52`，起点 `(x+0.5, y, z+0.5)`，目标 `(x+0.5, y+0.5, z+0.5)`，`count=玩家ticksExisted`，`scale=0.3`，`noClip=true`。

#### D) `FXSonic`（1.7.10 版）— `client/fx/FXSonic.java`
- `:27-66`：`particleMaxAge = age + rand(age/2)`；`noClip=true`；`particleScale=1.0`；方向改由 `dir`(0..5) 决定 `yaw/pitch`（0→pitch 90、1→pitch −90、2→yaw 180、3→yaw 0、4→yaw 90、5→yaw 270）。
- `renderParticle :68-101`：`blend (770,1)`（SRC_ALPHA, ONE），`disable cull`；`frame = min(15,(int)(14*fade)+1)`；
  `lightmap=220`；`rotY(-yaw)`→`rotX(pitch)`→`scale(0.5, 0.5, -0.5)`→`glColor4f(0, 0.5, 0.5, 1)`→`model.renderAll()`；贴图 `thaumcraft:textures/models/ripple<frame>.png`。
- 触发：`TileVortexStabilizer.java:132-139`，每 tick 生成（最多 3 个并存），位置 = 目标点 `+0.5`，`age=10`，`dir=direction`。

#### E) 其它 1.7.10 调用点（`Thaumcraft.proxy.*`，优先照抄）
- **注魔符文**：`TileVat.java:793-802` `blockRunes(w, x, y-2, z, 0.5+rand*0.2, 0.1, 0.7+rand*0.3, INFUSER_PARTICLE_COUNT=25, -0.03f)`。
- **不稳定**：`TileVat.java:895-905` `nodeBolt(中心, ±2 格内随机点)`，条件 `rand.nextInt(INSTABILITY_EFFECT_CHANCE=200) <= instability`。
- **原料飞入**：`TileVat.java:820-894`（结构同 1.12，但用 `particleCount(2)` 决定粒子数：ALL→2×base）。
- **精华气泡**：`TileVat.java:1795`（见 B）。
- **矩阵 / 灵魂信标**：`TileSoulBeacon.java:35` `arcLightning(玩家眼位, 信标中心)`。
- **守墓者**：`EntityGravekeeper.java:72-84` `proxy.beam(自身, 目标, type=0, color=16773444(0xFFEE44), reverse=false, endmod=2.5, age=1)`。
- **雷电皮肤**：`EventHandlerEntity.java:395/417/441/492` `arcLightning(...)`；`:596` `spark(...)`；`:812-813` `blockSparkle(beacon, x, y-2/y-3, z, 16777215, 20)`。
- **漩涡**：`TileVortex.java:104/119` `burst(4.0f)`；`:322` 饥饿节点 `particleCount(1)` + 随机 rayTrace 拉粒子；
  `TileVortexRender.java` 用 `thaumcraft:textures/misc/nodes.png` 作 `renderFacingStrip`、`thaumcraft:textures/misc/vortex.png` 作旋转四边形。
- **透镜**：`ItemLensOrderEntropy.java:208/246/273` `blockRunes(...)`。
- **精华中继/扫描**：`ClientProxy.java:453`（`arcLightning` 拆解）、`TileRecombinatorRender.java:62` `proxy.beam`。

> 选参考的原则：**1.7.10 是 TC4 原型**，凡 TC4R 提供等价 API（blockRunes / arcLightning / nodeBolt / drawInfusionParticles1..4 / beam / beamBore / burst / wispFX / sparkle / blockSparkle）时，
> 行为与 1.7.10 一致，应直接映射；`FXContainment / FXEssentiaBubble / FXEssentiaTrail / FXSonic` 在 1.12 里**不存在**（TC6 换了机制），只能以 1.7.10 为准。
> 反之，`FXCelestialAbsorption / FXElementalWisp / FXFluxExplosion / RenderModifiedMatrix 排气 / ISPacketFXInfusionSource` 只在 1.12 存在，以 1.12 为准。

### 1.3 TC4R 可复用件与差距（关键）

`spawnGenericParticle`（共享参考 A.1）签名：
`(level, pos, Vec3 velocity, r, g, b, alpha, boolean loop, int start, int num, int inc, int maxAge, int delay, float scale)`。
后端 `ChampionGenericParticle`（`reference/tc4r-20721-sources/.../client/ChampionGenericParticle.java:44-142`）：

- 单 `alpha`、单 `scale`（`quadSize = 0.1F*scale`）、单 `friction = 0.98`、`hasPhysics=false`、`lifetime = maxAge + delay`；
- 层恒为 `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`（**无 `setLayer` 区分**）；
- `tick`：loop 时 `frame = start + age/inc % num`；非 loop 时 `frame = start + min(num*age/lifetime, num-1)`（忽略 `inc`）；
- UV：16×16 cell，`CELL_WITH_BLEED_GUARD = 0.0624375`，经 `EffectAtlases.PARTICLES` 映射。
- **没有**：`setAlphaF(float[])`、`setScale(float[])`、`setRandomMovementScale`、`setWind`、`setRotationSpeed`、逐粒子随机速度、`setGridSize`。
  即 #1/#3/#9 的“每颗随机 alpha 曲线 / 双 scale 段 / 随机位移 / 风 / 自转”全部丢失。

其余可直接替换的调用（共享参考 A.10 迁查表）：
`blockRunes→spawnBlockRune`、`arcLightning→spawnArc`、`nodeBolt→spawnNodeLightning`、`burst→spawnBurst`、
`drawInfusionParticles1→spawnInfusionItem`、`2→spawnInfusionBlock`、`3→spawnAttractedSparkle(mode 1)`、`4→spawnAttractedSparkle(mode 2)`、
`drawGenericParticles→spawnGenericParticle`、`scanHighlight→spawnParticle(SCAN_RUNE)`、`beam/beamBore→spawnBeam`、
`beamCont→showFocusBeam`、`blockSparkle→WorldEffectsApi.blockSparkle`、`sparkle→spawnQualitySparkle`、`wispFX→spawnWispMote`。

---

## 2. 端口现状（file:line 证据）

- **完全没有注册任何粒子**：无 `assets/isorropia/particles/` 目录、无 `ParticleProvider`、无 `RegisterParticleProvidersEvent`
  （`grep -rn "ParticleProvider|RegisterParticleProviders" src/main/java` 仅命中 `EntityIncubatedEgg.java:6/37` 的原版 `ParticleTypes.ITEM`）。
- `client/fx/CreatureFx.java:16-23`：只有 `gravekeeperBeam`，用 `ThaumcraftClientEffectsApi.spawnEssentiaTrail(...)` 近似 1.12 的 `beamBore`；
  调用点 `common/entities/EntityGravekeeper.java:72`（`tickCount % 2 == 0` 才发，客户端分支）。这是**已有但语义偏离**（流光 ≠ 光束）。
- `common/tiles/TileVat.java` 的服务端占位（`WorldEffectsApi.blockSparkle`）：
  `:247`(治疗闪光 2 粒)、`:327`(开始注入 0x7F3FBF×6)、`:382`(0xFFF7A0×3)、`:422`(0xFFFFFF×2)、`:455/:459`(0xFFFFFF×12)、`:546`(zap 0x9B00FF×24)。
  这些**都不是上游行为**（上游对应的是吸收/爆发/符文粒子），只是为了“有反馈”的替身。
- `common/infusion/CreatureAbilities.java:90`：雷电皮肤用 `blockSparkle(0x7FE1FF, 6)` 代替 1.12 的 `arcLightning`（退化）。
- `RenderEmber.java:22-23` + `Billboards.java:20-47`：ember 已用 `RenderType.eyes(sheet)` 手工四边形重画（近似 TC6 `renderFacingQuad`）——
  UV `(frame%16)/16`，`grid=16`，对应 `isorropia:textures/misc/particles.png`（**256²**）。
- `client/renderer/tiles/RenderVat.java` 存在（1 个文件）；**没有** 矩阵 BER（`TileModifiedMatrix` 有 BE 但无 renderer，`ClientSetup.java:71-74` 只注册了 VAT）。
  因此 #9（排气羽流）、#10（矩阵 halo）、#5/#6（符文/电弧）在端口**全缺**。
- 网络层：`common/network/` 只有 `IsorropiaNetwork.java` + `SyncLivingDataPacket.java`；1.12 的 `ISPacketFXInfusionSource` / `ParticuleDestroyMessage` **未移植**，
  也没有 `sourceFX` 渲染通路（`TileVat.java` 无 `getSourceFX`）。
- **死接线风险**（对齐 MEMORY 的 “dead-wiring defect”）：上述 `WorldEffectsApi.blockSparkle` 是唯一在跑的 FX；其余渲染路径均缺。

---

## 3. 资源清单（是否需要、是否已存在）

| 资源 | 上游 | 端口状态 | 处理 |
|---|---|---|---|
| `isorropia:textures/misc/particles.png` 256² | 1.12 自带（TC6 拷贝） | **存在**（256²，`.mcmeta` blur） | 保留；ember/taintpig 用 |
| `isorropia:textures/misc/nodes.png` 2048² | 1.12 自带 | **存在** | 若做漩涡优先用 `thaumcraft:misc/nodes.png`（TC4R 1088² 打包图），或保留本文件当独立贴图 |
| `isorropia:textures/fx/vortex.png` 64² | 1.7.10 TH | **存在** | 保留 |
| `isorropia:textures/fx/ripple.png` 64² | 1.7.10 TH（单帧） | **存在** | 声波需要 `ripple1..15`，见下 |
| `thaumcraft:textures/models/ripple1..15.png` 128² | TC4 | TC4R **有 1..15**（共享参考 C） | 用 `thaumcraft:` 命名空间；**1.12 引用的 `thaumcraft:models/obj/hemis.obj` 路径必须改为 `thaumcraft:textures/models/hemis.obj`** |
| `thaumcraft:textures/models/hemis.obj` | TC4 | **有** | 直接用 |
| `thaumcraft:textures/misc/eldritch_portal.png` 2048×128 | TC4 | **有** | 直接用 |
| TC4 `misc/particles.png`（打包 200×440） | TC4 | TC4R 有 | 自写精华气泡/流光的 UV 一律经 `ThaumcraftClientRenderApi.particleAtlasUv(0.5625,0.625,0.0625,0.125)` 换算 |
| `isorropia:particle/*.png` 精灵 | — | **缺** | 仅当走 `registerSpriteSet` 时才需要；自建 ParticleProvider 可直接绑独立贴图，无需精灵 JSON |

---

## 4. 目标 1.20.1 设计（逐元素）

### #1 天穹吸收 `FXCelestialAbsorption`（**自定义粒子**）
- 参考：1.12（唯一来源）。**必须自写**：TC4R `spawnGenericParticle` 无 `setAlphaF(float[])`/`setScale(float[])`/`setRandomMovementScale`。
- 新建 `client/particle/CelestialAbsorptionParticle.java`（`extends TextureSheetParticle`）：
  逐帧复刻 §1.1(1) 的 50 颗参数（absorb 分支、`slowDown` 0.995/0.95、逐颗 alpha 数组、`scale 0.5→0.125` 双段、随机位移 0.0025/0.001/0.0025、
  `frame = start(320/512) + age%16`、`delay`）。
- 粒子层：`WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`（等价 TC6 layer 0）。
- 外部触发：`TileVat` 服务端在吸收发生时走 `ActionPresentationApi.presentAt(level, isorropia:vat_absorb, origin, target, null, absorb?1:0, 0)`；
  客户端在 `FMLClientSetupEvent` 注册 handler → 生成 N 颗本粒子。**不要**在 BE 里直接 new 粒子（BE tick 可能在服务端）。
- 注册：`RegisterParticleProvidersEvent.registerSpriteSet`（或用独立贴图 + `registerSpecial`）。
- sync：无（origin 足够；颜色/absorb 走 `detail`/`value`）。

### #2 元素光团 `FXElementalWisp`
- 1.12 死代码 → **不实现**（若日后需要，按 #3 的模板做 10 颗）。

### #3 通量爆炸 `FXFluxExplosion`（**自定义粒子**）
- 参考：1.12。同 #1 的自写模板；参数见 §1.1(3)（540 颗、初速 ±rand/1.5、loop、slowDown 0.955、alpha 数组、双 scale、随机位移）。
- 建议**降数量**：540 颗 × 每颗 16 帧对大场景过重；提供 config 缩放（原作不降，实机看帧率）。
- 触发：`TileVat` 客户端在爆炸分支（1.12 `:1158`）本地生成；服务端 `level.explode(...)` 已存在。可用 `ActionPresentationApi` 广播 `isorropia:vat_flux_boom`。
- sync：只需 origin（`BlockPos`）。

### #4 声波 `FXSonic`
- **直接调用** `ThaumcraftClientEffectsApi.spawnSonicWave(livingEntity, baseAge)`（共享参考 A.1 / A.10）。
  其 `EldritchSonicEffect` 与 1.12 `FXSonic` 的 `scale(0.25h,0.25h,-h)`、`ripple1..15`、`lifetime=baseAge+rand(baseAge/2)` 完全一致。
- 差异：1.7.10 版用固定 `(0.5,0.5,-0.5)` 与 `dir` 枚举；1.12 用目标实体的身高与朝向。TC4R 的 `spawnSonicWave` 取实体朝向 → 与 1.12 一致，**这是正确选择**。
- 触发：`TileVortexStabilizer` 的等价 BE（若端口有）在 `redstoned` 时每 tick 调一次（基座 10 tick 寿命、最多 3 个并存）。
- 风险：blend 差异（1.12 = `(770,771)` 半透明；TC4R `EldritchSonicEffect` 用 `additiveTranslucentEmissiveDepthWriteBlurred`）——实机比对颜色是否发白发亮。

### #5 vat 注入符文 `blockRunes`
- **直接调用** `ThaumcraftClientEffectsApi.spawnBlockRune(level, Vec3(x,y-2,z), r, g, b, duration=25, gravity=-0.03F)`。
  API 内部 +0.5（共享参考 A.1），故 `Vec3` 传方块角坐标 `(x, y-2, z)` 即可。
- 颜色：`r=0.5+rand*0.2, g=0.1, b=0.7+rand*0.3`（1.7.10 `TileVat.java:793`；1.12 的 r 少 0.5、y 用 −2.5，**采用 1.7.10**）。
- 位置：1.7.10 用 `y-2`；1.12 用 `y-2.5`。取 1.7.10（原型）。
- 触发：在**客户端** BE tick（或 BER）每 tick 调（1.12/1.7.10 都是在客户端 `doEffects()` 里调）。
  端口把 `doEffects` 逻辑放进 `TileVat` 的客户端分支或 `BlockEntityTicker` 的客户端侧。
- sync：BE 需同步 `infusing/craftCount/startUp/stability`（端口已有 `sync()`）。

### #6 vat 不稳定电弧
- **直接调用** `spawnArc(level, start, end, EffectColor.rgb(intColor), height)`：1.12 用 `arcLightning(…, 0.8,0.1,1.0, h=0.1)` → 颜色 `0xCC1AFF`(0.8,0.1,1.0)，高度 0.1。
- 位置：start = `(x+0.5, y+1.5, z+0.5)`；end = `±4` 格内、`yy` 抬到空气处（1.12 `:360-367`）。
- 条件 `stability<0 && rand.nextInt(200) <= |stability|`，**客户端**判定即可（无需 sync；若怕两端不一致，用 `presentAt`）。
- 1.7.10 的 `nodeBolt` 版本（`TileVat.java:897`）也映射为 `spawnNodeLightning`；二选一，建议用 1.12 的 `spawnArc`（更接近“暴走放电”）。

### #7 注魔原料飞入（`ISPacketFXInfusionSource` / `sourceFX`）
- 端口**未移植**，需重建。上游行为：服务端在玩家/基座供料时发 `(p1=vatPos, p2=sourcePos, color=entityId|0)`（32 格）；
  客户端把 `(p2, ticks, color)` 存进 `TileVat.sourceFX`，由 BER/客户端 tick 渲染。
- 目标：把 `sourceFX` 状态**放进 BE 的同步数据**（`getUpdateTag`/`getUpdatePacket`），键 = `p2 + color`；或继续用自定义包（`IsorropiaNetwork` 里注册）。推荐**包**（与 1.12 一致，避免每 tick 全量同步 map）。
- 每个 entry 每 tick：
  - 玩家（`p2 == vatPos`）：4× `spawnAttractedSparkle(level, src, lifetimeTarget, () -> Vec3.atCenterOf(vatPos.above()), 2)`（对应 `drawInfusionParticles4`）。
    目标点 TC4 为 `(x+0.5, y+1-0.5, z+0.5)`（共享参考 A.1）。
  - 基座：`rand.nextInt(3)==0` → `spawnAttractedSparkle(..., mode 1)`（= `drawInfusionParticles3`）；
    `ItemBlock` → 4× `spawnInfusionBlock(level, src, target, state, vatPos.above(), face)`（= `drawInfusionParticles2`）；
    其它物品 → 4× `spawnInfusionItem(level, src, (x+0.5,y+0.5,z+0.5), stack)`（= `drawInfusionParticles1`）。
- 起点：`(fx.loc.x+rand, fx.loc.y+rand+1.0, fx.loc.z+rand)`；`ticks` 基座 60、玩家 15。
- **src 侧**：`ISPacketFXInfusionSource` 由服务端在 `TileVat.xpStep`（玩家）/`ingredientStep`（基座）里发；端口需在等价位置补发。

### #8 原料被消耗的碎屑（`ParticuleDestroyMessage`）
- 上游 1.12 行为 = 原版方块破坏粒子。目标：
  - 简单：服务端 `WorldEffectsApi.blockSparkle(level, pos, 10.0, -9999, 20)`（随机色，对应 1.7.10 `PacketFXInfusionDone` 的 `blockSparkle(…,-9999,20)`）。
  - 忠实：客户端收到包后 `level.playLocalSound`/`Minecraft.getInstance().particleEngine` 播原版 destroy 粒子——
    直接在 `LivingEntity` 吃到 taint 的位置 `level.addDestroyBlockEffect(pos, state)`（原版 API）。
- 触发：1.12 `EntityAIEatTaint.java:160`（10 格）；端口在等价 AI 里发。

### #9 矩阵排气羽流（`RenderModifiedMatrix:109`）
- 参考：1.12（唯一）。**必须自写粒子**（需要 wind / rotationSpeed / 双 scale / alpha 双段，TC4R 都没有）。
- 新建 `client/particle/ExpungePlumeParticle.java`：`maxAge=100+rand(60)`、颜色 `(1,0.3,0.9)`、`alpha 0.5→0.0` 线性、
  `scale 2.0→5.0`、`layer 1`、`slowDown 1.0`、`wind 0.001`、`rotSpeed=rand`、旋转方向 ±1。
- 触发：在 `TileModifiedMatrix` 的 BER（新建，见 #10）render 时，若 `master.isExpunging()`，每帧随机 1 颗（1.12 就是这么做的）。
- 数值位置：`(bx+0.2+rand*0.6, by+0.2+rand*0.6, bz+0.2+rand*0.6)`。

### #10 矩阵光芒 halo（**渲染，非粒子**）
- **直接调用** `ThaumcraftClientVisualsApi.renderInfusionHalo(poseStack, buffers, craftCount)`（共享参考 A.3/A.9，内部自带 `translate(0.5,0.5,0.5)`）。
  1.12 `drawHalo` 的 `q = fancy?20:10`、`Random(245L)`、6 次累乘旋转、`len=(r*20+5)/20*growth`、`w=(r*2+1)/20*growth`、
  白心 + `(204,0,255,0)` 外点 —— 与 TC4R `renderHalo` 逐项一致（共享参考 B.4）。**不要重写**。
- 需要在端口新建 `client/renderer/tiles/RenderModifiedMatrix.java`（BER）并在 `ClientSetup` 注册（当前 `ClientSetup.java:71-74` 只注册 VAT）。
- `shouldRenderOffScreen=true`；包围盒 `pos-1 … pos+1`（对齐 TC4R 做法）。

### #11 灵魂信标电弧 `TileSoulBeacon:33`
- **直接调用** `spawnArc(level, playerEyePos, Vec3(x+0.5, y+0.75, z+0.5), EffectColor.rgb(0x05FF05), 0.5F)`
  （1.12 颜色 `(0.05,1.0,0.05)`；1.7.10 `TileSoulBeacon.java:35` 同）。
- 触发点：1.12 在 `activate()` 里 `world.isRemote` 分支 → 客户端本地调。端口：`BlockSoulBeacon.use` 在客户端分支调；或服务端 `presentAt` + 客户端 handler。
- 同时 `playSound(SoundsTC.zap)`（TC4R 音效）。

### #12 雷电皮肤反伤弧 `EntityEventHandler:442`
- **直接调用** `spawnArc(level, basePos+eyeball, targetPos+eyeball, EffectColor.rgb(0x33CCCC), 1.0F)`（1.12 `(0.2,0.8,0.8)`）。
- 1.12 在**服务端事件**里调客户端 API（靠容错）；1.20.1 必须改成
  `ActionPresentationApi.presentAt(serverLevel, isorropia:shock_skin, from, to, null, 0, 0x33CCCC)` + 客户端 handler → `spawnArc`。
- 端口现状：`CreatureAbilities.shockBack` 用 `blockSparkle` 占位（`CreatureAbilities.java:90`）→ 应升级为上面的 `presentAt`。
- 1.7.10 `EventHandlerEntity` 的 4 处 `arcLightning`（:395/417/441/492）与 `spark`（:596）同法处理。

### #13 透镜扫描高亮 `OrdoLens`
- TC4R **无 `scanHighlight`**；近似 = `spawnParticle(level, ThaumcraftParticle.SCAN_RUNE, pos, (rgb…))`（共享参考 A.10）。
- 触发：`OrdoLens.handleRenderGameOverlay`（客户端）里对 `isThingStillScannable` 的实体/物品/方块调用。
- 1.7.10 `ItemLensOrderEntropy` 用 `blockRunes` 表达同一意图 → 也可 `spawnBlockRune`。两者择一，建议 `SCAN_RUNE`（更接近 1.12 `scanHighlight`）。

### #14 守墓者圣光 `EntityGravekeeper`
- 1.12：`beamBore(…, type=0, color=0xFFEE44, reverse=false, endmod=2.5, progress=1, age=1)`；
  1.7.10：`proxy.beam(…, type=0, color=0xFFEE44, reverse=false, endmod=2.5, age=1)` —— 两者几乎一致（`beamBore` 是 `beam` 的 handle 版）。
- 目标：`spawnBeam(level, from, to, new BeamSettings(BeamTexture.DEFAULT, EffectColor.rgb(0xFFEE44), 1, width, 2.5F, false, true, 5, true))`
  或逐帧 `ThaumcraftClientVisualsApi.renderBoreBeam(pose, buffers, start, end, rgb, false, animation)`（在 `RenderGravekeeper` 里画）。
  持续型（每 tick 重发）→ 逐帧版更合适。
- `width` 取值见共享参考风险 #6（TC4 构造里 `setSize(0.02,0.02)`，实际宽度存疑）——**实机定**。
- 端口现状 `CreatureFx.gravekeeperBeam` 用 `spawnEssentiaTrail`（`:22`）**语义错误**（流光 ≠ 光束）→ 改为 `renderBoreBeam` 或 `spawnBeam`。
- sync：无需（两端各自有实体位置）；`RenderGravekeeper` 客户端每帧算 start/end。

### #15 禁闭爆发 `FXContainment`（**自定义粒子**，1.7.10 为准）
- 新建 `client/particle/ContainmentParticle.java`：`maxAge=40`、`scale=1.0`（每 tick `/1.15`）、
  贴图 `thaumcraft:textures/misc/eldritch_portal.png`、`frame = (nanoTime/50ms)%16`、UV `[frame/16, frame/16+0.0625] × [0,1]`、
  `alpha=0.1`、色 `(1,1,1)`、四边形 `±3.0*scale` 相机基向量展开、`blend TRANSLUCENT_TRANSPARENCY` + `depthMask(false)`。
- RenderType：自定义（`NativeRenderTypes.create` + `TRANSLUCENT_TRANSPARENCY` + `COLOR_WRITE` + `NO_CULL`，shader `entity_translucent_emissive` 或 `particles`），
  避免 `particle` shader 的 alpha<0.1 裁剪把 0.1 的柔边削没（共享参考风险 #3）。
- 触发：`PacketFXContainment(x,y,z)` 等价——端口若无 containment 焦点物品，此项可暂缓；有则在释放处 `presentAt` + handler。
- sync：仅坐标。

### #16 精华气泡 `FXEssentiaBubble`（**自定义粒子**，1.7.10 为准）
- 新建 `client/particle/EssentiaBubbleParticle.java`：寿命 `20+rand(20)`、`scale=(sin(count/2)*0.1+1)*0.3`（每 tick ×1.05）、
  `motionY = 0.025 + sin(count/3)*0.002`（每 tick `+=0.00125`、`*=0.985`）、gravity 0.2、
  颜色 = 要素色 ±20%、`alpha=0.5`、`FULL_BRIGHT`（`setBrightness(240)` → `LightTexture.pack(15,0)` 或 `FULL_BRIGHT`）。
- UV：源 256² cell `u[0.5625,0.625] v[0.0625,0.125]` → 若绑独立 256² `isorropia:particles.png` 直接用它；
  若走 TC4R 打包图集则 `particleAtlasUv(0.5625,0.625,0.0625,0.125)`。**强烈建议绑独立贴图**（避免打包换算的不确定性）。
- 触发：`PacketFXEssentiaBubble(x+0.5,y-2,z+0.5, aspectColor)` → 一次 10 颗，位置 `x+1.5*(rand-0.5)`、`scale=0.3`、`delay=i*2+2`。
  端口可复刻包，或 `presentAt`（但 `presentAt` 无“一次 10 颗 + aspect 色” → 用包更直接）。
- 源头：`TileVat` 的 `clientEssentiaFX(aspect)`（1.7.10 `:1795`，在 `takeFromContainer` 成功时）。

### #17 精华流光 `FXEssentiaTrail`
- **直接调用** `spawnEssentiaTrail(level, Vec3(src), Vec3(target), phase=player.ticksExisted, color, scale=0.3F)`（共享参考 A.1）。
  `EssentiaTrailParticle` 已复刻（alpha 0.5、quad `(sin(phase/2)*0.1+1)*scale*0.1`、寿命 `d*30/2+rand(d*30)`）。
- 注意：**不自动 +0.5** → 传 `Vec3(x+0.5, y, z+0.5)` 起点、`Vec3(x+0.5, y+0.5, z+0.5)` 目标（1.7.10 `TileEssentiaDynamoRender:40-50`）。
- 触发：`TileEssentiaDynamoRender` 等价 BER 里每帧（`essentia != null && ticksProvided > 0`）。
- `noClip`：TC4R API 无该参数（默认行为）——接受差异。

### #18 漩涡 / 饥饿节点（`TileVortex` + `TileVortexRender`）
- 渲染：用 `ThaumcraftClientVisualsApi.renderNode(AuraNodeState, pose, buffers, animation, opacity)` /
  `renderOrientedNode`（`misc/nodes.png` 打包）替代手写 `renderFacingStrip`；核心用 `renderBloomAura`。
  `TileVortexRender` 的 `alpha=(viewDistance-dist)/viewDistance`（viewDistance 64）、`frame=(nanoTime/40ms + x)%32`、`scale=0.4*size`(size=10)
  与 TC4R `AuraNodeRenderer`（共享参考 B.8）同源，可对齐。
- 粒子：`burst(4.0f)` → `spawnBurst(level, pos, 4.0F)`（`TileVortex.java:104/119`）；饥饿节点牵引 → `spawnAttractedSparkle` 或自写。
- 贴图：`thaumcraft:misc/nodes.png`（TC4R 1088²）优先；`isorropia:misc/nodes.png` 2048² 可作独立贴图但会与 TC4R 观感不一致。

### #19 ember / taintpig 火球贴图
- 已做（`Billboards.drawFrame`，`RenderType.eyes`）。**核对**：1.12 帧号 = `144+i+7`(i=0..6) = 151..157；端口 `RenderEmber.java:23` = `151 + tickCount%7` ✓。
- `isorropia:textures/misc/particles.png` 是 256² 独立贴图，UV 直接 `(frame%16)/16` ✓。taintpig 同法（帧 192+i，grid 32）。
- 风险：`RenderType.eyes` 的 `ADDITIVE_TRANSPARENCY(ONE,ONE)`（共享参考 B.1）与 1.12 的 `(770,1)` 略有差别 → 实机看亮度。

### #20 服务端 `WorldEffectsApi.blockSparkle` 占位
- 端口已有调用（`TileVat.java:247/327/382/422/455/459/546`、`CreatureAbilities.java:90`）。
- **保留**作为兜底，但应逐步被 #1/#3/#5 的真实特效取代；`blockSparkle` 的 `color=-9999` 语义与 TC4 一致（共享参考 A.1）。

---

## 5. 注册与同步汇总

- **粒子注册**：`ClientSetup` 增加 `@SubscribeEvent static void onRegisterParticleProviders(RegisterParticleProvidersEvent e)`
  （模组总线），为每个自建粒子 `registerSpriteSet`（有精灵 JSON）或 `registerSpecial`（独立贴图）。
  新增 `assets/isorropia/particles/<name>.json`（仅 `registerSpriteSet` 需要）。
- **BE 同步**：`TileVat` 需同步 `infusing, celestialAuraNeeded, celestialAura, craftCount, startUp, stability, expunge, mode`（部分已有 `sync()`）；
  `sourceFX` 建议走**包**（`IsorropiaNetwork` 注册 `ISPacketFXInfusionSource` 的 1.20.1 版），不塞进 updateTag。
- **服务端触发通道**：优先 `ActionPresentationApi.presentAt(...)`（共享参考 A.8）——服务端只发坐标/颜色，客户端 handler 调 Effects API。
  这是“服务端触发、客户端画”且无需自建网络包的首选；复杂载荷（aspect 色 + 10 颗气泡 + delay）才用自建包。
- **钟表**：动画时钟一律用客户端本地 `cameraEntity.tickCount + partialTick`（TC4R 惯例），只同步状态量（共享参考 D.3）。

---

## 6. 风险与只能实机确认

1. **`spawnGenericParticle` 能力缺口**：若坚持用 TC4R API 而不自写粒子，#1/#3/#9 会丢掉逐颗 alpha/双 scale/随机位移/风/自转，观感明显变淡变方。
2. **alpha 裁剪**：`particle` shader 丢弃“贴图×顶点色”alpha<0.1；`entity_translucent(_emissive)` 丢弃贴图 alpha<0.1。
   禁闭粒子的 `alpha=0.1`、气泡 0.5 都可能被削边 → 必要时改 `eyes`/`entity_no_outline` shader 的自管 pass（共享参考风险 #3）。
3. **混合覆盖**：自定加色 RT 可能被 core shader JSON 覆盖 → 用 `NativeRenderTypes.create`（共享参考 D.5）。
4. **声波 blend**：1.12 `(770,771)` vs TC4R `EldritchSonicEffect` 加色 → 颜色/亮度差异需实机比对。
5. **`BeamSettings.width`**：TC4 `beamBore` 的宽度语义存疑（共享参考风险 #6）→ 守墓者光束宽度实机定。
6. **粒子量**：`FXFluxExplosion` 540 颗 ×16 帧，是原作即有的大开销；1.20.1 帧率实机确认，必要时按 `qualityScaledParticleCount` 缩放。
7. **`RenderEmber` 帧号**：1.12 `144+i+7` 与端口 `151+i` 数值相同，但 `UtilsFX.renderFacingQuad` 的帧序（是否 0-based）未从 TC4 源码逐字核对 → 实机看火球是否闪烁在同一格。
8. **`presentAt` 距离/上限**：Effects API 校验（共享参考 §通用约定）失败只打一次 WARN → 开发期盯日志 `Ignoring invalid client effect request`。

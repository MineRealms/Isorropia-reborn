# 00 — 共享渲染参考：TC4R 20732 客户端 API + TC4R 自身实现 + Forge 47 要点

本文件只描述“可用的轮子”和“TC4R 自己怎么画”，不分析 Isorropia 本身。各元素分析文档（01-xx）引用这里的锚点，不必重复抄写。

## 0. 版本基线与引用约定

| 记号 | 含义 | 位置 |
|---|---|---|
| `D:` | **20732 编译期 jar** 的 Vineflower 反编译（权威；工程实际编译它） | 包根 `dev/tc4port/thaumcraft/`，见下方再生命令 |
| `S:` | 20721 源码（`reference/tc4r-20721-sources/dev/tc4port/thaumcraft/`） | 仅作对照；20732 新增的类在 S 中不存在 |
| `TC4:` | TC4 1.7.10 4.2.3.5 反编译 | `H:/MinecraftMods/TC4/thaumcraft/` |
| `JAR` | `H:/MinecraftMods/_tc4_1201/thaumcraft-forge-4.2.3.5-1.20.1-port.0.1.0-20732.jar` | 与 `local-repo/.../thaumcraft-forge-0.1.0-20732.jar` sha256 相同（`b04bed49…3a90`） |
| `F47:` | Forge 47.4.23 sources / mapped jar | `~/.gradle/caches/forge_gradle/.../1.20.1-47.4.23/` |
| `MC:` | 1.20.1 原版 core shader | `~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar` |

`D:` 行号的再生命令（Vineflower 1.10.1，参数固定则行号可复现；建议放到 `reference/tc4r-20732-decompiled`，git 忽略）：

```bash
java -jar ~/.gradle/caches/modules-2/files-2.1/org.vineflower/vineflower/1.10.1/4f48c5947b21f9ebc743e7c80215ee839d3dc668/vineflower-1.10.1.jar \
  -dgs=1 -log=WARN \
  ~/.gradle/caches/forge_gradle/deobf_dependencies/dev/tc4port/thaumcraft-forge/0.1.0-20732_mapped_official_1.20.1/thaumcraft-forge-0.1.0-20732_mapped_official_1.20.1.jar \
  <out-dir>
```

**20721 → 20732 的 `api/client` 差异**（以 20732 为准）：新增 `BeamSettings`、`BeamTexture`、`EffectColor`、`EffectSpriteSheet`、`FocusProjectileVisual`、`LightningSettings`、`MarkerVisual`、`ScreenEffect`、`ThaumcraftClientFeedbackApi`、`ThaumcraftClientVisualsApi`、`ThaumcraftParticle`；删除 `ItemTickState`；`ThaumcraftClientEffectsApi` 从 18 个方法扩到 44 个；`ThaumcraftClientRenderApiCommon` 新增 `effectAtlasUv` 与 `showFocusBeam(..., BeamTexture, boolean)`。`api/presentation` 无增删。

**通用约定（全文适用）**
- `ThaumcraftClientRenderApi` 只是 `extends ThaumcraftClientRenderApiCommon` 的空壳（`D:api/client/ThaumcraftClientRenderApi.java:3`），静态方法两个类名都能调。
- `ThaumcraftClientEffectsApi`：参数非法时**记一次 WARN 并静默忽略**（`reject`，`D:api/client/ThaumcraftClientEffectsApi.java:574`）；`ThaumcraftClientVisualsApi` / `ThaumcraftClientRenderApiCommon` / 值类型构造器：参数非法时**抛 `IllegalArgumentException`**。
- “需 ClientLevel”= 传入的 `level` 必须 `== Minecraft.getInstance().level`（`currentLevel`，`D:…EffectsApi.java:537`），否则被拒。即：只能在客户端线程、客户端当前世界里调；公共代码要经客户端专用类转发（不要让服务端加载 `ClientLevel`）。
- Effects API 统一上限：位置/目标距离 ≤ 256 格（`distanceToSqr ≤ 65536`，`:562`）；速度 `|v| ≤ 16` 格/tick（`lengthSqr ≤ 256`，`:566`）；`size ∈ (0, 64]`、`colorType ∈ [0,7]`（`validateEffect :541`）；重力 `|g| ≤ 16`（`:558`）；颜色分量 `∈ [0,1]`（`finiteColor :554`）。
- 颜色 `int rgb` 一律按 `0xRRGGBB` 取低 24 位（`rgb & 0xFFFFFF`）。

---

## A. TC4R 客户端渲染/特效 API（逐类）

### A.1 `ThaumcraftClientEffectsApi`（全部客户端、全部需 ClientLevel；自走 tick 的粒子/效果，调用一次即可）

| 方法签名（`D:` 行 / `S:` 行） | 画什么（后端类 → 粒子层） | 参数语义/单位/约束 | 对应 TC4 `ClientProxy` |
|---|---|---|---|
| `spawnEssentiaTrail(ClientLevel, Vec3 source, Vec3 target, int phase, int color, float scale)` D:58 / S:42 | 精华流光点，沿途飞向 target（`EssentiaTrailParticle` → `WORLD_LAST_LEGACY_ATLAS_TRANSLUCENT_LAYER_1`；alpha 0.5，四边形尺寸 `(sin(phase/2)*0.1+1)*scale*0.1`，寿命 `d*30/2 + rand(d*30)`，`D:client/EssentiaTrailParticle.java:36-65`） | **不自动 +0.5**，调用方给方块中心；`scale ∈ (0,64]`；`phase`=TC4 的 `count` | `essentiaTrailFx(w,x,y,z,x2,y2,z2,count,color,scale)` TC4:client/ClientProxy.java:577（TC4 内部 +0.5） |
| `spawnSparkle(level, Vec3 pos, Vec3 target, float size, int colorType, int multiplier, float gravity, boolean noClip)` D:72 / S:58 | `FXSparkle` 等价（`SparkleParticle.spawnType` → `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`） | `multiplier 1..400`（TC4 `m`，影响寿命）；target 可等于 pos | `new FXSparkle(w,x,y,z,tx,ty,tz,size,type,m)` |
| `spawnQualitySparkle(level, Vec3 pos, float size, int colorType, float gravity)` D:88 / S:78 | 同上，带画质抽样 `rand(6) < particleCount(2)`、`multiplier=6`、`noClip=true`（`D:client/SparkleParticle.java:341`） | | `sparkle(x,y,z,size,color,gravity)` TC4:ClientProxy.java:344；4 参版 = `size 1.5f, gravity 0` (`SparkleParticle.java:410`) |
| `spawnMovingSparkle(level, pos, Vec3 velocity, size, colorType, multiplier, gravity, noClip)` D:94 / S:90 | 同上，初速度直给 | `|v| ≤ 16` | — |
| `spawnCustomSparkle(level, pos, size, r, g, b, int frameInterval, int lifetime, int firstFrame, gravity, noClip, boolean normalBlend, boolean shrink, boolean slowdown, ParticleCameraFade)` D:108 / S:105 | 自定色 sparkle；`normalBlend=true` 走 `WORLD_LAST_LEGACY_ATLAS_TRANSLUCENT_LAYER_1`，否则加色层 | `frameInterval 1..400`、`lifetime 1..12000`、`firstFrame 0..3`；`cameraFade` 非空（用 `ParticleCameraFade.NONE`） | — |
| `spawnWispMote(level, pos, size, r, g, b, boolean shrink, gravity)` D:162 / S:118 | 彩色 wisp 光团（`WispMoteParticle` → `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`，寿命 `36/(rand*0.3+0.7)`） | `size ∈ (0,1024]` | `wispFX(w,x,y,z,size,r,g,b)` = `shrink=false, gravity=0.02f` TC4:ClientProxy.java:425 |
| `spawnBlockSparkle(level, BlockPos, int color, int baseCount)` D:179 / S:131 | 方块周围闪光（`InfusionSparkleEffects.blockSparkle` → `NodeJarFormationParticle.spawnBlockSparkle`），数量 = `particleCount(baseCount)` | `color=-9999` → 每粒随机 `0.33+rand*0.67`（`D:client/InfusionSparkleEffects.java:19-21`）；`baseCount 0..400` | `blockSparkle(w,x,y,z,c,count)` TC4:ClientProxy.java:328 |
| `qualityScaledParticleCount(int base)` D:187 / S:143 | 返回 `ALL→2*base, DECREASED→base, MINIMAL→0`（`InfusionSparkleEffects.java:26-35`） | `0..400` | `particleCount(base)` TC4:ClientProxy.java:599（同公式） |
| `spawnBlockRune(level, Vec3 logicalPos, r, g, b, int duration, gravity)` D:196 / S:154 | 符文粒子（`RunedStoneRuneParticle`，寿命 `3*duration`，alpha 前 1/5 线性升到 0.5 后线性降，→ `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`） | **内部 +0.5**（传方块角坐标）；`duration 1..400` | `blockRunes(w,x,y,z,r,g,b,dur,grav)` TC4:ClientProxy.java:618（同样 +0.5） |
| `spawnMovingWisp(level, pos, Vec3 velocity, size, colorType, shrink, gravity, noClip)` D:206 / S:163 | 调色板 wisp（`PaletteWispParticle.spawnMoving`，rawVelocity；层 `NitorParticlePass.QUEUE`） | `size ∈ [0,64]` | `wispFX2(w,x,y,z,size,type,shrink,clip,grav)` ≈ `velocity=Vec3.ZERO`（TC4 初速需对照 TC4:client/fx/particles/FXWisp.java:60） |
| `spawnFollowingFallingWisp(level, Vec3 pos, Entity target)` D:223 / S:175 | 跟随实体下落的 wisp | target 必须同 level、距离 ≤ 256 | — |
| `spawnBurst(level, pos, float size)` D:236 / S:185 | wisp 爆散（`WispBurstParticle`，31 tick，`LEGACY_EFFECT_NODE_ADDITIVE`，32 帧 `wisp_burst_00..31` 64²） | | `burst(w,x,y,z,size)` TC4:ClientProxy.java:465 |
| `spawnSmokeSpiral(level, pos, float radius, float startAngle, double minY, int color)` D:242 / S:193 | 螺旋烟 | `|minY| ≤ 3e7`，`color 0..0xFFFFFF` | `smokeSpiral(w,x,y,z,rad,start,miny,color)` TC4:373 |
| `spawnFocusLightning / spawnShockLightning / spawnNodeLightning(level, Vec3 start, Vec3 end)` D:252/258/264 / S:201/210/219 | 预设闪电（`WispLightningEffect`：node = `Bolt(…,10,4.0,5)`；focus = `(6,0.5,8,0.125, outer .1/.1/.6, inner .1/.1/1)`；shock = `(0,0.4,4,0.125,…)`，`D:client/WispLightningEffect.java:57-85`） | 起止点 ≥ 1e-3 格 | `nodeBolt(w,x,y,z,x2,y2,z2)` TC4:495（TC4 坐标版 `setType(0)`、实体版 `setType(3)`，颜色差异需对照 TC4:client/fx/bolt/） |
| `spawnGenericParticle(level, pos, Vec3 velocity, r, g, b, alpha, boolean loop, int start, int num, int inc, int maxAge, int delay, float scale)` D:270 / S:225 | TC4 `FXGeneric`（`ChampionGenericParticle`，从 TC4 `particles.png` 16×16 格索引取帧，内部转打包图集；→ `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`） | `start 0..255`、`num 1..256`、`start+num ≤ 256`、`inc 1..256`、`maxAge 1..12000`、`delay 0..12000` | `drawGenericParticles(…)` TC4:803（参数顺序一致） |
| `particleType(ThaumcraftParticle)` D:328 / `spawnParticle(level, ThaumcraftParticle, Vec3 pos, Vec3 args)` D:333 | 直接 `level.addParticle(type, pos, args)`；args 语义见 A.2 | args 校验 `validProviderArguments :509` | — |
| `spawnBeam(level, Vec3 start, Vec3 end, BeamSettings)` D:343；`spawnBeam(level, Vec3 start, Entity target, BeamSettings)` D:350 | 三平面旋转光束（`CoordinateBeamEffect`，自 tick 到 `lifetime`；实体版终点 = `pos + (0, eyeHeight − bbHeight/2, 0)`，并跟随实体） | 见 A.5 `BeamSettings`；相机距起点 > 50 格（FAST 25）不生成 | `beam(...)` TC4:509 |
| `spawnLightning(level, start, end, LightningSettings)` D:359 | 自定义闪电 | 见 A.5 | `bolt / nodeBolt` 的通用版 |
| `spawnArc(level, start, end, EffectColor, float height)` D:366 | TC4 `FXArc` + 终点一个 `ELDRITCH_ARC_SPARKLE`（`D:client/EldritchArcEffect.java:62-68`） | `|height| ≤ 256` | `arcLightning(w,x,y,z,tx,ty,tz,r,g,b,h)` TC4:854（同样在终点放 FXSparkle） |
| `spawnSonicWave(LivingEntity source, int baseAge)` D:373 | TC4 `FXSonic`：`textures/models/hemis.obj` + `ripple1..15.png`（`EldritchSonicEffect`，每帧 RT `additiveTranslucentEmissiveDepthWriteBlurred`，`scale(0.25h,0.25h,-h)`，寿命 `baseAge + rand(baseAge/2)`，`D:client/EldritchSonicEffect.java:23-142`） | `baseAge 1..1200`；source 必须在当前客户端世界 | TC4:client/fx/other/FXSonic.java:34-101 |
| `spawnShieldRunes(Entity, float yaw, float pitch)` D:379 | 符文护盾（TC4 `FXShieldRunes`） | | — |
| `spawnWardHit(BlockHitResult)` D:385 | 守护方块受击半球（`hemis0..15`） | | `blockWard` TC4:625 |
| `spawnInfusionItem(level, Vec3 source, Vec3 target, ItemStack)` D:389 | 物品碎屑被吸向 target（`AttractionParticleEffects.item` → `InfusionItemParticle`，层 `WORLD_LAST_BLOCK_ADDITIVE_LAYER_2`） | stack 非空（内部 copy） | `drawInfusionParticles1(w,x,y,z,x2,y2,z2,item,md)` TC4:765，目标 `(x2+0.5, y2−0.5, z2+0.5)` |
| `spawnInfusionBlock(level, source, target, BlockState, BlockPos sourcePos, Direction face)` D:395 | 方块碎屑被吸（`InfusionBlockParticle`，`WORLD_LAST_BLOCK_TRANSLUCENT_LAYER_1`） | state 非空气 | `drawInfusionParticles2` TC4:775 |
| `spawnHungryNodeFragment(...)` D:401 / `spawnBoreFragment(level, source, Vec3 lifetimeTarget, Supplier<Vec3> target, state, sourcePos, face)` D:407 | 饥饿节点/奥术钻碎屑 | | `hungryNodeFX` TC4:759 / `boreDigFx` TC4:565 |
| `spawnVisSparkle(level, source, target, int rgb)` D:417 | vis 吸取闪光（`VisSparkleParticle`） | | — |
| `spawnVent(level, pos, Vec3 heading, int rgb, float scale)` D:423 | 管道排气（`TubeVentParticle`） | `|heading| ≤ 16`，`scale ∈ (0,64]` | `drawVentParticles(…,color[,scale])` TC4:796/815（TC4 alpha 0.4） |
| `spawnBubble(level, pos, int age, int rgb, double riseSpeed)` D:429 | 坩埚气泡（`CrucibleBubbleParticle`，`WORLD_LAST_ADDITIVE_LAYER_0`） | `age −4..1200`，`|riseSpeed| ≤ 16` | `crucibleBubble` TC4:402（age=1）、`crucibleBoil` TC4:386（age=3，`bubblespeed=0.003*j`） |
| `spawnFroth(level, pos, boolean down)` D:435 | 坩埚泡沫 | | `crucibleFroth/FrothDown` TC4:409/417 |
| `spawnDroplet(level, pos, EffectColor)` D:441 | 腐化液滴（`LEGACY_EFFECT_TRANSLUCENT`） | | `dropletFX` TC4:738 |
| `Particle spawnSwarm(level, Entity target, float speed, float turnSpeed, gravity)` D:448 | 腐化蜂群；**返回粒子**（可后续控制），失败返回 null | `speed 0..16`，`turnSpeed 0..360` | `swarmParticleFx` |
| `spawnTargetedWisp(level, pos, Entity target, int colorType, shrink, gravity)` D:463 | 飞向实体的调色板 wisp（尺寸固定 0.4F，`PaletteWispParticle.java:366`） | | `wispFX4(w,x,y,z,target,type,shrink,grav)` TC4:457 |
| `spawnInfusedStoneHit(level, state, BlockHitResult, rgb)` D:469 / `spawnInfusedStoneBreak(level, state, pos, rgb)` D:475 | 染色地形碎屑 | | — |
| `spawnSpark(level, pos, float size, EffectColor, float alpha)` D:481 | TC4 `FXSpark`（`ChampionSparkParticle`，`WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_2`） | | `spark(x,y,z,size,r,g,b,a)` TC4:363 |
| `spawnSlimyBubble(level, pos, size, EffectColor)` D:488 | 高级炼金炉气泡 | | — |
| `spawnNitorWisp(level, source, target, size, colorType, shrink, gravity)` D:495 | nitor 风格目的地 wisp | | — |
| `spawnAttractedSparkle(level, Vec3 source, Vec3 lifetimeTarget, Supplier<Vec3> target, int mode)` D:501 | `BoreSparkleParticle`：`mode 0` 绿 `(0.2, 0.6+r*0.3, 0.2)`；`mode 1` 紫 `(0.4+r*0.2, 0.2, 0.6+r*0.3)`；`mode 2` 绿 `(0.2, 0.6+r*0.3, 0.3)`（`D:client/BoreSparkleParticle.java:31-37`），寿命 `≈ dist*3`，重力 0.2，摩擦 0.985 | `mode 0..2` | `drawInfusionParticles3` = mode 1（TC4:782），`drawInfusionParticles4` = mode 2（TC4:789）；目标点 TC4 为 `(x2+0.5, y2−0.5, z2+0.5)` |

一行示例：`ThaumcraftClientEffectsApi.spawnEssentiaTrail(clientLevel, Vec3.atCenterOf(from), Vec3.atCenterOf(to), phase, aspectColor, 1.0F);`

### A.2 `ThaumcraftParticle`（enum，D:api/client/ThaumcraftParticle.java:8-114）与 `spawnParticle` 的 args 语义

`id()` = `thaumcraft:<path>`（D:61），`type()` → `TCParticles.*`（D:65）。经 `spawnParticle(level, p, pos, args)` 或原版 `level.addParticle(p.type(), …)` 生成，args 由 `validProviderArguments`（D:…EffectsApi.java:509-527）约束：

| 常量组 | args (x,y,z) 含义 |
|---|---|
| `WISP_MOTE, SPARKLE, DARK_SPARKLE, ALUMENTUM_MOTE, SCAN_RUNE, INFUSION_RUNE, NODE_JAR_FORMATION, FOCAL_MANIPULATOR_SPARKLE, CRUCIBLE_BUBBLE, ELDRITCH_ARC_SPARKLE, ELDRITCH_LOCK_SPARK` | 三个分量均 ∈ [0,1]（颜色/参数） |
| `TAINT_SWARM` | 不可经此路径（用 `spawnSwarm`） |
| `BORE_SPARKLE, INFUSION_SPARKLE, INFUSION_XP_SPARKLE` | args = **目标点**（≤256 格）；颜色 mode 0/1/2 同 `spawnAttractedSparkle` |
| `DESTINATION_WISP_MAGENTA/BLUE/GREEN/DARK` | args = 位移向量（`lengthSqr ≤ 65536`）；colorType 固定 0/2/3/5（`D:client/TCClientEvents.java:911-914`） |
| `ELDRITCH_WISP, TARGETED_ELDRITCH_WISP` | `x` = 实体 id（非负整数） |
| `WISP_BURST` | `x` = size ∈ (0,1024] |
| `PALETTE_WISP` | `x` = colorType 0..7 整数，`y` = size ∈ (0,1024]，`z` = gravity（`ConfiguredProvider` 固定 shrink=true、noClip=true，`D:client/PaletteWispParticle.java:530-533`） |
| `SHOCK_SPARK_0/1/2` | `x∈[0,1]`，`y∈(0,64]`，`z∈[0,1]` |
| `RUNED_STONE_RUNE` | `x` = duration ∈ [1,1201)，`y,z ∈ [0,1]` |
| `SMOKE_SPIRAL, WARDEN_SMOKE_SPIRAL` | `x` ∈ (0,64] |
| 其余（`TAINT_SPLASH/SPLOOSH/LAND_SPLASH/DROPLET, THAUMIC_SLIME_SPLASH, FROST_SHARD_DEBRIS, ESSENTIA_TRAIL, ADVANCED_FURNACE_BUBBLE, TUBE_VENT, VIS_SPARKLE, LIFTER_SPARKLE, FIRE_BAT_SPARK, CHAMPION_GENERIC, CHAMPION_SPARK, TEMPORARY_HOLE_SPARKLE, WARD_HIT`） | 视为速度，`|v| ≤ 16` |

服务端也可 `serverLevel.sendParticles(ThaumcraftParticle.X.type(), …)`（`SimpleParticleType`），但只有 SimpleParticleType 语义（count/offset/speed），复杂参数仍需自定义包 → 客户端调 Effects API。

### A.3 `ThaumcraftClientVisualsApi`（20732 新增；**在 BER / EntityRenderer / RenderLevelStage 回调里每帧调用**，用调用方的 `PoseStack` 与 `MultiBufferSource`；参数非法抛 IAE）

| 方法（D:api/client/ThaumcraftClientVisualsApi.java 行） | 画什么 / 后端 / RenderType | 参数 |
|---|---|---|
| `renderFocusProjectile(pose, buffers, FocusProjectileVisual kind, int age, int seed, int lifetime, float damage, float partialTick, int packedLight)` :32 | 焦点弹体（EMBER/FIREBALL/FROST/SHOCK_ORB/PRIMAL；`FocusProjectileRenderer`） | `lifetime 1..12000`，`damage 0..640`，`partialTick∈[0,1]` |
| `renderItemHalo(pose, buffers, int age, float growth)` :45 | 特殊物品放射光芒（`SpecialItemRenderer.renderRays`，FAST 5 / FANCY 10 条；RT `specialItemHalo`：POSITION_COLOR TRIANGLES、lightning shader、`LIGHTNING_TRANSPARENCY`、LEQUAL、COLOR_WRITE） | `growth∈[0,1]` |
| `renderInfusionHalo(pose, buffers, int craftCount)` :51 | 注魔矩阵光芒（`InfusionMatrixRenderer.renderHalo`，见 B.4）；**内部自带 `translate(0.5,0.5,0.5)`**，pose 应在方块角 | `craftCount ≥ 0` |
| `renderNodeBubble(pose, buffers, int count, int lockStrength, double animation)` :60 | 节点稳定器泡（`NodeMachineRenderHelper.renderBubble`，`misc/node_bubble.png`，`additiveTranslucentEmissive`，lockStrength==2 时色 `0xFF4444`） | `count 0..37`，`lockStrength 0..2` |
| `renderBloomAura(pose, buffers, float scale, int frame)` :69 | 以太花节点光晕（`misc/nodes.png`） | `scale∈(0,64]` |
| `renderEldritchPortal(pose, buffers, int openingTicks, float partialTick)` :75 | 邪术传送门面（`misc/eldritch_portal.png` 2048×128，`normalTranslucentEmissiveDepthTest`） | |
| `renderCultistPortal(pose, buffers, age, hurtTicks, pulseTicks, healthFraction, partialTick)` :85 | 邪教传送门（`misc/cultist_portal.png`，`normalTranslucentEmissiveDepthWrite`） | |
| `renderWatcherBeam(pose, buffers, Vec3 direction, float charge, float animation, int age)` :98 | 守望者光束（`models/watcher_beam.png`） | `charge∈[0,1]` |
| `renderBoreBeam(pose, buffers, Vec3 start, Vec3 end, int rgb, boolean reverse, double animation)` :110 | 奥术钻光束（`misc/beam_primary.png`/`beam_secondary.png`，`additiveTranslucent`）——TC4 `beamBore` 的逐帧版 | start/end 为 pose 局部坐标，≤256 |
| `renderBoreImpact(pose, buffers, int rgb, double animation)` :119 | 钻头命中闪光（`misc/particles.png`，`excavationImpactNoCutoff`） | |
| `renderBeam(pose, buffers, Vec3 start, Vec3 end, BeamSettings, int age, float partialTick, double animation)` :125 | 与 `spawnBeam` 同几何的逐帧版（`CoordinateBeamEffect.renderLocal`，D:client/CoordinateBeamEffect.java:92-125） | 生命周期由调用方管理 |
| `renderPowerBeam(pose, buffers, start, end, EffectColor, float opacity, float scroll)` :142 / `renderPowerFlare(pose, buffers, EffectColor, opacity, int frame)` :156 | vis 中继能量束/光斑（`VisRelayRenderer`，`misc/beam_primary.png`） | TC4 `beamPower` 的逐帧版 |
| `renderRitualRibbon(pose, buffers, source, target, double age, double animation)` :163 | 邪教仪式飘带（`misc/wispy.png` 512×128，`normalTranslucentEmissiveDepthTestBlurred`） | |
| `renderBoreVortex(pose, buffers, float y, float scale, float spin, float tilt, float alpha)` :176 | 漩涡（`misc/vortex.png`，`normalTranslucentEmissiveDepthWriteBlurred`） | |
| `renderMirrorField(BlockPos, Direction, float partialTick, PoseStack)` :187 | 镜面场（无 buffers 参数，内部自建 RT） | |
| `renderMarker(pose, buffers, MarkerVisual, int rgb)` :192 / `renderMarkerLink(pose, buffers, Vec3 source, Direction face, int color, double animation)` :198 | 傀儡标记（`misc/mark.png`/`home.png`）/ 连线（`misc/script.png`） | `color −1..15` |
| `renderNode(AuraNodeState, pose, buffers, double animation, float opacity)` :211 | 有朝向的节点（`AuraNodeRenderer.renderOrientedNode`，帧 `floorMod((int)(animation*1.25),32)`） | `opacity∈[0,1]` |
| `renderInventoryNode(AuraNodeState, pose, buffers, long animation)` :219 | GUI/物品里的节点（`frameTime = floorMod(animation,128)*5/4`） | |

示例（BER 内）：`ThaumcraftClientVisualsApi.renderInfusionHalo(poseStack, buffers, be.craftCount());`

### A.4 `ThaumcraftClientRenderApi(Common)`（D:api/client/ThaumcraftClientRenderApiCommon.java；S 同名文件）

| 方法 | D 行 / S 行 | 说明 |
|---|---|---|
| `EffectUv particleAtlasUv(float u0, float u1, float v0, float v1)` | 34 / 42 | **把 TC4 原 256² `particles.png` 的归一化 UV 换算到 TC4R 打包后的 200×440 `particles.png`**（`EffectAtlases.PARTICLES`）。凡是照抄 TC4 `particles.png` UV（如 `particle%16/16f`）的代码必须经过它 |
| `EffectUv effectAtlasUv(EffectSpriteSheet, u0, u1, v0, v1)` | 39 / — | 同上，支持 `PARTICLES`(200×440)、`LARGE_PARTICLES`(同图 y≥192 的 32px 格)、`HARNESS`、`SPARKS` 与 `FIREBALLS`(`particles_secondary.png` 208×584，源 512²)、`NODES`(`nodes.png` 1088²，源 2048²) |
| `boolean isFocusDialAtBottom()` | 56 / 50 | 客户端配置 |
| `drawAspect(GuiGraphics, AspectId, int x, int y, float alpha)` | 60 / 57 | GUI 画 16×16 要素图标（32² 贴图缩放，乘要素色，`SRC_ALPHA, ONE_MINUS_SRC_ALPHA`；不画数量），`D:client/AspectGuiRender.java:13-50` |
| `RenderType legacyLinearAdditiveBillboard(ResourceLocation tex)` | 64 / 64 | `entity_translucent_emissive` shader，线性过滤(blur)，`LIGHTNING_TRANSPARENCY`(SRC_ALPHA, ONE)，NO_CULL，COLOR_WRITE（不写深度），LEQUAL，OVERLAY，无 lightmap（`D:client/WispRenderTypes.java` ADDITIVE_TRANSLUCENT_EMISSIVE_BLURRED）≈ TC4 `glBlendFunc(770,1)+glDepthMask(false)+全亮` |
| `RenderType legacyLinearTranslucentBillboard(tex)` | 68 / 71 | 同上但 `TRANSLUCENT_TRANSPARENCY`（`normalTranslucentEmissiveDepthTestBlurred`）≈ TC4 `(770,771)` 不写深度、全亮 |
| `RenderType legacyLinearTranslucentDepthWriteBillboard(tex)` | 72 / 78 | `entity_translucent` shader（**受光照/lightmap**），blur，TRANSLUCENT，NO_CULL，写深度，LIGHTMAP，OVERLAY |
| `createEldritchOrbRenderer(EntityRendererProvider.Context)` | 76 / 87 | 现成邪术球实体渲染器 |
| `RenderType eldritchOrbRays()` | 80 / 94 | POSITION_COLOR、TRIANGLES、lightning shader、`LIGHTNING_TRANSPARENCY`、LEQUAL、COLOR_WRITE、按上传排序——**TC4 “放射光芒/halo” 三角扇的通用 RT**（顶点只要 `vertex().color().endVertex()`） |
| `LivingEntity currentHeldItemEntity()` | 85 / 110 | BEWLR 渲染手持物时取持有者（TC4R mixin 压栈 `HeldItemRenderContext`） |
| `WorldRenderScope beginAfterLevelWorldRender(WorldRenderContext, boolean depthTested)` / `(Matrix4f modelView, Matrix4f projection, boolean depthTested)` | 89,98 / 129,143 | 仅 AFTER_LEVEL 阶段；设置矩阵与深度状态，`close()` 恢复 `depthMask(true)+enableDepthTest`。Forge 侧可传 `WorldRenderTransforms.modelViewMatrix()` 与 `event.getProjectionMatrix()`（TC4R 自己就这么构造，`D:client/TCClientBootstrap.java:699-701`） |
| `renderLegacyFramedWand / renderLegacyDirectEntityWand(stack, pose, buffers, light, overlay)` | 107,112 / 165,177 | 画 TC4 法杖 |
| `renderInstalledFocus(stack, boolean staffScale, pose, animationTick, light, overlay)` | 117 / 188 | 只画焦点 |
| `Vec3 resolveFocusCastOrigin(LivingEntity, InteractionHand)` | 127 / 202 | 法杖尖端世界坐标 |
| `showFocusBeam(ResourceLocation effectId, LivingEntity, hand, Vec3 target, int rgb, float endScale, int impactTicks[, BeamTexture, boolean reverse])` | 131,143 / 214 | 持续焦点光束（TC4 `beamCont`）；`endScale 0..4`，`impactTicks 0..5` |
| `record EffectUv(u0,u1,v0,v1)` / `interface WorldRenderScope extends AutoCloseable` | 184 / 192 | |

### A.5 值类型（全部 20732 新增）

| 类型 | D 位置 | 字段/常量/约束 |
|---|---|---|
| `record EffectColor(float red, green, blue)` | api/client/EffectColor.java:3 | 分量 ∈[0,1]；`EffectColor.rgb(int)` :14 |
| `record BeamSettings(BeamTexture texture, EffectColor color, int lifetime, float width, float endScale, boolean reverse, boolean pulse, int rotationSpeed, boolean additive)` | BeamSettings.java:5-35 | `lifetime 1..1200`，`width (0,64]`，`endScale [0,64]`，`rotationSpeed 1..360`。几何（CoordinateBeamEffect.renderLocal:92-125）：3 个面绕轴每 60° 一片；半宽 `0.15*size`，末端乘 `endScale`；`pulse` 时 `size=min(age/4, width)`、最后 4 tick opacity `0.4−(4−remaining)*0.1`，否则 opacity 0.4；V 滚动 `(reverse?anim:−anim)*0.2 % 1`；旋转 `rotationClock%360*rotationSpeed`；顶点 light 200；RT `additive ? additiveTranslucentEmissiveDepthTest : normalTranslucentEmissiveDepthTest`。TC4 `FXBeam` 默认：`pulse=true`、`rotationspeed=5`、`blendmode=1`（加色）、op 0.4（TC4:client/fx/beams/FXBeam.java:54-61,216-273） |
| `enum BeamTexture {DEFAULT("beam"), PRIMARY("beam_primary"), SECONDARY("beam_secondary"), TERTIARY("beam_tertiary")}` | BeamTexture.java:6-20 | `texture()` → `thaumcraft:textures/misc/<path>.png`；对应 TC4 `FXBeam.type` 0/1/2/3（`beam/beam1/beam2/beam3.png`） |
| `record LightningSettings(int duration, double multiplier, int increment, double width, EffectColor outer, EffectColor inner, boolean additive)` | LightningSettings.java:5-36 | `NODE=(10,4.0,5,0.03,(.6,.3,.6),(1,.6,1),true)`，`FOCUS=(6,0.5,8,0.125,(.1,.1,.6),(.1,.1,1),true)`；`duration 0..1200`，`multiplier 0..64`，`increment 1..256`，`width (0,64]` |
| `record ParticleCameraFade(float hiddenDistance, float fullSizeDistance)` | ParticleCameraFade.java:3-23 | `NONE=(0,0)`；`visibility(depth)=clamp((depth−hidden)/(full−hidden))` |
| `enum EffectSpriteSheet {PARTICLES, LARGE_PARTICLES, HARNESS, SPARKS, FIREBALLS, NODES}` | EffectSpriteSheet.java:3 | 见 A.4 `effectAtlasUv` |
| `enum MarkerVisual {MARK, HOME, EMPTY}` / `enum ScreenEffect {DEATH_GAZE, BLURRED_VISION, UNNATURAL_HUNGER, SUN_SCORNED}` / `enum FocusProjectileVisual {EMBER, FIREBALL, FROST, SHOCK_ORB, PRIMAL}` | 各自 :3 | |

### A.6 `ThaumcraftClientFeedbackApi`（20732 新增，客户端，D:api/client/ThaumcraftClientFeedbackApi.java）

| 方法 | 行 | 说明 |
|---|---|---|
| `showScreenEffect(ScreenEffect, int ticks)` | 27 | 后处理屏幕效果（`WarpMobEffectPostChainManager`，`shaders/post/*.json`），`ticks 1..72000` |
| `showWarpVignette()` / `showEldritchMist(boolean prolonged)` | 36 / 40 | 扭曲暗角 / 邪术雾 |
| `showNotification(Component, Optional<AspectId>, int rgb)` | 48 | 左上角通知 |
| `showResearchCompletion(ResearchKey)` / `showResearchDiscovery()` | 52 / 56 | 研究弹窗/音效 |
| `showAspectGain(AspectId, int amount, boolean discovered, int visualCount)` | 60 | 获得要素动画（`visualCount 0..64`） |
| `playWarpWhispers()` | 89 | 本地播放 `whispers`（0.5, 1.0） |

### A.7 管线钩子（客户端；多为 TC4R 内部 mixin 调度，附属 mod 通常只注册不调度）

| 类/成员 | D 行 / S 行 | 用途与用法 |
|---|---|---|
| `ParticlePipeline.register(ToIntFunction<ParticleRenderType> order, BiConsumer<ParticleRenderType,TextureManager> before, Runnable after)` | 19 / 22 | 给自定义 `ParticleRenderType` 排序（返回 ≥0 的 rank 参与排序，−1 不管）并在每个粒子桶前后插状态。`prepare/finish/comparator` 由 `ParticleBucketMixin`/`ParticleOrderMixin` 调用，勿手调。TC4R 自身 rank：`LEGACY_EFFECT_ADDITIVE 0, NODE_ADDITIVE 1, TRANSLUCENT 2, WORLD_LAST_ADDITIVE_LAYER_0 10, LEGACY_ATLAS_ADDITIVE_0 11, TRANSLUCENT_1 20/21, BLOCK_TRANSLUCENT_1 22, ELDRITCH_ORB_CORE 23, ADDITIVE_2 30/31/32, TRANSLUCENT_3 40`（`D:client/ParticleSheets.java:159-187`） |
| `interface QueuedParticleType extends ParticleRenderType` | 5 / — | 标记“延迟到 TC4R 自管 pass 渲染”的粒子层（TC4R 内部 `NativeParticleSheetPlatform.queued`） |
| `record WorldRenderContext(Stage getStage, PoseStack getPoseStack, Matrix4f getModelViewMatrix, Matrix4f getProjectionMatrix, float getPartialTick, Camera getCamera, Frustum getFrustum)`；`Stage {AFTER_BLOCK_ENTITIES, AFTER_TRANSLUCENT_BLOCKS, AFTER_PARTICLES, AFTER_WEATHER, AFTER_LEVEL, OTHER}` | 8-24 / 14 | TC4R 由 Forge `RenderLevelStageEvent` 转换（`TCClientBootstrap.java:684-713`，pose 为**新的单位 PoseStack**，modelView = `WorldRenderTransforms.modelViewMatrix()`）。公开 record，附属 mod 可自行 new；没有公开的订阅入口 |
| `WorldRenderBoundary.beforeTranslucent(Runnable)` | 12 / 15 | 在 LevelRenderer 画半透明方块前（含深度快照前）回调（`WorldRenderBoundaryMixin`）；用于必须在 translucent 之前写入的特效 |
| `WorldRenderTransforms.capture/modelViewMatrix/cameraRelativeLight/cameraRelativePose/resetWorldPose` | 19/27/31/35,41/46 / 29/37/41/48,57/65 | `renderLevel` HEAD 处捕获视图矩阵；`cameraRelativePose(pose)` 把 BER 的 pose（含视图旋转）转成“相机相对、无视图旋转”的 pose，用于 BER 里排队、之后在 world-stage 回放（TC4R 的矩阵/节点都这么做） |
| `RenderTypeBlendPlatform.enter(boolean)/restore(boolean)/renderTypeOwnsBlend()` | 9/15/19 / 13/19/23 | 为 true 时 `ForgeShaderBlendMixin` 跳过 `ShaderInstance.apply()` 里 shader JSON 的 `BlendMode.apply()`，让 RenderType 的 transparency shard 生效。见 D.5“混合覆盖”风险 |
| `HudHeartHooks.register(Supplier<? extends HudHeartPass>)`；`HudHeartPass{begin(float max,int absorption); beforeHeart(GuiGraphics,int x,int y); finish(GuiGraphics)}` | 14 / 15；HudHeartPass 5-10 | 每颗心绘制前插画（TC4R 用于符文护盾心，`RunicHeartPass`） |
| `ClothingLayerHooks.register(Interceptor)`；`Interceptor.render(int pass, EntityModel<T>, ResourceLocation tex, PoseStack, MultiBufferSource, int light, T entity, int color, Runnable original)` | 17,57 / 25,15 | 村民服装层拦截（链式，必须调 `original.run()` 才继续） |
| `ClientBehaviorHooks.CAPE / BIOME_COLOR / HUMANOID_POSE / HAND_LAYER / EQUIP_RESET / CONTINUOUS_FLIGHT / BEFORE_DRAW` | 19-25 | `OrderedCallbacks`：披风显示、群系色、人形姿势、手持层、换物动画、连续飞行、绘制前 |
| `HeldItemRenderContext.push/pop/currentEntity` | 14/18/30 | 见 A.4 `currentHeldItemEntity` |
| `interface NativeItemClientExtensions extends IClientItemExtensions` | 11-27 | 附加 `armorLayerTint`、`animateArmor`；可直接当 `IClientItemExtensions` 用 |
| `WandRenderApi.getArmPose(LivingEntity, InteractionHand, ItemStack)` | 12 / 22 | 使用中返回 `BOW_AND_ARROW`，否则 null |
| `ResearchRecipeRendererApi.register(ResourceLocation id, Class<R> recipeClass, BiConsumer<R, ResearchRecipeRenderContext>) → Registration`；`render(Recipe, ctx)` | 17,35 / 27,43 | 研究书配方页自定义绘制；ctx 提供 `graphics() font() x() y() output() item(stack,x,y,bool) ingredient(ing,x,y) isHovered(x,y,w,h)`（ResearchRecipeRenderContext.java:8-23） |

### A.8 `api/presentation` 与动作演出

| 方法 | D 行 / S 行 | 侧 | 说明 |
|---|---|---|---|
| `WorldEffectsApi.blockSparkle(ServerLevel, BlockPos, double range, int color, int baseCount)` | 17 / 16 | **服务端**（必须在 server 线程） | 向方块中心 `range` 格内玩家发 `BlockSparklePayload` → 客户端 `spawnBlockSparkle`。TC4 `PacketFXBlockSparkle` 等价；`color=-9999` 随机色 |
| `ActionPresentationApi.present(Player, hand, ResourceLocation id, Vec3 origin[, Vec3 target, Entity targetEntity, double detail, int value[, ActionPresentationAudience]])` | 17,21,27 / 27,35,43 | 双端都调 | 客户端侧：audience 可预测且 source 是本地玩家 → 立即本地演出；服务端侧：`sendsConfirmation` 时发给 origin 64 格内玩家（`CONFIRMED_ALL` 含 source，其余排除 source）（`D:common/ActionPresentationService.java:35-44,120-122`） |
| `ActionPresentationApi.presentAt(ServerLevel, id, origin, target, targetEntity, detail, value)` | 53 / 52 | 服务端 | 无 source 的广播，64 格 |
| `ActionPresentationAudience {NONE, LOCAL_ONLY, PREDICTED, CONFIRMED_ALL, CONFIRMED_OBSERVERS}` | 3-16 | | `predictsLocally()= PREDICTED|LOCAL_ONLY`；`sendsConfirmation()= PREDICTED|CONFIRMED_*` |
| `ThaumcraftClientPresentationApi.register(ResourceLocation id, ActionPresentationHandler) → Registration` | 12 / 20 | 客户端 | 注册演出处理器；`ActionPresentationHandler.present(ActionPresentationContext)`；context 字段 `presentationId, level(ClientLevel), source(@Nullable), hand, sourcePosition, origin, target, Optional<Entity> targetEntity, detail, value, seed, predicted`（ActionPresentationContext.java:13-25） |

用法：服务端事件 → `presentAt(level, isorropia:vat_zap, from, to, null, 0, color)`；客户端在 `FMLClientSetupEvent` 里 `register(isorropia:vat_zap, ctx -> ThaumcraftClientEffectsApi.spawnArc(ctx.level(), ctx.origin(), ctx.target(), EffectColor.rgb(ctx.value()), 1F))`。这是“服务端触发、客户端画”且无需自建网络包的首选通道。

### A.9 非 API 但 `public` 的可复用件（锁定 20732 时可用；升级 TC4R 需复查）

| 类 | D 位置 | 可复用内容 |
|---|---|---|
| `client.ParticleSheets`（public final） | client/ParticleSheets.java:17-188 | 公开 `ParticleRenderType`：`LEGACY_EFFECT_ADDITIVE`（原版粒子图集，`SRC_ALPHA, ONE`，depthMask false）、`LEGACY_EFFECT_TRANSLUCENT`（`SRC_ALPHA, ONE_MINUS_SRC_ALPHA`）、`LEGACY_EFFECT_NODE_ADDITIVE`（绑 `misc/nodes.png`）、`WORLD_LAST_ADDITIVE_LAYER_0/2`、`WORLD_LAST_TRANSLUCENT_LAYER_1/3`（原版图集，雾起点设为 MAX → 不受雾）、`WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`/`TRANSLUCENT_LAYER_1`（绑**打包后的** `misc/particles.png`，线性过滤）、`WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_2`（绑 `particles_secondary.png`）、`WORLD_LAST_BLOCK_TRANSLUCENT_LAYER_1`/`ADDITIVE_LAYER_2`（绑方块图集）。共同状态：particle shader、`blendFunc(SRC_ALPHA, dst)`、`depthMask(false)`（`particleSheet :105-125`）|
| `client.TCParticleRenderTypes`（public） | client/TCParticleRenderTypes.java:30-45 | 同上字段的别名 + `PRE_TRANSLUCENT_WARD_HIT_ADDITIVE` |
| `nativeimpl.NativeRenderTypes.create(String, VertexFormat, Mode, int size, [boolean crumbling, boolean sorted,] CompositeState)` | nativeimpl/NativeRenderTypes.java:26-33 | 包一层 `OwnedBlendType`，`end()` 时 `RenderTypeBlendPlatform.enter(true)`（:82-95）→ 自定义 RT 的混合函数不被 shader JSON 覆盖 |
| `nativeimpl.NativeRenderStates`（public static final） | nativeimpl/NativeRenderStates.java:16-40 | 原版 protected shard 的公开别名：`LIGHTNING_TRANSPARENCY`、`ADDITIVE_TRANSPARENCY`、`TRANSLUCENT_TRANSPARENCY`、`NO_TRANSPARENCY`、`COLOR_WRITE`、`COLOR_DEPTH_WRITE`、`CULL`、`NO_CULL`、`LEQUAL_DEPTH_TEST`、`NO_DEPTH_TEST`、`LIGHTMAP`、`OVERLAY`、`POLYGON_OFFSET_LAYERING`、`VIEW_OFFSET_Z_LAYERING`、`POSITION_COLOR_SHADER`、`RENDERTYPE_EYES_SHADER`、`RENDERTYPE_LIGHTNING_SHADER`、`RENDERTYPE_ENTITY_TRANSLUCENT(_EMISSIVE)_SHADER`、`RENDERTYPE_ENTITY_CUTOUT(_NO_CULL)_SHADER`、`RENDERTYPE_ENTITY_NO_OUTLINE_SHADER`、`RENDERTYPE_ENTITY_SOLID_SHADER`、`RENDERTYPE_END_PORTAL_SHADER`、`WINDOW_LINE_WIDTH` |
| `nativeimpl.NativeBlockEntityRenderer<T>` | nativeimpl/NativeBlockEntityRenderer.java:7-11 | 只给 BER 加了 `default AABB getRenderBoundingBox(T)`；**Forge 47 不会调用它**，TC4R 是在 BE 的 `getRenderBoundingBox()` 里转调（如 `AlchemyFurnaceBlockEntity.java:536-546`）。附属 mod 直接在 BE 覆写 Forge 的 `getRenderBoundingBox()` 即可 |
| `client.AddonRenderTypeAccess` | client/AddonRenderTypeAccess.java:10-24 | A.4 三个 billboard RT 的实现入口 |
| `client.EffectAtlases` + `nativeimpl.PackedAtlasLayout` | client/EffectAtlases.java:6-11,531-532,916-917；nativeimpl/PackedAtlasLayout.java | 打包图集网格（PARTICLES 源 256²→200×440 16px 格；SPARKS/FIREBALLS 源 512²→208×584；NODES 源 2048²→1088² 64px 格 32×32）；优先用 A.4 的 `particleAtlasUv/effectAtlasUv` |
| `client.GogglesAspectView.resolve(BlockEntity)` | client/GogglesAspectView.java:14-27 | 护目镜要显示的要素列表 |
| `api.aspect.AspectApi.get(AspectId)` → `AspectDefinition{color, texture, blendMode, components…}` | api/aspect/AspectApi.java:33；AspectDefinition.java:10-18 | **要素颜色/图标的正式 API**（`AspectClientRenderState`/`AspectRenderIdentity` 是包私有，不可用）；`AspectBlendMode {ADDITIVE, NORMAL}` 对应 TC4 `Aspect.getBlend()` 1/771 |
| `api.aspect.AspectContainerView`、`api.item.GogglesOverlayGear`、`api.item.AuraRevealingGear` | AspectContainerView.java:6-16 | BE 实现 `AspectContainerView` → 戴护目镜看它时**自动**出现 TC4 风格要素浮标（B.9）；物品实现 `GogglesOverlayGear.showsThaumcraftGogglesOverlay(stack, player)` → 视为护目镜；`AuraRevealingGear.revealsThaumcraftAura` → 显示节点 |

**包私有（不可直接调用，只能照抄状态）**：`TCClientRenderTypes`、`WispRenderTypes`、`AspectClientRenderState`、`AspectRenderIdentity`、`ItemFoilRendering`、`InfusionMatrixModel`、`NamedGroupObjMesh`、`GogglesTrackedBlockEntityRenderer`、`JarClientExtensions`。

### A.10 TC4 / TC6 特效调用 → TC4R 对照（迁移速查）

| 上游调用（TC4 `Thaumcraft.proxy.*` / TC6 `FXDispatcher.INSTANCE.*`） | TC4R 替代 | 备注 |
|---|---|---|
| `blockSparkle(w,x,y,z,c,count)` | 客户端 `spawnBlockSparkle(level,pos,c,count)`；服务端 `WorldEffectsApi.blockSparkle(sl,pos,range,c,count)` | |
| `sparkle(x,y,z,size,color,grav)` / `sparkle(x,y,z,color)` | `spawnQualitySparkle(level,pos,size,color,grav)` / `(…,1.5F,color,0F)` | |
| `spark(x,y,z,size,r,g,b,a)` | `spawnSpark(level,pos,size,new EffectColor(r,g,b),a)` | |
| `wispFX(w,x,y,z,size,r,g,b)` | `spawnWispMote(level,pos,size,r,g,b,false,0.02F)` | |
| `wispFX2(w,x,y,z,size,type,shrink,clip,grav)` | `spawnMovingWisp(level,pos,Vec3.ZERO,size,type,shrink,grav,clip)`，或 `spawnParticle(PALETTE_WISP,pos,(type,size,grav))`（固定 shrink/noClip=true） | 初速需对照 TC4 FXWisp:60 |
| `wispFX3(w,x,y,z,x2,y2,z2,size,type,shrink,grav)` | `spawnDestinationWisp(level,pos,target,size,type,shrink,grav)` | |
| `wispFX4(w,x,y,z,entity,type,shrink,grav)` | `spawnTargetedWisp(level,pos,entity,type,shrink,grav)` | 尺寸固定 0.4 |
| `wispFXEG(w,x,y,z,entity)` | `spawnParticle(level, ELDRITCH_WISP/TARGETED_ELDRITCH_WISP, pos, (entityId,0,0))` | |
| `burst(w,x,y,z,size)` | `spawnBurst(level,pos,size)` | |
| `essentiaTrailFx(w,x,y,z,x2,y2,z2,count,color,scale)` | `spawnEssentiaTrail(level, (x+.5,y+.5,z+.5), (x2+.5,…), count, color, scale)` | 需自己 +0.5 |
| `blockRunes(w,x,y,z,r,g,b,dur,grav)` | `spawnBlockRune(level,(x,y,z),r,g,b,dur,grav)` | API 内部 +0.5；`dur ≤ 400` |
| `arcLightning(w,x,y,z,tx,ty,tz,r,g,b,h)` | `spawnArc(level,start,end,new EffectColor(r,g,b),h)` | 终点 sparkle 已包含 |
| `nodeBolt(w,x,y,z,x2,y2,z2)` / `bolt(w,src,tgt)` | `spawnNodeLightning` / `spawnLightning(level,a,b,LightningSettings.NODE 或自定义)` | |
| `beam(w,sx,sy,sz,tx,ty,tz,type,color,reverse,endmod,age)` | `spawnBeam(level,start,end,new BeamSettings(BeamTexture.values()[type], EffectColor.rgb(color), age, width, endmod, reverse, true, 5, true))` | `width` 见 E 风险 #6 |
| `beamCont(...)`（法杖持续） | `ThaumcraftClientRenderApi.showFocusBeam(...)` | |
| `beamBore(...)` / `beamPower(...)`（返回句柄、每 tick 更新） | 逐帧：`ThaumcraftClientVisualsApi.renderBoreBeam` / `renderPowerBeam`（在 BER/EntityRenderer 里画）；或自走：`spawnBeam(level,start,entity,settings)` | 无“句柄”式 API |
| `drawInfusionParticles1(item)` / `2(block)` | `spawnInfusionItem(level,src,(x2+.5,y2−.5,z2+.5),stack)` / `spawnInfusionBlock(level,src,tgt,state,pos,face)` | |
| `drawInfusionParticles3` / `4` | `spawnAttractedSparkle(level,src,tgt,()->tgt,1)` / `(…,2)`，或 `spawnParticle(INFUSION_SPARKLE / INFUSION_XP_SPARKLE, src, tgt)` | |
| `drawGenericParticles(...)`（TC4 `FXGeneric`，TC6 `FXGeneric`） | `spawnGenericParticle(...)`（参数一一对应） | TC6 FXGeneric 的 builder 字段需按含义映射 |
| `drawVentParticles(...)` | `spawnVent(level,pos,heading,rgb,scale)` | TC4 默认 scale 需对照 FXVent |
| `crucibleBubble/Froth/FrothDown`、`smokeSpiral`、`dropletFX`、`hungryNodeFX`、`boreDigFx` | `spawnBubble/spawnFroth/spawnSmokeSpiral/spawnDroplet/spawnHungryNodeFragment/spawnBoreFragment` | |
| TC4/TC6 `FXSonic`（hemis + ripple） | `spawnSonicWave(LivingEntity, baseAge)` | 只支持以实体头部朝向发出 |
| TC6 `scanHighlight(...)` | 无直接 API；近似 `spawnParticle(SCAN_RUNE, pos, rgb)` | |
| `soulTrail`、`sourceStreamFX`（`FXWispArcing`） | 无 | 需自写粒子 |

---

## B. TC4R 自身如何渲染（Isorropia 要“长得像”的对象）

### B.1 RenderType / 混合状态目录

**原版 transparency shard 的真实混合函数**（javap `RenderStateShard` 静态 lambda 核实）：

| shard | 函数 | 对应 TC4 GL |
|---|---|---|
| `TRANSLUCENT_TRANSPARENCY` | `blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ONE_MINUS_SRC_ALPHA)` | `glBlendFunc(770, 771)` |
| `LIGHTNING_TRANSPARENCY` | `blendFunc(SRC_ALPHA, ONE)` | `glBlendFunc(770, 1)`（TC4 几乎所有发光特效） |
| `ADDITIVE_TRANSPARENCY` | `blendFunc(ONE, ONE)` | `glBlendFunc(1, 1)` |
| `GLINT_TRANSPARENCY` | `(SRC_COLOR, ONE, ZERO, ONE)` | — |
| `CRUMBLING_TRANSPARENCY` | `(DST_COLOR, SRC_COLOR, ONE, ZERO)` | — |

`CompositeState.builder()` 默认：`NO_TRANSPARENCY`、`LEQUAL_DEPTH_TEST`、`CULL`、`COLOR_DEPTH_WRITE`、`NO_LIGHTMAP`、`NO_OVERLAY`、`MAIN_TARGET`。

**原版 shader 的 alpha 丢弃与光照**（MC:shaders/core 核实）：

| shader | 丢弃条件 | 光照 |
|---|---|---|
| `rendertype_entity_translucent` / `_cutout` / `_cutout_no_cull` | **贴图** `alpha < 0.1`（乘顶点色之前） | 法线方向光 + lightmap |
| `rendertype_entity_translucent_emissive` | 贴图 `alpha < 0.1` | 法线方向光，无 lightmap（全亮） |
| `particle` | **贴图×顶点色** `alpha < 0.1` | lightmap |
| `rendertype_eyes` | 不丢弃 | 无（`vertexColor = Color`），雾里整体淡出 |
| `rendertype_entity_no_outline` | 不丢弃 | 方向光 × lightmap |
| `rendertype_lightning` | 不丢弃（无贴图） | 无 |

→ TC4 的 `glAlphaFunc(516, 0.003921569F)`（几乎不裁剪）只能用 eyes / no_outline / lightning shader 复现；TC4R 的“NoCutoff”系列就是 `RENDERTYPE_EYES_SHADER` + 自定 transparency（`D:client/WispRenderTypes.java:59-78`）。

**TC4R 内部 RT（包私有，照抄状态即可）**——`D:client/TCClientRenderTypes.java`：

| 名称（行） | 格式/模式 | shader | 贴图(blur,mip) | transparency | depth | write | cull | lightmap | 用途 |
|---|---|---|---|---|---|---|---|---|---|
| `infusionHalo` :46 | POSITION_COLOR / TRIANGLES，sorted | lightning | — | LIGHTNING | LEQUAL | COLOR | **CULL** | — | 注魔光芒 |
| `specialItemHalo` / `eldritchOrbRays` :61/:75 | 同上 | lightning | — | LIGHTNING | LEQUAL | COLOR | 默认 CULL | — | 物品光芒 |
| `additiveTranslucent(tex)` :115 | NEW_ENTITY / QUADS，crumbling | entity_translucent | (F,F) | LIGHTNING | 默认 | COLOR | NO_CULL | LIGHTMAP | 钻头光束等 |
| `infusionMatrixGlow(tex)` :179 → `legacyUnlitTranslucentNoCutoff(…, additive=T, blur=F, mip=F, depthTest=T, cull=T)` | NEW_ENTITY / QUADS，sorted | **eyes** | (F,F) | LIGHTNING | LEQUAL | COLOR | CULL | — | 矩阵外层发光 |
| `normal/additiveTranslucentEmissiveDepthTest[Blurred]` :256 | NEW_ENTITY | entity_translucent_emissive | (blur,F) | TRANSLUCENT / LIGHTNING | LEQUAL | COLOR | NO_CULL | — | 光束、传送门 |
| `…EmissiveNoDepth[Billboard]` :276 | NEW_ENTITY | entity_translucent_emissive | (blur,F) | 同上 | **NO_DEPTH** | COLOR | NO_CULL | — | 护目镜要素浮标 |
| `translucentDepthWrite(tex, additive, blur, sorted, crumbling)` :296 | NEW_ENTITY | entity_translucent | (blur,F) | 同上 | 默认 | 默认(深度写) | NO_CULL | LIGHTMAP | 实体半透明壳 |
| `legacyNodeTranslucent(tex, additive, blur, depthTest)` :610 | NEW_ENTITY | entity_no_outline | (blur,F) | 同上 | LEQUAL/NO | COLOR | NO_CULL | LIGHTMAP | 节点层 |
| `legacyLightmapped{Translucent,Billboard}NoCutoff` :348-381 | NEW_ENTITY | entity_no_outline | 参数 | 参数 | 参数 | COLOR | 参数 | LIGHTMAP | 受光照但不裁剪 |
| `legacyEyes{ColorOnly,DepthWrite}` :404-429 | NEW_ENTITY | eyes | (F,F) | **ADDITIVE(1,1)** | LEQUAL | COLOR / COLOR_DEPTH | NO_CULL | — | 眼睛发光层 |
| `thaumometerAspect(tex)` :232 | NEW_ENTITY | entity_translucent | (F,F) | LIGHTNING | NO_DEPTH | COLOR | NO_CULL | LIGHTMAP | 扫描仪要素 |

### B.2 源质罐 `warded_jar`（方块模型 + BER 液体/标签 + BEWLR 物品）

**方块外壳**：静态 JSON（`JAR assets/thaumcraft/models/block/warded_jar.json`）：`"render_type": "minecraft:translucent"`，`ambientocclusion:false`，两个元素 `[3,0,3]→[13,12,13]`（罐身）与 `[5,12,5]→[11,14,11]`（盖），贴图 `block/jar_side`、`jar_top`、`jar_bottom`（16²）。四个 facing 共用同一模型（blockstate 不旋转）。

**液体**（`D:client/WardedJarRenderer.java:66-88`，TC4 对照 `TC4:client/renderers/tile/TileJarRenderer.java:101-134`，数值一致）：
- 精灵 `thaumcraft:block/animatedglow`（方块图集；16×320 = 20 帧，`frametime 2`，帧序 0→19→1 往返）。
- 盒子 `x,z ∈ [0.25, 0.75]`，`y ∈ [0.0625, 0.0625 + min(1, amount/capacity) * 0.625]`，6 面，UV 每面整张精灵 `(u0,v0)-(u1,v1)`。
- 顶点色 = 要素色 RGB，alpha 255；light = `max(200, packedLight)`（TC4：`max(200, 方块亮度)`）；overlay NO_OVERLAY；法线按面。
- RenderType = `RenderType.solid()`（方块图集，经 BER buffer）。
- 无 ClientLevel 需求；BE 需同步 `aspect / amount / capacity / filter / FACING`。

**标签**（`WardedJarRenderer.java:90-139`；TC4：`TileJarRenderer.java:50-84`）：
1. `push`；`translate(0.5, 0.41, 0.5)`；按 facing 绕 `NORTH→ZN, SOUTH→ZP, WEST→XN, EAST→XP` 旋转 `rot` 度；`translate(−0.5, −0.41, −0.5)`。
   `rot = (filter.serialized().hashCode() + blockX + legacyFacing) % 4 − 2`，`legacyFacing N=2 S=3 W=4 E=5`（TC4 用 `Config.crooked` 开关，TC4R 恒开）。
2. 纸签：`RenderType.entityCutoutNoCull(thaumcraft:textures/models/label.png)`（16²），水平 `[0.25,0.75]`、竖直 `[0.16,0.66]`，贴在距该面 `0.187` 的平面（SOUTH/EAST 为 `1−0.187`），白色 alpha 255，light `15728880`。
3. 要素图标：`RenderType.entityTranslucentEmissive(aspectTexture, false)`，水平 `[0.332,0.668]`、竖直 `[0.242,0.578]`，平面 `0.186`，U 翻转，颜色 `0x191919`、alpha 204（TC4 “单色标签”），light `15728880`。
4. 每面 4 顶点顺序与法线见 `emitFacingQuad :157-203`。

**物品**（`D:client/WardedJarItemRenderer.java:38-82`）：BEWLR；`ItemFoilRendering.withFoil` 处理附魔光；读 `TCItemState.ASPECTS`（恰 1 种要素才画液体）、`FilledJarEssentiaApi.capacity(stack,true)`、`TCItemState.JAR_FILTER`；facing 固定 `EAST`；先 `renderContents`（液体+标签）再 `blockRenderer.renderSingleBlock(state(FACING=EAST), …, ModelData.EMPTY, null)` 画玻璃壳。物品模型 `parent: builtin/entity`，display 与 B.11 的 `tc4_block_entity` 完全相同。

**破坏粒子**：`JarClientExtensions.addDestroyEffects → true`（不出原版碎块，`D:client/JarClientExtensions.java:15-17`）。

### B.3 脑罐 `brain_jar`（罐内悬浮物 + 盐水的参考实现）

`D:client/BrainJarRenderer.java:34-85`（TC4：`TileJarRenderer.java:136-155`）：`translate(0.5, 0.01, 0.5)` → `rotX(180)` → `bob = sin(playerTicks/14)*0.03 + 0.03` → `translate(0, −0.8+bob, 0)` → `rotY(插值 rotation 弧度)` → `rotY(−90°)` → `scale(0.4)` → 脑模型 `RenderType.entityCutoutNoCull(thaumcraft:textures/models/brain2.png)`（256×128）。盐水用方块图集精灵 `thaumcraft:models/jarbrine`（64×32，见 C.1：`textures/models/` 被拼进方块图集），RT `NativeRenderTypes.getEntityRenderType(RenderType.translucent(), false)`。方块模型 `loader: thaumcraft:decorated_block, decoration: brain_jar`（TC4R 内部 loader，附属 mod 不可复用其 decoration 名）。

### B.4 注魔矩阵 `runic_matrix` BER（转动立方 + 发光层 + 光芒）

模型 `InfusionMatrixModel`（`D:client/InfusionMatrixModel.java:22-31`）：`LayerDefinition 64×64`；`base` = `texOffs(0,0).addBox(−8,−8,−8, 16,16,16)`；`overlay` = `texOffs(0,32)` 同尺寸；贴图 `thaumcraft:textures/models/infuser.png`（64²）。TC4 原版：`TC4:client/renderers/tile/TileRunicMatrixRenderer.java:82-171`（数值逐项一致）。

变换（`InfusionMatrixRenderer.java:229-258`）：`ticks = cameraEntity.tickCount + partialTick`；
1. `translate(0.5, 0.5, 0.5)`；`rotY((ticks % 360) * startUp)`；`rotX(35 * startUp)`；`rotZ(45 * startUp)`。
2. `instability = min(6, 1 + instabilityValue * 0.66 * min(craftCount,50)/50)`。
3. 对 `a,b,c ∈ {0,1}`：抖动（仅 active）`xW = sin((ticks + a*10) / (15 − instability/2)) * 0.01 * startUp * instability`，`yW` 用 `b*10` 与 `14 −…`，`zW` 用 `c*10` 与 `13 −…`；`push`；`translate(xW ± 0.25, yW ± 0.25, zW ± 0.25)`（0→−，1→+）；`a>0: rotX(90)`；`b>0: rotY(90)`；`c>0: rotZ(90)`；`scale(0.45)`；画 base；`pop`。

层与状态：base `RenderType.entityCutout(infuser.png)`、正常 packedLight；glow（仅 active）`infusionMatrixGlow`（B.1：eyes + LIGHTNING + CULL + 不写深度），light `15728880`，颜色 ARGB = `(1, 0.8*a, 0.1*a, a)`，`a = (sin((ticks + a*2 + b*3 + c*4)/4) * 0.1 + 0.2) * startUp`（`createFrame :186-191`）——等价 TC4 `glColor4f(0.8, 0.1, 1.0, a)` + `(770,1)`（TC4R 把 alpha 预乘进 RGB）。

光芒 `renderHalo(pose, buffers, craftCount)`（:260-309，TC4 `drawHalo :30-80`）：`rays = FAST?10:20`；`growth = min(craftCount,50)/50`（≤0 不画）；`new Random(245L)`；`translate(0.5,0.5,0.5)`；每条：`rotX(r*360) rotY(r*360) rotZ(r*360) rotX(r*360) rotY(r*360) rotZ(r*360 + craftCount/500*360)`（累乘，不 push/pop）；`len = (r*20+5)/20*growth`，`w = (r*2+1)/20*growth`；3 个三角形：中心 `(0,0,0)` 色 `(255,255,255,255)`，外点 `(±0.866w, len, −0.5w)`、`(0, len, w)` 色 `(204,0,255,0)`；RT `infusionHalo`。

排程：BER 只把“相机相对 pose”排队（`WorldRenderTransforms.cameraRelativePose`），在 `AFTER_BLOCK_ENTITIES` 统一回放：base → `endBatch` → glow → `endBatch` → 破坏纹理（`ModelBakery.DESTROY_TYPES[stage]` + `SheetedDecalTextureGenerator`）→ halo（:61-170）。`shouldRenderOffScreen → true`；包围盒 `pos−1 … pos+1`（:318-331）。BE 需同步 `active, crafting, startUp, instability, craftCount`。

物品：`InfusionMatrixItemRenderer`（BEWLR，静止 8 立方，`renderItem :94-99`），模型 parent `thaumcraft:item/tc4_block_entity`；方块模型 `cube_all` 贴 `block/blank`（不可见，仅粒子 `block/pedestal_top`）。

### B.5 奥术基座 `arcane_pedestal` BER

`D:client/ArcanePedestalRenderer.java:22-53`（TC4：`TilePedestalRenderer.java:18-44`）：`ticks = cameraEntity.tickCount + pt`；`bob = sin(ticks/16) * 0.05`；`legacyFullBlock = BlockItem && model.isGui3d()`，`scale = full ? 2 : 1`；用 `model.applyTransform(GROUND)` 取地面变换的 Y 平移 `gY`，`localCenterY = full ? 0 : 0.125`；`renderY = 1.15 + bob + scale * (0.1 + localCenterY − gY)`；`translate(0.5, renderY, 0.5)` → `rotY(ticks % 360)` → `scale(s)` → `itemRenderer.renderStatic(stack×1, GROUND, packedLight, overlay, …, level, 0)`。`shouldRenderOffScreen → true`，包围盒 `[pos, pos+(1,2,1)]`。方块模型：`[0,0,0]-[16,4,16]`、`[4,4,4]-[12,12,12]`、`[2,12,2]-[14,16,14]`（`block/pedestal_side/top`）。

### B.6 坩埚液面 `crucible`

`D:client/CrucibleRenderer.java:24-63`（TC4：`TileCrucibleRenderer.java:21-35`）：精灵 `minecraft:block/water_still`；单个朝上四边形 `x,z ∈ [0,1]`、`y = fluidHeight()`；`recolor = totalAspects/100`，`>0` 时 `0.5 + recolor/2`；底色 `0x4361FF`（TC4R 的“1.7.10 蓝水等价色”），`R×(1−recolor/3)`、`G×(1−recolor)`、`B×(1−recolor/2)`，alpha 255，`packedLight`，法线 +Y；RT `RenderType.translucent()`。TC4 原版：白色 × 1.7.10 自带蓝色的水图标、`(770,771)`。

### B.7 精华管道

几何由自定义模型 loader `thaumcraft:static_block`（`static_part: essentia_tube`）在区块网格里画（`StatefulStaticMeshes:200`），BER 只画阀门手轮（`D:client/EssentiaTubeRenderer.java:11-27`）。盒子尺寸（`D:client/EssentiaTubeMesh.java:62-130`）：臂截面 `[7/16, 9/16]`，有连接时延伸到 `0` 或 `1`（`extended` 时 `−0.375` / `1.375`）；核心 `[0.375,0.625]`（`pipe_2`），直通窄核心 `[0.40625,0.59375]`（`pipe_3`）；过滤管 `[0.34375,0.65625]`（`pipe_filter` + 要素色 `pipe_filter_core`）；缓冲 `[0.25,0.75]`；阀门贴图 `textures/models/valve.png`（128×64，`entityCutoutNoCull`）。附属 mod 无法复用该 loader，只可参考尺寸。

### B.8 灵气节点 / 节点罐

`D:client/AuraNodeRenderer.java`：贴图 `misc/nodes.png`（打包 1088²，帧网格 32×32，用 `EffectAtlases.NODES`）；RT `legacyNodeTranslucent(tex, additive, blur=true, depthTest)`（B.1）。`renderNode :96-152`：未揭示时只画核心 `emitLayer(strip 1, radius 0.5)` alpha `0.101960786`（=26/255）；揭示时 `alpha = (viewDist − dist)/viewDist`（viewDist 64，手持扫描仪 48）× 修饰器脉动；帧 `floorMod(nanoTime/40ms + x, 32)`；广告牌朝向 `poseStack.mulPose(entityRenderDispatcher.cameraOrientation())`；要素层 `scale = 0.2 + aspectPulse * amount/50`，`aspectPulse = sin(ticks/(15 − i%15))*0.25 + 0.5`（:425-428）；`emitLayer :450-473` 画 `±radius` 方形，UV `(frame/32, strip/32)` 转打包图集，顶点亮度 `LightTexture.pack(14,0)`。节点罐 `NodeJarRenderer`（:9-16）→ `renderContainedNode`（:578，核心半径 0.35、alpha 0.1，要素层 ×0.7）。

### B.9 护目镜 HUD（世界内要素浮标）与容器要素浮层

触发与数据（`D:client/GogglesClientEvents.java:51-83`，TC4：`TC4:client/lib/RenderEventHandler.java:154-174, 228-290`）：
- 条件：`RevealingEquipment.hasGogglesOverlay(player)`（盔甲+饰品中任一物品 `instanceof GogglesOverlayGear` 返回 true，或数据驱动的装备能力）；准星方块的 BE `instanceof AspectContainerView` 且 `GogglesAspectView.resolve` 非空（先 `visibleAspects()` 按 `visibleAspectOrder()`；为空时显示 `visibleAspectFilters()`，数量 −1 不画数字）。
- 音符盒/奥术耳：显示 `"Note: n"`（`renderNote :159-177`）。
- `tagScale`：每次高亮 `+= 0.031 − tagScale/10`（上限 0.3），每帧 `decayScale` `−0.005`。
- 位置：BE 中心（若该 BE 的渲染器被 `GogglesTrackedBlockEntityRenderer` 包装则取渲染中心）`+ (0, spaceAbove?0.4:0 + (WandRechargePedestal?0.6:0), 0)`；方向 = 上方是空气则 `UP`，否则命中面。
- 在 `AFTER_LEVEL` 回放（`renderAspectRows :126-157`）：每行 5 个；`shiftX = (cur − rowSize/2 + 0.5) * s * 4 * s`；换行 `shiftY += s*1.05`；`translate(center + s*2*dir.step)` → `rotY(−camera.yRot)` → `translate(shiftX,0,0)` → `rotZ(180)` → `scale(s)`。
- 图标（`renderAspect :179-202`）：未知要素用 `aspects/_unknown.png`；RT `normalTranslucentEmissiveNoDepthBillboard`（无深度测试）；`±0.5` 方块，要素色、alpha 191，`FULL_BRIGHT`。
- 数字（`renderAmount :204-221`）：`translate(0, 0.24, −0.1)`、`scale(0.04)`；阴影字 `0xBF111111` 于 `(14−w, 1)`，白字 `−1` 于 `(13−w, 0)`；`Font.DisplayMode.SEE_THROUGH`，`FULL_BRIGHT`。

**结论：Isorropia 的 BE 只要实现 `AspectContainerView`，护目镜浮标由 TC4R 自动绘制**；只有非 BE 目标（实体、自定义透镜视图）才需要自己画，可照抄上述数值（`GogglesClientEvents`/`TCClientRenderTypes` 包私有）。

容器内物品要素（`D:client/ContainerAspectOverlay.java:15-33`）：仅当 `!TCClientConfig.aspectTooltipComponent()` 时，悬停槽位在 `(mouseX−5, mouseY−24)`、z+500 画 `ItemAspectTooltip.renderImage`；否则走 tooltip component。物品要素来自 TC4R 数据（`object_aspects`），附属 mod 不需要渲染代码。

### B.10 粒子实现模式

- 粒子类型：`TCParticles.*`（`SimpleParticleType`），提供器在 `TCClientEvents.registerParticleProviders`（`D:client/TCClientEvents.java:870-916`）全部 `event.registerSpriteSet(type, Provider::new)`，仅 `FROST_SHARD_DEBRIS` 用 `registerSpecial`。
- 精灵 JSON：`assets/thaumcraft/particles/<name>.json` → `{"textures": ["thaumcraft:sparkle_00", …]}` → `textures/particle/<sprite>.png`。注意：多数 TC4R 粒子虽注册了 SpriteSet，**实际绘制用打包的 `misc/particles.png`**（层 `WORLD_LAST_LEGACY_ATLAS_*`），精灵只为满足原版加载。
- 各粒子类所在层（`getRenderType()`）：Sparkle/WispMote/BoreSparkle/RunedStoneRune/InfusionRune/ScanRune/NodeJarFormation/ChampionGeneric/VisSparkle → `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`；EssentiaTrail/SmokeSpiral/TubeVent/TaintSwarm/EldritchWisp → `…_TRANSLUCENT_LAYER_1`；ChampionSpark/ShockSpark/EldritchLockSpark → `…_ADDITIVE_LAYER_2`；CrucibleBubble → `WORLD_LAST_ADDITIVE_LAYER_0`；WispBurst → `LEGACY_EFFECT_NODE_ADDITIVE`；InfusionBlock/HungryNode/BoreDig → `WORLD_LAST_BLOCK_TRANSLUCENT_LAYER_1`；InfusionItem → `WORLD_LAST_BLOCK_ADDITIVE_LAYER_2`；Taint 系 → `LEGACY_EFFECT_TRANSLUCENT`；PaletteWisp → `NitorParticlePass.QUEUE`（AFTER_BLOCK_ENTITIES 自管 pass）。

### B.11 注册模式（TC4R 自身）

- BER：`EntityRenderersEvent.RegisterRenderers`（`TCClientEvents.registerBlockEntityRenderers`），需要护目镜中心追踪的包 `GogglesTrackedBlockEntityRenderer.provider(...)`（包私有）。
- 物品 BEWLR：在 `FMLClientSetupEvent` 里 `ClientExtensionTarget.register(item, NativeItemClientExtensions{ getCustomRenderer() })`（TC4R 内部手段，`TCClientEvents.java:448-454,1122-1129`）；附属 mod 用 Forge 标准 `Item#initializeClient`（D.7）。
- 物品模型父级 `thaumcraft:item/tc4_block_entity`（`JAR assets/thaumcraft/models/item/tc4_block_entity.json`）：`parent builtin/entity`，`gui_light side`；display：`gui rot[30,225,0] trans[0,0,0] scale 0.625`；`ground rot 0 trans[0,3,0] scale 0.25`；`fixed scale 0.5`；`thirdperson_righthand rot[75,45,0] trans[0,2.5,0] scale 0.375`；`firstperson_righthand rot[0,45,0] scale 0.4`；`firstperson_lefthand rot[0,225,0] scale 0.4`。附属 mod 可直接 `"parent": "thaumcraft:item/tc4_block_entity"`。
- GUI 浮层：`RegisterGuiOverlaysEvent`（`TCClientEvents.registerGuiLayers :671-680`：`warp_vignette`、`hover_harness`、`wand_dial`、`focus_radial`、`sanity_checker`、`aspect_gain`、`player_notifications`、`research_popup`）。
- 世界阶段渲染：`MinecraftForge.EVENT_BUS` 上 `RenderLevelStageEvent` → `WorldRenderContext`（A.7）。

---

## C. TC4R 贴图/精灵清单（`JAR` 内 `assets/thaumcraft/`，python zipfile 列出并读 PNG 头）

### C.1 图集事实

- `JAR assets/minecraft/atlases/blocks.json` 给方块图集加了三个源：`directory source=models prefix=models/`、`single thaumcraft:misc/parchment`、`directory source=aspects prefix=aspects/`。`directory` 源跨**所有命名空间** → 任何 mod 的 `textures/models/**` 与 `textures/aspects/**` 都会被拼进方块图集（精灵名 `<ns>:models/<path>`、`<ns>:aspects/<name>`）。这就是工程决策 7（渲染器皮肤放 `textures/entity/`）的原因；反过来，`isorropia:aspects/*`（工程已有 8 张）天然可作方块图集精灵用。
- 同一张 `textures/models/x.png` 既可作独立贴图（`RenderType.entityXxx(rl)`），也可作方块图集精灵 `thaumcraft:models/x`（如 `models/jarbrine`）。独立绑定时 `.mcmeta` 的 `blur` 生效；作图集精灵时不生效。
- **TC4R 重新打包/改名的 TC4 贴图**：`misc/particles.png` 256²→**200×440**；`misc/particles2.png`→`misc/particles_secondary.png` 512²→**208×584**；`misc/nodes.png` 2048²→**1088×1088**；`beam1/2/3.png`→`beam_primary/secondary/tertiary.png`；`radial2.png`→`radial_secondary.png`；TC6 路径 `models/obj/hemis.obj` 在 TC4R 为 TC4 路径 `textures/models/hemis.obj`。凡按 TC4 像素/归一化坐标取 UV 的代码必须用 `particleAtlasUv/effectAtlasUv`。

### C.2 相关贴图（尺寸 / mcmeta）

| 路径（`thaumcraft:textures/…`） | 尺寸 | 备注 |
|---|---|---|
| `block/animatedglow.png` | 16×320 | 动画 frametime 2，帧 0→19→1；罐内液体精灵 `thaumcraft:block/animatedglow` |
| `block/jar_side/jar_top/jar_bottom.png`、`jar_side_void/jar_top_void.png` | 16² | 罐方块模型 |
| `block/brainbox.png`、`block/blank.png`、`block/empty.png` | 16² | |
| `block/pedestal_top/pedestal_side.png` | 16² | 基座/矩阵粒子 |
| `block/crucible1..6.png` | 16² | 坩埚 |
| `block/pipe_1/2/3.png` 32²、`pipe_buffer/filter/filter_core/oneway/restrict/valve.png` | | 管道 |
| `block/runeborder.png` 64²、`block/wardedglassrune.png` 16²、`block/es_i_1.png` 32×192（动画 frametime 2，帧序 0,2,4,3,2,1,3,5,4,3,2,1） | | 符文/精华贮存 |
| `models/jar.png`、`models/jar_void.png`、`models/jarbrine.png` | 64×32 | TC4 `ModelJar` 皮肤 / 盐水 |
| `models/label.png` | 16² | 罐标签（Isorropia 悬挂标签也用它） |
| `models/brain2.png` | 256×128 | 罐中脑 |
| `models/infuser.png` | 64² | 矩阵立方（上 32 行 base，下 32 行 overlay） |
| `models/valve.png` | 128×64 | |
| `models/hemis.obj` + `models/hemis0..15.png`（128²，1..15 带 blur） | | 守护半球/声波 |
| `models/ripple1..15.png` | 128²，blur | 声波波纹（**无 ripple0**） |
| `models/scanner.obj` + `scanner.png` 256²、`scanscreen.png` 128²(blur) | | |
| `models/node_stabilizer.obj` + `node_stabilizer(_over).png` 32²、`node_converter(_over).png` 32² | | |
| `models/crystal.png` 64×32、`crystalcapacitor.png` 128×64、`vcrystal.obj/png`、`vis_relay.obj/png` 32² | | |
| `models/goggles.png`、`runic_goggles.png` | 128×64 | |
| `models/golem_damage.png`、`golem_decoration.png` | | |
| `misc/particles.png` | **200×440**（blur） | TC4 主粒子表（打包） |
| `misc/particles_secondary.png` | **208×584**（blur） | TC4 `particles2`（打包） |
| `misc/nodes.png` | **1088²**（blur） | 节点（打包） |
| `misc/node_bubble.png` | 256² | |
| `misc/wisp.png` 256²(blur)、`misc/wispy.png` 512×128(blur) | | |
| `misc/beam.png` 32×64、`beam_primary.png` 32²、`beam_secondary.png` 64²、`beam_tertiary.png` 64×128、`beamh.png` 64² | | `BeamTexture` |
| `misc/vortex.png` 256²(blur)、`misc/tunnel.png` 64² | | |
| `misc/radial.png`、`misc/radial_secondary.png` | 256² | 焦点轮盘 |
| `misc/script.png` 256×16(blur)、`misc/mark.png` 128²(blur)、`misc/home.png` 256²(blur) | | |
| `misc/eldritch_portal.png`、`misc/cultist_portal.png` | 2048×128 | 16 帧横条 |
| `misc/particlefield.png` 256²(blur)、`particlefield32.png` 32²、`p_large.png`/`p_small.png` 32² | | |
| `misc/potions.png` 256²、`misc/vignette.png` 256²(blur) | | |
| `item/lightningringv.png` 4096×256、`item/lightningring.png` 256×4096 | | 充能节点环（16 帧） |
| `aspects/<name>.png` 32²（48 个 + `_unknown`）、`aspects/_back.png` 64² | | |
| `particle/sparkle_00..03`、`essentia_trail`、`scan_rune_00..15`、`node_jar_formation_00..08`、`crucible_bubble_0..2`、`tube_vent_1..5`、`smoke_spiral_1..4`、`taint_swarm_00..07`、`shock_spark_{0,1,2}_00..07`（均 16²）；`wisp_mote` 32²；`wisp_burst_00..31` 64² | | 原版粒子图集精灵 |
| `gui/hud.png` 256² | | |

着色器资源：`shaders/post/{blurtc,desaturatetc,hunger,sunscorned}.json`、`shaders/program/tc4_bloom(_color).{json,fsh}`（后处理，经 `showScreenEffect` 使用）。

### C.3 Isorropia 1.12 / Thaumic Horizons 中引用的 `thaumcraft` 命名空间贴图在 TC4R 的存在性

（脚本扫描 `ResourceLocation("thaumcraft", …)`、`UtilsFX.bindTexture(…)`、`"thaumcraft:textures/…"`；IS = Isorropia_kedition `fr/wind_blade/isorropia/`，TH = ThaumicHorizons `com/kentington/thaumichorizons/`）

| 引用路径 | TC4R | 引用处 |
|---|---|---|
| `textures/models/label.png` | 有 | IS:client/renderer/entities/RenderHangingLabel.java:16 |
| `textures/models/jar.png` | 有 | TH:common/tiles/TileSoulJar.java:32 |
| `textures/models/infuser.png` | 有 | TH:client/renderer/tile/TileVatMatrixRender.java:99 |
| `textures/misc/particles.png` | 有（**已打包**） | IS:client/libs/RenderEventHandler.java:62 |
| `textures/misc/nodes.png` | 有（**已打包**） | TH:client/renderer/tile/TileVortexRender.java:259 |
| `textures/misc/vortex.png` | 有 | IS:common/lenses/AirLens.java:23；TH:TileVortexRender.java:260 |
| `textures/misc/radial.png` | 有 | IS:client/libs/RenderEventHandler.java:259；TH:client/lib/RenderEventHandler.java:262 |
| `textures/misc/radial2.png` | **无** → 用 `radial_secondary.png` | IS:RenderEventHandler.java:260；TH:RenderEventHandler.java:279 |
| `textures/misc/eldritch_portal.png` | 有 | TH:client/fx/FXContainment.java:104 |
| `textures/models/hemis.obj` | 有 | TH:client/fx/FXSonic.java:113 |
| `models/obj/hemis.obj`（TC6 路径） | **无** → `textures/models/hemis.obj` | IS:client/fx/FXSonic.java:28 |
| `textures/models/ripple<frame>.png` | 有 1..15（无 0） | IS:client/fx/FXSonic.java:71 |
| `textures/models/crystal.png`、`golem_damage.png`、`golem_decoration.png`、`node_converter(_over).png`、`node_stabilizer.obj`、`scanner.obj` | 有 | TH 各 tile 渲染器 |
| `textures/aspects/_unknown.png` | 有 | TH:client/gui/GuiBloodInfuser.java:427 |
| `textures/gui/gui_researchbook_overlay.png`、`gui_researchback.png`、`gui_focuspouch.png` | 有 | IS:RenderEventHandler.java:60；TH |
| `textures/misc/r_runicupg.png` | 有 | TH:common/ThaumicHorizons.java:2386 |
| `textures/gui/paper.png` | **无**（TC6 独有） | IS:client/libs/RenderEventHandler.java:63 |
| `textures/items/celestial/{sun,stars1,moon1..8}.png` | **无**（TC6 独有） | IS:common/celestial/CelestialBody.java:18-63 |
| `textures/items/lightningringv.png`（TH 字符串经 `UtilsFX.bindTexture` → thaumcraft 命名空间；TH 自身资源无此图） | 改名为 `item/lightningringv.png`（4096×256） | TH:client/renderer/tile/TileEssentiaDynamoRender.java:98、TileEtherealShardRender.java:201、TileRecombinatorRender.java:83 |

缺失项只能从 TC6 / Isorropia 资源复制到 `isorropia` 命名空间（版权与来源由各分析文档记录）。

---

## D. Forge 47（1.20.1，47.4.23）实现要点

### D.1 `forge:obj` 模型

`F47: net/minecraftforge/client/model/obj/ObjLoader.java:48-61` 读取的键（默认值）：`model`（必填，`ns:models/…/x.obj`）、`automatic_culling`(true)、`shade_quads`(true)、`flip_v`(false)、`emissive_ambient`(true)、`mtl_override`(null)。另外 Forge 通用模型键：`render_type`（如 `"minecraft:cutout"`/`"minecraft:translucent"`）、`textures`、`visibility`（`{"组名": false}`，`isComponentVisible`，`ObjModel.java:339,572`）、`transform`。

```json
{
  "loader": "forge:obj",
  "model": "isorropia:models/block/vat_redo.obj",
  "flip_v": true,
  "emissive_ambient": false,
  "render_type": "minecraft:cutout",
  "textures": { "particle": "isorropia:block/vat", "vat": "isorropia:block/vat" },
  "visibility": { "glass": false }
}
```

- `.mtl` 的 `map_Kd #vat` → 取 JSON `textures.vat`；`map_Kd ns:path` → 直接当方块图集精灵（`UnbakedGeometryHelper.resolveDirtyMaterial :77-95`）。**OBJ 贴图必须在方块图集**：放 `textures/block/`（或 `textures/item/`；`textures/models/`、`textures/aspects/` 也会被 TC4R 拼入，但与决策 7 冲突，避免）。
- `flip_v: true` 时 `v = 1 − v`（`ObjModel.java:428`）。1.7.10 Forge `WavefrontObject` 读取时同样做了 `1 − v`（待核对），所以给 1.7.10 `IModelCustom` 用的 OBJ 一般要 `flip_v: true`。
- `emissive_ambient: true` 时 mtl 的 `Ka` 被当作假光照 `(Ka.r+Ka.g+Ka.b)*15/3`（`ObjModel.java:386-391`）；不想发光就设 false。mtl 额外键 `forge_TintIndex`。
- `o` / `g` 分组 → part 名，可用 `visibility` 做“一个 OBJ 多个 JSON 只显示某组”，替代 1.7.10 的 `renderPart("name")`。
- TC4R 自己的 OBJ 渲染 helper（`NamedGroupObjMesh`）包私有，不可复用。

### D.2 在 BER 里画 BakedModel（额外模型）

```java
// 模组总线
@SubscribeEvent static void onAdditional(ModelEvent.RegisterAdditional e) {
    e.register(new ResourceLocation("isorropia", "block/vat_obj"));   // 指 models/block/vat_obj.json，不带 models/ 与 .json
}
// BER.render
BakedModel m = Minecraft.getInstance().getModelManager().getModel(VAT_OBJ);    // Forge 补丁方法（ModelManager.java.patch +getModel(ResourceLocation)）
Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
    poseStack.last(), buffers.getBuffer(RenderType.cutout()), null, m,
    1f, 1f, 1f, packedLight, packedOverlay, ModelData.EMPTY, RenderType.cutout());
```
签名（javap 核实）：`ModelBlockRenderer.renderModel(PoseStack.Pose, VertexConsumer, @Nullable BlockState, BakedModel, float r, float g, float b, int light, int overlay, ModelData, @Nullable RenderType)`；`BlockRenderDispatcher.renderSingleBlock(BlockState, PoseStack, MultiBufferSource, int, int, ModelData, RenderType)`。BER 的 buffer 支持区块 RT（solid/cutout/translucent）；半透明需正确排序时用 `RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)` 或 `Sheets.translucentCullBlockSheet()`。`ModelEvent.ModifyBakingResult/BakingCompleted` 可拿 `Map<ResourceLocation,BakedModel>` 包装模型。

### D.3 BER 注册、可见范围、同步

- 注册：模组总线 `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer(BlockEntityType<? extends T>, BlockEntityRendererProvider<T>)`（`F47:client/event/EntityRenderersEvent.java:126`）；模型层 `RegisterLayerDefinitions#registerLayerDefinition(ModelLayerLocation, Supplier<LayerDefinition>)`，BER 构造里 `ctx.bakeLayer(LAYER)`。工程现有入口：`src/main/java/fr/wind_blade/isorropia/client/ClientSetup.java:37-71`。
- `BlockEntityRenderer` 默认方法：`shouldRenderOffScreen(T)`（超出方块时返回 true）、`getViewDistance()`（默认 64）、`shouldRender(T, Vec3 camera)`。
- 视锥裁剪用 BE 的 `IForgeBlockEntity#getRenderBoundingBox()`（`F47:common/extensions/IForgeBlockEntity.java:105`，默认取碰撞箱；无限大用 `INFINITE_EXTENT_AABB` :96）。在 **BE 类**里覆写（返回 AABB 不涉客户端类，服务端安全）。TC4R 的 `NativeBlockEntityRenderer.getRenderBoundingBox(T)` 不会被 Forge 调用。
- 同步：BE 覆写 `getUpdateTag()`（`saveWithoutMetadata()` 或精简 tag）与 `getUpdatePacket()`（`ClientboundBlockEntityDataPacket.create(this)`）；数据变化时 `setChanged()` + `level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)`；Forge 默认 `onDataPacket` 调 `load(tag)`（`IForgeBlockEntity.java:53-58`）。动画用的时钟优先客户端本地（`cameraEntity.tickCount + partialTick`，TC4R 一贯做法），只同步状态量。客户端逐 tick 逻辑（如插值角度）放 `BlockEntityTicker` 的客户端分支。

### D.4 Curios 5.14.1 `ICurioRenderer`

javap（`curios-forge-5.14.1+1.20.1_mapped_official_1.20.1.jar`）：
```java
<T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
    RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers, int light,
    float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch);
// 静态辅助
ICurioRenderer.translateIfSneaking(PoseStack, LivingEntity);  ICurioRenderer.rotateIfSneaking(PoseStack, LivingEntity);
ICurioRenderer.followHeadRotations(LivingEntity, ModelPart...);  ICurioRenderer.followBodyRotations(LivingEntity, HumanoidModel<LivingEntity>...);
ICurioRenderer.renderModel(Model, ResourceLocation, PoseStack, MultiBufferSource, int light, RenderType);
// 注册
CuriosRendererRegistry.register(Item, Supplier<ICurioRenderer>);
```
`register` 只把 Supplier 存入表；Curios 在 `EntityRenderersEvent.AddLayers`（`Curios$ClientProxy.addLayers`）调 `CuriosRendererRegistry.load()` 才实例化。所以在 `FMLClientSetupEvent` 里注册即可，Supplier 内可安全 `Minecraft.getInstance().getEntityModels().bakeLayer(...)`。头部饰品典型：`if (parent.getModel() instanceof HumanoidModel<?> h) h.head.translateAndRotate(poseStack);` 或 `followHeadRotations(entity, myHead)`。

### D.5 自定义 RenderType（加色/半透明）与“混合被 shader 覆盖”坑

- `RenderType.create(String, VertexFormat, Mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, CompositeState)` 在 Forge 47 是 public（7 参版）；`CompositeState.builder()` 与 `TextureStateShard(rl, blur, mipmap)`、`ShaderStateShard(Supplier<ShaderInstance>)`、`TransparencyStateShard(name, setup, clear)` 构造器均 public。原版 shard 常量是 `protected static` → 写一个 `extends RenderType` 的私有构造器工具类引用，或直接用 TC4R `NativeRenderStates` 的公开别名（A.9）。
- **混合覆盖风险**：1.20.1 所有原版 core shader JSON 都带 `"blend": {"func":"add","srcrgb":"srcalpha","dstrgb":"1-srcalpha"}`（MC 核实），`ShaderInstance.apply()` 会调 `BlendMode.apply()`；后者用 `equals(lastApplied)` 缓存且 `lastApplied` 只在不透明/透明切换时更新（javap 核实），所以在某些绘制顺序下 shader 的普通混合会**覆盖** RenderType 的 `LIGHTNING/ADDITIVE` 混合，造成加色特效偶发变成普通 alpha 混合。TC4R 的对策：`ForgeShaderBlendMixin`（`D:nativeimpl/mixin/ForgeShaderBlendMixin.java`）在 `RenderTypeBlendPlatform.renderTypeOwnsBlend()` 为 true 时跳过 shader 混合，而 `NativeRenderTypes.create(...)` 的 `end()` 会置 true。**Isorropia 的自定义加色 RT 建议用 `NativeRenderTypes.create(...)` 创建**（或自己覆写 `end()` 包 `RenderTypeBlendPlatform.enter(true)/restore(prev)`）。
- 推荐配方（照抄 TC4R）：TC4 `(770,1)` + 不写深度 + 全亮 + 不裁剪 → `RENDERTYPE_EYES_SHADER` + `LIGHTNING_TRANSPARENCY` + `COLOR_WRITE`（+ `NO_CULL` 或 `CULL`），格式 `NEW_ENTITY`/`QUADS`；TC4 `(770,771)` 半透明全亮 → `entity_translucent_emissive` + `TRANSLUCENT_TRANSPARENCY` + `COLOR_WRITE`（注意贴图 alpha<0.1 被丢弃）；纯顶点色光芒 → `eldritchOrbRays()`（公开）。

```java
static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(tex -> NativeRenderTypes.create(
    "isorropia_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
    RenderType.CompositeState.builder()
        .setShaderState(NativeRenderStates.RENDERTYPE_EYES_SHADER)
        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
        .setTransparencyState(NativeRenderStates.LIGHTNING_TRANSPARENCY)
        .setWriteMaskState(NativeRenderStates.COLOR_WRITE)
        .setCullState(NativeRenderStates.NO_CULL)
        .createCompositeState(false)));
```
- 原版现成 RT：`entityTranslucentEmissive(rl[,outline])`、`eyes(rl)`（ADDITIVE 1,1）、`energySwirl(rl,u,v)`、`lightning()`、`beaconBeam(rl, translucent)`、`dragonExplosionAlpha(rl)`、`itemEntityTranslucentCull(rl)`、`entityCutoutNoCull(rl)`。
- 自定义 GLSL：模组总线 `RegisterShadersEvent#registerShader(new ShaderInstance(provider, rl, format), consumer)`（1.12 Isorropia 的 `shader/test.vert/test2.frag` 若需保留走此路）。

### D.6 粒子

```java
// 公共注册（双端）
DeferredRegister<ParticleType<?>> P = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "isorropia");
RegistryObject<SimpleParticleType> SONIC = P.register("sonic", () -> new SimpleParticleType(false)); // true=无视粒子数量上限
// 客户端模组总线
@SubscribeEvent static void onParticles(RegisterParticleProvidersEvent e) {
    e.registerSpriteSet(SONIC.get(), SonicParticle.Provider::new);   // SpriteSet → Provider
    // e.registerSpecial(type, provider)  无精灵；e.registerSprite(type, ParticleProvider.Sprite) 单精灵
}
// assets/isorropia/particles/sonic.json: {"textures": ["isorropia:sonic_0", "isorropia:sonic_1"]}
//   → assets/isorropia/textures/particle/sonic_0.png（进原版粒子图集）
```
签名：`F47:client/event/RegisterParticleProvidersEvent.java:54/70/87`。Forge 47 的 `ParticleEngine` 改为按 key 集合遍历（`ParticleEngine.java.patch`：`for (type : this.particles.keySet())`，TreeMap 按 `ForgeHooksClient.makeParticleRenderTypeComparator` 排序，TC4R mixin 再按 A.7 rank 重排）→ 自定义 `ParticleRenderType`（实现 `begin(BufferBuilder, TextureManager)`/`end(Tesselator)`）会被渲染。要 TC4 观感：粒子 `getRenderType()` 直接返回 `ParticleSheets.LEGACY_EFFECT_ADDITIVE`（原版粒子图集 + `SRC_ALPHA,ONE` + 不写深度）或 `WORLD_LAST_LEGACY_ATLAS_ADDITIVE_LAYER_0`（绑打包 `particles.png`，UV 用 `particleAtlasUv` 换算）。`particle` shader 对“贴图×顶点色”alpha<0.1 丢弃（B.1）——TC4 低 alpha 粒子会整片消失，需抬高 alpha 或改用 eyes shader 的自管 pass。

### D.7 物品自定义渲染

- BEWLR：在 Item/BlockItem 子类覆写 `initializeClient(Consumer<IClientItemExtensions> c)`，`c.accept(new IClientItemExtensions(){ BlockEntityWithoutLevelRenderer r; public BlockEntityWithoutLevelRenderer getCustomRenderer(){ if (r==null) r = new MyBewlr(); return r; } })`（懒创建：构造期 `Minecraft` 的 BE 分发器/模型集尚未就绪）。BEWLR 构造 `super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels())`，覆写 `renderByItem(ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, int light, int overlay)`；坐标原点为 0..1 方块角（display 变换之后）。若在构造里 bake 模型层，需要在资源重载后重建（BEWLR 本身是 `ResourceManagerReloadListener`，可在 `RegisterClientReloadListenersEvent` 注册）。
- 物品模型 JSON：`"parent": "builtin/entity"` 或 `"thaumcraft:item/tc4_block_entity"`（B.11 的 display）。
- `IClientItemExtensions` 其他钩子：`getArmPose`、`applyForgeHandTransform`、`getHumanoidArmorModel`、`getGenericArmorModel`、`renderHelmetOverlay(stack, player, w, h, pt)`（头盔屏幕叠加，透镜/护目镜 HUD 可用）（`F47 IClientItemExtensions.java:60-161`）。
- `ItemProperties.register(Item, ResourceLocation, ItemPropertyFunction)`（javap：1.20.1 签名；工程已在 `ClientSetup.java:39` 的 `enqueueWork` 里用）；模型 `overrides: [{"predicate": {"isorropia:x": 0.5}, "model": "..."}]`。

### D.8 HUD 与世界阶段渲染

- `RegisterGuiOverlaysEvent#registerAbove/registerBelow(ResourceLocation other, String id, IGuiOverlay)`、`registerAboveAll/BelowAll(String id, IGuiOverlay)`；`IGuiOverlay.render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight)`（`F47:client/gui/overlay/IGuiOverlay.java:19`）。锚点 `VanillaGuiOverlay.X.id()`。
- `RenderLevelStageEvent`（Forge 总线）阶段：`AFTER_SKY, AFTER_SOLID_BLOCKS, AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS, AFTER_CUTOUT_BLOCKS, AFTER_ENTITIES, AFTER_BLOCK_ENTITIES, AFTER_TRANSLUCENT_BLOCKS, AFTER_TRIPWIRE_BLOCKS, AFTER_PARTICLES, AFTER_WEATHER, AFTER_LEVEL`（`F47 RenderLevelStageEvent.java:165-214`）；getter：`getPoseStack()`（含视图旋转）、`getProjectionMatrix()`、`getPartialTick()`、`getCamera()`、`getFrustum()`、`getRenderTick()`。常规写法：`pose.pushPose(); pose.translate(-cam.x,-cam.y,-cam.z); …; MultiBufferSource.BufferSource bs = mc.renderBuffers().bufferSource(); …; bs.endBatch(rt); pose.popPose();`。TC4 的 `RenderWorldLastEvent` ≈ `AFTER_LEVEL`（或要被半透明遮挡时用 `AFTER_TRANSLUCENT_BLOCKS`）。
- 广告牌：`poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation())`（TC4R 节点做法）；TC4 `ActiveRenderInfo.rotationX/XZ…` 手工四边形可等价替换。

### D.9 1.7.10 GL → 1.20.1 对照

| TC4 / 1.7.10 | 1.20.1 等价 |
|---|---|
| `TileEntitySpecialRenderer.renderTileEntityAt(te, x, y, z, pt)` 中 `glTranslated(x, y, z)` | BER 的 `poseStack` 已在方块角，**去掉 (x,y,z)**，保留其后的偏移 |
| `glTranslatef(a,b,c)` / `glRotatef(deg, x,y,z)` / `glScalef` | `pose.translate` / `pose.mulPose(Axis.XP/YP/ZP.rotationDegrees(deg))`（负轴用 `XN/YN/ZN`；任意轴 `new Quaternionf().rotationAxis(rad, x,y,z)`）/ `pose.scale`；顺序保持一致 |
| `glPushMatrix/glPopMatrix` | `pushPose/popPose` |
| `glColor4f(r,g,b,a)` | 顶点色，或 `ModelPart.render(pose, vc, light, overlay, r, g, b, a)` |
| `glBlendFunc(770,1)` / `(770,771)` / `(1,1)` | `LIGHTNING_` / `TRANSLUCENT_` / `ADDITIVE_TRANSPARENCY`（B.1） |
| `glDepthMask(false)` | `COLOR_WRITE`；`glDisable(GL_DEPTH_TEST)` → `NO_DEPTH_TEST` |
| `glDisable(GL_CULL_FACE)` | `NO_CULL` |
| `glDisable(GL_LIGHTING)` + `setLightmapTextureCoords(240,240)` | packedLight `LightTexture.FULL_BRIGHT`(15728880 = 0xF000F0)，配无方向光 shader（eyes）或 emissive shader |
| `glAlphaFunc(516, 0.003921569F)` | 只能用不丢弃的 shader（eyes / entity_no_outline / lightning），见 B.1 |
| `glShadeModel(GL_SMOOTH)` + 逐顶点色 | 默认即逐顶点插值 |
| `Tessellator.startDrawing(6)`（三角扇） | `Mode.TRIANGLES` 手工拆成三角形（TC4R halo 做法） |
| `UtilsFX.renderQuadCenteredFromTexture(tex, scale, r,g,b, bright, blend, alpha)` | 自建 `±0.5*scale` 四边形，RT 按 blend 选择，light = bright |
| `ModelRenderer` + `render(0.0625F)` | `ModelPart`（`CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d[, CubeDeformation])`、`PartPose.offsetAndRotation`），`LayerDefinition.create(mesh, texW, texH)`；单位、Y 向下、UV 展开与 1.7.10 一致；`mirror` → `.mirror()` |
| `IModelCustom.renderAll/renderPart` | `forge:obj` + `visibility`（D.1）+ D.2 |
| `bindTexture(rl)` 后画 | 每个贴图一个 RT：`buffers.getBuffer(RenderType.xxx(rl))` |
| `EntityFX` + `ParticleEngine.addEffect` | `Particle`/`TextureSheetParticle` + `particleEngine.add(p)` 或 `level.addParticle(type, …)` |

---

## E. 选用顺序与风险

**选用顺序**（每个分析文档按此决策）：
1. 有 TC4R 公开 API 等价物（A.1/A.3/A.4/A.8）→ 直接调用；服务端触发用 `ActionPresentationApi.presentAt` + 客户端 handler，或 `WorldEffectsApi.blockSparkle`。
2. 容器要素显示 → BE 实现 `AspectContainerView`（B.9），不写渲染代码。
3. 需要 TC4 观感的自绘 → RT 用 A.4 公开 RT / A.9 `NativeRenderTypes`+`NativeRenderStates` 照抄 B.1 状态；粒子层用 `ParticleSheets.*`；UV 涉及 TC4 粒子表一律 `particleAtlasUv/effectAtlasUv`。
4. 纯 Forge 实现（D 节）。

**风险 / 只能实机确认**

| # | 内容 |
|---|---|
| 1 | A.9 所列类不是 API，TC4R 升级（>20732）可能改名/改可见性；锁定版本前提下可用，升级时 `gradlew build` 会暴露。 |
| 2 | D.5 混合覆盖：用原版 `RenderType.create` 做的加色 RT 是否被 shader JSON 混合覆盖取决于绘制历史，只能实机多场景（物品栏打开/附魔光物品同屏/粒子多）确认；用 `NativeRenderTypes.create` 可规避。 |
| 3 | alpha 裁剪：`entity_translucent(_emissive)`/`entity_cutout` 丢弃贴图 alpha<0.1，`particle` 丢弃最终 alpha<0.1。TC4 低 alpha 辉光的柔边会被削掉；需实机比对，必要时改 eyes shader。 |
| 4 | 雾：eyes / emissive shader 用 `linear_fog_fade`（远处整体变淡），TC4R 的 `WORLD_LAST_*` 粒子层把雾起点设为 MAX（不受雾）；与 1.7.10 观感差异需实机看。 |
| 5 | `WorldRenderContext` 没有公开订阅入口；在 Forge `RenderLevelStageEvent` 里自行渲染时，pose/modelView 组合（`event.getPoseStack()` vs `WorldRenderTransforms.modelViewMatrix()`）需实机验证位置不漂移；`beginAfterLevelWorldRender` 只接受 AFTER_LEVEL。 |
| 6 | `BeamSettings.width` 对应 TC4 `FXBeam` 的 `field_70130_N`；TC4 构造里先赋 1.0F 后又 `setSize(0.02F,0.02F)`（TC4:client/fx/beams/FXBeam.java:61-65），实际有效宽度存疑，需实机对比选值。 |
| 7 | `animatedglow`/`es_i_1` 等动画精灵在 BER 里经方块图集取 UV，帧动画随图集 tick；BEWLR（GUI）中是否同步动画需实机确认。 |
| 8 | `ThaumcraftClientEffectsApi` 校验失败只打一次 WARN 并静默；开发期请看日志 `Ignoring invalid client effect request`，否则“特效没出来”难排查。 |
| 9 | 1.7.10 OBJ 的 V 翻转（D.1）未从 1.7.10 Forge 源码直接核对；首个 OBJ 落地时实机确认 `flip_v`。 |
| 10 | Curios 饰品渲染还受玩家“显示饰品”开关与槽位 `render` 设置影响；需实机确认可见。 |

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

<!-- CONTINUE -->

# Isorropia 1.20.1 移植计划

## 来源与目标

| 角色 | 路径 | 说明 |
|---|---|---|
| 移植对象（功能基线） | `../Isorropia_kedition` | 1.12.2-kedition-2.1.4，TC6 API，154 个 Java 文件 |
| 机制参考（TC4 原型） | `../ThaumicHorizons` | GTNH 1.7.10 版 Thaumic Horizons，Isorropia 的前身；TC4 机制（精华、注魔、扫描）以它为准 |
| 目标 API | TC4R `dev.tc4port:thaumcraft-forge:0.1.0-20732` | 运行版本 `4.2.3.5-1.20.1-port.0.1.0-20732`；源码参考 `reference/tc4r-20721-sources`（20721，git 忽略） |
| 构建/工具参考 | `../Technomancy-1.20.1` | 同一 TC4R 的成熟移植工程（local-repo、探针、GameTest 套路） |
| 插件写法参考 | `../_tc4_1201/forbidden-magic-*.jar` | TC4R 生态里的附属 mod：要素/研究/实体要素 datapack 写法 |

范围：移植 Isorropia 已有的功能，不移植 Thaumic Horizons 中 Isorropia 没有的内容。Isorropia 中依赖 TC6 独有机制（光环区块 vis、knowledge 点数、研究 stage）的地方，按 Thaumic Horizons 的 TC4 做法落到 TC4R。

## 已定决策

1. **工程**：Forge 47.4.23 / 官方映射 / Java 17；TC4R 精确锁定 20732；Curios 5.14.1（TC4R 必需，且躯体大脑是 Curios 头部饰品）。Gradle 代理在 `~/.gradle/gradle.properties`（127.0.0.1:7890），不进工程。
2. **包与 ID**：包根 `fr.wind_blade.isorropia`，mod id `isorropia`，注册名沿用原版 1.12 的名字。
3. **要素**：`fames`/`corpus` 是 TC4 原生要素，直接用 `thaumcraft:fames`/`thaumcraft:corpus`；七个罪孽要素定义为 `isorropia:*`（`data/isorropia/thaumcraft/aspects/<name>.json`）。已知重叠：Forbidden Magic 定义了同名的 `forbidden_magic:*`，两者同装会出现同名不同 ID 的要素。TC6→TC4 的要素换算集中在 `common/Aspects`。
4. **研究**：TC6 研究 JSON（stage/knowledge）改写为 TC4R 格式 `data/isorropia/thaumcraft/research/isorropia.json`，类别 `isorropia:isorropia`，key 带命名空间。
5. **配方**：全部改为 datapack JSON（`thaumcraft:crucible` / arcane / `thaumcraft:infusion` 等 TC4R serializer），不保留 Java 注册。
6. **砍掉**：Thaumic Bases 联动（烟鬼乌贼 + `tb_compat.json`，1.20.1 无 TB）；贪食/色欲透镜（原版未注册的死代码）；Saehrimnir 系列（原版未注册）。
7. **贴图位置**：TC4R 会把所有命名空间的 `textures/models/` 拼进方块图集，所以渲染器皮肤一律放 `textures/entity/`；物品模型用的图标放 `textures/item/`。

## 阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| S0 | MDK 骨架、TC4R/Curios 依赖、git | 完成 |
| S1 | 方块/物品/音效/创造栏注册，资源迁移（贴图改路径、lang→json、blockstate/模型重写），原版配方、战利品表 | 完成（复杂物品先占位，随所属阶段替换） |
| S2 | TC 数据：要素、物品/实体要素、研究树、坩埚/奥术/注魔配方 | 完成（`python tools/validate_tc_data.py` 静态校验通过；加载需实机确认） |
| S3 | 14 个实体 + AI + 属性 + 渲染器/模型 | 完成（偏差见下） |
| S4 | 治疗缸多方块 + 生物注魔/自我注魔 + 能力（capability）+ 网络包 | 完成（偏差见下） |
| S5 | 透镜系统 + 躯体大脑（Curios）+ 按键 + HUD | 完成（透镜行为/轮盘/世界叠加/HUD 读数/躯体大脑落点均在 DEV 客户端实机验证，见下） |
| S6 | 改造矩阵、灵魂罐、灵魂信标、收容焦点、变形手指 | 完成（均实机验证，偏差见下） |
| S7 | 客户端特效与渲染收尾；集中实机验证 | 完成：TC6 `FXGeneric` 移植为 `GenericFx`，天穹吸收/通量爆炸/矩阵排气/守墓者光束按原参数重建；删除无上游依据的替身闪光；`tools/client_sweep.py` 31 个客户端探针与服务端探针 01–06 全部通过（见 RENDERING、VALIDATION） |

每个阶段：实现 → `gradlew build` → 修复 → 提交。实机测试（runServer/runClient/探针）集中在批次末尾一次做。

## 渲染与特效（跨阶段）

渲染这块单独成篇，因为它是「不跑客户端就发现不了」的重灾区：

- 逐区域**规格**在 [render-spec/](render-spec/)（00 是共享的 TC4R/Forge 渲染参考，01–07 是 vat/matrix/灵魂罐与信标/粒子特效/实体审计/透镜与 HUD/物品模型），
  每条都带 1.7.10 与 1.12 的行号、精确几何与混合状态、以及「该照哪个上游」的判断。
- **现状**（做了什么、与规格的差在哪、客户端验证的硬限制）见 [RENDERING.zh-CN.md](RENDERING.zh-CN.md)。
- 截图默认只有世界（探针把 `hideGui` 置真）；加 `--gui` 或用 `guishot` 时 HUD 与界面也进图，可以直接按像素验收。

## 并行开发方式

S2、S3 在独立 worktree 分支上并行（`../Isorropia-wt/s2-data`、`../Isorropia-wt/s3-entities`），完成后合入 master。共享的基础设施（`IsorropiaAspects`、`IsorropiaNetwork`）先在 master 落地，避免各分支各写一份。

## S2 数据说明

文件：研究 `data/isorropia/thaumcraft/research/isorropia.json`（38 条，类别 `isorropia:isorropia`）；配方 `data/isorropia/recipes/{crucible,arcane,infusion}/`（17 坩埚 + 1 奥术 + 8 注魔，配方 ID 如 `isorropia:crucible/wheat_growth`）；物品要素 `object_aspects/isorropia.json` + `vanilla_sins.json`；实体要素 `entity_aspects/isorropia.json`。改数据后跑 `python tools/validate_tc_data.py`。

**物品替换（TC6 → TC4R）**
- 要素水晶：火/风/秩序 → `thaumcraft:fire_shard`/`air_shard`/`order_shard`；嫉妒水晶 → 带 `isorropia:invidia` 的 `thaumcraft:crystal_essence`（`thaumcraft:components` 原料）。
- 书写工具 → `inkwell`；黄铜板 → `minecraft:iron_ingot`；生物奥术心智 → `zombie_brain`；注魔矩阵 → `runic_matrix`；vis 共鸣器 → `resonator`（源质共鸣器）；虚空锭/虚空罐/脑罐/大脑 → `void_ingot`/`void_jar`/`brain_jar`/`zombie_brain`。
- 基础透镜奥术配方：TC6 的 20 vis + 六种水晶各 1 → 六种初始要素各 5 vis。注魔要素量沿用 TC6 原值，未缩放。

**研究改写**
- TC6 stage/knowledge 改为 TC4 `aspects` + `complexity`（数值参照 Thaumic Horizons 同类研究）；TC6 meta：HIDDEN→CONCEALED，SPIKY→SPECIAL，HEX/REVERSE 丢弃。
- 扫描要求改为触发器（条目加 HIDDEN，扫到任意一个即发现，不再要求全部）：动物→创造物注魔，腐化生物→治疗缸，鸡/牛/猪/猪人/末影人/铁傀儡/村民→Similitudo，羊→自剪毛，兔/史莱姆→果冻兔，可驯服生物→灌注忠诚，`!scan.sun`→`lux` 要素触发（火透镜），`!scan.taintsrc`→腐化方块触发（噬腐猪），`!ignis`→`ignis` 要素触发（地狱犬）。
- 丢弃：`required_item`/`required_craft`、`!experiment.villager_zombie`（治疗缸）、`m_hellandback`（地狱犬）、治疗缸附录（无文本）、父研究 MINDBIOTHAUMIC。
- 父研究映射：BASEALCHEMY→CRUCIBLE，BASEARTIFICE→GOGGLES，WARDEDJARS→JARLABEL，BASEELDRITCH→隐藏父 ELDRITCHMINOR，`f_TELEPORT`→隐藏父 FOCUSPORTABLEHOLE；透镜注魔另加隐藏父 INFUSION。
- 生物注魔/自我注魔（治疗缸里做，S4 实现）只有文字页；治疗缸多方块的蓝图页（COMPOUND_CRAFTING + 成型 handler）留到 S4。
- 要素：TC4R 食物已自带 fames/corpus，Isorropia 补的 HUNGER/FLESH 规则不再需要；高饱和食物加 gula 改成原版食物静态表；原版物品的罪孽要素放在 `vanilla_sins.json`，装了 Forbidden Magic 时不加载（同原版行为）。实体为 `required: false`，S3 注册实体后生效。
- 语言：旧 TC6 `research.*`（含扫描提示）全部删除，`<PAGE>` 拆成多页，`<BR>`/`<LINE>` 保留（TC4R 支持）。研究背景转为 512×512 PNG。

### S3 与上游的偏差

- 守墓猫、守护黑豹以 `Cat` 为基类（1.20.1 只有猫可驯服），用猫模型；黑豹恒为驯服、无主人时只是中立护卫，主人留给 S4 注魔 `tame(player)`；皮肤生成时随机 guardian1–3（上游实际恒为 guardian1）。
- 羊蛛：类别改为 CREATURE，不在和平/远离时消失、不妨碍睡觉、生成时去掉骷髅骑手与药水效果。
- 腐化猪：吃 `thaumcraft:taint` 标签方块（腐化土/草→泥土，结壳腐化→石头，其余→空气），按 TH 净化该列生物群系；TC4R API 无净化接口，唯一的内部调用集中在 `common/compat/TaintBridge`。目标改为 `thaumcraft:taint_spider`。
- 果冻兔：TC4 无区块灵气，生长不再抽 vis（上游本就不看抽取结果）；vitium 兔改为 `FluxApi.consumeNearby` 吸收附近通量。剥一层后 20 tick 无敌，防止火焰/仙人掌每 tick 剥层。ignis 兔名称键改为能命中的 `bunnignis`。
- 矿猪：矿物表由 `forge:ores/*` 与 `forge:nuggets/*`（无粒则 `forge:dusts/*`）推导，每种材料一条；主矿/其他权重进 `isorropia-common.toml`（默认 1/1 同上游）。`taint_eat_cooldown` 同在该文件。
- 守墓猫的圣光束用 `ThaumcraftClientEffectsApi.spawnEssentiaTrail` 代替 TC6 beamBore。刷怪蛋配色取 TH 原型（上游全黑）。
- 悬挂标签：手持 TC4R `thaumcraft:jar_label` 右键方块侧面放置，需研究 `isorropia:hanginglabel`，罐子（`EssentiaJarView`）上不放。
- 子类实体的战利品表引用原版表（猪/蜘蛛/羊/鸡/兔/牛）；矿猪掉 1–3 石头。
- 未移植：`EntityAILigo`、`EntityAIZapAttack`、`ModelGuardianPanther`（上游均无引用）。

### S4 与上游的偏差

- **治疗缸成型**：用法杖右键未成型的框架（`data` 无需注册表；TC4R 没有“法杖右键 → 多方块”扩展点，所以走 Forge 的 `RightClickBlock` 事件，在法杖自己处理之前接管），需研究 `isorropia:curativevat`，扣水/土/秩序各 50 vis（照 TH 原值）。结构照 TH：上下层大木木板环 + 奥术构造体中心，中间两层玻璃环 + 两格水源；成型后中间两格中心变成 `vat_interior`。
- **配方改为数据驱动**：`data/isorropia/isorropia/creature_infusions/*.json`（19 条）与 `self_infusions/*.json`（10 条），字段见 `common/infusion/*.java`；物品/要素替换沿用 S2 的表，另把 1.12 染料 meta 还原成 1.20 的染料物品（0=黑、1=红、2=绿、3=棕、4=蓝、11=黄、15=白）。
- **要素输入**：缸只吸收“当前需要”的要素（`demand`），从相邻 `EssentiaTransport` 主动抽 1 点；缸本体与四个端口都实现 `EssentiaTransport`，端口转发给主方块实体。
- **vis**：改为 TC4R 的 `VisNetworkApi.drain`（单位 centivis），在缸上方矩阵所在位置按 recipe 的通道抽取；TH 的“抽不到 vis 就损血”没做，改成周期性的不稳定事件。
- **不稳定事件**：保留上游 13 类（喷出基座物品/电弧/负面药水/给玩家加扭曲/爆炸/伤害缸内生物），概率与权重按上游；`!INSTABILITY` 研究在 TC4R 里不存在，故只加扭曲、不改进度。
- **天体灵气**：`CelestialBody` 按 TC4 的日月相位判定；因为 TC4R 没有区块灵气，聚合改成“每周期采 1 点，攒够整份再继续”。
- **灵魂罐**：`isorropia:jar_soul` 物品现在就能用——把生物装罐（消耗 TC 的 `warded_jar`）后对缸右键即放入缸内；罐方块的实体渲染仍留到 S6。
- **自我注魔**：11 项里 9 项被动能力已实现（`common/infusion/SelfInfusions`）；变形手指的便携工作台界面（S6）与按键开关（S5）未接——`climbDisabled`/`invisibilityDisabled` 字段已就位。上游“失败也把成果写进缸”的 bug 没有照抄，只有成功才记入。
- **肿瘤扭曲**：把玩家扭曲（permanent/sticky/temporary）分档吸进肿瘤、上限 50，身体死亡时原样还回并清空自我注魔（照上游）。
- **收容焦点**：未做（S6）。
- **研究页**：治疗缸的蓝图页（COMPOUND_CRAFTING）已加（S7 收尾）——TC4R 的 `compound_blueprints` 只对内置的 9 个 handler 真正成型，所以本模组的蓝图只做展示，成型仍由法杖右键事件处理。水格用水桶表示。

### S5 进度明细

已完成（均已在 DEV 客户端实机验证）：

- **透镜系统**：`common/lenses/{Lens,AirLens,FireLens,OrdoLens,EnvyLens,Lenses,LensManager,EnvyStealing}.java`。
  透镜存在**护目镜自身的 NBT**（`isorropia:lenses:{left,right}`）里，与 1.12 一致——护目镜是挂载点，没有护目镜透镜无用。
  火透镜给夜视（装上即生效、卸下即移除）、学者透镜用 TC4R 的 `ScanManager` 研究视线目标（包在 `compat/ScanBridge`）、
  嫉妒透镜把生物的掉落「看」出来（`EnvyStealing` 真滚一次掉落表）、基里安透镜纯客户端绘制。
  网络：`LensChangePacket`（装配/卸下，服务端校验护目镜与物品）。
- **透镜轮盘**：`client/hud/LensWheel.java` + `client/LensKeys.java`（默认 **J**）。按住打开、鼠标指向选片、潜行则清空两槽；
  环用 `fill` 画（上游用的是 TC 的 radial 贴图，不属于本模组）。**实机已验证**装配与效果，`--gui` 截图确认轮盘像素。
- **躯体大脑**：`ItemSomaticBrain`（Curios 头部）、`ModelSomaticBrain`、`SomaticBrainCurioRenderer`；
  本模组自带 `data/isorropia/curios/{slots/head.json,entities/player.json}` 声明头部槽位（TC4R 只声明了 ring/necklace/belt）。
  1.12 的变换原点与现代 `head.translateAndRotate` 不同，落点按实机第三视角截图定准（`SomaticBrainCurioRenderer` 里一处常量）。
- **能力字段**：`LivingData` 增 `envy`/`hasLooted`（嫉妒透镜 HUD 的前置）并进同步包。嫉妒值记在**被看生物自己**的数据上、`syncTo` 给佩戴者。
- **世界内叠加**：`client/fx/LensWorldFx.java` 在 `RenderLevelStageEvent.AFTER_LEVEL` 里画基里安涟漪/漩涡（活物/亡灵/异界三种配色与旋向）
  与学者透镜的目标名牌；用 TC4R 的 `ThaumcraftClientRenderApi.beginAfterLevelWorldRender` 恢复世界矩阵与深度态。
  世界通道**不依赖 HUD 可见性**，所以它同时负责把目标写进 `LensClientState`（按 F1 隐藏 HUD 时名牌仍在）。
- **屏幕读数**：`client/hud/LensHud.java`（注册在 HOTBAR 之上）——学者透镜在准星处画目标名牌与要素行
  （生物走 `AspectQueryApi.entity` 只画要素、方块走 `placedBlock`、方块上的掉落物走 `item`），
  嫉妒透镜在右下角画 `invidia_hud.png` 进度条（绿色填充自下而上，按 `envy / maxHealth × 5.15` 折算）。
  已知偏差：`AspectQueryApi.block(state)` 对原版放置方块返回空，必须用 `placedBlock(stack, state)`（见 VALIDATION）。

未完成：

- 变形手指（`morphicFingers`）的便携奥术工作台界面——按计划属 S6。
- 躯体大脑的「知识拦截」：1.12 只有头部饰品渲染，没有拦截逻辑，本移植同样不做。

### S6 进度明细

已完成（均在 DEV 客户端实机验证）：

- **变形手指**：`common/menu/{MenusIS,MorphicFingersMenu}`、`client/gui/MorphicFingersScreen`、`OpenFingersPacket`、
  `client/MorphicKeys`（默认 **M**）。长了手指（自我注魔 `morphic_fingers`）的玩家按 M 打开**便携奥术工作台**：
  匹配 TC4R 的奥术配方；vis 先从玩家所在的 vis 网络抽（与治疗缸同一套），不够的部分由六个要素水晶槽补（一颗水晶 = 一点 vis），
  界面沿用 TC 工作台的要素环；关闭时格子里的东西全部掉出（上游也不保存）。服务端收包后再查一次手指，客户端说了不算。
- **灵魂信标复生**：`common/infusion/SoulBeacon`。绑定的玩家受致死一击时，先照常结算旧身体（还肿瘤扭曲、清空自我注魔），
  然后若信标仍在、其下方治疗缸里有待命身体（`MODE_SELF_READY`），就取消死亡：原地炸开并洒通量，背包与饰品全掉，
  传送进缸、获得新身体的能力、被缸收着（右键出缸），信标解绑（一次性）。
  顺带修了缸挪动玩家的方式（改走 `connection.teleport`），之前玩家进出缸都只改了服务端坐标、会被客户端拉回。
  维度改按名字存（上游存数字 id，原移植误用了 `hashCode`）。

与上游的偏差：

- 上游复生后会在客户端把物品栏数组清空（修补它自己的不同步）；1.20.1 的 `dropAll()` 会正常同步，不需要。
- 撤掉了一条非上游的捷径：玩家钻进带待命身体的缸再出来就能拿走能力（上游只有复生这一条路）。

- **收容焦点**：`common/items/ItemFocusContainment`（实现 TC4R 的 `FocusIdentityItem`，升级存在物品 NBT）、
  `common/focus/ContainmentFocus`（经 `FocusActionApi` 注册的持续施法动作）、`client/fx/ContainmentFx`（TC4 的持续法杖光束）、
  焦点档案 `data/isorropia/thaumcraft/focus_catalog/focus_containment.json`（**文件名必须等于焦点 id 的 path**，否则 TC4R 拒绝加载整个目录）、
  免疫标签 `data/isorropia/tags/entity_types/containment_immune.json`、注魔配方 `recipes/infusion/focus_containment.json`。
  机制照 Thaumic Horizons（TC4 原型）：按住右键发出 10 格光束，每 tick 付 aer+perditio 各 10 centivis（与法杖上的节俭升级、杖帽折扣联动）；
  光束落在生物上时累积「收容量」，首 tick 为 2+效能/2，之后每 tick 2+效能/3，同时每 tick 衰减 1；
  超过**生物当前血量 ×20** 即装罐（所以先打残更快，与研究文字一致），消耗一个空守卫罐（或空的灵魂罐；创造模式不耗）。
  无空罐时光束立即停止并提示。音效照 TH：抓住生物期间每 1.2 秒 `jacobs`，装罐时 `craftfail`。配方照 TH：传送门焦点 + 琥珀×4、蛛网×2、铁栏杆×2，
  vacuos/vinculum 各 32、iter 8，不稳定度 6。
- 顺带修了灵魂罐的一个隐患：找「可用的罐子」时会把**已装了生物的灵魂罐**也算进去并消耗掉，等于把里面的生物删了；现在只认空罐。

与上游的偏差（收容焦点）：

- 1.12 的收容是 TC6 的「焦点效果」（在焦点操纵台里组合），TC4R 没有这套体系，所以照 TC4 原型做成独立焦点物品。
- 收容量按「玩家 + 目标」记：换目标就从零开始（TH 只按玩家记，换目标会继承进度，属可钻的空子）。
- TH 的 `slow` 升级（给目标上缓慢）没做：需要新增一个焦点升级定义及其图标，收益很小。五个等级都只开放节俭与效能。
- **装罐的传送门特效不做**：TH 的 `FXContainment` 在批次外加顶点、随后被自己的 `startDrawingQuads` 清掉，原版里从来没显示过；1.12 版本的特效函数是空的。
  装罐只有声音，与玩家当年实际看到的一致。

- **改造矩阵**（逐项对照 1.12 后修正）：法杖第一下开缸（启动音量 0.5）；之后潜行且有存量通量则排通量，否则先重新清点稳定器再开始注魔；
  找不到可注魔的东西时**什么也不做、缸保持开启**（之前自创了关缸、提示语和失败音效，已删）。拆掉矩阵时：注魔中则缸解体，
  无论如何缸都停下且启动量立即归零（之前只在缸开着时处理、且启动量靠自然回落）。
  稳定度改为上游的四档（极稳/稳定/不稳/极不稳，以 ±25 与 12.5 为界），每周期侵蚀 = 配方不稳定度 ÷ 档位系数（5/6/7/8），之前固定除以 6。
- **矩阵的护目镜读数**：1.12 戴护目镜看矩阵时会显示稳定度档位、存量通量、天穹灵气进度、每周期对称回补与最大损耗；
  TC4R 的护目镜浮层不允许方块追加文字，所以由 `client/hud/MatrixReadout` 自绘在准星下方。对称回补与配方不稳定度随缸同步给客户端。
- **灵魂罐**（逐项对照 1.12 后修正）：补上「手持空守卫罐右键缸 → 把缸里的生物装回灵魂罐」（上游有、之前缺）；
  罐名改由物品按内容生成（`ItemJarSoul#getName`，「Jarred 猪」），不再给物品盖一个固定的自定义名字（之前拆罐掉落时名字还显示成 `minecraft:pig`）；
  法杖打开放置的罐子时生物出现在罐子原位（之前在上方一格）。

与上游的偏差（矩阵与灵魂罐）：

- 矩阵只能放在缸的**主方块**（顶层中心）上方；上游允许放在任意缸方块上，但只有放在主方块上才真正能用。
- 注魔中拆矩阵：本移植的缸解体会掉出已消耗的原料并杀死缸内生物、喷出通量；上游是爆炸并污染灵气，TC4R 没有区块灵气，故不照搬。
- 从缸里装回罐子时不允许在注魔进行中操作（上游没拦，会让注魔失去对象）。
- 物品形态的灵魂罐不画罐内生物：上游的物品渲染也只画罐子本身。

## 范围外新增：TH 独有的内容

上面各阶段只移植 **Isorropia 已有的功能**。TH（GTNH 1.7.10）另有约 7 个整块系统是 Isorropia 从未承接的
（位面、血液注射、灵魂机器、能量节点、自定义傀儡、重构器、材料晶体等），本节记录**从 TH 补进来的、超出原范围的**内容。

### 照明焦点（`isorropia:illumination`）

来源：TH `common/items/ItemFocusIllumination.java`、`common/blocks/BlockLight.java`、`BlockLightSolar.java`、
`common/tiles/TileLight.java`；研究 `focusIllumination`（TH `ThaumicHorizons.java:1721-1739`）。

- **焦点**：`ItemFocusIllumination`（`FocusIdentityItem`）+ `common/focus/IlluminationFocus`（TC4R `FocusActionApi`，激活方式 `USE_BLOCK`，一次性施法）。
  对准方块面右键，在其相邻空气格留下一个「光珠」；消耗 **ignis 100 + aer 100 centivis**（TH 原值），frugal 折扣经 TC4R 的 `vis_discount` 参数生效。
- **光珠方块** `isorropia:light`：小方块（0.3–0.7），无碰撞、无掉落、即破、可被任何方块直接替换；发光 14（普通）/15（solar）。
  16 色调色板与 TH `ItemFocusIllumination.colors` 完全一致，颜色经 `ClientSetup#onBlockColors` 用 `tintindex` 上色，方块与物品不会各说各话。
- **solar 升级**：TH 的 `FocusUpgradeType.solar`（ordo 8 + vacuos 4），第 3 阶可选；装上后光珠变成 solar 光珠，走进去的亡灵着火 5 秒（照 TH）。
- **配方**：TH 是**坩埚**配方（萤石 + lux 12 / potentia 8 / ignis 8），照搬为 `isorropia:crucible/focus_illumination`。
- **研究**：`isorropia:illumination`，父研究 `FOCUSFIRE`，CONCEALED，坐标 (-1, 6)。

与 TH 的偏差：

- **未做染料换色**：TH 用 `RecipesFocusIlluminationDyes`（染料 + 焦点 → 改 metadata）给 16 种颜色。
  本移植把颜色放进了焦点的 property（`isorropia:colour`，默认 11＝黄，即 TH 出货色），但**没有做换色配方**，
  所以玩家暂时只能得到默认色；方块侧的 16 色与 `BlockColors` 已就位，加配方即可开放。
- **未做光珠粒子/辉光渲染**：TH 用 ISBRH 画发光球体、并在 `ClientProxy.illuminationFX` 里撒粒子；
  本移植用一个小立方体模型 + 颜色 tint 代替，观感接近但不是逐像素还原。实机外观仍需人眼确认。
- **方块与太阳光珠合并为一个方块**（TH 是 `BlockLight` + `BlockLightSolar` 两个类），靠 `solar` 属性区分。

### 液化焦点（`isorropia:liquefaction`）

来源：TH `common/items/ItemFocusLiquefaction.java`；研究 `focusLiquefaction`（TH `ThaumicHorizons.java:1673-1695`）。

- **焦点**：`ItemFocusLiquefaction`（`FocusIdentityItem`）+ `common/focus/LiquefactionFocus`（TC4R `FocusActionApi`，
  `USE_AIR`/`USING_TICK`/`STOP_USING`，`CONTINUOUS` 持续光束）。按住右键持续输出一条灼热光束。
- **光束效果**（按 TH 的 `processBlock` 优先级）：
  - **熔融**：石→岩浆（慢）、冰/浮冰/蓝冰/霜冰→水（下界为空气）、草方块/菌丝/土径→泥土、TNT→引爆、雪块/树叶/花/树苗/作物/藤蔓/蛛网→空气。
  - **熔炼**：其余有熔炉配方的方块按熔炉结果产出（方块变方块或掉物品）。
  - **点燃**：可燃烧的方块被点燃。
  - **掉落物**：地上的物品实体被就地熔炼（如粗铁→铁锭）。
  - **生物**：只受少量火焰伤害（`1 + 0.5×potency`），并着火 4 秒，并非武器。
- **破坏进度**：照 TH，光束先在方块上「积累」一个与硬度相关的进度（石头 20、可熔/可燃 0.25、普通熔炼 2.0），
  每 tick 累加 `0.15 + 0.05×potency`，满则生效；换目标即清零。进度按玩家记在服务端。
- **升级**：`frugal`（折扣）、`potency`（加快、加伤）、`enlarge`（第 3 阶，扩到 3×3×3 邻域，逐格收费）、
  `nuggets`（第 1/2/4 阶，按 TH 的 45%/级掉地狱熔炉式额外产物）、`purity`（第 5 阶，把矿石净化为原生晶簇而非普通金属）。
- **消耗**：每熔掉一格 **ignis 40 centivis**（TH 原值），打生物改走命名成本 `critter`（ignis 15）。
- **配方**：TH 是**注魔**配方（不稳定度 3，ignis 12 / metallum 8 / permutatio 4；催化物钻石；材料 火焰焦点 + salis mundus + 挖掘焦点），
  照搬为 `isorropia:infusion/focus_liquefaction`。
- **研究**：`isorropia:liquefaction`，父研究 `isorropia:illumination` + `FOCUSEXCAVATION`，CONCEALED，坐标 (1, 8)。

与 TH 的偏差：

- **`nuggets` 走 TC4R 20732 的地狱熔炉配方表**：TH 调 `ThaumcraftApi.getSmeltingBonus(stack)` 取单条奖励；
  20732 已改成数据驱动的 `thaumcraft:infernal_smelting` 配方（`bonus: true` 的产物）。本移植扫描 `TCRecipes.INFERNAL_SMELTING`
  里匹配该方块的配方、取其中的 bonus 产物，因此**不区分 append/replace 模式与优先级**——奖励表本身是逐条附加的，实际效果一致。
- **`purity` 直接掉原生晶簇，不再代玩家二次熔炼**：TH 会拿晶簇再查一次熔炉配方。TC4R 里晶簇的熔炼是 `thaumcraft:common_metal_smelting`
  自定义配方（晶簇→2 锭），本移植改用 `ArcaneBoreMiningResults.replace(...)`（TH `Utils.findSpecialMiningResult` 的 20732 等价物）
  直接掉晶簇，玩家自行熔炼即可得到双倍金属，也更贴合研究文本「净化矿石为原生晶簇」。
- **光束观感**：TH 用 `beamCont` + `smeltFX` 粒子；本移植依赖 TC4R `CONTINUOUS` 施法自带的预测光束，并复用 `SoundEvents.FIRE_AMBIENT`
  作光束嘶声（每 24 tick 一次）。未逐帧还原 TH 的熔炼粒子。


### 分解焦点（`isorropia:disintegration`）

来源：TH `common/items/ItemFocusDisintegration.java`；研究 `focusDisintegration`（TH `ThaumicHorizons.java:1696-1719`）。

- **焦点**：`ItemFocusDisintegration`（`FocusIdentityItem`）+ `common/focus/DisintegrationFocus`（TC4R `FocusActionApi`，
  `USE_AIR`/`USING_TICK`/`STOP_USING`，`CONTINUOUS` 持续光束）。按住右键持续输出一条紫色光束。
- **光束效果**（照 TH 的 `processBlock` 优先级，实体优先于方块）：
  - **方块**：只有**带要素**的方块才会被分解。光束按硬度积累进度（照挖掘焦点的算法），满了之后把方块「解绑」为结晶要素——
    每个要素按点数掉出对应数量的一颗 `thaumcraft:crystal_essence`，然后方块消失（先过 `GameplayHooks.fireBlockBreak`，尊重保护/事件）。
  - **掉落物**：带要素的物品实体被整叠分解，每个要素掉出 `点数 × 叠数` 颗结晶要素。
  - **生物**：只受少量「分解」伤害（`0.5 + 0.25×potency + 0.5×enervation`，`ThaumcraftDamageTypes.dissolve`）。
- **进度**：与挖掘焦点一致——首次瞄准新方块的那一 tick 只记录目标、不加速度；之后每 tick 累加
  `0.05 + 0.015×potency`，满则生效；换目标即清零。进度按玩家记在服务端，并用负的 `destroyBlockProgress` 覆盖层给施法者显示裂纹。
- **升级**：`frugal`（折扣）、`potency`（加快、加伤）、`enlarge`（第 3 阶，主方块破后连带周围一整壳方块，逐格收费）、
  `enervation`（第 5 阶，本模组新增升级，命中生物附加 2 秒缓慢 I）。
- **消耗**：每分解一格 **ignis 30 + perditio 30 + terra 30 centivis**（TH 原值），打生物改走命名成本 `critter`（ignis/perditio/terra 各 12）。
- **配方**：TH 是**注魔**配方（不稳定度 7，perditio 32 / praecantatio 24 / vitreus 32 / permutatio 16；催化物挖掘焦点；
  材料 液死桶 ×2 + 原始护符 + 炼金煤），照搬为 `isorropia:infusion/focus_disintegration`。
- **研究**：`isorropia:disintegration`，父研究 `isorropia:liquefaction` + `LIQUIDDEATH`，CONCEALED，坐标 (3, 8)。

与 TH 的偏差：

- **去掉 `alchemite` 父研究**：TH 的父研究是 `focusLiquefaction` + `alchemite` + `LIQUIDDEATH`。`alchemite` 是 TH 自有材料，
  本整合包没有对应物，故仅保留 `isorropia:liquefaction` + `LIQUIDDEATH`（液死是 TC4R 原生研究）。
- **结晶要素物品用 TC4R 的组件式实现**：TH 用 `ItemCrystalEssence` 的 NBT 存单个要素、靠**堆叠数**表示点数；本移植用
  `ThaumcraftItemApi.createCrystalEssence(aspect)` 生成组件式结晶，再把 `ItemStack` 的 `count` 设为点数，语义一致。
- **不可摧毁的产出物**：TH 用 `EntityItemInvulnerable`（重写 `attackEntityFrom` 返回 false）。1.20.1 无该子类，改用普通
  `ItemEntity` + `setInvulnerable(true)`。
- **光束不吞自己的产出**：TH 的 `getPointedEntity` 显式跳过 `itemCrystalEssence`，本移植的 `pointedEssence` 同样排除结晶要素，
  避免光束反复分解刚生成的结晶。
- **光束观感**：同液化焦点，依赖 TC4R `CONTINUOUS` 自带光束，并复用 `TCSounds.ZAP` 作光束噼啪声（TH 原为 100ms 间隔，本移植
  放慢到 5 tick 一次）。未逐帧还原 TH 的 `disintegrateFX` 粒子。



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
| S6 | 改造矩阵、灵魂罐、灵魂信标、收容焦点、变形手指 | |
| S7 | 客户端特效与渲染收尾；集中实机验证 | |

每个阶段：实现 → `gradlew build` → 修复 → 提交。实机测试（runServer/runClient/探针）集中在批次末尾一次做。

## 渲染与特效（跨阶段）

渲染这块单独成篇，因为它是「不跑客户端就发现不了」的重灾区：

- 逐区域**规格**在 [render-spec/](render-spec/)（00 是共享的 TC4R/Forge 渲染参考，01–07 是 vat/matrix/灵魂罐与信标/粒子特效/实体审计/透镜与 HUD/物品模型），
  每条都带 1.7.10 与 1.12 的行号、精确几何与混合状态、以及「该照哪个上游」的判断。
- **现状**（做了什么、与规格的差在哪、客户端验证的硬限制）见 [RENDERING.zh-CN.md](RENDERING.zh-CN.md)。
- 本环境 `Screenshot.grab` 只截世界不截 GUI：世界内渲染可用截图验收，HUD 只能靠「探针读状态 + 人眼」。

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
- **研究页**：治疗缸的蓝图页（COMPOUND_CRAFTING）仍未加——TC4R 的 `compound_blueprints` 只对内置的 9 个 handler 真正成型，第三方条目仅展示，留到 S7 做展示页。

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


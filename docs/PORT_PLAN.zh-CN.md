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
| S1 | 方块/物品/音效/创造栏注册，资源迁移（贴图改路径、lang→json、blockstate/模型重写），原版配方、战利品表 | |
| S2 | TC 数据：要素、物品/实体要素、研究树、坩埚/奥术/注魔配方 | |
| S3 | 14 个实体 + AI + 属性 + 渲染器/模型 | 完成（偏差见下） |
| S4 | 治疗缸多方块 + 生物注魔/自我注魔 + 能力（capability）+ 网络包 | |
| S5 | 透镜系统 + 躯体大脑（Curios）+ 按键 + HUD | |
| S6 | 改造矩阵、灵魂罐、灵魂信标、收容焦点、变形手指 | |
| S7 | 客户端特效与渲染收尾；集中实机验证 | |

每个阶段：实现 → `gradlew build` → 修复 → 提交。实机测试（runServer/runClient/探针）集中在批次末尾一次做。

### S3 与上游的偏差

- 守墓猫、守护黑豹以 `Cat` 为基类（1.20.1 只有猫可驯服），用猫模型；黑豹恒为驯服、无主人时只是中立护卫，主人留给 S4 注魔 `tame(player)`；皮肤生成时随机 guardian1–3（上游实际恒为 guardian1）。
- 羊蛛：类别改为 CREATURE，不在和平/远离时消失、不妨碍睡觉、生成时去掉骷髅骑手与药水效果。
- 腐化猪：吃 `thaumcraft:taint` 标签方块（腐化土/草→泥土，结壳腐化→石头，其余→空气），按 TH 净化该列生物群系；TC4R API 无净化接口，唯一的内部调用集中在 `common/compat/TaintBridge`。目标改为 `thaumcraft:taint_spider`。
- 果冻兔：TC4 无区块灵气，生长不再抽 vis（上游本就不看抽取结果）；vitium 兔改为 `FluxApi.consumeNearby` 吸收附近通量。剥一层后 20 tick 无敌，防止火焰/仙人掌每 tick 剥层。ignis 兔名称键改为能命中的 `bunnignis`。
- 矿猪：矿物表由 `forge:ores/*` 与 `forge:nuggets/*`（无粒则 `forge:dusts/*`）推导，每种材料一条；主矿/其他权重进 `isorropia-common.toml`（默认 1/1 同上游）。`taint_eat_cooldown` 同在该文件。
- 守墓猫的圣光束用 `ThaumcraftClientEffectsApi.spawnEssentiaTrail` 代替 TC6 beamBore。刷怪蛋配色取 TH 原型（上游全黑）。
- 悬挂标签：手持 TC4R `thaumcraft:jar_label` 右键方块侧面放置，需研究 `isorropia:HANGINGLABEL`（S2 研究 key 须一致），罐子（`EssentiaJarView`）上不放。
- 子类实体的战利品表引用原版表（猪/蜘蛛/羊/鸡/兔/牛）；矿猪掉 1–3 石头。
- 未移植：`EntityAILigo`、`EntityAIZapAttack`、`ModelGuardianPanther`（上游均无引用）。

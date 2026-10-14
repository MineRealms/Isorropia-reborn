# Isorropia 多版本构建方案（1.20.1 Forge + 1.21.1 NeoForge）

本文记录把 Isorropia 从单版本 ForgeGradle 6 仓库迁移到 Stonecutter 多版本仓库的过程，
方法论与 Technomancy 的 `docs/MULTIVERSION-BUILD-PLAN.zh-CN.md` 相同，这里只记 Isorropia
自己的测量结果、偏差和踩坑。

工具链：Stonecutter 0.9.8 + ModDevGradle 2.0.148（`net.neoforged.moddev.legacyforge` /
`net.neoforged.moddev`）+ Gradle 9.4.1。根目录 `src/` 为规范源码（1.20.1 方言），
每个 Stonecutter 节点位于 `versions/<节点>/`，只放 `gradle.properties`。

## S0 — 仓库重构，只保留 1.20.1 节点（已完成）

改动：`settings.gradle` 声明 `1.20.1-forge` 节点；`build.gradle` 改为共享脚本，Forge 分支
换用 MDG legacyforge；属性拆分为根 `gradle.properties`（模组元数据）+ 节点
`versions/1.20.1-forge/gradle.properties`（加载器/MC/依赖版本）；`stonecutter.gradle`
注册 json/mcmeta 处理器与 `buildAndCollect`。

### S0 门禁结果

| 门禁 | 结果 |
|---|---|
| `:1.20.1-forge:build --offline` | 绿（reobfJar 由 MDG 自动执行，勿手写 `finalizedBy`） |
| JUnit | 13/13 绿（DuoWeightedRandomTest 5、AspectNbtTest 3、VatStructureTest 5） |
| `tools/validate_tc_data.py` | 61 研究 / 59 配方 / 0 错误 / 1 既有警告（`#forge:crops`） |
| jar 内容 parity vs FG6 基线 | 894/894 条目，880 项字节一致；差异仅为 MANIFEST + 下述 13 个类 |

### 基线 parity 的两个坑（都是真的，都已解决）

1. **换行符污染 jar。** `core.autocrlf=true` 的新 worktree 会把资源 checkout 成 CRLF，
   而 `processResources` 对资源是逐字节复制，CRLF 直接进 jar。修法：`.gitattributes`
   固定 `eol=lf`（bat/ps1 除外）并重排 worktree。注意 parity 基线必须用同样方式
   normalize 过的干净 checkout 重建，否则两边方向相反、差异更多。
2. **javac 向类里塞 super 转发覆写。** 13 个类（9 个实体渲染器 + 3 个可繁殖实体 +
   `isorropia.mixins.json` 之外的 MANIFEST）在 FG6 产物里多出
   `m_6512_`（shouldShowName）×2 与 `m_142606_`（getBreedOffspring）×1 的
   protected/public 转发覆写，方法体只有 `super` 调用——源码里从来没有这些覆写
   （全历史 `git log -S` 为空）。FG6 对本地 ForgeFlower 反编译再 javac 重编译的
   vanilla jar 编译时，javac 生成了这些转发；MDG 用预编译 official 映射 jar，不再生成。
   用 javap 归一化常量池后逐一比对：**仅删除、零新增**，虚分派会落到同一个 super 实现，
   行为等价。此 13 项作为 MDG 迁移的已论证偏差记录，不追平。

### S0 附带改动

- `tools/client_probe.py`：`gradlew runClient` → `:1.20.1-forge:runClient`（Stonecutter
  的任务在节点项目上）；bridge 仓库定位改为向上逐级查找（worktree 比主仓库深一层）。
- **Rosetta bridge 兼容坑。** bridge 1.0.0 的 `findMinecraftDirectory` 在
  `minecraft.gameDir` 和 `--gameDir` 都取不到时会按 classpath 找 `.minecraft` 目录——
  MDG 的 classpath 里每个 `net.minecraftforge\...` 缓存条目都含 `.minecraft` 子串
  （来自 `.minecraftforge`），逐级向上走到盘符根时 `getFileName()` 返回 null 且
  1.0.0 没判空 → 构造期 NPE，mod 加载失败。FG6 靠自己的 `--gameDir` 处理命中第二分支
  所以没炸。修法：MDG 的 client run 里显式
  `systemProperty 'minecraft.gameDir', <game dir>`，让第一分支命中（bridge 仓库的
  1.0.1 源码已加判空，但其 build/libs 里的 jar 是 NeoForge 1.21.1 变体，不能用于
  Forge 1.20.1）。
- **客户端启动门禁通过**：`client_probe.py start` 65 秒后 bridge 上线；
  `probes/client/270_s0_boot.java` 在 MDG 编译产物的运行时里确认
  22 blocks / 61 items / 16 BE types / 19 entity types 注册齐全。

## S1 — 接入 1.21.1-neoforge 节点（已完成）

- `settings.gradle` 加 `versions(['1.21.1-neoforge': '1.21.1'])`；`build.gradle` 加 NeoForge
  分支（核心 MDG：mod 依赖用普通 `implementation`；`unitTest { enable(); testedMod }`）。
- TC4R neoforge `0.1.0-20736` jar+pom 从 Technomancy 的 local-repo 装配，SHA-256 与
  `local-repo/DEPENDENCIES.1.21.1-neoforge.json` 核对一致；Curios `9.5.1+1.21.1`；
  TerraBlender 用 Modrinth 文件 id `6e8GCrLb` 钉死。
- client run 指向 `run-client-1.21.1`、bridge 端口 48792（两节点不共端口）；数据生成输出到
  `src/generated/resources-1.21.1/`（防两节点 runData 互相覆盖）。

### S1 误差面测量（`:1.21.1-neoforge:compileJava --offline`，未做任何源码改动）

| 指标 | 值 |
|---|---|
| 报错文件 | **126 / 217** |
| 错误行 | 1740（其中"找不到符号" 892） |
| 最大缺失族 | `net.minecraftforge.registries`(54) / `Mod`(24+18) / `net.minecraftforge.network`(24+2) / `eventbus.api`(22) / `fml.common`(22) / `api.distmarker`(16) / `net.minecraftforge.event*`(14+10+8+…) / `ForgeRegistries`(14) / `TickEvent`(12) / `ForgeConfigSpec`(10) / `NetworkEvent`(10) / `PacketDistributor`(8) / Curios API(12) / 客户端事件与渲染事件(12+6+4+…) |

全部是预期中的 Forge→NeoForge API 迁移面，与 Technomancy S1 的家族一致；没有结构性意外
（无 gson/vanilla 映射漂移类错误——那些在 MDG 下不可能出现，两节点共用 official 映射）。
门禁：`:1.20.1-forge:build --offline` 在双节点注册后仍然绿。

## S2 — 替换层 + 资源层（已完成）

- `stonecutter.gradle` 替换表（整个 `replacements {}` 在 `if (!neo) return` 之后，全部只作用
  于 NeoForge 节点）：eventbus→`net.neoforged.bus.api`、distmarker→`net.neoforged.api.distmarker`、
  `fml.`→`net.neoforged.fml.`，类改名（MinecraftForge→NeoForge、ForgeConfigSpec→ModConfigSpec、
  IForgeMenuType→IMenuTypeExtension、IForgeShearable→IShearable、RegistryObject→DeferredHolder、
  ForgeRegistries→NeoForgeRegistries、LivingHurtEvent→LivingIncomingDamageEvent、
  RegisterGuiOverlaysEvent→RegisterGuiLayersEvent、ForgeCapabilities→Capabilities +
  capabilities 包移动），最后 `net.minecraftforge.` → `net.neoforged.neoforge.` 兜底 +
  `new ResourceLocation(` → `ResourceLocation.fromNamespaceAndPath(` 正则反向对。
- `gradle/neoforge-resources.gradle`（NeoForge 节点 processResources）：pack_format 15→34、
  排除 Forge 版 `mods.toml`（节点本地 `versions/1.21.1-neoforge/src/main/resources/META-INF/
  neoforge.mods.toml` 提供 `[[mixins]]` + neoforge 依赖）、目录单数化（recipe/loot_table/…）、
  `data/forge/tags` → `data/c/tags` 命名空间迁移、TC4R 配方条件 `forge:conditions`→
  `neoforge:conditions`、配方 `result.item`→`result.id`。
- Curios 在 NeoForge 节点改为 `implementation`（与 Technomancy 不同：Isorropia 的
  SomaticBrainCurioRenderer 编译期引用 Curios API）。

### S2 门禁结果

| 门禁 | 结果 |
|---|---|
| 1.20.1 产物 parity（替换表落地后） | 仍为 13 个已论证类差异 + MANIFEST，**零漂移** |
| 1.21.1 生成树残留 `net.minecraftforge` | **0**（逐文件计数全为 0） |
| 1.21.1 资源层 | pack_format 34 ✓、neoforge.mods.toml ✓、recipe/loot_table 单数化 ✓、c 命名空间标签 ✓、59/59 配方 `result.id` ✓ |
| 1.21.1 编译错误 | 1740 → **1272**；剩余全部结构性：TickEvent 拆分(12)、NetworkEvent/SimpleChannel(12)、VanillaGuiOverlay(6)、Capability 拆分、DistExecutor、NetworkHooks、FMLJavaModLoadingContext、ModelData 等符号级错误 |

## S3 — 结构层（编译通过，2026-10-14）

规范源码保留 1.20.1 方言，差异通过 Stonecutter 条件和少量 platform 适配器处理：
物品自定义数据、配方输入、菜单打开、实体初始化、方块交互、顶点提交、网络 payload、
方块实体序列化、效果 Holder、跨维度传送与 SavedData。`tools/dual/` 中的两个脚本
提供机械迁移及 `--check` 遗漏检查。

- 两节点 `compileJava` 通过；`buildAndCollect --offline` 通过，各 13 项 JUnit 通过。
- 1.20.1 的 S3 适配器和渲染修复会改变类字节，不能沿用 S0/S2 的“仅 13 类差异”结论。
  与现存 FG6 jar 比较，运行时修复前的非 class 资源在归一化换行后仅 MANIFEST 不同；
  本轮另有明确资源修复：补图集、修正血液注入器粒子贴图、移动重叠研究节点。

## S4 — 运行时验证已完成，CI 待配置

客户端按版本隔离目录和端口：默认 Forge `run-client` / 48791；设置
`ISORROPIA_NODE=1.21.1-neoforge` 使用 `run-client-1.21.1` / 48792。
`tools/client_probe.py` 的 start/exec/shot/guishot/stop 与 `tools/client_sweep.py` 都遵从该变量。
不得把 1.21 世界交给 1.20 加载；本次 1.21 使用单独复制的探针世界，原 1.20 世界保持原版本。

### 实机发现并修复

- NeoForge TOML 段落缺换行导致模组不被识别，JUnit 执行器和客户端均加载失败。
- 血液注入器 `Object...` 权重表把 Holder 强转 MobEffect；修复解包及即时治疗的比较。
- 放大器、矩阵和漩涡跨材质缓存 VertexConsumer，1.21 图标渲染出现 `Not building!`；改为使用时取缓冲。
- 战利品引用 `name`→`value`，`looting_enchant`→`enchanted_count_increase`，补 looting enchantment。
- 熔炉配方字符串 result 改为物品对象；`thaumcraft:components`→`neoforge:components`；
  `forge:swim_speed`→`neoforge:swim_speed`。仅 NeoForge 资源处理时转换。
- 三个模型缺失图集条目/粒子纹理错误；爆裂药瓶与脑饰品研究坐标重叠。

### 探针

- `271_dual_boot`：22 blocks / 61 items / 16 BE types / 19 entity types，实际打开 VisDynamoMenu。
- `272_dual_components`：自定义数据隔离与写回、物品子类型、即时治疗、BloodInfuserMenu。
- `273_dual_item_icons`：在渲染线程逐个绘制 61 个物品，覆盖机器图标的方块实体渲染。
- 上述三项已在两版本运行通过，并查看机器 GUI 截图。
- NeoForge 另通过 45/46/53_fingers、56/57/72、114–120 共 13 项交互探针；
  56/57/72 的旧版探针 API 已改为双版本可用。启动日志不再有数据加载 ERROR 或缺失贴图。
- Forge 全量 `client_sweep.py` 首轮 90/94：117/119/120 发现 MDG 开发运行未加载 Mixin，
  加入显式 `--mixin isorropia.mixins.json` 后日志确认 hook installed；63–65、117–120
  与 271–274 共 11 项复测全部通过。首轮 65 熔炼另有时序失败，独立重跑及重启后重跑均通过。
- `274_dual_empty_hand` 从原版 ServerPlayerGameMode 入口验证空手开菜单，两版本均通过。
  NeoForge 最后一次启动后重新运行 271–274 全绿，日志无 ERROR、缺失贴图或研究坐标重叠警告。

CI 矩阵尚未落地：TC4R 是用户提供、不可重分发的本地产物，仓库清单没有可下载 URL。
自动 CI 需要预装依赖的 runner 或受控的私有依赖源；本地 `buildAndCollect` 已覆盖双节点构建与测试。

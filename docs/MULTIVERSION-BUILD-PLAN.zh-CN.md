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

## S2 — 替换层 + 资源层（计划）

- `stonecutter.parameters {}` 内的 replacements（包移动 → 类名 → vanilla 漂移正则，
  正则必须带反向对，特定规则在兜底规则之前）。
- 资源转换（参照 Technomancy `gradle/neoforge-resources.gradle`）：pack_format 15→34、
  `neoforge.mods.toml`、forge→neoforge 标签、配方 `result.item`→`result.id`、
  数据包目录单数化。
- 门禁：1.20.1 产物回到 0 差异（除 MANIFEST + 13 类）；1.21.1 产物零
  `net.minecraftforge` 引用。

## S3 — 结构层（计划）：`//? if` 条件 + 节点独有源码，两节点编译通过。

## S4 — 运行时与 CI（计划）：节点各自 JUnit；1.21.1 客户端冒烟（bridge 48792）；
`buildAndCollect` 收双 jar；CI 矩阵。

# Claude Code 交接入口

Isorropia（kedition）从 1.12.2/TC6 移植到 **Forge 1.20.1 + Thaumcraft 4 Reborn (TC4R)** 的独立工程。
先读这三个再动手：

1. [README.md](README.md) —— 工程是什么、怎么构建/运行/测试、文档索引。
2. [docs/PORT_PLAN.zh-CN.md](docs/PORT_PLAN.zh-CN.md) —— 来源与目标、已定决策、阶段表（**当前进度看这里**）、每阶段与上游的偏差。
3. [docs/VALIDATION.zh-CN.md](docs/VALIDATION.zh-CN.md) —— **怎么证明东西真的能跑**（服务端/客户端探针、截图、网格预览、数据校验）以及实机抓到的缺陷清单。改动较大时，先看它再决定验证方式。

渲染/特效相关的改动另读 [docs/RENDERING.zh-CN.md](docs/RENDERING.zh-CN.md)，精确的几何/UV/混合状态在
[docs/render-spec/](docs/render-spec/)（由 1.7.10/1.12 源码逐行分析得出，含行号）。

## 构建与测试

```bash
./gradlew.bat build                 # 编译 + JUnit；先校验 local-repo 里 TC4R 的 SHA-256
python tools/validate_tc_data.py    # TC 数据静态校验（改数据后必跑）

python tools/client_probe.py start  # 起客户端进探针世界并连 Rosetta 桥（世界内渲染可用截图验证）
python tools/rcon.py stop           # 优雅停服（run-server）
```

不常见的细节：

- `local-repo/` 的 TC4R jar/pom 不入库，缺失时从 `../Technomancy-1.20.1/local-repo` 复制（见 `local-repo/README.md`）。
- `JAVA_HOME` 是 jdk-21，PATH 里的 `java` 是 17；构建通过 toolchain 使用 17。
- TC4R API 查阅：`reference/tc4r-20721-sources`（git 忽略）。20721 与 20732 有差异时**以编译结果为准**。
- 只读类工作（找代码、读大文件、批量搜索）优先交给 opencode：`/h/MinecraftMods/.opencode/oc.sh explore '<问题>'`
  （读 `.opencode/agents/` 里的只读 agent；结果当线索，落地的结论要用定点 grep 复核）。

## 约定

- 旧代码只作行为参考，不逐行翻译；1.12 的 metadata 子类型拆成 blockstate 属性或 NBT。
- TC 机制以 `../ThaumicHorizons`（TC4 原型）为准，TC6 独有机制不硬搬；TC6 与 TC4 都有的，取「1.12 实际出货的观感/数值」。
- 引用 TC4R 内部类只能出现在 `common/compat/{ThaumBridge,TaintBridge,ScanBridge}`，并在此登记为版本升级复核点。
  - `ThaumBridge.WandTarget`：TC4R 20732 的 `common.WandInteractionTarget` 标记；升级时复核法杖 first-use 是否仍优先分派方块交互。
  - `mixin/WandItemMixin`：注入 TC4R `dev.tc4port.thaumcraft.item.WandItem` 的 `onUseTick`（开发环境名 `onUseTick`，生产 jar 为 `m_5929_`，均不重映射），
    让本模组机器接管按住的法杖。升级 TC4R 后确认启动日志有 `Wand use tick hook installed`，并跑 117–120。
    Forge 1.20.1 只认清单里的 `MixinConfigs`：开发运行读 `src/main/resources/META-INF/MANIFEST.MF`，jar 由 `build.gradle` 生成同名属性。
- 每个阶段：实现 → `build` → **实机验证**（见 VALIDATION）→ 提交；实机测试按批次集中做，不要每个小改动都开一次游戏。
- `common` 里不得引用 client 类；客户端渲染放 `client/`，服务端只发事件/包。

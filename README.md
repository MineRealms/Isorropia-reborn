# Thaumic Isorropia — 1.20.1 移植（Unofficial）

把 1.12.2 / Thaumcraft 6 的 **Isorropia（kedition）** 移植到 **Forge 1.20.1 + Thaumcraft 4 Reborn (TC4R)**。
玩法基线是 1.12 的 Isorropia；TC 机制（精华、注魔、扫描、多方块）以它的前身 **Thaumic Horizons（1.7.10，TC4）** 为准。

```text
Minecraft 1.20.1 / Forge 47.4.23 / Java 17
必需依赖：TC4R 0.1.0-20732（运行版本 4.2.3.5-1.20.1-port.0.1.0-20732）、Curios 5.14.1
可选运行时：TerraBlender 3.0.1.10
mod id：isorropia      包名：fr.wind_blade.isorropia
```

## 快速开始

```bash
./gradlew.bat build                 # 编译 + JUnit；先校验 local-repo 里 TC4R 的 SHA-256
./gradlew.bat runServer             # 专用服务器（run-server/）
./gradlew.bat runClient             # 客户端（run-client/）
python tools/validate_tc_data.py    # TC 数据静态校验（研究/配方/要素/语言键）
python tools/client_probe.py start  # 自动开客户端进探针世界并连上 Rosetta 桥
python tools/client_sweep.py        # 在同一会话里跑完全部客户端探针并逐项判定
```

TC4R 是用户提供的本地构建，**不入库**：把 `local-repo/dev/tc4port/thaumcraft-forge/0.1.0-20732/{jar,pom}`
从相邻工程（如 `../Technomancy-1.20.1/local-repo`）复制过来即可，或用 `-Ptc4Repository=<maven 根>` 指向别处。
API 查阅用的 20721 sources 解包在 `reference/tc4r-20721-sources`（git 忽略）。

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| S0 | 工程骨架、TC4R/Curios 依赖、git | 完成 |
| S1 | 方块/物品/音效/创造栏、资源迁移 | 完成 |
| S2 | 研究树、坩埚/奥术/注魔配方、物品/实体要素 | 完成（静态校验通过） |
| S3 | 14 个实体 + AI + 属性 + 渲染器/模型 | 完成 |
| S4 | 治疗缸多方块 + 生物/自我注魔 + capability + 网络 | 完成，**实机验证通过** |
| S5 | 透镜系统 + 躯体大脑 + 按键 + HUD | 完成，**实机验证通过**（透镜/轮盘/世界叠加/HUD 读数/躯体大脑落点） |
| S6 | 灵魂信标绑定/复生流程、改造矩阵交互收尾、收容焦点、变形手指界面 | 完成，**实机验证通过** |
| S7 | 客户端特效收尾 + 集中实机验收 | 完成：特效按 1.12/TH 原参数重建；一键回归 31 个客户端探针 + 6 个服务端探针全部通过 |

上述 S0–S7 是**原 Isorropia 范围**的验收记录，不代表 GTNH Thaumic Horizons 全部内容已移植。

后续 TH 扩展已加入照明/液化/分解/活化焦点、魔力与精华发电机、节点监视器、碎片嬗变和炼金炸药。
**合成节点 / 以太碎片**已完成本批验收：精华扩容、网络充能、法杖抽取、节点读数及晶体渲染；完整客户端回归 82 步通过（62 步含行为断言、20 步为冒烟检查），JUnit 13 项通过。详细边界见计划与验证文档。
已移植内容的收尾批次补了魔力发电机选择界面、法杖按住机器时不再同时施放焦点、照明焦点染色与 TH 式光珠、收容焦点 slow 升级。
位面/漩涡/口袋空间、转导放大器、血液注魔、灵魂加工机器、工作傀儡等 TH 系统仍未移植。

细表与逐阶段偏差见 [docs/PORT_PLAN.zh-CN.md](docs/PORT_PLAN.zh-CN.md)。

## 文档索引

| 文件 | 内容 |
|---|---|
| [CLAUDE.md](CLAUDE.md) | 交接入口：构建、目录、当前该做什么 |
| [docs/PORT_PLAN.zh-CN.md](docs/PORT_PLAN.zh-CN.md) | 来源与目标、已定决策、阶段表、每阶段与上游的偏差 |
| [docs/VALIDATION.zh-CN.md](docs/VALIDATION.zh-CN.md) | **验证方法**：服务端/客户端探针、截图、网格预览、数据校验；实机抓到的缺陷清单 |
| [docs/RENDERING.zh-CN.md](docs/RENDERING.zh-CN.md) | 渲染与特效：已实现项、与规格的对应、未完成项 |
| [docs/render-spec/](docs/render-spec/) | 逐区域渲染规格（00 共享 API 参考 + 01–07 分区域），由 1.7.10/1.12 源码逐行分析得出 |

## 目录

```text
src/main/java/fr/wind_blade/isorropia/
  common/     注册、方块/物品/实体、治疗缸与注魔、透镜、capability、网络、兼容桥
  client/     渲染器（vat/matrix/jar/beacon）、实体与 curio 渲染、HUD、FX、按键
src/main/resources/
  assets/isorropia/  方块/物品模型、贴图、语言、音效、OBJ（vat_redo.obj）
  data/isorropia/    研究 JSON、坩埚/奥术/注魔配方、注魔数据、要素、curios 槽位、战利品表
probes/        实机探针（server/ 与 client/），配合 Rosetta 调试桥使用
tools/         构建与校验脚本：数据校验、资源导入、注魔数据生成、网格预览、RCON、客户端探针
docs/          计划、验证、渲染文档与渲染规格
```

## 授权

原 Isorropia 与 Thaumic Horizons 的代码/素材版权归各自作者（`mods.toml` 中 `mod_license` 为 All Rights Reserved）。
本仓库是移植工作区，不含 TC4R 本体。

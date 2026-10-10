# Claude Code 交接入口

Isorropia（kedition）从 1.12.2/TC6 移植到 Forge 1.20.1/TC4R 的独立工程。先读 [docs/PORT_PLAN.zh-CN.md](docs/PORT_PLAN.zh-CN.md)（来源、已定决策、阶段进度）。

## 构建

```bash
./gradlew.bat build          # 编译 + JUnit；先校验 local-repo 里的 TC4R 哈希
python tools/validate_tc_data.py   # TC 数据（研究/配方/要素/语言键）静态校验
```

- `local-repo/` 的 TC4R jar/pom 不入库，缺失时从 `../Technomancy-1.20.1/local-repo` 复制（见 `local-repo/README.md`）。
- `JAVA_HOME` 是 jdk-21，PATH 里的 `java` 是 17；构建通过 toolchain 使用 17。
- TC4R API 查阅：`reference/tc4r-20721-sources`（git 忽略；从 Technomancy 的 20721 sources.jar 解出）。20732 与 20721 有差异时以编译结果为准。

## 约定

- 旧代码只作行为参考，不逐行翻译；1.12 的 metadata 子类型拆成 blockstate 属性或 NBT。
- TC 机制以 `../ThaumicHorizons`（TC4 原型）为准，TC6 独有机制不硬搬。
- 每个阶段结束必须 `build` 通过再提交；实机测试集中在批次末尾。

# 验证方法与实测记录

本工程的规矩：**编译通过不算完成，数据校验通过也不算完成**——TC 的集成点（注册、多方块、注魔、研究、渲染）
只有在跑起来的游戏里才能证伪。下面是可复用的验证手段，以及它们实际抓到过的缺陷。

## 一、验证手段

### 1. 快速层：`gradlew build`

编译 + JUnit。现有测试覆盖纯逻辑（治疗缸几何、矿猪掉落权重、果冻 NBT 往返、要素 NBT 编解码）。
纯 JUnit **不得触碰注册表**：`BlocksIS` 之类的 `DeferredRegister` 在无 Forge 环境下初始化会抛
`ExceptionInInitializerError`，所以测试只测纯函数。

### 2. 数据层：`python tools/validate_tc_data.py`

静态校验所有 TC 数据：研究条目字段与父研究闭合、配方引用、要素 id、物品 id（对照 TC4R jar 的注册名）、
注魔数据（`data/isorropia/isorropia/{creature,self}_infusions`：kind/organ/组件/要素/research/hook/modifiers 的属性 id）、
语言键与研究的对应关系。改数据后必跑。

### 3. 服务端实机：Rosetta 桥 + `probes/server/*`

`H:\MinecraftMods\RainJava-work\RosettaRemoteDebugBridge` 可以在**运行中的游戏里执行任意 Java 方法体**，
通过 127.0.0.1 的 TCP 控制面（token 认证）。用法：

```bash
# 1) 用 dev 映射的 jar（reobf 过的 jar 在 dev 环境会 AbstractMethodError）
cd ../RainJava-work/RosettaRemoteDebugBridge && ./gradlew.bat jar -x reobfJar --no-daemon
cp build/libs/rosetta_remote_debug_bridge-1.0.0.jar ../../Isorropia-1.20.1/run-server/mods/rosetta_bridge_dev.jar

# 2) 开服（run-server/ 下要有 eula.txt 与 server.properties；RCON 建议打开）
./gradlew.bat runServer

# 3) 跑探针：token 在 run-server/RosettaRemoteDebugBridge/remote-token.txt，端口 48790
T=$(cat run-server/RosettaRemoteDebugBridge/remote-token.txt)
python ../RainJava-work/RosettaRemoteDebugBridge/legacy/tools/rosetta_remote.py --port 48790 --token "$T" \
    exec "$PWD/probes/server/01_environment.java"
```

探针是**方法体**，必须 `return` 一个字符串（返回内容会打印）。

| 探针 | 检查 |
|---|---|
| 01_environment | 模组列表、我们注册的方块/物品/实体/BE、要素、研究条数、注魔数据条数 |
| 02_grant_and_form_vat | 用 admin context 授予研究；搭框架并成型治疗缸（原点写在 `run-server/rosetta_probes/vat_origin.txt`） |
| 03_curative_wired | jar → 管道 → 管道 → 缸的供料，缸内一头受伤的猪 |
| 04_curative_check | 03 之后约 30 秒跑：猪被治好、jar 的 victus 减少 |
| 05_vat_state | 缸的状态快照 |
| 06_creature_infusion | 基座 + 猪 + 要素 → 生物注魔完成、基座被消耗 |

### 4. 客户端实机：`python tools/client_probe.py`

客户端的工作目录是 `run-client/`，桥端口 **48791**（与服务端分开，可同时跑）。桥只在**进入世界**后才监听
（`ServerStartedEvent`），所以客户端用 `-PquickPlay=<世界>` 直接进存档。

```bash
python tools/client_probe.py start                 # 复制探针世界（若缺）→ 启动客户端 → 等桥就绪
python tools/client_probe.py exec probes/client/22_matrix_scene.java
python tools/client_probe.py shot vat_quads 64.5 302.5 58.0 0 28 4        # 世界截图
python tools/client_probe.py shot lens_wheel 64.5 300 60 0 0 1 --gui      # 连 HUD 一起截
python tools/client_probe.py guishot fingers_by_key                       # 原样截当前打开的界面（不动镜头）
python tools/client_probe.py stop                  # 优雅关闭客户端
```

`shot` 分两步：先在**服务端线程**把玩家传送/转向并强制 `ChunkSource.move()`，等若干秒后**在渲染线程**重新
设定朝向再 `Screenshot.grab`（同一帧内改姿态，避免鼠标漂移）。截图落在 `run-client/screenshots/`。

**HUD 进不进截图，取决于抓帧时的 `options.hideGui`**：Forge 在 `hideGui`（F1）为真时跳过所有
`RegisterGuiOverlaysEvent` 的绘制，而探针默认把它置真以免 HUD 挡住世界图，所以不加开关时截图**只有世界**。
`shot ... --gui` 会在抓帧前把它置回假，物品栏与自建 HUD（透镜轮盘、要素读数、嫉妒进度条）都会进图；
世界内渲染（`AFTER_LEVEL` 里的涟漪/名牌）两种模式都在。所以 HUD 像素也能截图验收，不必只靠人眼。

| 客户端探针 | 检查 |
|---|---|
| 25_lens_wheel | 装片后轮盘确实每帧在画（`LensWheel.isOpen()/hasDrawn()`） |
| 26_brain_equip | 躯体大脑装进 Curios 头部槽，第三视角截图看落点 |
| 27_lens_air | 基里安透镜：身前几只生物，截图看涟漪/漩涡 |
| 31_lens_entities | 列出会画涟漪的活物及距离（排查「为什么没涟漪」） |
| 32_lens_ordo | 学者透镜：正前方一头牛，看名牌与要素读数 |
| 33_lens_envy | 嫉妒透镜：给牛灌一半嫉妒，看右下进度条 |
| 36_block_aspects2 | 对比 `block(state)` 与 `placedBlock(stack,state)` 的要素差（原版方块只有后者非空） |
| 43_lens_drop_probe / 44_lens_ordo_drop2 | 学者透镜读准星下方块上的掉落物（名牌+要素走物品） |
| 45_fingers_open / 46_fingers_recipe / 53_fingers_take | 变形手指：长出手指并直接开界面、在格子里摆基础透镜配方、潜行取出成品并核对扣费 |
| 47_beacon_setup → 48_beacon_kill → 51_vat_exit | 灵魂信标：成型治疗缸、放好待命身体并绑定，然后致死一击；应当活着、解绑、只剩新身体的能力、被缸收着；再右键出缸，客户端与服务端坐标都在缸顶上方 |
| 49_keys → 50_fingers_key_check | 走真实按键路径：J 按住开轮盘/松开关轮盘，M 打开便携工作台；并检查两个键各只注册一次 |
| 52_containment_hold → 53_containment_check | 收容焦点：生存模式、满充能法杖装上焦点、一个空守卫罐、残血猪；按住右键（真实按键路径），之后检查猪进了灵魂罐、守卫罐被消耗、无罐后自动停手、vis 被扣 |
| 54_containment_visual | 同上但猪满血，用来截光束（`shot` 会切创造模式，创造模式不耗罐） |
| 55_containment_rules → 53 | 凋灵免疫、铁傀儡可收；无空罐时光束立刻停、不扣 vis、猪还在 |
| 56_matrix_flow | 改造矩阵：法杖第一下开缸、无可注魔时不关缸、拆矩阵则停缸且启动量归零（缸仍成型）；末尾戴好护目镜，可接 `shot matrix_readout ... --gui` 看稳定度读数 |
| 57_soul_jar_flow | 灵魂罐：手持空守卫罐右键缸把生物装回罐（罐名随内容、不带缸里的无 AI 标记）、满罐右键缸放回并退还守卫罐、法杖开放置的罐子生物出现在罐子原位 |

**探针体跑在服务端线程上**（`Thread.currentThread()` 是 `Server thread`）。所以凡是要服务端回话的操作——
客户端发包、服务端开界面、关界面——在探针返回之前都不会发生：探针里等多久都看不到结果。做法是拆成两个探针，
前一个只「按下」，后一个再「看结果」（见 49_keys → 50_fingers_key_check）。纯客户端的状态（轮盘开合）可以在
同一个探针里轮询等结果，但要**等结果而不是固定睡眠**：后台窗口帧率低，同一帧里排进去的「按下」和「松开」
会在任何一个 tick 看见之前就抵消掉。

### 5. 网格对照：`python tools/mesh_preview.py`

把 OBJ + 贴图做一次软件光栅化渲染，用来在**不进游戏**的情况下判断「三角形和 UV 是不是我以为的样子」。
治疗缸的 OBJ 就是这么定案贴图朝向与形状的；游戏渲染与预览图不一致时，问题一定在渲染状态而不是数据。

```bash
python tools/mesh_preview.py src/main/resources/assets/isorropia/models/block/vat_redo.obj \
    src/main/resources/assets/isorropia/textures/block/curative_vat.png /tmp/vat_preview.png \
    --camera "0,5.5,-6.5" --look "0,2,0"
```

### 6. 事件处理器撞名：`python tools/check_listener_names.py`

Forge 的 EventBus 把每个 `@SubscribeEvent` 方法编译成一个类，类名只由
「包名 + 声明类的简单名 + 方法名 + 事件简单名」拼成，并按这个名字缓存。两个处理器拼出同一个名字时，
后注册的会**复用**前一个的类：前一个跑两次，后一个一次都不跑，而且没有任何报错。嵌套类最容易中招
（两个不同外部类里的 `Register.onRegisterKeys` 在同一个包里就撞了）。加了事件处理器就跑一次，有撞名时退出码 1。

### 7. 服务端优雅停服

```bash
python tools/rcon.py stop        # run-server/server.properties 里 RCON 密码为 isoropia
```

强制 kill 会跳过 `ServerStoppingEvent`，任何「存盘后仍在」的验证都作废。

## 二、实机抓到的缺陷（全部已修）

这些**没有一个**能在编译期或静态检查里发现，都是跑起来才暴露的：

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 1 | 任何含治疗缸的区块在客户端加载失败，缸不可见 | 客户端用 `load()` 解析**同步用的精简 update tag**，里面没有 `Body` 字段，`CelestialBody.byName` 抛 `IllegalArgumentException` | 同步数据走独立读取器 `readSync()`；未知天体名回落 `NONE` |
| 2 | 注魔永远走不完 | 缸把要素存进 32 容量的缓冲，而配方要 50+，`essentiaStep` 永远等不到齐 | 注魔中的要素**直接抵扣需求**（上游 `addToContainer` 的语义），不再进缓冲 |
| 3 | 抽到要素后吸力不消失、管道持续灌料 | `demanded` 只增不减 | 每周期重算需求（`clearDemand`），只广告当前缺口 |
| 4 | 放生物进去后缸仍在「浸泡尸体」模式，不治病 | 模式是从多处状态翻出来的**记忆值**，会变陈旧 | 空闲模式改为每 tick 推导（有生物=治疗，有偶无生物=浸泡） |
| 5 | 注魔完成时刷屏 `Invalid message SyncLivingDataPacket` | 包类写了但从未注册 | `IsorropiaNetwork.init()` 注册 |
| 6 | 多方块渲染成一团乱三角 | 面按**三角形**写进了 `Mode.QUADS` 缓冲（每 4 个顶点被拼成一个四边形） | 每个面写满 4 个顶点（三角形重复末点） |
| 7 | 缸的正面整片消失、只剩背面 | 用的 `entityTranslucent` **带背面剔除**，而 OBJ 的绕序不可靠 | 自建 NO_CULL 半透明 RenderType（照 TC4R 的 shim 写法） |
| 8 | 灵魂信标的光柱不出现 | 天空检测用了信标**自身**那一格（被自己挡住）；增长系数乘在了贴图缩放而不是长度上 | 检测 `pos.above()`；`height = 256 × scale` |
| 9 | 受击反伤只对远程生效 | `getDirectEntity() instanceof LivingEntity` 的守卫同时也吃掉了近战 | 只对「远程」分支加守卫，反伤照所有攻击者 |
| 10 | 躯体大脑无处可戴 | TC4R 只声明了 `ring`、`necklace`、`belt` 三个 curio 槽，没有 `head` | 本模组自带 `data/isorropia/curios/{slots/head.json,entities/player.json}` |
| 11 | 嫉妒进度条永远空 | `EnvyLens` 把嫉妒累加到**玩家**的数据上，而 HUD 读的是**被看生物**的数据 | 计数改记在生物自己的 `LivingData` 上，再 `syncTo(player)` |
| 12 | 学者读数对方块显示「无要素」 | `AspectQueryApi.block(state)` 对原版放置方块返回空（它只认 datapack 声明的 block 要素） | 改走 `placedBlock(new ItemStack(state.getBlock()), state)`（原版也是问方块的**物品**要素） |
| 13 | 学者读数对地面显示「Air」 | 玩家悬空、准星 `hitType == MISS`，并非读数错误 | 探针先在脚下铺一块草方块再抓图；读数随即显示 "Grass Block" |
| 14 | M 键（变形手指）按了没反应；J 键注册了两次 | 两个按键类的嵌套处理器 `Register.onRegisterKeys`、`Poller.onClientTick` 在 EventBus 里**撞名**（见上文第 6 节），手指键的注册和轮询从未执行，透镜键的各执行两次 | 处理器改用不重复的方法名；新增 `tools/check_listener_names.py` 静态检查 |
| 15 | 灵魂信标复生后保留了旧身体的能力 | 复生判定放在了「旧身体死亡清算」之前，旧能力没被清掉、新能力叠加上去 | 照上游顺序：先还肿瘤扭曲、清空旧能力，再复生并只给新身体的能力；复生者照上游被缸收着，右键出缸 |
| 16 | 玩家右键出缸后又回到缸里（出不来） | 缸用 `entity.setPos` 挪动占用者；对玩家这只改了服务端坐标，客户端下一个移动包又把位置拉回去 | 玩家走 `connection.teleport`（`TileVat.place`），进缸与出缸都是 |

## 三、踩过的坑（写下来省下一个人一天）

- **伪玩家（FakePlayer）的研究每开一次服务器就没了**：它挂在能力上、不落盘。所有需要研究门槛的探针都要在
  **同一次会话**里先授予（见 06 的头部那段）。
- **句柄方向**：灵魂罐只从**上表面**出料（`isConnectable(face) == face == UP`），管道只**服务**不主动推。
  所以「罐 → 管 → 缸」必须是罐在下、管在上；缸要**主动抽**。贴错方向时表现为「什么都不发生」。
- **强制加载区块**：没有玩家在线时，`level.setChunkForced` 必须先做，否则缸和管道根本不 tick。
- **`getRenderBoundingBox` 在方块实体上**（1.20.1 的 `BlockEntityRenderer` 没有这个方法），BER 只覆写
  `shouldRenderOffScreen` / `getViewDistance`。
- **不要在 `common` 里碰 client 类**；BER/粒子都放 `client/`，服务端只发事件（见 `ClientFxPacket`）。
- **TC4R 会把所有命名空间的 `textures/models/` 拼进方块图集**，渲染器皮肤一律放 `textures/entity/`。
- **内部类的兼容点集中放置**：`common/compat/{ThaumBridge,TaintBridge,ScanBridge}` 是唯一允许引用 TC4R
  `*.registry/*.common/*.nativeimpl` 的地方，升级 TC4R 时先看这三个文件。
- **`System.out` 不进 `run-client/logs/*.log`**：开发环境里它只到控制台，也就是 `client_probe.py start`
  重定向出来的 `build/client-run.log`。在 `latest.log` 里找不到自己打的调试输出，不能说明那段代码没跑。
- **`shot` 会把玩家切成创造模式并飞行**（避免摔落），所以要测「生存模式才有」的消耗（罐子、物品），别在中间截图。
- **生存模式的探针要先设和平难度**：夜里开阔平台会刷僵尸，把玩家打下去摔死（"doomed to fall by Zombie"），后面的探针全读到一个死人。
- **服务端时间会「追帧」**：探针阻塞服务端线程后，服务端会连跑补帧，墙钟 0.5 秒能过 40 tick。按 tick 计的进度不要用墙钟估算。
- **测试完记得停服/停客户端**：客户端 `python tools/client_probe.py stop`，服务端 `python tools/rcon.py stop`。

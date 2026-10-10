# 06 — Overlays / HUD / 穿戴渲染（透镜、石化、躯体大脑）

> 上游指代：`IS12:` = `H:/MinecraftMods/Isorropia_kedition/src/main/java/fr/wind_blade/isorropia`（1.12，Isorropia 实际发布的版本）；`TH17:` = `H:/MinecraftMods/ThaumicHorizons/src/main/java/com/kentington/thaumichorizons`（1.7.10/TC4 原型）。
> 端口目录：`H:/MinecraftMods/Isorropia-1.20.1/src/main/java/fr/wind_blade/isorropia`。
> TC4R 客户端 API 一律引用共享参考 `00-api-reference.md` 的锚点。

## 0. 结论速览

| 元素 | 端口现状 | 参考取舍 | 目标 |
|---|---|---|---|
| 透镜轮盘 HUD（radial） | 缺（S5 未开工） | **1.12**（双透镜 LEFT/RIGHT；1.7.10 只有单透镜+LensCase） | `RegisterGuiOverlaysEvent` 自绘，`GuiGraphics` |
| AirLens 世界内涟漪 | 缺 | **1.12**（世界空间广告牌）；1.7.10 是屏幕空间投影，仅作数值旁证 | `RenderLevelStageEvent.AFTER_LEVEL` 自绘 |
| OrdoLens 扫描 HUD（名称+要素） | 缺 | **1.12**（AspectHelper+ScanningManager，TC4R 有等价 `ScanManager`） | IGuiOverlay + 复用 `ThaumometerHud` 布局 |
| OrdoLens 世界内浮动名字 | 缺 | 1.12 | `RenderLevelStageEvent` 自绘 |
| EnvyLens 进度条 HUD | 缺 | **1.12 独有**（1.7.10 无对应） | IGuiOverlay + 需服务端同步 `envy` |
| FireLens 夜视 | 缺 | 1.12 | 纯药水，无绘制 |
| GluttonyLens / LustLens | 缺 | 1.12（**空实现死代码**，已定决策 6 砍掉） | 不移植 |
| 石化全屏叠加（cobblestone） | 缺 | **1.12 独有**（TH 无） | IGuiOverlay |
| 透镜 tooltip 行 | 缺 | 1.12 | `ItemTooltipEvent` |
| 研究页 CurativeInfusionRecipe 绘制 | 缺（研究背景已迁移） | 1.12 独有，且强依赖 TC6 `GuiResearchPage` 反射 | 用 TC4R `ResearchRecipeRendererApi`（见 00-A.7） |
| `ShaderHelper` + `test.vert` / `test2.frag` | 目录不存在 | 1.12（**死代码，无任何调用**） | **不移植** |
| `CombinedTextureManager` / `CombinedLayeredColorMaskTexture` | 缺 | 1.12（**死代码，无任何调用**，且 1.12 版自身有 NPE） | **不移植** |
| Somatic Brain 头部 Curios 渲染 | 缺（物品是裸 `Item`） | **1.12 独有**（TH 无 Somatic Brain） | `ICurioRenderer`，Curios 5.14.1 |
| 护目镜要素浮标（世界内） | 缺 | 复用 TC4R | 见 §9 |

**总体判断**：本区绝大多数内容在 1.12 是自绘（TC6 `UtilsFX` 辅助 + 裸 GL），TC4R 没有可直接复用的“透镜 HUD”轮子，只有护目镜浮标（`GogglesClientEvents`，包私有，只能实现 `AspectContainerView`/`GogglesOverlayGear` 触发，不能加自定义 HUD 行）。因此这些 HUD 需要自写。

---

## 1. 触发器与数据流（1.12）

### 1.1 谁算“戴着揭示器/护目镜”

`LensManager`（IS12:`common/lenses/LensManager.java`）：

- `getGoggles(player)`（:70-89）：先遍历 **Baubles** 槽（`BaublesApi.getBaublesHandler(player).getSlots()`），找 `item instanceof IGoggles && ((IGoggles)item).showIngamePopups(stack, player)`；找不到再遍历 `player.getArmorInventoryList()`。
- `getRevealer(player)`（:91-113）：先 `getGoggles`，非空即返回；否则同样在两处找 `item instanceof IRevealer && ((IRevealer)item).showNodes(stack, player)`。
- `LENSSLOT`（:115-143）：`LEFT("LeftLens", 42.0F)`、`RIGHT("RightLens", -42.0F)`；`getName()` 即 NBT 键名 `"LeftLens"` / `"RightLens"`，`getAngle()` 即轮盘两半的旋转角。
- `getLens(revealer, slot)`（:29-34）：读 `revealer` NBT 的 `LeftLens`/`RightLens` 字符串（`ResourceLocation`），到 `IsorropiaAPI.lensRegistry` 取 `Lens`。
- `putLens` / `setLens` / `removeLens` / `changeLens`（:17-68）：写 NBT 并调 `handleRemoval`。

1.12 的透镜存在**饰品/头盔物品自身的 NBT**里（不是玩家能力）。

**TC4R 对应**（00-A.9 / `reference/.../common/RevealingEquipment.java`）：`RevealingEquipment.showsGogglesOverlay(stack,player)`（= `EquipmentAbilities.showsGogglesOverlay`，物品实现 `api.item.GogglesOverlayGear` 或数据驱动装备能力）与 `RevealingEquipment.revealsAura(stack,player)`（`api.item.AuraRevealingGear`）；`hasGogglesOverlay(player)` / `hasAuraRevealer(player)` 扫全部护甲+饰品槽（`EquippedItems.armorAndAccessories`，含 Curios）。

> 端口要保真：Isorropia 的 `ItemLens` **不是** IGoggles/IRevealer 本身，它是被插进护目镜/揭示器 NBT 的“镜片”。所以护目镜本体必须实现 TC4R 的 `GogglesOverlayGear`（`showsThaumcraftGogglesOverlay` 返回 true）才能让 `hasGogglesOverlay` 为真、从而触发透镜系统。

### 1.2 键位

`KeyHandler`（IS12:`common/events/KeyHandler.java`）：

- `keyLens = new KeyBinding("Change Goggles Lens", 36, "key.categories.misc")`（:45）→ **默认键码 36 = LWJGL `KEY_J`**（1.7.10 的 `keyV` 默认 `KEY_NONE`，见 TH17:`common/lib/THKeyHandler.java`，故 1.12 是唯一可信参考）。
- `keyMorphic = new KeyBinding("Activate Morphic Fingers", Keyboard.KEY_M, ...)`（:46）
- `keyC` / `keyX` 默认 `KEY_NONE`（:47-48）。

按下逻辑（`keyEvent`，:56-77）：`keyLens.isKeyDown()` + `inGameHasFocus` + `player != null`：
- 潜行按住 → `sendToServer(new LensRemoveMessageSP())`（卸下镜片）并 return；
- 否则 `lastPressV = now`，`radialLock = false`，若 `getRevealer(player)` 非空 → `radialActive = true`。
松开（:74-76）：`radialActive = false`。

`radialActive` / `radialLock` / `lastPressV` 均为 `public static`（:34-42）——HUD 直接读。

**端口目标**：`KeyMapping`（Forge `RegisterKeyMappingsEvent`，mod 总线）+ `ClientTickEvent`/`InputEvent`。默认键改成 `GLFW.GLFW_KEY_J`（对应 1.12 的 36），category 用 `"key.categories.misc"`。`KeyHandler` 的 M/C/X 三个键与自我注魔能力相关（S4/S5），本区只关心 `keyLens`。

### 1.3 逐 tick 派发（供透镜系统使用）

`EntityEventHandler.playerTick`（IS12:`common/events/EntityEventHandler.java:89-116`）在 **START 阶段双端**（`event.player.world` 无 side 判定，逻辑内部各自判 `world.isRemote`）派发：

```
revealer = LensManager.getRevealer(player)
if (!revealer.isEmpty() && revealer.hasTagCompound() && revealer.getTagCompound().getString("LeftLens") != null) {
    leftLens = getLens(revealer, LEFT); rightLens = getLens(revealer, RIGHT)
    doubleLens = rightLens != null && rightLens.equals(leftLens)
    if (leftLens != null)  leftLens.handleTicks(world, player, doubleLens)
    if (!doubleLens && rightLens != null) rightLens.handleTicks(world, player, false)
}
```

> 注意 `getString(...) != null` 恒真：真正条件是“有 revealer 且带 NBT”。`doubleLens` 表示左右装同一镜片——渲染时只画一次（左侧）。

### 1.4 渲染派发

`RenderEventHandler`（IS12:`client/libs/RenderEventHandler.java`，`@SideOnly(CLIENT)`，在 `Client.preInit` 用 `MinecraftForge.EVENT_BUS.register(RenderEventHandler.class)` 注册，见 IS12:`client/Client.java:41`；`new KeyHandler()` 在 `postInit` 注册，:87）：

- `onOverlay(RenderGameOverlayEvent.Pre)`（:76-123）：
  - `type == TEXT` → `fociRight.handleFociRadial(...)`、`fociLeft.handleFociRadial(...)`（**轮盘**）。
  - `type == HOTBAR` → 取左/右透镜，调 `handleRenderGameOverlay(world, player, resolution, doubleLens, partialTicks)`；随后画**石化**叠加。
- `renderLast(RenderWorldLastEvent)`（:214-231）→ 左/右透镜 `handleRenderWorldLast(world, player, doubleLens, partialTicks)`。
- `renderPlayerEvent` / `preRenderLiving` / `postRenderLiving`（:235-246）：**空实现**。
- `onModelBakeEvent`（:69-73）+ `RenderCustomItem`（IS12:`client/renderer/RenderCustomItem.java`）：灵魂罐物品渲染，属施工区 03，不在本区。
- `onToolTip`（:159-176）、`onDrawGui`（:127-156）：见 §7、§8。

`fociHUD` 是 `RenderEventHandler` 的内部类（:258-501），两个实例 `fociLeft`/`fociRight` 各自持有 `LENSSLOT type`；`static radialHudScale`（:57）为共享缩放。

---

## 2. 透镜轮盘 HUD（`fociHUD`，1.12）

### 2.1 何时显示

`handleFociRadial(mc, time, event)`（:277-358）在 `KeyHandler.radialActive || radialHudScale > 0` 时工作：

- `radialActive && mc.currentScreen != null` → 强制关轮盘并置 `radialLock = true`、`setIngameFocus/setIngameNotInFocus`。
- `radialActive && radialHudScale == 0` → **扫描背包**：遍历 `mc.player.inventory.mainInventory`（:295），对每个 `instanceof ItemLens` 的槽位记录
  `foci.put(lens.getLens().getRegistryName(), a)`（`TreeMap<ResourceLocation,Integer>`，值=背包槽号）、`lensStack.put(key, item.copy())`、`fociScale.put(key, 1.0F)`、`lensHover.put(key, FALSE)`、`lensSlot.put(key, a)`；若 `foci.size() > 0 && mc.inGameHasFocus` → `inGameHasFocus=false; mouseHelper.ungrabMouseCursor()`。
- 松手后（`!radialActive && currentScreen == null && lastState`）恢复鼠标抓取（:311-317）。
- 每次 `time > lastTime`（`lastTime = time + 5`，即每 5ms 更新一次，:354）：
  - 悬停项 `fociScale += 0.025F`（上限 1.3F），非悬停 `-= 0.025F`（下限 1.0F）（:332-342）。
  - `!radialActive` → `radialHudScale -= 0.05F`；否则 `radialHudScale = min(radialHudScale + 0.05F, 1.0F)`（:343-347）。
  - `radialHudScale < 0` → 归零并 `radialLock = false`（:349-352）。
  - 悬停且 `!radialActive && !radialLock` → `LensManager.putLens(player, lens, type)` + `sendToServer(new LensChangeMessage(lens, lensSlot.get(key), type))`，`radialLock = true`（:322-331）。

### 2.2 绘制（`renderFocusRadialHUD`，:360-500）

屏幕空间（**自设正交投影，不走世界相机**）：

1. `goggles = LensManager.getRevealer(mc.player)`；为 null 直接 return（:363-365）。
2. `lens = lensRegistry[goggles NBT[type.getName()]]`（:366-369）。
3. `i = Mouse.getEventX() * sw / displayWidth`；`j = sh - Mouse.getEventY() * sh / displayHeight - 1`；`k = Mouse.getEventButton()`（:370-372）。`sw/sh` = `event.getResolution().getScaledWidth_double()/Height`。
4. `if (lensStack.size() == 0) return;`
5. 状态：`glPushMatrix` → `glClear(GL_DEPTH_BUFFER_BIT=256)` → 投影 `glOrtho(0, sw, sh, 0, 1000, 3000)` → modelview identity → `translate(0,0,-2000)` → **`glDisable(GL_DEPTH_TEST=2929)`** → **`glDepthMask(false)`**（:376-385）。
6. `glPushMatrix` → `translate(sw/2, sh/2, 0)`。`width = 16.0F + lensStack.size() * 2.5F`（:387-389）。`translate(type.getAngle(), 0, 0)`（LEFT:+42，RIGHT:−42）。
7. **底板 1**：`bindTexture(radial)`（`thaumcraft:textures/misc/radial.png`）；push；`if LEFT rotate(-(partialTicks + (ticksExisted % 720)/2.0F), 0,0,1) else rotate(+..., 0,0,1)`；`glAlphaFunc(516, 0.003921569F)`；`glEnable(BLEND)`；`glBlendFunc(770, 771)`；`UtilsFX.renderQuadCentered(1, 1, 0, width*2.75F*radialHudScale, 0.5F, 0.5F, 0.5F, 200, 771, 0.5F)`；`glDisable(BLEND)`；`glAlphaFunc(516, 0.1F)`；pop（:391-404）。
8. **底板 2**：`bindTexture(radial2)`（**1.20.1 → `thaumcraft:textures/misc/radial_secondary.png`**，`radial2.png` 在 TC4R 不存在，见 00-C.3）；旋转方向与底板 1 **相反**；同样调用但 `width*2.55F`（:405-418）。
9. `lens != null` → `RenderHelper.enableGUIStandardItemLighting()`，`ri.renderItemIntoGUI(new ItemStack(lens.getItemLens()), -8, -8)`（:419-429）；鼠标在 `±10` 内则 `tt = new ItemStack(lens.getItemLens())`。
10. `glScaled(radialHudScale, radialHudScale, radialHudScale)`；`currentRot = -90.0F * radialHudScale`；`pieSlice = 360.0F / lensStack.size()`（:436-439）。
11. 对每个 `key`（`foci.firstKey()` 起，`foci.higherKey(key)` 迭代，**按 ResourceLocation 排序**）：
    `xx = cos(currentRot/180*π) * width`，`yy = sin(currentRot/180*π) * width`，`currentRot += pieSlice`；push；**LEFT → `translate(xx, yy, 100)`；RIGHT → `translate(-xx, yy, 100)`**；`scale(fociScale[key])`；GUI 光照；`renderItemIntoGUI(item2, -8, -8)`；disable 光照；pop（:441-461）。
12. 悬停判定（:462-488）：仅 `!radialLock && radialActive`。鼠标相对每个切片中心：LEFT 用 `mx2 = i - sw/2 - xx + (-type.getAngle())`，RIGHT 用 `mx2 = i - sw/2 + xx + (-type.getAngle())`，`my2 = j - sh/2 - yy`；命中 `±10` → `lensHover[key]=TRUE`、`tt = lensStack[key]`；`k == 0`（鼠标左键按下）→ `radialActive=false; radialLock=true;` 并 `putLens` + `sendToServer(LensChangeMessage)`。
13. `glPopMatrix`；`tt != null` → `UtilsFX.drawCustomTooltip(mc.currentScreen, mc.fontRenderer, tt.getTooltip(player, ADVANCED), 0, 20, 11)`（:492-494）。
14. 收尾：`glDepthMask(true)`、`glEnable(2929)`、`glDisable(3042)`、`glColor4f(1,1,1,1)`、`glPopMatrix`（:495-499）。

**`UtilsFX.renderQuadCentered` 的几何**：TC6 源码不在本机（`Isorropia_kedition` 树内无 TC6 jar/源），无法逐行核对。TC4（TC4:`client/lib/UtilsFX.java:325-338`，1.7.10 的等价 `renderQuadCenteredFromTexture(scale,r,g,b,bright,blend,opacity)`）为：`glScalef(scale,scale,scale)` → `glEnable(BLEND); glBlendFunc(GL_SRC_ALPHA, blend)` → 顶点色 `(r,g,b,opacity)` → 四顶点 `(-0.5,0.5,0)(u0 v1)`, `(0.5,0.5,0)(u1 v1)`, `(0.5,-0.5,0)(u1 v0)`, `(-0.5,-0.5,0)(u0 v0)`（XY 平面 z=0）。**1.12 调用多出前导 `(1,1,0)` 三个参数，含义无法从现有源码确定——实机确认后再定四边形半径是否等于 `width*2.75*radialHudScale` 还是其一半**。1.20.1 实现建议：以 `±0.5*extent` 的四边形、灰色 `(0.5,0.5,0.5)`、alpha `0.5`、`FULL_BRIGHT`、`SRC_ALPHA/ONE_MINUS_SRC_ALPHA` 起步，按实机比对调整 `extent`。

### 2.3 1.7.10 差异（仅旁证）

TH17:`client/lib/RenderEventHandler.java:230-368`：单透镜（头盔 NBT 键 `"Lens"`）；轮盘不区分 LEFT/RIGHT，无 `translate(type.getAngle())`；物品源是背包 + `ItemLensCase`（1.12 已去掉 LensCase）；两底板旋转方向相反；其余几何（ortho `1000..3000`、`translate(0,0,-2000)`、`translate(sw/2,sh/2)`、`width = 16 + n*2.5`、`width*2.75 / width*2.55`、`currentRot=-90*scale`、`pieSlice=360/n`、item 画在 `(-8,-8)`）与 1.12 **完全一致**。1.12 的 `translate(±x, y, 100)` 与 `type.getAngle()` 是新增。

### 2.4 1.20.1 目标

- 类：`client/hud/LensRadialOverlay implements IGuiOverlay`（或 `IGuiOverlay` lambda）。注册：模组总线 `RegisterGuiOverlaysEvent#registerAbove(VanillaGuiOverlay.HOTBAR.id(), "lens_radial", overlay)` 或 `registerAboveAll`。
- 轮盘需要自定义正交投影：不再手改 GL 矩阵，直接用 `GuiGraphics.pose()`（GUI 阶段已是正交、y 向下、z 由 `translate(0,0,z)` 控制）。**depth 处理**：GuiGraphics 的 GUI 渲染默认 `NO_DEPTH_TEST`（`RenderSystem.disableDepthTest` 由 `GuiGraphics`/`GuiRenderState` 保证），因此原 `glDisable(2929)+glDepthMask(false)` 天然等价，**不需要**额外处理；但物品图标（`renderItemIntoGUI`）需要 `GuiGraphics.renderItem`，它会自己管理 3D 深度（内部 `translucent` 阶段会 `enableDepthTest`），所以轮盘底板应分两批画：底板（平面纹理）→ `flush` → 物品。
- 底板纹理：`thaumcraft:textures/misc/radial.png`、`thaumcraft:textures/misc/radial_secondary.png`（256²，TC4R 已有）→ 用 `GuiGraphics.blit(ResourceLocation, x, y, 0, u, v, w, h, texW, texH)`，`u0=v0=0`，`u1=v1=1`，`texW=texH=256`，四角矩形边长 = 2×halfExtent，颜色 ARGB `0x80xxxxxx`（alpha 0.5）。
- 旋转：`pose.mulPose(Axis.ZP.rotationDegrees(deg))`，方向与 1.12 一致。
- 物品图标：`graphics.renderItem(stack, -8, -8)`（1.20.1 `GuiGraphics.renderItem(ItemStack,int,int)` 原点即左上角，等价于 1.12 `renderItemIntoGUI(...,-8,-8)`）。
- tooltip：`graphics.renderTooltip(font, stack, 0, 20)`（不再用 `UtilsFX.drawCustomTooltip`）。`ADVANCED` → `TooltipFlag.Default.ADVANCED`。
- 按键：`KeyMapping` + `ClientTickEvent`；`radialActive/radialLock` 移到 HUD 类的静态字段。
- 数据：需要 `ItemLens` → 对应 `ResourceLocation`（或 Lens 类型）的可客户端查询。**端口当前 `ItemLens` 只是裸 `Item`**（`common/items/misc/ItemLens.java`），无 `getLens()`——透镜系统（S5）必须补 `Lens` 字段与注册表（见 §11）。
- **鼠标抓取**：1.12 直接改 `mc.inGameHasFocus` / `mouseHelper.ungrabMouseCursor()`。1.20.1 等价物是 `Minecraft.mouseHandler.releaseMouse()/grabMouse()` 与 `Minecraft.setWindowActive`——1.20.1 无 `inGameHasFocus` 字段；用 `mc.mouseHandler.isMouseGrabbed()` 判断、`mc.mouseHandler.releaseMouse()/grabMouse()` 控制。**这是必须实机确认的行为**（1.20.1 把焦点/抓取状态重构过）。

---

## 3. AirLens —— 世界内涟漪/漩涡（1.12）

IS12:`common/lenses/AirLens.java`。常量：`AIR_LENS_DIST_CAP = 24`；`TEX = isorropia:textures/fx/ripple.png`（**端口已有**，64×64）；`TEX_UNDEAD = isorropia:textures/fx/vortex.png`（**端口已有**，64×64）；`TEX_ELDRITCH = thaumcraft:textures/misc/vortex.png`（TC4R 有，256²）。

`handleRenderWorldLast(world, player, doubleLens, pt)`（:39-122）：

- `if (mc.gameSettings.thirdPersonView > 0) return;`
- 取 `player` 周围 `AxisAlignedBB(±24)` 内所有 `EntityLivingBase`（排除自己），谓词 `e -> e != playerIn`。
- 对每个实体：
  - `playerOff* = prev + (cur-prev)*pt`（玩家插值坐标）；`eOff*` 同理。
  - `scale = |e.getPositionVector() - player.getPositionVector()|`（**用当前位置向量，不是插值**）。
  - `sizeOffset = e.ticksExisted % 16`。
  - `cappedTchebychevDist = min(getTchebychevDistance(e, player), 24)`；`alpha = 1 - cappedTchebychevDist/24`。`getTchebychevDistance` = `max(|dx|, max(|dy|, |dz|))`（IS12:`common/libs/helpers/IRMathHelper.java`）。
  - `size = min(e.height, e.width)`。
  - **分支 A：`e instanceof IEldritchMob`**（TC4 接口）：`size *= 0.8`；push；`glDisable(2929)`；`translate(-playerOffX,-playerOffY,-playerOffZ)`；`translate(eOffX, eOffY + e.height/2, eOffZ)`；`rotate(-player.rotationYaw, 0,1,0)`；`rotate(player.rotationPitch, 1,0,0)`；`rotate(e.ticksExisted % 360, 0,0,1)`；`UtilsFX.renderQuadCentered(TEX_ELDRITCH, size, 1,1,1, -99, 771, alpha)`；`rotate(-(e.ticksExisted % 360)*2, 0,0,1)`；`size *= 2`；再画一次；`glEnable(2929)`；pop。
  - **分支 B：`e.isEntityUndead()`**：`size *= 1.3`；同样变换但 `rotate(-(ticksExisted % 360), 0,0,1)` 先画、`size *= 2`、`rotate(+(ticksExisted % 360)*2, 0,0,1)` 再画；`TEX_UNDEAD`。
  - **分支 C：普通生物**：`numbers = min(48.0/scale + 1.0, 4.0)`；对 `i in [0, numbers)`：`numSize = ((i*16)/(numbers+1.0) + sizeOffset) % 16.0 / 12.0 * size`；每层单独 push/变换（与 A/B 同）后 `renderQuadCentered(TEX, numSize, 1,1,1, -99, 771, alpha)`，pop。

`renderQuadCentered(tex, size, r,g,b, bright, blend, alpha)`：brightness `-99`（≤0 → 不设亮度，即全亮，等价 1.7.10 `glDisable(GL_LIGHTING)`+全亮）；blend `771` → `glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)`；顶点色 `(1,1,1,alpha)`。几何同 §2.2（±0.5 quad）——**这是“世界内广告牌”而非屏幕空间**。

### 3.1 1.7.10 参考（TH17:`common/items/lenses/ItemLensAir.java`）——**屏幕空间**，仅作 alpha/尺寸旁证

TH17 把实体投影到屏幕（`pos.dotProduct(look)`、`hScale = proj.length * tan(fov/2) * 2`、`horiz/hScale`、`vert/hScale/var7*var6`），中心 `xCenter = var6*(1+horiz)/2`、`yCenter = var7*(1-vert)/2`，四顶点直接画在 glOrtho 屏幕坐标（`z=1.0`）。UV 顺序：
`(xCenter-ripSize, yCenter+ripSize)uv(0,1)`、`(+ripSize,+ripSize)uv(1,1)`、`(+ripSize,-ripSize)uv(1,0)`、`(-ripSize,-ripSize)uv(0,0)`。
- 非亡灵：`alpha = 0.3 - scale/80`；`numRipples = min((int)(48/scale+1), 4)`；`ripSize = size*((i*16/(n+1) + ticksExisted%16) % 16)/12`。
- 亡灵：`alpha = 0.1 - scale/240`；`size *= 2`；两片 vortex，`angle = ticksExisted%360`，用 `sin/cos` 做四顶点旋转，第二片用 `-angle` 且半径减半。

**取舍：以 1.12 为准**（Isorropia 实际发布；世界空间广告牌天然适配 1.20.1 的 `RenderLevelStageEvent`；1.7.10 的屏幕空间投影在 1.20.1 要复刻整套 FOV 投影，收益低）。差异记录：alpha 公式 1.7.10 是 `0.3 - scale/80`（随距离衰减），1.12 改成 `1 - 切比雪夫距离/24`（贴脸满、24 格外 0）。1.12 还新增了 eldritch 分支。

### 3.2 1.20.1 目标

- 类：`client/hud/LensWorldOverlays`（或每透镜一个 `client/renderer/…`）挂在 `RenderLevelStageEvent`。阶段：**`AFTER_LEVEL`**（要“穿过墙可见”的观感）或 `AFTER_TRANSLUCENT_BLOCKS`（受不透明方块遮挡）。1.12 用 `glDisable(2929)` → 深度测试关闭 = 穿透可见 → 语义上最接近 `AFTER_LEVEL`。
- 渲染器：把每个 quad 变成一个 `VertexConsumer` 批次。RT 选择（00-A.4/A.9）：
  - 1.12 用 `(770,771)` + 全亮 + 不裁剪 → 用 TC4R 公开 `ThaumcraftClientRenderApi.legacyLinearTranslucentBillboard(tex)`（=`entity_translucent_emissive` + `TRANSLUCENT_TRANSPARENCY` + NO_CULL + 线性过滤，不写深度）。若需不写深度 + LEQUAL，这是最接近的公开 RT。
  - 或者自建（00-D.5 配方）：`entity_translucent_emissive` + `TRANSLUCENT_TRANSPARENCY` + `COLOR_WRITE` + `NO_CULL`（**注意**：`entity_translucent_emissive` 会丢弃贴图 alpha<0.1，ripple/vortex 有柔边，按 00-E 风险 #3 可能削边；若明显削边，改用 `RENDERTYPE_EYES_SHADER`，但那会把混合变成重写——eyes shader 不丢弃、但 RT 的 transparency shard 决定混合）。建议先试 `legacyLinearTranslucentBillboard`，实机比对柔边。
- 变换顺序（每个实体，`PoseStack` 已含视图旋转与相机平移，见 00-D.8）：**去掉 1.12 的 `translate(-playerOff)`**（`RenderLevelStageEvent` 的 pose 在世界原点，需自行 `translate(-cam.x,-cam.y,-cam.z)` 或直接用 TC4R 的 `beginAfterLevelWorldRender`）。顺序：
  `translate(eX, eY + e.height/2, eZ)` → `mulPose(Axis.YP.rotationDegrees(-player.getYRot()))` → `mulPose(Axis.XP.rotationDegrees(player.getXRot()))` →（A/B 分支）`mulPose(Axis.ZP.rotationDegrees(±ticksExisted%360))` → 画 quad（`±0.5*size`，UV 全 0..1）→（第二片）再旋转。
  顶点 `light`：`FULL_BRIGHT`（`LightTexture.FULL_BRIGHT`）；overlay `NO_OVERLAY`；颜色 `(1,1,1,alpha)`。
- 触发与数据：**纯客户端**，只需“玩家戴着护目镜/揭示器”+ 附近的 `LivingEntity`（`level.getEntitiesOfClass(LivingEntity.class, aabb)`）。无服务端同步。
- 端口现状：`AirLens` 类不存在；`textures/fx/ripple.png`、`vortex.png` 已就位。

---

## 4. OrdoLens —— 扫描 HUD + 世界内浮动名字（1.12）

IS12:`common/lenses/OrdoLens.java`。

### 4.1 `handleTicks`（服务端，:40-54）

`EntityUtils.getPointedEntity(world, player, 1.0D, 5.0D, 0.0F, true)` → `ScanningManager.isThingStillScannable(player, target)` → `ScanningManager.scanTheThing(player, target)`；否则 `RayTraceResult`（自写 `rayTrace`，:215-231，用 `REACH_DISTANCE` 属性）命中方块 → 对方块做同样扫描。**纯服务端逻辑**。

### 4.2 `handleRenderGameOverlay`（屏幕空间，:57-97）

- `if (mc.gameSettings.thirdPersonView != 0) return;`（注意是 `!= 0`，与 AirLens 的 `> 0` 不同）。
- `target = EntityUtils.getPointedEntity(world, player, 1.0D, 5.0D, 0.0F, true)`：
  - 有实体：维护 `RenderEventHandler.thaumTarget`（= TC4 的 `thaumTarget` 全局），若目标切换 → `RenderEventHandler.tagscale = 0; nameSize = 0`。若可扫描 → `FXDispatcher.INSTANCE.scanHighlight(target)`。然后 **return**（实体分支不在屏幕画名称/要素——名称走世界内，见 4.3）。
  - 无实体：`RayTraceResult mop = rayTrace(...)`；`mop.typeOfHit == BLOCK` 时：看方块上方是否堆着 `EntityItem`（`getEntitiesWithinAABB(EntityItem, AABB(blockPos + (0,1,0)))`），有则 `renderNameAndAspects(resolution, AspectHelper.getObjectAspects(itemstack), displayName)` + scanHighlight；否则 `renderNameAndAspects(resolution, AspectHelper.getObjectAspects(new ItemStack(block)), block.getLocalizedName())` + scanHighlight。

### 4.3 `handleRenderWorldLast`（世界内名字，:100-139）

若 `RenderEventHandler.thaumTarget != null`：取目标实体插值坐标，`translate(-playerOff)` → `translate(eOffX, eOffY + entity.height + (aspects非空 ? 1.1 : 0.6), eOffZ)` → `rotate(-player.rotationYaw, 0,1,0)` → `rotate(180, 0,0,1)` → `nameSize += 0.031F`（上限 1.0F）→ `size = 0.03F * nameSize` → `scale(size,size,size)` → `fontRenderer.drawString(name, 1 - width/2, 1, Color.WHITE.getRGB()=0xFFFFFF)`。名称取 `EntityItem.getItem().getDisplayName()` 或 `entity.getName()`。

### 4.4 `renderNameAndAspects` + `drawAspectTag`（屏幕空间要素行，:148-213）

`renderNameAndAspects(resolution, aspects, text)`：
- `w = resolution.getScaledWidth()`，`h = getScaledHeight()`，`size = 18`。
- 对每个要素（`num` 从 0）：
  - `yOff = num / max(5, aspects.size()) * 18`。
  - `thisRow`：`if (aspects.size()-num < max(5, size)) thisRow = aspects.size()-num; else thisRow = max(5, size);`
  - `x = w/2 - 18*thisRow/2 + 18*num`，`y = h/2 + 16 + yOff`。
  - `drawAspectTag(aspect, amount, x, y)`。
  - `if (++num % max(5,size) == 0)` 重算 `thisRow`。
- `text` 非空 → `ingameGUI.drawString(font, text, w/2 - width/2, h/2 - 16, 16777215)`。

`drawAspectTag(aspect, amount, x, y)`（:184-213）：
- `color = new Color(aspect.getColor())`；`glColor4f(r/255,g/255,b/255, 0.5F)`（随后被顶点色覆盖）。
- `bindTexture(aspect.getImage())`；`begin(7=QUADS, POSITION_TEX_COLOR)`；四顶点（16×16）：
  `(x, y) uv(0,0)`、`(x, y+16) uv(0,1)`、`(x+16, y+16) uv(1,1)`、`(x+16, y) uv(1,0)`，顶点色 = 要素 RGB、alpha = `color.getAlpha()/255`（要素色自带 alpha，通常 255）。
- `glScalef(0.5,0.5,0.5)`；白字数量：`new DecimalFormat("#######.##").format(amount)`，位置 `(24 + x*2, 32 - FONT_HEIGHT + y*2)`，颜色 `16777215`。

**1.7.10 参考**（TH17:`common/items/lenses/ItemLensOrderEntropy.java:163-319`）：`renderNameAndAspects` 固定 5 列（`thisRow = min(size-num, 5)`，`yOff = num/5*18`，`x = w/2 - 18*thisRow/2 + 18*(num%5)`），其余公式相同；`drawAspectTag` 几何相同但顶点色 alpha 固定 0.5。1.12 把“5 列”换成 `max(5, size)`（当要素 >5 时布局与 1.7.10 不同，且 `num` 不带 `%5` → 1.12 >5 要素时 x 会溢出，属 1.12 自身缺陷）。

**取舍**：以 **1.12** 为准（Isorropia 发布版；且 TC4R 有直接等价：`common.ScanManager.findTarget/isValidTarget/complete`、`common.EntityAspectCatalog`、`client.ThaumometerHud`）。1.7.10 的 5 列布局可作为“要素 ≤5 时”的等价参考。

### 4.5 1.20.1 目标

- **服务端扫描**（`handleTicks` 等价）：放 `ServerTick`/`PlayerTick`（服务端分支），用 `ScanManager.findTarget(player)` / `isValidTarget` / `complete`。这部分不是渲染，但决定了 HUD 能否有数据。
- **屏幕 HUD**：`IGuiOverlay`。**强烈建议复用 `client.ThaumometerHud` 的布局数值**（`reference/.../client/ThaumometerHud.java:55-77`）：`centerX = guiWidth/2`、`centerY = guiHeight/2`；名称 `drawCenteredString(font, targetName, centerX, centerY+35, 0xF0EE96)`；要素列 `columns = min(5, n)`、`x = centerX - rowCount*9 + inRow*18 + 1`、`y = centerY - 20 + row*18`、图标 `AspectGuiRender.blitAdditive(graphics, aspect, x, y, 1.0F)`（**包私有**，公开替代是 `ThaumcraftClientRenderApi.drawAspect(GuiGraphics, AspectId, x, y, alpha)`，00-A.4），数量 `pose.translate(x+15, y+11, 1); pose.scale(0.5,0.5,1); drawString(font, amount, -font.width(amount), 0, 0xFFFFFF, true)`。
  > 注意：`ThaumometerHud` 自身只在“双手持扫描仪”时渲染（`isHoldingThaumometer`），**不能直接复用其触发**，只能抄它的布局数值。
- **世界内浮动名字**：`RenderLevelStageEvent.AFTER_LEVEL`，`pose.translate(-cam.x,-cam.y,-cam.z)` 后 `translate(eX, eY + height + (hasAspects ? 1.1 : 0.6), eZ)`，`mulPose(Axis.YP.rotationDegrees(-player.getYRot()))`，`mulPose(Axis.ZP.rotationDegrees(180))`，`scale(size)`；用 `Font.drawInBatch(..., Font.DisplayMode.SEE_THROUGH, ...)`（参考 `GogglesClientEvents.renderNote` 的写法，`reference/.../client/GogglesClientEvents.java:162-180`）。`nameSize` 动态插值（`+0.031F` 上限 1.0）。
- **`scanHighlight`**：TC4 `FXDispatcher.scanHighlight` → 用 `ThaumcraftClientEffectsApi.spawnParticle(level, ThaumcraftParticle.SCAN_RUNE, pos, …)`（00-A.1 末尾“TC6 scanHighlight → 近似 spawnParticle(SCAN_RUNE)”）。
- 数据：需要 `RenderEventHandler.thaumTarget`/`tagscale`（1.12 里其实是 `thaumcraft.client.lib.events.RenderEventHandler` 的静态字段）→ 端口用 `LensClientState` 静态持有 `thaumTarget`（客户端 Entity）+ `tagscale`/`nameSize`。

---

## 5. EnvyLens —— 屏幕右下进度条（1.12）

IS12:`common/lenses/EnvyLens.java`。常量：`ENVY = isorropia:textures/aspects/invidia_hud.png`（**端口已有**，实测 **32×16**）。

- `handleTicks`（:49-108，**服务端**）：对 `EntityUtils.getPointedEntity(world, player, 0, 1.5, 5.0F, true)` 指向的 `EntityLiving`，读其 loot table 池；有池则 `cap.envy++`，`envy > health*20` 时生成掉落并 `hasLooted = true`；每次变化 `sendTo(new CapabilityMessage(living, cap.serializeNBT()), player)`（**这就是客户端 HUD 的数据来源**）。
- `handleRenderGameOverlay`（:112-122）：`getPointedEntity(world, player, 1.0D, 5.0D, 5.0F, true)`；`instanceof EntityLiving` 且 `!cap.hasLooted` → `renderEnvyHUD(resolution, entityId)`。
- `renderEnvyHUD`（:134-175）：
  - `x = getScaledWidth_double() - 16`，`y = getScaledHeight_double() - 16`（**右下角，16×8 的条**）。
  - `pourcentage = envy / health * 5.15`；`> 100` 不画。
  - `p = pourcentage*8/100`，`u = 8 - p`，`i = 1 - pourcentage/100`。
  - `color = new Color(0x00ba00)`（绿）。
  - **第 1 批**（高光/底）：`glAlphaFunc(516, 0.003921569F)`；`glColor4f(r/255,g/255,b/255, 0.5F)`；`bindTexture(ENVY)`；`begin(7, POSITION_TEX_COLOR)`；顶点 `(x,y)uv(0,0)`、`(x,y+8)uv(0,1)`、`(x+16,y+8)uv(1,1)`、`(x+16,y)uv(1,0)`，顶点色 `(1,1,1,0.5)`（白 50% 覆盖整张图到 16×8 矩形）。
  - **第 2 批**（进度）：同 UV 顺序但 y 从 `y+u` 起、v 从 `i` 起：`(x,y+u)uv(0,i)`、`(x,y+8)uv(0,1)`、`(x+16,y+8)uv(1,1)`、`(x+16,y+u)uv(1,i)`，顶点色 = 绿色 RGBA `(0,186,0,255)`——**从底部向上填充**。
  - `glColor4f(1,1,1,1)`；`glAlphaFunc(516, 0.003921569F)`。
- `handleRenderWorldLast` / `handleRemoval`：空。

**取舍：1.12 独有**（TH17 无对应透镜）。唯一参考。

### 5.1 1.20.1 目标

- `IGuiOverlay`，`graphics.blit(ENVY_RL, x, y, 0, 0, 0, 16, 8, 32, 16)` 画白色/绿色两片。**注意 UV**：纹理 32×16 被拉伸到 16×8；1.20.1 用 `blit(rl, x, y, z, uOffset, vOffset, w, h, texW, texH)`，uOffset0..1 映射到 0..32 像素——需要按像素给参数（`uOffset` 单位是纹理像素）。进度片：源像素区域 y 从 `i*16` 到 16，目标 y 从 `y+u` 到 `y+8`（高度 = `p`）——即 `blit(rl, x, (int)(y+u), 0, 0, (int)(i*16), 16, (int)p, 32, 16)`（用整数像素版本 `blit(rl,int,int,int,float,float,int,int,int,int)`）。
- **数据同步（关键）**：HUD 需要客户端能读被指实体身上同步过来的 `envy` 计数与 `hasLooted`。端口当前 `common/capabilities/LivingData.java` 无 `envy`/`hasLooted`（grep 无命中），S4 的能力同步需补这两个字段 + `CapabilityMessage` 等价包。**这是 EnvyLens HUD 的前置依赖**。
- 颜色 `0x00ba00`；白色覆盖片 ARGB `0x80FFFFFF`；绿色进度片 ARGB `0xFF00BA00`。

---

## 6. FireLens / GluttonyLens / LustLens

- **FireLens**（IS12:`common/lenses/FireLens.java`）：唯一有副作用的是 `handleRenderGameOverlay`（:19-27）——若玩家没有夜视（`Potion.getPotionById(16)` = 夜视）或剩余 <242 tick，则 `addPotionEffect(new PotionEffect(nightVision, 255, 0, false, false))`。`handleRemoval`（:32-34）移除夜视。无绘制。1.7.10 版本（TH17:`ItemLensFire.java`）多了水下移除 + `IlluminePotionEffect` 标记（防误删玩家自喝的夜视）。
  → 1.20.1：`MobEffects.NIGHT_VISION`，持续 255、amplifier 0、`ambient=false, visible=false`；要复刻 1.7.10 的“只删自己加的”需一个标记（1.20.1 药水实例不可扩展 → 用每帧 `if (!visible && duration<242 && amplifier==0)` 近似，或维护一个客户端标志）。**风险：可能删掉玩家自饮的夜视**，需实机确认。
- **GluttonyLens / LustLens**（IS12:`common/lenses/GluttonyLens.java`、`LustLens.java`）：四个方法**全空实现**。`Common.java:121-124` 只注册了 air/fire/ordo/envy。→ 端口**不移植**（已定决策 6）。
- **1.7.10 的 EarthLens / WaterLens**：`ItemLensEarth.handleRender` 空 + `RenderEventHandler.renderTick` 放置/还原 `blockEvanescent`（:405-541），`ItemLensWater.handleRender` 空 + `RenderEventHandler.clearWater` 取消 `RenderBlockOverlayEvent`（:587-603）。Isorropia 1.12 未移植这两个（1.12 里没有对应类/无 earth/water lens 注册）。→ 参考价值仅在于若将来补 WaterLens，用 `RenderBlockOverlayEvent` 的事件路径（1.20.1 → `RenderBlockScreenEffectEvent`，属 TC4R/Forge 层面，本区不做）。

---

## 7. 石化全屏叠加（1.12 独有）

IS12:`client/libs/RenderEventHandler.java:99-121`（在 `onOverlay` 的 **HOTBAR** 分支内）：

- 条件：`LivingBaseCapability cap = Common.getCap(player); cap.petrification > 0`。
- `flag = 1.0F - (100 - petrification) / 100.0F`（= `petrification/100`）。
- `width = resolution.getScaledWidth_double()`，`height = getScaledHeight_double()`；`f2 = height / 34.0F`，`f3 = width / 34.0F`。
- `glPushMatrix`；`glBlendFunc(770, 771)`；`glColor4f(1,1,1,flag)`；`bindTexture("textures/blocks/cobblestone.png")`；`begin(7=QUADS, POSITION_TEX)`；顶点（**UV 用像素而非归一化 → 靠 GL_REPEAT 平铺 34×34 次**）：
  `(0, f2, 1) uv(0, height)`、`(f3, f2, 1) uv(width, height)`、`(f3, 0, 1) uv(width, 0)`、`(0, 0, 1) uv(0, 0)`。
  顶点 `buffer.color(1,1,1,0.1F)`——`POSITION_TEX` 格式**无 COLOR 元素，此调用被忽略**；实际色 = `glColor4f(1,1,1,flag)`（alpha = petrification/100）。
- `glColor4f(1,1,1,1)`；`glPopMatrix`。

**1.20.1 目标**：`IGuiOverlay`（可注册在 `VanillaGuiOverlay.HOTBAR` 之上/或 `registerAboveAll`）。平铺需要用 `GuiGraphics` 的重复UV：`graphics.blit(rl, x, y, uOffset, vOffset, w, h, regionW, regionH, texW, texH)` 的 regionW/H 是**精灵像素**（`regionW/texW` 决定 UV 跨度），不支持 >1 的 UV → 需自己用 `BufferBuilder` 画一个 UV 从 `(0,0)` 到 `(34,34)` 的四边形，并把纹理 wrap 设为 `REPEAT`（`RenderSystem.setShaderTexture` + 该 RT 的 `TextureStateShard`/`RepeatMode`）。或最稳妥：循环画 34×34 个 16×16 的 `blit`（1024 次/帧，性能可接受但难看，建议前者）。颜色 ARGB `0x??FFFFFF`（alpha = petrification/100）。方块图集精灵名 `minecraft:block/cobblestone`。
- 纹理：vanilla `textures/block/cobblestone.png` 改为图集精灵；1.20.1 不能直接 `bindTexture` 方块路径 → 用 `InventoryMenu.BLOCK_ATLAS` 或 `TextureAtlas.LOCATION_BLOCKS` 作为 `renderType` 的 textures 参数，`u/v` 取精灵的 `getU0()..getU1()`。因为要 REPEAT 平铺，**建议单独绑 `minecraft:textures/block/cobblestone.png`**（该文件在客户端资源中存在，可被 `ResourceLocation` 直接加载，非图集）。
- 触发数据：`petrification` 来自 S4 的 `LivingData`/`LivingCapability`，**需同步到客户端**。端口 `common/capabilities/LivingData.java` 需确认是否已有 `petrification`（grep 未见）——若缺，属 S4 缺口。
- **实机确认点**：1.12 的 34×34 UV 平铺是否真的 repeat（取决于该纹理的 wrap 设置）；alpha 是否只有 `flag`（顶点色被忽略）。1.20.1 要显式实现。

---

## 8. tooltip 与研究页

### 8.1 透镜 tooltip（IS12:`RenderEventHandler.onToolTip` :159-176）

条件：`stack.hasTagCompound() && (item instanceof IGoggles || item instanceof IRevealer)`。读 NBT `LeftLens`/`RightLens` 字符串，非空则 `tooltip.add(1, YELLOW + I18n.format("lens." + lensRegistry.getValue(rl).getTranslationKey()))`（**插在 index 1**，即第二行）。

端口 lang 键已就位（`lang/en_us.json:45-48`：`lens.fire_lens`/`lens.air_lens`/`lens.ordo_lens`/`lens.envy_lens`），与 1.12 的 `Lens.getTranslationKey()`（= registry path）一致。

**1.20.1**：`ItemTooltipEvent`（Forge 总线），`event.getToolTip().add(1, Component.literal("§e").append(Component.translatable("lens."+...)))`；用 `TextFormatting.YELLOW`。条件改为 `stack.getItem() instanceof GogglesOverlayGear/AuraRevealingGear` 或直接由护目镜物品类判断。

### 8.2 研究页 CurativeInfusionRecipe 绘制（IS12:`RenderEventHandler.onDrawGui` :127-156 + `drawCurativeInfusionRecipe` :178-210）

在 `GuiScreenEvent.DrawScreenEvent` 中，若 `screen instanceof GuiResearchPage`，用 `ReflectionHelper` 取私有字段 `recipePage/recipeLists/shownRecipe`，若当前页是 `CurativeInfusionRecipe`，画：
- `getVis() > 0`：白色 `"<int vis>"` 于 `(x - width/2 + 70, y - 45)`（x=(width-256)/2+128, y=(height-256)/2+128），色 `5263440`；再绑 `thaumcraft:textures/gui/gui_researchbook_overlay.png`，`glColor4f(1,1,1,0.4)`，`translate(x, y-55,0)`，`scale(2,2,1)`，`drawTexturedModalRect(30, 0, 68, 76, 12, 12)`（UV 68,76 尺寸 12×12）。
- `getCelestialAura() != 0`：绑 `recipe.getCelestialBody().getTex()`，`translate(x+56, y-17)`，`scale(0.12,0.12,1)`，`glColor4f(1,1,1,0.7)`，`drawTexturedModalRect(30, 0, 46, 45, 161, 162)`。
- `getFluxRejection() > 0`：`GuiResearchBrowser.drawForbidden(x, y-70)`（TC6 的“禁忌”符文）。
- 注意 `recipe.getCelestialBody().getTex()` 指向 TC6 独有贴图 `textures/items/celestial/*.png`（**端口不存在**，00-C.3）。

**1.20.1 目标**：**不要**照抄（反射 TC6 `GuiResearchPage` 在 TC4R 无意义）。改用 00-A.7 的 `ResearchRecipeRendererApi.register(ResourceLocation id, Class<R> recipeClass, BiConsumer<R, ResearchRecipeRenderContext>)` + `render(Recipe, ctx)`，ctx 提供 `graphics()/font()/x()/y()/output()/item()/ingredient()/isHovered()`。vis/天体/禁制三块按上列像素重画。天体贴图缺失 → 属 S2/S4 的数据缺口，需在 `isorropia` 命名空间补图（本区记录，不实现）。

---

## 9. 护目镜要素浮标（世界内）——复用 TC4R，勿重写

TC4 `IGoggles` 的方块要素浮标在 TC4R 已实现（00-B.9 / `reference/.../client/GogglesClientEvents.java`）：

- 触发：`RevealingEquipment.hasGogglesOverlay(player)` 且准星方块的 BE `instanceof AspectContainerView`（`api/aspect/AspectContainerView`）且 `GogglesAspectView.resolve(be)` 非空；音符盒/奥术耳显示 `"Note: n"`。
- `tagScale`：每次高亮 `+= 0.031F - tagScale/10`（上限 0.3），每帧 `decayScale()` 减 0.005。
- 布局：每行 5 个，`shiftX = (cur - rowSize/2 + 0.5) * s * 4 * s`，换行 `shiftY += s*1.05`，`translate(center + s*2*dir.step)` → `mulPose(YP, -camera.getYRot())` → `translate(shiftX,0,0)` → `mulPose(ZP,180)` → `scale(s)`；图标 `±0.5`，要素色、alpha 191、`FULL_BRIGHT`；数字 `translate(0,0.24,-0.1); scale(0.04)`，阴影 `0xBF111111` 于 `(14-w,1)`、白 `0xFFFFFFFF` 于 `(13-w,0)`，`SEE_THROUGH`。
- 在 `AFTER_LEVEL` 阶段回放（`renderQueuedContainerAspects`，要求 `Stage.AFTER_LEVEL`）。

**Isorropia 的用法**：自己的方块（改性矩阵、灵魂罐、治疗缸等）只要 BE `implements AspectContainerView`，**要素浮标自动出现**（不必写任何渲染代码）。`GogglesClientEvents` 是包私有、无公开订阅入口 → **无法给它追加自定义 HUD 行**（例如透镜自己的 overlay）。因此透镜的屏幕 HUD 必须走 `RegisterGuiOverlaysEvent` 自建，而不是塞进护目镜管线。

需要“被当作护目镜”的物品（Isorropia 的护目镜/揭示器本体）→ `implements GogglesOverlayGear`（`showsThaumcraftGogglesOverlay(stack, player)`）或 `AuraRevealingGear`。Isorropia 的镜片是 NBT 挂件，不实现这两个接口。

---

## 10. Shader / 纹理辅助类 —— 全部死代码，不移植

| 文件 | 1.12 现状 | 结论 |
|---|---|---|
| `client/libs/ShaderHelper.java` | `createProgram/createShader/getLogInfo/readFileAsString/initShaders`，用 `ARBShaderObjects`（1.20.1 已删）。**全树无任何调用**（`grep ShaderHelper` 只命中定义） | **不移植** |
| `assets/isorropia/shader/test.vert` | `#version 400 core`，输出 `color = position.xyz`、`pass_textureCoords`。**无引用** | **不移植** |
| `assets/isorropia/shader/test2.frag` | `out_Color = vec4(color,1) + texture(textureSampler, pass_textureCoords)`（“底图+顶点色”叠加）。**无引用** | **不移植** |
| `client/libs/CombinedTextureManager.java` | 继承 `TextureManager`，`bindTexture(rl)` 时用 `CombinedLayeredColorMaskTexture` 生成“材质+进度掩码”贴图并绑到 `PLACEHOLDER_LOCATION`。**无引用**（`grep` 只命中定义） | **不移植** |
| `client/libs/CombinedLayeredColorMaskTexture.java` | `loadTexture` 里 `bufferedimage1` 从未赋值 → `int i = bufferedimage1.getType()` **必 NPE**；`iresource` 恒 null。显然是未完成/废弃 | **不移植** |

若将来真需要“材质+进度 mask”的外观（用于雕像/克隆体），1.20.1 应改用真实 `ShaderInstance`（00-D.5 `RegisterShadersEvent#registerShader`）或 CPU 合成 `DynamicTexture` + `RegisterClientReloadListenersEvent`，但**本区无需求**。

---

## 11. Somatic Brain —— 头部 Curios 渲染（1.12 独有）

### 11.1 上游

`common/items/baubles/ItemSomaticBrain.java`（1.12）：

- `implements IBauble, IRenderBauble`；`getBaubleType = BaubleType.HEAD`（:26-28）；`getRarity = RARE`。
- `onPlayerBaubleRender(stack, player, RenderType.HEAD, partialTicks)`（:31-44）：
  ```
  boolean helmet = !player.getItemStackFromSlot(HEAD).isEmpty();
  IRenderBauble.Helper.translateToHeadLevel(player);
  IRenderBauble.Helper.translateToFace();
  IRenderBauble.Helper.defaultTransforms();
  GlStateManager.scale(1.81F, -1.81F, 1.81F);
  GlStateManager.translate(0.0D, helmet ? -1.73D : -1.72D, 0.27D);
  GlStateManager.enableBlend();
  bindTexture(ModelSomaticBrain.TEX);   // isorropia:textures/models/somatic_brain.png
  ModelSomaticBrain.INSTANCE.render(null, 0,0,0,0,0, 1.0F);
  GlStateManager.disableBlend();
  ```

**Baubles 1.12-1.5.2 `Helper` 精确变换**（`javap -c baubles/api/render/IRenderBauble$Helper` 反汇编）：

- `translateToHeadLevel(player)` = `translate(0, -player.getDefaultEyeHeight(), 0)`（默认 1.62）；若 `player.isSneaking()`，再 `translate(0.25F*sin(pitch*π/180), 0.25F*cos(pitch*π/180), 0)`。
- `translateToFace()` = `rotate(90, 0,1,0)`；`rotate(180, 1,0,0)`；`translate(0, -4.35, -1.27)`。
- `defaultTransforms()` = `translate(0, 3.0, 1.0)`；`scale(0.55, 0.55, 0.55)`。

净缩放 = `0.55 * 1.81 ≈ 0.9955`（Y 取负 → 模型上下翻转）。

`client/model/ModelSomaticBrain.java`：`ModelCustomArmor(1.0F, 0, 32, 32)`，`textureWidth=textureHeight=32`；`bipedHead = new ModelRenderer(this)`，`setRotationPoint(0, 24, 0)`；三个 `ModelBox`（**只有这三个盒子被渲染**，`render()` 只画 `bipedHead`，且忽略 `f5`，内部用 `0.0625F`）：

| texOffs(u,v) | addBox(x,y,z, w,h,d) | 备注 |
|---|---|---|
| `(0, 21)` | `(-2.5, -2.0, -2.0, 5, 2, 4)` | 最小 |
| `(0, 12)` | `(-3.0, -3.0, -3.0, 6, 3, 6)` | 中 |
| `(0, 0)`  | `(-4.0, -4.0, -4.0, 8, 4, 8)` | 最大（外层） |

纹理：`isorropia:textures/models/somatic_brain.png`（1.12，32×32）。**端口已有**于 `src/main/resources/assets/isorropia/textures/entity/somatic_brain.png`（32×32，实测），符合决策 7（渲染器皮肤放 `textures/entity/`，避免被拼进方块图集）。

**1.7.10 无对应**（TH 无 Somatic Brain；TH 只有 `BlockBrain`）→ 只能以 1.12 为准。

### 11.2 1.20.1 目标

- 模型类：`client/model/ModelSomaticBrain extends EntityModel<LivingEntity>`（或 `ModelPart` 容器），`createBodyLayer()` 用 `LayerDefinition.create(mesh, 32, 32)`，`CubeListBuilder.create().texOffs(0,21).addBox(-2.5F,-2.0F,-2.0F, 5, 2, 4)` 等三盒；`PartPose.ZERO`（**不要** `offset(0,24,0)`——1.7.10 的 `rotationPoint(0,24,0)` 是 `ModelCustomArmor` 的相对原点的历史写法；1.20.1 里应把 24/16=1.5 的偏移并入整体 `translate`，或直接让盒子在原点、由渲染器变换定位）。注册：`EntityRenderersEvent.RegisterLayerDefinitions`（`ClientSetup.onRegisterLayers` 已存在，加一行）。
- Curios 渲染器：`client/curio/SomaticBrainCurioRenderer implements ICurioRenderer`，`CuriosRendererRegistry.register(ItemsIS.SOMATIC_BRAIN.get(), SomaticBrainCurioRenderer::new)` 在 `FMLClientSetupEvent`（`ClientSetup.onClientSetup` 里 `enqueueWork`）。`ICurioRenderer.render(...)` 签名见 00-D.4。
- 变换链复刻（Curios 5.14.1，`poseStack` 进入时已在实体原点、未含头部旋转）：
  1. 头部跟随：`if (renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid) humanoid.head.translateAndRotate(poseStack);`（等价 1.12 的 `translateToHeadLevel + translateToFace` 的头部定位；Curios 也提供 `ICurioRenderer.followHeadRotations`）。
  2. `ICurioRenderer.translateIfSneaking(poseStack, entity)`（补足 sneak 位移；1.12 的 sneak 位移是 `translateToHeadLevel` 里那段 `0.25*sin/cos(pitch)`，1.20.1 用 Curios 的等价 helper，**实机比对**）。
  3. `poseStack.translate(0.0, 3.0*?, 1.0*?)`… 1.12 的 `defaultTransforms` 的 `(0,3,1)` 单位是“方块”（在 `translateToFace` 之后）——**必须实机调参**：把 `defaultTransforms` 的 `translate(0,3,1)`、`scale(0.55)` 与 `scale(1.81,-1.81,1.81)`、`translate(0, helmet?-1.73:-1.72, 0.27)` 依次 `poseStack.translate/mulPose/scale` 照抄，然后实机微调（这是最容易漂移的一处）。
  4. `GameRenderer.getRendertypeEntityCutoutNoCullShader()`（或 `RenderType.entityCutoutNoCull(TEX)`）画模型：`model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1,1,1,1)`。1.12 用了 `enableBlend`，但纹理若含 alpha 用 `entityCutoutNoCull` 即可（**实机确认是否需 `entityTranslucent`**）。
  - `helmet` 分支依赖玩家头盔槽是否为空（`entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()`）。
- 纹理：`isorropia:textures/entity/somatic_brain.png`（已在端口，32×32）。
- 物品注册：端口 `ItemsIS.SOMATIC_BRAIN` 目前是 `new Item(...)`（`ItemsIS.java:35-36`）——需改为专用类（或只需 `CuriosRendererRegistry.register` 指向该 RegistryObject，不必改类）。
- 注册时机：`CuriosRendererRegistry.register` 只存 Supplier，Curios 在 `EntityRenderersEvent.AddLayers` 里 `load()` 才实例化（00-D.4），故 Supplier 内可安全 `Minecraft.getInstance().getEntityModels().bakeLayer(LAYER)`。

---

## 12. 目标 1.20.1 设计汇总

### 12.1 新建类与注册点

| 类（建议路径） | 类型 | 注册点（总线） |
|---|---|---|
| `client/hud/LensRadialOverlay` | `IGuiOverlay` | 模组总线 `RegisterGuiOverlaysEvent#registerAbove(VanillaGuiOverlay.HOTBAR.id(), ...)` |
| `client/hud/LensScreenOverlays`（EnvyLens 条、OrdoLens 名称+要素） | `IGuiOverlay` | 同上 |
| `client/hud/PetrificationOverlay` | `IGuiOverlay` | 同上 |
| `client/render/LensWorldOverlays`（AirLens 涟漪、OrdoLens 浮动名） | `RenderLevelStageEvent` 监听 | Forge 总线 `MinecraftForge.EVENT_BUS` |
| `client/curio/SomaticBrainCurioRenderer` | `ICurioRenderer` | `FMLClientSetupEvent` → `CuriosRendererRegistry.register` |
| `client/model/ModelSomaticBrain` | `ModelLayerLocation` | 模组总线 `EntityRenderersEvent.RegisterLayerDefinitions` |
| `client/LensKeys`（`KeyMapping`） | — | 模组总线 `RegisterKeyMappingsEvent` |
| `common/lenses/Lens` + 4 子类 + `LensRegistry` | 逻辑（S5） | 见 §12.3 |

### 12.2 复用清单

- 护目镜浮标：`GogglesClientEvents`（**包私有，不可扩展**）→ 仅通过 `AspectContainerView` / `GogglesOverlayGear` / `AuraRevealingGear` 触发。
- 要素图标 GUI：`ThaumcraftClientRenderApi.drawAspect(graphics, AspectId, x, y, alpha)`（00-A.4）。
- 要素数据：`AspectApi.get(AspectId)` → `AspectDefinition{color, texture, blendMode}`（00-A.9）。
- 扫描：`common.ScanManager.findTarget/isValidTarget/complete/hasBeenScanned/findPointedEntity`；HUD 布局抄 `client.ThaumometerHud`。
- 粒子（scanHighlight）：`ThaumcraftClientEffectsApi.spawnParticle(level, ThaumcraftParticle.SCAN_RUNE, pos, rgb)`。
- 世界内全亮无深度广告牌 RT：`ThaumcraftClientRenderApi.legacyLinearTranslucentBillboard(tex)`（00-A.4）。
- 文本：`Font.drawInBatch(..., Font.DisplayMode.SEE_THROUGH, ...)`（参考 `GogglesClientEvents.renderNote`）。

### 12.3 透镜系统（S5）必须向客户端提供的最小数据

1. **护目镜/揭示器识别**：护目镜物品 `implements GogglesOverlayGear`（`showsThaumcraftGogglesOverlay` 返回 true）；揭示器 `implements AuraRevealingGear`。否则 `hasGogglesOverlay` 为假，整套透镜渲染都不触发。
2. **NBT 契约**：`ItemStack` 上的 `"LeftLens"` / `"RightLens"`（字符串 = `Lens` 的 ResourceLocation）——沿用 1.12 的 `LENSSLOT.getName()`。透镜注册名沿用 `isorropia:air_lens` / `fire_lens` / `ordo_lens` / `envy_lens`（与端口 lang 键、1.12 一致）。
3. **`ItemLens#getLens()`**：客户端能由物品拿到 `Lens`（端口当前 `ItemLens` 无此字段，见 `common/items/misc/ItemLens.java`）。这是轮盘和派发的核心查询。
4. **`Lens` 客户端子类**：`AirLens` / `OrdoLens` / `EnvyLens` / `FireLens` 的客户端渲染方法（可共用一个 `LensRenderDispatcher`）。
5. **同步**：
   - 换镜片：`LensChangeMessage`（客户端 → 服务端，携带 `lensRegistry` 的 id/int + 背包槽 + LENSSLOT），1.12 用 `ForgeRegistry.getID/getValue` → 1.20.1 用 `ResourceLocation` 字符串（1.20.1 无 `ForgeRegistry` 数字 id）或自定义 registry 的 `RegistryObject`。
   - 卸镜片：`LensRemoveMessageSP`（客户端 → 服务端）。
   - EnvyLens 的 `envy`/`hasLooted`：服务端 → 客户端（1.12 的 `CapabilityMessage` 等价）。
   - AirLens / OrdoLens 的世界内渲染**无需额外网络**（纯客户端读本地实体/准星）。
6. **护目镜触发条件**：1.12 `LensManager.getRevealer` 顺带把护目镜本身当 revealer（`getGoggles` 优先）。1.20.1 用 `RevealingEquipment.hasGogglesOverlay(player)`（含 Curios 槽）。

---

## 13. 风险 / 只能实机确认

| # | 内容 |
|---|---|
| 1 | `UtilsFX.renderQuadCentered` 的 1.12 语义（前导 `(1,1,0)` 三参）无法从本机源码确定；轮盘底板与 AirLens 涟漪的四边形**半径**只能以 TC4 的 `±0.5*scale` 起步，实机对比。 |
| 2 | 轮盘鼠标抓取：1.12 直接改 `mc.inGameHasFocus`/`mouseHelper.ungrabMouseCursor()`，1.20.1 无 `inGameHasFocus` 字段 → 用 `MouseHandler.releaseMouse()/grabMouse()`，行为（尤其打开背包时的强制关闭）需实机验证。 |
| 3 | 石化平铺 34×34：需 `GL_REPEAT`；1.20.1 `GuiGraphics.blit` 不支持 >1 的 UV，需自建 buffer + `RepeatMode`。alpha 是否只取 `flag`（顶点色被忽略）需核对 1.20.1 顶点色是否真被写入。 |
| 4 | `entity_translucent_emissive` 丢弃贴图 alpha<0.1（00-B.1），ripple/vortex 的柔边可能被削（00-E 风险 #3）→ 备选 `RENDERTYPE_EYES_SHADER`。 |
| 5 | AirLens 的 eldritch/undead 判定依赖 TC4 接口 `IEldritchMob` / `isEntityUndead()`；TC4R 是否有等价标记（如实体 tag/`TaintApi` 风格）需确认，否则 eldritch 分支退化。 |
| 6 | 1.12 的 `handleFociRadial` 每 5ms 更新一次（`lastTime = time + 5`），依赖 `System.nanoTime()/1000000L`；1.20.1 HUD 每帧调用 `IGuiOverlay.render`，需要把该 5ms 节流与 `fociScale` 缓动照搬，否则动画速度不同。 |
| 7 | Somatic Brain 的 Baubles `Helper` 变换（`translateToFace/defaultTransforms` 的 `(0,-4.35,-1.27)`、`(0,3,1)`、`scale(0.55*1.81)`）是为 1.7.10 的渲染原点设计；1.20.1 Curios 的头部件变换原点不同，**极可能需重新调参**。逐项照抄后实机看头盔下/无头盔两个高度（`-1.73/-1.72`）。 |
| 8 | FireLens 的“只删自己给的夜视”在 1.20.1 无法用药水实例标记 → 可能误删玩家自饮夜视。 |
| 9 | EnvyLens 与石化 HUD 依赖 S4 的能力同步（`envy`/`hasLooted`/`petrification`），端口当前 `common/capabilities/LivingData.java` 未含这些字段（grep 无命中）→ 属前置缺口，HUD 无法单独完成。 |
| 10 | OrdoLens 研究页/要素 HUD 需要 `PlayerKnowledgeService.get(player).knowsAspect(aspect)`（决定画 `aspects/_unknown.png` 还是真图标），参考 `GogglesClientEvents.renderAspect`。 |
| 11 | 1.12 的 `handleRenderGameOverlay` 用 `RenderGameOverlayEvent.HOTBAR` 的分辨率；1.20.1 对应 `IGuiOverlay.render(gui, graphics, partialTick, screenWidth, screenHeight)`，坐标基准一致（GUI 缩放后）。 |
| 12 | 轮盘底板 `radial2.png` → TC4R `radial_secondary.png`（已改名，00-C.3）；漏改会静默丢纹理（紫黑格）。 |

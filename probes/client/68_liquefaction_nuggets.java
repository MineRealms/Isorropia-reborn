// Liquefaction focus, part 6: installs a focus carrying three ranks of the nuggets upgrade and holds the beam on
// an iron ore, so 69 can see the infernal-furnace bonus metal drop alongside the plain smelt.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:liquefaction"), context);
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);

var focusItem = (fr.wind_blade.isorropia.common.items.ItemFocusLiquefaction)
        fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_LIQUEFACTION.get();
var nuggets = dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "nuggets"));
var potency = dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "potency"));
// Slot 3 (rank 3) only admits frugal/potency/enlarge, so a valid three-nuggets focus fills it with potency.
var state = new dev.tc4port.thaumcraft.api.focus.FocusStateView(
        fr.wind_blade.isorropia.common.items.ItemFocusLiquefaction.ID,
        java.util.List.of(nuggets, nuggets, potency, nuggets));
var upgraded = focusItem.withFocusState(new net.minecraft.world.item.ItemStack(focusItem), state);
sb.append("install=").append(dev.tc4port.thaumcraft.api.focus.FocusApi.install(player,
        net.minecraft.world.InteractionHand.MAIN_HAND, upgraded, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE));

double x = 40.5, y = 310.0, z = 40.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 6; dz++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x) + dx, (int) y - 1, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dz = 1; dz <= 4; dz++) for (int dy = 0; dy <= 3; dy++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x), (int) y + dy, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
for (var stale : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
        new net.minecraft.world.phys.AABB(35, 305, 35, 46, 316, 46))) {
    stale.discard();
}
var target = new net.minecraft.core.BlockPos((int) Math.floor(x), (int) y + 1, (int) Math.floor(z) + 3);
level.setBlock(target, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 3);
player.teleportTo(level, x, y, z, 0.0F, 0.0F);
sb.append(" target=").append(target);

var use = mc.options.keyUse.getKey();
// The server only syncs the wand it just put in hand at the end of its tick, after this probe returns, so the
// client is handed the same wand directly; otherwise it would click with whatever it held before.
var clientWand = player.getMainHandItem().copy();
mc.execute(() -> {
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.setScreen(null);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(0.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();

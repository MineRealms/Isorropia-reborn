// Liquefaction focus, part 4: a self-contained item test. Grants the research, hands over a charged wand with the
// focus, floats a raw iron item squarely in the beam's path and holds use so 67 can check it is smelted in place.
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
var focus = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_LIQUEFACTION.get());
sb.append("install=").append(dev.tc4port.thaumcraft.api.focus.FocusApi.install(player,
        net.minecraft.world.InteractionHand.MAIN_HAND, focus, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE));

// A stone floor, a clear column ahead, and a floating raw iron item in it at eye height.
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
        new net.minecraft.world.phys.AABB(36, 306, 36, 45, 315, 45))) {
    stale.discard();
}
var item = new net.minecraft.world.entity.item.ItemEntity(level, x, y + 1.5, z + 2.0,
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 2));
item.setNoGravity(true);
item.setDeltaMovement(0.0, 0.0, 0.0);
level.addFreshEntity(item);
player.teleportTo(level, x, y, z, 0.0F, 0.0F);
sb.append(" item@").append(item.position());

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

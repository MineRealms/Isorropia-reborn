// Animation focus, part 1: grants the research, installs the focus on a charged wand, clears a small arena, puts a
// gold block three blocks ahead at eye height, and clicks use. Probe bodies run on the server thread, so the golem
// is raised after this returns: check with 78.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:animation"), context);

server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
var focus = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_ANIMATION.get());
var installed = dev.tc4port.thaumcraft.api.focus.FocusApi.install(player, net.minecraft.world.InteractionHand.MAIN_HAND,
        focus, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append("install=").append(installed);

double x = 60.5, y = 310.0, z = 60.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 6; dz++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x) + dx, (int) y - 1, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dz = 0; dz <= 5; dz++) for (int dy = 0; dy <= 3; dy++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x), (int) y + dy, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
for (var stale : level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityAnimatedBlock.class,
        new net.minecraft.world.phys.AABB(56, 306, 56, 65, 315, 66))) {
    stale.discard();
}
var target = new net.minecraft.core.BlockPos((int) Math.floor(x), (int) y + 1, (int) Math.floor(z) + 3);
level.setBlock(target, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(), 3);
sb.append(" type=").append(fr.wind_blade.isorropia.common.entities.AnimatedBlockType.of(level.getBlockState(target)));
sb.append(" target=").append(target);
player.teleportTo(level, x, y, z, 0.0F, 0.0F);

var use = mc.options.keyUse.getKey();
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(0.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();

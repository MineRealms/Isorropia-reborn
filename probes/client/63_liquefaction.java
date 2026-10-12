// Liquefaction focus, part 1: grants the research, puts a charged wand with the focus in hand, builds a small
// stone floor with an ice block three blocks ahead at eye height, and holds the use key so the beam melts it.
// Probe bodies run on the server thread, so the melt only happens after this returns: check with 64 later.
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
var installed = dev.tc4port.thaumcraft.api.focus.FocusApi.install(player, net.minecraft.world.InteractionHand.MAIN_HAND,
        focus, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append("install=").append(installed);

// A stone floor to stand on, and a clear column three blocks ahead ending in an ice block at eye height.
double x = 40.5, y = 310.0, z = 40.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 6; dz++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x) + dx, (int) y - 1, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dz = 1; dz <= 2; dz++) for (int dy = 0; dy <= 2; dy++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x), (int) y + dy, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
var target = new net.minecraft.core.BlockPos((int) Math.floor(x), (int) y + 1, (int) Math.floor(z) + 3);
level.setBlock(target, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState(), 3);
player.teleportTo(level, x, y, z, 0.0F, 0.0F);

var view = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow();
sb.append(" focus=").append(view.installedFocus().map(f -> f.focusId().toString()).orElse("none"))
  .append(" vis=").append(view.visCentivis());
sb.append(" target=").append(target.getX()).append(",").append(target.getY()).append(",").append(target.getZ());

var use = mc.options.keyUse.getKey();
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(0.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();

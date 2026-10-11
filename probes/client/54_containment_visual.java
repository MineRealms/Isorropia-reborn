// Containment focus, visual check: as 52, but on a healthy pig so the beam can be photographed before capture.
// jar, sets a weakened pig five blocks ahead and starts holding right-click on it through the real key path.
// Probe bodies run on the server thread, so the beam only runs after this returns: check with 53 later.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:containment"), context);

// A survival player on an open platform at night is soon knocked off it by a zombie; keep the world peaceful.
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
var focus = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_CONTAINMENT.get());
var installed = dev.tc4port.thaumcraft.api.focus.FocusApi.install(player, net.minecraft.world.InteractionHand.MAIN_HAND,
        focus, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append("install=").append(installed);
player.getInventory().add(new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("warded_jar")));

// Stand on a slab of stone high up, facing +Z, with the pig fenced in five blocks ahead.
double x = 40.5, y = 310.0, z = 40.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 8; dz++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x) + dx, (int) y - 1, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
player.teleportTo(level, x, y, z, 0.0F, 10.0F);
var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(x, y, z + 5.0, 180.0F, 0.0F);
pig.setNoAi(true);
pig.setHealth(pig.getMaxHealth()); // a whole pig takes ten seconds: long enough to photograph the beam
level.addFreshEntity(pig);
sb.append(" pig=").append(pig.getId()).append(" hp=").append(pig.getHealth());
var view = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow();
sb.append(" focus=").append(view.installedFocus().map(f -> f.focusId().toString()).orElse("none"))
  .append(" vis=").append(view.visCentivis());

// Hold right-click the way the mouse does.
var use = mc.options.keyUse.getKey();
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.setYRot(0.0F); mc.player.setXRot(10.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();

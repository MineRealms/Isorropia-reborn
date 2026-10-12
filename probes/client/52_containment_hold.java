// Containment focus, part 1: grants the research, puts a charged wand with the focus in hand, gives one warded
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
// The focus probes share this platform and leave their target blocks in the beam's path; clear it.
for (int dz = 1; dz <= 8; dz++) for (int dy = 0; dy <= 2; dy++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x, y + dy, z + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
// Earlier runs leave creatures here (pigs, a wandering cow); the beam would hold the wrong one, or hop between
// overlapping ones so no hold ever fills.
for (var stale : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
        new net.minecraft.world.phys.AABB(x - 3, y - 1, z - 2, x + 3, y + 4, z + 9))) {
    stale.discard();
}
var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(x, y, z + 5.0, 180.0F, 0.0F);
pig.setNoAi(true);
pig.setHealth(4.0F); // 80 measure: about four seconds of beam
level.addFreshEntity(pig);
sb.append(" pig=").append(pig.getId()).append(" hp=").append(pig.getHealth());
var view = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow();
sb.append(" focus=").append(view.installedFocus().map(f -> f.focusId().toString()).orElse("none"))
  .append(" vis=").append(view.visCentivis());

// Hold right-click the way the mouse does.
var use = mc.options.keyUse.getKey();
// Close any open menu properly first (a menu left open by an earlier probe swallows the click), let go of the
// key, and press only a few frames later.
// The server only syncs the wand it just put in hand at the end of its tick, after this probe returns, so the client
// is handed the same wand directly; otherwise it would click with whatever it held before.
var clientWand = player.getMainHandItem().copy();
mc.execute(() -> {
    if (mc.player != null) {
        mc.player.closeContainer();
        mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    }
    mc.setScreen(null);
    net.minecraft.client.KeyMapping.set(use, false);
});
Thread.sleep(300);
mc.execute(() -> {
    mc.player.setYRot(0.0F); mc.player.setXRot(10.0F);
    net.minecraft.client.KeyMapping.set(use, true);
    net.minecraft.client.KeyMapping.click(use);
});
return sb.toString();

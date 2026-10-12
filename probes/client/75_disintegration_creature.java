// Disintegration focus, part 6: installs a focus carrying the enervation upgrade, spawns an iron golem in the beam's
// path and holds use, so 76 can see it take dissolve damage and the slowness the upgrade leaves behind.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:disintegration"), context);
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
var focusItem = (fr.wind_blade.isorropia.common.items.ItemFocusDisintegration)
        fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_DISINTEGRATION.get();
var frugal = dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "frugal"));
var enervation = dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "enervation"));
var state = new dev.tc4port.thaumcraft.api.focus.FocusStateView(
        fr.wind_blade.isorropia.common.items.ItemFocusDisintegration.ID,
        java.util.List.of(frugal, frugal, frugal, frugal, enervation));
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
var box = new net.minecraft.world.phys.AABB(36, 306, 36, 45, 315, 45);
for (var stale : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
    stale.discard();
}
for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
    if (!(mob instanceof net.minecraft.world.entity.player.Player)) {
        mob.discard();
    }
}
var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
golem.moveTo(x, y, z + 3.5, 0.0F, 0.0F);
level.addFreshEntity(golem);
sb.append(" golem_hp=").append(golem.getHealth()).append("/").append(golem.getMaxHealth());
player.teleportTo(level, x, y, z, 0.0F, 0.0F);

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

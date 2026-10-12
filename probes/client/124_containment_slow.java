// Containment focus slow upgrade, part 1: a focus carrying TH's slow upgrade at its second rank is held on a
// full-health pig, which takes far longer than the test to bottle. 125 checks the pig was slowed.
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
var released = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.setScreen(null);
    net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
    mc.player.stopUsingItem();
    released.complete(true);
});
released.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
var focus = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_CONTAINMENT.get());
var identity = (dev.tc4port.thaumcraft.api.focus.FocusIdentityItem) focus.getItem();
focus = identity.withFocusState(focus, new dev.tc4port.thaumcraft.api.focus.FocusStateView(identity.focusId(focus),
        java.util.List.of(dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.parse("thaumcraft:frugal"),
                dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.parse("isorropia:slow"))));
sb.append("install=").append(dev.tc4port.thaumcraft.api.focus.FocusApi.install(player,
        net.minecraft.world.InteractionHand.MAIN_HAND, focus, dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE));
player.getInventory().add(new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("warded_jar")));

double x = 20.5, y = 310.0, z = 120.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 8; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 2; dy++) {
        level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y + dy, z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
for (var stale : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
        new net.minecraft.world.phys.AABB(x - 4, y - 1, z - 3, x + 4, y + 4, z + 9))) {
    stale.discard();
}
var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(x, y, z + 5.0, 180.0F, 0.0F);
pig.setNoAi(true);
level.addFreshEntity(pig);
System.getProperties().put("isorropia.slowPig", pig.getUUID().toString());
player.connection.teleport(x, y, z, 0.0F, 10.0F);
level.getChunkSource().move(player);

var clientWand = player.getMainHandItem().copy();
var pressed = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(10.0F);
    var key = mc.options.keyUse.getKey();
    net.minecraft.client.KeyMapping.set(key, true);
    net.minecraft.client.KeyMapping.click(key);
    pressed.complete(true);
});
pressed.get(10, java.util.concurrent.TimeUnit.SECONDS);
sb.append(" pigHp=").append(pig.getHealth());
return sb.toString();

// Vis Dynamo selection screen, part 1: an empty-handed click opens TH's six-switch menu on the server, server button
// 2 (ignis) flips ignis off, and a charge pulse then neither takes nor offers ignis. The menu stays open for 115.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
var released = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.setScreen(null);
    net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
    mc.player.stopUsingItem();
    released.complete(true);
});
released.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
player.closeContainer();
server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));

double x = 140.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dz = 0; dz <= 6; dz++) for (int dy = 0; dy <= 3; dy++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x, y + dy, z + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
var dynamoPos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 3);
level.setBlock(net.minecraft.core.BlockPos.containing(x, y, z + 3),
        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
level.setBlock(dynamoPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VIS_DYNAMO.get().defaultBlockState(), 3);
level.setBlock(dynamoPos.south(), dev.tc4port.thaumcraft.api.ThaumcraftContent.block("vis_relay").defaultBlockState(), 3);
player.connection.teleport(x, y, z, 0.0F, 0.0F);
level.getChunkSource().move(player);

var dynamo = (fr.wind_blade.isorropia.common.tiles.TileVisDynamo) level.getBlockEntity(dynamoPos);
for (var channel : dev.tc4port.thaumcraft.api.aspect.VisChannel.values()) {
    if (!dynamo.isEnabled(channel)) dynamo.toggle(channel);
}
var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(dynamoPos),
        net.minecraft.core.Direction.NORTH, dynamoPos, false);
var used = fr.wind_blade.isorropia.common.blocks.BlocksIS.VIS_DYNAMO.get().use(level.getBlockState(dynamoPos), level,
        dynamoPos, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
sb.append("use=").append(used).append(" menu=").append(player.containerMenu.getClass().getSimpleName());
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS;
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER;
sb.append(" button=").append(player.containerMenu.clickMenuButton(player, 2));
sb.append(" ignisOff=").append(!dynamo.isEnabled(ignis)).append(" aerOn=").append(dynamo.isEnabled(aer));

var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
var before = dev.tc4port.thaumcraft.api.wand.WandApi.view(wand).orElseThrow().visCentivis();
dynamo.beginCharging(player, wand);
var after = dev.tc4port.thaumcraft.api.wand.WandApi.view(wand).orElseThrow().visCentivis();
sb.append(" offerIgnis=").append(dynamo.offeredCentivis(ignis)).append(" offerAer=").append(dynamo.offeredCentivis(aer));
sb.append(" paidIgnis=").append(before.get(ignis) - after.get(ignis)).append(" paidAer=").append(before.get(aer) - after.get(aer));
return sb.toString();

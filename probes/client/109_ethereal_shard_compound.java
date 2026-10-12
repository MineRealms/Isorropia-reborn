// Two independent networks: victus needs both terra and aqua. A terra-only source must not fill it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
player.stopUsingItem();
mc.execute(() -> net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false));
var victus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("victus");
var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
var aqua = dev.tc4port.thaumcraft.api.aspect.VisChannel.AQUA.aspectId();
for (int i = 0; i < 2; i++) {
    var pos = new net.minecraft.core.BlockPos(230 + i * 24, 311, 103);
    for (int dz = -1; dz <= 3; dz++) {
        level.removeBlock(pos.offset(0, 0, dz), false);
        level.setBlock(pos.offset(0, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
    level.setBlock(pos, fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get().defaultBlockState(), 3);
    level.setBlock(pos.south(), dev.tc4port.thaumcraft.api.ThaumcraftContent.block("vis_relay").defaultBlockState(), 3);
    var nodePos = pos.south(2);
    level.setBlock(nodePos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("energized_aura_node").defaultBlockState(), 3);
    var node = (dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity) level.getBlockEntity(nodePos);
    var channels = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
    channels.put(terra, 128);
    if (i == 0) channels.put(aqua, 128);
    node.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(java.util.Optional.empty(),
            dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL, dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
            new dev.tc4port.thaumcraft.api.node.NodeVis(channels), dev.tc4port.thaumcraft.api.node.NodeVis.EMPTY));
    var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(pos);
    shard.addEssence(dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
            dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(victus, 2)));
}
// The upstream fractional boundary is strictly >100; also prove partial cents survive NBT.
var block = fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get().defaultBlockState();
var dummy = new fr.wind_blade.isorropia.common.tiles.TileEtherealShard(net.minecraft.core.BlockPos.ZERO, block);
dummy.addEssence(dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
        dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(terra, 2)));
var method = dummy.getClass().getDeclaredMethod("addFractional", dev.tc4port.thaumcraft.api.aspect.AspectId.class, int.class);
method.setAccessible(true);
method.invoke(dummy, terra, 100);
boolean boundary = dummy.stored(terra) == 0;
var restored = new fr.wind_blade.isorropia.common.tiles.TileEtherealShard(net.minecraft.core.BlockPos.ZERO, block);
restored.load(dummy.saveWithoutMetadata());
method.invoke(restored, terra, 1);
return "compoundNetworks=2 boundary100=" + boundary + " fractionalRestored=" + (restored.stored(terra) == 1);

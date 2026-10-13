// Builds a display of the three planar machines, all powered, for a screenshot: the teleological recombinator over a
// node, the vortex attenuator beaming at a hungry node, and the transduction amplifier on an energized node stack.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var origin = new net.minecraft.core.BlockPos(320, 300, 320);
level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);

for (int dx = -4; dx <= 18; dx++) {
    for (int dz = -6; dz <= 8; dz++) {
        for (int dy = -1; dy <= 6; dy++) {
            level.setBlock(origin.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}

var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var ordo = dev.tc4port.thaumcraft.api.aspect.VisChannel.ORDO.aspectId();
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS.aspectId();

// --- 1. Teleological recombinator over a node ---
var recNode = origin;
level.setBlock(recNode, dev.tc4port.thaumcraft.registry.TCBlocks.AURA_NODE.get().defaultBlockState(), 3);
if (level.getBlockEntity(recNode) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity rec) {
    rec.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(java.util.Optional.empty(),
            dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL, dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ordo, 8, ignis, 8)),
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ordo, 8, ignis, 8))));
}
var recPos = recNode.above();
level.setBlock(recPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.RECOMBINATOR.get().defaultBlockState(), 3);
level.setBlock(recPos.north(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
if (level.getBlockEntity(recPos) instanceof fr.wind_blade.isorropia.common.tiles.TileRecombinator recTile) {
    recTile.setShouldActivate(true);
    for (int i = 0; i < 4; i++) {
        fr.wind_blade.isorropia.common.tiles.TileRecombinator.serverTick(level, recPos, level.getBlockState(recPos), recTile);
    }
}

// --- 2. Vortex attenuator aimed at a hungry node ---
var vorNode = origin.offset(7, 0, 0);
level.setBlock(vorNode, dev.tc4port.thaumcraft.registry.TCBlocks.AURA_NODE.get().defaultBlockState(), 3);
if (level.getBlockEntity(vorNode) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity vor) {
    vor.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(java.util.Optional.empty(),
            dev.tc4port.thaumcraft.api.node.NodeTypeId.HUNGRY, dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ignis, 32)),
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(ignis, 32))));
}
var vorFacing = net.minecraft.core.Direction.SOUTH;
var vorPos = vorNode.relative(vorFacing.getOpposite(), 4);
level.setBlock(vorPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX_STABILIZER.get().defaultBlockState()
        .setValue(fr.wind_blade.isorropia.common.blocks.BlockVortexStabilizer.FACING, vorFacing), 3);
level.setBlock(vorPos.above(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
if (level.getBlockEntity(vorPos) instanceof fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer vorTile) {
    for (int i = 0; i < 3; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer.serverTick(level, vorPos, level.getBlockState(vorPos), vorTile);
    }
}

// --- 3. Transduction amplifier on an energized node stack ---
var ampNode = origin.offset(14, 0, 0);
var ampStab = ampNode.below();
var ampTrans = ampNode.above();
level.setBlock(ampStab, dev.tc4port.thaumcraft.registry.TCBlocks.NODE_STABILIZER.get().defaultBlockState(), 3);
level.setBlock(ampTrans, dev.tc4port.thaumcraft.registry.TCBlocks.NODE_TRANSDUCER.get().defaultBlockState(), 3);
level.setBlock(ampNode, dev.tc4port.thaumcraft.registry.TCBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState(), 3);
if (level.getBlockEntity(ampNode) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity ampN) {
    ampN.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(java.util.Optional.empty(),
            dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL, dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(aer, 128)),
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of())));
}
var ampFacing = net.minecraft.core.Direction.SOUTH;
var ampPos = ampNode.relative(ampFacing.getOpposite());
level.setBlock(ampPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.TRANSDUCTION_AMPLIFIER.get().defaultBlockState()
        .setValue(fr.wind_blade.isorropia.common.blocks.BlockTransductionAmplifier.FACING, ampFacing), 3);
level.setBlock(ampPos.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
if (level.getBlockEntity(ampPos) instanceof fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier ampTile) {
    fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier.serverTick(level, ampPos, level.getBlockState(ampPos), ampTile);
    ampTile.setShouldActivate(true);
    fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier.serverTick(level, ampPos, level.getBlockState(ampPos), ampTile);
}

return "scene at " + origin + " rec=" + recPos + " vortex=" + vorPos + " amp=" + ampPos;

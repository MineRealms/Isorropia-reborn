// Builds a fully matured planar vortex so a screenshot can show the rift core and its aspect swirl.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var origin = new net.minecraft.core.BlockPos(300, 300, 300);
level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);

for (int dx = -10; dx <= 10; dx++) {
    for (int dz = -10; dz <= 10; dz++) {
        for (int dy = 0; dy <= 5; dy++) {
            level.setBlock(origin.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(origin.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}
level.setBlock(origin, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
if (level.getBlockEntity(origin) instanceof fr.wind_blade.isorropia.common.tiles.TileVortex vortex) {
    var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
    var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS.aspectId();
    var aqua = dev.tc4port.thaumcraft.api.aspect.VisChannel.AQUA.aspectId();
    var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
    vortex.setAspects(java.util.Map.of(aer, 40, ignis, 35, aqua, 25, terra, 20));
    var state = level.getBlockState(origin);
    for (int i = 0; i < 60; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, origin, state, vortex);
    }
}
return "vortex scene at " + origin;

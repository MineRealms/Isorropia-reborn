// Stage 4 (VFX): stand a fresh rift in front of the player over a wide stone floor so a screenshot can show
// the hungry-node particles (TC4R's HungryNodeClientEffect, driven from the rift's clientTick) streaming
// inward. The rift is left un-pre-ticked so its collapse counter stays near zero and the scene survives the
// settle window.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
if (player == null || player.isDeadOrDying() || player.isRemoved()) {
    player = server.getPlayerList().respawn(player, false);
}
player.setPos(600.5, 220.0, 604.5);
player.setPortalCooldown(0);
var vPos = new net.minecraft.core.BlockPos(600, 220, 612);
for (int dx = -8; dx <= 8; dx++) {
    for (int dz = -8; dz <= 8; dz++) {
        level.setBlock(new net.minecraft.core.BlockPos(600 + dx, 219, 612 + dz),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}
level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,
        new net.minecraft.world.level.ChunkPos(vPos), 3, vPos);
level.removeBlock(vPos, false);
level.setBlock(vPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
var vortex = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(vPos);
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId(), 40);
aspects.put(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId(), 30);
vortex.setAspects(aspects);
sb.append("vortex=").append(vPos).append(" count=").append(vortex.count()).append(" beams=")
        .append(vortex.beams()).append(" player=").append(player.blockPosition()).append("\n");
return sb.toString();

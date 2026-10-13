// Reads back the client-side block entities of the planar scene to prove the render state actually reaches the client,
// and cross-checks the server side of the amplifier stack.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var client = mc.level;
var world = server.overworld();
var sb = new StringBuilder();

var vorPos = new net.minecraft.core.BlockPos(327, 300, 316);
var vor = client.getBlockEntity(vorPos);
sb.append("vortexBE=").append(vor == null ? "null" : vor.getClass().getSimpleName()).append("\n");
if (vor instanceof fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer s) {
    sb.append("vortexRedstoned=").append(s.isRedstoned()).append("\n");
    sb.append("vortexHasBeam=").append(s.hasBeam()).append("\n");
    sb.append("vortexBeamEnd=").append(s.beamEnd()).append("\n");
}

var recPos = new net.minecraft.core.BlockPos(320, 301, 320);
var rec = client.getBlockEntity(recPos);
sb.append("recBE=").append(rec == null ? "null" : rec.getClass().getSimpleName()).append("\n");
if (rec instanceof fr.wind_blade.isorropia.common.tiles.TileRecombinator r) {
    sb.append("recActivated=").append(r.isActivated()).append("\n");
    sb.append("recCount=").append(r.count()).append("\n");
}

var ampPos = new net.minecraft.core.BlockPos(334, 300, 319);
sb.append("clientAmpBlock=").append(client.getBlockState(ampPos).getBlock()).append("\n");
sb.append("clientAmpBE=").append(client.getBlockEntity(ampPos)).append("\n");
sb.append("serverAmpBlock=").append(world.getBlockState(ampPos).getBlock()).append("\n");
sb.append("serverAmpBE=").append(world.getBlockEntity(ampPos)).append("\n");
sb.append("serverNodeBlock=").append(world.getBlockState(ampPos.south()).getBlock()).append("\n");
sb.append("serverStabBlock=").append(world.getBlockState(ampPos.south().below()).getBlock()).append("\n");
sb.append("serverTransBlock=").append(world.getBlockState(ampPos.south().above()).getBlock()).append("\n");
return sb.toString();

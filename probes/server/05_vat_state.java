// A snapshot of the same numbers, taken twice, to tell "not ticking" from "ticking wrongly".
StringBuilder sb = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var parts = new String(java.nio.file.Files.readAllBytes(new java.io.File("rosetta_probes/vat_origin.txt").toPath())).trim().split(",");
var master = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
sb.append("serverTicks=").append(server.getTickCount());
var jarPos = master.offset(0, 0, 1);
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaJarView view) {
    sb.append(" jar=").append(view.visibleAspects().amounts());
} else {
    sb.append(" jar=NOTFOUND(").append(jarPos).append(")");
}
if (level.getBlockEntity(jarPos.below()) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaJarView tubeView) {
    sb.append(" tube=").append(tubeView.visibleAspects().amounts());
}
sb.append("\nvat stored=").append(vat.visibleAspects().amounts()).append(" mode=").append(vat.getMode());
var contained = vat.getContained();
sb.append("\npig hp=").append(contained == null ? "NONE" : contained.getHealth())
  .append(" alive=").append(contained == null ? "-" : contained.isAlive());
sb.append("\nchunkLoaded=").append(level.isLoaded(master)).append(" forcedChunk=")
  .append(level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(master.getX() >> 4, master.getZ() >> 4)));
return sb.toString();

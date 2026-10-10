// Feed the vat the way Thaumcraft does: jar -> tube -> tube -> vat. A jar only gives through its top face.
StringBuilder sb = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var parts = new String(java.nio.file.Files.readAllBytes(new java.io.File("rosetta_probes/vat_origin.txt").toPath())).trim().split(",");
var master = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
var tubeBlock = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("essentia_tube");
var jarBlock = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("warded_jar");
var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);

// jar -> tubeFar -> tubeNear(on the vat) -> the vat
var jarPos = master.offset(0, -1, 2);
var tubeFar = master.offset(0, 0, 2);
var tubeNear = master.offset(0, 0, 1);
for (var pos : new net.minecraft.core.BlockPos[]{master.offset(0, -2, 2), master.offset(0, 1, 1), master.offset(0, -1, 2)}) {
    level.setBlockAndUpdate(pos, air);
}
level.setBlockAndUpdate(tubeNear, tubeBlock.defaultBlockState());
level.setBlockAndUpdate(tubeFar, tubeBlock.defaultBlockState());
level.setBlockAndUpdate(jarPos, jarBlock.defaultBlockState());
long filled = 0;
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar) {
    filled = jar.addEssentia(fr.wind_blade.isorropia.common.IsorropiaAspects.VICTUS, 32,
            net.minecraft.core.Direction.UP, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
}
sb.append("jar=").append(jarPos).append(" filled=").append(filled)
  .append(" tubes=").append(tubeFar).append(",").append(tubeNear);

// A hurt pig and the vat switched on, in idle mode so the cure path runs.
var pig = vat.getContained();
if (pig == null || !pig.isAlive()) {
    if (pig != null) {
        vat.releaseContained();
    }
    pig = net.minecraft.world.entity.EntityType.PIG.create(level);
    pig.moveTo(master.getX() + 0.5, master.getY() - 1.6, master.getZ() + 0.5, 0.0F, 0.0F);
    level.addFreshEntity(pig);
    vat.setContained(pig);
}
vat.setHasEffigy(false);
pig.setHealth(5.0F);
vat.setActive(true);
sb.append("\npig=").append(pig.getId()).append(" hp=").append(pig.getHealth());
sb.append(" mode=").append(vat.getMode()).append(" effigy=").append(vat.getEffigyEssentia());
return sb.toString();

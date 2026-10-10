// Places the matrix on the vat, switches the vat on and runs an infusion, so the matrix can be photographed.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var master = new net.minecraft.core.BlockPos(64, 300, 64);
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
// Clear the leftovers of earlier probes so only the vat and its matrix are in frame.
for (int dx = -6; dx <= 6; dx++) {
    for (int dz = -6; dz <= 6; dz++) {
        for (int dy = 0; dy <= 3; dy++) {
            var pos = master.offset(dx, dy, dz);
            var state = level.getBlockState(pos);
            if (state.is(dev.tc4port.thaumcraft.api.ThaumcraftContent.block("arcane_pedestal"))
                    || state.is(dev.tc4port.thaumcraft.api.ThaumcraftContent.block("essentia_tube"))
                    || state.is(dev.tc4port.thaumcraft.api.ThaumcraftContent.block("warded_jar"))) {
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }
}
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, master);
var above = master.above();
if (!level.getBlockState(above).is(fr.wind_blade.isorropia.common.blocks.BlocksIS.MODIFIED_MATRIX.get())) {
    level.setBlockAndUpdate(above, fr.wind_blade.isorropia.common.blocks.BlocksIS.MODIFIED_MATRIX.get().defaultBlockState());
}
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
vat.abortInfusion();
vat.setHasEffigy(false);
if (vat.getContained() != null) {
    vat.releaseContained();
}
String mode = "active";
if (true) {
    vat.setActive(true);
    for (int i = 0; i < 200; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVat.serverTick(level, master, level.getBlockState(master), vat);
    }
}
return "matrix=" + level.getBlockEntity(above) + " mode=" + mode + " startUp=" + vat.getStartUp()
    + " active=" + vat.isActive() + " craftCount=" + vat.getCraftCount();

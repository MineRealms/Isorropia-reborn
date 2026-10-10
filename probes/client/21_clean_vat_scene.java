// Clears the surroundings of the probe vat so a screenshot shows only the vat, then forms it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var master = new net.minecraft.core.BlockPos(64, 300, 64);
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
var box = new net.minecraft.world.phys.AABB(master).inflate(12, 8, 12);
for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box)) {
    entity.discard();
}
for (int dx = -3; dx <= 3; dx++) {
    for (int dz = -3; dz <= 3; dz++) {
        for (int dy = 0; dy <= 3; dy++) {
            var pos = master.offset(dx, dy, dz);
            if (level.getBlockState(pos).is(dev.tc4port.thaumcraft.api.ThaumcraftContent.block("arcane_pedestal"))) {
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }
}
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, master);
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
vat.abortInfusion();
vat.setHasEffigy(true);
return "cleared and formed, mode=" + vat.getMode();

// Drives the vat into its effigy states so the corpse render can be photographed.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var master = new net.minecraft.core.BlockPos(64, 300, 64);
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
if (vat == null) {
    return "no vat at " + master;
}
var state = level.getBlockState(master);
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
if (vat.getContained() != null) {
    vat.releaseContained();
}
vat.abortInfusion();
vat.setHasEffigy(true);
// A pig standing on the rim from earlier probes would spoil the shot.
for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(master).inflate(6, 4, 6))) {
    entity.discard();
}
String charge = "skip";
if (args != null && String.valueOf(args).contains("charge")) {
    for (int round = 0; round < 60; round++) {
        vat.addEssentia(fr.wind_blade.isorropia.common.IsorropiaAspects.VICTUS, 4, net.minecraft.core.Direction.UP,
                dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
        fr.wind_blade.isorropia.common.tiles.TileVat.serverTick(level, master, state, vat);
    }
    charge = "charged";
}
return charge + " mode=" + vat.getMode() + " essentia=" + vat.getEffigyEssentia();

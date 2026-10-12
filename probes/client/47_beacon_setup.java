// Forms a vat, leaves a body waiting in it, puts a soul beacon on top and binds the player to it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

// A clear spot a little away from the player, at their own height.
if (!player.isAlive()) {
    return "player is dead: run 00_reset first";
}
// A fixed spot clear of the other scenes, so repeated runs do not scatter vats across the sky.
var base = new net.minecraft.core.BlockPos(88, 300, 64);
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, base);
var beaconPos = base.above();
level.setBlock(beaconPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.SOUL_BEACON.get().defaultBlockState(), 3);

var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(base);
if (vat == null || !vat.isMaster()) {
    return "no vat master at " + base + " (got " + level.getBlockEntity(base) + ")";
}
// A soaked effigy is what makes the vat a ready body; 30 victus is what it takes.
vat.setHasEffigy(true);
var essField = fr.wind_blade.isorropia.common.tiles.TileVat.class.getDeclaredField("effigyEssentia");
essField.setAccessible(true);
essField.setInt(vat, 30);
// Give the body an ability so the revival can be seen to hand it over.
var field = fr.wind_blade.isorropia.common.tiles.TileVat.class.getDeclaredField("selfInfusions");
field.setAccessible(true);
((java.util.Set) field.get(vat)).add(fr.wind_blade.isorropia.common.infusion.SelfInfusions.DIAMOND_SKIN);
sb.append("vat=").append(vat.isMaster()).append(" mode=").append(vat.getMode())
  .append(" infusions=").append(vat.getSelfInfusions());

// The player starts with an ability of their own that the old body must lose, and without the body's.
var playerData = fr.wind_blade.isorropia.common.capabilities.LivingData.get(player).orElseThrow();
playerData.resetSelfInfusions();
playerData.addSelfInfusion(fr.wind_blade.isorropia.common.infusion.SelfInfusions.MORPHIC_FINGERS);
playerData.sync();

var beacon = (fr.wind_blade.isorropia.common.tiles.TileSoulBeacon) level.getBlockEntity(beaconPos);
beacon.activate(player);
// Stand the player beside the vat so the death spot is known and in the open.
// Something to stand on beside the vat: a survival player left in the air would fall to their death and spend
// the beacon before the kill probe runs.
for (int dx = 3; dx <= 5; dx++) for (int dz = -1; dz <= 1; dz++) {
    level.setBlockAndUpdate(base.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
}
player.teleportTo(level, base.getX() + 4.5, base.getY() + 1.0, base.getZ() + 0.5, player.getYRot(), player.getXRot());
return sb.toString() + " bound=" + player.getPersistentData().getBoolean("soulBeacon") + " base=" + base;

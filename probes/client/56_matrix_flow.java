// The modified matrix, as upstream: a wand tap switches the vat on; a tap with nothing to infuse leaves it on;
// breaking the matrix stops the vat and loses its wind-up at once. Leaves the matrix back in place with the
// vat on and the player wearing goggles in front of it, so the stability readout can be shot afterwards with
//   python tools/client_probe.py shot matrix_readout 64.5 301.0 61.5 0 15 1 --gui
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var master = new net.minecraft.core.BlockPos(64, 300, 64);
var above = master.above();
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, master);
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
vat.abortInfusion();
vat.setActive(false);
level.setBlockAndUpdate(above, fr.wind_blade.isorropia.common.blocks.BlocksIS.MODIFIED_MATRIX.get().defaultBlockState());
var matrix = (fr.wind_blade.isorropia.common.tiles.TileModifiedMatrix) level.getBlockEntity(above);
var sb = new StringBuilder();

// Any wand will do for a tap.
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
sb.append("tap 1 (off -> on): ").append(matrix.onWandUse(player)).append(" active=").append(vat.isActive());
sb.append("\ntap 2 (nothing to infuse): ").append(matrix.onWandUse(player)).append(" active=").append(vat.isActive())
  .append(" band=").append(vat.stabilityBand()).append(" symmetry=").append(vat.getSymmetryStability())
  .append(" lossPerCycle=").append(vat.lossPerCycle());

// Wind the vat up, then break the matrix: the vat must stop with no wind-up left.
for (int i = 0; i < 100; i++) {
    fr.wind_blade.isorropia.common.tiles.TileVat.serverTick(level, master, level.getBlockState(master), vat);
}
sb.append("\nwound up: startUp=").append(vat.getStartUp());
level.setBlockAndUpdate(above, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
sb.append("\nmatrix broken: active=").append(vat.isActive()).append(" startUp=").append(vat.getStartUp())
  .append(" vatStillFormed=").append(level.getBlockEntity(master) instanceof fr.wind_blade.isorropia.common.tiles.TileVat);

// Put it back, switched on, with goggles on the player for the readout.
level.setBlockAndUpdate(above, fr.wind_blade.isorropia.common.blocks.BlocksIS.MODIFIED_MATRIX.get().defaultBlockState());
matrix = (fr.wind_blade.isorropia.common.tiles.TileModifiedMatrix) level.getBlockEntity(above);
matrix.onWandUse(player);
player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(
        net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "goggles_of_revealing"))));
sb.append("\nready for the readout: active=").append(vat.isActive());
return sb.toString();

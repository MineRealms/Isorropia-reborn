var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var stack = fr.wind_blade.isorropia.common.items.misc.ItemCat.create(
        fr.wind_blade.isorropia.common.items.misc.ItemCat.EnumCat.COW, "dual probe");
var data = fr.wind_blade.isorropia.common.platform.StackData.read(stack);
data.putString("probe", "uncommitted");
if (fr.wind_blade.isorropia.common.platform.StackData.read(stack).contains("probe")) {
    throw new IllegalStateException("StackData.read leaked live data");
}
fr.wind_blade.isorropia.common.platform.StackData.update(stack, tag -> tag.putString("probe", "committed"));
if (!fr.wind_blade.isorropia.common.platform.StackData.read(stack).getString("probe").equals("committed")) {
    throw new IllegalStateException("StackData.update did not persist");
}
if (fr.wind_blade.isorropia.common.items.misc.ItemCat.getType(stack)
        != fr.wind_blade.isorropia.common.items.misc.ItemCat.EnumCat.COW) {
    throw new IllegalStateException("Cat subtype lost");
}
var pos = player.blockPosition().offset(3, 0, 0);
level.setBlock(pos, fr.wind_blade.isorropia.common.blocks.BlocksIS.BLOOD_INFUSER.get().defaultBlockState(), 3);
var tile = (fr.wind_blade.isorropia.common.tiles.TileBloodInfuser) level.getBlockEntity(pos);
tile.addSelected(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("sano"));
tile.addSelected(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("sano"));
var effects = tile.getCurrentEffects();
if (effects.isEmpty() || effects.get(0).getDuration() != 1) {
    throw new IllegalStateException("Healing mixture invalid: " + effects);
}
fr.wind_blade.isorropia.common.platform.Menus.open(player, tile, pos);
return "DUAL_COMPONENTS_OK stackCopy=true subtype=COW instantTicks=1 menu=" + player.containerMenu.getClass().getSimpleName();

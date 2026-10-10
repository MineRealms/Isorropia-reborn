// Fits the scholar's lens on both slots and parks a cow right in front so the readout and the floating name show.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var goggles = new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("goggles_of_revealing"));
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.LEFT,
        fr.wind_blade.isorropia.common.lenses.Lenses.ORDO);
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.RIGHT,
        fr.wind_blade.isorropia.common.lenses.Lenses.ORDO);
player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, goggles);
// One cow, still and close, so the crosshair is on it and the label has room.
var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("minecraft:cow"));
var cow = type.create(level);
cow.moveTo(player.getX(), player.getY(), player.getZ() + 4.0);
cow.setNoGravity(true);
if (cow instanceof net.minecraft.world.entity.Mob mob) {
    mob.setNoAi(true);
}
level.addFreshEntity(cow);
return "ordo fitted; cow at " + cow.blockPosition();

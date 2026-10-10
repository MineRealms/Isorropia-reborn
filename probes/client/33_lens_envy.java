// Fits the trickster's lens and gives the cow in front a part-filled patience bar, so the readout can be seen.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var goggles = new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("goggles_of_revealing"));
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.LEFT,
        fr.wind_blade.isorropia.common.lenses.Lenses.ENVY);
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.RIGHT,
        fr.wind_blade.isorropia.common.lenses.Lenses.ENVY);
player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, goggles);
var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("minecraft:cow"));
var cow = (net.minecraft.world.entity.LivingEntity) type.create(level);
cow.moveTo(player.getX(), player.getY(), player.getZ() + 4.0);
cow.setNoGravity(true);
if (cow instanceof net.minecraft.world.entity.Mob mob) {
    mob.setNoAi(true);
}
level.addFreshEntity(cow);
// Starts from nothing; the server adds while the wearer looks, so a short settle leaves the bar part-filled.
var data = fr.wind_blade.isorropia.common.capabilities.LivingData.get(cow).orElse(null);
if (data != null) {
    data.addEnvy(0);
    data.sync();
}
return "envy fitted; cow at " + cow.blockPosition() + " envy=" + (data == null ? "none" : data.getEnvy());

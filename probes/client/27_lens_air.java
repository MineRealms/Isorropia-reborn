// Fits the kirlian lens and hangs a few creatures in front of the player so the ripples can be screenshotted.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var goggles = new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("goggles_of_revealing"));
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.LEFT,
        fr.wind_blade.isorropia.common.lenses.Lenses.AIR);
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.RIGHT,
        fr.wind_blade.isorropia.common.lenses.Lenses.AIR);
player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, goggles);
double px = player.getX(), py = player.getY(), pz = player.getZ();
var sb = new StringBuilder();
String[][] spawns = {{"minecraft:cow", "-3", "5"}, {"minecraft:zombie", "3", "5"}, {"minecraft:pig", "0", "9"}};
for (String[] spec : spawns) {
    var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse(spec[0]));
    var entity = type.create(level);
    entity.moveTo(px + Double.parseDouble(spec[1]), py, pz + Double.parseDouble(spec[2]));
    entity.setNoGravity(true);
    if (entity instanceof net.minecraft.world.entity.Mob mob) {
        mob.setNoAi(true);
    }
    level.addFreshEntity(entity);
    sb.append(spec[0]).append("@").append(entity.blockPosition()).append(" ");
}
return sb.toString();

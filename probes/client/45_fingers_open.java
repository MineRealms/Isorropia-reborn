// Grows the morphic fingers on the player and opens the portable arcane workbench so the GUI can be shot.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var data = fr.wind_blade.isorropia.common.capabilities.LivingData.get(player).orElse(null);
if (data == null) {
    return "no living data on the player";
}
data.addSelfInfusion(fr.wind_blade.isorropia.common.infusion.SelfInfusions.MORPHIC_FINGERS);
data.sync();
player.openMenu(new net.minecraft.world.SimpleMenuProvider(
        (id, inv, ignored) -> new fr.wind_blade.isorropia.common.menu.MorphicFingersMenu(id, inv),
        net.minecraft.network.chat.Component.translatable("container.isorropia.morphic_fingers")));
return "fingers granted; menu=" + player.containerMenu.getClass().getSimpleName();

// Equips the somatic brain in the player's Curios head slot so a third-person shot can judge its placement.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var brain = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.SOMATIC_BRAIN.get());
var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElse(null);
if (handler == null) {
    return "no curios handler";
}
handler.setEquippedCurio("head", 0, brain);
var after = handler.getStacksHandler("head").map(h -> h.getStacks().getStackInSlot(0).toString()).orElse("no head slot");
return "head[0]=" + after;

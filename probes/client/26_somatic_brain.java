// Puts the somatic brain in the Curios head slot and switches the camera to third person so it can be seen.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
var brain = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.SOMATIC_BRAIN.get());
var equipped = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
        .map(handler -> handler.getStacksHandler("head").map(stacks -> {
            stacks.getStacks().setStackInSlot(0, brain);
            return true;
        }).orElse(false)).orElse(false);
sb.append("equipped=").append(equipped);
sb.append(" headSlot=").append(top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
        .map(handler -> handler.getStacksHandler("head").map(stacks -> stacks.getStacks().getStackInSlot(0).toString())
                .orElse("none")).orElse("no inventory"));
// Third person, and clear any leftover screen so the world shot is clean.
mc.execute(() -> {
    mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
    mc.setScreen(null);
});
return sb.toString();

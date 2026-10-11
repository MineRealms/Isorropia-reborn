// Fills the morphic fingers' grid with the base lens' arcane recipe and its vis crystals, and reports the result.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
if (!(player.containerMenu instanceof fr.wind_blade.isorropia.common.menu.MorphicFingersMenu fingers)) {
    return "menu is " + player.containerMenu.getClass().getName();
}
// The recipe is research-gated; the admin context is what the command uses.
var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
var key = dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:arcanelenses");
var granted = dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player, key, context);

var gold = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT);
var salis = new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("salis_mundus"));
var pane = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_PANE);
// G S G / S P S / G S G
var pattern = new net.minecraft.world.item.ItemStack[]{
        gold, salis, gold, salis, pane, salis, gold, salis, gold};
for (int i = 0; i < 9; i++) {
    fingers.getSlot(1 + i).set(pattern[i].copy());
}
// Five whole vis of each primal, since the superflat world has no vis network to draw on.
var channels = dev.tc4port.thaumcraft.api.aspect.VisChannel.values();
for (int i = 0; i < channels.length; i++) {
    var crystal = dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createCrystalEssence(channels[i].aspectId());
    crystal.setCount(5);
    fingers.getSlot(10 + i).set(crystal);
}
var result = fingers.getSlot(0).getItem();
return "granted=" + granted + " result=" + result + " count=" + result.getCount();

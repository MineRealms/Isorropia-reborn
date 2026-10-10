// Starts a real infusion that is left to tick on its own, so its effects can be photographed.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var master = new net.minecraft.core.BlockPos(64, 300, 64);
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, master);
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
vat.abortInfusion();
vat.setHasEffigy(false);
if (vat.getContained() != null) {
    vat.releaseContained();
}
// Pedestals with the awakened blood infusion, and the matrix on top to switch the vat on.
var pedestalBlock = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("arcane_pedestal");
var items = new net.minecraft.world.item.ItemStack[]{
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLISTERING_MELON_SLICE),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLISTERING_MELON_SLICE),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE),
        new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("silverwood_log"))};
var positions = new net.minecraft.core.BlockPos[]{
        master.offset(2, 0, 0), master.offset(-2, 0, 0), master.offset(0, 0, 2), master.offset(0, 0, -2)};
for (int i = 0; i < positions.length; i++) {
    level.setBlockAndUpdate(positions[i], pedestalBlock.defaultBlockState());
    if (level.getBlockEntity(positions[i]) instanceof dev.tc4port.thaumcraft.api.machine.InfusionPedestal pedestal) {
        pedestal.setItem(0, items[i]);
    }
}
var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(master.getX() + 0.5, master.getY() - 1.6, master.getZ() + 0.5, 0.0F, 0.0F);
level.addFreshEntity(pig);
vat.setContained(pig);
var fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
for (String key : new String[]{"isorropia:creatureinfusions", "isorropia:curativevat"}) {
    dev.tc4port.thaumcraft.api.research.ResearchApi.complete(fake,
            dev.tc4port.thaumcraft.api.research.ResearchKey.parse(key), context);
}
vat.setActive(true);
boolean started = vat.startInfusion(fake);
// The essentia the infusion needs would normally arrive by tube; the probe keeps topping the vat up.
for (int i = 0; i < 40; i++) {
    vat.addEssentia(fr.wind_blade.isorropia.common.IsorropiaAspects.VICTUS, 4, net.minecraft.core.Direction.UP,
            dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
    vat.addEssentia(fr.wind_blade.isorropia.common.IsorropiaAspects.COGNITIO, 4, net.minecraft.core.Direction.UP,
            dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
}
return "started=" + started + " mode=" + vat.getMode() + " (left to tick)";

// A creature infusion from start to finish: pedestals, a pig, the matrix switch, and essentia fed by hand
// through the same entry point a tube uses.
StringBuilder sb = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var parts = new String(java.nio.file.Files.readAllBytes(new java.io.File("rosetta_probes/vat_origin.txt").toPath())).trim().split(",");
var master = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
var state = level.getBlockState(master);
level.setChunkForced(master.getX() >> 4, master.getZ() >> 4, true);
if (vat.getContained() != null) {
    vat.releaseContained();
}
vat.setHasEffigy(false);
vat.abortInfusion();
vat.setActive(false);

// Four pedestals with the awakened blood infusion on them.
var pedestalBlock = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("arcane_pedestal");
var items = new net.minecraft.world.item.ItemStack[]{
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLISTERING_MELON_SLICE),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLISTERING_MELON_SLICE),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE),
        new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("silverwood_log"))};
var pedestalPositions = new net.minecraft.core.BlockPos[]{
        master.offset(2, 0, 0), master.offset(-2, 0, 0), master.offset(0, 0, 2), master.offset(0, 0, -2)};
int placed = 0;
for (int i = 0; i < pedestalPositions.length; i++) {
    var pos = pedestalPositions[i];
    if (!level.getBlockState(pos).is(pedestalBlock)) {
        level.setBlockAndUpdate(pos, pedestalBlock.defaultBlockState());
    }
    if (level.getBlockEntity(pos) instanceof dev.tc4port.thaumcraft.api.machine.InfusionPedestal pedestal) {
        pedestal.setItem(0, items[i]);
        placed++;
    }
}
sb.append("pedestals=").append(placed);

var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
pig.moveTo(master.getX() + 0.5, master.getY() - 1.6, master.getZ() + 0.5, 0.0F, 0.0F);
level.addFreshEntity(pig);
vat.setContained(pig);
sb.append(" pig=").append(pig.getId());

var fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
// A fake player's research lives only for the session, so the gate is opened again here.
var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
for (String key : new String[]{"isorropia:creatureinfusions", "isorropia:curativevat", "isorropia:selfinfusion"}) {
    dev.tc4port.thaumcraft.api.research.ResearchApi.complete(fake,
            dev.tc4port.thaumcraft.api.research.ResearchKey.parse(key), context);
}
vat.setActive(true); // the matrix does this before it starts an infusion
boolean started = vat.startInfusion(fake);
sb.append("\nstarted=").append(started).append(" mode=").append(vat.getMode()).append(" recipe=").append(vat.getCreatureRecipe());

// Feed the infusion: the same call a tube makes, repeated as the steps demand essentia.
var victus = fr.wind_blade.isorropia.common.IsorropiaAspects.VICTUS;
var cognitio = fr.wind_blade.isorropia.common.IsorropiaAspects.COGNITIO;
for (int round = 0; round < 40; round++) {
    for (int i = 0; i < 4; i++) {
        vat.addEssentia(victus, 4, net.minecraft.core.Direction.UP, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
        vat.addEssentia(cognitio, 4, net.minecraft.core.Direction.UP, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
    }
    for (int tick = 0; tick < 20; tick++) {
        fr.wind_blade.isorropia.common.tiles.TileVat.serverTick(level, master, state, vat);
    }
    if (vat.getMode() != 2) {
        sb.append("\nfinished after round ").append(round);
        break;
    }
}
sb.append("\nmode=").append(vat.getMode());
var data = fr.wind_blade.isorropia.common.capabilities.LivingData.get(pig).orElse(null);
sb.append("\npigInfusions=").append(data == null ? "NO CAPABILITY" : data.getInfusions().keySet());
sb.append("\npigOrgan(blood)=").append(data == null ? "-" : data.getOrgan("blood"));
sb.append("\npigOwner=").append(data == null ? "-" : data.getOwner());
var pedestalsLeft = 0;
for (var pos : pedestalPositions) {
    if (level.getBlockEntity(pos) instanceof dev.tc4port.thaumcraft.api.machine.InfusionPedestal pedestal
            && !pedestal.item().isEmpty()) {
        pedestalsLeft++;
    }
}
sb.append("\npedestalsStillHolding=").append(pedestalsLeft);
return sb.toString();

// NeoForge flavour: caps are queried through the level, not the block entity.
// TC4R 20740 turned vis sources and essentia transport into capabilities (VisNetworkApi.registerProvider
// is gone and EssentiaApi.transport resolves peers only through EssentiaCapabilities.TRANSPORT). Every
// Isorropia machine that exposes one is built and exercised here on the real server thread:
//   vis dynamo      -> VisSource, charged from a wand, drained through the network
//   essentia dynamo -> VisSource + EssentiaTransport, drawing from a warded jar and offering centivis
//   blood infuser   -> EssentiaTransport, resolved by EssentiaApi.transport
//   curative vat    -> EssentiaTransport, resolved by EssentiaApi.transport
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
double x = 140.5, y = 310.0, z = 160.5;
for (int dx = -4; dx <= 10; dx++) for (int dz = -4; dz <= 6; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 3; dy++) {
        level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y + dy, z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
player.teleportTo(level, x, y + 1.0, z, 0.0F, 0.0F);
var visCap = dev.tc4port.thaumcraft.api.node.VisCapabilities.SOURCE;
var transportCap = dev.tc4port.thaumcraft.api.essentia.EssentiaCapabilities.TRANSPORT;
var relay = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("vis_relay");

// --- vis dynamo: cap present, charged by a wand, then drained through the network ---
var visPos = net.minecraft.core.BlockPos.containing(x, y, z);
level.setBlockAndUpdate(visPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VIS_DYNAMO.get().defaultBlockState());
level.setBlockAndUpdate(visPos.east(), relay.defaultBlockState());
var vis = (fr.wind_blade.isorropia.common.tiles.TileVisDynamo) level.getBlockEntity(visPos);
sb.append("vis_cap=").append(level.getCapability(visCap, visPos, null) != null);
var wand = fr.wind_blade.isorropia.common.wand.CrystalWandParts.chargedWand();
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
vis.beginCharging(player, wand);
sb.append(" vis_supplying=").append(vis.canSupplyVis())
  .append(" vis_endpoint=").append(vis.networkEndpoint() != null && vis.networkEndpoint().equals(visPos));
// The dynamo only exposes its offer once its own tick has copied the pulse into `provided`.
fr.wind_blade.isorropia.common.tiles.TileVisDynamo.serverTick(level, visPos, level.getBlockState(visPos), vis);
// A consumer standing on the dynamo: the graph finds the wireless source one block away.
int drained = dev.tc4port.thaumcraft.api.node.VisNetworkApi.drain(level, visPos.above(),
        dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS, 3,
        dev.tc4port.thaumcraft.api.aspect.VisAction.EXECUTE);
sb.append(" vis_drained=").append(drained);

// --- essentia dynamo: both caps, jar below, then it draws and offers centivis ---
var jarPos = net.minecraft.core.BlockPos.containing(x + 4, y, z);
var edPos = jarPos.above();
level.setBlockAndUpdate(jarPos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("warded_jar").defaultBlockState());
long filled = 0;
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar) {
    filled = jar.addEssentia(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:ignis"), 32,
            net.minecraft.core.Direction.UP, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
}
level.setBlockAndUpdate(edPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.ESSENTIA_DYNAMO.get().defaultBlockState());
level.setBlockAndUpdate(edPos.east(), relay.defaultBlockState());
var ed = (fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo) level.getBlockEntity(edPos);
sb.append("\ned_filled=").append(filled)
  .append(" ed_visCap=").append(level.getCapability(visCap, edPos, null) != null)
  .append(" ed_transportCap=").append(level.getCapability(transportCap, edPos, null) != null)
  .append(" ed_resolved=").append(dev.tc4port.thaumcraft.api.essentia.EssentiaApi.transport(level, edPos,
          net.minecraft.core.Direction.DOWN) == level.getBlockEntity(edPos));
for (int i = 0; i < 40; i++) {
    fr.wind_blade.isorropia.common.tiles.TileEssentiaDynamo.serverTick(level, edPos,
            level.getBlockState(edPos), ed);
}
sb.append(" ed_ticks=").append(ed.ticksProvided())
  .append(" ed_provided=").append(ed.providedCentivis(dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS))
  .append(" ed_supplying=").append(ed.canSupplyVis())
  .append(" ed_takenFromJar=").append(32 - (level.getBlockEntity(jarPos)
          instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport j ? j.essentiaAmount(net.minecraft.core.Direction.UP) : -1));

// --- blood infuser and curative vat: transport resolves through the capability ---
var infPos = net.minecraft.core.BlockPos.containing(x + 8, y, z);
level.setBlockAndUpdate(infPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.BLOOD_INFUSER.get().defaultBlockState());
sb.append("\ninfuser_cap=").append(level.getCapability(transportCap, infPos, null) != null)
  .append(" infuser_resolved=").append(dev.tc4port.thaumcraft.api.essentia.EssentiaApi.transport(level, infPos,
          net.minecraft.core.Direction.UP) == level.getBlockEntity(infPos));
var vatPos = net.minecraft.core.BlockPos.containing(x + 8, y, z + 3);
level.setBlockAndUpdate(vatPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.CURATIVE_VAT.get().defaultBlockState());
sb.append("\nvat_cap=").append(level.getCapability(transportCap, vatPos, null) != null)
  .append(" vat_resolved=").append(dev.tc4port.thaumcraft.api.essentia.EssentiaApi.transport(level, vatPos,
          net.minecraft.core.Direction.UP) == level.getBlockEntity(vatPos));
return sb.toString();

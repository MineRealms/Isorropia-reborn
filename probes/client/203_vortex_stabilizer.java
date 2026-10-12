// Vortex attenuator: prove registration (block/item/research/recipe) and that a redstone-powered beam aimed at a
// hungry aura node switches it to a normal node, then restores it when the beam is removed.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

// --- registration checks ---
var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
        fr.wind_blade.isorropia.common.items.ItemsIS.VORTEX_STABILIZER.get());
sb.append("itemId=").append(itemId).append("\n");
sb.append("blockPresent=").append(fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX_STABILIZER.isPresent()).append("\n");

var rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/vortex_stabilizer");
var recipe = server.getRecipeManager().byKey(rl);
sb.append("recipePresent=").append(recipe.isPresent()).append("\n");
if (recipe.isPresent()) {
    sb.append("recipeType=").append(recipe.get().getType()).append("\n");
}

// The amplifier recipe was corrected to use the Node Transducer catalyst (blockStoneDevice.11); confirm it still loads.
var ampRl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "arcane/transduction_amplifier");
sb.append("ampRecipePresent=").append(server.getRecipeManager().byKey(ampRl).isPresent()).append("\n");

var research = dev.tc4port.thaumcraft.research.ResearchCatalog.get(
        new dev.tc4port.thaumcraft.api.research.ResearchKey("isorropia", "vortexStabilizer"));
sb.append("researchPresent=").append(research != null).append("\n");
if (research != null) {
    sb.append("parents=").append(research.parents()).append("\n");
    sb.append("pages=").append(research.pages().size()).append("\n");
}

// --- runtime: aim the beam at a hungry node ---
var nodePos = new net.minecraft.core.BlockPos(230, 311, 130);
level.setBlock(nodePos, dev.tc4port.thaumcraft.registry.TCBlocks.AURA_NODE.get().defaultBlockState(), 3);
if (!(level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity node)) {
    return sb.append("no_node_be\n").toString();
}
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
node.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(
        java.util.Optional.empty(),
        dev.tc4port.thaumcraft.api.node.NodeTypeId.HUNGRY,
        dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(aer, 64)),
        new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(aer, 64))));

// Machine five blocks north of the node, firing south straight at it, powered by a redstone block beneath it.
var facing = net.minecraft.core.Direction.SOUTH;
var machinePos = nodePos.relative(facing.getOpposite(), 5);
for (int dz = -1; dz <= 1; dz++) {
    level.setBlock(machinePos.offset(0, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
var machineState = fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX_STABILIZER.get()
        .defaultBlockState().setValue(fr.wind_blade.isorropia.common.blocks.BlockVortexStabilizer.FACING, facing);
level.setBlock(machinePos, machineState, 3);
// A redstone block directly above face-adjacent powers the machine without sitting in the beam.
level.setBlock(machinePos.above(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
if (!(level.getBlockEntity(machinePos) instanceof fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer stabilizer)) {
    return sb.append("no_machine_be\n").toString();
}
sb.append("nodeTypeBefore=").append(node.nodeState().type()).append("\n");

// Probe chunks may not tick, so drive the machine directly; the scan is gated on gameTime % 5, and the short
// sleeps let the server clock advance so some calls fall on a scan tick.
for (int i = 0; i < 120 && !stabilizer.hasBeam(); i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer.serverTick(level, machinePos, machineState, stabilizer);
    try { Thread.sleep(10); } catch (InterruptedException ignored) { }
}
sb.append("redstoned=").append(stabilizer.isRedstoned()).append("\n");
sb.append("hasBeam=").append(stabilizer.hasBeam()).append("\n");
sb.append("beamEnd=").append(stabilizer.beamEnd()).append("\n");
sb.append("nodeTypePowered=").append(node.nodeState().type()).append("\n");

// Cut the power; the beam should drop and the node should go back to hungry.
level.removeBlock(machinePos.above(), false);
stabilizer.setRedstoned(level.hasNeighborSignal(machinePos));
var hungry = new dev.tc4port.thaumcraft.api.node.NodeTypeId(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "hungry"));
for (int i = 0; i < 120 && !node.nodeState().type().equals(hungry); i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer.serverTick(level, machinePos, machineState, stabilizer);
    try { Thread.sleep(10); } catch (InterruptedException ignored) { }
}
sb.append("redstonedAfter=").append(stabilizer.isRedstoned()).append("\n");
sb.append("nodeTypeReleased=").append(node.nodeState().type()).append("\n");

// clean up
level.removeBlock(machinePos, false);
level.removeBlock(nodePos, false);
return sb.toString();

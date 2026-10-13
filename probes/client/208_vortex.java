// Vortex: prove registration, that four amplifiers collapse an energized node into a vortex carrying the node's base
// aspects, and that the rift itself ramps up, swallows its 5x5x5, drags entities in, grinds nearby soft blocks, is
// damped by six attenuator beams, and finally collapses.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

// --- registration checks ---
var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
        fr.wind_blade.isorropia.common.items.ItemsIS.VORTEX.get());
sb.append("itemId=").append(itemId).append("\n");
sb.append("blockPresent=").append(fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.isPresent()).append("\n");
sb.append("bePresent=").append(fr.wind_blade.isorropia.common.tiles.TilesIS.VORTEX.isPresent()).append("\n");

var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();

// --- four amplifiers collapse an energized node ---
var nodePos = new net.minecraft.core.BlockPos(400, 300, 400);
var stabPos = nodePos.below();
var transPos = nodePos.above();
for (int dx = -6; dx <= 6; dx++) {
    for (int dz = -6; dz <= 6; dz++) {
        level.setBlock(nodePos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}
level.setBlock(stabPos, dev.tc4port.thaumcraft.registry.TCBlocks.NODE_STABILIZER.get().defaultBlockState(), 3);
level.setBlock(transPos, dev.tc4port.thaumcraft.registry.TCBlocks.NODE_TRANSDUCER.get().defaultBlockState(), 3);
level.setBlock(nodePos, dev.tc4port.thaumcraft.registry.TCBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState(), 3);
if (level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity node) {
    node.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(java.util.Optional.empty(),
            dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL, dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(aer, 128)),
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of())));
}
for (var dir : new net.minecraft.core.Direction[] { net.minecraft.core.Direction.NORTH,
        net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST }) {
    var ampPos = nodePos.relative(dir);
    var ampState = fr.wind_blade.isorropia.common.blocks.BlocksIS.TRANSDUCTION_AMPLIFIER.get().defaultBlockState()
            .setValue(fr.wind_blade.isorropia.common.blocks.BlockTransductionAmplifier.FACING, dir.getOpposite());
    level.setBlock(ampPos, ampState, 3);
}
sb.append("nodeBefore=").append(level.getBlockState(nodePos).getBlock()).append("\n");

int ticks = 0;
for (; ticks < 400; ticks++) {
    for (var dir : new net.minecraft.core.Direction[] { net.minecraft.core.Direction.NORTH,
            net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST }) {
        var ampPos = nodePos.relative(dir);
        if (level.getBlockEntity(ampPos) instanceof fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier amp) {
            amp.setShouldActivate(true);
            fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier.serverTick(level, ampPos,
                    level.getBlockState(ampPos), amp);
        }
    }
    if (level.getBlockState(nodePos).getBlock() == fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get()) {
        break;
    }
}
sb.append("collapsed=").append(level.getBlockState(nodePos).getBlock()
        == fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get()).append("\n");
sb.append("collapseTicks=").append(ticks).append("\n");
if (level.getBlockEntity(nodePos) instanceof fr.wind_blade.isorropia.common.tiles.TileVortex collapsed) {
    sb.append("collapsedAspects=").append(collapsed.aspectMap()).append("\n");
}

// --- a fresh rift: ramp up, eat its 5x5x5, drag an item, grind nearby glass ---
var vPos = new net.minecraft.core.BlockPos(430, 300, 430);
for (int dx = -10; dx <= 10; dx++) {
    for (int dz = -10; dz <= 10; dz++) {
        level.setBlock(vPos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}
level.setBlock(vPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
var vortex = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(vPos);
vortex.setAspects(java.util.Map.of(aer, 20));
var vortexState = level.getBlockState(vPos);

// A marker inside the 5x5x5 (eaten once mature) and a ring of glass outside it but within the grind range.
var markerPos = vPos.offset(2, 0, 0);
level.setBlock(markerPos, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3);
int glassBefore = 0;
for (int dx = -4; dx <= 4; dx++) {
    for (int dz = -4; dz <= 4; dz++) {
        if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
            continue;
        }
        if ((dx + dz) % 2 != 0) {
            continue;
        }
        level.setBlock(vPos.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), 3);
        glassBefore++;
    }
}
var item = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
        vPos.getZ() + 6.5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
level.addFreshEntity(item);

for (int i = 0; i < 60; i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vortexState, vortex);
}
sb.append("countAfterRamp=").append(vortex.count()).append("\n");
sb.append("markerEaten=").append(level.getBlockState(markerPos).isAir()).append("\n");

for (int i = 0; i < 400; i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vortexState, vortex);
}
int glassAfter = 0;
for (int dx = -4; dx <= 4; dx++) {
    for (int dz = -4; dz <= 4; dz++) {
        if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
            continue;
        }
        if ((dx + dz) % 2 != 0) {
            continue;
        }
        if (level.getBlockState(vPos.offset(dx, 0, dz)).is(net.minecraft.world.level.block.Blocks.GLASS)) {
            glassAfter++;
        }
    }
}
sb.append("glassBefore=").append(glassBefore).append(" glassAfter=").append(glassAfter).append("\n");
sb.append("groundBlocks=").append(glassAfter < glassBefore).append("\n");
sb.append("itemPulled=").append(item.getDeltaMovement().lengthSqr() > 0.0).append("\n");

// --- damping: six beams pin the rift, so it neither eats nor winds its clock up ---
var dampedPos = new net.minecraft.core.BlockPos(460, 300, 460);
level.setBlock(dampedPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
var damped = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(dampedPos);
damped.setAspects(java.util.Map.of(aer, 20));
damped.setBeams(6);
var dampedState = level.getBlockState(dampedPos);
for (int i = 0; i < 80; i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, dampedPos, dampedState, damped);
}
sb.append("dampedCount=").append(damped.count()).append("\n");
sb.append("dampedHeld=").append(damped.count() == 50 && damped.beams() == 6).append("\n");

// --- collapse: past the maximum the rift tears itself apart ---
var boomPos = new net.minecraft.core.BlockPos(475, 300, 475);
level.setBlock(boomPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
var boom = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(boomPos);
boom.setAspects(java.util.Map.of(aer, 20));
var boomState = level.getBlockState(boomPos);
int collapseTicks = 0;
for (; collapseTicks < 4000
        && level.getBlockState(boomPos).getBlock() == fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get();
        collapseTicks++) {
    fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, boomPos, boomState, boom);
}
sb.append("collapseTicks2=").append(collapseTicks).append("\n");
sb.append("collapsedAway=").append(
        level.getBlockState(boomPos).getBlock() != fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get())
        .append("\n");
sb.append("collapsedInto=").append(level.getBlockState(boomPos).getBlock()).append("\n");

// --- an attenuator pins the rift: the beam adds a "beam" of suppression while it holds ---
var pinPos = new net.minecraft.core.BlockPos(490, 300, 490);
var pinVortexPos = pinPos.relative(net.minecraft.core.Direction.SOUTH, 4);
for (int dx = -2; dx <= 6; dx++) {
    for (int dz = -2; dz <= 6; dz++) {
        level.setBlock(pinPos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    }
}
level.setBlock(pinVortexPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
var pinned = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(pinVortexPos);
pinned.setAspects(java.util.Map.of(aer, 20));

var pinFacing = net.minecraft.core.Direction.SOUTH;
level.setBlock(pinPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX_STABILIZER.get().defaultBlockState()
        .setValue(fr.wind_blade.isorropia.common.blocks.BlockVortexStabilizer.FACING, pinFacing), 3);
level.setBlock(pinPos.above(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
var attenuator = (fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer) level.getBlockEntity(pinPos);
var pinState = level.getBlockState(pinPos);
for (int i = 0; i < 5; i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer.serverTick(level, pinPos, pinState, attenuator);
}
sb.append("pinnedTargetIsVortex=").append(attenuator.target()
        instanceof fr.wind_blade.isorropia.common.tiles.TileVortex).append("\n");
sb.append("pinnedBeams=").append(pinned.beams()).append("\n");

level.removeBlock(pinPos.above(), false);
attenuator.setRedstoned(level.hasNeighborSignal(pinPos));
for (int i = 0; i < 5; i++) {
    fr.wind_blade.isorropia.common.tiles.TileVortexStabilizer.serverTick(level, pinPos, pinState, attenuator);
}
sb.append("releasedBeams=").append(pinned.beams()).append("\n");

// clean up
level.removeBlock(pinPos, false);
level.removeBlock(pinVortexPos, false);
level.removeBlock(vPos, false);
level.removeBlock(dampedPos, false);
level.removeBlock(dampedPos.above(), false);
level.removeBlock(nodePos, false);
level.removeBlock(transPos, false);
level.removeBlock(stabPos, false);
return sb.toString();

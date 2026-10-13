// Stage 3, part 1: build a planar gateway wall in the world, slot a keystone bound to a fresh plane into the
// receptacle, charge it with a staff, and cut the portal. Asserts the world-side frame and opening, the plane-side
// twin tile's destination, and the plane's recorded gateway slot.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
if (pocket == null) {
    return "no_pocket_level";
}

var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
aspects.put(terra, 40);
aspects.put(aer, 20);
var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.create(pocket, store, "Gateway Probe Plane",
        aspects, net.minecraft.world.level.Level.OVERWORLD, new net.minecraft.core.BlockPos(0, 200, 0));
int radius = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.radiusFor(aspects);
sb.append("plane=").append(id).append(" radius=").append(radius).append("\n");

// Clear a stage for the wall, then build the five-by-five ring: thaumium at the corners, arcane stone along the edges,
// with the receptacle in the middle of the top row. The three-by-three opening stays air.
int bx = 100, by = 200, bz = 100;
for (int dx = -3; dx <= 3; dx++) {
    for (int dy = -6; dy <= 2; dy++) {
        for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(new net.minecraft.core.BlockPos(bx + dx, by + dy, bz + dz),
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
var slotPos = new net.minecraft.core.BlockPos(bx, by, bz);
level.setBlock(slotPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.SLOT.get().defaultBlockState(), 3);
var corner = dev.tc4port.thaumcraft.registry.TCBlocks.byId("thaumium_block").get();
var edge = dev.tc4port.thaumcraft.registry.TCBlocks.byId("arcane_stone").get();
for (int across = -2; across <= 2; across++) {
    for (int down = 0; down >= -4; down--) {
        if (across == 0 && down == 0) {
            continue;
        }
        boolean ring = down == 0 || down == -4 || across == -2 || across == 2;
        if (!ring) {
            continue;
        }
        boolean isCorner = (across == -2 || across == 2) && (down == 0 || down == -4);
        level.setBlock(new net.minecraft.core.BlockPos(bx + across, by + down, bz),
                (isCorner ? corner : edge).defaultBlockState(), 3);
    }
}

var slot = (fr.wind_blade.isorropia.common.tiles.TileSlot) level.getBlockEntity(slotPos);
if (slot == null) {
    return "no_slot";
}
int colour = fr.wind_blade.isorropia.common.world.PocketPlaneBuilder.colourOf(aspects);
slot.insertKeystone(id, colour);
sb.append("hasKeystone=").append(slot.hasKeystone()).append(" plane=").append(slot.planeId())
        .append(" colour=").append(Integer.toHexString(slot.planeColour())).append("\n");

// A staff that can pay the 250-vis-each charge: a silverwood staff holds 250 per channel, and a thaumium cap trims 10%.
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.parse("silverwood_staff"),
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.parse("thaumium"));
sb.append("wandVis=").append(dev.tc4port.thaumcraft.api.wand.WandApi.view(wand).orElseThrow().visCentivis())
        .append("\n");

slot.makePortal(wand, player);
sb.append("portalOpen=").append(slot.portalOpen()).append(" which=").append(slot.which()).append("\n");
sb.append("firstFree=")
        .append(fr.wind_blade.isorropia.common.world.PocketGateways.firstAvailablePortal(store, id)).append("\n");

// TH turned the wall into fifteen frame cells (metadata 0..14) and a nine-cell opening; the count must come out exact.
int gateway = 0;
int portal = 0;
for (int across = -2; across <= 2; across++) {
    for (int down = 0; down >= -4; down--) {
        if (across == 0 && down == 0) {
            continue;
        }
        var st = level.getBlockState(new net.minecraft.core.BlockPos(bx + across, by + down, bz));
        if (st.is(fr.wind_blade.isorropia.common.blocks.BlocksIS.GATEWAY.get())) {
            gateway++;
        } else if (st.is(fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get())) {
            portal++;
        }
    }
}
sb.append("gatewayCells=").append(gateway).append(" portalCells=").append(portal).append("\n");

// The plane-side twin must be carved on the side the plane handed out, with its destination recorded.
int which = slot.which();
var twin = fr.wind_blade.isorropia.common.world.PocketGateways.twinPos(id, radius, which);
if (twin == null) {
    return sb + "twin=null\n";
}
sb.append("twin=").append(twin).append(" isPortal=")
        .append(pocket.getBlockState(twin).is(fr.wind_blade.isorropia.common.blocks.BlocksIS.PORTAL.get()))
        .append("\n");
if (pocket.getBlockEntity(twin) instanceof fr.wind_blade.isorropia.common.tiles.TilePortal tp) {
    sb.append("twinPlane=").append(tp.planeId()).append(" twinWhich=").append(tp.which())
            .append(" dest=").append(tp.destination()).append(" arrival=").append(tp.arrival()).append("\n");
} else {
    sb.append("twinTile=missing\n");
}
var recorded = store.plane(id);
sb.append("storePortal=").append(recorded == null ? "null" : recorded.portalPos(which))
        .append(" storeDim=").append(recorded == null ? "null" : recorded.portalDim(which)).append("\n");
return sb.toString();

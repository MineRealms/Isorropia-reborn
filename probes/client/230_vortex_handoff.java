// Stage 4: a fully damped rift (six beams) stops eating, and a primordial pearl dropped into it seeds a pocket plane
// (TH's handlePocketPlaneStuff -> createDimension). The rift becomes that plane's doorway and carries a traveller
// standing in it across; the plane's exit rift is born a cheat doorway that carries them home again.
//
// Entities only enter a level's entity list inside entity-ticking chunks, so the player is parked twenty blocks away
// (on a floor) to make the rift's chunk tick; the pearl is then found by the rift's own ticker, driven directly here.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
try {
    if (player == null || player.isDeadOrDying() || player.isRemoved()) {
        player = server.getPlayerList().respawn(player, false);
        sb.append("revived=").append(player != null && !player.isDeadOrDying()).append("\n");
    }
    var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
    if (pocket == null) {
        return "no_pocket_level";
    }
    var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
    var terra = dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId();
    var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
    aspects.put(terra, 40);
    aspects.put(aer, 20);

    var vPos = new net.minecraft.core.BlockPos(620, 200, 600);
    // A floor from the player's spot to the rift, so the player does not fall and the rift's chunk ticks entities.
    for (int x = 596; x <= 624; x++) {
        for (int dz = -4; dz <= 4; dz++) {
            level.setBlock(new net.minecraft.core.BlockPos(x, 199, 600 + dz),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
    }
    player.setPos(600.5, 200.0, 600.5);
    sb.append("parked=").append(player.blockPosition()).append(" raw=").append(player.getX()).append(",")
            .append(player.getY()).append(",").append(player.getZ()).append(" dim=")
            .append(player.level().dimension().location()).append("\n");
    player.setPortalCooldown(0);
    level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,
            new net.minecraft.world.level.ChunkPos(vPos), 3, vPos);
    level.removeBlock(vPos, false);
    level.setBlock(vPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
    var vortex = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(vPos);
    if (vortex == null) {
        return sb + "no_vortex_tile\n";
    }
    vortex.setAspects(aspects);
    vortex.setBeams(6);
    var vState = level.getBlockState(vPos);

    // Drop a primordial pearl into the damped rift; it must be visible to the rift and seed a plane.
    var pearl = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(
                    dev.tc4port.thaumcraft.registry.TCItems.PRIMORDIAL_PEARL.get()));
    pearl.setNoGravity(true);
    level.addFreshEntity(pearl);
    sb.append("pearlVisible=").append(level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
            new net.minecraft.world.phys.AABB(vPos).inflate(1.0)).size()).append("\n");

    for (int i = 0; i < 60; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vState, vortex);
    }
    sb.append("created=").append(vortex.createdDimension()).append(" planeId=").append(vortex.planeId())
            .append(" pearlConsumed=").append(pearl.isRemoved()).append(" count=").append(vortex.count())
            .append("\n");
    if (!vortex.createdDimension()) {
        return sb + "NO_PLANE\n";
    }

    var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
    var plane = store.plane(vortex.planeId());
    sb.append("storePlane=").append(plane != null).append(" returnVortex=")
            .append(plane == null ? "null" : plane.returnVortex()).append(" returnDim=")
            .append(plane == null ? "null" : plane.returnDimension().location()).append(" radius=")
            .append(plane == null ? "null" : plane.radius()).append("\n");

    // The plane's exit rift must be a cheat doorway that knows its plane and how to send a visitor home.
    var exit = fr.wind_blade.isorropia.common.world.PocketPlane.exit(vortex.planeId());
    sb.append("exitIsVortex=").append(pocket.getBlockState(exit).is(
            fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get())).append("\n");
    if (pocket.getBlockEntity(exit) instanceof fr.wind_blade.isorropia.common.tiles.TileVortex exitVortex) {
        sb.append("exitCheat=").append(exitVortex.isCheat()).append(" exitCreated=")
                .append(exitVortex.createdDimension()).append(" exitPlane=").append(exitVortex.planeId())
                .append(" exitAspectCount=").append(exitVortex.aspectMap().size()).append("\n");
    } else {
        sb.append("exitTile=missing\n");
    }

    // A second pearl must be ignored: a rift seeds exactly one plane (createdDimension short-circuits the hand-off).
    int planeCountBefore = store.planeCount();
    var pearl2 = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(
                    dev.tc4port.thaumcraft.registry.TCItems.PRIMORDIAL_PEARL.get()));
    pearl2.setNoGravity(true);
    level.addFreshEntity(pearl2);
    for (int i = 0; i < 5; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vState, vortex);
    }
    sb.append("secondPearlIgnored=").append(store.planeCount() == planeCountBefore && !pearl2.isRemoved())
            .append("\n");

    // A traveller standing in the rift is carried into the plane...
    sb.append("beforeEnter pos=").append(player.blockPosition()).append(" dead=").append(player.isDeadOrDying())
            .append(" removed=").append(player.isRemoved()).append("\n");
    player.setPos(vPos.getX() + 0.5, vPos.getY() + 0.5, vPos.getZ() + 0.5);
    sb.append("teleportedTo=").append(player.blockPosition()).append("\n");
    player.setPortalCooldown(0);
    sb.append("inRift=").append(level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
            new net.minecraft.world.phys.AABB(vPos)).size()).append("\n");
    fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vState, vortex);
    sb.append("afterEnter dim=").append(player.level().dimension().location()).append(" pos=")
            .append(player.blockPosition()).append(" cooldown=").append(player.getPortalCooldown()).append("\n");

    // Leave the traveller standing in the plane's exit rift; the next real ticks carry them home (probe 231 asserts).
    player.setPos(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5);
    player.setPortalCooldown(0);
    sb.append("parkedAtExit=").append(player.blockPosition()).append(" dim=")
            .append(player.level().dimension().location()).append("\n");
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return sb + "\nEXC: " + sw.toString();
}

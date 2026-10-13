// TH's voidling hand-off, self-contained: the player is parked IN the rift's chunk (entities only register
// immediately in the player's own chunk), the rift is damped to six beams, and the ticker is driven by hand.
// Golem powder dropped in must be consumed and come out as permanent coreless VOID golems owned by the thrower.
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
    var vPos = new net.minecraft.core.BlockPos(600, 220, 612);
    for (int x = 594; x <= 606; x++) {
        for (int dz = 606; dz <= 618; dz++) {
            level.setBlock(new net.minecraft.core.BlockPos(x, 219, dz),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
    }
    player.setPos(600.5, 220.0, 610.5);
    player.setPortalCooldown(0);
    level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,
            new net.minecraft.world.level.ChunkPos(vPos), 3, vPos);
    level.removeBlock(vPos, false);
    level.setBlock(vPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VORTEX.get().defaultBlockState(), 3);
    var vortex = (fr.wind_blade.isorropia.common.tiles.TileVortex) level.getBlockEntity(vPos);
    if (vortex == null) {
        return sb + "no_vortex_tile\n";
    }
    var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
    aspects.put(dev.tc4port.thaumcraft.api.aspect.VisChannel.TERRA.aspectId(), 40);
    vortex.setAspects(aspects);
    vortex.setBeams(6);
    var vState = level.getBlockState(vPos);

    // Clear any stale golems from earlier runs so the count is honest.
    for (var g : level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityGolemTH.class,
            new net.minecraft.world.phys.AABB(vPos).inflate(16.0))) {
        g.discard();
    }
    var drop = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(
                    fr.wind_blade.isorropia.common.items.ItemsIS.GOLEM_POWDER.get(), 3));
    drop.setThrower(player.getUUID());
    drop.setNoGravity(true);
    level.addFreshEntity(drop);
    sb.append("dropVisible=").append(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(vPos).inflate(2.0)).isEmpty()).append("\n");

    for (int i = 0; i < 60; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vState, vortex);
    }
    sb.append("dropConsumed=").append(drop.isRemoved()).append("\n");
    var voidlings = level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityGolemTH.class,
            new net.minecraft.world.phys.AABB(vPos).inflate(6.0));
    boolean allVoid = !voidlings.isEmpty();
    boolean allOwned = !voidlings.isEmpty();
    boolean noStored = vortex.storedItems().isEmpty();
    int cored = 0;
    for (var g : voidlings) {
        allVoid &= g.getGolemType() == fr.wind_blade.isorropia.common.entities.AnimatedBlockType.VOID;
        allOwned &= g.getOwner() == player;
        cored += g.getCore() == -1 ? 0 : 1;
    }
    sb.append("voidlings=").append(voidlings.size()).append(" allVoid=").append(allVoid)
            .append(" allOwned=").append(allOwned).append(" cored=").append(cored)
            .append(" noStored=").append(noStored).append("\n");
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return sb + "\nEXC: " + sw.toString();
}

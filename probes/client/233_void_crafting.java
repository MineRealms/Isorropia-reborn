// Void crafting (TH's handleVoidCrafting): a fully damped rift (six beams) transforms matter dropped into it and
// keeps the result inside. A void ingot becomes void putty, salis mundus clumps into wisps, and an inert crystal
// wand comes back as a charged crystal wand. A wand right-click then pulls the oldest stored item back out.
//
// Entities only enter a level's entity list inside entity-ticking chunks, so the player is parked nearby (on a floor)
// to make the rift's chunk tick; the rift's own ticker is then driven directly here.
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
    for (int x = 592; x <= 608; x++) {
        for (int dz = -8; dz <= 8; dz++) {
            level.setBlock(new net.minecraft.core.BlockPos(x, 219, 612 + dz),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
    }
    player.setPos(600.5, 220.0, 596.5);
    player.setPortalCooldown(0);
    sb.append("parked=").append(player.blockPosition()).append("\n");
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
    aspects.put(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId(), 20);
    vortex.setAspects(aspects);
    vortex.setBeams(6);
    var vState = level.getBlockState(vPos);

    var salisMundus = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_mundus"));
    // Drop a void ingot (x3), a salis mundus, and an inert crystal wand into the damped rift.
    var ingot = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(
                    dev.tc4port.thaumcraft.registry.TCItems.VOID_INGOT.get(), 3));
    ingot.setNoGravity(true);
    level.addFreshEntity(ingot);
    var salt = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(salisMundus));
    salt.setNoGravity(true);
    level.addFreshEntity(salt);
    var wand = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(
                    fr.wind_blade.isorropia.common.items.ItemsIS.CRYSTAL_WAND.get()));
    wand.setNoGravity(true);
    level.addFreshEntity(wand);
    sb.append("dropped=").append(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(vPos).inflate(2.0)).size()).append("\n");

    for (int i = 0; i < 60; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vState, vortex);
    }
    sb.append("ingotConsumed=").append(ingot.isRemoved()).append(" saltConsumed=").append(salt.isRemoved())
            .append(" wandConsumed=").append(wand.isRemoved()).append("\n");

    var stored = vortex.storedItems();
    int putty = 0;
    boolean chargedWand = false;
    for (var stack : stored) {
        if (stack.is(fr.wind_blade.isorropia.common.items.ItemsIS.VOID_PUTTY.get())) {
            putty += stack.getCount();
        }
        if (stack.is(dev.tc4port.thaumcraft.registry.TCItems.WAND.get())) {
            var view = dev.tc4port.thaumcraft.api.wand.WandApi.view(stack);
            chargedWand = view.isPresent()
                    && fr.wind_blade.isorropia.common.wand.CrystalWandParts.ROD.equals(view.get().rod())
                    && fr.wind_blade.isorropia.common.wand.CrystalWandParts.CAP.equals(view.get().cap())
                    && view.get().visCentivis().values().stream().allMatch(v -> v == 25000);
        }
    }
    sb.append("storedCount=").append(stored.size()).append(" putty=").append(putty)
            .append(" chargedWand=").append(chargedWand).append("\n");

    long wisps = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
            new net.minecraft.world.phys.AABB(vPos).inflate(16.0)).stream()
            .filter(e -> e.getType() == dev.tc4port.thaumcraft.registry.TCEntities.WISP.get()).count();
    sb.append("wisps=").append(wisps).append("\n");

    // A wand right-click pulls the oldest stored item back out and empties that slot.
    int before = vortex.storedItems().size();
    vortex.popStored(player);
    int after = vortex.storedItems().size();
    sb.append("pop before=").append(before).append(" after=").append(after).append("\n");

    // Anything else dropped in (e.g. plain stone) is left alone.
    var stone = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
    stone.setNoGravity(true);
    level.addFreshEntity(stone);
    for (int i = 0; i < 5; i++) {
        fr.wind_blade.isorropia.common.tiles.TileVortex.serverTick(level, vPos, vState, vortex);
    }
    sb.append("stoneIgnored=").append(!stone.isRemoved()).append(" storedUnchanged=")
            .append(vortex.storedItems().size() == after).append("\n");
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return sb + "\nEXC: " + sw.toString();
}

// TH's working golems, exec 1: (A) golem animation powder sprinkled on a stone block raises it into a ROCK golem
// owned by the sprinkler, which takes TC4R cores and upgrades by right-click and settles back into its block on
// death; (B) sets up the damped rift and drops golem powder into it. Exec 2 (240) runs the rift's ticks and asserts
// the voidlings — the entity manager only registers the drop once a real world tick has promoted the chunk, so the
// two halves must be separate probe runs.
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
    sb.append("creative=").append(player.getAbilities().instabuild).append("\n");

    // ---- Scene A: powder on a stone block ----
    var blockPos = new net.minecraft.core.BlockPos(604, 220, 596);
    for (int x = 592; x <= 616; x++) {
        for (int dz = -10; dz <= 10; dz++) {
            level.setBlock(new net.minecraft.core.BlockPos(x, 219, 596 + dz),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
    }
    player.setPos(600.5, 220.0, 590.5);
    player.setPortalCooldown(0);
    level.removeBlock(blockPos, false);
    level.setBlock(blockPos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);

    var powderStack = new net.minecraft.world.item.ItemStack(
            fr.wind_blade.isorropia.common.items.ItemsIS.GOLEM_POWDER.get(), 4);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, powderStack);
    var hit = new net.minecraft.world.phys.BlockHitResult(
            new net.minecraft.world.phys.Vec3(604.5, 220.0, 596.5),
            net.minecraft.core.Direction.UP, blockPos, false);
    var ctx = new net.minecraft.world.item.context.UseOnContext(player,
            net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    var result = fr.wind_blade.isorropia.common.items.ItemsIS.GOLEM_POWDER.get().useOn(ctx);
    sb.append("powderResult=").append(result).append("\n");
    var golems = level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityGolemTH.class,
            new net.minecraft.world.phys.AABB(blockPos).inflate(8.0));
    sb.append("golemCount=").append(golems.size()).append(" powderLeft=").append(powderStack.getCount())
            .append(" blockGone=").append(level.getBlockState(blockPos).isAir()).append("\n");
    if (golems.isEmpty()) {
        return sb + "no_golem_spawned\n";
    }
    var golem = golems.get(0);
    sb.append("type=").append(golem.getGolemType())
            .append(" hp=").append(golem.getHealth()).append("/").append(golem.getMaxHealth())
            .append(" core=").append(golem.getCore())
            .append(" owner=").append(golem.getOwner() == player).append("\n");
    // Coreless ROCK golems last TH's 1200 ticks before settling back.
    for (int i = 0; i < 20; i++) {
        golem.tick();
    }
    sb.append("aliveAfter20=").append(golem.isAlive()).append("\n");

    // Right-click a TC4R core onto the golem: it becomes a settled guardian. The registry also holds a blank core
    // (coreId null), so pick one with a task identity.
    var coreItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
            .map(e -> e.getValue())
            .filter(i -> i instanceof dev.tc4port.thaumcraft.item.GolemCoreItem
                    && ((dev.tc4port.thaumcraft.item.GolemCoreItem) i).coreId() != null)
            .findFirst().orElse(null);
    var upgItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.entrySet().stream()
            .map(e -> e.getValue())
            .filter(i -> i instanceof dev.tc4port.thaumcraft.item.GolemUpgradeItem
                    && ((dev.tc4port.thaumcraft.item.GolemUpgradeItem) i).upgradeId() != null)
            .findFirst().orElse(null);
    sb.append("coreItem=").append(coreItem == null ? "none" : coreItem).append("\n");
    if (coreItem != null) {
        var coreStack = new net.minecraft.world.item.ItemStack(coreItem);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, coreStack);
        var coreResult = golem.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        sb.append("coreResult=").append(coreResult).append(" coreInstalled=").append(golem.getCore())
                .append(" coreLeft=").append(coreStack.getCount()).append("\n");
    }
    if (upgItem != null && golem.getCore() != -1) {
        var upgStack = new net.minecraft.world.item.ItemStack(upgItem, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, upgStack);
        var id = dev.tc4port.thaumcraft.common.GolemUpgradeId.BUILT_INS.indexOf(
                ((dev.tc4port.thaumcraft.item.GolemUpgradeItem) upgItem).upgradeId());
        var upgResult = golem.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        golem.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var third = golem.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        sb.append("upgId=").append(id).append(" upgResult=").append(upgResult)
                .append(" amount=").append(golem.getUpgradeAmount(id))
                .append(" thirdRefused=").append(third == net.minecraft.world.InteractionResult.PASS).append("\n");
    }

    // Death settles the golem back into the block it was raised from (pin it home first: it may have started
    // shadowing the player during the tick loop).
    golem.moveTo(604.5, 220.0, 596.5, 0.0F, 0.0F);
    golem.getNavigation().stop();
    golem.hurt(golem.damageSources().magic(), 1000.0F);
    sb.append("blockRestored=")
            .append(level.getBlockState(blockPos).is(net.minecraft.world.level.block.Blocks.STONE))
            .append(" dead=").append(golem.isDeadOrDying()).append("\n");

    // ---- Scene B setup: the damped rift + the powder drop. Assertion runs live in probe 240. ----
    var vPos = new net.minecraft.core.BlockPos(600, 220, 612);
    for (int x = 594; x <= 606; x++) {
        for (int dz = -6; dz <= 6; dz++) {
            level.setBlock(new net.minecraft.core.BlockPos(x, 219, 612 + dz),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
    }
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

    var drop = new net.minecraft.world.entity.item.ItemEntity(level, vPos.getX() + 0.5, vPos.getY() + 0.5,
            vPos.getZ() + 0.5, new net.minecraft.world.item.ItemStack(
                    fr.wind_blade.isorropia.common.items.ItemsIS.GOLEM_POWDER.get(), 3));
    drop.setThrower(player.getUUID());
    drop.setNoGravity(true);
    level.addFreshEntity(drop);
    sb.append("sceneBReady drop=").append(drop.getUUID()).append("\n");
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return sb + "\nEXC: " + sw.toString();
}

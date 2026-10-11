// Run after 48_beacon_kill: right-clicking the vat lets the reborn wizard out above it, keeping the new abilities.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var vat = net.minecraft.core.BlockPos.betweenClosedStream(player.blockPosition().offset(-1, -1, -1), player.blockPosition().offset(1, 3, 1))
        .map(level::getBlockEntity).filter(be -> be instanceof fr.wind_blade.isorropia.common.tiles.TileVat v && v.isMaster())
        .map(be -> (fr.wind_blade.isorropia.common.tiles.TileVat) be).findFirst().orElse(null);
if (vat == null) {
    return "no vat around " + player.blockPosition();
}
var pos = vat.getBlockPos();
var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
var result = level.getBlockState(pos).use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
var data = fr.wind_blade.isorropia.common.capabilities.LivingData.get(player).orElseThrow();
return "use=" + result + " stillHeld=" + (vat.getContained() == player) + " pos=" + player.blockPosition()
        + " vatTop=" + pos.getY() + " invulnerable=" + player.isInvulnerable()
        + " diamondSkin=" + data.hasSelfInfusion(fr.wind_blade.isorropia.common.infusion.SelfInfusions.DIAMOND_SKIN);

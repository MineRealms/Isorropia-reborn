var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var pos = player.blockPosition().offset(2, 0, 0);
level.setBlock(pos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VIS_DYNAMO.get().defaultBlockState(), 3);
player.closeContainer();
var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
var held = player.getItemInHand(hand);
player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
try {
    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
            net.minecraft.core.Direction.UP, pos, false);
    var result = player.gameMode.useItemOn(player, level, net.minecraft.world.item.ItemStack.EMPTY, hand, hit);
    if (!(player.containerMenu instanceof fr.wind_blade.isorropia.common.menu.VisDynamoMenu)) {
        throw new IllegalStateException("Vanilla empty-hand dispatch did not open menu: " + result);
    }
    return "DUAL_EMPTY_HAND_OK menu=" + player.containerMenu.getClass().getSimpleName();
} finally {
    player.setItemInHand(hand, held);
}

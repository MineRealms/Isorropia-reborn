var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
if (server == null) throw new IllegalStateException("Integrated server missing");
var registries = net.minecraft.core.registries.BuiltInRegistries.class;
var counts = new java.util.LinkedHashMap<String, Integer>();
for (String name : java.util.List.of("BLOCK", "ITEM", "BLOCK_ENTITY_TYPE", "ENTITY_TYPE")) {
    var registry = (net.minecraft.core.Registry<?>) registries.getField(name).get(null);
    int count = (int) registry.keySet().stream().filter(id -> id.getNamespace().equals("isorropia")).count();
    counts.put(name, count);
}
if (counts.get("BLOCK") != 22 || counts.get("ITEM") != 61
        || counts.get("BLOCK_ENTITY_TYPE") != 16 || counts.get("ENTITY_TYPE") != 19) {
    throw new IllegalStateException("Incomplete registration: " + counts);
}
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var pos = player.blockPosition().offset(2, 0, 0);
var block = fr.wind_blade.isorropia.common.blocks.BlocksIS.VIS_DYNAMO.get();
level.setBlock(pos, block.defaultBlockState(), 3);
if (!(level.getBlockEntity(pos) instanceof fr.wind_blade.isorropia.common.tiles.TileVisDynamo)) {
    throw new IllegalStateException("Vis dynamo block entity missing");
}
var held = player.getMainHandItem();
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
try {
    block.use(level.getBlockState(pos), level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                    net.minecraft.core.Direction.UP, pos, false));
} finally {
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
}
if (!(player.containerMenu instanceof fr.wind_blade.isorropia.common.menu.VisDynamoMenu)) {
    throw new IllegalStateException("Vis dynamo menu did not open");
}
return "DUAL_BOOT_OK " + counts + " menu=" + player.containerMenu.getClass().getSimpleName();

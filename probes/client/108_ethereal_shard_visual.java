// Five shard appearances, left to right: empty, aer, ignis, aqua, mixed. Also exercises the BEWLR item.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
player.stopUsingItem();
player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
player.getAbilities().flying = true;
player.onUpdateAbilities();
mc.execute(() -> { mc.setScreen(null); net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false); });
var aspects = new String[][] {{}, {"aer"}, {"ignis"}, {"aqua"}, {"ignis", "aqua", "ordo"}};
for (int i = 0; i < aspects.length; i++) {
    var pos = new net.minecraft.core.BlockPos(196 + i * 2, 311, 110);
    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
        level.setBlock(pos.offset(dx, -1, dz), net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
    }
    level.removeBlock(pos, false);
    level.setBlock(pos, fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get().defaultBlockState(), 3);
    var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(pos);
    for (String name : aspects[i]) {
        var aspect = dev.tc4port.thaumcraft.api.aspect.AspectId.parse(name);
        var essence = dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
                dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(aspect, 2));
        essence.setCount(8);
        shard.addEssence(essence);
        shard.addToStore(aspect, 24);
    }
}
player.getInventory().clearContent();
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
        new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.ETHEREAL_SHARD.get()));
player.connection.teleport(200.5, 312, 102, 0F, 15F);
level.getChunkSource().move(player);
level.setDayTime(6000);
return "visualShards=5";

// Illumination motes, part 1: five motes in five colours on a quartz shelf. Each must render nothing of its own and
// carry the tile that throws off TH's wisps; 123 counts the wisps after a few seconds.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var light = fr.wind_blade.isorropia.common.blocks.BlocksIS.LIGHT.get();
int[] colours = {1, 4, 11, 14, 15};
int invisible = 0, tiles = 0;
for (int i = 0; i < colours.length; i++) {
    var pos = new net.minecraft.core.BlockPos(60 + i * 2, 311, 60);
    level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK.defaultBlockState(), 3);
    level.setBlock(pos, light.defaultBlockState().setValue(fr.wind_blade.isorropia.common.blocks.BlockLight.COLOUR,
            colours[i]), 3);
    var state = level.getBlockState(pos);
    if (state.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE) invisible++;
    if (level.getBlockEntity(pos) instanceof fr.wind_blade.isorropia.common.tiles.TileLight) tiles++;
}
player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
player.getAbilities().flying = true;
player.onUpdateAbilities();
player.connection.teleport(64.5, 311.5, 55.5, 0.0F, 10.0F);
level.getChunkSource().move(player);
return "invisible=" + invisible + " tiles=" + tiles;

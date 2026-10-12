// Animation focus, part 7: a golem that falls should settle back into the block it was made of. Spawns a stone
// golem directly on a clear patch, kills it, and checks a stone block lands where it stood.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

double x = 100.5, y = 300.0, z = 100.5;
for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x) + dx, (int) y - 1, (int) Math.floor(z) + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 2; dy++) {
        level.setBlock(new net.minecraft.core.BlockPos((int) Math.floor(x) + dx, (int) y + dy, (int) Math.floor(z) + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
var golem = fr.wind_blade.isorropia.common.entities.EntitiesIS.ANIMATED_BLOCK.get().create(level);
golem.moveTo(x, y, z, 0.0F, 0.0F);
golem.loadGolem(net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 600, false, false, player);
level.addFreshEntity(golem);
var at = golem.blockPosition();
sb.append("spawned_at=").append(at).append(" hp=").append(golem.getHealth());
golem.kill();
sb.append(" at=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(at).getBlock()));
sb.append(" below=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(at.below()).getBlock()));
return sb.toString();

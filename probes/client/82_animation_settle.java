// Animation focus, part 6: a golem that falls should settle back into the block it was made of. Kills the stone
// golem raised in 80 and checks a stone block lands where it stood.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();
var box = new net.minecraft.world.phys.AABB(76, 306, 76, 85, 315, 86);
var golems = level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityAnimatedBlock.class, box);
sb.append("before=").append(golems.size());
for (var golem : golems) {
    var at = golem.blockPosition();
    sb.append(" hp_before=").append(golem.getHealth()).append("/").append(golem.getMaxHealth());
    golem.kill();
    sb.append(" alive=").append(golem.isAlive()).append(" removed=").append(golem.isRemoved());
    sb.append(" died_at=").append(at);
    sb.append(" at=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
            .getKey(level.getBlockState(at).getBlock()));
    sb.append(" below=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
            .getKey(level.getBlockState(at.below()).getBlock()));
}
return sb.toString();

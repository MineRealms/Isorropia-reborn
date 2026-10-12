// Animation focus, part 5: the stone golem raised in 80 should carry both the berserk and the detonation flags,
// and - because the extend upgrade was installed twice - outlast a plain golem. Releases the use key.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
var target = new net.minecraft.core.BlockPos(80, 311, 83);
sb.append("target_block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(target).getBlock()));
var box = new net.minecraft.world.phys.AABB(76, 306, 76, 85, 315, 86);
var golems = level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityAnimatedBlock.class, box);
sb.append(" golems=").append(golems.size());
for (var golem : golems) {
    sb.append(" [state=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
            .getKey(golem.getBlockState().getBlock()));
    sb.append(" type=").append(golem.getAnimatedType());
    sb.append(" berserk=").append(golem.isBerserk()).append(" detonation=").append(golem.isDetonation()).append("]");
}
var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
return sb.toString();

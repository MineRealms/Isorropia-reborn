// Animation focus, part 2: the gold block should be gone and a conjured golem standing in its place, made of gold
// (a METAL golem: 35 health, 15 armour), following the player who called it. Releases the use key.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
var target = new net.minecraft.core.BlockPos(60, 311, 63);
sb.append("target_block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(target).getBlock()));
var box = new net.minecraft.world.phys.AABB(56, 306, 56, 65, 315, 66);
var golems = level.getEntitiesOfClass(fr.wind_blade.isorropia.common.entities.EntityAnimatedBlock.class, box);
sb.append(" golems=").append(golems.size());
for (var golem : golems) {
    sb.append(" [state=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
            .getKey(golem.getBlockState().getBlock()));
    sb.append(" type=").append(golem.getAnimatedType());
    sb.append(" hp=").append(golem.getHealth()).append("/").append(golem.getMaxHealth());
    sb.append(" armor=").append(golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
    sb.append(" berserk=").append(golem.isBerserk()).append(" detonation=").append(golem.isDetonation());
    sb.append(" owner=").append(golem.getOwner() != null).append("]");
}
var use = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(use, false));
return sb.toString();

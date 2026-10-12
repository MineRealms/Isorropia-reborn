// Alchemite (S6), part 1: a stone shell around a charge. The alchemite block sits at the centre and a primed
// charge with a six-tick fuse is lit on top of it, so the blast also sets the block off through wasExploded.
// Check the wreckage with 98 a few seconds later.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var centre = new net.minecraft.core.BlockPos(100, 310, 170);
// A solid stone box, then a small hollow in the middle for the charge.
for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
    level.setBlock(centre.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
    level.setBlock(centre.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}
level.setBlock(centre, fr.wind_blade.isorropia.common.blocks.BlocksIS.ALCHEMITE.get().defaultBlockState(), 3);
sb.append("block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(centre).getBlock()));
sb.append(" stone=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getKey(level.getBlockState(centre.offset(0, -1, 0)).getBlock()));

// A ring of ordinary nodes in the blast. Every outcome scars them — shattered, fading or unstable — so 98 can
// check that none is still a plain node afterwards.
int nodes = 0;
for (int dx = -2; dx <= 2; dx += 2) for (int dz = -2; dz <= 2; dz += 2) {
    if (dx == 0 && dz == 0) {
        continue;
    }
    level.setBlockAndUpdate(centre.offset(dx, 0, dz),
            dev.tc4port.thaumcraft.api.ThaumcraftContent.block("aura_node").defaultBlockState());
    nodes++;
}
sb.append(" nodes=").append(nodes);

// Prime it the way fire would, but with a short fuse so the sweep does not wait four seconds for it.
var primed = new fr.wind_blade.isorropia.common.entities.EntityAlchemitePrimed(level,
        centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, player);
primed.setFuse(6);
level.addFreshEntity(primed);
sb.append(" entity=true fuse=").append(primed.getFuse());

// Stand well clear of the five-block blast, looking down at it for a screenshot.
player.teleportTo(level, centre.getX() + 0.5 + 11.0, centre.getY() + 5.0, centre.getZ() + 0.5 + 11.0, 135.0F, 30.0F);
return sb.toString();

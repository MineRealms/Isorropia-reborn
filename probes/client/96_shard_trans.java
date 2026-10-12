// Shard Transmutation (S6): a crucible charged with one primal aspect and a unit of Permutatio turns a balanced
// shard into that aspect's shard. Six crucibles, one per aspect, each over a lava source. The recipe is gated on
// isorropia:shardtrans, so the research is granted first. Heat is pumped directly (the crucible's own server tick)
// so the result does not depend on wall-clock ticking while the probe is away.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
var granted = dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:shardtrans"), context);
// complete() answers ALREADY_COMPLETE on a re-run, so report the resulting knowledge instead of the call result.
sb.append("research=").append(granted)
  .append(" known=").append(dev.tc4port.thaumcraft.api.research.ResearchApi.isComplete(player,
          dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:shardtrans")));

String[] primals = {"ignis", "aqua", "aer", "terra", "ordo", "perditio"};
String[] shards = {"fire_shard", "water_shard", "air_shard", "earth_shard", "order_shard", "entropy_shard"};
double x0 = 100.5, y = 310.0, z = 140.5;
for (int dx = -3; dx <= 18; dx++) for (int dz = -3; dz <= 3; dz++) {
    level.setBlock(new net.minecraft.core.BlockPos((int) x0 + dx, (int) y - 2, (int) z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = -1; dy <= 3; dy++) {
        level.setBlock(new net.minecraft.core.BlockPos((int) x0 + dx, (int) y + dy, (int) z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
int made = 0;
for (int i = 0; i < 6; i++) {
    var pos = new net.minecraft.core.BlockPos((int) x0 + i * 3, (int) y, (int) z);
    // A previous run leaves its shards lying on the pad; clear them so the readback is unambiguous.
    for (var stale : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                    pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2))) {
        stale.discard();
    }
    level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
    level.setBlockAndUpdate(pos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("crucible").defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof dev.tc4port.thaumcraft.block.entity.CrucibleBlockEntity crucible)) {
        sb.append(" noBE=").append(shards[i]);
        continue;
    }
    crucible.fillWater(1000);
    var aspects = new java.util.LinkedHashMap<dev.tc4port.thaumcraft.api.aspect.AspectId, Integer>();
    aspects.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:" + primals[i]), 2);
    aspects.put(dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:permutatio"), 1);
    crucible.addAspects(aspects);
    // Bring the crucible to a boil without waiting on the ticker; 151 ticks of heat clears BOILING_HEAT.
    for (int t = 0; t < 151; t++) {
        dev.tc4port.thaumcraft.block.entity.CrucibleBlockEntity.serverTick(level, pos, level.getBlockState(pos), crucible);
    }
    var shard = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, y + 0.4, pos.getZ() + 0.5,
            new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("balanced_shard")));
    shard.setThrower(player.getUUID());
    level.addFreshEntity(shard);
    crucible.attemptSmelt(shard);
    var found = new java.util.TreeSet<String>();
    for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                    pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2))) {
        found.add(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem()).getPath());
    }
    var ok = found.contains(shards[i]);
    if (ok) made++;
    sb.append(i == 0 ? " " : " ").append(shards[i]).append("=").append(ok);
    if (!ok) sb.append(found);
    sb.append(" boiling=").append(crucible.boiling());
}
sb.append(" made=").append(made).append("/6");

// Stand back on the shelf so the pad is in view for a screenshot.
player.teleportTo(level, x0 + 7.5, y + 5.0, z + 9.5, 180.0F, 35.0F);
return sb.toString();

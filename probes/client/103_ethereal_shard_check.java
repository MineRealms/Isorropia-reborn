// Ethereal Shard, part 3: reads the shard back after the hold, and proves three things — the wand gained vis, the
// shard lost the vis it held, and a broken shard hands its bound essence back four capacity at a time.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var shardPos = net.minecraft.core.BlockPos.containing(200.5, 311.0, 103.5);
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var wandAer = dev.tc4port.thaumcraft.api.wand.WandApi.view(player
        .getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND))
        .map(v -> v.visCentivis().getOrDefault(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 0)).orElse(-1);
sb.append("wandGained=").append(wandAer > 0);
sb.append(" wandAer=").append(wandAer);
sb.append(" using=").append(player.isUsingItem());

if (!(level.getBlockEntity(shardPos) instanceof fr.wind_blade.isorropia.common.tiles.TileEtherealShard shard)) {
    return sb.append(" shard=missing").toString();
}
sb.append(" stored=").append(shard.stored(aer));
sb.append(" shardSpent=").append(shard.stored(aer) < 5);
sb.append(" capacity=").append(shard.capacity(aer));
sb.append(" draining=").append(shard.draining(level));

// Three essence of aer went in, so a broken shard returns three.
var drops = shard.essenceDrops();
sb.append(" drops=").append(drops.size());
sb.append(" dropCount=").append(drops.isEmpty() ? 0 : drops.get(0).getCount());
sb.append("dropAspect=").append(drops.isEmpty() ? "-"
        : dev.tc4port.thaumcraft.api.aspect.AspectApi.getItemAspects(drops.get(0)).amounts().keySet()
                .iterator().next().value().getPath());

// Let go of the wand so the shard is left at rest for whatever runs next.
var useKey = mc.options.keyUse.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(useKey, false));
return sb.toString();

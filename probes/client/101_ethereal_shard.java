// Ethereal Shard, part 1: grants the research, checks the block/item/recipe loaded, builds a pad, places a shard
// with a vis relay and an energized node feeding it, and feeds the shard a stack of ethereal essence. The player
// is then handed an empty wand and turned to face the shard; the wand click itself happens in 102, once the
// client has taken the teleport.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
var granted = dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:etherealshard"), context);
// A re-run of the sweep finds the research already known, which is just as good as granting it here.
sb.append("known=").append(granted == dev.tc4port.thaumcraft.api.research.ResearchCompletionResult.COMPLETED
        || granted == dev.tc4port.thaumcraft.api.research.ResearchCompletionResult.ALREADY_COMPLETE);
sb.append(" block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "ethereal_shard")));
sb.append(" item=").append(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "ethereal_shard")));
sb.append(" recipe=").append(server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/ethereal_shard"))
        .isPresent());

server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
// An earlier run may have left the use key held, which would keep tapping the shard right through the setup and
// outrun the network's refill. Let go of it before anything is measured.
var useKey = mc.options.keyUse.getKey();
var released = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.setScreen(null);
    net.minecraft.client.KeyMapping.set(useKey, false);
    mc.player.stopUsingItem();
    released.complete(true);
});
released.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
player.closeContainer();
player.getAbilities().flying = false;
player.onUpdateAbilities();
player.getInventory().clearContent();
player.getInventory().selected = 0;
player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));

double x = 200.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dz = 0; dz <= 6; dz++) for (int dy = 0; dy <= 3; dy++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x, y + dy, z + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}

var shardPos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 3);
var relayPos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 4);
var nodePos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 5);
level.setBlock(net.minecraft.core.BlockPos.containing(x, y, z + 3),
        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
level.setBlock(shardPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get().defaultBlockState(), 3);
level.setBlock(relayPos, dev.tc4port.thaumcraft.registry.TCBlocks.VIS_RELAY.get().defaultBlockState(), 3);
level.setBlock(nodePos, dev.tc4port.thaumcraft.registry.TCBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState(), 3);
if (level.getBlockEntity(nodePos) instanceof dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity node) {
    node.setNodeState(new dev.tc4port.thaumcraft.api.node.AuraNodeState(
            java.util.Optional.empty(),
            dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL,
            dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE,
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of(
                    dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId(), 128)),
            new dev.tc4port.thaumcraft.api.node.NodeVis(java.util.Map.of())));
}
sb.append(" shard=").append(shardPos).append(" relay=").append(relayPos).append(" node=").append(nodePos);

var be = level.getBlockEntity(shardPos);
sb.append(" be=").append(be == null ? "null" : be.getClass().getSimpleName());
if (!(be instanceof fr.wind_blade.isorropia.common.tiles.TileEtherealShard shard)) {
    return sb.toString();
}

// Three ethereal essence of aer, right-clicked through the block so the routing is covered too.
var essence = dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi.createWispEssence(
        dev.tc4port.thaumcraft.api.aspect.AspectAmounts.of(
                dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId(), 2));
essence.setCount(3);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, essence);
var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(shardPos),
        net.minecraft.core.Direction.UP, shardPos, false);
var used = fr.wind_blade.isorropia.common.blocks.BlocksIS.ETHEREAL_SHARD.get()
        .use(level.getBlockState(shardPos), level, shardPos, player,
                net.minecraft.world.InteractionHand.MAIN_HAND, hit);
sb.append(" use=").append(used);
sb.append(" capacity=").append(shard.capacity(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId()));
sb.append(" essenceLeft=").append(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount());

// A little aer banked already, so the draw in 102 has something to take.
shard.addToStore(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId(), 5);
sb.append(" stored=").append(shard.stored(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId()));

var wand = dev.tc4port.thaumcraft.api.wand.WandApi.create(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD,
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
sb.append(" wandAer=").append(dev.tc4port.thaumcraft.api.wand.WandApi.view(wand)
        .map(v -> v.visCentivis().getOrDefault(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER, 0)).orElse(-1));

player.teleportTo(level, x, y, z, 0.0F, 0.0F);
return sb.toString();

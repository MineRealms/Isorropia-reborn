// Vis Dynamo, part 1: grants the research, checks the item/block/recipe loaded, builds a pad, and puts a vis relay
// beside a vis dynamo at eye height three blocks ahead. The player gets a fully charged wand and is turned to face
// the dynamo; the click itself happens in 85 once the client has taken the teleport.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();

var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);
sb.append("research=").append(dev.tc4port.thaumcraft.api.research.ResearchApi.complete(player,
        dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:visdynamo"), context));
sb.append(" block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "vis_dynamo")));
sb.append(" item=").append(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "vis_dynamo")));
sb.append(" recipe=").append(server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/vis_dynamo")).isPresent());

server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
player.getInventory().clearContent();
var wand = dev.tc4port.thaumcraft.api.wand.WandApi.createFullyCharged(
        dev.tc4port.thaumcraft.api.wand.WandMaterialId.WOOD, dev.tc4port.thaumcraft.api.wand.WandMaterialId.IRON);
player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, wand);
sb.append(" wand=").append(dev.tc4port.thaumcraft.api.wand.WandApi.view(wand)
        .map(v -> String.valueOf(v.visCentivis().get(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER))).orElse("-"));

double x = 100.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
}
for (int dz = 0; dz <= 6; dz++) for (int dy = 0; dy <= 3; dy++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x, y + dy, z + dz),
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
}

var dynamoPos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 3);
var relayPos = net.minecraft.core.BlockPos.containing(x, y + 1, z + 4);
level.setBlock(net.minecraft.core.BlockPos.containing(x, y, z + 3),
        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
level.setBlock(dynamoPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.VIS_DYNAMO.get().defaultBlockState(), 3);
level.setBlock(relayPos, dev.tc4port.thaumcraft.registry.TCBlocks.VIS_RELAY.get().defaultBlockState(), 3);
sb.append(" dynamo=").append(dynamoPos).append(" relay=").append(relayPos);
sb.append(" be=").append(level.getBlockEntity(dynamoPos) instanceof fr.wind_blade.isorropia.common.tiles.TileVisDynamo);

player.teleportTo(level, x, y, z, 0.0F, 0.0F);
return sb.toString();

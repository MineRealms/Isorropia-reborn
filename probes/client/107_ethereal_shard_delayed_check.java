var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(
        new net.minecraft.core.BlockPos(200, 311, 103));
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER;
int gained = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow()
        .visCentivis().getOrDefault(aer, 0);
mc.execute(() -> net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false));
player.stopUsingItem();
return "delayedGained=" + gained + " remaining=" + shard.stored(aer.aspectId());

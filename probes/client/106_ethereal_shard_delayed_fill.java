// The first tap had nothing to take. It must still track the held wand without a new click.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
var pos = new net.minecraft.core.BlockPos(200, 311, 103);
var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) level.getBlockEntity(pos);
boolean waiting = shard.resolveDrainer(level) != null;
shard.addToStore(dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId(), 3);
return "waiting=" + waiting + " delayedFill=3";

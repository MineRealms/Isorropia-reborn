// Held wand vs. focus, part 3: held against the dynamo, the wand paid the dynamo's 40 centivis per pulse on every
// primal alike and nothing more for the fire stream. The wizard then looks away with the key still down; 120 checks
// that the machine let go of the held use, as TH's dynamo stopped it.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var dynamoPos = new net.minecraft.core.BlockPos(140, 311, 103);
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS;
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER;
var start = String.valueOf(System.getProperties().get("isorropia.focusHold")).split(",");
var vis = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow().visCentivis();
int ignisSpent = Integer.parseInt(start[1]) - vis.get(ignis);
int aerSpent = Integer.parseInt(start[0]) - vis.get(aer);
var dynamo = (fr.wind_blade.isorropia.common.tiles.TileVisDynamo) level.getBlockEntity(dynamoPos);
var sb = new StringBuilder("using=").append(player.isUsingItem())
        .append(" holding=").append(fr.wind_blade.isorropia.common.compat.HeldWandTargets.isHolding(player, dynamoPos))
        .append(" charging=").append(dynamo.canSupplyVis())
        .append(" focusSuppressed=").append(aerSpent > 0 && aerSpent == ignisSpent && aerSpent % 40 == 0)
        .append(" machineIgnis=").append(ignisSpent).append(" machineAer=").append(aerSpent);
mc.execute(() -> {
    mc.player.setYRot(90.0F);
    mc.player.setXRot(0.0F);
});
return sb.toString();

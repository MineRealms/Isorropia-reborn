// Held wand vs. focus, part 2: read the control (the fire stream spent ignis and nothing else), let go, then hold the
// same wand against the vis dynamo from 114. Checked in 119.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var ignis = dev.tc4port.thaumcraft.api.aspect.VisChannel.IGNIS;
var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER;
var start = String.valueOf(System.getProperties().get("isorropia.focusHold")).split(",");
var vis = dev.tc4port.thaumcraft.api.wand.WandApi.view(player.getMainHandItem()).orElseThrow().visCentivis();
int ignisSpent = Integer.parseInt(start[1]) - vis.get(ignis);
int aerSpent = Integer.parseInt(start[0]) - vis.get(aer);
var sb = new StringBuilder("controlFires=").append(ignisSpent > 0 && aerSpent == 0)
        .append(" controlIgnis=").append(ignisSpent).append(" controlAer=").append(aerSpent);

var released = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
    mc.player.stopUsingItem();
    released.complete(true);
});
released.get(10, java.util.concurrent.TimeUnit.SECONDS);
player.stopUsingItem();
System.getProperties().put("isorropia.focusHold", vis.get(aer) + "," + vis.get(ignis));
Thread.sleep(200);
var clientWand = player.getMainHandItem().copy();
var pressed = new java.util.concurrent.CompletableFuture<Boolean>();
mc.execute(() -> {
    mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, clientWand);
    mc.player.setYRot(0.0F);
    mc.player.setXRot(0.0F);
    var key = mc.options.keyUse.getKey();
    net.minecraft.client.KeyMapping.set(key, true);
    net.minecraft.client.KeyMapping.click(key);
    pressed.complete(true);
});
pressed.get(10, java.util.concurrent.TimeUnit.SECONDS);
return sb.toString();

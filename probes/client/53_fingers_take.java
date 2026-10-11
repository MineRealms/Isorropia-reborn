// Shift-takes the result from the fingers' workbench and reports what was spent.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
if (!(player.containerMenu instanceof fr.wind_blade.isorropia.common.menu.MorphicFingersMenu fingers)) {
    return "no fingers menu";
}
var before = fingers.getSlot(0).getItem();
var taken = fingers.quickMoveStack(player, 0);
var after = fingers.getSlot(0).getItem();
var sb = new StringBuilder();
sb.append("result before=").append(before).append(" taken=").append(taken).append(" after=").append(after);
for (int i = 0; i < 9; i++) {
    sb.append(System.lineSeparator()).append("grid[").append(i).append("]=").append(fingers.getSlot(1 + i).getItem());
}
for (int i = 0; i < 6; i++) {
    sb.append(System.lineSeparator()).append("crystal[").append(i).append("]=").append(fingers.getSlot(10 + i).getItem());
}
return sb.toString();

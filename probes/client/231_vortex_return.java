// Stage 4 (assert, part 2): the traveller left standing in a pocket plane's exit rift (probe 230) must have been
// carried home by that rift's own ticker -- the exit rift is a cheat doorway born with createdDimension, so it runs
// TH's transferTravellers.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
sb.append("dim=").append(player.level().dimension().location()).append(" pos=").append(player.blockPosition())
        .append(" cooldown=").append(player.getPortalCooldown()).append("\n");
return sb.toString();

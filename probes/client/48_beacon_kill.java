// Deals the bound player a killing blow and reports whether the beacon revived them instead. Expected: alive,
// unbound, holding the body's diamond skin but no longer the old body's fingers, and held in the vat.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var data = fr.wind_blade.isorropia.common.capabilities.LivingData.get(player).orElse(null);
var sb = new StringBuilder();

sb.append("before: hp=").append(player.getHealth())
  .append(" pos=").append(player.blockPosition())
  .append(" bound=").append(player.getPersistentData().getBoolean("soulBeacon"))
  .append(" diamondSkin=").append(data != null && data.hasSelfInfusion(
          fr.wind_blade.isorropia.common.infusion.SelfInfusions.DIAMOND_SKIN));

player.hurt(level.damageSources().genericKill(), 1000.0F);

sb.append(System.lineSeparator()).append("after: alive=").append(player.isAlive())
  .append(" hp=").append(player.getHealth())
  .append(" pos=").append(player.blockPosition())
  .append(" bound=").append(player.getPersistentData().getBoolean("soulBeacon"))
  .append(" diamondSkin=").append(data != null && data.hasSelfInfusion(
          fr.wind_blade.isorropia.common.infusion.SelfInfusions.DIAMOND_SKIN))
  .append(" fingers=").append(data != null && data.hasSelfInfusion(
          fr.wind_blade.isorropia.common.infusion.SelfInfusions.MORPHIC_FINGERS))
  .append(" inventoryEmpty=").append(player.getInventory().isEmpty());
// Whichever vat now holds the player.
var near = net.minecraft.core.BlockPos.betweenClosedStream(player.blockPosition().offset(-1, -1, -1), player.blockPosition().offset(1, 3, 1))
        .map(level::getBlockEntity).filter(be -> be instanceof fr.wind_blade.isorropia.common.tiles.TileVat v && v.isMaster())
        .map(be -> (fr.wind_blade.isorropia.common.tiles.TileVat) be).findFirst().orElse(null);
sb.append(" vatHoldsPlayer=").append(near != null && near.getContained() == player)
  .append(" vatMode=").append(near == null ? -1 : near.getMode())
  .append(" vatInfusions=").append(near == null ? "-" : near.getSelfInfusions());
return sb.toString();

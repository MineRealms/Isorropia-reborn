// Seroconverter, part 1: a warded jar of 8 victus with a blood infuser on top, on a pad east of the
// essentia dynamo's. Dial two units of victus, single-shot mode, and drive the tick by hand; check
// the injection's NBT, the drained jar, and the idle return.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
double x = 128.5, y = 310.0, z = 100.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 3; dy++) {
        level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y + dy, z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
// Park the player in the machine's chunk so it ticks and exists.
player.teleportTo(level, x - 0.5, y + 1.0, z + 1.5, 0.0F, 0.0F);
var jarPos = net.minecraft.core.BlockPos.containing(x, y, z);
var infuserPos = jarPos.above();
level.setBlockAndUpdate(jarPos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("warded_jar").defaultBlockState());
var victus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:victus");
long filled = 0;
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar) {
    filled = jar.addEssentia(victus, 8, net.minecraft.core.Direction.UP,
            dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
}
level.setBlockAndUpdate(infuserPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.BLOOD_INFUSER.get().defaultBlockState());
sb.append("filled=").append(filled).append(" tile=").append(
        level.getBlockEntity(infuserPos) instanceof fr.wind_blade.isorropia.common.tiles.TileBloodInfuser);
var infuser = (fr.wind_blade.isorropia.common.tiles.TileBloodInfuser) level.getBlockEntity(infuserPos);
// Load a blood syringe and dial two units of victus in single-shot mode.
infuser.container().setItem(0, new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get()));
infuser.addSelected(victus);
infuser.addSelected(victus);
infuser.setMode(1);
sb.append(" dialled=").append(infuser.dialled().size());
// One tick finds the target and shows the pull; the draw is one unit per tick.
var state = level.getBlockState(infuserPos);
fr.wind_blade.isorropia.common.tiles.TileBloodInfuser.serverTick(level, infuserPos, state, infuser);
int suctionDown = infuser.suctionAmount(net.minecraft.core.Direction.DOWN);
sb.append(" sucking=").append(infuser.currentlySucking() != null ? infuser.currentlySucking().serialized() : "null")
  .append(" suctionDown=").append(suctionDown);
for (int i = 0; i < 12; i++) {
    fr.wind_blade.isorropia.common.tiles.TileBloodInfuser.serverTick(level, infuserPos, state, infuser);
}
sb.append(" drained=").append(8 - filled + filled);
int jarLeft = 0;
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar2) {
    jarLeft = jar2.essentiaAmount(net.minecraft.core.Direction.UP);
}
sb.append(" jarLeft=").append(jarLeft)
  .append(" syringeLeft=").append(infuser.syringe().getCount())
  .append(" modeAfter=").append(infuser.getMode());
var out = infuser.output(0);
sb.append(" out=").append(out.getItem())
  .append(" count=").append(out.getCount());
var effects = net.minecraft.world.item.alchemy.PotionUtils.getAllEffects(out.getTag());
for (var effect : effects) {
    sb.append(" effect=").append(effect.getEffect().getDescriptionId())
      .append(" amp=").append(effect.getAmplifier())
      .append(" dur=").append(effect.getDuration());
}
sb.append(" colour=").append(String.format("%06x", out.getTag() != null && out.getTag().contains("color")
        ? out.getTag().getInt("color") : -1));
sb.append(" out2=").append(infuser.output(1).isEmpty()).append(" out9=").append(infuser.output(8).isEmpty());
sb.append(" dialAfter=").append(infuser.dialled().size());
return sb.toString();

// Seroconverter, part 3 (clean): runtime recipe, TH's button protocol driven server-side through a
// real menu, automatic mode across two syringes, the full-output-rack gate, and the addEssentia push
// path. Fresh pad, self-contained.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
double x = 128.5, y = 310.0, z = 124.5;
for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 7; dz++) {
    level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y - 1, z + dz),
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
    for (int dy = 0; dy <= 3; dy++) {
        level.setBlock(net.minecraft.core.BlockPos.containing(x + dx, y + dy, z + dz),
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }
}
player.teleportTo(level, x - 0.5, y + 1.0, z + 1.5, 0.0F, 0.0F);
var jarPos = net.minecraft.core.BlockPos.containing(x, y, z);
var infuserPos = jarPos.above();
level.setBlockAndUpdate(jarPos, dev.tc4port.thaumcraft.api.ThaumcraftContent.block("warded_jar").defaultBlockState());
level.setBlock(infuserPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
level.setBlockAndUpdate(infuserPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.BLOOD_INFUSER.get().defaultBlockState());
var infuser = (fr.wind_blade.isorropia.common.tiles.TileBloodInfuser) level.getBlockEntity(infuserPos);
// --- runtime recipe ---
sb.append("recipe=").append(server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/blood_infuser")).isPresent());
// --- TH's button protocol through a real server menu ---
var known = fr.wind_blade.isorropia.common.menu.BloodInfuserMenu.knownSorted(player);
var first = known.get(0);
var second = known.get(1);
sb.append(" known=").append(known.size())
  .append(" a=").append(first.serialized()).append(" b=").append(second.serialized());
if (level.getBlockEntity(jarPos) instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport jar) {
    jar.addEssentia(second, 8, net.minecraft.core.Direction.UP, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
}
var menu = new fr.wind_blade.isorropia.common.menu.BloodInfuserMenu(77, player.getInventory(), infuser);
int i0 = known.indexOf(first);
int i1 = known.indexOf(second);
menu.clickMenuButton(player, -i1 - 1);             // add one second
menu.clickMenuButton(player, -i1 - 1);             // add a second second
menu.clickMenuButton(player, -i0 - 1);             // add one first
sb.append(" dial=").append(infuser.dialled());
menu.clickMenuButton(player, 3 + i0);              // remove the first again
sb.append(" afterRemove=").append(infuser.dialled());
menu.clickMenuButton(player, 2);                   // automatic mode
sb.append(" mode=").append(infuser.getMode());
// --- automatic mode: two syringes, two injections, mode stays 2 ---
var inj = fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_INJECTION.get();
infuser.container().setItem(0, new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get()));
for (int i = 0; i < 12; i++) {
    fr.wind_blade.isorropia.common.tiles.TileBloodInfuser.serverTick(level, infuserPos, level.getBlockState(infuserPos), infuser);
}
boolean auto1 = infuser.output(0).getItem() == inj && infuser.syringe().isEmpty();
infuser.container().setItem(0, new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get()));
for (int i = 0; i < 12; i++) {
    fr.wind_blade.isorropia.common.tiles.TileBloodInfuser.serverTick(level, infuserPos, level.getBlockState(infuserPos), infuser);
}
int jarLeft = level.getBlockEntity(jarPos)
        instanceof dev.tc4port.thaumcraft.api.essentia.EssentiaTransport j2 ? j2.essentiaAmount(net.minecraft.core.Direction.UP) : -1;
sb.append(" auto1=").append(auto1)
  .append(" auto2=").append(infuser.output(1).getItem() == inj)
  .append(" modeStill=").append(infuser.getMode())
  .append(" jarLeft=").append(jarLeft);
// --- full output rack gates production: no suck, syringe stays ---
infuser.container().setItem(0, new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get()));
var junk = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64);
for (int i = 1; i <= 9; i++) {
    infuser.container().setItem(i, junk.copy());
}
for (int i = 0; i < 6; i++) {
    fr.wind_blade.isorropia.common.tiles.TileBloodInfuser.serverTick(level, infuserPos, level.getBlockState(infuserPos), infuser);
}
sb.append(" fullRackSyringeKept=").append(!infuser.syringe().isEmpty())
  .append(" fullRackSucked=").append(infuser.currentlySucking() != null)
  .append(" fullRackAcqEmpty=").append(infuser.acquiredAmount(second) == 0);
// --- addEssentia push path: pump an aspect straight into a second machine, it lands in acquired ---
var infuser2Pos = infuserPos.east();
level.setBlockAndUpdate(infuser2Pos, fr.wind_blade.isorropia.common.blocks.BlocksIS.BLOOD_INFUSER.get().defaultBlockState());
var infuser2 = (fr.wind_blade.isorropia.common.tiles.TileBloodInfuser) level.getBlockEntity(infuser2Pos);
infuser2.container().setItem(0, new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get()));
infuser2.addSelected(first);
fr.wind_blade.isorropia.common.tiles.TileBloodInfuser.serverTick(level, infuser2Pos,
        level.getBlockState(infuser2Pos), infuser2);
int pushed = infuser2.addEssentia(first, 3, net.minecraft.core.Direction.DOWN,
        dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
sb.append(" pushAccepted=").append(pushed > 0)
  .append(" acquired=").append(infuser2.acquiredAmount(first));
return sb.toString();

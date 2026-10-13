// Seroconverter, part 4 (fixed): menu shift-click moves, the NBT round-trip, and breaking the block
// drops its contents. Uses a fresh pad; menu indices 37..45 are the player's hotbar (inv 0..8).
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
double x = 128.5, y = 310.0, z = 148.5;
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
level.setBlockAndUpdate(infuserPos, fr.wind_blade.isorropia.common.blocks.BlocksIS.BLOOD_INFUSER.get().defaultBlockState());
var infuser = (fr.wind_blade.isorropia.common.tiles.TileBloodInfuser) level.getBlockEntity(infuserPos);
var aqua = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:aqua");
var syringeItem = fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get();
var injItem = fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_INJECTION.get();
player.getInventory().clearContent();
// Seed: an injection on the rack; machine slot 0 starts EMPTY (so a shift-click can fill it).
infuser.container().setItem(1, new net.minecraft.world.item.ItemStack(injItem));
infuser.addSelected(aqua);
infuser.addSelected(aqua);
infuser.addEssentia(aqua, 1, net.minecraft.core.Direction.DOWN, dev.tc4port.thaumcraft.api.essentia.EssentiaTransferMode.EXECUTE);
infuser.setMode(2);
// --- quickMoveStack: rack output (menu slot 1) out to the player ---
var menu = new fr.wind_blade.isorropia.common.menu.BloodInfuserMenu(79, player.getInventory(), infuser);
var moved1 = menu.quickMoveStack(player, 1);
sb.append(" move1=").append(moved1.getItem() == injItem)
  .append(" rackEmpty=").append(infuser.container().getItem(1).isEmpty())
  .append(" playerGot=").append(player.getInventory().getItem(8).getItem() == injItem);
// --- a syringe in the hotbar (inv 0 = menu 37) shifts into machine slot 0 ---
infuser.container().setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(syringeItem));
var moved2 = menu.quickMoveStack(player, 37);
sb.append(" move2=").append(moved2.getItem() == syringeItem)
  .append(" slot0=").append(infuser.container().getItem(0).getItem() == syringeItem)
  .append(" inv0Empty=").append(player.getInventory().getItem(0).isEmpty());
// --- junk refuses to shift into the machine ---
infuser.container().setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8));
var moved3 = menu.quickMoveStack(player, 37);
sb.append(" junkStayed=").append(moved3.isEmpty() && infuser.container().getItem(0).isEmpty());
// --- NBT round-trip: a fresh BE fed this tile's saved tag must keep everything ---
infuser.container().setItem(0, new net.minecraft.world.item.ItemStack(syringeItem));
infuser.container().setItem(2, new net.minecraft.world.item.ItemStack(injItem));
var saved = infuser.saveWithFullMetadata();
var fresh = new fr.wind_blade.isorropia.common.tiles.TileBloodInfuser(infuserPos, level.getBlockState(infuserPos));
fresh.load(saved);
sb.append(" nbtMode=").append(fresh.getMode() == 2)
  .append(" nbtDial=").append(fresh.dialled().size() == 1 && fresh.dialled().containsKey(aqua))
  .append(" nbtAcq=").append(fresh.acquiredAmount(aqua) == 1)
  .append(" nbtSyringe=").append(fresh.container().getItem(0).getItem() == syringeItem)
  .append(" nbtInj=").append(fresh.container().getItem(2).getItem() == injItem);
// --- breaking the block drops its contents ---
level.setBlock(infuserPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
int drops = 0;
boolean sawSyringe = false;
boolean sawInj = false;
for (var itemEntity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
        new net.minecraft.world.phys.AABB(infuserPos.offset(-2, -1, -2), infuserPos.offset(2, 3, 2)))) {
    var stack = itemEntity.getItem();
    drops++;
    sawSyringe |= stack.getItem() == syringeItem;
    sawInj |= stack.getItem() == injItem;
}
sb.append(" onRemoveGone=").append(level.getBlockEntity(infuserPos) == null)
  .append(" drops=").append(drops)
  .append(" sawSyringe=").append(sawSyringe)
  .append(" sawInj=").append(sawInj);
return sb.toString();

// Fits the scholar's lens and puts a diamond in the block space the crosshair is about to land above, so the
// readout should name the item and show its aspects instead of the block underneath.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var goggles = new net.minecraft.world.item.ItemStack(dev.tc4port.thaumcraft.api.ThaumcraftContent.item("goggles_of_revealing"));
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.LEFT,
        fr.wind_blade.isorropia.common.lenses.Lenses.ORDO);
fr.wind_blade.isorropia.common.lenses.Lens.write(goggles, fr.wind_blade.isorropia.common.lenses.Lens.Slot.RIGHT,
        fr.wind_blade.isorropia.common.lenses.Lenses.ORDO);
player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, goggles);
// Aim the client at the ground first, so the block the readout will name is known exactly.
var target = new net.minecraft.core.BlockPos(-1, 79, 1);
level.setBlockAndUpdate(target, net.minecraft.core.registries.BuiltInRegistries.BLOCK
        .get(net.minecraft.resources.ResourceLocation.parse("minecraft:air")).defaultBlockState());
var drop = new net.minecraft.world.entity.item.ItemEntity(level, target.getX() + 0.5, target.getY() + 0.2,
        target.getZ() + 0.5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
drop.setNoGravity(true);
drop.setDeltaMovement(0.0, 0.0, 0.0);
level.addFreshEntity(drop);
return "ordo fitted; diamond at " + drop.blockPosition() + " (target " + target + ")";

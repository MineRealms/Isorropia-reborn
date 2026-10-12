package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;

/** Thaumic Horizons' dispenser behaviour for alchemite: like TNT, it leaves the dispenser already lit. */
public final class AlchemiteDispenseBehavior extends DefaultDispenseItemBehavior {
    private AlchemiteDispenseBehavior() {
    }

    /** Called once from common setup, on the main thread, as the dispenser registry is not thread safe. */
    public static void register() {
        DispenserBlock.registerBehavior(ItemsIS.ALCHEMITE.get(), new AlchemiteDispenseBehavior());
    }

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        Level level = source.getLevel();
        Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
        BlockPos pos = source.getPos().relative(facing);
        EntityAlchemitePrimed primed = new EntityAlchemitePrimed(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, null);
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS,
                1.0F, 1.0F);
        level.gameEvent(null, GameEvent.ENTITY_PLACE, pos);
        stack.shrink(1);
        return stack;
    }
}

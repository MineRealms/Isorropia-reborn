package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.projectile.EntityBlastPhial;
import fr.wind_blade.isorropia.common.entities.projectile.EntitySyringe;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import fr.wind_blade.isorropia.common.items.misc.ItemSyringeInjection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Thaumic Horizons' dispenser behaviour for injections: a blast phial is thrown as a phial, anything else as a thrown
 * syringe carrying that injection's essences. TH registered this from its common setup; the listener is declared here
 * so the registration travels with the behaviour instead of touching the mod's constructor.
 */
@Mod.EventBusSubscriber(modid = Isorropia.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SyringeDispenseBehavior extends DefaultDispenseItemBehavior {
    /** TH's launch velocity and inaccuracy for a dispensed injection. */
    private static final float VELOCITY = 1.1F;
    private static final float INACCURACY = 6.0F;
    /** TH's {@code playAuxSFX(1002, ...)}: the bow-release sound rather than the usual dispenser click. */
    private static final int BOW_LEVEL_EVENT = 1002;

    private SyringeDispenseBehavior() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // The dispenser registry is not thread safe, so it is filled on the main thread.
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ItemsIS.SYRINGE_INJECTION.get(),
                new SyringeDispenseBehavior()));
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ItemsIS.BLAST_PHIAL.get(),
                new SyringeDispenseBehavior()));
    }

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        Level level = source.getLevel();
        Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
        // TH's BlockSource.getX()/getY()/getZ() were block-centred, so the shot leaves the block in front.
        BlockPos pos = source.getPos().relative(facing);
        Projectile shot = ItemSyringeInjection.isPhial(stack)
                ? new EntityBlastPhial(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy())
                : new EntitySyringe(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.getTag());
        shot.shoot(facing.getStepX(), facing.getStepY() + 0.1F, facing.getStepZ(), VELOCITY, INACCURACY);
        level.addFreshEntity(shot);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(BlockSource source) {
        source.getLevel().levelEvent(BOW_LEVEL_EVENT, source.getPos(), 0);
    }
}

package fr.wind_blade.isorropia.common.blocks;

import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.entities.EntityAlchemitePrimed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Thaumic Horizons' alchemite: a charge primed the way TNT is — fire, redstone or a burning projectile — whose
 * blast unbinds blocks into crystallized essentia. It is set off by other explosions too, which is what makes a
 * pile of it chain.
 */
public class BlockAlchemite extends Block {

    public BlockAlchemite(Properties properties) {
        super(properties);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        prime(level, pos, igniter);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            prime(level, pos, null);
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            prime(level, pos, null);
            level.removeBlock(pos, false);
        }
    }

    //? if forge {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        return fr.wind_blade.isorropia.common.platform.BlockInteractions.toItemResult(use(state, level, pos, player, hand, hit));
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            //? if forge {
            return super.use(state, level, pos, player, hand, hit);
            //?} else {
            /*return InteractionResult.PASS;
            *///?}
        }
        this.onCaughtFire(state, level, pos, hit.getDirection(), player);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Item item = stack.getItem();
        if (!player.isCreative()) {
            if (stack.is(Items.FLINT_AND_STEEL)) {
                //? if forge {
                stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
                //?} else {
                /*stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                *///?}
            } else {
                stack.shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(item));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Entity owner = projectile.getOwner();
        if (projectile.isOnFire() && projectile.mayInteract(level, pos)) {
            prime(level, pos, owner instanceof LivingEntity living ? living : null);
            level.removeBlock(pos, false);
        }
    }

    /** Caught in another blast, it answers with a short fuse of its own, as TNT does. */
    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide) {
            return;
        }
        EntityAlchemitePrimed primed = new EntityAlchemitePrimed(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, explosion.getIndirectSourceEntity());
        int fuse = primed.getFuse();
        primed.setFuse(level.random.nextInt(fuse / 4) + fuse / 8);
        level.addFreshEntity(primed);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    private static void prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (level.isClientSide) {
            return;
        }
        EntityAlchemitePrimed primed = new EntityAlchemitePrimed(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, igniter);
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS,
                1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }
}

package fr.wind_blade.isorropia.common.items;

import dev.tc4port.thaumcraft.api.ThaumcraftContent;
import fr.wind_blade.isorropia.common.entities.EntitiesIS;
import fr.wind_blade.isorropia.common.entities.EntityGolemTH;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Thaumic Horizons' golem animation powder: sprinkled on a plain, breakable block it raises that block's own matter
 * into a working golem. Unlike the animation focus's short-lived conjuring, a powdered golem lasts TH's thousand
 * ticks and counts the sprinkler as its maker before it settles back into the ground.
 */
public class ItemGolemPowder extends Item {
    public ItemGolemPowder(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null || !animatable(level, pos, state)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        EntityGolemTH golem = EntitiesIS.GOLEM_TH.get().create(level);
        if (golem == null) {
            return InteractionResult.PASS;
        }
        golem.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        golem.loadGolem(state, EntityGolemTH.POWDER_LIFETIME, false, false, player);
        golem.restrictTo(golem.blockPosition(), 32);
        level.removeBlock(pos, false);
        level.addFreshEntity(golem);
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                ThaumcraftContent.sound("wand"), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /** Whether the block can be raised: plain, breakable matter, or one of the few solid transparent blocks. */
    private static boolean animatable(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return state.canOcclude() || whitelisted(state);
    }

    /** The solid but transparent blocks upstream let the powder animate anyway. */
    private static boolean whitelisted(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.CAKE || block == Blocks.CACTUS || block == Blocks.GLASS
                || block == Blocks.PACKED_ICE || block == Blocks.ICE || block == Blocks.COBWEB
                || block == Blocks.TNT;
    }
}

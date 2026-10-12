package fr.wind_blade.isorropia.common.blocks;

import dev.tc4port.thaumcraft.api.wand.WandApi;
import dev.tc4port.thaumcraft.common.WandInteractionTarget;
import fr.wind_blade.isorropia.common.tiles.TileVisDynamo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * TH's vis dynamo. Holding a wand against it charges the tile, which then feeds the vis network through
 * an adjacent relay.
 *
 * <p>TH opened an aspect-selection screen on a bare hand; that screen is still to come, so for now a
 * non-wand click does nothing.</p>
 */
public class BlockVisDynamo extends Block implements EntityBlock, WandInteractionTarget {
    public BlockVisDynamo(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVisDynamo(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof TileVisDynamo dynamo) {
                TileVisDynamo.serverTick(lvl, pos, st, dynamo);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (WandApi.view(held).isEmpty()) {
            return InteractionResult.PASS;
        }
        /*
         * Both sides must enter the held use: the client's release is what ends the charge, and the
         * server-side player is what the tile reads back each tick.
         */
        player.startUsingItem(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileVisDynamo dynamo) {
            dynamo.beginCharging(serverPlayer, held);
        }
        return InteractionResult.SUCCESS;
    }
}

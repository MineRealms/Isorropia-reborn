package fr.wind_blade.isorropia.common.blocks;

import dev.tc4port.thaumcraft.api.wand.WandApi;
import fr.wind_blade.isorropia.common.compat.HeldWandTargets;
import fr.wind_blade.isorropia.common.compat.ThaumBridge;
import fr.wind_blade.isorropia.common.menu.VisDynamoMenu;
import fr.wind_blade.isorropia.common.tiles.TileVisDynamo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * TH's vis dynamo. Holding a wand against it charges the tile, which then feeds the vis network through
 * an adjacent relay; clicking it with anything else opens TH's six-primal selection screen.
 */
public class BlockVisDynamo extends Block implements EntityBlock, ThaumBridge.WandTarget {
    public BlockVisDynamo(Properties properties) {
        super(properties);
    }

    /** The base and rings are drawn by the block entity renderer, as TH's dynamo was all TESR. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVisDynamo(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (lvl, pos, st, be) -> {
                    if (be instanceof TileVisDynamo dynamo) {
                        TileVisDynamo.clientTick(lvl, pos, st, dynamo);
                    }
                }
                : (lvl, pos, st, be) -> {
                    if (be instanceof TileVisDynamo dynamo) {
                        TileVisDynamo.serverTick(lvl, pos, st, dynamo);
                    }
                };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof TileVisDynamo dynamo)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (WandApi.view(held).isEmpty()) {
            // TH opened the selection screen for anything but a wand.
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (id, inventory, opener) -> new VisDynamoMenu(id, inventory, dynamo), getName()), pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        /*
         * Both sides must enter the held use: the client's release is what ends the charge, and the
         * server-side player is what the tile reads back each tick. The dynamo then owns the held wand.
         */
        player.startUsingItem(hand);
        HeldWandTargets.begin(player, pos);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            dynamo.beginCharging(serverPlayer, held);
        }
        return InteractionResult.SUCCESS;
    }
}

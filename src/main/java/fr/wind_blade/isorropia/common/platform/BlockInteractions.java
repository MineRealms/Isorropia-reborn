package fr.wind_blade.isorropia.common.platform;

import net.minecraft.world.InteractionResult;

/**
 * 1.20.5 split {@code Block#use} into {@code useItemOn} (any held stack, empty included) and {@code useWithoutItem}.
 * Every block keeps its 1.20.1 {@code use} body; on 1.21.1 a generated {@code useItemOn} calls it and maps the result
 * here. {@code tools/dual/block_use.py} writes that delegate, so new blocks only need the 1.20.1 override.
 */
public final class BlockInteractions {
    private BlockInteractions() {
    }

    //? if neoforge {
    /*// A pass hands the click on, so the held item's own use (or the empty-hand fallback) still runs.
    public static net.minecraft.world.ItemInteractionResult toItemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            case PASS -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    *///?}
}

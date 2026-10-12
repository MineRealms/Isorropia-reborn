package fr.wind_blade.isorropia.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a machine's item with the machine's own block entity renderer, as Thaumic Horizons' inventory renderers
 * did with their TESRs. One idle block entity per machine is kept for it, never placed in a level.
 */
public final class MachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MachineItemRenderer instance;
    private final Map<Block, BlockEntity> shown = new HashMap<>();

    private MachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static MachineItemRenderer instance() {
        if (instance == null) {
            instance = new MachineItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof EntityBlock entityBlock)) {
            return;
        }
        BlockEntity entity = shown.computeIfAbsent(item.getBlock(),
                block -> entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState()));
        if (entity != null) {
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(entity, poseStack, buffers, light, overlay);
        }
    }
}

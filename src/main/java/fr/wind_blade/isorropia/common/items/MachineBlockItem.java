package fr.wind_blade.isorropia.common.items;

import fr.wind_blade.isorropia.client.renderer.MachineItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * The item of a block drawn only by its block entity renderer. Thaumic Horizons drew these machines in the hand and
 * in the inventory with the same renderer as in the world, so the item hands its drawing to that renderer too.
 */
public class MachineBlockItem extends BlockItem {
    public MachineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return MachineItemRenderer.instance();
            }
        });
    }
}

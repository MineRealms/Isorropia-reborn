package fr.wind_blade.isorropia.common.items.baubles;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** A brain worn on the head, in the Curios head slot where the original bauble used to go. */
public class ItemSomaticBrain extends Item implements ICurioItem {
    public ItemSomaticBrain(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "head".equals(slotContext.identifier());
    }
}

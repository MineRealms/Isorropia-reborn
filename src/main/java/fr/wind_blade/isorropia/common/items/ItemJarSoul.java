package fr.wind_blade.isorropia.common.items;

import fr.wind_blade.isorropia.common.infusion.SoulJar;
import fr.wind_blade.isorropia.common.platform.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The soul jar as an item, named after the creature inside it, as upstream's jar item was. */
public class ItemJarSoul extends BlockItem {
    public ItemJarSoul(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = StackData.read(stack);
        if (tag.isEmpty() || !tag.contains(SoulJar.ENTITY_TAG)) {
            return super.getName(stack);
        }
        CompoundTag data = tag.getCompound(SoulJar.ENTITY_TAG);
        Component creature = EntityType.by(data).map(EntityType::getDescription)
                .orElseGet(() -> Component.literal(data.getString("id")));
        return Component.translatable("item.isorropia.jar_soul.jarred", creature);
    }
}

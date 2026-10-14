package fr.wind_blade.isorropia.common.platform;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if neoforge {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

/**
 * Item NBT access that works on both loaders: 1.20.1 keeps a plain tag on the stack, while 1.20.5+
 * stores it as the {@code minecraft:custom_data} component. Callers mutate a copy and explicitly
 * commit it, so the two loaders see the same read-modify-write flow.
 */
public final class StackData {
    private StackData() {
    }

    /** A copy of the stack's custom data; never null, never the live tag. */
    public static CompoundTag read(ItemStack stack) {
        //? if forge {
        return stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
        //?} else {
        /*return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        *///?}
    }

    public static void write(ItemStack stack, CompoundTag tag) {
        //? if forge {
        stack.setTag(tag.isEmpty() ? null : tag.copy());
        //?} else {
        /*if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        *///?}
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> edit) {
        CompoundTag tag = read(stack);
        edit.accept(tag);
        write(stack, tag);
    }
}

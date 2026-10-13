package fr.wind_blade.isorropia.common.items.misc;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A creature's blood, drawn with a warded syringe. It keeps the creature's NBT so the Seroconverter can later rebuild
 * it, and names itself after the donor. Its tooltip lives in {@code client.SyringeTooltips}, because TH's told the
 * wizard which aspects the sample cost only for aspects they had already discovered.
 */
public class ItemSyringeBloodSample extends Item {
    /** TH's tags: the donor's display name and the creature's own NBT, as {@code critterName} and {@code critter}. */
    public static final String CRITTER_NAME = "critterName";
    public static final String CRITTER = "critter";

    /**
     * What TH always charged to clone anything: four points of Life, before the creature's own infusion costs.
     * TH read those extra costs from a {@code CreatureInfusion} tag the blood infuser stamped on the creature; this
     * port's infusion costs are data-driven recipes instead, so only the flat Life cost is left to show.
     */
    private static final Map<AspectId, Integer> CLONING_COST = Map.of(AspectId.parse("thaumcraft:victus"), 4);

    public ItemSyringeBloodSample(Properties properties) {
        super(properties);
    }

    /** TH's {@code item.syringeSample.name + ": " + critterName}, with {@code INVALID} when the tag is missing. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.isorropia.syringe_blood_sample.named", donorName(stack));
    }

    /** The donor's name, or TH's bare {@code INVALID} for a sample whose creature data was lost. */
    public static String donorName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CRITTER_NAME)) {
            return "INVALID";
        }
        return tag.getString(CRITTER_NAME);
    }

    /** The creature's stored data, ready to be handed back to a spawned entity, or null for a sample without one. */
    @Nullable
    public static CompoundTag critterTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.contains(CRITTER) ? null : tag.getCompound(CRITTER);
    }

    /** What the sample costs to clone, as TH's tooltip listed it. */
    public static Map<AspectId, Integer> cloningCosts() {
        return new LinkedHashMap<>(CLONING_COST);
    }
}

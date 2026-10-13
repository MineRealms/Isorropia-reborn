package fr.wind_blade.isorropia.common.items.misc;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thaumic Horizons' warded syringe. Right-clicking draws the wizard's own blood into a blood syringe; right-clicking
 * a creature instead takes a blood sample carrying that creature's data, which is what the Seroconverter later clones
 * from. Warded syringes are spent on use.
 */
public class ItemSyringeEmpty extends Item {
    /** TH's {@code getPointedEntity(world, p, 1.5)}: a creature must be right in front of the wizard's nose. */
    private static final double REACH = 1.5D;

    /**
     * TH stripped these before storing a creature's NBT: a sample carries genetic data, not a saddlebag full of loot.
     * The 1.12 inventory tags are spelled here as well, since that is what TH's list meant on the old entity layout.
     */
    private static final List<String> STRIPPED_TAGS = List.of("Items", "ArmorItems", "HandItems", "ArmorItem",
            "SaddleItem", "ChestedHorse");

    public ItemSyringeEmpty(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // TH mutated the stack on both sides; on 1.20.1 the drawn blood is server-only, so the client merely swings.
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Entity pointed = pointedEntity(level, player);
        if (pointed instanceof LivingEntity critter && !(pointed instanceof Player) && isBloodDonor(critter)) {
            give(player, stack, sample(critter));
        } else {
            give(player, stack, new ItemStack(ItemsIS.SYRINGE_BLOOD.get()));
        }
        return InteractionResultHolder.consume(stack);
    }

    /** True for the creature types whose blood TH would draw: ordinary animals, bats and fish. */
    private static boolean isBloodDonor(LivingEntity critter) {
        if (critter.getMobType() == MobType.UNDEAD || critter instanceof AbstractVillager) {
            return false;
        }
        return switch (critter.getType().getCategory()) {
            case CREATURE, AMBIENT, WATER_CREATURE -> true;
            default -> false;
        };
    }

    /** A blood sample tagged with the creature's data, exactly as TH's {@code ItemSyringeEmpty} wrote it. */
    public static ItemStack sample(LivingEntity critter) {
        CompoundTag critterTag = new CompoundTag();
        critter.saveWithoutId(critterTag);
        critterTag.putString("id", EntityType.getKey(critter.getType()).toString());
        STRIPPED_TAGS.forEach(critterTag::remove);
        ItemStack sample = new ItemStack(ItemsIS.SYRINGE_BLOOD_SAMPLE.get());
        CompoundTag tag = sample.getOrCreateTag();
        tag.putString(ItemSyringeBloodSample.CRITTER_NAME, critter.getName().getString());
        tag.put(ItemSyringeBloodSample.CRITTER, critterTag);
        return sample;
    }

    /** TH only spent the syringe when the drawn blood actually fit in the wizard's inventory. */
    private static void give(Player player, ItemStack syringe, ItemStack drawn) {
        if (player.getInventory().add(drawn)) {
            syringe.shrink(1);
        }
    }

    /**
     * The creature the wizard is looking at, within {@link #REACH}. TH's own ray (a copy of vanilla's 1.7.10
     * mouse-over ray); on 1.20.1 the same hit is the block clip stopped at the first block, then the nearest pickable
     * entity in the box the ray swept.
     */
    @Nullable
    public static Entity pointedEntity(Level level, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).normalize().scale(REACH);
        Vec3 end = start.add(reach);
        HitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(reach).inflate(1.0D), Entity::isPickable, REACH * REACH);
        return hit == null ? null : hit.getEntity();
    }
}

package fr.wind_blade.isorropia.common.items.misc;

import fr.wind_blade.isorropia.common.entities.projectile.EntityBlastPhial;
import fr.wind_blade.isorropia.common.platform.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
//? if forge {
import net.minecraft.world.item.alchemy.PotionUtils;
//?}
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Thaumic Horizons' injection: an essentia-treated syringe that is injected into its user (or into whatever it is
 * struck against) and hands over the potion effects the Seroconverter mixed into it. The blast-phial form is thrown
 * instead, bursting into a cloud of the same effects.
 *
 * <p>TH kept both forms in one class and told them apart by item damage. Items carry no damage in 1.20.1, so each
 * form is registered under its own id and the flag is held here.</p>
 */
public class ItemSyringeInjection extends Item {
    /** TH's {@code getMaxItemUseDuration}: the syringe is injected in eight ticks. */
    private static final int USE_TICKS = 8;
    /** TH tinted an injection that carried no colour of its own with {@code Color.RED}. */
    private static final int DEFAULT_COLOR = 0xFF0000;
    /** TH stored the colour under its own {@code color} tag; vanilla potions spell the same thing {@code CustomPotionColor}. */
    private static final String COLOR = "color";

    private final boolean phial;

    public ItemSyringeInjection(Properties properties, boolean phial) {
        super(properties);
        this.phial = phial;
    }

    /** True for the blast phial, false for the injection that is injected into a creature. */
    public boolean isPhial() {
        return this.phial;
    }

    public static boolean isPhial(ItemStack stack) {
        return stack.getItem() instanceof ItemSyringeInjection injection && injection.phial;
    }

    @Override
    //? if forge {
    public int getUseDuration(ItemStack stack) {
    //?} else {
    /*public int getUseDuration(ItemStack stack, LivingEntity entity) {
    *///?}
        return USE_TICKS;
    }

    /** TH's {@code EnumAction.bow}: the wizard braces the syringe against their skin as if drawing a bow. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.phial) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            EntityBlastPhial blast = new EntityBlastPhial(level, player, stack.copyWithCount(1));
            // TH threw the phial with EntityBlastPhial's ctor: power 0.5, which it scaled to a velocity of 0.75.
            blast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.75F, 1.0F);
            level.addFreshEntity(blast);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** TH's {@code onEaten}: the essences go into whoever injected themselves, and the syringe is spent. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        List<MobEffectInstance> effects = effects(stack);
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (!level.isClientSide) {
            applyEffects(effects, entity);
        }
        // TH returned null once the last syringe was used, which is an empty stack here.
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /** TH's {@code hitEntity}: struck against a creature, the injection goes into it instead of into the user. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            applyEffects(effects(stack), target);
            stack.shrink(1);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** The effects this injection carries, read the way TH read them: through the vanilla potion tags. */
    public static List<MobEffectInstance> effects(ItemStack stack) {
        //? if forge {
        CompoundTag tag = stack.getTag();
        return tag == null ? List.of() : PotionUtils.getAllEffects(tag);
        //?} else {
        /*net.minecraft.nbt.ListTag list = StackData.read(stack).getList("effects", net.minecraft.nbt.Tag.TAG_COMPOUND);
        List<MobEffectInstance> effects = new java.util.ArrayList<>();
        for (net.minecraft.nbt.Tag entry : list) {
            if (entry instanceof net.minecraft.nbt.CompoundTag compound) {
                effects.add(net.minecraft.world.effect.MobEffectInstance.load(compound));
            }
        }
        return effects;
        *///?}
    }

    private static void applyEffects(List<MobEffectInstance> effects, LivingEntity target) {
        for (MobEffectInstance effect : effects) {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    /**
     * TH tinted the syringe with the {@code color} tag the Seroconverter wrote, falling back to plain red. Vanilla
     * potions carry the same value as {@code CustomPotionColor}, which is what this port's own potion data uses.
     */
    public static int colorOf(@Nullable CompoundTag tag) {
        if (tag == null) {
            return DEFAULT_COLOR;
        }
        if (tag.contains(COLOR)) {
            return tag.getInt(COLOR);
        }
        //? if forge {
        return tag.contains(PotionUtils.TAG_CUSTOM_POTION_COLOR) ? tag.getInt(PotionUtils.TAG_CUSTOM_POTION_COLOR)
                : DEFAULT_COLOR;
        //?} else {
        /*return tag.contains("CustomPotionColor") ? tag.getInt("CustomPotionColor") : DEFAULT_COLOR;
        *///?}
    }

    @Override
    //? if forge {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // TH's addInformation was vanilla's potion tooltip verbatim.
        PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
    }
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext level, List<Component> tooltip, TooltipFlag flag) {
        // TH's addInformation was vanilla's potion tooltip verbatim.
        net.minecraft.world.item.alchemy.PotionContents.addPotionTooltip(effects(stack), tooltip::add, 1.0F, 20.0F);
    }
    *///?}
}

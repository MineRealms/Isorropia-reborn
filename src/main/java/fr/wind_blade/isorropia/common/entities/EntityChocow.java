package fr.wind_blade.isorropia.common.entities;

import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A cow that is milked into chocolate milk. */
public class EntityChocow extends Cow {
    public EntityChocow(EntityType<? extends Cow> type, Level level) {
        super(type, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BUCKET) && !this.isBaby()) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            ItemStack filled = ItemUtils.createFilledResult(held, player, new ItemStack(ItemsIS.CHOCOLATE_MILK_BUCKET.get()));
            player.setItemInHand(hand, filled);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
}

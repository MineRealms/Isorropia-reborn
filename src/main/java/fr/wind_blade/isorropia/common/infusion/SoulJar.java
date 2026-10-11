package fr.wind_blade.isorropia.common.infusion;

import dev.tc4port.thaumcraft.api.ThaumcraftContent;
import fr.wind_blade.isorropia.common.items.ItemsIS;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * A creature kept inside a soul jar. The jar holds the whole entity as NBT, so it survives the world being
 * unloaded; a warded jar is what a capture costs, exactly as upstream charged.
 */
public final class SoulJar {
    /** Where the captured entity lives inside the stack's tag. */
    public static final String ENTITY_TAG = "EntityData";

    private SoulJar() {
    }

    /** Empties a jar of whatever it holds, if it holds a living creature at all. */
    public static LivingEntity take(Player player, ItemStack jar) {
        LivingEntity stored = peek(jar, player.level());
        if (stored == null) {
            return null;
        }
        jar.shrink(1);
        player.getInventory().add(new ItemStack(ThaumcraftContent.item("warded_jar")));
        return stored;
    }

    /** Puts the creature in a jar, if the player has one to spare and something to catch. */
    public static boolean capture(Player player, LivingEntity target) {
        if (player.level().isClientSide || !target.isAlive() || target instanceof Player) {
            return false;
        }
        ItemStack jar = new ItemStack(ItemsIS.JAR_SOUL.get());
        CompoundTag data = new CompoundTag();
        target.save(data);
        jar.getOrCreateTag().put(ENTITY_TAG, data);
        jar.setHoverName(Component.translatable("item.isorropia.jar_soul.jarred",
                target.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
        int slot = findJarSlot(player);
        if (slot < 0 && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("isorropia.containment.fail"), true);
            return false;
        }
        if (slot >= 0 && !player.getAbilities().instabuild) {
            player.getInventory().removeItem(slot, 1);
        }
        if (!player.getInventory().add(jar)) {
            player.drop(jar, false);
        }
        target.discard();
        return true;
    }

    /** Whether the player carries a jar a creature could be put in. */
    public static boolean hasEmptyJar(Player player) {
        return player.getAbilities().instabuild || findJarSlot(player) >= 0;
    }

    /**
     * The jar that would be spent, or -1 for a player carrying none. A soul jar only counts while it is empty:
     * spending a full one would destroy the creature already in it.
     */
    private static int findJarSlot(Player player) {
        var warded = ThaumcraftContent.item("warded_jar");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(warded) || stack.is(ItemsIS.JAR_SOUL.get()) && !holdsCreature(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean holdsCreature(ItemStack jar) {
        return jar.getTag() != null && jar.getTag().contains(ENTITY_TAG);
    }

    /** Puts a stored creature back into the world at a position, and hands back what was freed. */
    public static LivingEntity releaseAt(CompoundTag data, Level level, net.minecraft.core.BlockPos pos) {
        if (level.isClientSide || data == null || data.isEmpty()) {
            return null;
        }
        Entity stored = EntityType.create(data, level).orElse(null);
        if (!(stored instanceof LivingEntity living)) {
            return null;
        }
        living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(living);
        return living;
    }

    /** The creature stored in a jar stack, or null for an empty jar or a broken one. */
    @Nullable
    public static LivingEntity peek(ItemStack jar, Level level) {
        CompoundTag data = jar.getTag() == null ? null : jar.getTag().getCompound(ENTITY_TAG);
        if (data == null || data.isEmpty()) {
            return null;
        }
        Entity stored = EntityType.create(data, level).orElse(null);
        return stored instanceof LivingEntity living ? living : null;
    }
}

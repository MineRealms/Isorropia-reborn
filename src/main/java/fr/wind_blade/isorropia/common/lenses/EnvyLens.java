package fr.wind_blade.isorropia.common.lenses;

import fr.wind_blade.isorropia.common.capabilities.LivingData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Trickster's lens: while the wearer stares at a creature, the lens works out what it would drop and slowly
 * talks it out of the creature. When the patience runs out the drop falls where the creature stands and the
 * lens is done with it; the creature keeps its health, which is the trick.
 */
public class EnvyLens extends Lens {
    /** How much envy each tick of looking adds. */
    private static final int ENVY_PER_TICK = 1;
    /** Patience is proportional to how much the creature is worth to the lens: its health times this. */
    private static final int HEALTH_FACTOR = 20;
    private static final double RANGE = 32.0;

    public EnvyLens() {
        super(id("envy_lens"), 0x00BA00);
    }

    @Override
    public void tick(ServerPlayer player, ItemStack goggles, Slot slot) {
        LivingData data = LivingData.get(player).orElse(null);
        if (data == null) {
            return;
        }
        LivingEntity target = lookedAt(player);
        if (target == null) {
            return;
        }
        data.addEnvy(ENVY_PER_TICK);
        if (data.getEnvy() < target.getMaxHealth() * HEALTH_FACTOR) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // The creature's own drops, handed to whoever was watching it.
        for (ItemStack stack : EnvyStealing.rollDrops(level, target, player)) {
            target.spawnAtLocation(stack);
        }
        data.setHasLooted(true);
        data.clearEnvy();
        data.sync();
    }

    /** The creature the wearer is looking at, if it is close enough and in front of them. */
    @Nullable
    private static LivingEntity lookedAt(Player player) {
        var eye = player.getEyePosition();
        var look = player.getLookAngle();
        var end = eye.add(look.scale(RANGE));
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, end).inflate(1.0), entity -> entity != player && entity.isAlive())) {
            var hit = candidate.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = eye.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        return closest;
    }
}

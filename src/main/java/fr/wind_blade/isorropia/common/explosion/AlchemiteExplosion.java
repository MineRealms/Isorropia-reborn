package fr.wind_blade.isorropia.common.explosion;

import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.aspect.AspectQueryApi;
import dev.tc4port.thaumcraft.api.aspect.VisAction;
import dev.tc4port.thaumcraft.api.damage.ThaumcraftDamageTypes;
import dev.tc4port.thaumcraft.api.item.ThaumcraftItemApi;
import dev.tc4port.thaumcraft.api.node.AuraNodeState;
import dev.tc4port.thaumcraft.api.node.AuraNodeView;
import dev.tc4port.thaumcraft.api.node.NodeApi;
import dev.tc4port.thaumcraft.api.node.NodeModifierId;
import dev.tc4port.thaumcraft.api.node.NodeTypeId;
import dev.tc4port.thaumcraft.api.ThaumcraftContent;
import fr.wind_blade.isorropia.common.network.ClientFxPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thaumic Horizons' alchemite blast. It is an ordinary explosion in reach and damage — creatures caught in it
 * take dissolving damage — but where a block would be destroyed it is shattered into the crystallized essentia
 * of its aspects instead. Aura nodes are not shattered outright: most of the time the blast merely scars them.
 *
 * <p>Vanilla's {@link Explosion} does the ray-casting and the entity damage; the blocks it collected are then
 * unbound here one by one, so {@link Explosion.BlockInteraction#KEEP} stops it from dropping anything itself.
 */
public final class AlchemiteExplosion {

    /** TH's blast radius. */
    public static final float RADIUS = 5.0F;

    /** TH's size for the burst the client plays over the blast. */
    public static final float BURST_SIZE = 10.0F;

    private static final double NODE_SHATTER_CHANCE = 0.25D;
    private static final double NODE_FADE_CHANCE = 0.5D;

    /**
     * Aura nodes are collected before their resistance is consulted: TH checks for a node first and only then
     * charges the ray for the block, which matters because a node's blast resistance (200) would otherwise stop
     * the ray dead and leave it untouched. Reporting no resistance for them reproduces that ordering.
     */
    private static final ExplosionDamageCalculator NODE_PIERCE = new ExplosionDamageCalculator() {
        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos,
                BlockState state, FluidState fluid) {
            return state.is(ThaumcraftContent.block("aura_node"))
                    ? Optional.empty()
                    : super.getBlockExplosionResistance(explosion, level, pos, state, fluid);
        }
    };

    private AlchemiteExplosion() {
    }

    public static void detonate(ServerLevel level, @Nullable Entity source, double x, double y, double z) {
        Explosion explosion = new Explosion(level, source, ThaumcraftDamageTypes.dissolve(level), NODE_PIERCE,
                x, y, z, RADIUS, false, Explosion.BlockInteraction.KEEP);
        explosion.explode();
        // A server-side blast plays no sound of its own (only the client replays one for its own explosions) and
        // KEEP suppresses the smoke ring, so both are done here the way TH's doExplosionB did them.
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (BlockPos pos : List.copyOf(explosion.getToBlow())) {
            unbind(level, pos, explosion);
        }
        ClientFxPacket.burst(level, new Vec3(x, y, z), BURST_SIZE);
    }

    /** Turns one block in the blast into its crystallized essentia, or drops it as usual when it has none. */
    private static void unbind(ServerLevel level, BlockPos pos, Explosion explosion) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (state.is(ThaumcraftContent.block("aura_node")) && level.getBlockEntity(pos) instanceof AuraNodeView node) {
            unbindNode(level, pos, node);
            return;
        }
        ItemStack stack = new ItemStack(state.getBlock());
        AspectAmounts aspects = stack.isEmpty() ? AspectAmounts.EMPTY : AspectQueryApi.placedBlock(stack, state);
        if (aspects.amounts().isEmpty()) {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos), null, ItemStack.EMPTY);
        } else {
            for (Map.Entry<AspectId, Integer> entry : aspects.amounts().entrySet()) {
                spawnEssence(level, pos, entry.getKey(), entry.getValue());
            }
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        }
        // Removing it through the block hook lets alchemite (and anything else that reacts to a blast) answer.
        state.onBlockExploded(level, pos, explosion);
    }

    /**
     * TH rolls once per node: a quarter of the time it is shattered into its vis, a quarter leaves it fading,
     * and the rest make it unstable. Only the shattered case is destructive.
     */
    private static void unbindNode(ServerLevel level, BlockPos pos, AuraNodeView node) {
        AuraNodeState state = node.nodeState();
        double roll = level.random.nextDouble();
        if (roll < NODE_SHATTER_CHANCE) {
            for (Map.Entry<AspectId, Integer> entry : state.currentVis().amounts().entrySet()) {
                spawnEssence(level, pos, entry.getKey(), entry.getValue());
            }
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(level.getBlockState(pos)));
            level.removeBlock(pos, false);
        } else if (roll < NODE_FADE_CHANCE) {
            NodeApi.replaceLoadedState(level, pos, state,
                    state.withState(state.type(), NodeModifierId.FADING, state.baseVis(), state.currentVis()),
                    VisAction.EXECUTE);
        } else {
            NodeApi.replaceLoadedState(level, pos, state,
                    state.withState(NodeTypeId.UNSTABLE, NodeModifierId.NONE, state.baseVis(), state.currentVis()),
                    VisAction.EXECUTE);
        }
    }

    /**
     * Drops invulnerable crystals of the given aspect, their stack counts carrying the amount. A shattered node can
     * hold far more than a stack, so the amount is split into full stacks rather than one oversized one.
     */
    private static void spawnEssence(ServerLevel level, BlockPos pos, AspectId aspect, int count) {
        while (count > 0) {
            ItemStack essence = ThaumcraftItemApi.createCrystalEssence(aspect);
            int size = Math.min(count, essence.getMaxStackSize());
            essence.setCount(size);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, essence);
            drop.setInvulnerable(true);
            level.addFreshEntity(drop);
            count -= size;
        }
    }
}

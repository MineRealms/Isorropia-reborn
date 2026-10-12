package fr.wind_blade.isorropia.common.compat;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TC4's {@code IWandable.onUsingWandTick}: once a wand is held against one of the mod's machines, the machine owns
 * the held use, so the wand's focus does not fire alongside it. TC4R only does this for its own aura nodes; for
 * every other block it keeps running the installed focus (fire, shock and excavation every tick, addon foci through
 * their USING_TICK action). {@code WandItemMixin} asks {@link #interceptUseTick} before each wand use tick.
 *
 * <p>As in TH, looking away from the machine ends the use instead of handing the held click back to the focus.
 * The client and the integrated server share this class, so each side keeps its own record.</p>
 */
@Mod.EventBusSubscriber(modid = Isorropia.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HeldWandTargets {
    /** How far a wizard may aim and still be holding the wand against the machine; the tiles use the same reach. */
    public static final double REACH = 6.0D;
    private static final String HANDLER_SUFFIX = "isorropia$machineHold";
    private static final Map<UUID, BlockPos> CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> SERVER = new ConcurrentHashMap<>();

    private HeldWandTargets() {
    }

    private static Map<UUID, BlockPos> side(LivingEntity entity) {
        return entity.level().isClientSide ? CLIENT : SERVER;
    }

    /** Called by a machine block right after it put the wizard into the held use, on both sides. */
    public static void begin(Player player, BlockPos machine) {
        side(player).put(player.getUUID(), machine.immutable());
    }

    /** True while this player's held wand belongs to a machine; the wand's own use tick is then skipped. */
    public static boolean interceptUseTick(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        Map<UUID, BlockPos> holds = side(player);
        BlockPos machine = holds.get(player.getUUID());
        if (machine == null) {
            return false;
        }
        if (!player.isUsingItem() || !(player.level().getBlockState(machine).getBlock() instanceof ThaumBridge.WandTarget)) {
            holds.remove(player.getUUID());
            return false;
        }
        if (!machine.equals(aimedBlock(player))) {
            // TH stopped using the wand when the wizard looked away rather than letting the focus take over.
            holds.remove(player.getUUID());
            player.stopUsingItem();
        }
        return true;
    }

    /** True if this player's held use currently belongs to the given machine. */
    public static boolean isHolding(Player player, BlockPos machine) {
        return machine.equals(side(player).get(player.getUUID()));
    }

    private static BlockPos aimedBlock(Player player) {
        Vec3 eye = player.getEyePosition();
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(REACH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
    }

    /** Whether the mixin made it into TC4R's wand; logged once at setup so a TC4R update cannot hide the regression. */
    public static boolean hooked() {
        try {
            Class<?> wand = Class.forName("dev.tc4port.thaumcraft.item.WandItem");
            for (Method method : wand.getDeclaredMethods()) {
                if (method.getName().endsWith(HANDLER_SUFFIX)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static void reportHook() {
        if (hooked()) {
            Isorropia.LOGGER.info("Wand use tick hook installed: machines own the held wand");
        } else {
            Isorropia.LOGGER.warn("Wand use tick hook missing: foci will fire while a wand is held against a machine");
        }
    }

    /** A new held use starts clean; the machine block marks it again if it is the one that started it. */
    @SubscribeEvent
    public static void onHeldWandUseStart(LivingEntityUseItemEvent.Start event) {
        side(event.getEntity()).remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onHeldWandLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CLIENT.remove(event.getEntity().getUUID());
        SERVER.remove(event.getEntity().getUUID());
    }
}

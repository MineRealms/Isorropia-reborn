package fr.wind_blade.isorropia.common.events;

import dev.tc4port.thaumcraft.api.ThaumcraftApiHelper;
import dev.tc4port.thaumcraft.api.aspect.VisAction;
import dev.tc4port.thaumcraft.api.aspect.VisChannel;
import dev.tc4port.thaumcraft.api.aspect.VisConsumptionContext;
import dev.tc4port.thaumcraft.api.aspect.VisCost;
import dev.tc4port.thaumcraft.api.damage.ThaumcraftDamageTypes;
import dev.tc4port.thaumcraft.api.research.ResearchApi;
import dev.tc4port.thaumcraft.api.research.ResearchKey;
import dev.tc4port.thaumcraft.api.wand.WandApi;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.capabilities.LivingData;
import fr.wind_blade.isorropia.common.focus.ContainmentFocus;
import fr.wind_blade.isorropia.common.infusion.CreatureAbilities;
import fr.wind_blade.isorropia.common.infusion.SelfInfusions;
import fr.wind_blade.isorropia.common.infusion.SoulBeacon;
import fr.wind_blade.isorropia.common.lenses.LensManager;
import fr.wind_blade.isorropia.common.vat.VatStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
//?} else {
/*import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
*///?}
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Everything the infusion system hangs off the world with: the per-entity infusion data, the wand gesture that
 * forms a vat, the abilities a body carries, and what happens when one of those bodies dies.
 */
@Mod.EventBusSubscriber(modid = Isorropia.MODID)
public final class IsorropiaEvents {
    private static final ResourceLocation LIVING_DATA = Isorropia.id("living_data");
    private static final ResearchKey CURATIVE_VAT = ResearchKey.of(Isorropia.MODID, "curativevat");
    /** Thaumic Horizons charged water, earth and order vis for the vat. */
    private static final Map<VisChannel, Integer> VAT_COST = Map.of(VisChannel.AQUA, 50, VisChannel.TERRA, 50, VisChannel.ORDO, 50);

    private IsorropiaEvents() {
    }

    // NeoForge 1.21.1 has no attach event: LivingData rides a lazily-created data attachment instead.
    //? if forge {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity living && !living.getCapability(LivingData.CAPABILITY).isPresent()) {
            event.addCapability(LIVING_DATA, new LivingData.Provider(living));
        }
    }
    //?}

    /** Infusions survive a dimension change; a self-infused body does not survive dying in one. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            return;
        }
        LivingData.get(event.getOriginal()).ifPresent(
                old -> LivingData.get(event.getEntity()).ifPresent(fresh -> fresh.deserialize(old.serialize())));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LivingData.get(player).ifPresent(data -> data.syncTo(player));
        }
    }

    /** A player who leaves stops holding anything in the containment beam. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ContainmentFocus.forget(event.getEntity().getUUID());
    }

    //? if forge {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        playerTick(player);
    }
    //?} else {
    /*@SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerTick(player);
        }
    }
    *///?}

    private static void playerTick(ServerPlayer player) {
        LivingData.get(player).ifPresent(data -> SelfInfusions.tick(player, data));
        LensManager.tick(player);
    }

    // NeoForge 1.21 dropped LivingTickEvent; EntityTickEvent fires for every entity, so narrow it there.
    //? if forge {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player) {
            return;
        }
        LivingData.get(entity).ifPresent(data -> CreatureAbilities.tick(entity, data));
    }
    //?} else {
    /*@SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity.level().isClientSide || entity instanceof Player) {
            return;
        }
        LivingData.get(entity).ifPresent(data -> CreatureAbilities.tick(entity, data));
    }
    *///?}

    /** A silver heart shrugs off taint; a shock skin answers a blow; an ender heart slips out of the way. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        LivingData data = LivingData.get(entity).orElse(null);
        if (data == null) {
            return;
        }
        if (data.hasSelfInfusion(SelfInfusions.SILVER_HEART) && isTaint(event.getSource())) {
            event.setCanceled(true);
            return;
        }
        // Shock skin answers any attacker; the ender heart only dodges what was thrown or shot at it.
        if (data.hasInfusion(CreatureAbilities.SHOCK_SKIN)
                && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != entity) {
            CreatureAbilities.shockBack(entity, attacker);
        }
        Entity direct = event.getSource().getDirectEntity();
        if (data.hasInfusion(CreatureAbilities.ENDER_HEART) && direct != null && direct != event.getSource().getEntity()) {
            CreatureAbilities.teleportFromRanged(entity);
            event.setCanceled(true);
        }
    }

    /**
     * A body that takes a killing blow: the tumor gives its warp back and every ability the body was grown with
     * is lost. A wizard bound to a soul beacon is then reborn in the body waiting in the vat, with that body's
     * abilities only, which is the whole point of growing one. Upstream does the same, in the same order.
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide
                || player.getHealth() - event.getAmount() > 0.0F) {
            return;
        }
        LivingData.get(player).ifPresent(data -> {
            if (data.getTumorWarpTotal() > 0) {
                SelfInfusions.releaseTumorWarp(player, data);
            }
            if (!data.getSelfInfusions().isEmpty()) {
                data.resetSelfInfusions();
            }
            data.sync();
        });
        SoulBeacon.tryRevive(event, player);
    }

    /**
     * Forming the vat: a wand right-click on the frame the player built. Thaumcraft has no extension point for
     * a wand turning a structure into a multiblock, so the click is claimed here.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide) {
            return;
        }
        ItemStack wand = event.getItemStack();
        if (WandApi.view(wand).isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        if (!ResearchApi.isComplete(player, CURATIVE_VAT)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        if (VatStructure.isVatBlock(level.getBlockState(clicked))) {
            return;
        }
        BlockPos master = VatStructure.findUnformed(level, clicked).orElse(null);
        if (master == null) {
            return;
        }
        VisCost cost = VisCost.ofWholeVis(VAT_COST);
        if (!ThaumcraftApiHelper.consumeVisFromWand(wand, player, cost, VisAction.EXECUTE, VisConsumptionContext.CRAFTING)
                .consumed()) {
            return;
        }
        VatStructure.form(level, master);
        event.setCanceled(true);
    }

    /** A wand aimed at a creature with the portability organ jars it, as upstream's capture gesture. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide
                || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (WandApi.view(event.getItemStack()).isEmpty()) {
            return;
        }
        LivingData data = LivingData.get(target).orElse(null);
        if (data != null && data.hasInfusion(CreatureAbilities.PORTABILITY) && CreatureAbilities.jarInto(event.getEntity(), target)) {
            event.setCanceled(true);
        }
    }

    private static boolean isTaint(DamageSource source) {
        return source.is(ThaumcraftDamageTypes.FLUX_TAINT);
    }
}

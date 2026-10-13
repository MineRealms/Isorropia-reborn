package fr.wind_blade.isorropia.common.wand;

import dev.tc4port.thaumcraft.api.research.ResearchRequirement;
import dev.tc4port.thaumcraft.api.wand.WandApi;
import dev.tc4port.thaumcraft.api.wand.WandCapSpec;
import dev.tc4port.thaumcraft.api.wand.WandMaterialId;
import dev.tc4port.thaumcraft.api.wand.WandPartApi;
import dev.tc4port.thaumcraft.api.wand.WandRechargeSpec;
import dev.tc4port.thaumcraft.api.wand.WandRodSpec;
import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * TH's disposable crystal wand, split into the rod and cap TC4R builds wands from. TH registered both parts at
 * startup ({@code ROD_CRYSTAL} / {@code CAP_CRYSTAL}); here they are installed through {@link WandPartApi} because
 * TC4R's data-driven parts need a raw part item and a tag, and this wand is never assembled by hand.
 *
 * <p>TH's rift hands out a wand of these parts charged to the brim: a huge capacity (250 vis a primal) that, in TH,
 * could never recharge. TC4R has no "disposable" flag, so the wand this makes is a normal one; the rift simply
 * creates it already full, which is the only way it is ever obtained.</p>
 */
public final class CrystalWandParts {
    /** One material id for both parts, as TH named its rod and cap "disposable" alike. */
    public static final WandMaterialId ROD = WandMaterialId.of(Isorropia.id("crystal"));
    public static final WandMaterialId CAP = WandMaterialId.of(Isorropia.id("crystal"));

    private static final int CRAFT_COST = 99;
    private static final int ORDER = 100;

    private CrystalWandParts() {
    }

    /** Installs the crystal rod and cap. Call once, from the common setup phase. */
    public static void register() {
        // TH's WandRod("disposable", 250, ..., 99, crystalRod.png), glowing, and not a staff.
        WandPartApi.registerRod(new WandRodSpec(Isorropia.id("crystal_wand_rod"), ROD, 250, CRAFT_COST,
                WandRechargeSpec.none(), ResearchRequirement.none(),
                Isorropia.id("textures/models/crystal_rod.png"), false, true, false, ORDER));
        // TH's WandCap("disposable", 0.9F, ..., 99) with crystalCap.png: the same 0.9 discount as thaumium.
        WandPartApi.registerCap(new WandCapSpec(Isorropia.id("crystal_wand_cap"), CAP, 0.9F, Set.of(), 0.9F,
                Isorropia.id("textures/models/crystal_cap.png"), CRAFT_COST, ResearchRequirement.none(), ORDER));
    }

    /** The wand the rift hands out: a crystal rod and cap, charged to the brim. */
    public static ItemStack chargedWand() {
        return WandApi.createFullyCharged(ROD, CAP);
    }
}

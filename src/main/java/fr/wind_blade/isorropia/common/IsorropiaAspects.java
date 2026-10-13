package fr.wind_blade.isorropia.common;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.resources.ResourceLocation;

/**
 * Aspect identities used by the port. Isorropia created {@code fames} and {@code corpus} because TC6 had
 * dropped them; TC4R has both natively. The seven sin aspects are ours, declared as data under
 * {@code data/isorropia/thaumcraft/aspects/}.
 *
 * <p>TC6 constant to TC4 id: AIR aer, EARTH terra, FIRE ignis, WATER aqua, ORDER ordo, ENTROPY perditio,
 * VOID vacuos, LIGHT lux, MOTION motus, COLD gelum, CRYSTAL vitreus, METAL metallum, LIFE victus,
 * DEATH mortuus, ENERGY potentia, EXCHANGE permutatio, AURA auram, FLUX vitium, DARKNESS tenebrae,
 * ELDRITCH alienis, FLIGHT volatus, PLANT herba, TOOL instrumentum, CRAFT fabrico, MECHANISM machina,
 * TRAP vinculum, SOUL spiritus, MIND cognitio, SENSES sensus, AVERSION telum, PROTECT tutamen,
 * DESIRE lucrum, UNDEAD exanimis, BEAST bestia, MAN humanus, MAGIC praecantatio. TC6 ALCHEMY has no
 * TC4 counterpart; use permutatio.
 */
public final class IsorropiaAspects {
    public static final AspectId AER = tc("aer");
    public static final AspectId TERRA = tc("terra");
    public static final AspectId IGNIS = tc("ignis");
    public static final AspectId AQUA = tc("aqua");
    public static final AspectId ORDO = tc("ordo");
    public static final AspectId PERDITIO = tc("perditio");
    public static final AspectId VACUOS = tc("vacuos");
    public static final AspectId LUX = tc("lux");
    public static final AspectId MOTUS = tc("motus");
    public static final AspectId GELUM = tc("gelum");
    public static final AspectId VITREUS = tc("vitreus");
    public static final AspectId METALLUM = tc("metallum");
    public static final AspectId VICTUS = tc("victus");
    public static final AspectId MORTUUS = tc("mortuus");
    public static final AspectId POTENTIA = tc("potentia");
    public static final AspectId PERMUTATIO = tc("permutatio");
    public static final AspectId AURAM = tc("auram");
    public static final AspectId VITIUM = tc("vitium");
    public static final AspectId TENEBRAE = tc("tenebrae");
    public static final AspectId ALIENIS = tc("alienis");
    public static final AspectId VOLATUS = tc("volatus");
    public static final AspectId HERBA = tc("herba");
    public static final AspectId INSTRUMENTUM = tc("instrumentum");
    public static final AspectId FABRICO = tc("fabrico");
    public static final AspectId MACHINA = tc("machina");
    public static final AspectId VINCULUM = tc("vinculum");
    public static final AspectId SPIRITUS = tc("spiritus");
    public static final AspectId COGNITIO = tc("cognitio");
    public static final AspectId SENSUS = tc("sensus");
    public static final AspectId TELUM = tc("telum");
    public static final AspectId TUTAMEN = tc("tutamen");
    public static final AspectId LUCRUM = tc("lucrum");
    public static final AspectId EXANIMIS = tc("exanimis");
    public static final AspectId BESTIA = tc("bestia");
    public static final AspectId HUMANUS = tc("humanus");
    public static final AspectId PRAECANTATIO = tc("praecantatio");
    public static final AspectId SANO = tc("sano");
    public static final AspectId LIMUS = tc("limus");
    public static final AspectId VENENUM = tc("venenum");
    public static final AspectId MESSIS = tc("messis");
    /** TH's weather aspect (TC6 {@code WEATHER}). */
    public static final AspectId TEMPESTAS = tc("tempestas");
    /** TH's mining aspect (TC6 {@code MINE}). */
    public static final AspectId PERFODIO = tc("perfodio");
    /** TH's harvest aspect (TC6 {@code HARVEST}). */
    public static final AspectId METO = tc("meto");
    /** TH's travel aspect (TC6 {@code TRAVEL}). */
    public static final AspectId ITER = tc("iter");

    /** Hunger. */
    public static final AspectId FAMES = tc("fames");
    /** Flesh. */
    public static final AspectId CORPUS = tc("corpus");
    public static final AspectId INVIDIA = own("invidia");
    public static final AspectId GULA = own("gula");
    public static final AspectId LUXURIA = own("luxuria");
    public static final AspectId INFERNUS = own("infernus");
    public static final AspectId SUPERBIA = own("superbia");
    public static final AspectId DESIDIA = own("desidia");
    public static final AspectId IRA = own("ira");

    private IsorropiaAspects() {
    }

    private static AspectId tc(String path) {
        return AspectId.of(ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }

    private static AspectId own(String path) {
        return AspectId.of(ResourceLocation.fromNamespaceAndPath("isorropia", path));
    }
}

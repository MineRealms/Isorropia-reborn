"""Wires the lens system: items, capability fields, network registration and the server tick."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/fr/wind_blade/isorropia"

# The four lens items already exist; give them a lookup so a lens can be handed back to the player.
path = JAVA / "common/items/ItemsIS.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""    private ItemsIS() {
    }""", """    private ItemsIS() {
    }

    /** The item a lens is made of, so one can be handed back or shown in a menu. */
    public static java.util.Optional<ItemStack> forLens(net.minecraft.resources.ResourceLocation lens) {
        if (Lenses.AIR.id().equals(lens)) {
            return Optional.of(new ItemStack(AIR_LENS.get()));
        }
        if (Lenses.FIRE.id().equals(lens)) {
            return Optional.of(new ItemStack(FIRE_LENS.get()));
        }
        if (Lenses.ORDO.id().equals(lens)) {
            return Optional.of(new ItemStack(ORDO_LENS.get()));
        }
        if (Lenses.ENVY.id().equals(lens)) {
            return Optional.of(new ItemStack(ENVY_LENS.get()));
        }
        return Optional.empty();
    }""")
s = s.replace("import fr.wind_blade.isorropia.common.items.misc.ItemLens;",
              "import fr.wind_blade.isorropia.common.items.misc.ItemLens;\nimport fr.wind_blade.isorropia.common.lenses.Lenses;")
s = s.replace("import java.util.function.Supplier;", "import java.util.Optional;\nimport java.util.function.Supplier;")
path.write_text(s, encoding="utf-8")

# The envy lens needs somewhere to remember its own progress and whether it has already stolen.
path = JAVA / "common/capabilities/LivingData.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""    private boolean climbDisabled;""", """    private int envy;
    private boolean hasLooted;
    private boolean climbDisabled;""")
s = s.replace("""    public boolean isClimbDisabled() {""", """    /** How much the trickster's lens has talked out of whatever the player was looking at. */
    public int getEnvy() {
        return envy;
    }

    public void addEnvy(int amount) {
        envy += amount;
    }

    public void clearEnvy() {
        envy = 0;
    }

    /** True once the lens has already taken a creature's drop, so it does not take it twice. */
    public boolean hasLooted() {
        return hasLooted;
    }

    public void setHasLooted(boolean hasLooted) {
        this.hasLooted = hasLooted;
    }

    public boolean isClimbDisabled() {""")
s = s.replace("""        tag.putBoolean("ClimbDisabled", climbDisabled);""",
              """        tag.putInt("Envy", envy);
        tag.putBoolean("HasLooted", hasLooted);
        tag.putBoolean("ClimbDisabled", climbDisabled);""")
s = s.replace("""        climbDisabled = tag.getBoolean("ClimbDisabled");""",
              """        envy = tag.getInt("Envy");
        hasLooted = tag.getBoolean("HasLooted");
        climbDisabled = tag.getBoolean("ClimbDisabled");""")
path.write_text(s, encoding="utf-8")

# Register the new packet: ids are handed out in call order, so this one goes last.
path = JAVA / "common/network/IsorropiaNetwork.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""        register(ClientFxPacket.class, NetworkDirection.PLAY_TO_CLIENT,
                ClientFxPacket::encode, ClientFxPacket::decode, ClientFxPacket::handle);""",
              """        register(ClientFxPacket.class, NetworkDirection.PLAY_TO_CLIENT,
                ClientFxPacket::encode, ClientFxPacket::decode, ClientFxPacket::handle);
        register(LensChangePacket.class, NetworkDirection.PLAY_TO_SERVER,
                LensChangePacket::encode, LensChangePacket::decode, LensChangePacket::handle);""")
path.write_text(s, encoding="utf-8")

# The server runs the worn lenses once per player tick.
path = JAVA / "common/events/IsorropiaEvents.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            LivingData.get(player).ifPresent(data -> SelfInfusions.tick(player, data));
        }
    }""",
"""    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        LivingData.get(player).ifPresent(data -> SelfInfusions.tick(player, data));
        LensManager.tick(player);
    }""")
s = s.replace("import fr.wind_blade.isorropia.common.infusion.SelfInfusions;",
              "import fr.wind_blade.isorropia.common.infusion.SelfInfusions;\nimport fr.wind_blade.isorropia.common.lenses.LensManager;")
path.write_text(s, encoding="utf-8")

print("lenses wired")

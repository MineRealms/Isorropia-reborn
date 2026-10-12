package fr.wind_blade.isorropia.client.fx;

import dev.tc4port.thaumcraft.api.client.ThaumcraftClientEffectsApi;
import fr.wind_blade.isorropia.common.network.ClientFxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Plays the effects the server asks for. Each one maps onto a Thaumcraft effect where Thaumcraft has the
 * equivalent, so the vat and the creatures it infuses look like the rest of the mod.
 */
public final class IsorropiaFx {
    private IsorropiaFx() {
    }

    public static void play(ClientFxPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        switch (packet.kind()) {
            case RUNE -> runes(level, packet);
            case ARC -> ThaumcraftClientEffectsApi.spawnFocusLightning(level, packet.from(), packet.to());
            case ITEM_FLIGHT -> ThaumcraftClientEffectsApi.spawnDestinationWisp(level, packet.from(), packet.to(),
                    0.35F, 0, true, 0.0F);
            case FLUX_BURST -> burst(level, packet);
            case BURST -> ThaumcraftClientEffectsApi.spawnBurst(level, packet.from(), packet.strength());
        }
    }

    /** The runes that crawl over the floor of a working vat, in the colours the original used. */
    private static void runes(ClientLevel level, ClientFxPacket packet) {
        float red = packet.red();
        float green = packet.green();
        float blue = packet.blue();
        for (int i = 0; i < packet.count(); i++) {
            float jitterR = red * (0.8F + level.random.nextFloat() * 0.4F);
            float jitterG = green * (0.8F + level.random.nextFloat() * 0.4F);
            float jitterB = blue * (0.8F + level.random.nextFloat() * 0.4F);
            ThaumcraftClientEffectsApi.spawnBlockRune(level, packet.from(), jitterR, jitterG, jitterB, 1, -0.03F);
        }
    }

    /** A vat coming apart throws its flux out in one go, as 1.12's flux explosion. */
    private static void burst(ClientLevel level, ClientFxPacket packet) {
        VatFx.fluxExplosion(level, packet.from().x, packet.from().y, packet.from().z);
    }

    /** Beams between two points in the world, used by the creatures that burn things. */
    public static void arc(ClientLevel level, Vec3 from, Vec3 to) {
        ThaumcraftClientEffectsApi.spawnShockLightning(level, from, to);
    }
}

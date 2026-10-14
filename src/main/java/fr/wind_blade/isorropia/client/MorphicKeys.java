package fr.wind_blade.isorropia.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.capabilities.LivingData;
import fr.wind_blade.isorropia.common.infusion.SelfInfusions;
import fr.wind_blade.isorropia.common.network.IsorropiaNetwork;
import fr.wind_blade.isorropia.common.network.OpenFingersPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
//? if forge {
import net.minecraftforge.event.TickEvent;
//?} else {
/*import net.neoforged.neoforge.client.event.ClientTickEvent;
*///?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The morphic fingers' key. Tapping it asks the server to open the portable arcane workbench; the server
 * checks the fingers were actually grown before it does.
 */
public final class MorphicKeys {
    public static final KeyMapping FINGERS = new KeyMapping("key.isorropia.fingers",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, LensKeys.CATEGORY);

    private MorphicKeys() {
    }

    @Mod.EventBusSubscriber(modid = Isorropia.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        private Register() {
        }

        // Handler names must be unique per package: the event bus merges handlers whose class simple name,
        // method and event match (see tools/check_listener_names.py).
        @SubscribeEvent
        public static void onRegisterFingersKey(RegisterKeyMappingsEvent event) {
            event.register(FINGERS);
        }
    }

    @Mod.EventBusSubscriber(modid = Isorropia.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Poller {
        private Poller() {
        }

        //? if forge {
        @SubscribeEvent
        public static void onFingersKeyTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            pollFingersKey();
        }
        //?} else {
        /*@SubscribeEvent
        public static void onFingersKeyTick(ClientTickEvent.Post event) {
            pollFingersKey();
        }
        *///?}

        private static void pollFingersKey() {
            Minecraft minecraft = Minecraft.getInstance();
            // Drain the press even when a screen is up, so it does not fire when the screen closes.
            boolean pressed = FINGERS.consumeClick();
            if (!pressed || minecraft.player == null || minecraft.screen != null) {
                return;
            }
            LivingData.get(minecraft.player).ifPresent(data -> {
                if (data.hasSelfInfusion(SelfInfusions.MORPHIC_FINGERS)) {
                    IsorropiaNetwork.sendToServer(new OpenFingersPacket());
                }
            });
        }
    }
}

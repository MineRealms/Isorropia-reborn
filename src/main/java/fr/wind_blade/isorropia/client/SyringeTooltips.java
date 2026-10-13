package fr.wind_blade.isorropia.client;

import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.player.PlayerKnowledgeApi;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.items.misc.ItemSyringeBloodSample;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Thaumic Horizons' blood-sample tooltip. TH wrote it into the item, which 1.7.10 only asked for on the client and
 * handed the viewing wizard; on 1.20.1 an item cannot see who is reading it, so the lines are added from the client's
 * tooltip event - the same place Thaumcraft Reborn writes the tooltips that depend on what the wizard has discovered.
 */
@Mod.EventBusSubscriber(modid = Isorropia.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SyringeTooltips {
    private SyringeTooltips() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack sample = event.getItemStack();
        if (!(sample.getItem() instanceof ItemSyringeBloodSample)) {
            return;
        }
        event.getToolTip().add(
                Component.translatable("item.isorropia.syringe_blood_sample.cost").withStyle(ChatFormatting.GRAY));
        Player viewer = event.getEntity();
        for (Map.Entry<AspectId, Integer> cost : ItemSyringeBloodSample.cloningCosts().entrySet()) {
            event.getToolTip().add(known(viewer, cost.getKey())
                    ? AspectApi.tooltipEntry(cost.getKey(), " x" + cost.getValue())
                    : Component.translatable("tc.aspect.unknown"));
        }
    }

    /** TH greyed an aspect out to {@code Unknown Aspect} until the wizard had discovered it. */
    private static boolean known(@Nullable Player viewer, AspectId aspect) {
        return viewer != null && PlayerKnowledgeApi.view(viewer).discoveredAspects().contains(aspect);
    }
}

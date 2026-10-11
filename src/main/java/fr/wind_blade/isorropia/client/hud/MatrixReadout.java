package fr.wind_blade.isorropia.client.hud;

import fr.wind_blade.isorropia.common.lenses.LensManager;
import fr.wind_blade.isorropia.common.tiles.TileModifiedMatrix;
import fr.wind_blade.isorropia.common.tiles.TileVat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * What revealing goggles show of a curative vat while looking at its modified matrix, as the 1.12 matrix's goggles
 * text did: how stable the vat is, the flux it holds, the celestial aura it has gathered, and what the stabilisers
 * give back against what the running infusion takes each cycle. Thaumcraft 4 Reborn's goggles overlay offers no
 * way for a block to add lines, so the readout is drawn here, under the crosshair.
 */
public final class MatrixReadout {
    /** Upstream's colour for the celestial aura line, (0.49, 0.56, 0.64). */
    private static final int AURA_COLOUR = 0x7D8EA2;
    private static final int TOP_OFFSET = 30;

    private MatrixReadout() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !LensManager.isGoggles(minecraft.player.getItemBySlot(EquipmentSlot.HEAD))) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof TileModifiedMatrix matrix)) {
            return;
        }
        TileVat vat = matrix.getVat();
        if (vat == null) {
            return;
        }
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + TOP_OFFSET;
        int step = minecraft.font.lineHeight + 1;
        TileVat.StabilityBand band = vat.stabilityBand();
        y = line(graphics, minecraft, Component.translatable(band.translationKey())
                .withStyle(colour(band), ChatFormatting.BOLD), x, y, step);
        if (vat.getFluxStocked() > 0.0F) {
            y = line(graphics, minecraft, Component.translatable("isorropia.vat.flux", number(vat.getFluxStocked()))
                    .withStyle(ChatFormatting.DARK_PURPLE), x, y, step);
        }
        if (vat.getCelestialAura() > 0.0F || vat.getCelestialAuraNeeded() > 0) {
            y = line(graphics, minecraft, Component.translatable("isorropia.vat.celestial_aura",
                    number(vat.getCelestialAura()), vat.getCelestialAuraNeeded())
                    .withStyle(style -> style.withColor(AURA_COLOUR)), x, y, step);
        }
        y = line(graphics, minecraft, Component.translatable("isorropia.vat.stability_gain",
                number(vat.getSymmetryStability())).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), x, y, step);
        float loss = vat.lossPerCycle();
        if (loss != 0.0F) {
            line(graphics, minecraft, Component.translatable("isorropia.vat.stability_loss", number(loss))
                    .withStyle(ChatFormatting.RED), x, y, step);
        }
    }

    private static int line(GuiGraphics graphics, Minecraft minecraft, Component text, int x, int y, int step) {
        graphics.drawCenteredString(minecraft.font, text, x, y, 0xFFFFFF);
        return y + step;
    }

    private static ChatFormatting colour(TileVat.StabilityBand band) {
        return switch (band) {
            case VERY_STABLE -> ChatFormatting.GREEN;
            case STABLE -> ChatFormatting.DARK_GREEN;
            case UNSTABLE -> ChatFormatting.GOLD;
            case VERY_UNSTABLE -> ChatFormatting.RED;
        };
    }

    /** At most two decimals and no trailing zeros, as upstream's formatter printed them. */
    private static String number(float value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}

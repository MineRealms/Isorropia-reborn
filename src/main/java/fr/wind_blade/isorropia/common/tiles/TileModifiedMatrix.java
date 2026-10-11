package fr.wind_blade.isorropia.common.tiles;

import dev.tc4port.thaumcraft.api.aspect.AspectAmounts;
import dev.tc4port.thaumcraft.api.aspect.AspectContainerView;
import fr.wind_blade.isorropia.common.sounds.SoundsIS;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The runic matrix adapted to the curative vat: it sits on top of the vat, shows what the vat holds through
 * goggles, and is where a wand switches the vat on and starts an infusion.
 */
public class TileModifiedMatrix extends BlockEntity implements AspectContainerView {
    public TileModifiedMatrix(BlockPos pos, BlockState state) {
        super(TilesIS.MODIFIED_MATRIX.get(), pos, state);
    }

    @Nullable
    public TileVat getVat() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof TileVat vat
                && vat.isMaster() ? vat : null;
    }

    /**
     * A wand right-click, as upstream: the first switches the vat on; after that, sneaking vents its stored flux
     * and anything else takes stock of the stabilisers and starts an infusion from the pedestals. A tap that
     * finds nothing to infuse does nothing, and the vat stays on.
     */
    public boolean onWandUse(Player player) {
        TileVat vat = getVat();
        if (vat == null || level == null) {
            return false;
        }
        if (!vat.isActive()) {
            vat.setActive(true);
            level.playSound(null, worldPosition, SoundsIS.CURATIVE_INFUSION_START.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            return true;
        }
        if (vat.isInfusing() || vat.isExpunging()) {
            return false;
        }
        if (player.isShiftKeyDown() && vat.getFluxStocked() > 0.0F) {
            vat.ventFlux();
            return true;
        }
        vat.surveyStabilizers();
        return vat.startInfusion(player);
    }

    @Override
    public AspectAmounts visibleAspects() {
        TileVat vat = getVat();
        return vat == null ? new AspectAmounts(Map.of()) : vat.visibleAspects();
    }
}

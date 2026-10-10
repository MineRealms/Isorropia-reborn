package fr.wind_blade.isorropia.common.vat;

import fr.wind_blade.isorropia.common.blocks.BlockCurativeVat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The vat's shape is pure geometry, so it is worth pinning down without a game. */
class VatStructureTest {
    @Test
    void theTopAndBottomLayersAreWoodRingsAroundConstructs() {
        for (int dy : new int[] {0, -3}) {
            assertEquals(VatStructure.Part.CONSTRUCT, VatStructure.partAt(0, dy, 0));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    assertEquals(VatStructure.Part.WOOD, VatStructure.partAt(dx, dy, dz));
                }
            }
        }
    }

    @Test
    void theMiddleLayersAreGlassAroundWater() {
        for (int dy : new int[] {-1, -2}) {
            assertEquals(VatStructure.Part.WATER, VatStructure.partAt(0, dy, 0));
            assertEquals(VatStructure.Part.GLASS, VatStructure.partAt(1, dy, 0));
            assertEquals(VatStructure.Part.GLASS, VatStructure.partAt(1, dy, 1));
        }
    }

    @Test
    void onlyTheTopCentreIsTheMaster() {
        assertEquals(BlockCurativeVat.Type.TOP, VatStructure.typeAt(0, 0, 0));
        assertEquals(BlockCurativeVat.Type.BOTTOM, VatStructure.typeAt(0, -3, 0));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    assertEquals(BlockCurativeVat.Type.PLACEHOLDER, VatStructure.typeAt(dx, 0, dz));
                }
            }
        }
    }

    @Test
    void theFourBottomEdgeMidpointsAreTheEssentiaPorts() {
        assertEquals(BlockCurativeVat.Type.CONNECTOR, VatStructure.typeAt(0, -2, 1));
        assertEquals(BlockCurativeVat.Type.CONNECTOR, VatStructure.typeAt(1, -2, 0));
        assertEquals(BlockCurativeVat.Type.CONNECTOR, VatStructure.typeAt(0, -2, -1));
        assertEquals(BlockCurativeVat.Type.CONNECTOR, VatStructure.typeAt(-1, -2, 0));
        // The corners of the same layer are plain glass, and so are the ports one layer up.
        assertEquals(BlockCurativeVat.Type.GLASS, VatStructure.typeAt(1, -2, 1));
        assertEquals(BlockCurativeVat.Type.GLASS, VatStructure.typeAt(0, -1, 1));
    }

    @Test
    void everyPositionTurnsBackIntoWhatItWasBuiltFrom() {
        for (int dy = 0; dy > -VatStructure.HEIGHT; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    VatStructure.Part part = VatStructure.partFor(new net.minecraft.core.BlockPos(0, 0, 0),
                            new net.minecraft.core.BlockPos(dx, dy, dz));
                    assertEquals(VatStructure.partAt(dx, dy, dz), part);
                }
            }
        }
    }
}

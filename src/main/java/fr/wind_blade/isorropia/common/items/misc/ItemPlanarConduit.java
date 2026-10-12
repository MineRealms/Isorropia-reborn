package fr.wind_blade.isorropia.common.items.misc;

import net.minecraft.world.item.Item;

/**
 * Thaumic Horizons' planar conduit: an ender pearl shattered into a tesseract, filled with wisp
 * essence and cased in void metal. In TH it was a bare Item with a custom icon; the 1.20.1 texture
 * comes from the model JSON, so this only needs to be a plain item.
 */
public class ItemPlanarConduit extends Item {
    public ItemPlanarConduit() {
        super(new Properties());
    }
}

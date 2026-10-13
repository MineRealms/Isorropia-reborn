package fr.wind_blade.isorropia.common.items.misc;

import net.minecraft.world.item.Item;

/**
 * Thaumic Horizons' blood syringe: the syringe a wizard fills with their own blood, which the Seroconverter then
 * treats with essentia. TH gave this class no behaviour of its own, only two damage values - the blood syringe and a
 * second, inert "Blast Phial" that TH's blast-phial recipe produced and nothing could ever use. The blast phial that
 * TH's own syringe entity and dispenser actually threw was the phial form of {@link ItemSyringeInjection}, so that is
 * where the blast phial lives in this port, and this item stays what it always was: an ingredient and a research icon.
 */
public class ItemSyringeBlood extends Item {
    public ItemSyringeBlood(Properties properties) {
        super(properties);
    }
}

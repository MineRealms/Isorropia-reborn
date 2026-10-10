package fr.wind_blade.isorropia.common.items;

import dev.tc4port.thaumcraft.api.aspect.AspectId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AspectNbtTest {
    private static final AspectId ORDO = AspectId.parse("ordo");
    private static final AspectId GULA = AspectId.parse("isorropia:gula");

    @Test
    void roundTripKeepsOrderAndNamespaces() {
        Map<AspectId, Integer> aspects = new LinkedHashMap<>();
        aspects.put(GULA, 5);
        aspects.put(ORDO, 2);
        CompoundTag tag = new CompoundTag();
        AspectNbt.write(tag, aspects);

        ListTag list = tag.getList(AspectNbt.ASPECTS_TAG, Tag.TAG_COMPOUND);
        assertEquals("isorropia:gula", list.getCompound(0).getString("key"));
        assertEquals("ordo", list.getCompound(1).getString("key"));
        assertEquals(aspects, AspectNbt.read(tag));
        assertEquals(GULA, AspectNbt.read(tag).keySet().iterator().next());
    }

    @Test
    void survivesStringSerialisation() throws Exception {
        CompoundTag tag = new CompoundTag();
        AspectNbt.write(tag, Map.of(ORDO, 5));
        CompoundTag parsed = net.minecraft.nbt.TagParser.parseTag(tag.toString());
        assertEquals(Map.of(ORDO, 5), AspectNbt.read(parsed));
    }

    @Test
    void ignoresMalformedEntries() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag bad = new CompoundTag();
        bad.putString("key", "Not A Valid Id!");
        bad.putInt("amount", 3);
        list.add(bad);
        CompoundTag empty = new CompoundTag();
        empty.putString("key", "ordo");
        empty.putInt("amount", 0);
        list.add(empty);
        tag.put(AspectNbt.ASPECTS_TAG, list);
        assertTrue(AspectNbt.read(tag).isEmpty());
        assertTrue(AspectNbt.read(new CompoundTag()).isEmpty());
    }
}

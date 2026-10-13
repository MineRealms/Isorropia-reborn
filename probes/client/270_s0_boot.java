// S0 boot gate: the ModDevGradle-built client joined the probe world and the mod is live.
// Run via `python tools/client_probe.py start` (world iso-probe), then exec this file.
var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK.keySet().stream().filter(k -> k.getNamespace().equals("isorropia")).count();
var items = net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream().filter(k -> k.getNamespace().equals("isorropia")).count();
var beTypes = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet().stream().filter(k -> k.getNamespace().equals("isorropia")).count();
var entityTypes = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(k -> k.getNamespace().equals("isorropia")).count();
if (blocks == 0 || items == 0 || beTypes == 0 || entityTypes == 0)
    return "FAIL registrations incomplete: blocks=" + blocks + " items=" + items + " beTypes=" + beTypes + " entityTypes=" + entityTypes;
return "OK level=" + mc.level.dimension().location() + " blocks=" + blocks + " items=" + items + " beTypes=" + beTypes + " entityTypes=" + entityTypes;

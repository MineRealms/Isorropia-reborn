// Compares the block-state query with the placed-block query the original used (the block's item aspects).
var sb = new StringBuilder();
String[] names = {"minecraft:grass_block", "minecraft:dirt", "minecraft:stone", "minecraft:oak_log",
        "minecraft:crafting_table", "minecraft:iron_ore", "minecraft:coal_ore"};
for (String name : names) {
    var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(name));
    var state = block.defaultBlockState();
    var stack = new net.minecraft.world.item.ItemStack(block);
    sb.append("\n  ").append(name);
    sb.append("\n     block()      = ").append(dev.tc4port.thaumcraft.api.aspect.AspectQueryApi.block(state).amounts());
    sb.append("\n     placedBlock()= ").append(dev.tc4port.thaumcraft.api.aspect.AspectQueryApi.placedBlock(stack, state).amounts());
    sb.append("\n     item()       = ").append(dev.tc4port.thaumcraft.api.aspect.AspectQueryApi.item(stack).amounts());
}
return sb.toString();

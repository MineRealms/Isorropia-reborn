// Illumination focus, part 2 (run a few seconds after 61): did the click leave a mote on the wall's face, is it
// the light block, what colour and light level does it carry, and did the crucible recipe load?
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var registry = net.minecraftforge.registries.ForgeRegistries.BLOCKS
        .getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "light"));
sb.append("block_registered=").append(registry != null);
sb.append(" item_registered=").append(net.minecraftforge.registries.ForgeRegistries.ITEMS
        .containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_illumination")));

// The mote should sit in the air block in front of the wall (z + 2), at foot or head height.
var found = new StringBuilder();
for (int dy = 0; dy <= 2; dy++) {
    var pos = new net.minecraft.core.BlockPos(40, 310 + dy, 42);
    var state = level.getBlockState(pos);
    if (state.is(registry)) {
        found.append(" mote@").append(pos.getY())
             .append(" colour=").append(state.getValue(fr.wind_blade.isorropia.common.blocks.BlockLight.COLOUR))
             .append(" solar=").append(state.getValue(fr.wind_blade.isorropia.common.blocks.BlockLight.SOLAR))
             .append(" emission=").append(state.getLightEmission(level, pos));
    }
}
sb.append(" found=").append(found.length() == 0 ? "NONE" : found.toString());

// Did the crucible recipe for the focus load?
var recipes = server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "crucible/focus_illumination"));
sb.append(" recipe=").append(recipes.isPresent());

// And is the focus profile in the synchronized catalog?
var profile = dev.tc4port.thaumcraft.api.focus.FocusApi.registry()
        .profile(dev.tc4port.thaumcraft.api.focus.FocusId.of(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_illumination")));
sb.append(" profile=").append(profile.isPresent());
profile.ifPresent(p -> sb.append(" base_cost=").append(p.baseCost()));
return sb.toString();

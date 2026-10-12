// Liquefaction focus, part 2 (a few seconds after 63): the ice block should have thawed to water. Confirms the
// item, recipe, profile and both custom upgrades loaded, then swaps the target for an iron ore and keeps the beam
// held so 65 can check the smelt.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

sb.append("item=").append(net.minecraftforge.registries.ForgeRegistries.ITEMS
        .containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_liquefaction")));
sb.append(" recipe=").append(server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/focus_liquefaction")).isPresent());

var profile = dev.tc4port.thaumcraft.api.focus.FocusApi.registry()
        .profile(dev.tc4port.thaumcraft.api.focus.FocusId.of(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_liquefaction")));
sb.append(" profile=").append(profile.isPresent());
profile.ifPresent(p -> sb.append(" base_cost=").append(p.baseCost())
        .append(" action_costs=").append(p.actionCosts()));

var registry = dev.tc4port.thaumcraft.api.focus.FocusApi.registry();
sb.append(" nuggets=").append(registry.upgrade(dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "nuggets"))).isPresent());
sb.append(" purity=").append(registry.upgrade(dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "purity"))).isPresent());

var target = new net.minecraft.core.BlockPos(40, 311, 43);
var state = level.getBlockState(target);
sb.append(" target_block=").append(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()));

// Swap in an iron ore and leave the beam held so 65 can see the smelt.
level.setBlock(target, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 3);
return sb.toString();

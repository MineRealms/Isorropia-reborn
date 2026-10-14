// Disintegration focus, part 3: confirms the item, the infusion recipe, the profile, its base and critter costs,
// its upgrade ranks and the enervation upgrade all loaded.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();

sb.append("item=").append(net.minecraft.core.registries.BuiltInRegistries.ITEM
        .containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_disintegration")));
sb.append(" recipe=").append(server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/focus_disintegration")).isPresent());

var profile = dev.tc4port.thaumcraft.api.focus.FocusApi.registry()
        .profile(dev.tc4port.thaumcraft.api.focus.FocusId.of(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_disintegration")));
sb.append(" profile=").append(profile.isPresent());
profile.ifPresent(p -> sb.append(" base_cost=").append(p.baseCost())
        .append(" action_costs=").append(p.actionCosts())
        .append(" ranks=").append(p.ranks()));

var registry = dev.tc4port.thaumcraft.api.focus.FocusApi.registry();
sb.append(" enervation=").append(registry.upgrade(dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "enervation"))).isPresent());
return sb.toString();

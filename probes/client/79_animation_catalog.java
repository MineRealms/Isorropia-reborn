// Animation focus, part 3: confirms the item, the infusion recipe, the profile, its six-primal base cost, its
// upgrade ranks, the duration parameter and the berserk/detonation upgrades all loaded.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var sb = new StringBuilder();

sb.append("item=").append(net.minecraftforge.registries.ForgeRegistries.ITEMS
        .containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_animation")));
sb.append(" recipe=").append(server.getRecipeManager().byKey(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "infusion/focus_animation")).isPresent());

var profile = dev.tc4port.thaumcraft.api.focus.FocusApi.registry()
        .profile(dev.tc4port.thaumcraft.api.focus.FocusId.of(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "focus_animation")));
sb.append(" profile=").append(profile.isPresent());
profile.ifPresent(p -> sb.append(" base_cost=").append(p.baseCost())
        .append(" ranks=").append(p.ranks())
        .append(" duration=").append(p.parameters().minimum("duration"))
        .append("..").append(p.parameters().maximum("duration")));

var registry = dev.tc4port.thaumcraft.api.focus.FocusApi.registry();
sb.append(" berserk=").append(registry.upgrade(dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "berserk"))).isPresent());
sb.append(" detonation=").append(registry.upgrade(dev.tc4port.thaumcraft.api.focus.FocusUpgradeId.of(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "detonation"))).isPresent());
return sb.toString();

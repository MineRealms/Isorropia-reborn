// Illumination focus dyes: a fresh focus is TH's yellow, red dye makes it TH's red (palette 1), the last of several
// dyes wins, and a stray item or a second focus spoils the recipe, as in TH's RecipesFocusIlluminationDyes.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var focusItem = fr.wind_blade.isorropia.common.items.ItemsIS.FOCUS_ILLUMINATION.get();
var grid = new net.minecraft.world.inventory.TransientCraftingContainer(
        new net.minecraft.world.inventory.CraftingMenu(-1, player.getInventory()), 3, 3);
java.util.function.Function<net.minecraft.world.item.ItemStack[], net.minecraft.world.item.ItemStack> craft = items -> {
    grid.clearContent();
    for (int i = 0; i < items.length; i++) grid.setItem(i, items[i]);
    return server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, level)
            .map(recipe -> recipe.assemble(grid, level.registryAccess()))
            .orElse(net.minecraft.world.item.ItemStack.EMPTY);
};
var fresh = new net.minecraft.world.item.ItemStack(focusItem);
var red = craft.apply(new net.minecraft.world.item.ItemStack[] {fresh.copy(),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE)});
var lastWins = craft.apply(new net.minecraft.world.item.ItemStack[] {
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_DYE), fresh.copy(),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHITE_DYE)});
var stray = craft.apply(new net.minecraft.world.item.ItemStack[] {fresh.copy(),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT)});
var twoFoci = craft.apply(new net.minecraft.world.item.ItemStack[] {fresh.copy(), fresh.copy(),
        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE)});
var redName = red.isEmpty() ? "none"
        : ((net.minecraft.network.chat.contents.TranslatableContents) red.getHoverName().getContents()).getKey();
return "defaultColour=" + fr.wind_blade.isorropia.common.items.ItemFocusIllumination.colour(fresh)
        + " redColour=" + (red.isEmpty() ? -1 : fr.wind_blade.isorropia.common.items.ItemFocusIllumination.colour(red))
        + " redName=" + redName
        + " lastWins=" + (lastWins.isEmpty() ? -1 : fr.wind_blade.isorropia.common.items.ItemFocusIllumination.colour(lastWins))
        + " strayRejected=" + stray.isEmpty() + " twoFociRejected=" + twoFoci.isEmpty();

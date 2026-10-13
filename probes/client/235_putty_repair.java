// Void putty repair (TH's RecipeVoidPuttyRepair): one damaged, repairable item plus void putty on the crafting grid
// repairs the item outright, whatever it is; anything else in the grid invalidates the recipe.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();
try {
    var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, 0) {
        public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        public boolean stillValid(net.minecraft.world.entity.player.Player p) {
            return true;
        }
    };
    var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
    var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    sword.setDamageValue(100);
    var putty = new net.minecraft.world.item.ItemStack(fr.wind_blade.isorropia.common.items.ItemsIS.VOID_PUTTY.get(), 2);
    var jav = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SALMON);
    var type = net.minecraft.world.item.crafting.RecipeType.CRAFTING;
    var access = level.registryAccess();

    grid.setItem(0, sword);
    grid.setItem(1, putty);
    var hit = server.getRecipeManager().getRecipeFor(type, grid, level);
    sb.append("matches=").append(hit.isPresent());
    hit.ifPresent(r -> {
        var out = r.assemble(grid, access);
        sb.append(" out=").append(out.getItem()).append(" dmg=").append(out.getDamageValue());
    });
    sb.append("\n");

    // A damaged item with no putty must not match.
    grid.clearContent();
    grid.setItem(0, sword);
    sb.append("noPutty=").append(server.getRecipeManager().getRecipeFor(type, grid, level).isPresent()).append("\n");

    // Putty alone must not match.
    grid.clearContent();
    grid.setItem(0, putty);
    sb.append("onlyPutty=").append(server.getRecipeManager().getRecipeFor(type, grid, level).isPresent()).append("\n");

    // An undamaged item plus putty must not match.
    grid.clearContent();
    var fresh = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
    grid.setItem(0, fresh);
    grid.setItem(1, putty);
    sb.append("undamaged=").append(server.getRecipeManager().getRecipeFor(type, grid, level).isPresent()).append("\n");

    // A stranger item in the grid must not match.
    grid.clearContent();
    grid.setItem(0, sword);
    grid.setItem(1, putty);
    grid.setItem(2, jav);
    sb.append("stranger=").append(server.getRecipeManager().getRecipeFor(type, grid, level).isPresent()).append("\n");
    return sb.toString();
} catch (Throwable t) {
    var sw = new java.io.StringWriter();
    t.printStackTrace(new java.io.PrintWriter(sw));
    return sb + "\nEXC: " + sw.toString();
}

// Seroconverter, part 2: redial a mix of three aspects on the pad machine and open its screen for a shot.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
var infuserPos = net.minecraft.core.BlockPos.containing(128, 311, 100);
var infuser = (fr.wind_blade.isorropia.common.tiles.TileBloodInfuser) level.getBlockEntity(infuserPos);
// A fresh syringe and a three-aspect dial, still idle until the screen's mode dot is pressed.
infuser.container().setItem(0, new net.minecraft.world.item.ItemStack(
        fr.wind_blade.isorropia.common.items.ItemsIS.SYRINGE_BLOOD.get()));
var victus = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:victus");
var ignis = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:ignis");
var aer = dev.tc4port.thaumcraft.api.aspect.AspectId.parse("thaumcraft:aer");
infuser.addSelected(victus);
infuser.addSelected(ignis);
infuser.addSelected(ignis);
infuser.addSelected(aer);
// Grant every aspect to the opener so the grid shows a full page.
var knowledge = dev.tc4port.thaumcraft.state.TCObjectState.PLAYER_KNOWLEDGE.get(player);
for (var entry : dev.tc4port.thaumcraft.api.aspect.AspectApi.definitions().entrySet()) {
    knowledge.discoverAspect(entry.getKey(), 16);
}
sb.append("dial=").append(infuser.dialled().size());
// Open the screen from the client side as a real right-click would.
var hit = new net.minecraft.world.phys.BlockHitResult(
        new net.minecraft.world.phys.Vec3(128.5, 311.5, 100.5), net.minecraft.core.Direction.NORTH, infuserPos, false);
var result = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
sb.append(" use=").append(result);
return sb.toString();

// Presses the lens key and the morphic fingers key the way the keyboard does, and reports what each opened.
// Each key must also be registered exactly once: a handler clash once registered the lens key twice and the
// fingers key never.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var player = server.getPlayerList().getPlayer(mc.player.getUUID());
var sb = new StringBuilder();
for (var name : java.util.List.of("key.isorropia.lens", "key.isorropia.fingers")) {
    sb.append(name).append(" x").append(java.util.Arrays.stream(mc.options.keyMappings)
            .filter(k -> k.getName().equals(name)).count()).append("  ");
}

// Goggles for the lens key, grown fingers for the fingers key.
player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation("thaumcraft", "goggles_of_revealing"))));
var data = fr.wind_blade.isorropia.common.capabilities.LivingData.get(player).orElseThrow();
data.addSelfInfusion(fr.wind_blade.isorropia.common.infusion.SelfInfusions.MORPHIC_FINGERS);
data.sync();
// The server cannot sync the goggles while this probe holds its thread, so the client's copy is set as well.
var goggles = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy();
mc.execute(() -> {
    mc.setScreen(null);
    mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, goggles);
});
Thread.sleep(600);

// A backgrounded client renders slowly, so each step waits for its outcome instead of a fixed time: a press and
// a release queued into the same frame would never be seen by a tick.
java.util.function.BooleanSupplier wheelOpen = fr.wind_blade.isorropia.client.hud.LensWheel::isOpen;
var lens = fr.wind_blade.isorropia.client.LensKeys.LENS.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.set(lens, true));
long until = System.currentTimeMillis() + 5000;
while (!wheelOpen.getAsBoolean() && System.currentTimeMillis() < until) Thread.sleep(50);
sb.append("\nJ held: wheel=").append(wheelOpen.getAsBoolean());
mc.execute(() -> net.minecraft.client.KeyMapping.set(lens, false));
until = System.currentTimeMillis() + 5000;
while (wheelOpen.getAsBoolean() && System.currentTimeMillis() < until) Thread.sleep(50);
sb.append("  J released: wheel=").append(wheelOpen.getAsBoolean());

// Tapping M asks the server for the portable workbench. Probe bodies run on the server thread, so the server
// cannot answer until this probe returns: run 50_fingers_key_check afterwards to see the screen.
var fingers = fr.wind_blade.isorropia.client.MorphicKeys.FINGERS.getKey();
mc.execute(() -> net.minecraft.client.KeyMapping.click(fingers));
sb.append("\nM tapped; now run 50_fingers_key_check");
return sb.toString();

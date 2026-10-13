// Confirm the Seroconverter screen is open.
var mc = net.minecraft.client.Minecraft.getInstance();
return "screen=" + (mc.screen == null ? "null" : mc.screen.getClass().getName());

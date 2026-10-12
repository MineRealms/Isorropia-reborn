// Removes the illumination motes 61 leaves on the focus platform: they sit in the line of the later beams and
// the ray stops on their outline, so the liquefaction and disintegration probes would hit a mote, not their target.
var mc = net.minecraft.client.Minecraft.getInstance();
var level = mc.getSingleplayerServer().overworld();
int cleared = 0;
for (var pos : net.minecraft.core.BlockPos.betweenClosed(30, 305, 30, 50, 318, 50)) {
    if (net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock()).toString().equals("isorropia:light")) {
        level.setBlockAndUpdate(pos.immutable(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        cleared++;
    }
}
return "motes cleared=" + cleared;

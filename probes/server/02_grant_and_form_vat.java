// Grant the probe player the vat research, then build and form a vat at a known spot high above the world.
StringBuilder sb = new StringBuilder();
var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
var level = server.overworld();
var fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);

// The gateway has to be opened with a real context; the admin one is what the command uses.
var context = new dev.tc4port.thaumcraft.api.research.ResearchCompletionContext(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("isorropia", "probe"),
        dev.tc4port.thaumcraft.api.research.ResearchCompletionReason.ADMIN_COMMAND);

var key = dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:creatureinfusions");
var vatKey = dev.tc4port.thaumcraft.api.research.ResearchKey.parse("isorropia:curativevat");
for (var k : new dev.tc4port.thaumcraft.api.research.ResearchKey[]{key, vatKey}) {
    try {
        var result = dev.tc4port.thaumcraft.api.research.ResearchApi.complete(fake, k, context);
        sb.append(System.lineSeparator()).append("complete(").append(k).append(")=").append(result)
          .append(" isComplete=").append(dev.tc4port.thaumcraft.api.research.ResearchApi.isComplete(fake, k));
    } catch (Throwable t) {
        sb.append(System.lineSeparator()).append("complete(").append(k).append(") threw ").append(t).append(" / cause=").append(t.getCause());
    }
}

// Build the vat frame by hand where the probe can always find it, then form it.
var probesDir = new java.io.File("rosetta_probes");
probesDir.mkdirs();
var originFile = new java.io.File(probesDir, "vat_origin.txt");
net.minecraft.core.BlockPos master;
if (originFile.exists()) {
    var parts = new String(java.nio.file.Files.readAllBytes(originFile.toPath())).trim().split(",");
    master = new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
} else {
    master = new net.minecraft.core.BlockPos(64, level.getMaxBuildHeight() - 20, 64);
    java.nio.file.Files.write(originFile.toPath(), (master.getX() + "," + master.getY() + "," + master.getZ()).getBytes());
}
// Clear the box first so the probe is repeatable.
for (int dy = 0; dy > -fr.wind_blade.isorropia.common.vat.VatStructure.HEIGHT; dy--) {
    for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
            level.setBlockAndUpdate(master.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
    }
}
var planks = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("greatwood_planks");
var construct = dev.tc4port.thaumcraft.api.ThaumcraftContent.block("alchemical_construct");
for (int dy = 0; dy > -fr.wind_blade.isorropia.common.vat.VatStructure.HEIGHT; dy--) {
    for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
            var part = fr.wind_blade.isorropia.common.vat.VatStructure.partAt(dx, dy, dz);
            var state = switch (part) {
                case WOOD -> planks.defaultBlockState();
                case CONSTRUCT -> construct.defaultBlockState();
                case GLASS -> net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState();
                case WATER -> net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
            };
            level.setBlockAndUpdate(master.offset(dx, dy, dz), state);
        }
    }
}
sb.append("\nmatchesUnformed=").append(fr.wind_blade.isorropia.common.vat.VatStructure.matchesUnformed(level, master));
fr.wind_blade.isorropia.common.vat.VatStructure.form(level, master);
sb.append("\nmaster=").append(master).append(" state=").append(level.getBlockState(master).getBlock().toString().replace("Block{", "").split("\\[")[0])
  .append(" variant=").append(level.getBlockState(master).getValue(fr.wind_blade.isorropia.common.blocks.BlockCurativeVat.VARIANT));
sb.append("\ninterior=").append(level.getBlockState(master.below()).getBlock() == fr.wind_blade.isorropia.common.blocks.BlocksIS.VAT_INTERIOR.get());
sb.append("\nconnector=").append(level.getBlockState(master.offset(0, -2, 1)).getValue(fr.wind_blade.isorropia.common.blocks.BlockCurativeVat.VARIANT));
sb.append("\nbe=").append(level.getBlockEntity(master));
var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) level.getBlockEntity(master);
sb.append("\nisMaster=").append(vat.isMaster()).append(" mode=").append(vat.getMode()).append(" contained=").append(vat.getContained());
sb.append("\nstoredAspects=").append(vat.visibleAspects().amounts());
return sb.toString();

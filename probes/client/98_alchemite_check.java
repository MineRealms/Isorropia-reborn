// Alchemite, part 2: the blast should have shattered the stone into Terra crystallized essentia instead of
// dropping stone items, and the alchemite block itself should be gone.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var centre = new net.minecraft.core.BlockPos(100, 310, 170);
int stoneLeft = 0;
for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
    if (level.getBlockState(centre.offset(dx, dy, dz)).is(net.minecraft.world.level.block.Blocks.STONE)) {
        stoneLeft++;
    }
}
sb.append("stoneLeft=").append(stoneLeft)
  .append(" alchemiteLeft=").append(level.getBlockState(centre)
          .is(fr.wind_blade.isorropia.common.blocks.BlocksIS.ALCHEMITE.get()));

// Every node in the blast should have been scarred: gone, fading, or unstable — never still a plain node.
int plainNodes = 0;
int nodesLeft = 0;
for (int dx = -2; dx <= 2; dx += 2) for (int dz = -2; dz <= 2; dz += 2) {
    if (dx == 0 && dz == 0) {
        continue;
    }
    var state = level.getBlockState(centre.offset(dx, 0, dz));
    if (!state.is(dev.tc4port.thaumcraft.api.ThaumcraftContent.block("aura_node"))) {
        continue;
    }
    nodesLeft++;
    if (level.getBlockEntity(centre.offset(dx, 0, dz)) instanceof dev.tc4port.thaumcraft.api.node.AuraNodeView node) {
        var ns = node.nodeState();
        if (ns.type().equals(dev.tc4port.thaumcraft.api.node.NodeTypeId.NORMAL)
                && ns.modifier().equals(dev.tc4port.thaumcraft.api.node.NodeModifierId.NONE)) {
            plainNodes++;
        }
    }
}
sb.append(" nodesLeft=").append(nodesLeft).append(" plainNodes=").append(plainNodes);

int stoneItems = 0;
boolean essence = false;
var found = new StringBuilder();
for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
        new net.minecraft.world.phys.AABB(centre.getX() - 8, centre.getY() - 4, centre.getZ() - 8,
                centre.getX() + 9, centre.getY() + 7, centre.getZ() + 9))) {
    var stack = item.getItem();
    var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
    var aspects = dev.tc4port.thaumcraft.api.aspect.AspectQueryApi.item(stack).amounts();
    found.append(" ").append(key).append("x").append(stack.getCount()).append("(").append(aspects).append(")");
    if (key.getPath().equals("stone")) {
        stoneItems += stack.getCount();
    }
    if (key.getPath().equals("crystal_essence") && !aspects.isEmpty()) {
        essence = true;
    }
}
sb.append(" stoneItems=").append(stoneItems).append(" essence=").append(essence);
sb.append(" drops=").append(found.length() == 0 ? "NONE" : found.toString());
return sb.toString();

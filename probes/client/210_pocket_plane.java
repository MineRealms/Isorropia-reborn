// Pocket plane skeleton: the datapack dimension exists and is void, a marker block sticks at the plane anchor, the
// plane registry stores and reads back, and an entity can be sent into the plane and returned.
var mc = net.minecraft.client.Minecraft.getInstance();
var server = mc.getSingleplayerServer();
var level = server.overworld();
var sb = new StringBuilder();

var pocket = fr.wind_blade.isorropia.common.world.PocketPlane.level(server);
sb.append("pocketLevelPresent=").append(pocket != null).append("\n");
if (pocket == null) {
    return sb.toString();
}
sb.append("pocketKey=").append(pocket.dimension().location()).append("\n");
sb.append("pocketMinY=").append(pocket.getMinBuildHeight()).append("\n");
sb.append("pocketMaxY=").append(pocket.getMaxBuildHeight()).append("\n");

var anchor = fr.wind_blade.isorropia.common.world.PocketPlane.anchor(0);
sb.append("anchor=").append(anchor).append("\n");
sb.append("anchorIsVoid=").append(pocket.getBlockState(anchor).isAir()).append("\n");
sb.append("column1IsVoid=").append(pocket.getBlockState(fr.wind_blade.isorropia.common.world.PocketPlane.anchor(1))
        .isAir()).append("\n");

pocket.setBlockAndUpdate(anchor, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
pocket.setBlockAndUpdate(fr.wind_blade.isorropia.common.world.PocketPlane.exit(0),
        net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
sb.append("markerPersisted=").append(pocket.getBlockState(anchor)
        .is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)).append("\n");
sb.append("exitPersisted=").append(pocket.getBlockState(fr.wind_blade.isorropia.common.world.PocketPlane.exit(0))
        .is(net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK)).append("\n");

var aer = dev.tc4port.thaumcraft.api.aspect.VisChannel.AER.aspectId();
var store = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server);
int id = store.reserve("Probe Plane", java.util.Map.of(aer, 32), net.minecraft.world.level.Level.OVERWORLD, anchor, 64);
sb.append("reservedId=").append(id).append("\n");
sb.append("storeCount=").append(store.planeCount()).append("\n");
var reread = fr.wind_blade.isorropia.common.world.PocketPlaneStore.get(server).plane(id);
sb.append("storeReRead=").append(reread != null).append("\n");
if (reread != null) {
    sb.append("storeAspects=").append(reread.aspects()).append("\n");
}

// An entity round trip proves the teleporter works both ways.
var pig = new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG, level);
pig.moveTo(0.5, 200.0, 0.5);
level.addFreshEntity(pig);
sb.append("pigStartDim=").append(pig.level().dimension().location()).append("\n");
var moved = fr.wind_blade.isorropia.common.world.PocketPlane.sendTo(pig,
        pocket, new net.minecraft.world.phys.Vec3(0.5, 129.0, 0.5));
sb.append("enteredPlane=").append(moved != null).append("\n");
if (moved != null) {
    sb.append("pigNowDim=").append(moved.level().dimension().location()).append("\n");
    sb.append("pigNowPos=").append(moved.blockPosition()).append("\n");
    var back = fr.wind_blade.isorropia.common.world.PocketPlane.sendTo(moved, level,
            new net.minecraft.world.phys.Vec3(0.5, 200.0, 0.5));
    sb.append("returnedHome=").append(back != null).append("\n");
    if (back != null) {
        sb.append("pigBackDim=").append(back.level().dimension().location()).append("\n");
        back.discard();
    }
}

pocket.setBlockAndUpdate(anchor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
pocket.setBlockAndUpdate(fr.wind_blade.isorropia.common.world.PocketPlane.exit(0),
        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
return sb.toString();

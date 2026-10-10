// Reports the block the crosshair hits, the block space above it, and what item entities sit there.
var mc = net.minecraft.client.Minecraft.getInstance();
var sb = new StringBuilder();
var p = mc.player;
sb.append("player=").append(String.format("%.2f,%.2f,%.2f", p.getX(), p.getY(), p.getZ()));
sb.append(" pitch=").append(p.getXRot());
sb.append("\n  hitType=").append(mc.hitResult == null ? "null" : mc.hitResult.getType());
if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
    var pos = hit.getBlockPos();
    sb.append("\n  hitBlockPos=").append(pos).append(" state=").append(mc.level.getBlockState(pos));
    sb.append("\n  above=").append(pos.above()).append(" state=").append(mc.level.getBlockState(pos.above()));
    var items = mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos.above()));
    sb.append("\n  itemsInAbove=").append(items.size());
    for (var i : items) {
        sb.append(" ").append(i.getItem()).append("@").append(String.format("%.2f,%.2f,%.2f", i.getX(), i.getY(), i.getZ()));
    }
}
var allItems = mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
        p.getBoundingBox().inflate(16.0), e -> true);
sb.append("\n  nearbyItems=").append(allItems.size());
for (var i : allItems) {
    sb.append(" ").append(i.getItem()).append("@").append(String.format("%.2f,%.2f,%.2f", i.getX(), i.getY(), i.getZ()));
}
return sb.toString();

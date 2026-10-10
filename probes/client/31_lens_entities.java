// Lists the living entities the kirlian lens would draw ripples for, with their distance from the camera.
var mc = net.minecraft.client.Minecraft.getInstance();
var done = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        var player = mc.player;
        var box = player.getBoundingBox().inflate(24.0);
        var list = player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box,
                e -> e != player);
        var sb = new StringBuilder("camera=" + String.format("%.1f,%.1f,%.1f", player.getX(), player.getY(), player.getZ()));
        for (var e : list) {
            sb.append("\n  ").append(e.getType().toShortString())
              .append(" pos=").append(String.format("%.2f,%.2f,%.2f", e.getX(), e.getY(), e.getZ()))
              .append(" old=").append(String.format("%.2f,%.2f,%.2f", e.xOld, e.yOld, e.zOld))
              .append(" wh=").append(String.format("%.2f/%.2f", e.getBbWidth(), e.getBbHeight()))
              .append(" dist=").append(String.format("%.2f", player.position().distanceTo(e.position())));
        }
        done.complete(sb.toString());
    } catch (Throwable t) {
        done.complete("FAILED " + t);
    }
});
return done.get(10, java.util.concurrent.TimeUnit.SECONDS);

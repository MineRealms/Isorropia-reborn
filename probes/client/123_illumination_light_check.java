// Illumination motes, part 2: count the client's wisp particles sitting on the motes from 122.
var mc = net.minecraft.client.Minecraft.getInstance();
var result = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        java.lang.reflect.Field field = null;
        for (var candidate : net.minecraft.client.particle.ParticleEngine.class.getDeclaredFields()) {
            if (java.util.Map.class.isAssignableFrom(candidate.getType())
                    && candidate.getGenericType().getTypeName().contains("Queue")) {
                field = candidate;
            }
        }
        field.setAccessible(true);
        var byType = (java.util.Map<?, ?>) field.get(mc.particleEngine);
        int near = 0;
        int clientTiles = 0;
        for (int i = 0; i < 5; i++) {
            if (mc.level.getBlockEntity(new net.minecraft.core.BlockPos(60 + i * 2, 311, 60))
                    instanceof fr.wind_blade.isorropia.common.tiles.TileLight) clientTiles++;
        }
        for (var queue : byType.values()) {
            for (var particle : (java.util.Collection<?>) queue) {
                var box = ((net.minecraft.client.particle.Particle) particle).getBoundingBox().getCenter();
                if (particle.getClass().getSimpleName().contains("Wisp") && box.y > 310.5 && box.y < 312.5
                        && box.z > 59.0 && box.z < 61.5 && box.x > 59.5 && box.x < 69.5) near++;
            }
        }
        result.complete("clientTiles=" + clientTiles + " wispsNear=" + (near > 0) + " wisps=" + near);
    } catch (Throwable t) {
        result.complete("FAILED " + t);
    }
});
return result.get(10, java.util.concurrent.TimeUnit.SECONDS);

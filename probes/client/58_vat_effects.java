// The 1.12 vat effects on the client, end to end through their real triggers, for the scene left by 56_matrix_flow:
// a vat drawing celestial aura rains motes into itself (RenderVat), a venting vat's matrix breathes a plume
// (RenderModifiedMatrix), and a vat tearing apart throws a flux explosion (the FLUX_BURST packet's handler).
// Pass one of "aura", "vent", "burst" by editing EFFECT; reports how many generic particles are alive afterwards.
var EFFECT = "aura";
var mc = net.minecraft.client.Minecraft.getInstance();
var master = new net.minecraft.core.BlockPos(64, 300, 64);
var done = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        var vat = (fr.wind_blade.isorropia.common.tiles.TileVat) mc.level.getBlockEntity(master);
        var cls = fr.wind_blade.isorropia.common.tiles.TileVat.class;
        var aura = cls.getDeclaredField("drawingAura");
        aura.setAccessible(true);
        var expunge = cls.getDeclaredField("expunge");
        expunge.setAccessible(true);
        aura.setBoolean(vat, EFFECT.equals("aura"));
        expunge.setBoolean(vat, EFFECT.equals("vent"));
        if (EFFECT.equals("burst")) {
            fr.wind_blade.isorropia.client.fx.VatFx.fluxExplosion(mc.level, 64.5, 301.5, 64.5);
        }
        done.complete("set " + EFFECT);
    } catch (Throwable t) {
        done.complete("FAILED " + t);
    }
});
var first = done.get(5, java.util.concurrent.TimeUnit.SECONDS);
Thread.sleep(1500);
var count = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        var field = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles");
        field.setAccessible(true);
        var map = (java.util.Map<?, ?>) field.get(mc.particleEngine);
        var additive = (java.util.Collection<?>) map.get(fr.wind_blade.isorropia.client.fx.GenericFx.ADDITIVE);
        var translucent = (java.util.Collection<?>) map.get(fr.wind_blade.isorropia.client.fx.GenericFx.TRANSLUCENT);
        count.complete("additive=" + (additive == null ? 0 : additive.size()) + " translucent="
                + (translucent == null ? 0 : translucent.size()));
    } catch (Throwable t) {
        count.complete("count FAILED " + t);
    }
});
return first + " -> " + count.get(5, java.util.concurrent.TimeUnit.SECONDS);

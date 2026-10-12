// Capture the actual BER's emitted vertices on the client, in addition to visually checking its screenshot.
var mc = net.minecraft.client.Minecraft.getInstance();
var result = new java.util.concurrent.CompletableFuture<String>();
mc.execute(() -> {
    try {
        var pos = new net.minecraft.core.BlockPos(200, 311, 103);
        var shard = (fr.wind_blade.isorropia.common.tiles.TileEtherealShard) mc.level.getBlockEntity(pos);
        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(shard);
        var data = new java.util.LinkedHashMap<String, int[]>();
        int[] pass = {0};
        net.minecraft.client.renderer.MultiBufferSource capture = type -> {
            // TC4R's shared billboard render types do not include their texture in toString().
            String kind = switch (pass[0]++) { case 0 -> "crystal"; case 1 -> "glyph"; default -> "beam"; };
            int[] counts = data.computeIfAbsent(kind, k -> new int[] {0, -1});
            return new com.mojang.blaze3d.vertex.VertexConsumer() {
                public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) { return this; }
                public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) {
                    counts[1] = r << 16 | g << 8 | b;
                    return this;
                }
                public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) { return this; }
                public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u, int v) { return this; }
                public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u, int v) { return this; }
                public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) { return this; }
                public void endVertex() { counts[0]++; }
                public void defaultColor(int r, int g, int b, int a) { }
                public void unsetDefaultColor() { }
            };
        };
        renderer.render(shard, 0.5F, new com.mojang.blaze3d.vertex.PoseStack(), capture,
                net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        var sb = new StringBuilder();
        data.forEach((kind, counts) -> sb.append(kind).append("Vertices=").append(counts[0])
                .append(" ").append(kind).append("Colour=").append(String.format("%06X", counts[1])).append(" "));
        result.complete(sb.toString());
    } catch (Throwable t) {
        result.complete("FAILED " + t);
    }
});
return result.get(10, java.util.concurrent.TimeUnit.SECONDS);

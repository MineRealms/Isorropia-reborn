package fr.wind_blade.isorropia.client.platform;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Immediate-mode position/texture/colour quads for screens. 1.21 reworked {@code BufferBuilder}: it is started by the
 * tesselator, vertices no longer need an explicit end, and the finished mesh is built rather than ended. Callers set
 * the shader, texture and blend state themselves, exactly as before.
 */
public final class QuadBatch {
    private final BufferBuilder buffer;
    private final Matrix4f pose;

    private QuadBatch(BufferBuilder buffer, Matrix4f pose) {
        this.buffer = buffer;
        this.pose = pose;
    }

    public static QuadBatch positionTexColor(Matrix4f pose) {
        //? if forge {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        //?} else {
        /*BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        *///?}
        return new QuadBatch(buffer, pose);
    }

    public QuadBatch vertex(float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
        //? if forge {
        this.buffer.vertex(this.pose, x, y, z).uv(u, v).color(red, green, blue, alpha).endVertex();
        //?} else {
        /*this.buffer.addVertex(this.pose, x, y, z).setUv(u, v).setColor(red, green, blue, alpha);
        *///?}
        return this;
    }

    public void draw() {
        //? if forge {
        BufferUploader.drawWithShader(this.buffer.end());
        //?} else {
        /*BufferUploader.drawWithShader(this.buffer.buildOrThrow());
        *///?}
    }
}

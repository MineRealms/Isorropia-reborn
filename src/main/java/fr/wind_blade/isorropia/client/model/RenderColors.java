package fr.wind_blade.isorropia.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The one place that knows how each loader pushes colour into a vertex buffer. 1.20.1 builds vertices with a
 * vertex/color/uv chain and hands models 0..1 float channels; 1.21.1 renamed the chain to addVertex/setColor and
 * models take one packed ARGB word instead. Renderers and models call these helpers with the 1.20.1 shapes and
 * stay identical on both loaders.
 */
public final class RenderColors {
    private RenderColors() {
    }

    /** Packs 0..1 float channels into the ARGB word the 1.21 model API takes. */
    public static int pack(float red, float green, float blue, float alpha) {
        return channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return Math.min(255, Math.max(0, (int) (value * 255.0F)));
    }

    /** Draws a whole model with per-channel tint, matching the 1.20.1 renderToBuffer shape. */
    public static void render(Model model, PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        //? if forge {
        model.renderToBuffer(poseStack, buffer, light, overlay, red, green, blue, alpha);
        //?} else {
        /*model.renderToBuffer(poseStack, buffer, light, overlay, pack(red, green, blue, alpha));
        *///?}
    }

    /** Draws one model part with per-channel tint, matching the 1.20.1 ModelPart.render shape. */
    public static void part(ModelPart part, PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        //? if forge {
        part.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        //?} else {
        /*part.render(poseStack, buffer, light, overlay, pack(red, green, blue, alpha));
        *///?}
    }

    /** Writes one vertex with 0..255 colour channels, the shape every billboard/beam helper uses. */
    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int overlay, int light, int red, int green, int blue, int alpha,
            float nx, float ny, float nz) {
        //? if forge {
        consumer.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
        //?} else {
        /*consumer.addVertex(pose.pose(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
        *///?}
    }

    /** Writes one vertex with 0..1 float colour channels, the shape the OBJ mesh emitter uses. */
    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int overlay, int light, float red, float green, float blue, float alpha,
            float nx, float ny, float nz) {
        //? if forge {
        consumer.vertex(pose.pose(), x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
        //?} else {
        /*consumer.addVertex(pose.pose(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
        *///?}
    }

    /** Writes one vertex against a raw matrix pair, the shape the billboard helpers use. */
    public static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z,
            float u, float v, int overlay, int light, int red, int green, int blue, int alpha,
            float nx, float ny, float nz) {
        //? if forge {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
        //?} else {
        /*Vector3f transformed = normal.transform(new Vector3f(nx, ny, nz));
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(transformed.x(), transformed.y(), transformed.z());
        *///?}
    }
}

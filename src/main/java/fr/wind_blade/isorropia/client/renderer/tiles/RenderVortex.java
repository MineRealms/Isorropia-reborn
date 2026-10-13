package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectBlendMode;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.api.client.EffectSpriteSheet;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import fr.wind_blade.isorropia.common.tiles.TileVortex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Thaumic Horizons' vortex. TH copied Thaumcraft's aura-node renderer for this, so we follow TC4R's own
 * {@code AuraNodeRenderer}: each stored aspect is a camera-facing, in-plane-spinning strip of the shared node atlas
 * tinted to the aspect's colour, and the rift core is a rotating quad of {@code misc/vortex.png} that swells with the
 * warm-up and shrinks again as the rift collapses. Once six attenuator beams are holding it, the core gives way to a
 * bright node strip, exactly as TH swapped it.
 */
public class RenderVortex implements BlockEntityRenderer<TileVortex> {
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/misc/nodes.png");
    private static final ResourceLocation CORE = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/misc/vortex.png");
    private static final int FRAMES = 32;
    private static final int MAX_BEAMS = 6;
    private static final float VIEW_DISTANCE = 64.0F;
    /** The core's full radius once the rift has matured. */
    private static final float CORE_RADIUS = 0.5F;

    public RenderVortex(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileVortex vortex, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
            int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || vortex.getLevel() == null) {
            return;
        }
        Map<AspectId, Integer> aspects = vortex.aspectMap();
        if (aspects.isEmpty()) {
            return;
        }
        Vec3 centre = Vec3.atCenterOf(vortex.getBlockPos());
        double distance = Math.sqrt(player.distanceToSqr(centre));
        if (distance > VIEW_DISTANCE) {
            return;
        }
        float alpha = (float) ((VIEW_DISTANCE - distance) / VIEW_DISTANCE);
        long nanoTime = System.nanoTime();
        int frame = Math.floorMod((int) (nanoTime / 40_000_000L + vortex.getBlockPos().getX()), FRAMES);
        long animationTime = nanoTime / 5_000_000L;
        int beams = vortex.beams();
        int count = vortex.count();
        boolean collapsing = vortex.collapsing();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());

        VertexConsumer translucent = buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearTranslucentBillboard(NODES));
        VertexConsumer additive = buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearAdditiveBillboard(NODES));

        float lastAngle = 0.0F;
        int index = 0;
        int aspectCount = aspects.size();
        for (Map.Entry<AspectId, Integer> entry : aspects.entrySet()) {
            AspectDefinition definition = AspectApi.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            float pulse = Mth.sin(player.tickCount / (14.0F - index)) * 0.25F + 0.5F;
            float scale = 0.2F + pulse * (entry.getValue() / 50.0F);
            long period = 5_000L + 500L * index;
            float angle = Mth.TWO_PI * (animationTime % period) / period;
            lastAngle = angle;
            boolean isAdditive = definition.blendMode() == AspectBlendMode.ADDITIVE;
            float aspectAlpha = alpha * (isAdditive ? 1.0F : 1.5F) / Math.max(1.0F, aspectCount / 2.0F);
            emitStrip(poseStack, isAdditive ? additive : translucent, frame, 0, scale, angle, definition.color(),
                    aspectAlpha);
            index++;
        }

        float coreRadius;
        if (count < 50 && !collapsing) {
            coreRadius = CORE_RADIUS * count / 50.0F;
        } else if (collapsing) {
            coreRadius = CORE_RADIUS * (1.0F - Math.min(25, count) / 25.0F);
        } else {
            coreRadius = CORE_RADIUS;
        }
        if (beams < MAX_BEAMS || count < 50) {
            float coreAngle = lastAngle * 20.0F / (1 + 2 * beams);
            emitQuad(poseStack, buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearTranslucentBillboard(CORE)),
                    coreRadius, coreAngle, 0xFFFFFF, alpha * 0.8F);
        } else {
            emitStrip(poseStack, translucent, frame, 2, 0.4F, lastAngle, 0xFFFFFF, alpha);
        }
        poseStack.popPose();
    }

    /** One cell of the node atlas: the frame runs along U, the strip along V. */
    private static void emitStrip(PoseStack poseStack, VertexConsumer vertices, int frame, int strip, float radius,
            float angle, int colour, float alpha) {
        float u0 = frame / (float) FRAMES;
        float u1 = (frame + 1.0F) / FRAMES;
        float v0 = strip / (float) FRAMES;
        float v1 = (strip + 1.0F) / FRAMES;
        var uv = ThaumcraftClientRenderApi.effectAtlasUv(EffectSpriteSheet.NODES, u0, u1, v0, v1);
        quad(poseStack, vertices, radius, angle, colour, alpha, uv.u0(), uv.v0(), uv.u1(), uv.v1());
    }

    /** The core is a whole texture, not an atlas cell. */
    private static void emitQuad(PoseStack poseStack, VertexConsumer vertices, float radius, float angle, int colour,
            float alpha) {
        quad(poseStack, vertices, radius, angle, colour, alpha, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    private static void quad(PoseStack poseStack, VertexConsumer vertices, float radius, float angle, int colour,
            float alpha, float u0, float v0, float u1, float v1) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(angle));
        PoseStack.Pose pose = poseStack.last();
        int red = colour >> 16 & 0xFF;
        int green = colour >> 8 & 0xFF;
        int blue = colour & 0xFF;
        int alphaByte = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        vertex(vertices, pose, -radius, -radius, red, green, blue, alphaByte, u1, v1);
        vertex(vertices, pose, radius, -radius, red, green, blue, alphaByte, u1, v0);
        vertex(vertices, pose, radius, radius, red, green, blue, alphaByte, u0, v0);
        vertex(vertices, pose, -radius, radius, red, green, blue, alphaByte, u0, v1);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, int red, int green,
            int blue, int alpha, float u, float v) {
        vertices.vertex(pose.pose(), x, y, 0.0F).color(red, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }
}

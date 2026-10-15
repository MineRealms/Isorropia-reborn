package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tc4port.thaumcraft.api.aspect.AspectApi;
import dev.tc4port.thaumcraft.api.aspect.AspectDefinition;
import dev.tc4port.thaumcraft.api.aspect.AspectId;
import dev.tc4port.thaumcraft.client.EffectAtlases;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.client.model.EtherealShardModel;
import fr.wind_blade.isorropia.common.tiles.TileEtherealShard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Random;

/** TH's floating ModelCrystal, orbiting aspect glyphs and scrolling, two-plane vis transfer ribbon. */
public class RenderEtherealShard implements BlockEntityRenderer<TileEtherealShard> {
    private static final ResourceLocation CRYSTAL = texture("models/crystal.png");
    private static final ResourceLocation GLYPHS = texture("misc/nodes.png");
    private static final ResourceLocation BEAM = texture("misc/wispy.png");
    private static final int GLYPH_FRAMES = 32;
    private static final float BOB = 0.1F;
    private static final float BEAM_HALF_WIDTH = 0.15F;
    private static final int BEAM_STEPS_PER_BLOCK = 8;

    private final ModelPart crystal;

    public RenderEtherealShard(BlockEntityRendererProvider.Context context) {
        this.crystal = context.bakeLayer(EtherealShardModel.LAYER).getChild("crystal");
    }

    @Override
    public void render(TileEtherealShard shard, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        float rotation = shard.rotation(partialTick);
        renderCrystal(shard, poseStack, buffers, rotation, averageColour(shard));
        renderGlyphs(shard, poseStack, buffers, rotation);
        renderBeam(shard, poseStack, buffers, partialTick);
    }

    /** TH starts each channel at white, then weights every bound aspect by its capacity. */
    private static int averageColour(TileEtherealShard shard) {
        long red = 255, green = 255, blue = 255, weight = 1;
        for (Map.Entry<AspectId, Integer> entry : shard.capacity().entrySet()) {
            AspectDefinition definition = AspectApi.get(entry.getKey());
            if (definition == null) {
                continue;
            }
            int amount = entry.getValue();
            red += (definition.color() >> 16 & 255) * (long) amount;
            green += (definition.color() >> 8 & 255) * (long) amount;
            blue += (definition.color() & 255) * (long) amount;
            weight += amount;
        }
        return (int) (red / weight) << 16 | (int) (green / weight) << 8 | (int) (blue / weight);
    }

    private void renderCrystal(TileEtherealShard shard, PoseStack poseStack, MultiBufferSource buffers,
            float rotation, int tint) {
        var pos = shard.getBlockPos();
        Random random = new Random(pos.getX() + (long) pos.getY() * pos.getZ());
        int ticks = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
        float shade = Mth.sin((ticks + random.nextInt(10)) / (5.0F + random.nextFloat())) * 0.075F + 0.925F;
        poseStack.pushPose();
        poseStack.translate(0.5, -0.15 + BOB * Mth.sin(rotation * Mth.DEG_TO_RAD), 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale((0.15F + random.nextFloat() * 0.075F) * 1.1F,
                (0.5F + random.nextFloat() * 0.1F) * 1.1F,
                (0.15F + random.nextFloat() * 0.05F) * 1.1F);
        // TH sends 210*shade to the lightmap and divides RGB by 220, not 255.
        int crystalLight = shard.getLevel() == null ? LightTexture.FULL_BRIGHT : (int) (210.0F * shade);
        RenderColors.part(crystal, poseStack, buffers.getBuffer(RenderType.entityTranslucent(CRYSTAL)), crystalLight,
                OverlayTexture.NO_OVERLAY, Math.min(1.0F, (tint >> 16 & 255) / 220.0F),
                Math.min(1.0F, (tint >> 8 & 255) / 220.0F), Math.min(1.0F, (tint & 255) / 220.0F), 1.0F);
        poseStack.popPose();
    }

    private static void renderGlyphs(TileEtherealShard shard, PoseStack poseStack, MultiBufferSource buffers,
            float rotation) {
        Map<AspectId, Integer> aspects = shard.capacity();
        if (aspects.isEmpty()) {
            return;
        }
        int frame = Mth.floor(rotation) % GLYPH_FRAMES;
        // TH samples strip 1. The current TC4R atlas pads every cell, so raw 1/32 UVs are not sufficient.
        var uv = EffectAtlases.NODES.uv(
                frame / 32.0F, (frame + 1) / 32.0F, 1.0F / 32.0F, 2.0F / 32.0F);
        VertexConsumer vertices = buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearTranslucentBillboard(GLYPHS));
        float step = Mth.TWO_PI / aspects.size();
        float radians = rotation * Mth.DEG_TO_RAD;
        int index = 0;
        for (AspectId aspect : aspects.keySet()) {
            AspectDefinition definition = AspectApi.get(aspect);
            if (definition != null) {
                float distance = 0.4F + 0.1F * Mth.cos(radians);
                float size = 0.1F + 0.005F * shard.stored(aspect);
                poseStack.pushPose();
                poseStack.translate(0.5 + distance * Mth.sin(2.0F * radians + step * index),
                        0.64 + BOB * Mth.sin(radians), 0.5 + distance * Mth.cos(2.0F * radians + step * index));
                poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                PoseStack.Pose pose = poseStack.last();
                vertex(vertices, pose, new Vec3(-size, -size, 0), uv.u0(), uv.v1(), definition.color(), 0.9F);
                vertex(vertices, pose, new Vec3(size, -size, 0), uv.u1(), uv.v1(), definition.color(), 0.9F);
                vertex(vertices, pose, new Vec3(size, size, 0), uv.u1(), uv.v0(), definition.color(), 0.9F);
                vertex(vertices, pose, new Vec3(-size, size, 0), uv.u0(), uv.v0(), definition.color(), 0.9F);
                poseStack.popPose();
            }
            index++;
        }
    }

    private static void renderBeam(TileEtherealShard shard, PoseStack poseStack, MultiBufferSource buffers,
            float partialTick) {
        if (shard.getLevel() == null || !shard.draining(shard.getLevel()) || shard.drainPlayer() == null) {
            return;
        }
        Player player = shard.getLevel().getPlayerByUUID(shard.drainPlayer());
        if (player == null || !player.isUsingItem()) {
            return;
        }
        Vec3 origin = Vec3.atLowerCornerOf(shard.getBlockPos());
        Vec3 from = ThaumcraftClientRenderApi.resolveFocusCastOrigin(player, InteractionHand.MAIN_HAND).subtract(origin);
        Vec3 to = new Vec3(0.5, 0.5, 0.5);
        Vec3 delta = to.subtract(from);
        float distance = (float) delta.length();
        if (distance < 1.0E-4F) {
            return;
        }
        int steps = Math.max(1, Math.round(distance) * BEAM_STEPS_PER_BLOCK);
        float reveal = Math.min(player.getTicksUsingItem() + partialTick, 10.0F) / 10.0F;
        int visibleSteps = Mth.clamp(Mth.floor(steps * reveal), 0, steps);
        float time = (float) (System.nanoTime() / 30_000_000L);
        Vec3 worldTarget = origin.add(to);
        VertexConsumer vertices = buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearAdditiveBillboard(BEAM));
        RibbonPoint previous = ribbonPoint(0, steps, distance, from, delta, worldTarget, time);
        for (int i = 1; i <= visibleSteps; i++) {
            RibbonPoint next = ribbonPoint(i, steps, distance, from, delta, worldTarget, time);
            ribbonQuad(vertices, poseStack.last(), previous, next, new Vec3(0, BEAM_HALF_WIDTH, 0), shard.drainColour());
            ribbonQuad(vertices, poseStack.last(), previous, next, new Vec3(BEAM_HALF_WIDTH, 0, 0), shard.drainColour());
            previous = next;
        }
    }

    /** TC4's drawFloatyLine: sinusoidal displacement, faded ends and a scrolling U coordinate. */
    private static RibbonPoint ribbonPoint(int index, int steps, float distance, Vec3 from, Vec3 delta,
            Vec3 worldTarget, float time) {
        float fraction = index / (float) steps;
        float alpha = 1.0F - Math.abs(index - steps / 2.0F) / (steps / 2.0F);
        double phase = distance * (1.0F - fraction) * BEAM_STEPS_PER_BLOCK - time % 32767.0F / 5.0F;
        double waveX = Mth.sin((float) ((worldTarget.z % 16.0 + phase) / 4.0)) * 0.5F * alpha;
        double waveY = Mth.sin((float) ((worldTarget.x % 16.0 + phase) / 3.0)) * 0.5F * alpha;
        double waveZ = Mth.sin((float) ((worldTarget.y % 16.0 + phase) / 2.0)) * 0.5F * alpha;
        Vec3 position = from.add((delta.x + waveX) * fraction, (delta.y + waveY) * fraction,
                (delta.z + waveZ) * fraction);
        return new RibbonPoint(position, (1.0F - fraction) * distance + time * 0.02F, alpha);
    }

    private static void ribbonQuad(VertexConsumer vertices, PoseStack.Pose pose, RibbonPoint first,
            RibbonPoint second, Vec3 width, int colour) {
        vertex(vertices, pose, first.position().subtract(width), first.u(), 1, colour, first.alpha());
        vertex(vertices, pose, first.position().add(width), first.u(), 0, colour, first.alpha());
        vertex(vertices, pose, second.position().add(width), second.u(), 0, colour, second.alpha());
        vertex(vertices, pose, second.position().subtract(width), second.u(), 1, colour, second.alpha());
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 pos, float u, float v,
            int colour, float alpha) {
        RenderColors.vertex(vertices, pose, (float) pos.x, (float) pos.y, (float) pos.z, u, v,
                OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, colour >> 16 & 255, colour >> 8 & 255, colour & 255,
                Math.round(alpha * 255.0F), 0.0F, 0.0F, 1.0F);
    }

    private record RibbonPoint(Vec3 position, float u, float alpha) {
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path);
    }
}

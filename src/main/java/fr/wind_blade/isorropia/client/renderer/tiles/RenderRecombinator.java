package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tc4port.thaumcraft.api.client.ThaumcraftClientRenderApi;
import dev.tc4port.thaumcraft.block.entity.AuraNodeBlockEntity;
import dev.tc4port.thaumcraft.registry.TCBlocks;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.model.RecombinatorModel;
import fr.wind_blade.isorropia.common.tiles.TileRecombinator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Thaumic Horizons' teleological recombinator. The clawed pedestal and its bobbing pearl come from TH's ModelRecombinator.
 * While a redstone signal feeds it, TH hung a scrolling lightning ring above the machine and dropped a white beam into
 * the node below; both are reproduced here, the ring from Thaumcraft's own animated strip.
 */
public class RenderRecombinator implements BlockEntityRenderer<TileRecombinator> {
    private static final ResourceLocation MODEL = Isorropia.id("textures/models/recombinator.png");
    private static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/item/lightningringv.png");
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/misc/wispy.png");
    private static final int RING_FRAMES = 16;
    private static final float RING_HALF_SIZE = 0.75F;
    private static final float BEAM_HALF_WIDTH = 0.06F;
    private static final int BEAM_STEPS = 6;

    private final ModelPart body;
    private final ModelPart pearl;

    public RenderRecombinator(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(RecombinatorModel.LAYER);
        this.body = root.getChild("body");
        this.pearl = root.getChild("pearl");
    }

    @Override
    public void render(TileRecombinator recombinator, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        float bob = Mth.sin((recombinator.animationTicks() + partialTick) / 8.0F) * 2.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        VertexConsumer model = buffers.getBuffer(RenderType.entityCutoutNoCull(MODEL));
        this.body.render(poseStack, model, light, overlay);
        this.pearl.y = RecombinatorModel.PEARL_REST_Y + bob;
        this.pearl.render(poseStack, model, light, overlay);
        poseStack.popPose();

        if (recombinator.isActivated()) {
            renderRing(poseStack, buffers, bob);
            renderBeam(recombinator, poseStack, buffers);
        }
    }

    /** TH hung a camera-facing animated lightning ring one and a half blocks above the machine. */
    private static void renderRing(PoseStack poseStack, MultiBufferSource buffers, float bob) {
        int frame = (int) ((System.nanoTime() / 40_000_000L) % RING_FRAMES);
        float u0 = frame / (float) RING_FRAMES;
        float u1 = (frame + 1) / (float) RING_FRAMES;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(RING));
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F + 0.1F * bob, 0.5F);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        vertex(vertices, pose, RING_HALF_SIZE, RING_HALF_SIZE, u1, 0.0F);
        vertex(vertices, pose, -RING_HALF_SIZE, RING_HALF_SIZE, u0, 0.0F);
        vertex(vertices, pose, -RING_HALF_SIZE, -RING_HALF_SIZE, u0, 1.0F);
        vertex(vertices, pose, RING_HALF_SIZE, -RING_HALF_SIZE, u1, 1.0F);
        poseStack.popPose();
    }

    /** TH dropped a white beam from the machine into the node directly beneath it. */
    private static void renderBeam(TileRecombinator recombinator, PoseStack poseStack, MultiBufferSource buffers) {
        Level level = recombinator.getLevel();
        if (level == null) {
            return;
        }
        BlockPos below = recombinator.getBlockPos().below();
        if (!(level.getBlockEntity(below) instanceof AuraNodeBlockEntity node)
                || !node.getBlockState().is(TCBlocks.AURA_NODE.get())) {
            return;
        }
        Vec3 from = new Vec3(0.5, 0.2, 0.5);
        Vec3 to = new Vec3(0.5, -0.5, 0.5);
        float time = (float) (System.nanoTime() / 30_000_000L);
        VertexConsumer vertices = buffers.getBuffer(ThaumcraftClientRenderApi.legacyLinearAdditiveBillboard(BEAM));
        RibbonPoint previous = new RibbonPoint(from, 0.0F, 0.0F);
        for (int i = 1; i <= BEAM_STEPS; i++) {
            float fraction = i / (float) BEAM_STEPS;
            Vec3 point = from.add(to.subtract(from).scale(fraction));
            RibbonPoint next = new RibbonPoint(point, fraction * 2.0F + time * 0.02F, 1.0F);
            ribbonQuad(vertices, poseStack.last(), previous, next, new Vec3(0.0, BEAM_HALF_WIDTH, 0.0), 0xFFFFFF);
            ribbonQuad(vertices, poseStack.last(), previous, next, new Vec3(BEAM_HALF_WIDTH, 0.0, 0.0), 0xFFFFFF);
            previous = next;
        }
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float u, float v) {
        RenderColors.vertex(vertices, pose, x, y, 0.0F, u, v, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT,
                255, 255, 255, 255, 0.0F, 0.0F, 1.0F);
    }

    private static void ribbonQuad(VertexConsumer vertices, PoseStack.Pose pose, RibbonPoint first, RibbonPoint second,
            Vec3 width, int colour) {
        vertex(vertices, pose, first.position().subtract(width), first.u(), 1.0F, colour, first.alpha());
        vertex(vertices, pose, first.position().add(width), first.u(), 0.0F, colour, first.alpha());
        vertex(vertices, pose, second.position().add(width), second.u(), 0.0F, colour, second.alpha());
        vertex(vertices, pose, second.position().subtract(width), second.u(), 1.0F, colour, second.alpha());
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 pos, float u, float v, int colour,
            float alpha) {
        RenderColors.vertex(vertices, pose, (float) pos.x, (float) pos.y, (float) pos.z, u, v,
                OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, colour >> 16 & 255, colour >> 8 & 255, colour & 255,
                Mth.clamp(Math.round(alpha * 255.0F), 0, 255), 0.0F, 0.0F, 1.0F);
    }

    private record RibbonPoint(Vec3 position, float u, float alpha) {
    }
}

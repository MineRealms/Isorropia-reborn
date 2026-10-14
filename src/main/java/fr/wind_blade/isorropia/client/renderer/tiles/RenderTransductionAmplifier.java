package fr.wind_blade.isorropia.client.renderer.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.wind_blade.isorropia.client.model.ObjMesh;
import fr.wind_blade.isorropia.common.blocks.BlockTransductionAmplifier;
import fr.wind_blade.isorropia.common.tiles.TileTransductionAmplifier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Thaumic Horizons drew the amplifier with Thaumcraft's node-converter model (a central lock and four pistons) and lit
 * it with a status colour: green while idle, orange while amplifying, red while overcharged by neighbouring amplifiers.
 * TC4R's converter shares the stabilizer's OBJ geometry, so we reuse that mesh with the converter textures and fold
 * the status colour into the emissive overlay pass, mimicking {@code NodeMachineRenderHelper.renderTransducer}.
 */
public class RenderTransductionAmplifier implements BlockEntityRenderer<TileTransductionAmplifier> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/models/node_stabilizer.obj");
    private static final ResourceLocation CONVERTER = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/models/node_converter.png");
    private static final ResourceLocation CONVERTER_OVERLAY = ResourceLocation.fromNamespaceAndPath("thaumcraft",
            "textures/models/node_converter_over.png");

    private final ObjMesh mesh = ObjMesh.get(MODEL);

    public RenderTransductionAmplifier(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileTransductionAmplifier amplifier, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        Direction facing = amplifier.getBlockState() != null
                ? amplifier.getBlockState().getValue(BlockTransductionAmplifier.FACING) : Direction.UP;
        int status = amplifier.status();
        // TH drove the piston travel and the overlay glow from v = min(50, count) / 137 (not /50 - dividing by the
        // clamp would push the arms right out of the block).
        float ramp = Math.min(50, Math.max(0, amplifier.count())) / 137.0F;
        float strength = ramp * 2.5F;
        int colour = switch (status) {
            case 2 -> 0xFFFF004D; // overcharged, red
            case 1 -> 0xFFFF991A; // amplifying, orange
            default -> 0xFF80FF80; // idle, green
        };
        float red = (colour >> 16 & 255) / 255.0F;
        float green = (colour >> 8 & 255) / 255.0F;
        float blue = (colour & 255) / 255.0F;
        float ticks = amplifier.animationTicks() + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.0F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        orient(facing, poseStack);

        mesh.render("lock", poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(CONVERTER)), light, overlay,
                1.0F, 1.0F, 1.0F, 1.0F);
        mesh.render("lock", poseStack, buffers.getBuffer(RenderType.entityTranslucent(CONVERTER_OVERLAY)),
                pulseLight(strength, Mth.sin(ticks / 3.0F) * 0.1F + 0.9F), overlay, red, green, blue, 1.0F);
        // Four pistons, each pushed out from the core by how hard the node is being amplified.
        for (int arm = 0; arm < 4; arm++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F * arm));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.translate(0.0F, 0.0F, ramp);
            mesh.render("piston", poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(CONVERTER)), light,
                    overlay, 1.0F, 1.0F, 1.0F, 1.0F);
            mesh.render("piston", poseStack, buffers.getBuffer(RenderType.entityTranslucent(CONVERTER_OVERLAY)),
                    pulseLight(strength, Mth.sin((ticks + arm * 5.0F) / 3.0F) * 0.1F + 0.9F),
                    overlay, red, green, blue, 1.0F);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** TH's 50..220 lightmap pulse, folded into a packed block/sky light so the overlay breathes instead of glaring. */
    private static int pulseLight(float strength, float pulse) {
        int value = Mth.clamp((int) (50.0F + 170.0F * strength * pulse), 0, 240);
        return value << 16 | value;
    }

    /** Rotate the model so its stem points at the node the amplifier is attached to. */
    private static void orient(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            default -> { /* UP: the model already points up */ }
        }
    }
}

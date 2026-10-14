package fr.wind_blade.isorropia.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.common.entities.EntityJellyRabbit;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

import java.util.List;

/**
 * The jelly rabbit's core: body, head and two slime eyes of the rabbit layout, tinted with the aspect colour and
 * shrunk to 55% around the feet.
 */
public class ModelJellyRabbit extends EntityModel<EntityJellyRabbit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("jelly_rabbit"), "main");
    private static final float SIZE = 0.55F;
    private final ModelPart head;
    private final ModelPart rightEye;
    private final ModelPart leftEye;
    private final List<ModelPart> parts;
    private float red = 1.0F;
    private float green = 1.0F;
    private float blue = 1.0F;
    private float entityHeight = 0.5F;

    public ModelJellyRabbit(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.head = root.getChild("head");
        this.rightEye = root.getChild("right_eye");
        this.leftEye = root.getChild("left_eye");
        this.parts = List.of(root.getChild("body"), this.head, this.rightEye, this.leftEye);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -10.0F, 6, 5, 10).mirror(),
                PartPose.offsetAndRotation(0.0F, 19.0F, 8.0F, -0.34906584F, 0.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-2.5F, -4.0F, -5.0F, 5, 4, 5).mirror(),
                PartPose.offset(0.0F, 16.0F, -1.0F));
        root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(58, 26).addBox(-1.9F, -3.1F, -5.5F, 1, 1, 1),
                PartPose.offset(0.0F, 16.0F, -1.0F));
        root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(58, 26).addBox(0.9F, -3.1F, -5.5F, 1, 1, 1),
                PartPose.offset(0.0F, 16.0F, -1.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EntityJellyRabbit rabbit, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pitch = headPitch * ((float) Math.PI / 180F);
        float yaw = netHeadYaw * ((float) Math.PI / 180F);
        for (ModelPart part : List.of(this.head, this.rightEye, this.leftEye)) {
            part.xRot = pitch;
            part.yRot = yaw;
        }
        int color = JellyColors.of(rabbit);
        this.red = (color >> 16 & 255) / 255.0F;
        this.green = (color >> 8 & 255) / 255.0F;
        this.blue = (color & 255) / 255.0F;
        this.entityHeight = rabbit.getBbHeight();
    }

    //? if forge {
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        float offset = this.entityHeight * (SIZE - 1.0F) / 2.0F + 0.08F;
        poseStack.translate(0.0F, 1.5F + offset, 0.0F);
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        for (ModelPart part : this.parts) {
            part.render(poseStack, buffer, light, overlay, red * this.red, green * this.green, blue * this.blue, alpha * JellyColors.ALPHA);
        }
        poseStack.popPose();
    }
    //?} else {
    /*@Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = (color >>> 24) / 255.0F;
        poseStack.pushPose();
        float offset = this.entityHeight * (SIZE - 1.0F) / 2.0F + 0.08F;
        poseStack.translate(0.0F, 1.5F + offset, 0.0F);
        poseStack.scale(SIZE, SIZE, SIZE);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        for (ModelPart part : this.parts) {
            part.render(poseStack, buffer, light, overlay,
                    RenderColors.pack(red * this.red, green * this.green, blue * this.blue, alpha * JellyColors.ALPHA));
        }
        poseStack.popPose();
    }
    *///?}
}

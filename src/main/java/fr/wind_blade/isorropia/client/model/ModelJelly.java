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
import net.minecraft.util.Mth;

import java.util.List;

/**
 * The jelly coat: the full 1.12 rabbit, every pivot moved down 16 px so that the 0.6 scale the layer applies
 * around the model origin keeps the feet on the ground.
 */
public class ModelJelly extends EntityModel<EntityJellyRabbit> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("jelly_rabbit"), "jelly");
    private static final float DEG = (float) Math.PI / 180F;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart leftThigh;
    private final ModelPart rightThigh;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart nose;
    private final List<ModelPart> parts;
    private float jumpRotation;

    public ModelJelly(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.leftFoot = root.getChild("left_foot");
        this.rightFoot = root.getChild("right_foot");
        this.leftThigh = root.getChild("left_thigh");
        this.rightThigh = root.getChild("right_thigh");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.head = root.getChild("head");
        this.rightEar = root.getChild("right_ear");
        this.leftEar = root.getChild("left_ear");
        this.nose = root.getChild("nose");
        this.parts = List.of(this.leftFoot, this.rightFoot, this.leftThigh, this.rightThigh, root.getChild("body"), this.leftArm,
                this.rightArm, this.head, this.rightEar, this.leftEar, root.getChild("tail"), this.nose);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(26, 24).addBox(-1.0F, 5.5F, -3.7F, 2, 1, 7).mirror(),
                PartPose.offset(3.0F, 33.5F, 3.7F));
        root.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(8, 24).addBox(-1.0F, 5.5F, -3.7F, 2, 1, 7).mirror(),
                PartPose.offset(-3.0F, 33.5F, 3.7F));
        root.addOrReplaceChild("left_thigh", CubeListBuilder.create().texOffs(30, 15).addBox(-1.0F, 0.0F, 0.0F, 2, 4, 5).mirror(),
                PartPose.offsetAndRotation(3.0F, 33.5F, 3.7F, -0.34906584F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_thigh", CubeListBuilder.create().texOffs(16, 15).addBox(-1.0F, 0.0F, 0.0F, 2, 4, 5).mirror(),
                PartPose.offsetAndRotation(-3.0F, 33.5F, 3.7F, -0.34906584F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -10.0F, 6, 5, 10).mirror(),
                PartPose.offsetAndRotation(0.0F, 35.0F, 8.0F, -0.34906584F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(8, 15).addBox(-1.0F, 0.0F, -1.0F, 2, 7, 2).mirror(),
                PartPose.offsetAndRotation(3.0F, 33.0F, -1.0F, -0.17453292F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 15).addBox(-1.0F, 0.0F, -1.0F, 2, 7, 2).mirror(),
                PartPose.offsetAndRotation(-3.0F, 33.0F, -1.0F, -0.17453292F, 0.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 0).addBox(-2.5F, -4.0F, -5.0F, 5, 4, 5).mirror(),
                PartPose.offset(0.0F, 32.0F, -1.0F));
        root.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-2.5F, -9.0F, -1.0F, 2, 5, 1).mirror(),
                PartPose.offsetAndRotation(0.0F, 32.0F, -1.0F, 0.0F, -0.2617994F, 0.0F));
        root.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(58, 0).addBox(0.5F, -9.0F, -1.0F, 2, 5, 1).mirror(),
                PartPose.offsetAndRotation(0.0F, 32.0F, -1.0F, 0.0F, 0.2617994F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(52, 6).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 2).mirror(),
                PartPose.offsetAndRotation(0.0F, 36.0F, 7.0F, -0.3490659F, 0.0F, 0.0F));
        root.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(32, 9).addBox(-0.5F, -2.5F, -5.5F, 1, 1, 1).mirror(),
                PartPose.offset(0.0F, 32.0F, -1.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void prepareMobModel(EntityJellyRabbit rabbit, float limbSwing, float limbSwingAmount, float partialTick) {
        this.jumpRotation = Mth.sin(rabbit.getJumpCompletion(partialTick) * (float) Math.PI);
    }

    @Override
    public void setupAnim(EntityJellyRabbit rabbit, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float pitch = headPitch * DEG;
        float yaw = netHeadYaw * DEG;
        this.nose.xRot = pitch;
        this.head.xRot = pitch;
        this.rightEar.xRot = pitch;
        this.leftEar.xRot = pitch;
        this.nose.yRot = yaw;
        this.head.yRot = yaw;
        this.leftThigh.xRot = (this.jumpRotation * 50.0F - 21.0F) * DEG;
        this.rightThigh.xRot = (this.jumpRotation * 50.0F - 21.0F) * DEG;
        this.leftFoot.xRot = this.jumpRotation * 50.0F * DEG;
        this.rightFoot.xRot = this.jumpRotation * 50.0F * DEG;
        this.leftArm.xRot = (this.jumpRotation * -40.0F - 11.0F) * DEG;
        this.rightArm.xRot = (this.jumpRotation * -40.0F - 11.0F) * DEG;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        for (ModelPart part : this.parts) {
            part.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        }
    }
}

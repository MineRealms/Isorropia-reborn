package fr.wind_blade.isorropia.client.model;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Thaumic Horizons' ModelBloodInfuser: the Seroconverter's iron chassis with a raised syringe port and four
 * essentia stubs, two to the north and south and two to the east and west. TH wrote the boxes in the old model
 * space, which the renderer enters through TH's own translate-plus-flip, so the poses here are TH's verbatim;
 * every part renders together, so the renderer just draws the baked root.
 */
public final class BloodInfuserModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("blood_infuser"), "main");

    private BloodInfuserModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 16, 10, 16), PartPose.offset(-8.0F, 14.0F, -8.0F));
        root.addOrReplaceChild("syringe_slot", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 6, 6, 6), PartPose.offset(-3.0F, 8.0F, -3.0F));
        root.addOrReplaceChild("pipe_1a", CubeListBuilder.create().texOffs(0, 48).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 2, 2, 3), PartPose.offset(-1.0F, 9.0F, 3.0F));
        root.addOrReplaceChild("pipe_1b", CubeListBuilder.create().texOffs(0, 54).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 2, 3, 2), PartPose.offset(-1.0F, 11.0F, 4.0F));
        root.addOrReplaceChild("pipe_2a", CubeListBuilder.create().texOffs(0, 48).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 2, 2, 3), PartPose.offset(-1.0F, 9.0F, -6.0F));
        root.addOrReplaceChild("pipe_2b", CubeListBuilder.create().texOffs(0, 54).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 2, 3, 2), PartPose.offset(-1.0F, 11.0F, -6.0F));
        root.addOrReplaceChild("pipe_3a", CubeListBuilder.create().texOffs(0, 48).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 3, 2, 2), PartPose.offset(3.0F, 9.0F, -1.0F));
        root.addOrReplaceChild("pipe_3b", CubeListBuilder.create().texOffs(0, 54).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 2, 3, 2), PartPose.offset(4.0F, 11.0F, -1.0F));
        root.addOrReplaceChild("pipe_4a", CubeListBuilder.create().texOffs(0, 48).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 3, 2, 2), PartPose.offset(-6.0F, 9.0F, -1.0F));
        root.addOrReplaceChild("pipe_4b", CubeListBuilder.create().texOffs(0, 54).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 2, 3, 2), PartPose.offset(-6.0F, 11.0F, -1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

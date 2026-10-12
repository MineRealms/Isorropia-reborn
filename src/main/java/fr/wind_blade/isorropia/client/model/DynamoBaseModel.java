package fr.wind_blade.isorropia.client.model;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Thaumic Horizons' ModelQuarterBlock: the one-pixel plate a dynamo stands on, mapped onto a 64 by 32 skin. */
public final class DynamoBaseModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("dynamo_base"), "main");

    private DynamoBaseModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).mirror()
                .addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 16.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static ModelPart plate(ModelPart root) {
        return root.getChild("plate");
    }
}

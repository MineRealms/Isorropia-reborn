package fr.wind_blade.isorropia.client.model;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** TC4's ModelCrystal, including its off-centre box, rotation point and 64 by 32 box UVs. */
public final class EtherealShardModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("ethereal_shard"), "main");

    private EtherealShardModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-16.0F, -16.0F, 0.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offsetAndRotation(0.0F, 32.0F, 0.0F, 0.7071F, 0.0F, 0.7071F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}

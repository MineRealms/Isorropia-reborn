package fr.wind_blade.isorropia.client.model;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * TH's ModelRecombinator: a low iron base, four pairs of claws rising around it, an inner step and a top cap, and the
 * pearl they cradle. TH authors the model with y=24 at the block's foot; every box here is re-expressed with y=0 at the
 * foot (the renderer translates the mesh to the block centre).
 */
public final class RecombinatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("recombinator"), "main");
    /** The pearl's resting height, in the same units TH used. It bobs two units either side of this. */
    public static final float PEARL_REST_Y = 8.0F;

    private RecombinatorModel() {
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 4.0F, 16.0F)
                .texOffs(0, 28).addBox(-1.0F, 4.0F, 7.0F, 2.0F, 8.0F, 1.0F)
                .texOffs(0, 28).addBox(-1.0F, 4.0F, -8.0F, 2.0F, 8.0F, 1.0F)
                .texOffs(6, 28).addBox(-8.0F, 4.0F, -1.0F, 1.0F, 8.0F, 2.0F)
                .texOffs(6, 28).addBox(7.0F, 4.0F, -1.0F, 1.0F, 8.0F, 2.0F)
                .texOffs(0, 38).addBox(-4.0F, 12.0F, -4.0F, 8.0F, 2.0F, 8.0F)
                .texOffs(0, 48).addBox(-2.0F, 14.0F, -2.0F, 4.0F, 2.0F, 4.0F)
                .texOffs(0, 54).addBox(-1.0F, 12.0F, 4.0F, 2.0F, 1.0F, 4.0F)
                .texOffs(0, 54).addBox(-1.0F, 12.0F, -8.0F, 2.0F, 1.0F, 4.0F)
                .texOffs(0, 59).addBox(4.0F, 12.0F, -1.0F, 4.0F, 1.0F, 2.0F)
                .texOffs(0, 59).addBox(-8.0F, 12.0F, -1.0F, 4.0F, 1.0F, 2.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("pearl", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, PEARL_REST_Y, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}

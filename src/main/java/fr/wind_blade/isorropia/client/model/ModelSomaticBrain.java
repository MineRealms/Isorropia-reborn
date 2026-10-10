package fr.wind_blade.isorropia.client.model;

import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The brain worn on a head: three stacked lobes, in the sizes the 1.12 model used. The boxes sit above the head
 * pivot so that scaling the part puts the brain on top of the skull.
 */
public final class ModelSomaticBrain {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("somatic_brain"), "main");

    private final ModelPart brain;

    public ModelSomaticBrain(ModelPart root) {
        this.brain = root.getChild("brain");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The lobes are the 1.12 sizes, stacked upwards: the entity renderer flips the model's Y, so negative
        // Y here is up in the world and the brain grows away from the head.
        root.addOrReplaceChild("brain", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 4, 8)
                .texOffs(0, 12).addBox(-3.0F, -7.0F, -3.0F, 6, 3, 6)
                .texOffs(0, 21).addBox(-2.5F, -9.0F, -2.0F, 5, 2, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    public ModelPart brain() {
        return brain;
    }
}

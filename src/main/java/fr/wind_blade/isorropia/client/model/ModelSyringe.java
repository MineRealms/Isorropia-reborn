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
 * Thaumic Horizons' syringe: a nine-pixel barrel with the needle off the front and the plunger behind. TH wrote it in
 * the old model space, where y grew downwards from the floor at twenty-four, and slid it back onto the entity's origin
 * before drawing it; the parts here are the same boxes already centred on that origin, needle to the west as TH's was.
 */
public final class ModelSyringe {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("syringe"), "main");

    private final ModelPart body;
    private final ModelPart needle;
    private final ModelPart plungerA;
    private final ModelPart plungerB;

    public ModelSyringe(ModelPart root) {
        this.body = root.getChild("body");
        this.needle = root.getChild("needle");
        this.plungerA = root.getChild("plunger_a");
        this.plungerB = root.getChild("plunger_b");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -1.5F, -1.5F, 9, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("needle", CubeListBuilder.create()
                .texOffs(25, 0).addBox(-8.5F, -0.5F, -0.5F, 4, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("plunger_a", CubeListBuilder.create()
                .texOffs(0, 8).addBox(4.5F, -0.5F, -0.5F, 1, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("plunger_b", CubeListBuilder.create()
                .texOffs(0, 12).addBox(4.5F, -1.5F, -1.5F, 1, 3, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** The barrel, which TH tinted with the injection's colour. */
    public ModelPart body() {
        return this.body;
    }

    /** The needle and the two plunger pieces, which TH drew untinted. */
    public ModelPart needle() {
        return this.needle;
    }

    public ModelPart plungerA() {
        return this.plungerA;
    }

    public ModelPart plungerB() {
        return this.plungerB;
    }
}

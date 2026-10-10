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
 * One sixteen-pixel cube of the modified matrix, on a 64x64 sheet: the body of the matrix reads the top half,
 * and the half below it is the glow drawn over the body when the vat runs. Upstream had exactly this pair, which
 * is why the matrix darkens and lights up as an infusion starts.
 */
public final class MatrixCubeModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Isorropia.id("modified_matrix"), "main");

    private final ModelPart base;
    private final ModelPart overlay;

    public MatrixCubeModel(ModelPart root) {
        this.base = root.getChild("base");
        this.overlay = root.getChild("overlay");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", cube(0), PartPose.ZERO);
        root.addOrReplaceChild("overlay", cube(32), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder cube(int textureOffset) {
        return CubeListBuilder.create().texOffs(0, textureOffset).addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
    }

    public ModelPart base() {
        return base;
    }

    public ModelPart overlay() {
        return overlay;
    }
}

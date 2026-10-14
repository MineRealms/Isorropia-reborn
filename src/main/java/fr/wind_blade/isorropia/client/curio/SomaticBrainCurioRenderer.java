package fr.wind_blade.isorropia.client.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.wind_blade.isorropia.client.model.RenderColors;
import fr.wind_blade.isorropia.Isorropia;
import fr.wind_blade.isorropia.client.model.ModelSomaticBrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Wears the brain on top of the head, at the size and height the original bauble used. */
public class SomaticBrainCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = Isorropia.id("textures/entity/somatic_brain.png");
    /** The original bauble's net scale: Baubles' defaultTransforms scaled 0.55, then the item scaled 1.81. */
    private static final float SCALE = 0.9955F;
    /** The head's crown, half a block above its pivot: the brain rests here and grows upwards. */
    private static final float CROWN = -0.5F;

    private final ModelSomaticBrain model;

    public SomaticBrainCurioRenderer() {
        this.model = new ModelSomaticBrain(Minecraft.getInstance().getEntityModels().bakeLayer(ModelSomaticBrain.LAYER));
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
            PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers, int light,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        poseStack.pushPose();
        humanoid.head.translateAndRotate(poseStack);
        ICurioRenderer.translateIfSneaking(poseStack, slotContext.entity());
        poseStack.translate(0.0F, CROWN, 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        RenderColors.part(model.brain(), poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}

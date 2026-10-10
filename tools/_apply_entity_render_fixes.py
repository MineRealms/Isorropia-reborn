"""One-off edit script for the entity renderer fixes in the render spec."""
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "src/main/java/fr/wind_blade/isorropia/client"

# 1. A camera-facing additive sprite state: one quad, no culling, no depth writes.
path = BASE / "ClientRenderTypes.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""    private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();""",
              """    private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SPRITE = new HashMap<>();""")
s = s.replace("""    private static RenderType create(String name, ResourceLocation texture,""",
"""    /**
     * An additive sprite sheet quad that faces the camera. It must not cull, because which way the camera basis
     * turns the quad is not controlled by the caller, and the stock eyes type it replaces does cull.
     */
    public static RenderType additiveSprite(ResourceLocation texture) {
        return SPRITE.computeIfAbsent(texture, tex -> NativeRenderTypes.create("isorropia_sprite",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(NativeRenderStates.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(NativeRenderStates.LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(NativeRenderStates.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(NativeRenderStates.COLOR_WRITE)
                        .setCullState(NativeRenderStates.NO_CULL)
                        .setLightmapState(NativeRenderStates.LIGHTMAP)
                        .setOverlayState(NativeRenderStates.OVERLAY)
                        .createCompositeState(false)));
    }

    private static RenderType create(String name, ResourceLocation texture,""")
path.write_text(s, encoding="utf-8")

# 2. The sprite is drawn once now that the render type does not cull.
path = BASE / "renderer/entities/Billboards.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""        VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(sheet));
        vertex(consumer, matrix, normal, -halfSize, -halfSize, u0, v1, r, g, b);
        vertex(consumer, matrix, normal, halfSize, -halfSize, u1, v1, r, g, b);
        vertex(consumer, matrix, normal, halfSize, halfSize, u1, v0, r, g, b);
        vertex(consumer, matrix, normal, -halfSize, halfSize, u0, v0, r, g, b);
        // eyes() culls back faces; the reversed winding keeps the sprite visible whichever way the camera basis turns it
        vertex(consumer, matrix, normal, -halfSize, halfSize, u0, v0, r, g, b);
        vertex(consumer, matrix, normal, halfSize, halfSize, u1, v0, r, g, b);
        vertex(consumer, matrix, normal, halfSize, -halfSize, u1, v1, r, g, b);
        vertex(consumer, matrix, normal, -halfSize, -halfSize, u0, v1, r, g, b);""",
"""        VertexConsumer consumer = buffers.getBuffer(ClientRenderTypes.additiveSprite(sheet));
        vertex(consumer, matrix, normal, -halfSize, -halfSize, u0, v1, r, g, b);
        vertex(consumer, matrix, normal, halfSize, -halfSize, u1, v1, r, g, b);
        vertex(consumer, matrix, normal, halfSize, halfSize, u1, v0, r, g, b);
        vertex(consumer, matrix, normal, -halfSize, halfSize, u0, v0, r, g, b);""")
s = s.replace("import net.minecraft.client.renderer.RenderType;\n",
              "import fr.wind_blade.isorropia.client.ClientRenderTypes;\n")
path.write_text(s, encoding="utf-8")

# 3. The label follows the 1.12 transform chain, is drawn on both sides, and its icon is an aspect tag.
path = BASE / "renderer/entities/RenderHangingLabel.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""    @Override
    public void render(EntityHangingLabel entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        quad(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(LABEL)), -0.03F, LABEL_HALF, 0xFFFFFF, 1.0F, light);
        AspectId aspect = entity.getAspect();
        AspectDefinition definition = aspect == null ? null : AspectApi.get(aspect);
        if (definition != null) {
            quad(poseStack, buffers.getBuffer(RenderType.entityTranslucent(definition.texture())), -0.031F, ICON_HALF,
                    definition.color(), 1.0F, light);
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }""",
"""    @Override
    public void render(EntityHangingLabel entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        // The chain the 1.12 renderer used: flip upright, turn with the entity, face outwards, off the wall.
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.03F);
        VertexConsumer label = buffers.getBuffer(RenderType.entityTranslucent(LABEL));
        quad(poseStack, label, 0.0F, LABEL_HALF, 0xFFFFFF, 1.0F, light);
        // A label has two sides, and upstream drew both.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        quad(poseStack, label, 0.0F, LABEL_HALF, 0xFFFFFF, 1.0F, light);
        poseStack.popPose();

        AspectId aspect = entity.getAspect();
        AspectDefinition definition = aspect == null ? null : AspectApi.get(aspect);
        if (definition != null) {
            // The aspect tag: a dark tile with the aspect's own sprite over it.
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, -0.001F);
            quad(poseStack, buffers.getBuffer(RenderType.entityTranslucent(TAG_BACKGROUND)), 0.0F, ICON_HALF,
                    TAG_COLOUR, TAG_ALPHA, light);
            quad(poseStack, buffers.getBuffer(RenderType.entityTranslucent(definition.texture())), -0.001F,
                    ICON_HALF, 0xFFFFFF, 1.0F, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }""")
s = s.replace("""    private static final float ICON_HALF = 8.0F * 0.021F;""",
"""    private static final float ICON_HALF = 8.0F * 0.021F;
    /** The tag behind the aspect sprite, as Thaumcraft draws it. */
    private static final ResourceLocation TAG_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/_back.png");
    private static final int TAG_COLOUR = 0x1A1A1A;
    private static final float TAG_ALPHA = 0.8F;""")
s = s.replace("import net.minecraft.resources.ResourceLocation;",
              "import net.minecraft.resources.ResourceLocation;\nimport net.minecraft.util.Mth;")
path.write_text(s, encoding="utf-8")

# 4. The sheeder is a sheared spider: no red eyes, and its wool lies on the model itself.
path = BASE / "renderer/entities/RenderSheeder.java"
s = path.read_text(encoding="utf-8")
s = s.replace("        this.addLayer(new SpiderEyesLayer<>(this));\n", "")
s = s.replace("import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;\n", "")
s = s.replace("            poseStack.translate(0.0D, -0.01D, -0.01D);\n            poseStack.scale(1.01F, 1.01F, 1.02F);\n", "")
path.write_text(s, encoding="utf-8")

# 5. The panther's shadow matches the original's.
path = BASE / "renderer/entities/RenderGuardianPanther.java"
s = path.read_text(encoding="utf-8")
s = s.replace("new CatModel<>(context.bakeLayer(ModelLayers.CAT)), 0.8F)",
              "new CatModel<>(context.bakeLayer(ModelLayers.CAT)), 0.4F)")
path.write_text(s, encoding="utf-8")

# 6. The taint pig's border is the taint itself and shows even on an unseen pig.
path = BASE / "renderer/entities/RenderTaintPig.java"
s = path.read_text(encoding="utf-8")
s = s.replace("""            if (entity.isInvisible()) {
                return;
            }
            float pulse""", """            float pulse""")
path.write_text(s, encoding="utf-8")

print("entity render fixes applied")

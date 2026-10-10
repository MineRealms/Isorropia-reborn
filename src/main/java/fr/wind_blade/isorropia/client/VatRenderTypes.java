package fr.wind_blade.isorropia.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.tc4port.thaumcraft.nativeimpl.NativeRenderStates;
import dev.tc4port.thaumcraft.nativeimpl.NativeRenderTypes;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * The two states the vat's mesh needs. Upstream drew its liquid with depth writes off and everything else with
 * them on, which is what lets the vat's body be seen through the liquid; no stock render type does that, and the
 * entity types cull back faces, which the mesh's winding cannot survive.
 *
 * <p>They are built from the render shims Thaumcraft itself uses to reach the vanilla state shards, so the vat
 * keeps Thaumcraft's lighting, fog and blending behaviour.
 */
public final class VatRenderTypes {
    private static final Map<ResourceLocation, RenderType> LIQUID = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SHELL = new HashMap<>();

    private VatRenderTypes() {
    }

    /** Blended, and leaves the depth buffer alone so whatever is behind it stays visible. */
    public static RenderType liquid(ResourceLocation texture) {
        return LIQUID.computeIfAbsent(texture, tex -> create("isorropia_vat_liquid", tex,
                NativeRenderStates.COLOR_WRITE));
    }

    /** Blended and depth-writing, so the glass of the vat reads as a real surface. */
    public static RenderType shell(ResourceLocation texture) {
        return SHELL.computeIfAbsent(texture, tex -> create("isorropia_vat_shell", tex,
                NativeRenderStates.COLOR_DEPTH_WRITE));
    }

    private static RenderType create(String name, ResourceLocation texture,
            RenderStateShard.WriteMaskStateShard writeMask) {
        return NativeRenderTypes.create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false,
                true, RenderType.CompositeState.builder()
                        .setShaderState(NativeRenderStates.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, true))
                        .setTransparencyState(NativeRenderStates.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NativeRenderStates.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(writeMask)
                        .setCullState(NativeRenderStates.NO_CULL)
                        .setLightmapState(NativeRenderStates.LIGHTMAP)
                        .setOverlayState(NativeRenderStates.OVERLAY)
                        .createCompositeState(false));
    }
}

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
 * The render states this mod's models need and the stock types cannot give: a liquid that leaves the depth
 * buffer alone so the body inside the vat stays visible, a shell that writes depth, and an additive glow for the
 * matrix. The entity render types cull back faces, which the vat mesh's winding cannot survive.
 *
 * <p>They are built from the render shims Thaumcraft itself uses to reach the vanilla state shards, so they keep
 * Thaumcraft's lighting, fog and blending behaviour.
 */
public final class ClientRenderTypes {
    private static final Map<ResourceLocation, RenderType> LIQUID = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SHELL = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();

    private ClientRenderTypes() {
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

    /**
     * Additive and colour-only, for the glow the matrix shows over its own body: additive so it brightens what
     * it lies on rather than hiding it, and without depth writes so the two passes can share the same surface.
     */
    public static RenderType additiveGlow(ResourceLocation texture) {
        return GLOW.computeIfAbsent(texture, tex -> NativeRenderTypes.create("isorropia_matrix_glow",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        // The unlit identity Thaumcraft uses for its own matrix glow: it keeps the vertex
                        // colour and skips the modern alpha cutoff, which a glow pass must not have.
                        .setShaderState(NativeRenderStates.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                        .setTransparencyState(NativeRenderStates.LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(NativeRenderStates.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(NativeRenderStates.COLOR_WRITE)
                        .setCullState(NativeRenderStates.CULL)
                        .setLightmapState(NativeRenderStates.LIGHTMAP)
                        .setOverlayState(NativeRenderStates.OVERLAY)
                        .createCompositeState(false)));
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

package fr.wind_blade.isorropia.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.wind_blade.isorropia.Isorropia;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A small Wavefront OBJ reader, enough for the vat's own model: positions, texture coordinates, normals, named
 * groups and material names. Rendering is per group so a caller can give the water its own render type and
 * blend state, exactly as the 1.12 renderer did with renderPart("Water") and renderAllExcept("Water").
 *
 * <p>Minecraft's own model system cannot do that split: a baked OBJ model hands back every quad at once, and a
 * block entity renderer needs the two passes separated.
 */
public final class ObjMesh {
    private static final Map<ResourceLocation, ObjMesh> CACHE = new LinkedHashMap<>();

    private final Map<String, List<Face>> groups;
    private final List<String> groupNames;

    private ObjMesh(Map<String, List<Face>> groups) {
        this.groups = groups;
        this.groupNames = List.copyOf(groups.keySet());
    }

    /**
     * One face corner: a position, its texture coordinate and the face normal. Quads keep four corners, so a
     * renderer can emit them as a fan.
     */
    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }

    public record Face(List<Vertex> corners) {
    }

    public static ObjMesh get(ResourceLocation model) {
        return CACHE.computeIfAbsent(model, ObjMesh::read);
    }

    public List<String> groupNames() {
        return groupNames;
    }

    /** A read-only census of a group, used by the render probes to check what was actually parsed. */
    public String census() {
        StringBuilder text = new StringBuilder();
        for (String name : groupNames) {
            List<Face> faces = groups.get(name);
            int corners = 0;
            int quads = 0;
            int triangles = 0;
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE;
            float maxZ = -Float.MAX_VALUE;
            for (Face face : faces) {
                corners += face.corners().size();
                if (face.corners().size() == 4) {
                    quads++;
                } else if (face.corners().size() == 3) {
                    triangles++;
                }
                for (Vertex vertex : face.corners()) {
                    minX = Math.min(minX, vertex.x());
                    maxX = Math.max(maxX, vertex.x());
                    minY = Math.min(minY, vertex.y());
                    maxY = Math.max(maxY, vertex.y());
                    minZ = Math.min(minZ, vertex.z());
                    maxZ = Math.max(maxZ, vertex.z());
                }
            }
            text.append(String.format("%n%-12s faces=%d quad=%d tri=%d x[%.2f,%.2f] y[%.2f,%.2f] z[%.2f,%.2f]",
                    name, faces.size(), quads, triangles, minX, maxX, minY, maxY, minZ, maxZ));
        }
        return text.toString();
    }

    private static ObjMesh read(ResourceLocation model) {
        List<Vector3f> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        Map<String, List<Face>> groups = new LinkedHashMap<>();
        String group = "default";
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(model).open();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0) {
                    continue;
                }
                switch (parts[0]) {
                    case "v" -> positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                    case "vt" -> uvs.add(new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
                    case "vn" -> normals.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                    case "g", "o" -> {
                        group = parts.length > 1 ? parts[1] : "default";
                        groups.computeIfAbsent(group, key -> new ArrayList<>());
                    }
                    case "f" -> groups.computeIfAbsent(group, key -> new ArrayList<>()).add(face(parts, positions, uvs, normals));
                    default -> {
                        // Materials and comments do not change the geometry.
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            Isorropia.LOGGER.error("Cannot read mesh {}: {}", model, error.toString());
        }
        return new ObjMesh(groups);
    }

    private static Face face(String[] parts, List<Vector3f> positions, List<float[]> uvs, List<Vector3f> normals) {
        List<Vertex> corners = new ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) {
            String[] indices = parts[i].split("/");
            Vector3f position = positions.get(resolve(indices[0], positions.size()));
            float[] uv = indices.length > 1 && !indices[1].isEmpty() ? uvs.get(resolve(indices[1], uvs.size())) : new float[] {0.0F, 0.0F};
            Vector3f normal = indices.length > 2 && !indices[2].isEmpty() ? normals.get(resolve(indices[2], normals.size()))
                    : new Vector3f(0.0F, 1.0F, 0.0F);
            corners.add(new Vertex(position.x, position.y, position.z, uv[0], 1.0F - uv[1],
                    normal.x, normal.y, normal.z));
        }
        return new Face(corners);
    }

    /** OBJ indices are one-based, and a negative index counts back from the end of the list. */
    private static int resolve(String index, int size) {
        int value = Integer.parseInt(index);
        int resolved = value < 0 ? size + value : value - 1;
        return Math.max(0, Math.min(resolved, size - 1));
    }

    /**
     * Draws one group. Render types are built for quads, so each face is written as exactly four vertices:
     * a triangle repeats its last corner, and a larger polygon is fanned into quads the same way. Writing a
     * face as three vertices would leave the buffer assembling quads out of neighbouring triangles, which comes
     * out as a shredded model.
     */
    public void render(String group, PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        List<Face> faces = groups.get(group);
        if (faces == null) {
            return;
        }
        for (Face face : faces) {
            List<Vertex> corners = face.corners();
            if (corners.size() == 4) {
                for (Vertex corner : corners) {
                    emit(corner, poseStack, consumer, light, overlay, red, green, blue, alpha);
                }
            } else if (corners.size() == 3) {
                emit(corners.get(0), poseStack, consumer, light, overlay, red, green, blue, alpha);
                emit(corners.get(1), poseStack, consumer, light, overlay, red, green, blue, alpha);
                emit(corners.get(2), poseStack, consumer, light, overlay, red, green, blue, alpha);
                emit(corners.get(2), poseStack, consumer, light, overlay, red, green, blue, alpha);
            } else if (corners.size() > 4) {
                for (int i = 1; i + 1 < corners.size(); i++) {
                    emit(corners.get(0), poseStack, consumer, light, overlay, red, green, blue, alpha);
                    emit(corners.get(i), poseStack, consumer, light, overlay, red, green, blue, alpha);
                    emit(corners.get(i + 1), poseStack, consumer, light, overlay, red, green, blue, alpha);
                    emit(corners.get(i + 1), poseStack, consumer, light, overlay, red, green, blue, alpha);
                }
            }
        }
    }

    /** Draws every group whose name is not in {@code except}. */
    public void renderAllExcept(List<String> except, PoseStack poseStack, VertexConsumer consumer, int light,
            int overlay, float red, float green, float blue, float alpha) {
        for (String name : groupNames) {
            if (!except.contains(name)) {
                render(name, poseStack, consumer, light, overlay, red, green, blue, alpha);
            }
        }
    }

    private static void emit(Vertex vertex, PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        consumer.vertex(poseStack.last().pose(), vertex.x(), vertex.y(), vertex.z())
                .color(red, green, blue, alpha)
                .uv(vertex.u(), vertex.v())
                .overlayCoords(overlay)
                .uv2(light)
                .normal(poseStack.last().normal(), vertex.nx(), vertex.ny(), vertex.nz())
                .endVertex();
    }

    /** The model's own texture path, for callers that want to log or look it up. */
    public static ResourceLocation texture(String name) {
        return Isorropia.id("textures/block/" + name.toLowerCase(Locale.ROOT));
    }
}

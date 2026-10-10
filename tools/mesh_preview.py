"""Software renderer for an OBJ model, to check a mesh and its UVs outside the game.

    python tools/mesh_preview.py <model.obj> <texture.png> <out.png> [--camera x,y,z] [--look x,y,z] [--size WxH]

Good enough to answer "are the triangles and the texture mapping what I think they are": flat shading from the
file's normals, nearest sampling, painter's algorithm. It is deliberately not the game's renderer — it is the
reference to compare the game against.
"""
import argparse
import math
from pathlib import Path

from PIL import Image


def parse_obj(path):
    positions, uvs, normals, faces = [], [], [], []
    group = "default"
    for line in Path(path).read_text(errors="replace").splitlines():
        parts = line.split()
        if not parts:
            continue
        if parts[0] == "v":
            positions.append(tuple(float(v) for v in parts[1:4]))
        elif parts[0] == "vt":
            uvs.append((float(parts[1]), float(parts[2])))
        elif parts[0] == "vn":
            normals.append(tuple(float(v) for v in parts[1:4]))
        elif parts[0] in ("o", "g"):
            group = parts[1] if len(parts) > 1 else "default"
        elif parts[0] == "f":
            corners = []
            for token in parts[1:]:
                idx = token.split("/")
                p = int(idx[0])
                t = int(idx[1]) if len(idx) > 1 and idx[1] else None
                n = int(idx[2]) if len(idx) > 2 and idx[2] else None
                def pick(i, seq):
                    if i is None:
                        return None
                    return seq[i - 1] if i > 0 else seq[len(seq) + i]
                corners.append((pick(p, positions), pick(t, uvs), pick(n, normals)))
            faces.append((group, corners))
    return faces


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def normalise(v):
    length = math.sqrt(dot(v, v)) or 1.0
    return (v[0] / length, v[1] / length, v[2] / length)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("model")
    parser.add_argument("texture")
    parser.add_argument("out")
    parser.add_argument("--camera", default="0,4,-6")
    parser.add_argument("--look", default="0,2,0")
    parser.add_argument("--size", default="640x480")
    parser.add_argument("--no-flip-v", action="store_true", help="sample V as written in the file")
    parser.add_argument("--light", default="0.4,0.8,-0.45")
    args = parser.parse_args()

    faces = parse_obj(args.model)
    sheet = Image.open(args.texture).convert("RGBA")
    tw, th = sheet.size
    pixels = sheet.load()
    width, height = (int(v) for v in args.size.lower().split("x"))
    camera = tuple(float(v) for v in args.camera.split(","))
    look = tuple(float(v) for v in args.look.split(","))
    light = normalise(tuple(float(v) for v in args.light.split(",")))

    forward = normalise(sub(look, camera))
    right = normalise(cross(forward, (0.0, 1.0, 0.0)))
    up = cross(right, forward)
    focal = width / (2.0 * math.tan(math.radians(35.0)))

    canvas = Image.new("RGBA", (width, height), (140, 180, 240, 255))
    out = canvas.load()
    buffer = []

    def project(point):
        rel = sub(point, camera)
        x, y, z = dot(rel, right), dot(rel, up), dot(rel, forward)
        if z <= 0.05:
            return None
        return (width / 2.0 + focal * x / z, height / 2.0 - focal * y / z, z)

    for group, corners in faces:
        projected = [project(c[0]) for c in corners]
        if any(p is None for p in projected):
            continue
        depth = sum(p[2] for p in projected) / len(projected)
        buffer.append((depth, group, corners, projected))

    buffer.sort(key=lambda entry: -entry[0])
    for _, group, corners, projected in buffer:
        normal = normalise(cross(sub(corners[1][0], corners[0][0]), sub(corners[2][0], corners[0][0])))
        shade = 0.55 + 0.45 * abs(dot(normal, light))
        for i in range(1, len(corners) - 1):
            tri = (corners[0], corners[i], corners[i + 1])
            screen = (projected[0], projected[i], projected[i + 1])
            # A tiny affine texture mapper: solve the 3x3 system for the barycentric UV interpolation.
            x0, y0 = screen[0][0], screen[0][1]
            x1, y1 = screen[1][0], screen[1][1]
            x2, y2 = screen[2][0], screen[2][1]
            min_x, max_x = int(min(x0, x1, x2)), int(max(x0, x1, x2)) + 1
            min_y, max_y = int(min(y0, y1, y2)), int(max(y0, y1, y2)) + 1
            denominator = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(denominator) < 1e-6:
                continue
            uvs = [c[1] for c in tri]
            for py in range(max(min_y, 0), min(max_y, height)):
                for px in range(max(min_x, 0), min(max_x, width)):
                    w0 = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / denominator
                    w1 = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / denominator
                    w2 = 1.0 - w0 - w1
                    if w0 < 0 or w1 < 0 or w2 < 0:
                        continue
                    if any(u is None for u in uvs):
                        continue
                    u = w0 * uvs[0][0] + w1 * uvs[1][0] + w2 * uvs[2][0]
                    v = w0 * uvs[0][1] + w1 * uvs[1][1] + w2 * uvs[2][1]
                    if not args.no_flip_v:
                        v = 1.0 - v
                    tx = min(tw - 1, max(0, int(u % 1.0 * tw)))
                    ty = min(th - 1, max(0, int((v % 1.0) * th)))
                    texel = pixels[tx, ty]
                    if texel[3] == 0:
                        continue
                    alpha = texel[3] / 255.0
                    colour = tuple(int(texel[c] * shade) for c in range(3))
                    under = out[px, py]
                    out[px, py] = tuple(int(colour[c] * alpha + under[c] * (1 - alpha)) for c in range(3)) + (255,)

    canvas.save(args.out)
    print("wrote", args.out, "faces drawn:", len(buffer))


if __name__ == "__main__":
    main()

"""Generates the planar gateway's block models and blockstates from Thaumic Horizons' own icon rules.

THaumic Horizons picked a texture per frame cell and per face at render time, from the world: ``BlockGatewayPortal``
returns one of nine gateway textures, lapis or arcane stone depending on the cell's metadata and on whether the
gateway runs along X. A modern block model cannot see its neighbours, so the two facts it needs are promoted to
blockstate properties: the cell becomes ``variant`` 0..14 and the alignment becomes ``aligned``. The alignment turns
out to be exactly a quarter turn of the model - TH's aligned branch is its unaligned branch with east and north
swapped - so only fifteen models are needed and the blockstate rotates them.

The keystone receptacle is the reverse case: TH drew it with a box model in code (``ModelReceptacle``), which is a
plain list of boxes and so becomes a block model here. Only the keystone cube inside it is conditional and tinted, so
the receptacle gets an empty and a filled model and a ``filled`` state to choose between them.
"""

import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "isorropia")

# TH's BlockGatewayPortal texture fields.
TEX = {
    "CornerTR": "isorropia:block/gateway_topright",
    "CornerTL": "isorropia:block/gateway_topleft",
    "CornerBR": "isorropia:block/gateway_bottomright",
    "CornerBL": "isorropia:block/gateway_bottomleft",
    "topR": "isorropia:block/gateway_top2",
    "topL": "isorropia:block/gateway_top",
    "R": "isorropia:block/gateway_right",
    "L": "isorropia:block/gateway_left",
    "B": "isorropia:block/gateway_bottom",
    "lapiz": "minecraft:block/lapis_block",
    "stone": "thaumcraft:block/arcane_stone",
}

# TH's side numbering: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
FACES = {0: "down", 1: "up", 2: "north", 3: "south", 4: "west", 5: "east"}


def left_side(x_aligned, side):
    if side == 2:
        return "R" if x_aligned else "stone"
    if side == 3:
        return "L" if x_aligned else "lapiz"
    if side == 4:
        return "stone" if x_aligned else "L"
    if side == 5:
        return "lapiz" if x_aligned else "R"
    return "lapiz"


def right_side(x_aligned, side):
    if side == 2:
        return "L" if x_aligned else "lapiz"
    if side == 3:
        return "R" if x_aligned else "stone"
    if side == 4:
        return "lapiz" if x_aligned else "R"
    if side == 5:
        return "stone" if x_aligned else "L"
    return "lapiz"


def bottom_side(x_aligned, side):
    if side == 0:
        return "stone"
    if side == 1:
        return "lapiz"
    return "B"


def icon(variant, side, x_aligned):
    if variant < 5:
        if variant == 0:
            return {0: "lapiz", 1: "stone"}.get(
                side,
                ("CornerTR" if x_aligned else "stone") if side == 2 else
                ("CornerTL" if x_aligned else "stone") if side == 3 else
                ("stone" if x_aligned else "CornerTL") if side == 4 else
                ("stone" if x_aligned else "CornerTR") if side == 5 else
                left_side(x_aligned, side),
            )
        return left_side(x_aligned, side)
    if variant == 4:
        return {0: "stone", 1: "lapiz"}.get(
            side,
            ("CornerBR" if x_aligned else "stone") if side == 2 else
            ("CornerBL" if x_aligned else "stone") if side == 3 else
            ("stone" if x_aligned else "CornerBL") if side == 4 else
            ("stone" if x_aligned else "CornerBR") if side == 5 else
            "lapiz",
        )
    if variant == 5:
        return {0: "lapiz", 1: "stone"}.get(
            side,
            ("topR" if x_aligned else "stone") if side == 2 else
            ("topL" if x_aligned else "stone") if side == 3 else
            ("stone" if x_aligned else "topL") if side == 4 else
            ("stone" if x_aligned else "topR") if side == 5 else
            "lapiz",
        )
    if variant in (6, 7, 9):
        return bottom_side(x_aligned, side)
    if variant == 8:
        return {0: "lapiz", 1: "stone"}.get(
            side,
            ("topL" if x_aligned else "stone") if side == 2 else
            ("topR" if x_aligned else "stone") if side == 3 else
            ("stone" if x_aligned else "topR") if side == 4 else
            ("stone" if x_aligned else "topL") if side == 5 else
            "lapiz",
        )
    if variant == 10:
        return {0: "lapiz", 1: "stone"}.get(
            side,
            ("CornerTL" if x_aligned else "stone") if side == 2 else
            ("CornerTR" if x_aligned else "stone") if side == 3 else
            ("stone" if x_aligned else "CornerTR") if side == 4 else
            ("stone" if x_aligned else "CornerTL") if side == 5 else
            right_side(x_aligned, side),
        )
    if variant in (11, 12, 13):
        return right_side(x_aligned, side)
    if variant == 14:
        return {0: "stone", 1: "lapiz"}.get(
            side,
            ("CornerBL" if x_aligned else "stone") if side == 2 else
            ("CornerBR" if x_aligned else "stone") if side == 3 else
            ("stone" if x_aligned else "CornerBR") if side == 4 else
            ("stone" if x_aligned else "CornerBL") if side == 5 else
            "lapiz",
        )
    return "lapiz"


def gateway_model(variant, x_aligned):
    """One frame cell: a full cube whose six faces carry TH's six icons."""
    textures = {"particle": TEX["stone"]}
    faces = {}
    for side, face in FACES.items():
        textures[face] = TEX[icon(variant, side, x_aligned)]
        faces[face] = {"texture": "#" + face, "cullface": face}
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}],
    }


# TH's ModelReceptacle: rotation point (x, y, z), size (w, h, d), texture offset (u, v).
RECEPTACLE = [
    ("corner1", (-8, 20, 4), (4, 4, 4), (0, 0)),
    ("corner2", (-8, 20, -8), (4, 4, 4), (0, 0)),
    ("corner3", (4, 20, -8), (4, 4, 4), (0, 0)),
    ("corner4", (4, 20, 4), (4, 4, 4), (0, 0)),
    ("side1a", (-4, 8, 4), (8, 4, 4), (0, 8)),
    ("corner5", (-8, 8, 4), (4, 4, 4), (0, 0)),
    ("corner6", (-8, 8, -8), (4, 4, 4), (0, 0)),
    ("corner7", (4, 8, -8), (4, 4, 4), (0, 0)),
    ("corner8", (4, 8, 4), (4, 4, 4), (0, 0)),
    ("side1b", (-4, 8, -8), (8, 4, 4), (0, 8)),
    ("side1c", (-4, 20, -8), (8, 4, 4), (0, 8)),
    ("side1d", (-4, 20, 4), (8, 4, 4), (0, 8)),
    ("side2a", (-8, 12, -8), (4, 8, 4), (0, 16)),
    ("side2b", (4, 12, 4), (4, 8, 4), (0, 16)),
    ("side2c", (-8, 12, 4), (4, 8, 4), (0, 16)),
    ("side2d", (4, 12, -8), (4, 8, 4), (0, 16)),
    ("side3a", (-8, 8, -4), (4, 4, 8), (0, 28)),
    ("side3b", (4, 8, -4), (4, 4, 8), (0, 28)),
    ("side3c", (-8, 20, -4), (4, 4, 8), (0, 28)),
    ("side3d", (4, 20, -4), (4, 4, 8), (0, 28)),
]
KEYSTONE = ("keystone", (-4, 12, -4), (8, 8, 8), (24, 0))


def box_element(origin, size, uv, tinted):
    """A 1.7 box in modern model space, with vanilla's box unwrap as UVs."""
    x, y, z = origin
    w, h, d = size
    u, v = uv
    x0, z0 = x + 8, z + 8
    y0 = y - 8  # the TESR drew the model half a block low, which turns TH's downward y upright again
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    # Vanilla's ModelPart.Cube unwrap, in texture pixels; block models want sixteenths of the texture.
    rects = {
        "down": (u + d, v, u + d + w, v + d),
        "up": (u + d + w, v, u + d + w + w, v + d),
        "west": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "east": (u + d + w, v + d, u + d + w + d, v + d + h),
        "south": (u + d + w + d, v + d, u + d + w + d + w, v + d + h),
    }
    faces = {}
    for face, (u0, v0, u1, v1) in rects.items():
        entry = {"texture": "#all", "uv": [u0 / 4.0, v0 / 4.0, u1 / 4.0, v1 / 4.0]}
        if tinted:
            entry["tintindex"] = 0
        faces[face] = entry
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


def receptacle_model(with_keystone):
    elements = [box_element(origin, size, uv, False) for _, origin, size, uv in RECEPTACLE]
    if with_keystone:
        _, origin, size, uv = KEYSTONE
        elements.append(box_element(origin, size, uv, True))
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "isorropia:models/receptacle", "all": "isorropia:models/receptacle"},
        "elements": elements,
    }


def write(path, payload):
    full = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2)
        handle.write("\n")


def main():
    for variant in range(15):
        write("models/block/gateway_%d.json" % variant, gateway_model(variant, False))

    variants = {}
    for variant in range(15):
        variants["variant=%d,aligned=false" % variant] = {"model": "isorropia:block/gateway_%d" % variant}
        variants["variant=%d,aligned=true" % variant] = {
            "model": "isorropia:block/gateway_%d" % variant,
            "y": 90,
        }
    write("blockstates/gateway.json", {"variants": variants})

    write("models/block/keystone_receptacle.json", receptacle_model(False))
    write("models/block/keystone_receptacle_full.json", receptacle_model(True))
    write("blockstates/keystone_receptacle.json", {"variants": {
        "filled=false": {"model": "isorropia:block/keystone_receptacle"},
        "filled=true": {"model": "isorropia:block/keystone_receptacle_full"},
    }})

    # The gateway surface is drawn by its block entity renderer, so it needs a model only to exist.
    write("models/block/portal.json", {"textures": {"particle": "isorropia:misc/vortex"}})
    write("blockstates/portal.json", {"variants": {"": {"model": "isorropia:block/portal"}}})

    for name in ("gateway", "keystone_receptacle", "portal"):
        write("models/item/%s.json" % name, {"parent": "isorropia:block/%s" % name})
    write("models/item/keystone.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "isorropia:item/keystone"},
    })


if __name__ == "__main__":
    main()

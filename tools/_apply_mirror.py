"""Adds the mirrored cubes the original jelly models used.

Upstream set `mirror = true` on these parts, which flips their texture, so the port's LayerDefinitions need the
same flag on the matching builders.
"""
import re
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent / "src/main/java/fr/wind_blade/isorropia/client/model"

# Every cube of the jelly core is mirrored upstream.
path = BASE / "ModelJelly.java"
s = path.read_text(encoding="utf-8")
s = re.sub(r"addBox\((?:[^()]|\([^()]*\))*?\)(?!\.mirror)", lambda m: m.group(0) + ".mirror()", s)
path.write_text(s, encoding="utf-8")

# The rabbit's body and head are mirrored; its eyes are not.
path = BASE / "ModelJellyRabbit.java"
s = path.read_text(encoding="utf-8")
for offset in ("texOffs(0, 0)", "texOffs(32, 0)"):
    s = re.sub(r"(CubeListBuilder\.create\(\)\." + re.escape(offset)
               + r"\.addBox\((?:[^()]|\([^()]*\))*?\))(?!\.mirror)", r"\1.mirror()", s)
path.write_text(s, encoding="utf-8")

print("mirrored cubes applied:",
      path.parent.joinpath("ModelJelly.java").read_text(encoding="utf-8").count(".mirror()"),
      "in ModelJelly,",
      path.read_text(encoding="utf-8").count(".mirror()"),
      "in ModelJellyRabbit")

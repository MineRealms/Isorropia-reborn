"""Import the 1.12 Isorropia assets into the 1.20.1 layout.

Re-runnable: it only writes files it owns (textures, sounds, generated item models) and the base
en_us.json; hand-written blockstates/models elsewhere in the tree are left alone.

    python tools/import_legacy_assets.py [--legacy ../Isorropia_kedition]
"""
import argparse
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEST = ROOT / "src/main/resources/assets/isorropia"

# 1.12 texture dir -> 1.20 dir. textures/models goes to entity/: TC4R stitches every namespace's
# textures/models/ into the block atlas, and these are renderer skins, not sprites.
TEXTURE_DIRS = {
    "aspects": "aspects", "blocks": "block", "items": "item", "models": "entity", "entity": "entity",
    "fx": "fx", "gui": "gui", "misc": "misc", "research": "research",
}
# fames/corpus are native TC4 aspects; TC4R ships their art.
SKIP_TEXTURES = {"aspects/fames.png", "aspects/corpus.png"}

LANG_RULES = [
    (re.compile(r"^tile\.isorropia\.(.+)\.name$"), r"block.isorropia.\1"),
    (re.compile(r"^item\.isorropia\.jelly\.name$"), r"item.isorropia.jelly.aspect"),
    (re.compile(r"^item\.isorropia\.(.+)\.name$"), r"item.isorropia.\1"),
    (re.compile(r"^entity\.(.+)\.name$"), r"entity.isorropia.\1"),
]
DROP_LANG = re.compile(r"^(tc\.aspect\.|item\.amuletMirror|item\.ringMirror|ir\.mirrorLink|item\.jarredSoul|"
                       r"entity\.dope_squid|research\.DOPESQUID)")


def is_png(path: Path) -> bool:
    with path.open("rb") as handle:
        return handle.read(8) == b"\x89PNG\r\n\x1a\n"


def import_textures(src: Path) -> list[str]:
    skipped = []
    for file in sorted((src / "textures").rglob("*")):
        if not file.is_file():
            continue
        rel = file.relative_to(src / "textures").as_posix()
        if rel in SKIP_TEXTURES:
            continue
        if file.suffix == ".png" and not is_png(file):
            skipped.append(rel)
            continue
        folder, _, name = rel.partition("/")
        target = DEST / "textures" / TEXTURE_DIRS[folder] / name.lower()
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(file, target)
        if folder == "research" and name.startswith("cat_"):
            # Item models need the icon inside the block/item atlas.
            icon = DEST / "textures/item/cat" / name.lower()
            icon.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(file, icon)
    return skipped


def import_sounds(src: Path) -> None:
    shutil.copyfile(src / "sounds.json", DEST / "sounds.json")
    (DEST / "sounds").mkdir(parents=True, exist_ok=True)
    for file in (src / "sounds").glob("*.ogg"):
        shutil.copyfile(file, DEST / "sounds" / file.name.lower())


def convert_texture_ref(ref: str) -> str:
    namespace, _, path = ref.partition(":")
    folder, _, name = path.partition("/")
    if namespace != "isorropia":
        return ref
    if folder == "research" and name.startswith("cat_"):
        return f"isorropia:item/cat/{name.lower()}"
    return f"isorropia:{TEXTURE_DIRS.get(folder, folder)}/{name.lower()}"


def import_item_models(src: Path) -> None:
    out = DEST / "models/item"
    out.mkdir(parents=True, exist_ok=True)
    for file in sorted((src / "models/item").glob("*.json")):
        try:
            model = json.loads(file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue  # soul_jar.json has a trailing comma; it is a parent-only model anyway
        if model.get("parent") != "item/generated":
            continue  # 3D/inherited item models are written by hand
        model["parent"] = "minecraft:item/generated"
        model["textures"] = {k: convert_texture_ref(v) for k, v in model["textures"].items()}
        (out / file.name.lower()).write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


def convert_lang(src: Path) -> dict:
    result = {}
    for raw in (src / "lang/en_us.lang").read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        if DROP_LANG.match(key):
            continue
        for pattern, replacement in LANG_RULES:
            if pattern.match(key):
                key = pattern.sub(replacement, key)
                break
        result[key] = value
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--legacy", default=str(ROOT.parent / "Isorropia_kedition"))
    parser.add_argument("--lang", action="store_true",
                        help="also (over)write en_us.json from the legacy .lang; it is hand-maintained afterwards")
    args = parser.parse_args()
    src = Path(args.legacy) / "src/main/resources/assets/isorropia"
    skipped = import_textures(src)
    import_sounds(src)
    import_item_models(src)
    if args.lang:
        lang_file = DEST / "lang/en_us.json"
        lang_file.parent.mkdir(parents=True, exist_ok=True)
        lang_file.write_text(json.dumps(convert_lang(src), indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("skipped invalid textures:", skipped)


if __name__ == "__main__":
    main()

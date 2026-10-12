"""Runs the client probes in one session and checks what each must report.

    python tools/client_probe.py start      # first, so the bridge is up
    python tools/client_sweep.py            # PASS/FAIL per step; exit code 1 if anything failed
    python tools/client_sweep.py 52 57      # only the steps whose probe names start with these

Each step runs one probe through client_probe.py and looks for substrings that only a working feature prints.
Steps that start something the server must answer (a key press, a held beam) are followed by a pause and a
separate checking probe, because probe bodies run on the server thread and nothing happens until they return.
"""
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROBES = ROOT / "probes" / "client"
FAILURE_MARKERS = ("ERROR", "Exception", "FAILED", "Compilation failed")

# (probe file stem, substrings that must all appear, seconds to wait afterwards)
STEPS = [
    ("00_reset", [], 2),
    ("00_reset_mode", ["alive=true mode=CREATIVE"], 1),
    ("10_creature_lineup", [], 1),
    ("20_effigy_states", [], 1),
    ("21_clean_vat_scene", [], 1),
    ("22_matrix_scene", ["active=true"], 1),
    ("23_jar_and_beacon", [], 1),
    ("24_infusion_visual", [], 1),
    ("25_lens_wheel", [], 1),
    ("26_brain_equip", [], 1),
    ("27_lens_air", [], 1),
    ("31_lens_entities", [], 1),
    ("32_lens_ordo", [], 1),
    ("33_lens_envy", [], 1),
    ("36_block_aspects2", [], 1),
    ("43_lens_drop_probe", [], 1),
    ("44_lens_ordo_drop2", [], 1),
    ("45_fingers_open", ["menu=MorphicFingersMenu"], 1),
    ("46_fingers_recipe", [], 1),
    ("53_fingers_take", [], 1),
    ("49_keys", ["key.isorropia.lens x1", "key.isorropia.fingers x1", "J held: wheel=true",
                 "J released: wheel=false"], 2),
    ("50_fingers_key_check", ["client screen=MorphicFingersScreen", "server menu=MorphicFingersMenu"], 1),
    ("47_beacon_setup", ["bound=true"], 3),
    ("48_beacon_kill", ["after: alive=true", "bound=false diamondSkin=true fingers=false", "vatHoldsPlayer=true"], 2),
    ("51_vat_exit", ["stillHeld=false", "invulnerable=false", "diamondSkin=true"], 1),
    ("52_containment_hold", ["install=INSTALLED", "focus=isorropia:focus_containment"], 7),
    ("53_containment_check", ["holds=minecraft:pig", "usingItem=false"], 1),
    ("55_containment_rules", ["canContain wither=false ironGolem=true", "emptyJar=false"], 4),
    ("53_containment_check", ["usingItem=false"], 1),
    ("56_matrix_flow", ["tap 1 (off -> on): true active=true", "tap 2 (nothing to infuse): false active=true",
                        "matrix broken: active=false startUp=0.0 vatStillFormed=true"], 1),
    ("57_soul_jar_flow", ["nameMatches=true", "customName=false", "noAiInJar=false", "vatHolds=pig wardedBack=1",
                          "jarGone=true"], 1),
    ("59_gravekeeper_beam", ["distance=3.0"], 1),
    # Thaumic Horizons content beyond Isorropia's own: the four foci and the vis dynamo.
    ("61_illumination", ["install=INSTALLED"], 3),
    ("62_illumination_check", ["mote@", "emission=14", "recipe=true", "profile=true"], 1),
    ("62_clear_motes", ["motes cleared="], 1),
    ("63_liquefaction", ["install=INSTALLED"], 6),
    ("64_liquefaction_check", ["target_block=minecraft:water"], 1),
    ("65_liquefaction_smelt", ["target_block=minecraft:air", "minecraft:iron_ingot"], 1),
    ("66_liquefaction_item", ["install=INSTALLED"], 6),
    ("67_liquefaction_item_check", ["minecraft:iron_ingot"], 1),
    ("68_liquefaction_nuggets", ["install=INSTALLED"], 6),
    ("69_liquefaction_nuggets_check", ["minecraft:iron_nugget"], 1),
    ("70_disintegration", ["install=INSTALLED"], 8),
    ("71_disintegration_check", ["target_block=minecraft:air", "thaumcraft:crystal_essence"], 1),
    ("72_disintegration_catalog", ["profile=true"], 1),
    ("73_disintegration_item", ["install=INSTALLED"], 6),
    ("74_disintegration_item_check", ["thaumcraft:crystal_essence"], 1),
    ("75_disintegration_creature", ["install=INSTALLED"], 6),
    ("76_disintegration_creature_check", ["minecraft:iron_golem hp="], 1),
    ("77_animation", ["install=INSTALLED"], 5),
    ("78_animation_check", ["golems=1", "state=minecraft:gold_block"], 1),
    ("79_animation_catalog", ["profile=true"], 1),
    ("80_animation_upgrades", ["install=INSTALLED"], 3),
    ("81_animation_upgrades_check", ["berserk=true detonation=true"], 1),
    ("82_animation_settle", [], 3),
    ("83_animation_settle_check", ["at=minecraft:dirt"], 1),
    ("84_vis_dynamo", ["be=true"], 1),
    ("85_vis_dynamo_click", [], 3),
    ("86_vis_dynamo_check", ["supplying=true", "drain1=5"], 1),
    ("87_vis_dynamo_release", [], 3),
    ("88_vis_dynamo_release_check", ["supplying=false"], 1),
]


def run(stem):
    out = subprocess.run([sys.executable, str(ROOT / "tools" / "client_probe.py"), "exec", str(PROBES / (stem + ".java"))],
                         capture_output=True, text=True, timeout=120, cwd=ROOT)
    return (out.stdout + out.stderr).strip()


def main():
    only = sys.argv[1:]
    failures = 0
    for stem, expected, pause in STEPS:
        if only and not any(stem.startswith(prefix) for prefix in only):
            continue
        try:
            output = run(stem)
        except subprocess.TimeoutExpired:
            output = "TIMEOUT"
        missing = [text for text in expected if text not in output]
        broken = [marker for marker in FAILURE_MARKERS if marker in output] + (["TIMEOUT"] if output == "TIMEOUT" else [])
        ok = not missing and not broken
        failures += 0 if ok else 1
        print(("PASS " if ok else "FAIL ") + stem)
        if not ok:
            for line in output.splitlines()[:8]:
                print("      | " + line[:200])
            if missing:
                print("      missing: " + "; ".join(missing))
        time.sleep(pause)
    print(f"{failures} failed" if failures else "all passed")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())

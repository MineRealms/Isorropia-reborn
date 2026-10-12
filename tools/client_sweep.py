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
    ("57_soul_jar_flow", ["soulJar=Jarred Pig", "customName=false", "noAiInJar=false", "vatHolds=pig wardedBack=1",
                          "jarGone=true"], 1),
    ("59_gravekeeper_beam", ["distance=3.0"], 1),
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

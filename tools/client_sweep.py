"""Runs the client probes in one session and checks what each must report.

    python tools/client_probe.py start      # first, so the bridge is up
    python tools/client_sweep.py            # PASS/FAIL per step; exit code 1 if anything failed
    python tools/client_sweep.py 52 57      # only the steps whose probe names start with these

Each step runs one probe through client_probe.py and looks for substrings that only a working feature prints.
Steps that start something the server must answer (a key press, a held beam) are followed by a pause and a
separate checking probe, because probe bodies run on the server thread and nothing happens until they return.
"""
import os
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
    ("52_containment_hold", ["install=INSTALLED", "focus=isorropia:focus_containment"], 9),
    # While the key is held vanilla retries the use every few ticks, so "using" flickers; the jar is the evidence.
    ("53_containment_check", ["holds=minecraft:pig"], 1),
    ("55_containment_rules", ["canContain wither=false ironGolem=true", "emptyJar=false"], 4),
    ("53_containment_check", ["pigs nearby=1"], 1),
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
    # The nuggets are a 45% roll per rank, so only the smelt itself is certain.
    ("69_liquefaction_nuggets_check", ["minecraft:iron_ingot"], 1),
    # The beam has to start, reach the ore and break it; eight ticks was often not enough for the drops to exist.
    ("70_disintegration", ["install=INSTALLED"], 20),
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
    ("89_essentia_dynamo", ["filled=32", "dynamo=true", "victus=[TERRA, AQUA]",
                            "humanus=[AER, TERRA, IGNIS, AQUA, ORDO, PERDITIO]"], 4),
    ("90_essentia_dynamo_check", ["offered ignis=12", "aer=0", "drain1=12 drain2=0"], 1),
    ("91_node_monitor", ["set=true", "monitor=true survives=true"], 2),
    ("92_node_monitor_drain", ["healthy: powered=false signal=0", "drained=true"], 2),
    ("93_node_monitor_check", ["critical: powered=true signal=15 strong=15"], 1),
    ("94_node_monitor_orphan", ["node removed"], 2),
    ("95_node_monitor_orphan_check", ["monitorGone=true", "isorropia:node_monitor"], 1),
    # Shard Transmutation: six heated crucibles each turn a balanced shard into one primal shard.
    ("96_shard_trans", ["known=true", "fire_shard=true", "water_shard=true", "air_shard=true",
                        "earth_shard=true", "order_shard=true", "entropy_shard=true", "made=6/6"], 1),
    # Alchemite: a charge whose blast turns blocks into crystallized essentia and scars aura nodes.
    ("97_alchemite", ["block=isorropia:alchemite", "nodes=8", "entity=true"], 5),
    ("98_alchemite_check", ["alchemiteLeft=false", "plainNodes=0", "stoneItems=0", "essence=true"], 1),
    ("99_alchemite_dispense", ["powered=true"], 2),
    ("100_alchemite_dispense_check", ["dispenserEmpty=true", "primed=1", "at=161, 310, 100"], 1),
    # Ethereal Shard: essence sets its capacity, the network fills it, and a wand draws it back out.
    ("101_ethereal_shard", ["known=true", "block=true", "item=true", "recipe=true", "be=TileEtherealShard",
                            "use=CONSUME", "capacity=12", "essenceLeft=0", "stored=5", "wandAer=0"], 4),
    ("102_ethereal_shard_draw", ["refilled=true", "network=true", "clientRefilled=true"], 3),
    ("103_ethereal_shard_check", ["wandGained=true", "shardSpent=true", "capacity=12", "drops=1", "dropCount=3",
                                  "dropAspect=aer"], 1),
    ("104_ethereal_shard_edges", ["rejected=true", "zeroRetained=true", "roundtrip=true", "legalStacks=true",
                                  "monitorCritical=true", "noEarlyDrops=true", "actualAer=130", "actualFire=1", "actualBlocks=1"], 1),
    ("105_ethereal_shard_empty_hold", ["empty=true"], 2),
    ("106_ethereal_shard_delayed_fill", ["waiting=true", "delayedFill=3"], 2),
    ("107_ethereal_shard_delayed_check", ["delayedGained=300", "remaining=0"], 1),
    ("109_ethereal_shard_compound", ["compoundNetworks=2", "boundary100=true", "fractionalRestored=true"], 4),
    ("110_ethereal_shard_compound_check", ["compoundCharged=true", "missingChannelBlocked=true"], 1),
    # Vis Dynamo selection screen: server button, client screen click, data slot back to the client.
    ("114_vis_dynamo_gui", ["menu=VisDynamoMenu", "button=true", "ignisOff=true", "aerOn=true", "offerIgnis=0",
                            "offerAer=5", "paidIgnis=0", "paidAer=40"], 2),
    ("115_vis_dynamo_gui_screen", ["screen=VisDynamoScreen", "ignisShownOff=true", "clicked=true"], 1),
    ("116_vis_dynamo_gui_check", ["aerOff=true", "clientAerOff=true"], 1),
    # A wand held against a machine does not also run its focus; the control proves the focus does run otherwise.
    ("117_focus_hold_control", ["install=INSTALLED", "hooked=true"], 2),
    ("118_focus_hold_machine", ["controlFires=true"], 3),
    ("119_focus_hold_check", ["using=true", "holding=true", "charging=true", "focusSuppressed=true"], 1),
    ("120_focus_hold_lookaway_check", ["released=true", "clientReleased=true"], 1),
    # Illumination: dye recolouring and TH's invisible, wisp-throwing motes.
    ("121_illumination_dye", ["defaultColour=11", "redColour=1", "redName=item.isorropia.focus_illumination.1",
                              "lastWins=15", "strayRejected=true", "twoFociRejected=true"], 1),
    ("122_illumination_light", ["invisible=5", "tiles=5"], 3),
    ("123_illumination_light_check", ["clientTiles=5", "wispsNear=true"], 1),
    # Containment slow upgrade.
    ("124_containment_slow", ["install=INSTALLED"], 3),
    ("125_containment_slow_check", ["slowed=true", "amplifier=0", "free=true"], 1),
    # Shared migration smoke: registry completeness, data writes, effects, menus and all item renderers.
    ("271_dual_boot", ["DUAL_BOOT_OK", "menu=VisDynamoMenu"], 1),
    ("272_dual_components", ["DUAL_COMPONENTS_OK", "instantTicks=1", "menu=BloodInfuserMenu"], 1),
    ("273_dual_item_icons", ["DUAL_ITEM_ICONS_OK count=61"], 1),
    ("274_dual_empty_hand", ["DUAL_EMPTY_HAND_OK menu=VisDynamoMenu"], 1),
]


def run(stem):
    out = subprocess.run([sys.executable, str(ROOT / "tools" / "client_probe.py"), "exec", str(PROBES / (stem + ".java"))],
                         capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=120, cwd=ROOT,
                         env={**os.environ, "PYTHONIOENCODING": "utf-8"})
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

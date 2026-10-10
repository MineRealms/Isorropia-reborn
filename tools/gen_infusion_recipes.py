"""Writes the vat's creature and self infusion data from the 1.12 registrations.

Re-runnable: it owns every file under data/isorropia/isorropia/{creature,self}_infusions.
Run from the project root: python tools/gen_infusion_recipes.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CREATURE = ROOT / "src/main/resources/data/isorropia/isorropia/creature_infusions"
SELF = ROOT / "src/main/resources/data/isorropia/isorropia/self_infusions"


def item(name, count=1):
    return {"item": name} if count == 1 else {"item": name, "count": count}


def tag(name, count=1):
    return {"tag": name} if count == 1 else {"tag": name, "count": count}


def potion(effect):
    return {"item": "minecraft:potion", "nbt": '{Potion:"minecraft:%s"}' % effect}


CREATURE_RECIPES = {
    "jelly_rabbit": {
        "kind": "specie", "research": "isorropia:jellyrabbit", "target": {"entity": "minecraft:rabbit"},
        "result": "isorropia:jelly_rabbit", "hook": "isorropia:jelly_rabbit",
        "components": [item("minecraft:slime_block"), item("thaumcraft:salis_mundus", 2),
                       item("thaumcraft:crystal_essence")],
        "aspects": {"thaumcraft:auram": 30, "thaumcraft:permutatio": 30},
        "instability": 2, "vis": {"aqua": 50},
    },
    "ore_boar": {
        "kind": "specie", "research": "isorropia:oreboar", "target": {"entity": "minecraft:pig"},
        "result": "isorropia:ore_pig", "hook": "isorropia:ore_pig",
        "components": [item("minecraft:furnace"), tag("forge:ores"), item("thaumcraft:fire_shard"),
                       item("thaumcraft:earth_shard")],
        "aspects": {"thaumcraft:fames": 125, "thaumcraft:ignis": 250,
                    "thaumcraft:metallum": 250, "thaumcraft:terra": 125},
        "instability": 4, "vis": {"terra": 50},
    },
    "taintfeeder": {
        "kind": "specie", "research": "isorropia:taintfeeder", "target": {"entity": "minecraft:pig"},
        "result": "isorropia:taint_pig",
        "components": [item("thaumcraft:bottled_taint"), item("thaumcraft:silverwood_log", 2),
                       item("thaumcraft:void_jar")],
        "aspects": {"thaumcraft:fames": 100, "thaumcraft:herba": 50, "thaumcraft:victus": 50,
                    "thaumcraft:vitium": 45},
        "instability": 4,
    },
    "scholars_chicken": {
        "kind": "specie", "research": "isorropia:scholarschicken", "target": {"entity": "minecraft:chicken"},
        "result": "isorropia:scholar_chicken",
        "components": [item("minecraft:black_dye"), item("minecraft:shears")],
        "aspects": {"thaumcraft:sensus": 60, "thaumcraft:permutatio": 30, "thaumcraft:tenebrae": 60},
        "instability": 2,
    },
    "chromatic_sheep": {
        "kind": "specie", "research": "isorropia:chromaticsheep", "target": {"entity": "minecraft:sheep"},
        "result": "isorropia:chromatic_sheep",
        "components": [item("minecraft:red_dye"), item("minecraft:green_dye"), item("minecraft:blue_dye")],
        "aspects": {"thaumcraft:sensus": 60, "thaumcraft:permutatio": 30, "thaumcraft:tenebrae": 60},
        "instability": 2,
    },
    "gravekeeper": {
        "kind": "specie", "research": "isorropia:gravekeeperinfusion", "target": {"entity": "minecraft:cat"},
        "result": "isorropia:gravekeeper",
        "components": [item("minecraft:bone"), item("minecraft:gold_block"), item("thaumcraft:amber"),
                       item("thaumcraft:silverwood_log")],
        "aspects": {"thaumcraft:lux": 100, "thaumcraft:exanimis": 60, "thaumcraft:ordo": 80},
        "instability": 6, "celestial": {"body": "sun", "aura": 50},
    },
    "chocolate_cow": {
        "kind": "specie", "research": "isorropia:chococow", "target": {"entity": "minecraft:cow"},
        "result": "isorropia:chocolate_cow",
        "components": [item("minecraft:brown_dye", 2), item("minecraft:sugar", 2)],
        "aspects": {"thaumcraft:corpus": 30, "thaumcraft:permutatio": 10},
        "instability": 2,
    },
    "golden_chicken": {
        "kind": "specie", "research": "isorropia:goldchicken", "target": {"entity": "minecraft:chicken"},
        "result": "isorropia:golden_chicken",
        "components": [item("minecraft:golden_apple", 2), item("thaumcraft:crucible"),
                       item("thaumcraft:thaumium_ingot")],
        "aspects": {"thaumcraft:fabrico": 45, "thaumcraft:lucrum": 30, "thaumcraft:metallum": 30},
        "instability": 2,
    },
    "mooshroom": {
        "kind": "specie", "research": "isorropia:monsterexchange", "target": {"entity": "minecraft:cow"},
        "result": "minecraft:mooshroom",
        "components": [item("minecraft:brown_mushroom", 2), item("minecraft:red_mushroom", 2)],
        "aspects": {"thaumcraft:bestia": 10, "thaumcraft:herba": 30},
        "instability": 4,
    },
    "guardian_panther": {
        "kind": "specie", "research": "isorropia:guardian_panther", "target": {"entity": "minecraft:cat"},
        "result": "isorropia:guardian_panther",
        "components": [potion("strong_strength"), item("minecraft:saddle")],
        "aspects": {"thaumcraft:bestia": 30, "thaumcraft:telum": 30},
        "instability": 2,
    },
    "nether_hound": {
        "kind": "specie", "research": "isorropia:nether_hound", "target": {"entity": "minecraft:wolf"},
        "result": "isorropia:nether_hound",
        "components": [item("minecraft:lava_bucket"), item("minecraft:blaze_rod", 2),
                       item("thaumcraft:fire_shard")],
        "aspects": {"thaumcraft:ignis": 30, "thaumcraft:telum": 30, "thaumcraft:permutatio": 30},
        "instability": 2,
    },
    "self_shearing": {
        "kind": "curative", "research": "isorropia:selfshearing", "target": {"type": "shearable"},
        "components": [item("minecraft:shears"), item("minecraft:comparator")],
        "aspects": {"thaumcraft:instrumentum": 60, "thaumcraft:machina": 30},
        "instability": 2,
    },
    "instilled_fidelity": {
        "kind": "curative", "research": "isorropia:instilledfidelity", "target": {"type": "tameable"},
        "components": [tag("forge:crops", 3)],
        "aspects": {"thaumcraft:bestia": 30, "thaumcraft:cognitio": 30},
        "instability": 2,
    },
    "quicksilver_limbs": {
        "kind": "organ", "organ": "muscle", "research": "isorropia:creatureinfusions",
        "target": {"type": "any"}, "aspects": {},
        "modifiers": [
            {"attribute": "minecraft:generic.movement_speed", "uuid": "b3f15142-2b27-11eb-adc1-0242ac120002",
             "name": "QUICKSILVER_SPEED", "amount": 0.08},
            {"attribute": "forge:swim_speed", "uuid": "c1719976-2b27-11eb-adc1-0242ac120002",
             "name": "QUICKSILVER_SWIM_SPEED", "amount": 0.2},
        ],
    },
    "ender_heart": {
        "kind": "organ", "organ": "heart", "research": "isorropia:creatureinfusions",
        "target": {"type": "any"},
        "components": [item("minecraft:egg", 2), item("minecraft:ender_pearl")],
        "aspects": {"thaumcraft:corpus": 60, "thaumcraft:motus": 60, "thaumcraft:alienis": 60},
    },
    "shock_skin": {
        "kind": "organ", "organ": "skin", "research": "isorropia:creatureinfusions",
        "target": {"type": "any"},
        "components": [item("minecraft:quartz", 2), item("minecraft:redstone"), item("thaumcraft:air_shard")],
        "aspects": {"thaumcraft:potentia": 30, "thaumcraft:telum": 30, "thaumcraft:aer": 5},
    },
    "awakened_blood": {
        "kind": "organ", "organ": "blood", "research": "isorropia:creatureinfusions",
        "target": {"type": "any"},
        "components": [item("minecraft:glistering_melon_slice", 2), item("minecraft:golden_apple"),
                       item("thaumcraft:silverwood_log")],
        "aspects": {"thaumcraft:victus": 50, "thaumcraft:cognitio": 50},
    },
    "portability": {
        "kind": "organ", "organ": "void", "research": "isorropia:creatureinfusions",
        "target": {"type": "any"},
        "components": [item("minecraft:ender_pearl"), item("thaumcraft:warded_jar")],
        "aspects": {"thaumcraft:vacuos": 50, "thaumcraft:motus": 50},
    },
    "diamond_skin": {
        "kind": "organ", "organ": "skin", "research": "isorropia:creatureinfusions",
        "target": {"type": "any"},
        "components": [item("minecraft:diamond", 2), item("minecraft:leather")],
        "aspects": {"thaumcraft:bestia": 10, "thaumcraft:tutamen": 30, "thaumcraft:vitreus": 30},
        "modifiers": [
            {"attribute": "minecraft:generic.armor", "uuid": "6bc2ebe8-2b1c-11eb-adc1-0242ac120002",
             "name": "DIAMOND_SKIN", "amount": 20.0},
            {"attribute": "minecraft:generic.armor_toughness", "uuid": "21afc412-2b1d-11eb-adc1-0242ac120002",
             "name": "DIAMOND_SKIN_TOUGHNESS", "amount": 8.0},
        ],
    },
}

SELF_RECIPES = {
    "quicksilver_limb": {
        "infusion": "isorropia:quicksilver_limb", "research": "isorropia:selfinfusion", "instability": 4,
        "components": [potion("strong_swiftness"), item("thaumcraft:thaumium_ingot", 2),
                       item("thaumcraft:quicksilver", 2)],
        "aspects": {"thaumcraft:motus": 100, "thaumcraft:machina": 100, "thaumcraft:volatus": 100},
    },
    "morphic_fingers": {
        "infusion": "isorropia:morphic_fingers", "research": "isorropia:morphicfingers", "instability": 6,
        "components": [item("thaumcraft:arcane_worktable"), item("thaumcraft:thaumium_ingot"),
                       item("thaumcraft:salis_mundus"), item("thaumcraft:thaumium_ingot")],
        "aspects": {"thaumcraft:instrumentum": 50, "thaumcraft:fabrico": 50, "thaumcraft:humanus": 50,
                    "thaumcraft:permutatio": 30},
    },
    "awakened_blood": {
        "infusion": "isorropia:awakened_blood", "research": "isorropia:selfinfusion", "instability": 5,
        "components": [item("minecraft:golden_apple"), item("minecraft:glistering_melon_slice", 4),
                       item("thaumcraft:zombie_brain")],
        "aspects": {"thaumcraft:victus": 200, "thaumcraft:cognitio": 50},
    },
    "diamond_skin": {
        "infusion": "isorropia:diamond_skin", "research": "isorropia:selfinfusion", "instability": 6,
        "components": [item("minecraft:leather_helmet"), item("minecraft:diamond", 2),
                       item("minecraft:leather_chestplate"), item("minecraft:diamond_block", 2),
                       item("minecraft:leather_leggings"), item("minecraft:leather_boots")],
        "aspects": {"thaumcraft:tutamen": 30, "thaumcraft:vitreus": 30, "thaumcraft:humanus": 30},
    },
    "silver_heart": {
        "infusion": "isorropia:silver_heart", "research": "isorropia:silverheart", "instability": 7,
        "components": [item("thaumcraft:silverwood_sapling"), item("thaumcraft:silverwood_log", 2),
                       item("thaumcraft:shimmerleaf")],
        "aspects": {"thaumcraft:ordo": 50, "thaumcraft:victus": 50, "thaumcraft:permutatio": 30},
    },
    "synth_skin": {
        "infusion": "isorropia:synth_skin", "research": "isorropia:synthskin", "instability": 6,
        "components": [item("minecraft:oak_leaves"), item("minecraft:oak_sapling"), item("minecraft:poppy"),
                       item("minecraft:vine")],
        "aspects": {"thaumcraft:herba": 100, "thaumcraft:fames": 100, "thaumcraft:lux": 100,
                    "thaumcraft:humanus": 16},
    },
    "amphibious": {
        "infusion": "isorropia:amphibious", "research": "isorropia:amphibious", "instability": 7,
        "components": [item("minecraft:cod", 2), potion("long_water_breathing")],
        "aspects": {"thaumcraft:aqua": 100, "thaumcraft:aer": 100, "thaumcraft:victus": 50,
                    "thaumcraft:permutatio": 16},
    },
    "warped_tumor": {
        "infusion": "isorropia:warped_tumor", "research": "isorropia:warpedtumor", "instability": 12,
        "components": [item("thaumcraft:tainted_flesh"), item("thaumcraft:bath_salts", 2),
                       item("minecraft:nether_star", 2), item("minecraft:obsidian")],
        "aspects": {"thaumcraft:vitium": 100, "thaumcraft:alienis": 75, "thaumcraft:vinculum": 50,
                    "thaumcraft:corpus": 45, "thaumcraft:permutatio": 30},
    },
    "spider_climb": {
        "infusion": "isorropia:spider_climb", "research": "isorropia:spiderclimb", "instability": 8,
        "components": [item("minecraft:cobweb", 4), item("thaumcraft:enchanted_fabric", 2),
                       item("minecraft:ladder", 2)],
        "aspects": {"thaumcraft:bestia": 50, "thaumcraft:motus": 75, "thaumcraft:permutatio": 50},
    },
    "chameleon_skin": {
        "infusion": "isorropia:chameleon_skin", "research": "isorropia:chameleonskin", "instability": 7,
        "components": [item("minecraft:spider_eye"), item("minecraft:red_dye"), item("minecraft:blue_dye"),
                       item("minecraft:yellow_dye"), item("minecraft:black_dye"), item("minecraft:white_dye")],
        "aspects": {"thaumcraft:sensus": 75, "thaumcraft:permutatio": 50, "thaumcraft:vacuos": 50},
    },
}


def write(directory, recipes):
    directory.mkdir(parents=True, exist_ok=True)
    for name, body in recipes.items():
        (directory / (name + ".json")).write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    write(CREATURE, CREATURE_RECIPES)
    write(SELF, SELF_RECIPES)
    print("%d creature infusions, %d self infusions" % (len(CREATURE_RECIPES), len(SELF_RECIPES)))

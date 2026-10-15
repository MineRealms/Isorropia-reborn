#!/usr/bin/env python3
"""Static checks for Isorropia's Thaumcraft (TC4R) datapack data.

Checks research parents/siblings/prerequisites, recipe_ids -> recipe files, recipe research keys,
item/block/tag ids (ours from BlocksIS/ItemsIS, TC4R from its jar, vanilla from the 1.20.1
client-extra jar the build produces), aspect ids, entity scan identities, icon textures and
research lang keys.

Usage: python tools/validate_tc_data.py [--tc4r-jar PATH] [--vanilla-jar PATH]
Exit code 1 when any error is found.
"""
import argparse
import glob
import json
import os
import re
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data', 'isorropia')
ASSETS = os.path.join(RES, 'assets', 'isorropia')
JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'fr', 'wind_blade', 'isorropia', 'common')
DEFAULT_TC4R = os.path.join(ROOT, 'local-repo', 'dev', 'tc4port', 'thaumcraft-forge', '0.1.0-20732',
                            'thaumcraft-forge-0.1.0-20732.jar')
# The 1.20.1 client jar the vanilla checks read. The legacyforge node runs on ModDevGradle, which
# writes it as a NeoFormRuntime artifact under the node's build directory; the older ForgeGradle 6
# layout kept it in the Gradle cache, and NFRT's own cache holds the pre-strip client jar. All three
# carry the same assets tree, so prefer what the build actually produced and fall back from there.
VANILLA_CANDIDATES = (
    os.path.join(ROOT, 'versions', '1.20.1-forge', 'build', 'moddev', 'artifacts'),
    os.path.expanduser('~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1'),
    os.path.expanduser('~/.gradle/caches/neoformruntime/artifacts'),
)


def find_vanilla_jar():
    for directory in VANILLA_CANDIDATES:
        matches = sorted(glob.glob(os.path.join(directory, 'client-extra*.jar'))
                         or glob.glob(os.path.join(directory, 'minecraft_1.20.1_client.jar')))
        if matches:
            return matches[-1]
    # Nothing built yet: name the build-output path so the warning points at the missing artifact.
    return os.path.join(VANILLA_CANDIDATES[0], 'client-extra-1.20.1.jar')

ENTRY_FIELDS = {'key', 'forge:conditions', 'category', 'aspects', 'display_column', 'display_row', 'complexity', 'icon',
                'flags', 'labels', 'parents', 'hidden_parents', 'prerequisite_selectors', 'siblings', 'object_triggers',
                'entity_triggers', 'aspect_triggers', 'warp', 'pages'}
PAGE_FIELDS = {'type', 'value', 'prerequisite', 'aspects', 'compound_blueprint', 'recipe_link_policy', 'recipe_ids',
               'external_link'}
FLAGS = {'SPECIAL', 'STUB', 'LOST', 'CONCEALED', 'HIDDEN', 'VIRTUAL', 'AUTO_UNLOCK', 'ROUND', 'SECONDARY'}
RECIPE_PAGES = {'CRUCIBLE_CRAFTING': {'thaumcraft:crucible'},
                'ARCANE_CRAFTING': {'thaumcraft:arcane_shaped', 'thaumcraft:arcane_shapeless'},
                'INFUSION_CRAFTING': {'thaumcraft:infusion'},
                'INFUSION_ENCHANTMENT': {'thaumcraft:infusion_enchantment'},
                'NORMAL_CRAFTING': None, 'PROCESS_CRAFTING': None, 'SMELTING': None}
TEXT_PAGES = {'TEXT', 'TEXT_CONCEALED', 'LORE', 'EXTERNAL_LINK', 'ASPECTS', 'COMPOUND_CRAFTING', 'SCEPTRE_CRAFTING'}

errors = []
warnings = []


def err(msg):
    errors.append(msg)


def ns(value, default='thaumcraft'):
    return value if ':' in value else default + ':' + value


def load_json(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


class Jar:
    def __init__(self, path):
        self.zip = zipfile.ZipFile(path)
        self.names = set(self.zip.namelist())

    def json(self, name):
        return json.loads(self.zip.read(name).decode('utf-8'))

    def stems(self, prefix, suffix='.json'):
        return {n[len(prefix):-len(suffix)] for n in self.names if n.startswith(prefix) and n.endswith(suffix)}


def registry_ids(jar, namespace):
    items = {namespace + ':' + s for s in jar.stems('assets/%s/models/item/' % namespace) if '/' not in s}
    blocks = {namespace + ':' + s for s in jar.stems('assets/%s/blockstates/' % namespace)}
    lang_name = 'assets/%s/lang/en_us.json' % namespace
    if lang_name in jar.names:
        for key in jar.json(lang_name):
            parts = key.split('.')
            if len(parts) == 3 and parts[1] == namespace and parts[0] in ('item', 'block'):
                (items if parts[0] == 'item' else blocks).add(namespace + ':' + parts[2])
    return items, blocks


def our_registry():
    text = ''
    for name in ('blocks/BlocksIS.java', 'items/ItemsIS.java'):
        with open(os.path.join(JAVA, name), encoding='utf-8') as f:
            text += f.read()
    blocks = set(re.findall(r'BLOCKS\.register\("([a-z0-9_]+)"', text))
    items = set(re.findall(r'ITEMS\.register\("([a-z0-9_]+)"', text))
    items |= set(re.findall(r'\bsimple\("([a-z0-9_]+)"', text))
    items |= set(re.findall(r'\b(?:blockItem|machineItem)\("([a-z0-9_]+)"', text))
    return {'isorropia:' + i for i in items}, {'isorropia:' + b for b in blocks}


def walk_items(node, path, out):
    """Collect item ids from recipe ingredient/result objects."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key in ('item', 'id') and isinstance(value, str):
                out.append((value, path))
            elif key == 'items':
                for v in (value if isinstance(value, list) else [value]):
                    if isinstance(v, str) and not v.startswith('#'):
                        out.append((v, path))
            elif key == 'tag' and isinstance(value, str):
                out.append(('#' + value, path))
            else:
                walk_items(value, path, out)
    elif isinstance(node, list):
        for value in node:
            walk_items(value, path, out)


def collect_mod_loaded(node, out=None):
    """Every modid named by a `mod_loaded` condition anywhere in the tree.

    Recipes and data maps may reference an optional mod's items behind such a condition (the
    GregTech variants below), and that mod's registry is not resolvable from the jars checked
    here, so its namespace has to be trusted rather than verified.
    """
    if out is None:
        out = set()
    if isinstance(node, dict):
        if isinstance(node.get('type'), str) and node['type'].endswith(':mod_loaded'):
            modid = node.get('modid')
            if isinstance(modid, str):
                out.add(modid)
        for value in node.values():
            collect_mod_loaded(value, out)
    elif isinstance(node, list):
        for value in node:
            collect_mod_loaded(value, out)
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--tc4r-jar', default=DEFAULT_TC4R)
    parser.add_argument('--vanilla-jar', default=find_vanilla_jar())
    args = parser.parse_args()

    tc = Jar(args.tc4r_jar)
    tc_items, tc_blocks = registry_ids(tc, 'thaumcraft')
    vanilla = Jar(args.vanilla_jar) if os.path.exists(args.vanilla_jar) else None
    if vanilla is None:
        warnings.append('vanilla jar not found (%s): minecraft ids are not checked' % args.vanilla_jar)
        mc_items = mc_blocks = mc_item_tags = set()
    else:
        mc_items, mc_blocks = registry_ids(vanilla, 'minecraft')
        mc_item_tags = {'minecraft:' + s for s in vanilla.stems('data/minecraft/tags/items/')}
    our_items, our_blocks = our_registry()
    items = tc_items | mc_items | our_items
    blocks = tc_blocks | mc_blocks | our_blocks
    item_tags = mc_item_tags | {'thaumcraft:' + s for s in tc.stems('data/thaumcraft/tags/items/')} \
        | {'forge:' + s for s in tc.stems('data/forge/tags/items/')}

    optional_modids = set()
    for root, _dirs, files in os.walk(os.path.join(ROOT, 'src', 'main', 'resources', 'data')):
        for name in files:
            if not name.endswith('.json'):
                continue
            try:
                with open(os.path.join(root, name), encoding='utf-8') as f:
                    optional_modids |= collect_mod_loaded(json.load(f))
            except (ValueError, OSError):
                continue

    def check_item(value, where):
        if value.startswith('#'):
            if vanilla is not None and value[1:] not in item_tags:
                warnings.append('%s: item tag %s not found in vanilla/TC4R data (may come from Forge)' % (where, value))
            return
        value = ns(value, 'minecraft')
        if value.split(':', 1)[0] in optional_modids:
            return
        if value.startswith('minecraft:') and vanilla is None:
            return
        if value not in items:
            err('%s: unknown item %s' % (where, value))

    def check_block(value, where):
        value = ns(value, 'minecraft')
        if value.split(':', 1)[0] in optional_modids:
            return
        if value.startswith('minecraft:') and vanilla is None:
            return
        if value not in blocks:
            err('%s: unknown block %s' % (where, value))

    # aspects
    aspects = {e['id'] for e in tc.json('data/thaumcraft/thaumcraft/aspects/default.json')['entries']}
    aspects |= {'isorropia:' + os.path.basename(p)[:-5] for p in glob.glob(os.path.join(DATA, 'thaumcraft', 'aspects', '*.json'))}

    def check_aspects(mapping, where):
        for key, amount in (mapping or {}).items():
            # An optional mod owns its own aspects (Forbidden Magic's seven sins), so a
            # namespace named by a mod_loaded condition is trusted rather than verified.
            if ns(key) not in aspects and key.split(':', 1)[0] not in optional_modids:
                err('%s: unknown aspect %s' % (where, key))
            if not isinstance(amount, int) or amount <= 0:
                err('%s: aspect %s has non-positive amount %r' % (where, key, amount))

    # entity scan identities
    identities = set()
    for name in tc.names:
        if name.startswith('data/thaumcraft/thaumcraft/entity_aspects/') and name.endswith('.json'):
            identities |= {e['scan_identity'] for e in tc.json(name).get('entries', [])}
    entity_files = glob.glob(os.path.join(DATA, 'thaumcraft', 'entity_aspects', '*.json'))
    for path in entity_files:
        rel = os.path.relpath(path, ROOT)
        for e in load_json(path).get('entries', []):
            for field in ('rule_id', 'id', 'scan_identity', 'aspects'):
                if field not in e:
                    err('%s: entity entry missing %s' % (rel, field))
            identities.add(e.get('scan_identity'))
            if e.get('id', '').startswith('isorropia:') and e.get('required', True):
                err('%s: %s must be "required": false until the entity is registered' % (rel, e.get('id')))
            check_aspects(e.get('aspects'), '%s %s' % (rel, e.get('id')))

    # object aspects
    for path in glob.glob(os.path.join(DATA, 'thaumcraft', 'object_aspects', '*.json')):
        rel = os.path.relpath(path, ROOT)
        root = load_json(path)
        for item_id, mapping in root.get('direct', {}).items():
            check_item(item_id, rel)
            check_aspects(mapping, '%s %s' % (rel, item_id))
        for edit in root.get('edits', []):
            if set(edit) != {'kind', 'id', 'mode', 'phase', 'aspects'}:
                err('%s: edit %s must have exactly kind/id/mode/phase/aspects' % (rel, edit.get('id')))
            if edit.get('mode') not in ('add', 'replace') or edit.get('phase') not in ('before_inference', 'after_inference'):
                err('%s: edit %s has invalid mode/phase' % (rel, edit.get('id')))
            if edit.get('kind') == 'item':
                check_item(edit['id'], rel)
            elif edit.get('kind') == 'tag':
                check_item('#' + edit['id'], rel)
            else:
                err('%s: edit %s has invalid kind' % (rel, edit.get('id')))
            check_aspects(edit.get('aspects'), '%s %s' % (rel, edit.get('id')))

    # recipes
    recipes = {}
    for path in glob.glob(os.path.join(DATA, 'recipes', '**', '*.json'), recursive=True):
        rid = 'isorropia:' + os.path.relpath(path, os.path.join(DATA, 'recipes')).replace(os.sep, '/')[:-5]
        recipes[rid] = load_json(path)
    tc_recipes = {'thaumcraft:' + s for s in tc.stems('data/thaumcraft/recipes/')}

    # research
    tc_research = tc.json('data/thaumcraft/thaumcraft/research/default.json')
    categories = {ns(c['key']) for c in tc_research['categories']}
    research_keys = {ns(e['key']) for e in tc_research['entries']}
    ours = []
    for path in glob.glob(os.path.join(DATA, 'thaumcraft', 'research', '*.json')):
        root = load_json(path)
        rel = os.path.relpath(path, ROOT)
        for c in root.get('categories', []):
            categories.add(ns(c['key']))
            ours.append(('category', rel, c))
        for e in root.get('entries', []):
            key = ns(e['key'])
            if key in research_keys:
                err('%s: duplicate research key %s' % (rel, key))
            research_keys.add(key)
            ours.append(('entry', rel, e))

    lang = load_json(os.path.join(ASSETS, 'lang', 'en_us.json'))
    used_lang = set()

    def need_lang(key, where):
        used_lang.add(key)
        if key not in lang:
            err('%s: missing lang key %s' % (where, key))

    def check_texture(loc, where):
        namespace, path = loc.split(':', 1)
        if namespace == 'isorropia':
            if not os.path.exists(os.path.join(ASSETS, *path.split('/'))):
                err('%s: missing texture %s' % (where, loc))
        elif 'assets/%s/%s' % (namespace, path) not in tc.names:
            err('%s: missing texture %s' % (where, loc))

    referenced_recipes = set()
    for kind, rel, obj in ours:
        if kind == 'category':
            key = ns(obj['key'])
            need_lang('tc.research_category.' + key, rel)
            check_texture(obj['icon'], rel)
            check_texture(obj['background'], rel)
            continue
        key = ns(obj['key'])
        where = '%s %s' % (rel, key)
        for field in obj:
            if field not in ENTRY_FIELDS:
                err('%s: unknown field %s' % (where, field))
        if ns(obj.get('category', '')) not in categories:
            err('%s: unknown category %s' % (where, obj.get('category')))
        for field in ('parents', 'hidden_parents', 'siblings'):
            for parent in obj.get(field, []):
                if ns(parent) not in research_keys:
                    err('%s: %s entry %s does not exist' % (where, field, parent))
                if ns(parent) == key:
                    err('%s: references itself in %s' % (where, field))
        for flag in obj.get('flags', []):
            if flag not in FLAGS:
                err('%s: unknown flag %s' % (where, flag))
        triggered = obj.get('entity_triggers') or obj.get('object_triggers') or obj.get('aspect_triggers')
        if triggered and not ({'HIDDEN', 'LOST'} & set(obj.get('flags', []))):
            err('%s: scan triggers only work with HIDDEN or LOST' % where)
        if not obj.get('aspects') and not ({'AUTO_UNLOCK', 'STUB', 'VIRTUAL'} & set(obj.get('flags', []))):
            err('%s: research without aspects cannot be purchased' % where)
        check_aspects(obj.get('aspects'), where)
        for aspect in obj.get('aspect_triggers', []):
            if ns(aspect) not in aspects:
                err('%s: unknown trigger aspect %s' % (where, aspect))
        for identity in obj.get('entity_triggers', []):
            if identity not in identities:
                err('%s: unknown entity scan identity %s' % (where, identity))
        for trigger in obj.get('object_triggers', []):
            if len(trigger) != 1:
                err('%s: object trigger must have one selector' % where)
            for selector, value in trigger.items():
                if selector == 'item':
                    check_item(value, where)
                elif selector == 'block':
                    check_block(value, where)
                elif selector not in ('item_tag', 'block_tag'):
                    err('%s: unknown object trigger selector %s' % (where, selector))
        icon = obj.get('icon', {})
        if icon.get('type') == 'STACK':
            check_item(icon['stack'].get('id') or icon['stack'].get('item'), where)
        elif icon.get('type') == 'TEXTURE':
            check_texture(icon['texture'], where)
        elif icon.get('type') != 'PRIMARY_ASPECT':
            err('%s: invalid icon %s' % (where, icon))
        need_lang('tc.research_name.' + key, where)
        need_lang('tc.research_text.' + key, where)
        for i, page in enumerate(obj.get('pages', []), 1):
            pwhere = '%s page %d' % (where, i)
            for field in page:
                if field not in PAGE_FIELDS:
                    err('%s: unknown field %s' % (pwhere, field))
            ptype = page.get('type')
            ids = page.get('recipe_ids', [])
            policy = page.get('recipe_link_policy')
            if ptype in RECIPE_PAGES:
                if not ids:
                    err('%s: recipe page without recipe_ids' % pwhere)
                if policy == 'COMPLETED_SINGLE' and len(ids) != 1:
                    err('%s: COMPLETED_SINGLE needs exactly one recipe' % pwhere)
                if policy == 'COMPLETED_CRUCIBLE_VARIANTS' and ptype != 'CRUCIBLE_CRAFTING':
                    err('%s: variant links are crucible-only' % pwhere)
            elif ptype in TEXT_PAGES:
                if ids or policy != 'DISABLED':
                    err('%s: non-recipe page must not reference recipes' % pwhere)
                if ptype in ('TEXT', 'TEXT_CONCEALED', 'LORE'):
                    need_lang(page.get('value', ''), pwhere)
                if ptype == 'TEXT_CONCEALED' and ns(page.get('prerequisite', '')) not in research_keys:
                    err('%s: unknown prerequisite %s' % (pwhere, page.get('prerequisite')))
            else:
                err('%s: unknown page type %s' % (pwhere, ptype))
            for rid in ids:
                referenced_recipes.add(rid)
                if rid in recipes:
                    allowed = RECIPE_PAGES.get(ptype)
                    if allowed and recipes[rid].get('type') not in allowed:
                        err('%s: recipe %s has type %s' % (pwhere, rid, recipes[rid].get('type')))
                elif rid not in tc_recipes:
                    err('%s: recipe %s has no recipe file' % (pwhere, rid))

    for rid, recipe in recipes.items():
        rtype = recipe.get('type', '')
        if not rtype.startswith('thaumcraft:'):
            continue
        where = 'recipe ' + rid
        if 'research' in recipe and ns(recipe['research']) not in research_keys:
            err('%s: unknown research %s' % (where, recipe['research']))
        if rid not in referenced_recipes:
            warnings.append('%s is not shown on any research page' % where)
        check_aspects(recipe.get('aspects') or recipe.get('vis'), where)
        found = []
        walk_items({k: v for k, v in recipe.items() if k not in ('aspects', 'vis')}, where, found)
        for value, w in found:
            check_item(value, w)
        for comp in recipe.get('components', []) + [recipe.get('central', {})]:
            if isinstance(comp, dict) and comp.get('type') == 'thaumcraft:components':
                check_aspects(comp.get('components', {}).get('thaumcraft:aspects'), where)

    # Attributes are hardcoded in the vanilla registry, so this list is written out rather than read.
    attributes = {'minecraft:' + name for name in (
        'generic.max_health', 'generic.follow_range', 'generic.knockback_resistance', 'generic.movement_speed',
        'generic.flying_speed', 'generic.attack_damage', 'generic.attack_knockback', 'generic.attack_speed',
        'generic.armor', 'generic.armor_toughness', 'generic.luck')}         | {'forge:swim_speed', 'forge:entity_reach', 'forge:block_reach'}

    # vat infusions: creature infusions and self infusions (data/isorropia/isorropia/*_infusions)
    creature_hooks = {'isorropia:jelly_rabbit', 'isorropia:ore_pig'}
    infusion_files = sorted(glob.glob(os.path.join(DATA, 'isorropia', 'creature_infusions', '*.json')))         + sorted(glob.glob(os.path.join(DATA, 'isorropia', 'self_infusions', '*.json')))
    infusion_names = set()
    for path in infusion_files:
        rel = os.path.relpath(path, ROOT)
        recipe = load_json(path)
        # The two registries are separate namespaces, so only a repeat inside one is a clash.
        name = os.path.dirname(path) + ':' + os.path.basename(path)[:-5]
        if name in infusion_names:
            err('%s: duplicate infusion id %s' % (rel, name))
        infusion_names.add(name)
        if 'research' in recipe and ns(recipe['research']) not in research_keys:
            err('%s: unknown research %s' % (rel, recipe['research']))
        for key in recipe.get('aspects', {}):
            if ns(key) not in aspects:
                err('%s: unknown aspect %s' % (rel, key))
        for component in recipe.get('components', []):
            if not isinstance(component, dict):
                err('%s: component %r is not an object' % (rel, component))
                continue
            if component.get('count', 1) < 1:
                err('%s: component count must be positive' % rel)
            for key in ('item', 'tag'):
                if key in component:
                    check_item('#' + component[key] if key == 'tag' else component[key], rel)
        if 'hook' in recipe and recipe['hook'] not in creature_hooks:
            err('%s: unknown hook %s' % (rel, recipe['hook']))
        for modifier in recipe.get('modifiers', []):
            if modifier.get('attribute') not in attributes:
                err('%s: unknown attribute %s' % (rel, modifier.get('attribute')))
        kind = recipe.get('kind')
        if kind is not None:
            if kind == 'specie' and 'result' not in recipe:
                err('%s: a specie infusion needs a result entity' % rel)
            if kind == 'organ' and 'organ' not in recipe:
                err('%s: an organ infusion needs an organ' % rel)
            if kind == 'organ' and recipe.get('organ') not in {'heart', 'skin', 'blood', 'muscle', 'void'}:
                err('%s: unknown organ %s' % (rel, recipe.get('organ')))
            if kind == 'specie' and not any(e.get('id') == recipe.get('result') for e in []):
                pass
        if kind is None and 'infusion' not in recipe:
            err('%s: a self infusion needs an infusion id' % rel)

    orphans = sorted(k for k in lang if k.startswith('isorropia.research_page.') and k not in used_lang)
    for key in orphans:
        warnings.append('unused lang key ' + key)
    for key in lang:
        if key.startswith('research.') or key == 'tc.research_category.ISORROPIA':
            warnings.append('legacy TC6 lang key left over: ' + key)

    for w in warnings:
        print('WARN ', w)
    for e in errors:
        print('ERROR', e)
    print('%d research entries, %d recipes, %d errors, %d warnings'
          % (sum(1 for k, _, _ in ours if k == 'entry'), len(recipes), len(errors), len(warnings)))
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main())

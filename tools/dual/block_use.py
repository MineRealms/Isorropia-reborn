"""Codemod: give every 1.20.1 ``Block#use`` override its 1.21.1 ``useItemOn`` delegate.

    python tools/dual/block_use.py          # rewrite src/main/java in place
    python tools/dual/block_use.py --check  # exit 1 if any override still lacks its delegate

1.20.5 split ``use`` into ``useItemOn`` and ``useWithoutItem``. The canonical source stays in the 1.20.1 dialect, so
each override keeps its ``@Override public InteractionResult use(...)`` on the Forge branch; on the NeoForge branch
the same method becomes a plain method and a ``useItemOn`` delegate maps its result through
``platform.BlockInteractions``. ``super.use(...)`` calls (the vanilla default, PASS) become a literal PASS there.
Idempotent: files that already carry the delegate are skipped.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / 'src' / 'main' / 'java'

SIGNATURE = re.compile(
    r'(?P<indent>[ \t]*)@Override\s*\n'
    r'[ \t]*public InteractionResult use\(BlockState (?P<state>\w+), Level (?P<level>\w+), BlockPos (?P<pos>\w+), '
    r'Player (?P<player>\w+), InteractionHand (?P<hand>\w+),\s*BlockHitResult (?P<hit>\w+)\) \{\n')
SUPER_CALL = re.compile(r'^(?P<indent>[ \t]*)return super\.use\((?P<args>[^)]*)\);\n', re.M)


def rewrite(text):
    if 'useItemOn(' in text:
        return text
    match = SIGNATURE.search(text)
    if not match:
        return text
    i = match.group('indent')
    g = match.groupdict()
    params = (f"BlockState {g['state']}, Level {g['level']}, BlockPos {g['pos']}, Player {g['player']}, "
              f"InteractionHand {g['hand']},\n{i}        BlockHitResult {g['hit']}")
    args = f"{g['state']}, {g['level']}, {g['pos']}, {g['player']}, {g['hand']}, {g['hit']}"
    block = (
        f"{i}//? if forge {{\n"
        f"{i}@Override\n"
        f"{i}public InteractionResult use({params}) {{\n"
        f"{i}//?}} else {{\n"
        f"{i}/*@Override\n"
        f"{i}protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,\n"
        f"{i}        {params}) {{\n"
        f"{i}    return fr.wind_blade.isorropia.common.platform.BlockInteractions.toItemResult(use({args}));\n"
        f"{i}}}\n\n"
        f"{i}public InteractionResult use({params}) {{\n"
        f"{i}*///?}}\n")
    text = text[:match.start()] + block + text[match.end():]

    def super_call(m):
        ind = m.group('indent')
        return (f"{ind}//? if forge {{\n{ind}return super.use({m.group('args')});\n{ind}//?}} else {{\n"
                f"{ind}/*return InteractionResult.PASS;\n{ind}*///?}}\n")
    return SUPER_CALL.sub(super_call, text)


def main():
    check = '--check' in sys.argv
    missing = []
    for path in sorted(ROOT.rglob('*.java')):
        text = path.read_text(encoding='utf-8')
        updated = rewrite(text)
        if updated != text:
            missing.append(path.relative_to(ROOT))
            if not check:
                path.write_text(updated, encoding='utf-8')
    verb = 'missing a delegate' if check else 'rewritten'
    for rel in missing:
        print(f'{verb}: {rel}')
    print(f'{len(missing)} file(s) {verb}')
    return 1 if check and missing else 0


if __name__ == '__main__':
    sys.exit(main())

"""Codemod: make block entities' self-calls in their sync methods compile on 1.21.1.

    python tools/dual/be_sync_calls.py          # rewrite src/main/java in place
    python tools/dual/be_sync_calls.py --check  # exit 1 if a bare self-call is left

The overriding signatures (``saveAdditional``/``loadAdditional``/``handleUpdateTag``/``onDataPacket``/
``getUpdateTag``) are already dual and take ``registries`` on 1.21.1; the bodies are shared, so the statements that
call those methods on ``this`` need the extra argument there:

    saveAdditional(tag);                  -> saveAdditional(tag, registries);
    load(tag);                            -> loadAdditional(tag, registries);
    handleUpdateTag(packet.getTag());     -> handleUpdateTag(packet.getTag(), registries);

Only statements that stand on their own line are touched, and only outside an existing conditional block.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / 'src' / 'main' / 'java'

RULES = [
    (re.compile(r'^(?P<i>[ \t]*)(?P<this>this\.)?saveAdditional\((?P<a>\w+)\);$'),
     lambda m: f"{m['this'] or ''}saveAdditional({m['a']}, registries);"),
    (re.compile(r'^(?P<i>[ \t]*)(?P<this>this\.)?load\((?P<a>\w+)\);$'),
     lambda m: f"{m['this'] or ''}loadAdditional({m['a']}, registries);"),
    (re.compile(r'^(?P<i>[ \t]*)(?P<this>this\.)?handleUpdateTag\((?P<a>[\w.()]+)\);$'),
     lambda m: f"{m['this'] or ''}handleUpdateTag({m['a']}, registries);"),
]


def rewrite(text):
    lines = text.split('\n')
    out = []
    depth = 0  # inside an existing //? block
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('//? if'):
            depth += 1
        elif stripped.startswith('*///?}') or stripped == '//?}':
            depth = max(0, depth - 1)
        if depth == 0:
            for pattern, neo in RULES:
                m = pattern.match(line)
                if m:
                    i = m['i']
                    out += [f'{i}//? if forge {{', line, f'{i}//?}} else {{', f'{i}/*{neo(m)}', f'{i}*///?}}']
                    break
            else:
                out.append(line)
        else:
            out.append(line)
    return '\n'.join(out)


def main():
    check = '--check' in sys.argv
    changed = []
    for path in sorted((ROOT / 'fr' / 'wind_blade' / 'isorropia' / 'common' / 'tiles').rglob('*.java')):
        text = path.read_text(encoding='utf-8')
        updated = rewrite(text)
        if updated != text:
            changed.append(path.name)
            if not check:
                path.write_text(updated, encoding='utf-8')
    for name in changed:
        print(('bare self-call: ' if check else 'rewritten: ') + name)
    print(f'{len(changed)} file(s)')
    return 1 if check and changed else 0


if __name__ == '__main__':
    sys.exit(main())

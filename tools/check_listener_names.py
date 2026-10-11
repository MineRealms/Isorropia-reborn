"""Finds @SubscribeEvent handlers that Forge's event bus would silently merge.

EventBus 6 compiles each handler into a class named
    <package>.__<declaring class simple name>_<method name>_<event simple name>
and caches those classes by name. Two handlers that produce the same name share one class, so the first
handler runs once per registration and the second never runs at all. Nested classes make this easy to hit:
LensKeys$Register.onRegisterKeys and MorphicKeys$Register.onRegisterKeys are both "Register" in the same
package. Run this after adding an event handler:

    python tools/check_listener_names.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "java"
CLASS = re.compile(r"\b(?:class|interface|enum|record)\s+(\w+)")
HANDLER = re.compile(r"@SubscribeEvent(?:\([^)]*\))?\s+(?:public\s+|static\s+|final\s+|synchronized\s+)*"
                     r"void\s+(\w+)\s*\(\s*(?:final\s+)?([\w.]+)(?:<[^>]*>)?\s+\w+\s*\)")


def enclosing_class(text, offset):
    """The innermost class whose body is still open at the offset."""
    stack = []
    depth = 0
    pending = None
    for match in re.finditer(r"[{}]|\b(?:class|interface|enum|record)\s+\w+", text[:offset]):
        token = match.group(0)
        if token == "{":
            depth += 1
            if pending is not None:
                stack.append((pending, depth))
                pending = None
        elif token == "}":
            if stack and stack[-1][1] == depth:
                stack.pop()
            depth -= 1
        else:
            pending = CLASS.match(token).group(1)
    return stack[-1][0] if stack else "?"


def main():
    seen = {}
    for path in sorted(ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        package = re.search(r"^package\s+([\w.]+);", text, re.M)
        package = package.group(1) if package else ""
        for match in HANDLER.finditer(text):
            method, event = match.group(1), match.group(2).split(".")[-1]
            owner = enclosing_class(text, match.start())
            name = f"{package}.__{owner}_{method}_{event}"
            line = text.count("\n", 0, match.start()) + 1
            seen.setdefault(name, []).append(f"{path.relative_to(ROOT.parent.parent.parent)}:{line}")
    clashes = {name: where for name, where in seen.items() if len(where) > 1}
    for name, where in clashes.items():
        print(f"CLASH {name}")
        for place in where:
            print(f"    {place}")
    print(f"{sum(len(w) for w in seen.values())} handlers, {len(clashes)} clashes")
    return 1 if clashes else 0


if __name__ == "__main__":
    sys.exit(main())

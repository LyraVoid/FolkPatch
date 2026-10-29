#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path


ROOTS = ("app/src/main", "core/ui/src/main", "core/designsystem/src/main")
ICON = re.compile(r"\bIcons(?:\.[A-Za-z]+)*\.[A-Za-z0-9_]+\b")
ALLOWED_STYLE = re.compile(r"^Icons\.(?:AutoMirrored\.)?Outlined\.")
DEFAULT_WHITELIST = Path(__file__).with_name("icon-whitelist.txt")


def scan(project: Path, roots: tuple[str, ...]) -> dict[str, list[str]]:
    found: dict[str, list[str]] = {}
    for root in roots:
        for path in sorted((project / root).rglob("*.kt")):
            for line_number, line in enumerate(path.read_text().splitlines(), 1):
                for icon in ICON.findall(line):
                    found.setdefault(icon, []).append(f"{path.relative_to(project)}:{line_number}")
    return found


def load_whitelist(path: Path) -> set[str]:
    return {
        line.strip()
        for line in path.read_text().splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }


def self_test() -> None:
    assert ICON.findall("Icon(Icons.Outlined.Add)") == ["Icons.Outlined.Add"]
    assert ICON.findall("Icons.AutoMirrored.Outlined.Send") == [
        "Icons.AutoMirrored.Outlined.Send"
    ]
    assert not ALLOWED_STYLE.match("Icons.Filled.Add")
    assert ALLOWED_STYLE.match("Icons.Outlined.Add")
    assert ALLOWED_STYLE.match("Icons.AutoMirrored.Outlined.Send")


def main() -> int:
    parser = argparse.ArgumentParser(description="Enforce the project icon whitelist.")
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--whitelist", type=Path, default=DEFAULT_WHITELIST)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0

    found = scan(args.project, ROOTS)
    whitelist = load_whitelist(args.whitelist)
    wrong_style = sorted(icon for icon in found if not ALLOWED_STYLE.match(icon))
    missing = sorted(set(found) - whitelist)
    stale = sorted(whitelist - set(found))

    for icon in wrong_style:
        print(f"wrong-style {icon}: {', '.join(found[icon])}", file=sys.stderr)
    for icon in missing:
        print(f"not-whitelisted {icon}: {', '.join(found[icon])}", file=sys.stderr)
    for icon in stale:
        print(f"stale-whitelist {icon}", file=sys.stderr)

    if wrong_style or missing or stale:
        return 1
    references = sum(len(paths) for paths in found.values())
    print(f"icons ok: {references} references, {len(found)} unique")
    return 0


if __name__ == "__main__":
    self_test()
    raise SystemExit(main())

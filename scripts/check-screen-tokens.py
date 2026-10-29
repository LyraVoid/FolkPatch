#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path


SCREEN = Path("app/src/main/java/me/bmax/apatch/ui/screen")
UI = Path("app/src/main/java/me/bmax/apatch/ui")
RAW_DIMENSION = re.compile(r"(?<![\w.])\d+(?:\.\d+)?\.(?:dp|sp)\b")
RAW_COLOR = re.compile(
    r"\bColor\.(?:Transparent|Unspecified|White|Black|Red|Green|Blue|Gray|"
    r"LightGray|DarkGray|Yellow|Cyan|Magenta)\b|"
    r"\bColor\s*\(\s*(?:0x|[0-9])|\b0x[0-9A-Fa-f]{8}\b"
)
SURFACE_ALPHA_OWNER = re.compile(
    r"\.surface(?:Bright|Dim|Container(?:High(?:est)?|Low(?:est)?)?)?"
    r"\s*\.copy\(\s*alpha\s*=\s*(?!0(?:\.0+)?f?\b|1(?:\.0+)?f?\b)[A-Za-z_(]"
)


def scan(project: Path) -> list[tuple[Path, int, str]]:
    findings: list[tuple[Path, int, str]] = []
    for path in sorted((project / SCREEN).rglob("*.kt")):
        for line_number, line in enumerate(path.read_text().splitlines(), 1):
            match = RAW_DIMENSION.search(line) or RAW_COLOR.search(line)
            if match:
                findings.append((path.relative_to(project), line_number, match.group()))
    return findings


def scan_surface_alpha(project: Path) -> list[tuple[Path, int, str]]:
    roots = [project / UI]
    roots.extend((project / "core").glob("*/src/main/java/me/bmax/apatch/ui"))
    findings: list[tuple[Path, int, str]] = []
    for root in roots:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.kt")):
            for line_number, line in enumerate(path.read_text().splitlines(), 1):
                match = SURFACE_ALPHA_OWNER.search(line)
                if match:
                    findings.append((path.relative_to(project), line_number, match.group()))
    return findings


def self_test() -> None:
    assert RAW_DIMENSION.search("padding(16.dp)")
    assert RAW_DIMENSION.search("fontSize = 14.sp")
    assert not RAW_DIMENSION.search("FolkDimens.Size16")
    assert RAW_COLOR.search("Color.Transparent")
    assert RAW_COLOR.search("Color(0xFF112233)")
    assert not RAW_COLOR.search("FolkColors.Transparent")
    assert SURFACE_ALPHA_OWNER.search("scheme.surface.copy(alpha = opacity)")
    assert not SURFACE_ALPHA_OWNER.search("scheme.surface.copy(alpha = 0f)")
    assert not SURFACE_ALPHA_OWNER.search("scheme.surface.copy(alpha = 1f)")


def main() -> int:
    parser = argparse.ArgumentParser(description="Reject screen-layer raw tokens.")
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0

    findings = scan(args.project)
    for path, line, value in findings:
        print(f"{path}:{line}: {value}", file=sys.stderr)
    if findings:
        return 1
    alpha_findings = scan_surface_alpha(args.project)
    for path, line, value in alpha_findings:
        print(f"{path}:{line}: {value}", file=sys.stderr)
    if alpha_findings:
        return 1
    print("screen tokens ok: 0 raw colors, 0 raw dp/sp")
    print("surface alpha ownership ok: 0 component-owned wallpaper alphas")
    return 0


if __name__ == "__main__":
    self_test()
    raise SystemExit(main())

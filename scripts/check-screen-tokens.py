#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path


SCREEN = Path("app/src/main/java/me/bmax/apatch/ui/screen")
RAW_DIMENSION = re.compile(r"(?<![\w.])\d+(?:\.\d+)?\.(?:dp|sp)\b")
RAW_COLOR = re.compile(
    r"\bColor\.(?:Transparent|Unspecified|White|Black|Red|Green|Blue|Gray|"
    r"LightGray|DarkGray|Yellow|Cyan|Magenta)\b|"
    r"\bColor\s*\(\s*(?:0x|[0-9])|\b0x[0-9A-Fa-f]{8}\b"
)


def scan(project: Path) -> list[tuple[Path, int, str]]:
    findings: list[tuple[Path, int, str]] = []
    for path in sorted((project / SCREEN).rglob("*.kt")):
        for line_number, line in enumerate(path.read_text().splitlines(), 1):
            match = RAW_DIMENSION.search(line) or RAW_COLOR.search(line)
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
    print("screen tokens ok: 0 raw colors, 0 raw dp/sp")
    return 0


if __name__ == "__main__":
    self_test()
    raise SystemExit(main())

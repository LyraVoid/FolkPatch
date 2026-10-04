#!/usr/bin/env python3
"""Run boot_patch.sh against a stub kptools. No device, SDK, or real binary."""
import os
from pathlib import Path
import shutil
import stat
import subprocess
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
ABORT = "- APatch requires CONFIG_KALLSYMS to be Enabled."
PROBE_DENIAL = (
    'avc: denied { open } path="/data/data/me.yuki.folk/patch/kernel"'
)
PROBE_STATUS = 13

KPTOOLS = textwrap.dedent(
    """\
    #!/usr/bin/env python3
    import os
    import sys

    mode = os.environ.get("KPTOOLS_MODE", "happy")
    args = sys.argv[1:]

    def probe(image):
        return args[:3] == ["-i", image, "-f"]

    if args[:1] == ["unpack"]:
        open("kernel", "w").close()
        sys.exit(0)
    if probe("kernel"):
        if mode == "probe_fail":
            sys.stderr.write(%r + "\\n")
            sys.exit(%d)
        if mode == "no_kallsyms":
            sys.stdout.write("# CONFIG_KALLSYMS is not set\\n")
            sys.exit(0)
        sys.stdout.write("CONFIG_KALLSYMS=y\\n")
        sys.exit(0)
    if args[:3] == ["-i", "kernel", "-l"]:
        sys.exit(0)
    if args[:1] == ["-p"]:
        sys.exit(0)
    if args[:1] == ["repack"]:
        if mode == "happy":
            open("new-boot.img", "wb").close()
        sys.exit(0)
    if probe("kernel.ori"):
        sys.stdout.write("CONFIG_KALLSYMS_ALL=y\\n")
        sys.exit(0)
    sys.stderr.write("unexpected kptools args: %%s\\n" %% (args,))
    sys.exit(99)
    """
    % (PROBE_DENIAL, PROBE_STATUS)
)


class BootPatchOutputTest(unittest.TestCase):
    def run_patch(self, mode, flash=False):
        with tempfile.TemporaryDirectory(prefix="boot-patch-") as directory:
            work = Path(directory)
            shutil.copy(ASSETS / "boot_patch.sh", work / "boot_patch.sh")
            shutil.copy(ASSETS / "util_functions.sh", work / "util_functions.sh")
            (work / "boot.img").write_bytes(b"boot")
            kptools = work / "kptools"
            kptools.write_text(KPTOOLS)
            kptools.chmod(kptools.stat().st_mode | stat.S_IEXEC)
            bindir = work / "bin"
            bindir.mkdir()
            getprop = bindir / "getprop"
            getprop.write_text("#!/bin/sh\necho arm64-v8a\n")
            getprop.chmod(getprop.stat().st_mode | stat.S_IEXEC)
            env = os.environ.copy()
            env["PATH"] = str(bindir) + os.pathsep + env.get("PATH", "")
            env["KPTOOLS_MODE"] = mode
            command = ["sh", "boot_patch.sh", "su", "boot.img"]
            if flash:
                command.append("true")
            return subprocess.run(
                command,
                cwd=work,
                env=env,
                capture_output=True,
                text=True,
            )

    def test_probe_failure_is_not_a_kallsyms_miss(self):
        result = self.run_patch("probe_fail")
        output = result.stdout + result.stderr
        self.assertEqual(result.returncode, PROBE_STATUS, output)
        self.assertIn(PROBE_DENIAL, output)
        self.assertNotIn("Successfully Patched!", output)
        self.assertNotIn(ABORT, output)

    def test_missing_kallsyms_aborts(self):
        result = self.run_patch("no_kallsyms")
        output = result.stdout + result.stderr
        self.assertEqual(result.returncode, 1, output)
        self.assertIn(ABORT, output)
        self.assertIn("- Patcher has Aborted!", output)
        self.assertNotIn("Successfully Patched!", output)

    def test_repack_without_output_fails(self):
        result = self.run_patch("missing")
        output = result.stdout + result.stderr
        self.assertEqual(result.returncode, 1, output)
        self.assertIn("new-boot.img", result.stderr)
        self.assertNotIn("Successfully Patched!", output)

    def test_happy_path_and_flash_refusal(self):
        patched = self.run_patch("happy")
        patched_output = patched.stdout + patched.stderr
        self.assertEqual(patched.returncode, 0, patched_output)
        self.assertIn("Successfully Patched!", patched.stdout)

        refused = self.run_patch("missing", flash=True)
        refused_output = refused.stdout + refused.stderr
        self.assertEqual(refused.returncode, 1, refused_output)
        self.assertIn("new-boot.img", refused.stderr)
        self.assertIn("refusing to flash", refused.stderr)
        self.assertNotIn("Successfully Flashed!", refused_output)
        self.assertNotIn("Successfully Patched!", refused_output)


if __name__ == "__main__":
    unittest.main()

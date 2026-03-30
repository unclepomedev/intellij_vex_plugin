import shutil
import subprocess
import sys


def fetch_vex_globals(context_name):
    # noinspection PyDeprecation
    vcc_bin = shutil.which("vcc")
    if not vcc_bin:
        print("Error: `vcc` executable not found in PATH", file=sys.stderr)
        return ""
    try:
        result = subprocess.run(
            [vcc_bin, "-X", context_name],
            capture_output=True,
            text=True,
            check=True,
            timeout=30,
        )
        return result.stdout
    except (subprocess.CalledProcessError, OSError, subprocess.TimeoutExpired) as e:
        print(f"Error fetching {context_name}: {e}", file=sys.stderr)
        return ""

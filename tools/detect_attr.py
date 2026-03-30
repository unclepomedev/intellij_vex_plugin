import subprocess
import sys
from pathlib import Path
import hou


def fetch_vex_globals(context_name):
    try:
        result = subprocess.run(
            ["vcc", "-X", context_name], capture_output=True, text=True, check=True
        )
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"Error fetching {context_name}: {e.stderr}", file=sys.stderr)
        return ""


if __name__ == "__main__":
    out_dir = Path("detect_attr")
    out_dir.mkdir(parents=True, exist_ok=True)

    for ctx in hou.vexContexts():
        ctx_name = ctx.name()
        data = fetch_vex_globals(ctx_name)
        if data:
            out_file = out_dir / f"{ctx_name}.txt"
            out_file.write_text(data, encoding="utf-8")

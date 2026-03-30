from pathlib import Path

import hou

from vex_utils import fetch_vex_globals


if __name__ == "__main__":
    out_dir = Path("detect_attr")
    out_dir.mkdir(parents=True, exist_ok=True)

    for ctx in hou.vexContexts():
        ctx_name = ctx.name()
        data = fetch_vex_globals(ctx_name)
        if data:
            out_file = out_dir / f"{ctx_name}.txt"
            out_file.write_text(data, encoding="utf-8")

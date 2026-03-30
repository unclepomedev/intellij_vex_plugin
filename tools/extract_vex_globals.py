import json
import re
from pathlib import Path

from vex_utils import fetch_vex_globals

GLOBAL_VAR_PATTERN = re.compile(r"\((.*?)\)\s+([a-zA-Z0-9_]+)\s+([a-zA-Z0-9_]+)")


def parse_global_variables(vcc_output):
    in_globals_block = False
    parsed_data = []

    for line in vcc_output.splitlines():
        line_stripped = line.strip()

        if line_stripped == "Global Variables:":
            in_globals_block = True
            continue

        if not in_globals_block or not line_stripped:
            continue

        if line_stripped.endswith(":"):
            break

        match = GLOBAL_VAR_PATTERN.match(line_stripped)
        if match:
            parsed_data.append(
                {
                    "name": match.group(3).strip(),
                    "type": match.group(2).strip(),
                    "access": match.group(1).strip(),
                }
            )

    return parsed_data


if __name__ == "__main__":
    import hou

    out_dir = Path("detect_attr")
    out_dir.mkdir(parents=True, exist_ok=True)

    all_globals = {}

    for ctx in hou.vexContexts():
        ctx_name = ctx.name()
        data = fetch_vex_globals(ctx_name)
        if data:
            parsed_list = parse_global_variables(data)
            all_globals[ctx_name] = parsed_list

    out_file = out_dir / "vex_globals.json"
    out_file.write_text(json.dumps(all_globals, indent=4), encoding="utf-8")

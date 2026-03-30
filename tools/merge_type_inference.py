import json
from collections import defaultdict
from pathlib import Path


def merge_for_type_inference(globals_path, attrs_path, out_path):
    with open(globals_path, "r", encoding="utf-8") as f:
        raw_globals = json.load(f)

    with open(attrs_path, "r", encoding="utf-8") as f:
        raw_attrs = json.load(f)

    merged_data = defaultdict(lambda: defaultdict(list))

    for type_name, var_list in raw_attrs.items():
        for var_name in var_list:
            merged_data[var_name][type_name].append("attribute")

    for ctx_name, var_list in raw_globals.items():
        for var_info in var_list:
            var_name = var_info["name"]
            var_type = var_info["type"]

            if ctx_name not in merged_data[var_name][var_type]:
                merged_data[var_name][var_type].append(ctx_name)

    final_data = {var_name: dict(types) for var_name, types in merged_data.items()}

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(final_data, f, indent=4)


if __name__ == "__main__":
    base_dir = Path("detect_attr")
    merge_for_type_inference(
        base_dir / "vex_globals.json",
        base_dir / "standard_attributes.json",
        base_dir / "type_inference_data.json",
    )

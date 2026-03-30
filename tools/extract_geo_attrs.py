import zipfile
import json
import os
import re
from pathlib import Path

TYPE_MAP = {
    "float": "float",
    "int": "int",
    "integer": "int",
    "vec3": "vector",
    "vector": "vector",
    "vector3": "vector",
    "vec4": "vector4",
    "vector4": "vector4",
    "string": "string",
    "str": "string",
    "dict": "dict",
    "dictionary": "dict",
}

NAME_PATTERN = re.compile(r"^\s*`([a-zA-Z0-9_]+)`:\s*$")
TYPE_PATTERN = re.compile(r"^\s*#type:\s*([a-zA-Z0-9_]+)\s*$")


def parse_help_content(content, attributes_data):
    current_attr = None

    for line in content.splitlines():
        name_match = NAME_PATTERN.match(line)
        if name_match:
            current_attr = name_match.group(1)
            continue

        if not current_attr:
            continue

        type_match = TYPE_PATTERN.match(line)
        if type_match:
            raw_type = type_match.group(1).lower()
            mapped_type = TYPE_MAP.get(raw_type)

            if mapped_type and current_attr not in attributes_data[mapped_type]:
                attributes_data[mapped_type].append(current_attr)

            current_attr = None


def extract_attributes_from_zips(zip_paths):
    attributes_data = {
        "float": [],
        "vector": [],
        "vector4": [],
        "int": [],
        "string": [],
        "dict": [],
    }

    for zip_path in zip_paths:
        if not zip_path.exists():
            continue

        with zipfile.ZipFile(zip_path, "r") as z:
            for file_info in z.infolist():
                if file_info.is_dir() or not file_info.filename.endswith(".txt"):
                    continue

                try:
                    content = z.read(file_info.filename).decode("utf-8")
                    parse_help_content(content, attributes_data)
                except Exception:
                    pass

    return attributes_data


if __name__ == "__main__":
    houdini_resources = os.environ.get("HOUDINI_RESOURCES")
    help_dir = Path(houdini_resources) / "houdini" / "help"

    target_zips = [
        help_dir / "model.zip",
        help_dir / "vellum.zip",
        help_dir / "pyro.zip",
        help_dir / "fluid.zip",
        help_dir / "destruction.zip",
        help_dir / "character.zip",
        help_dir / "dopparticles.zip",
        help_dir / "grains.zip",
        help_dir / "crowds.zip",
    ]

    out_dir = Path("detect_attr")
    out_dir.mkdir(parents=True, exist_ok=True)

    parsed_data = extract_attributes_from_zips(target_zips)

    out_file = out_dir / "standard_attributes.json"
    out_file.write_text(json.dumps(parsed_data, indent=4), encoding="utf-8")

PROJECT_ROOT := justfile_directory()
MAIN_RESOURCES := "src/main/resources"
# Override via HOUDINI_RESOURCES env var for your platform/version
HOUDINI_RESOURCES := env_var_or_default("HOUDINI_RESOURCES", "/Applications/Houdini/Houdini21.0.631/Frameworks/Houdini.framework/Versions/Current/Resources")

default:
    @just --list

dump:
    #!/usr/bin/env bash
    set -e

    echo "Initializing Houdini environment..."
    cd "{{ HOUDINI_RESOURCES }}"
    source houdini_setup

    echo "Dumping VEX API to {{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_api_dump.json ..."
    tmp_json="$(mktemp "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_api_dump.json.tmp.XXXXXX")"
    trap 'rm -f "$tmp_json"' EXIT
    hython "$HH/python3.11libs/opnode_sum.py" > "$tmp_json"
    mv "$tmp_json" "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_api_dump.json"
    trap - EXIT

    echo "Unzip VEX HELP to {{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help ..."
    tmp_help="$(mktemp -d "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help.tmp.XXXXXX")"
    trap 'rm -rf "$tmp_help"' EXIT
    unzip -qo "{{ HOUDINI_RESOURCES }}/houdini/help/vex.zip" "functions/*" -d "$tmp_help"
    rm -rf "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help.old"
    if [ -d "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help" ]; then
        mv "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help" "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help.old"
    fi
    mv "$tmp_help" "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help"
    trap - EXIT
    rm -rf "{{ PROJECT_ROOT }}/{{ MAIN_RESOURCES }}/vex_help.old"

    echo "Done"

parser:
    #!/usr/bin/env bash
    set -e

    rm -rf src/main/gen
    ./gradlew generateLexer generateParser


detect-attr:
    #!/usr/bin/env bash
    set -e

    cd "{{ HOUDINI_RESOURCES }}"
    source houdini_setup
    cd "{{ PROJECT_ROOT }}"
    hython tools/detect_attr.py

generate-type-inference:
    #!/usr/bin/env bash
    set -e

    cd "{{ HOUDINI_RESOURCES }}"
    source houdini_setup
    cd "{{ PROJECT_ROOT }}"

    hython tools/extract_vex_globals.py
    HOUDINI_RESOURCES="{{ HOUDINI_RESOURCES }}" hython tools/extract_geo_attrs.py
    hython tools/merge_type_inference.py

fmt-py:
    uv run ruff format tools tests

test-py:
    #!/usr/bin/env bash
    set -e

    cd "{{ PROJECT_ROOT }}"
    uv run pytest

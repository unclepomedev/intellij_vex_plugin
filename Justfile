PROJECT_ROOT := justfile_directory()
# configure according to env
HOUDINI_RESOURCES := "/Applications/Houdini/Houdini21.0.631/Frameworks/Houdini.framework/Versions/Current/Resources"

default:
    @just --list

dump:
    #!/usr/bin/env bash

    echo "Initializing Houdini environment..."
    cd {{ HOUDINI_RESOURCES }}
    source houdini_setup

    echo "Dumping VEX API to {{ PROJECT_ROOT }}/vex_api_dump.json ..."
    hython $HH/python3.11libs/opnode_sum.py > "{{ PROJECT_ROOT }}/vex_api_dump.json"

    echo "Unzip VEX HELP to {{ PROJECT_ROOT }}/vex_help ..."
    unzip -q {{HOUDINI_RESOURCES}}/houdini/help/vex.zip "functions/*" -d "{{PROJECT_ROOT}}/vex_help"

    echo "Done"

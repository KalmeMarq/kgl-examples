from typing import Protocol

import subprocess
import os
import sys
import re

INCLUDE_DIR = None

def resolve_includes(source: str, current_dir: str) -> str:
    pattern = re.compile(r'''//\s*(?:@include|#include)\s*\(\s*["']([^"']+)["']\s*\)''')

    def replacer(match):
        include_name = match.group(1)
        if not include_name.endswith(".wgsl"):
            include_name += ".wgsl"

        candidates = [
            os.path.join(current_dir, include_name),
            os.path.join(INCLUDE_DIR, include_name) if INCLUDE_DIR else None,
        ]

        for path in candidates:
            if path and os.path.isfile(path):
                with open(path, "r", encoding="utf-8") as f:
                    content = f.read()
                return resolve_includes(content, os.path.dirname(path))

        raise FileNotFoundError(f"Could not find include: {include_name}")

    return pattern.sub(replacer, source)

def convert_wgsl_to_glsl(file_path: str, output_dir: str):
    input_p = file_path
    base_name = os.path.splitext(os.path.basename(file_path))[0]

    output_spirv_p   = os.path.join(output_dir, base_name + ".spv")
    output_glsl_vert = os.path.join(output_dir, base_name + ".vsh")
    output_glsl_frag = os.path.join(output_dir, base_name + ".fsh")

    with open(input_p, "r", encoding="utf-8") as f:
        source = f.read()

    source = resolve_includes(source, os.path.dirname(input_p))

    preprocessed_p = os.path.join(output_dir, base_name + ".pre.wgsl")
    with open(preprocessed_p, "w", encoding="utf-8") as f:
        f.write(source)

    try:
        subprocess.run(
            ["naga", "--generate-debug-symbols", "--compact", preprocessed_p, output_spirv_p],
            check=True, capture_output=True, text=True
        )
    except subprocess.CalledProcessError as e:
        print(f"naga failed for {file_path}:\n{e.stderr}")
        return
    finally:
        if os.path.exists(preprocessed_p):
            os.remove(preprocessed_p)

    try:
        subprocess.run(
            [
                "spirv-cross",
                "--stage", "vert",
                "--entry", "vs_main",
                "--output", output_glsl_vert,
                "--remove-unused-variables",
                "--separate-shader-objects",
                "--flip-vert-y",
                output_spirv_p
            ],
            check=True, capture_output=False, text=True
        )
    except subprocess.CalledProcessError as e:
        print(f"spirv-cross (vert) failed for {file_path}: " + e)
        return

    try:
        subprocess.run(
            [
                "spirv-cross",
                "--stage", "frag",
                "--entry", "fs_main",
                "--output", output_glsl_frag,
                "--remove-unused-variables",
                "--separate-shader-objects",
                output_spirv_p
            ],
            check=True, capture_output=False, text=True
        )
    except subprocess.CalledProcessError as e:
        print(f"spirv-cross (frag) failed for {file_path}: " + e)
        return

    if os.path.exists(output_spirv_p):
        os.remove(output_spirv_p)


def main():
    script_dir = os.path.dirname(__file__).replace("\\", "/")
    shaders_dir = os.path.join(script_dir, "../src/main/resources/shaders").replace("\\", "/")

    global INCLUDE_DIR
    INCLUDE_DIR = os.path.join(shaders_dir, "include")

    output_root = sys.argv[1] if len(sys.argv) > 1 else shaders_dir
    os.makedirs(output_root, exist_ok=True)

    for name in os.listdir(shaders_dir):
        if name.endswith(".wgsl"):
            src = os.path.join(shaders_dir, name)
            convert_wgsl_to_glsl(src, output_root)

if __name__ == "__main__":
    main()
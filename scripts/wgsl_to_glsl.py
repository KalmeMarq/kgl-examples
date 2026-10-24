import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


def resolve_includes(source: str, inc_dir: Path | None) -> str:
    pattern = re.compile(r'''//\s*(?:@include|#include)\s*\(\s*["']([^"']+)["']\s*\)''')

    def replacer(match):
        inc_name: str = match.group(1)
        if not inc_name.endswith(".wgsl"):
            inc_name += ".wgsl"

        inc_file = inc_dir / inc_name

        if inc_file.is_file():
            return resolve_includes(inc_file.read_text(), inc_dir)

        raise FileNotFoundError()

    return pattern.sub(replacer, source)


def compile_wgsl_to_spirv(source: str):
    tmp_pre_file = Path("wgsl.tmp")
    tmp_out_file = Path("tmp.spv")

    tmp_pre_file.write_text(source)

    try:
        subprocess.run(
            ["naga", "--generate-debug-symbols", "--input-kind", "wgsl", "--keep-coordinate-space", "--compact", str(tmp_pre_file), str(tmp_out_file)],
            check=True, capture_output=True, text=True
        )
    except Exception as e:
        tmp_pre_file.unlink()
        raise e

    tmp_pre_file.unlink()
    data = tmp_out_file.read_bytes()
    tmp_out_file.unlink()
    return data


def get_resources_and_remap(spirv_data: bytes):
    in_file = Path("tmp.spv")
    in_file.write_bytes(spirv_data)

    try:
        result = subprocess.run(
            ["spirv-cross", "--dump-resources", str(in_file)],
            capture_output=True, text=True, check=True
        )
    except Exception as e:
        in_file.unlink()
        raise e

    in_file.unlink()

    resources = []

    pattern = re.compile(
        r"ID\s+\d+\s*:\s*(\S+).*?\(Set\s*:\s*(\d+)\).*?\(Binding\s*:\s*(\d+)\)",
        re.IGNORECASE
    )

    for m in pattern.finditer(result.stderr):
        name = m.group(1)
        set_  = int(m.group(2))
        bind  = int(m.group(3))
        resources.append((set_, bind, name))

    resources.sort(key=lambda x: (x[0], x[1]))

    mapping = {}
    next_binding = 0

    for set_, bind, name in resources:
        mapping[name] = next_binding
        next_binding += 1

    return mapping


def get_resources_and_remapA(spirv_data: bytes):
    TAG_TO_KIND = {
        "ubos": "ubo",
        "ssbos": "ssbo",
        "separate images": "texture",
        "separate samplers": "sampler",
    }

    LINE_RE = re.compile(
        r"ID\s+\d+\s*:\s*(\S+).*?\(Set\s*:\s*(\d+)\).*?\(Binding\s*:\s*(\d+)\)",
        re.IGNORECASE
    )

    in_file = Path("tmp.spv")
    in_file.write_bytes(spirv_data)

    try:
        result = subprocess.run(
            ["spirv-cross", "--dump-resources", str(in_file)],
            capture_output=True, text=True, check=True
        )
    finally:
        in_file.unlink()

    resources = {kind: [] for kind in TAG_TO_KIND.values()}
    kind = None

    for line in result.stderr.splitlines():
        stripped = line.strip()

        # heading line ("ubos", "ssbos", ...): unknown headings reset kind to None
        if stripped and not stripped.startswith(("ID", "=")):
            kind = TAG_TO_KIND.get(stripped)
            continue

        m = LINE_RE.search(line)
        if m and kind:
            resources[kind].append((int(m.group(2)), int(m.group(3)), m.group(1)))

    mapping = {}
    for kind, items in resources.items():
        items.sort()  # by (set, binding)
        for index, (_, _, name) in enumerate(items):
            mapping[name] = index  # counter restarts for each type

    return mapping


def compile_spirv_to_glsl(data: bytes, stage: str):
    in_file = Path("tmp.spv")
    out_file = Path("glsl")

    in_file.write_bytes(data)

    try:
        subprocess.run(
            [
                "spirv-cross",
                "--stage", "vert" if stage == "vertex" else "frag",
                "--entry", "vs_main" if stage == "vertex" else "fs_main",
                "--output", str(out_file),
                # "--remove-unused-variables",
                "--separate-shader-objects",
                str(in_file)
            ],
            check=True, capture_output=False, text=True
        )
    except Exception as e:
        in_file.unlink()
        raise e

    in_file.unlink()
    glsl = out_file.read_text()
    out_file.unlink()
    return glsl


def apply_remapped_resources(glsl: str, remapped: dict[str, int]):
    pattern = re.compile(
        r"layout\s*\(\s*binding\s*=\s*\d+([^)]*)\)\s*(uniform texture2D|uniform sampler|uniform|buffer|readonly\s+buffer|writeonly\s+buffer)?\s*(\w+)",
        re.MULTILINE
    )

    def replacer(m):
        rest_of_layout = m.group(1)
        qualifier = m.group(2) or ""
        name = m.group(3)

        if name not in remapped:
            return m.group(0)

        new_bind = remapped[name]
        qual = f" {qualifier}" if qualifier else ""

        return f"layout(binding = {new_bind}{rest_of_layout}){qual} {name}"

    return pattern.sub(replacer, glsl)


def process(source: str, inc_dir: Path | None):
    source = resolve_includes(source, inc_dir)

    spirv_data = compile_wgsl_to_spirv(source)

    vert_glsl = compile_spirv_to_glsl(spirv_data, "vertex")
    frag_glsl = compile_spirv_to_glsl(spirv_data, "fragment")

    remapped = get_resources_and_remapA(spirv_data)

    vert_glsl = apply_remapped_resources(vert_glsl, remapped)
    frag_glsl = apply_remapped_resources(frag_glsl, remapped)

    return vert_glsl, frag_glsl


def main():
    parser = argparse.ArgumentParser(prog='wgsl2glsl')
    parser.add_argument("--src", type=str, required=True)
    parser.add_argument("--dst", type=str, required=True)
    parser.add_argument("--inc", type=str)
    args = parser.parse_args()

    src = args.src
    dst = args.dst
    inc = None if args.inc is None else Path(args.inc)

    if src == '-':
        (vert, frag) = process("\n".join(sys.stdin.readlines()), inc)
        return

    if Path(src).is_file():
        (vert, frag) = process(Path(src).read_text(), inc)
    elif Path(src).is_dir():
        for file in Path(src).glob("*.wgsl"):
            (vert, frag) = process(file.read_text(), inc)

            vsh_name = Path(dst) / file.name.replace(".wgsl", ".vsh")
            fsh_name = Path(dst) / file.name.replace(".wgsl", ".fsh")

            vsh_name.write_text(vert)
            fsh_name.write_text(frag)


if __name__ == "__main__":
    main()
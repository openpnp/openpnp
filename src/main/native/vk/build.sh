#!/usr/bin/env bash
# Builds libopenpnp_vk.so, the Vulkan compute runtime. It loads libvulkan at run time, so only the
# headers and glslc are needed here.
# Usage: build.sh <output dir> <cache dir>
set -euo pipefail

out_dir=$1
cache_dir=$2
src_dir=$(cd "$(dirname "$0")" && pwd)

if [[ "$(uname -s)" != Linux || "$(uname -m)" != x86_64 ]]; then
    echo "openpnp_vk: skipping, only built on Linux x86_64"
    exit 0
fi
for tool in g++ glslc; do
    if ! command -v "$tool" >/dev/null; then
        echo "openpnp_vk: skipping, $tool not found"
        exit 0
    fi
done
if [[ ! -f /usr/include/vulkan/vulkan.h ]]; then
    echo "openpnp_vk: skipping, Vulkan headers not found"
    exit 0
fi

java_home=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
if [[ ! -f "$java_home/include/jni.h" ]]; then
    echo "openpnp_vk: skipping, jni.h not found under $java_home"
    exit 0
fi

generated="$cache_dir/vk-generated"
mkdir -p "$generated"
registry="$generated/shaders.inc"
entries=""
: > "$registry"
for shader in "$src_dir"/shaders/*.comp; do
    name=$(basename "$shader" .comp)
    glslc --target-env=vulkan1.2 -O -mfmt=c -o "$generated/$name.spv.inc" "$shader"
    printf 'const uint32_t spv_%s[] =\n#include "%s.spv.inc"\n;\n' "$name" "$name" >> "$registry"
    entries+="    {\"$name\", spv_$name, sizeof(spv_$name) / 4},"$'\n'
done
printf 'const ShaderSource shaderSources[] = {\n%s};\n' "$entries" >> "$registry"

mkdir -p "$out_dir"
g++ -std=c++17 -O2 -fPIC -shared -fvisibility=hidden -Wall \
    -I"$java_home/include" -I"$java_home/include/linux" -I"$generated" \
    "$src_dir"/*.cpp \
    -ldl -lpthread \
    -o "$out_dir/libopenpnp_vk.so"
echo "openpnp_vk: built $out_dir/libopenpnp_vk.so"

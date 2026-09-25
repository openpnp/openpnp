#!/usr/bin/env bash
# Builds libopenpnp_ocl.so against the OpenCV bundled in org.openpnp:opencv.
# Usage: build.sh <output dir> <opencv jar> <cache dir>
set -euo pipefail

out_dir=$1
opencv_jar=$2
cache_dir=$3
opencv_version=4.5.5
src_dir=$(cd "$(dirname "$0")" && pwd)

if [[ "$(uname -s)" != Linux || "$(uname -m)" != x86_64 ]]; then
    echo "openpnp_ocl: skipping, only built on Linux x86_64"
    exit 0
fi
for tool in g++ git unzip; do
    if ! command -v "$tool" >/dev/null; then
        echo "openpnp_ocl: skipping, $tool not found"
        exit 0
    fi
done

java_home=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
if [[ ! -f "$java_home/include/jni.h" ]]; then
    echo "openpnp_ocl: skipping, jni.h not found under $java_home"
    exit 0
fi

mkdir -p "$cache_dir"
opencv_src="$cache_dir/opencv-$opencv_version"
if [[ ! -d "$opencv_src/modules/core/include" ]]; then
    rm -rf "$opencv_src"
    git -c advice.detachedHead=false clone --quiet --depth 1 --branch "$opencv_version" --filter=blob:none --sparse \
        https://github.com/opencv/opencv.git "$opencv_src"
    git -C "$opencv_src" sparse-checkout set modules/core/include modules/imgproc/include
fi
mkdir -p "$cache_dir/generated/opencv2"
cat > "$cache_dir/generated/opencv2/opencv_modules.hpp" <<'EOF'
#define HAVE_OPENCV_CORE
#define HAVE_OPENCV_IMGPROC
EOF

lib_dir="$cache_dir/opencv-lib"
mkdir -p "$lib_dir"
unzip -o -q -j "$opencv_jar" nu/pattern/opencv/linux/x86_64/libopencv_java455.so -d "$lib_dir"

mkdir -p "$out_dir"
g++ -std=c++17 -O2 -fPIC -shared -fvisibility=hidden \
    -I"$java_home/include" -I"$java_home/include/linux" \
    -I"$cache_dir/generated" \
    -I"$opencv_src/modules/core/include" -I"$opencv_src/modules/imgproc/include" \
    "$src_dir/openpnp_ocl.cpp" \
    -L"$lib_dir" -l:libopencv_java455.so \
    -o "$out_dir/libopenpnp_ocl.so"
echo "openpnp_ocl: built $out_dir/libopenpnp_ocl.so"

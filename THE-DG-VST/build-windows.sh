#!/usr/bin/env bash
# Build THE DG VST for Windows x64 from a Linux host (cross-compilation MinGW-w64).
#
# Prerequisites:
#   - JUCE 8 checkout:        git clone --depth 1 --branch 8.0.10 https://github.com/juce-framework/JUCE.git
#   - Linux-hosted MinGW:     xpack-mingw-w64-gcc-15.2.0-2-linux-x64.tar.gz
#     from https://github.com/xpack-dev-tools/mingw-w64-gcc-xpack/releases
#
# Usage:
#   JUCE_DIR=/path/to/JUCE MINGW_ROOT=/path/to/xpack-mingw-w64-gcc-15.2.0-2 ./build-windows.sh
set -euo pipefail
cd "$(dirname "$0")"

JUCE_DIR="${JUCE_DIR:-/workspace/juce-mingw-patch/JUCE}"
MINGW_ROOT="${MINGW_ROOT:-/workspace/xpack/xpack-mingw-w64-gcc-15.2.0-2}"

cmake -S . -B build-win \
  -DCMAKE_TOOLCHAIN_FILE=cmake/mingw-w64-toolchain.cmake \
  -DMINGW_ROOT="$MINGW_ROOT" \
  -DJUCE_DIR="$JUCE_DIR" \
  -DJUCE_WINDOWS_HELPERS_CAN_RUN=TRUE \
  -DCMAKE_BUILD_TYPE=Release

cmake --build build-win --config Release -j "$(nproc)"

echo
echo "Artefacts Windows x64 :"
echo "  build-win/TheDGVST_artefacts/Release/VST3/THE DG VST.vst3/"
echo "  build-win/TheDGVST_artefacts/Release/Standalone/THE DG VST.exe"

# DSP unit tests (native)
if command -v g++ >/dev/null; then
  g++ -std=c++20 -O2 -o /tmp/dg_dsp_test tests/dsp_test.cpp
  /tmp/dg_dsp_test
fi

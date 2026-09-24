#!/bin/bash
cd "$(dirname "$0")/.."
rm -rf run/autotest run/saves/fashion_autotest_* run/config/fashion.json
export LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe
timeout 1800 xvfb-run -a -s "-screen 0 1600x900x24" ./gradlew runClient -Pautotest --console=plain
echo EXIT $?

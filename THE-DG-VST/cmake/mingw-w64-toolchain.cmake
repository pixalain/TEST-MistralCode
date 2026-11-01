# Cross-compilation toolchain: Windows x64 (VST3 / standalone .exe) from Linux
# using a Linux-hosted MinGW-w64 GCC (e.g. xPack mingw-w64-gcc).
#
# Usage:
#   cmake -S . -B build-win \
#         -DCMAKE_TOOLCHAIN_FILE=cmake/mingw-w64-toolchain.cmake \
#         -DMINGW_ROOT=/opt/xpack-mingw-w64-gcc-15.2.0-2 \
#         -DCMAKE_BUILD_TYPE=Release
set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR x86_64)

if(NOT DEFINED MINGW_ROOT)
    set(MINGW_ROOT "/workspace/xpack/xpack-mingw-w64-gcc-15.2.0-2" CACHE PATH
        "Root of a Linux-hosted mingw-w64-gcc distribution (the dir containing bin/)")
endif()

set(CMAKE_C_COMPILER   "${MINGW_ROOT}/bin/x86_64-w64-mingw32-gcc")
set(CMAKE_CXX_COMPILER "${MINGW_ROOT}/bin/x86_64-w64-mingw32-g++")
set(CMAKE_RC_COMPILER  "${MINGW_ROOT}/bin/x86_64-w64-mingw32-windres")
set(CMAKE_AR           "${MINGW_ROOT}/bin/x86_64-w64-mingw32-gcc-ar")
set(CMAKE_NM           "${MINGW_ROOT}/bin/x86_64-w64-mingw32-gcc-nm")
set(CMAKE_RANLIB       "${MINGW_ROOT}/bin/x86_64-w64-mingw32-gcc-ranlib")
set(CMAKE_DLLTOOL      "${MINGW_ROOT}/bin/x86_64-w64-mingw32-dlltool")
set(CMAKE_STRIP        "${MINGW_ROOT}/bin/x86_64-w64-mingw32-strip")

set(CMAKE_FIND_ROOT_PATH "${MINGW_ROOT}" "${MINGW_ROOT}/x86_64-w64-mingw32")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)

# Static link against GCC runtime so the plugin has no libgcc/libstdc++/winpthread
# DLL dependency: only system DLLs (KERNEL32, UCRT) remain.
set(CMAKE_EXE_LINKER_FLAGS_INIT    "-static -static-libgcc -static-libstdc++ -Wl,--kill-at")
set(CMAKE_SHARED_LINKER_FLAGS_INIT "-static -static-libgcc -static-libstdc++ -Wl,--kill-at")
set(CMAKE_MODULE_LINKER_FLAGS_INIT "-static -static-libgcc -static-libstdc++ -Wl,--kill-at")
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# MinGW's windows.h pulls in string.h in some configs, but JUCE headers use
# memset/strlen in the global namespace; force-include the C headers for every TU.
set(CMAKE_C_FLAGS_INIT "-include string.h -include stdlib.h -include stdio.h")
set(CMAKE_CXX_FLAGS_INIT "-include string.h -include stdlib.h -include stdio.h")

cmake_minimum_required(VERSION 3.22)

project(THE-DG-VST VERSION 1.0.0 LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# JUCE lives outside this repository (see README). Point JUCE_DIR at a JUCE checkout,
# e.g. -DJUCE_DIR=/workspace/JUCE  (Linux native test build) or a Windows checkout for the
# official Windows build.
if(NOT DEFINED JUCE_DIR)
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/../JUCE/CMakeLists.txt")
        set(JUCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../JUCE")
    elseif(EXISTS "/workspace/juce-mingw-patch/JUCE/CMakeLists.txt")
        set(JUCE_DIR "/workspace/juce-mingw-patch/JUCE")
    elseif(EXISTS "/workspace/JUCE/CMakeLists.txt")
        set(JUCE_DIR "/workspace/JUCE")
    else()
        message(FATAL_ERROR "JUCE not found. Pass -DJUCE_DIR=/path/to/JUCE")
    endif()
endif()
message(STATUS "Using JUCE at: ${JUCE_DIR}")

# This build avoids juceaide (the JUCE helper tool) entirely, so it works on hosts
# without pkg-config: we include JUCE with JUCE_MODULES_ONLY and then pull in a
# patched JUCEUtils (cmake/JUCEUtilsNoAide.cmake) that generates JuceHeader.h and
# BinaryData with plain CMake instead of invoking juceaide.
set(JUCE_MODULES_ONLY ON CACHE BOOL "" FORCE)
add_subdirectory("${JUCE_DIR}" "${CMAKE_BINARY_DIR}/JUCE" EXCLUDE_FROM_ALL)
include("${CMAKE_CURRENT_SOURCE_DIR}/cmake/JUCEUtilsNoAide.cmake")

juce_add_plugin(TheDGVST
    COMPANY_NAME "Vibe Audio"
    BUNDLE_ID "com.vibeaudio.thedgvst"
    PLUGIN_MANUFACTURER_CODE VbDG
    PLUGIN_CODE DgVs
    FORMATS VST3 Standalone
    PRODUCT_NAME "THE DG VST"
    DESCRIPTION "David Gilmour signature guitar effects chain (wah, comp, fuzz, octave, uni-vibe, phaser, flanger, chorus, tremolo, delay, reverb)"
    LV2URI "https://vibeaudio.com/the-dg-vst"
    ICON_BIG "${CMAKE_CURRENT_SOURCE_DIR}/Resources/icon.ico"
    ICON_SMALL "${CMAKE_CURRENT_SOURCE_DIR}/Resources/icon.ico"
)

juce_add_binary_data(TheDGVSTAssets SOURCES
    Resources/icon.png
)

target_sources(TheDGVST PRIVATE
    Source/PluginProcessor.cpp
    Source/PluginEditor.cpp
    Source/DspChain.cpp
    Source/ChromeLook.cpp
    Source/ChromeLook.h
)

target_include_directories(TheDGVST PRIVATE Source)

target_compile_definitions(TheDGVST PUBLIC
    JUCE_WEB_BROWSER=0
    JUCE_USE_CURL=0
    JUCE_VST3_HOST_CROSS_PLATFORM_UID=1
    JUCE_IGNORE_VST3_MISMATCHED_PARAMETER_ID_WARNING=1
    JUCE_VST3_CAN_REPLACE_VST2=0
    JUCE_DISPLAY_SPLASH_SCREEN=0
    JUCE_MODAL_LOOPS_PERMITTED=1
    JUCE_DONT_AUTOLINK_TO_WIN32_LIBRARIES=1
    JUCE_USE_WINDOWS_MEDIA_FORMAT=0
)

if(CMAKE_SYSTEM_NAME STREQUAL "Windows" AND CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    target_link_libraries(TheDGVST PRIVATE
        kernel32 user32 gdi32 winspool shell32 ole32 oleaut32 uuid
        comdlg32 advapi32 ws2_32 version shlwapi winmm oleacc wininet
        urlmon dbghelp psapi iphlpapi userenv bcrypt ks ksguid
        dwmapi d3d11 dxgi d2d1 dwrite imm32 comctl32 dcomp sapi
    )
    target_link_options(TheDGVST PRIVATE
        -Wl,--subsystem,windows
        -Wl,-Bstatic -lstdc++ -Wl,-Bdynamic
    )
endif()

target_link_libraries(TheDGVST PRIVATE
    TheDGVSTAssets
    juce::juce_audio_basics
    juce::juce_audio_devices
    juce::juce_audio_formats
    juce::juce_audio_processors
    juce::juce_audio_utils
    juce::juce_core
    juce::juce_data_structures
    juce::juce_dsp
    juce::juce_events
    juce::juce_graphics
    juce::juce_gui_basics
    juce::juce_gui_extra
    juce::juce_recommended_config_flags
)

if(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    target_compile_definitions(TheDGVST PRIVATE JUCE_PLUGINHOST_VST3=1)
endif()

# ---------------------------------------------------------------------------
# Native (Linux) DSP unit test — exercises the same DSP code the plugin uses.
if(CMAKE_SYSTEM_NAME STREQUAL "Linux" OR NOT CMAKE_CROSSCOMPILING)
    add_executable(dg_dsp_test tests/dsp_test.cpp Source/DspChain.cpp)
    target_include_directories(dg_dsp_test PRIVATE Source)
    target_compile_definitions(dg_dsp_test PRIVATE DG_DSP_STANDALONE=1)
    target_compile_features(dg_dsp_test PRIVATE cxx_std_20)
    enable_testing()
    add_test(NAME dg_dsp_test COMMAND dg_dsp_test)
endif()

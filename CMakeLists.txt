cmake_minimum_required(VERSION 3.22)

project(MonPremierVST VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

FetchContent_Declare(
    JUCE
    GIT_REPOSITORY https://github.com/juce-framework/JUCE.git
    GIT_TAG 8.0.15
    GIT_SHALLOW TRUE
)

FetchContent_MakeAvailable(JUCE)

juce_add_plugin(MonPremierVST
    COMPANY_NAME "Pixalain"
    PLUGIN_MANUFACTURER_CODE Pixa
    PLUGIN_CODE Mp01
    FORMATS VST3 Standalone
    PRODUCT_NAME "MonPremierVST"
    BUNDLE_ID "com.pixalain.monpremiervst"
)

target_sources(MonPremierVST PRIVATE
    Source/PluginProcessor.cpp
    Source/PluginEditor.cpp
)

target_compile_definitions(MonPremierVST PUBLIC
    JUCE_WEB_BROWSER=0
    JUCE_USE_CURL=0
    JUCE_VST3_HOST_CROSS_PLATFORM_UID=1
)

target_link_libraries(MonPremierVST PRIVATE
    juce::juce_audio_processors
    juce::juce_gui_extra
    juce::juce_recommended_config_flags
    juce::juce_recommended_warning_flags
    juce::juce_recommended_lto_flags
)

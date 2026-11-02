cmake_minimum_required(VERSION 3.22)

project(VSTbyIA VERSION 0.5.0 LANGUAGES C CXX)

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

juce_add_plugin(VSTbyIA
    COMPANY_NAME "Pixalain"
    PLUGIN_MANUFACTURER_CODE IaIa
    PLUGIN_CODE Ia01
    FORMATS VST3 Standalone
    PRODUCT_NAME "VST by IA"
    BUNDLE_ID "com.pixalain.monpremiervst"
)

target_sources(VSTbyIA PRIVATE
    Source/PluginProcessor.cpp
    Source/PluginEditor.cpp
)

target_compile_definitions(VSTbyIA PUBLIC
    JUCE_WEB_BROWSER=0
    JUCE_USE_CURL=0
    JUCE_VST3_CAN_REPLACE_VST2=0
)

target_link_libraries(VSTbyIA PRIVATE
    juce::juce_audio_processors
    juce::juce_audio_utils
    juce::juce_audio_devices
    juce::juce_dsp
    juce::juce_gui_extra
    juce::juce_recommended_config_flags
    juce::juce_recommended_warning_flags
    juce::juce_recommended_lto_flags
)

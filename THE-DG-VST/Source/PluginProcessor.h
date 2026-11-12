#pragma once

#include <juce_audio_processors/juce_audio_processors.h>
#include "DspChain.h"
#include "Presets.h"

class TheDGVSTAudioProcessor : public juce::AudioProcessor
{
public:
    TheDGVSTAudioProcessor();
    ~TheDGVSTAudioProcessor() override;

    void prepareToPlay (double sampleRate, int samplesPerBlock) override;
    void releaseResources() override;
    bool isBusesLayoutSupported (const BusesLayout& layouts) const override;
    void processBlock (juce::AudioBuffer<float>&, juce::MidiBuffer&) override;

    juce::AudioProcessorEditor* createEditor() override;
    bool hasEditor() const override;

    const juce::String getName() const override;
    bool acceptsMidi() const override;
    bool producesMidi() const override;
    bool isMidiEffect() const override;
    double getTailLengthSeconds() const override;

    int getNumPrograms() override;
    int getCurrentProgram() override;
    void setCurrentProgram (int index) override;
    const juce::String getProgramName (int index) override;
    void changeProgramName (int index, const juce::String& newName) override;

    // Program = factory preset index (host preset navigation).
    // getNumPrograms() = factory presets + user presets.
    void loadFactoryPreset (int index);
    void loadUserPreset (int index);          // index into userPresets
    int  getNumFactoryPresets() const;
    int  getNumUserPresets() const { return (int) userPresets.size(); }
    juce::String getUserPresetName (int index) const;
    void saveCurrentAsUserPreset (const juce::String& name);
    void deleteUserPreset (int index);
    void applyChainParamsToParameters (const dg::ChainParams& p);
    void readParametersInto (dg::ChainParams& p) const;
    juce::StringArray getAllPresetNames() const;

    std::vector<juce::String> userPresets; // user preset names, parallel to userPresetStates
    std::vector<juce::ValueTree> userPresetStates;

    void getStateInformation (juce::MemoryBlock& destData) override;
    void setStateInformation (const void* data, int sizeInBytes) override;

    // Parameter access for the editor
    juce::AudioProcessorValueTreeState apvts;
    dg::DspChain dspL, dspR;

    // Wah pedal follows MIDI CC1 (mod wheel) when enabled
    std::atomic<float> wahCc { -1.0f };
    std::atomic<int> currentProgram { 0 };

private:

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (TheDGVSTAudioProcessor)
};

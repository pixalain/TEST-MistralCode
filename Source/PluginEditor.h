#pragma once

#include "PluginProcessor.h"

class MonPremierVSTAudioProcessorEditor final : public juce::AudioProcessorEditor
{
public:
    explicit MonPremierVSTAudioProcessorEditor (MonPremierVSTAudioProcessor&);
    ~MonPremierVSTAudioProcessorEditor() override;

    void paint (juce::Graphics&) override;
    void resized() override;

private:
    MonPremierVSTAudioProcessor& processorRef;

    juce::Slider gainSlider;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> gainAttachment;

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (MonPremierVSTAudioProcessorEditor)
};

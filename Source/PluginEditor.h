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

    juce::Slider gainSlider { "Gain" };
    juce::Slider mixSlider  { "Reverb Mix" };
    juce::Slider roomSlider { "Room Size" };
    juce::Slider dampSlider { "Damping" };

    juce::Label gainLabel { {}, "Gain" };
    juce::Label mixLabel  { {}, "Reverb Mix" };
    juce::Label roomLabel { {}, "Room Size" };
    juce::Label dampLabel { {}, "Damping" };

    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> gainAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> mixAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> roomAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> dampAttachment;

    void setupSlider (juce::Slider& slider, const juce::String& paramId,
                      std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>& attachment);

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (MonPremierVSTAudioProcessorEditor)
};

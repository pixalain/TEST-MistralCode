#pragma once

#include "PluginProcessor.h"

class VSTbyIAAudioProcessorEditor final : public juce::AudioProcessorEditor
{
public:
    explicit VSTbyIAAudioProcessorEditor (VSTbyIAAudioProcessor&);
    ~VSTbyIAAudioProcessorEditor() override;

    void paint (juce::Graphics&) override;
    void resized() override;

private:
    VSTbyIAAudioProcessor& processorRef;

    juce::Slider gainKnob   { "Gain" };
    juce::Slider delayTimeKnob { "Delay Time" };
    juce::Slider delayFbKnob   { "Feedback" };
    juce::Slider delayMixKnob  { "Delay Mix" };
    juce::Slider mixKnob    { "Reverb Mix" };
    juce::Slider roomKnob   { "Room Size" };
    juce::Slider dampKnob   { "Damping" };

    juce::Label gainLabel   { {}, "Gain" };
    juce::Label timeLabel   { {}, "Delay Time" };
    juce::Label fbLabel     { {}, "Feedback" };
    juce::Label dMixLabel   { {}, "Delay Mix" };
    juce::Label mixLabel    { {}, "Reverb Mix" };
    juce::Label roomLabel   { {}, "Room Size" };
    juce::Label dampLabel   { {}, "Damping" };

    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> gainAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> delayTimeAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> delayFbAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> delayMixAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> mixAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> roomAttachment;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> dampAttachment;

    void setupKnob (juce::Slider& knob, const juce::String& paramId,
                    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>& attachment,
                    juce::Colour accent);

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (VSTbyIAAudioProcessorEditor)
};

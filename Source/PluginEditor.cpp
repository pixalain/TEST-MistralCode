#include "PluginEditor.h"

MonPremierVSTAudioProcessorEditor::MonPremierVSTAudioProcessorEditor (MonPremierVSTAudioProcessor& p)
    : AudioProcessorEditor (&p),
      processorRef (p)
{
    setupSlider (gainSlider, "gain",        gainAttachment);
    setupSlider (mixSlider,  "reverbMix",   mixAttachment);
    setupSlider (roomSlider, "reverbRoom",  roomAttachment);
    setupSlider (dampSlider, "reverbDamping", dampAttachment);

    for (auto* label : { &gainLabel, &mixLabel, &roomLabel, &dampLabel })
    {
        label->setColour (juce::Label::textColourId, juce::Colours::white);
        label->setJustificationType (juce::Justification::centred);
        addAndMakeVisible (*label);
    }

    setSize (480, 260);
}

MonPremierVSTAudioProcessorEditor::~MonPremierVSTAudioProcessorEditor() = default;

void MonPremierVSTAudioProcessorEditor::setupSlider (
    juce::Slider& slider,
    const juce::String& paramId,
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>& attachment)
{
    slider.setSliderStyle (juce::Slider::LinearHorizontal);
    slider.setTextBoxStyle (juce::Slider::TextBoxRight, false, 60, 20);
    addAndMakeVisible (slider);
    attachment = std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment> (
        processorRef.parameters, paramId, slider);
}

void MonPremierVSTAudioProcessorEditor::paint (juce::Graphics& g)
{
    g.fillAll (getLookAndFeel().findColour (juce::ResizableWindow::backgroundColourId));

    g.setColour (juce::Colours::white);
    g.setFont (16.0f);
    g.drawFittedText ("MonPremierVST - Gain + Reverb", getLocalBounds().removeFromTop (28),
                      juce::Justification::centred, 1);
}

void MonPremierVSTAudioProcessorEditor::resized()
{
    auto area = getLocalBounds().reduced (16);
    area.removeFromTop (28);

    auto row = [&] (juce::Slider& slider, juce::Label& label)
    {
        auto r = area.removeFromTop (52);
        label.setBounds (r.removeFromTop (18));
        slider.setBounds (r);
    };

    row (gainSlider, gainLabel);
    row (mixSlider,  mixLabel);
    row (roomSlider, roomLabel);
    row (dampSlider, dampLabel);
}

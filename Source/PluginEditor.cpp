#include "PluginEditor.h"

MonPremierVSTAudioProcessorEditor::MonPremierVSTAudioProcessorEditor (MonPremierVSTAudioProcessor& p)
    : AudioProcessorEditor (&p),
      processorRef (p)
{
    setupKnob (gainKnob, "gain",          gainAttachment);
    setupKnob (mixKnob,  "reverbMix",     mixAttachment);
    setupKnob (roomKnob, "reverbRoom",    roomAttachment);
    setupKnob (dampKnob, "reverbDamping", dampAttachment);

    for (auto* label : { &gainLabel, &mixLabel, &roomLabel, &dampLabel })
    {
        label->setColour (juce::Label::textColourId, juce::Colours::white);
        label->setJustificationType (juce::Justification::centred);
        addAndMakeVisible (*label);
    }

    setSize (560, 230);
}

MonPremierVSTAudioProcessorEditor::~MonPremierVSTAudioProcessorEditor() = default;

void MonPremierVSTAudioProcessorEditor::setupKnob (
    juce::Slider& knob,
    const juce::String& paramId,
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>& attachment)
{
    knob.setSliderStyle (juce::Slider::RotaryVerticalDrag);
    knob.setTextBoxStyle (juce::Slider::TextBoxBelow, false, 64, 18);
    knob.setColour (juce::Slider::rotarySliderFillColourId, juce::Colour (0xff4da6ff));
    knob.setColour (juce::Slider::rotarySliderOutlineColourId, juce::Colour (0xff666666));
    knob.setColour (juce::Slider::thumbColourId, juce::Colours::white);
    addAndMakeVisible (knob);
    attachment = std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment> (
        processorRef.parameters, paramId, knob);
}

void MonPremierVSTAudioProcessorEditor::paint (juce::Graphics& g)
{
    g.fillAll (getLookAndFeel().findColour (juce::ResizableWindow::backgroundColourId));

    g.setColour (juce::Colour (0xff1e1e1e));
    g.fillRoundedRectangle (getLocalBounds().toFloat().reduced (6.0f), 8.0f);

    g.setColour (juce::Colours::white);
    g.setFont (16.0f);
    g.drawFittedText ("MonPremierVST - Gain + Reverb", getLocalBounds().removeFromTop (30),
                      juce::Justification::centred, 1);
}

void MonPremierVSTAudioProcessorEditor::resized()
{
    auto area = getLocalBounds().reduced (14);
    area.removeFromTop (30);

    auto row = area.removeFromTop (area.getHeight());
    const int colWidth = row.getWidth() / 4;

    auto place = [&] (juce::Slider& knob, juce::Label& label)
    {
        auto col = row.removeFromLeft (colWidth);
        label.setBounds (col.removeFromTop (20));
        knob.setBounds (col.withSizeKeepingCentre (80, 110).reduced (4));
    };

    place (gainKnob, gainLabel);
    place (mixKnob,  mixLabel);
    place (roomKnob, roomLabel);
    place (dampKnob, dampLabel);
}

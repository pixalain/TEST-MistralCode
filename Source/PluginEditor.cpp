#include "PluginEditor.h"

namespace
{
const juce::Colour delayAccent  (0xff4da6ff);
const juce::Colour reverbAccent (0xffff9f43);
const juce::Colour gainAccent   (0xff6ee86e);
}

VSTbyIAAudioProcessorEditor::VSTbyIAAudioProcessorEditor (VSTbyIAAudioProcessor& p)
    : AudioProcessorEditor (&p),
      processorRef (p)
{
    setupKnob (gainKnob,      "gain",          gainAttachment,      gainAccent);
    setupKnob (delayTimeKnob, "delayTime",     delayTimeAttachment, delayAccent);
    setupKnob (delayFbKnob,   "delayFeedback", delayFbAttachment,   delayAccent);
    setupKnob (delayMixKnob,  "delayMix",      delayMixAttachment,  delayAccent);
    setupKnob (mixKnob,       "reverbMix",     mixAttachment,       reverbAccent);
    setupKnob (roomKnob,      "reverbRoom",    roomAttachment,      reverbAccent);
    setupKnob (dampKnob,      "reverbDamping", dampAttachment,      reverbAccent);

    for (auto* label : { &gainLabel, &timeLabel, &fbLabel, &dMixLabel,
                         &mixLabel, &roomLabel, &dampLabel })
    {
        label->setColour (juce::Label::textColourId, juce::Colours::white);
        label->setJustificationType (juce::Justification::centred);
        addAndMakeVisible (*label);
    }

    setSize (640, 330);
}

VSTbyIAAudioProcessorEditor::~VSTbyIAAudioProcessorEditor() = default;

void VSTbyIAAudioProcessorEditor::setupKnob (
    juce::Slider& knob,
    const juce::String& paramId,
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>& attachment,
    juce::Colour accent)
{
    knob.setSliderStyle (juce::Slider::RotaryVerticalDrag);
    knob.setTextBoxStyle (juce::Slider::TextBoxBelow, false, 70, 18);
    knob.setColour (juce::Slider::rotarySliderFillColourId, accent);
    knob.setColour (juce::Slider::rotarySliderOutlineColourId, juce::Colour (0xff666666));
    knob.setColour (juce::Slider::thumbColourId, juce::Colours::white);
    addAndMakeVisible (knob);
    attachment = std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment> (
        processorRef.parameters, paramId, knob);
}

void VSTbyIAAudioProcessorEditor::paint (juce::Graphics& g)
{
    g.fillAll (getLookAndFeel().findColour (juce::ResizableWindow::backgroundColourId));

    g.setColour (juce::Colour (0xff1e1e1e));
    g.fillRoundedRectangle (getLocalBounds().toFloat().reduced (6.0f), 8.0f);

    g.setColour (juce::Colours::white);
    g.setFont (16.0f);
    g.drawFittedText ("VST by IA - Gain + Delay + Reverb", getLocalBounds().removeFromTop (30),
                      juce::Justification::centred, 1);
}

void VSTbyIAAudioProcessorEditor::resized()
{
    auto area = getLocalBounds().reduced (14);
    area.removeFromTop (30);

    const int rowHeight = (area.getHeight() - 8) / 2;
    auto row1 = area.removeFromTop (rowHeight);
    auto row2 = area.removeFromTop (rowHeight + 8);

    auto placeRow = [&] (juce::Rectangle<int> row, std::initializer_list<std::pair<juce::Slider*, juce::Label*>> items)
    {
        const int colWidth = row.getWidth() / (int) items.size();
        for (auto& [knob, label] : items)
        {
            auto col = row.removeFromLeft (colWidth);
            label->setBounds (col.removeFromTop (20));
            knob->setBounds (col.withSizeKeepingCentre (80, 105).reduced (4));
        }
    };

    placeRow (row1, { { &gainKnob, &gainLabel }, { &delayTimeKnob, &timeLabel },
                      { &delayFbKnob, &fbLabel }, { &delayMixKnob, &dMixLabel } });
    placeRow (row2, { { &mixKnob, &mixLabel }, { &roomKnob, &roomLabel }, { &dampKnob, &dampLabel } });
}

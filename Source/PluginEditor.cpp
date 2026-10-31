#include "PluginEditor.h"

MonPremierVSTAudioProcessorEditor::MonPremierVSTAudioProcessorEditor (MonPremierVSTAudioProcessor& p)
    : AudioProcessorEditor (&p),
      processorRef (p)
{
    gainSlider.setSliderStyle (juce::Slider::LinearHorizontal);
    gainSlider.setTextBoxStyle (juce::Slider::TextBoxRight, false, 70, 22);
    addAndMakeVisible (gainSlider);

    gainAttachment = std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment> (
        processorRef.parameters, "gain", gainSlider);

    setSize (360, 140);
}

MonPremierVSTAudioProcessorEditor::~MonPremierVSTAudioProcessorEditor() = default;

void MonPremierVSTAudioProcessorEditor::paint (juce::Graphics& g)
{
    g.fillAll (getLookAndFeel().findColour (juce::ResizableWindow::backgroundColourId));

    g.setColour (juce::Colours::white);
    g.setFont (16.0f);
    g.drawFittedText ("MonPremierVST - Gain", getLocalBounds().removeFromTop (28),
                      juce::Justification::centred, 1);
}

void MonPremierVSTAudioProcessorEditor::resized()
{
    gainSlider.setBounds (20, 50, getWidth() - 40, 40);
}

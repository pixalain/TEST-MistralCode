#pragma once

#include <juce_audio_processors/juce_audio_processors.h>
#include "PluginProcessor.h"
#include "ChromeLook.h"

// One "pedal" cell: chrome footswitch (bypass) + chrome knobs + name plate.
struct PedalPanel : juce::Component
{
    juce::ToggleButton footswitch;
    juce::Label nameLabel;
    std::vector<std::unique_ptr<juce::Slider>> knobs;
    std::vector<std::unique_ptr<juce::Label>> knobLabels;
    std::vector<std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment>> attachments;
    std::vector<std::unique_ptr<juce::AudioProcessorValueTreeState::ButtonAttachment>> buttonAttachments;

    void layoutPedal (juce::Rectangle<int> area, const juce::String& name,
                      std::initializer_list<std::pair<const char*, const char*>> knobParams)
    {
        nameLabel.setText (name, juce::dontSendNotification);
        nameLabel.setJustificationType (juce::Justification::centred);
        nameLabel.setColour (juce::Label::textColourId, ChromeLook::textGrey());
        nameLabel.setFont (juce::FontOptions (13.0f).withStyle ("Bold"));
        addAndMakeVisible (nameLabel);

        footswitch.setButtonText ("ON");
        footswitch.setClickingTogglesState (true);
        addAndMakeVisible (footswitch);

        knobs.clear(); knobLabels.clear(); attachments.clear();
        for (auto& [paramId, label] : knobParams)
        {
            auto s = std::make_unique<juce::Slider> (juce::Slider::RotaryHorizontalVerticalDrag,
                                                      juce::Slider::TextBoxBelow);
            s->setTextBoxStyle (juce::Slider::NoTextBox, true, 0, 0);
            s->setColour (juce::Slider::rotarySliderFillColourId, juce::Colour (0xffffffff));
            addAndMakeVisible (*s);
            knobs.push_back (std::move (s));

            auto l = std::make_unique<juce::Label> (juce::String(), label);
            l->setJustificationType (juce::Justification::centred);
            l->setColour (juce::Label::textColourId, ChromeLook::textGrey().withAlpha (0.8f));
            l->setFont (juce::FontOptions (9.5f));
            addAndMakeVisible (*l);
            knobLabels.push_back (std::move (l));

            (void) paramId;
        }
        setBounds (area);
    }

    void attach (juce::AudioProcessorValueTreeState& apvts, const juce::String& bypassId,
                 std::initializer_list<std::pair<const char*, const char*>> knobParams)
    {
        buttonAttachments.clear();
        buttonAttachments.push_back (std::make_unique<juce::AudioProcessorValueTreeState::ButtonAttachment> (
            apvts, bypassId.toStdString(), footswitch));

        attachments.clear();
        size_t i = 0;
        for (auto& [paramId, label] : knobParams)
        {
            (void) label;
            if (i < knobs.size())
                attachments.push_back (std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment> (
                    apvts, paramId, *knobs[i]));
            ++i;
        }
    }

    void resized() override
    {
        auto r = getLocalBounds().reduced (6);
        nameLabel.setBounds (r.removeFromTop (20));

        auto sw = r.removeFromBottom (34);
        footswitch.setBounds (sw.withSizeKeepingCentre (64, 30));

        if (knobs.empty()) return;
        auto knobArea = r.reduced (2);
        int n = (int) knobs.size();
        int cellW = knobArea.getWidth() / n;
        for (int i = 0; i < n; ++i)
        {
            auto cell = knobArea.removeFromLeft (cellW).reduced (2);
            if ((size_t) i < knobLabels.size())
                knobLabels[size_t (i)]->setBounds (cell.removeFromBottom (12));
            knobs[size_t (i)]->setBounds (cell.withSizeKeepingCentre (cell.getWidth() - 4, cell.getWidth() - 4));
        }
    }
};

class TheDGVSTAudioProcessorEditor : public juce::AudioProcessorEditor,
                                     private juce::Timer
{
public:
    explicit TheDGVSTAudioProcessorEditor (TheDGVSTAudioProcessor&);
    ~TheDGVSTAudioProcessorEditor() override;

    void paint (juce::Graphics&) override;
    void resized() override;

private:
    void timerCallback() override;
    void refreshPresetList();
    void onPresetSelected();
    void onSavePreset();
    void onDeletePreset();
    void selectPreset (int itemId);

    TheDGVSTAudioProcessor& processorRef;
    ChromeLook chromeLook;

    juce::Label titleLabel;
    juce::Label subtitleLabel;

    // pedal row, in Gilmour signal-chain order
    PedalPanel wahPanel, compPanel, fuzzPanel, octavePanel, vibePanel,
               phaserPanel, flangerPanel, chorusPanel, tremPanel, delayPanel, reverbPanel;
    PedalPanel masterPanel;

    juce::Slider masterSlider;
    std::unique_ptr<juce::AudioProcessorValueTreeState::SliderAttachment> masterAttachment;
    juce::Label masterLabel;

    std::unique_ptr<juce::AudioProcessorValueTreeState::ComboBoxAttachment> fuzzModeAttachment;
    juce::ComboBox fuzzModeBox;

    // preset manager (chrome buttons + combo)
    juce::ComboBox presetBox;
    juce::TextButton prevPresetButton { "<" };
    juce::TextButton nextPresetButton { ">" };
    juce::TextButton savePresetButton { "SAVE" };
    juce::TextButton deletePresetButton { "DEL" };

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (TheDGVSTAudioProcessorEditor)
};

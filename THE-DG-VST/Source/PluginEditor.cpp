#include "PluginEditor.h"

TheDGVSTAudioProcessorEditor::TheDGVSTAudioProcessorEditor (TheDGVSTAudioProcessor& p)
    : AudioProcessorEditor (&p), processorRef (p)
{
    setLookAndFeel (&chromeLook);

    // ---- header ----
    titleLabel.setText ("THE DG VST", juce::dontSendNotification);
    titleLabel.setJustificationType (juce::Justification::centredLeft);
    titleLabel.setColour (juce::Label::textColourId, juce::Colour (0xffe8e8ee));
    titleLabel.setFont (juce::FontOptions (30.0f).withStyle ("Bold"));
    addAndMakeVisible (titleLabel);

    subtitleLabel.setText ("David Gilmour signature pedalboard  -  Strat into clean Hiwatt, effects from the board",
                           juce::dontSendNotification);
    subtitleLabel.setJustificationType (juce::Justification::centredLeft);
    subtitleLabel.setColour (juce::Label::textColourId, ChromeLook::textGrey().withAlpha (0.75f));
    subtitleLabel.setFont (juce::FontOptions (12.5f));
    addAndMakeVisible (subtitleLabel);

    // ---- pedals (signal chain order, as on Gilmour's boards) ----
    addAndMakeVisible (wahPanel);
    wahPanel.layoutPedal ({}, "WAH  (Cry Baby)", { { "wahPedal", "Pedal" } });
    wahPanel.attach (processorRef.apvts, "wahOn", { { "wahPedal", "Pedal" } });

    addAndMakeVisible (compPanel);
    compPanel.layoutPedal ({}, "COMP  (Dyna Comp / CS-2)",
                           { { "compSustain", "Sustain" }, { "compLevel", "Level" } });
    compPanel.attach (processorRef.apvts, "compOn",
                      { { "compSustain", "Sustain" }, { "compLevel", "Level" } });

    addAndMakeVisible (fuzzPanel);
    fuzzPanel.layoutPedal ({}, "FUZZ  (Face / Muff)",
                           { { "fuzzDrive", "Drive" }, { "fuzzTone", "Tone" }, { "fuzzVol", "Vol" } });
    fuzzPanel.attach (processorRef.apvts, "fuzzOn",
                      { { "fuzzDrive", "Drive" }, { "fuzzTone", "Tone" }, { "fuzzVol", "Vol" } });

    fuzzModeBox.addItemList (juce::StringArray { "Fuzz Face", "Big Muff" }, 1);
    fuzzModeBox.setSelectedId (2);
    fuzzModeBox.setColour (juce::ComboBox::backgroundColourId, ChromeLook::panelGrey());
    fuzzModeBox.setColour (juce::ComboBox::textColourId, ChromeLook::textGrey());
    fuzzModeBox.setColour (juce::ComboBox::outlineColourId, juce::Colour (0xff888890));
    addAndMakeVisible (fuzzModeBox);
    fuzzModeAttachment = std::make_unique<juce::AudioProcessorValueTreeState::ComboBoxAttachment> (
        processorRef.apvts, "fuzzMode", fuzzModeBox);

    addAndMakeVisible (octavePanel);
    octavePanel.layoutPedal ({}, "OCTAVE", { { "octUp", "Oct+" }, { "octSub", "Sub" } });
    octavePanel.attach (processorRef.apvts, "octaveOn", { { "octUp", "Oct+" }, { "octSub", "Sub" } });

    addAndMakeVisible (vibePanel);
    vibePanel.layoutPedal ({}, "UNI-VIBE",
                           { { "vibeRate", "Rate" }, { "vibeDepth", "Depth" }, { "vibeMix", "Mix" } });
    vibePanel.attach (processorRef.apvts, "vibeOn",
                      { { "vibeRate", "Rate" }, { "vibeDepth", "Depth" }, { "vibeMix", "Mix" } });

    addAndMakeVisible (phaserPanel);
    phaserPanel.layoutPedal ({}, "PHASER  (Phase 90)",
                             { { "phRate", "Rate" }, { "phDepth", "Depth" }, { "phMix", "Mix" } });
    phaserPanel.attach (processorRef.apvts, "phaserOn",
                        { { "phRate", "Rate" }, { "phDepth", "Depth" }, { "phMix", "Mix" } });

    addAndMakeVisible (flangerPanel);
    flangerPanel.layoutPedal ({}, "FLANGER  (E. Mistress)",
                              { { "flRate", "Rate" }, { "flDepth", "Depth" }, { "flRegen", "Regen" }, { "flMix", "Mix" } });
    flangerPanel.attach (processorRef.apvts, "flangerOn",
                         { { "flRate", "Rate" }, { "flDepth", "Depth" }, { "flRegen", "Regen" }, { "flMix", "Mix" } });

    addAndMakeVisible (chorusPanel);
    chorusPanel.layoutPedal ({}, "CHORUS  (CE-2)",
                             { { "chRate", "Rate" }, { "chDepth", "Depth" }, { "chMix", "Mix" } });
    chorusPanel.attach (processorRef.apvts, "chorusOn",
                        { { "chRate", "Rate" }, { "chDepth", "Depth" }, { "chMix", "Mix" } });

    addAndMakeVisible (tremPanel);
    tremPanel.layoutPedal ({}, "TREMOLO  (Tremulator)", { { "trRate", "Rate" }, { "trDepth", "Depth" } });
    tremPanel.attach (processorRef.apvts, "tremOn", { { "trRate", "Rate" }, { "trDepth", "Depth" } });

    addAndMakeVisible (delayPanel);
    delayPanel.layoutPedal ({}, "DELAY  (Echorec / 2290)",
                            { { "delayTime", "Time" }, { "delayFb", "FB" }, { "delayMix", "Mix" } });
    delayPanel.attach (processorRef.apvts, "delayOn",
                       { { "delayTime", "Time" }, { "delayFb", "FB" }, { "delayMix", "Mix" } });

    addAndMakeVisible (reverbPanel);
    reverbPanel.layoutPedal ({}, "REVERB  (SPX90)",
                             { { "revMix", "Mix" }, { "revSize", "Size" }, { "revDecay", "Decay" } });
    reverbPanel.attach (processorRef.apvts, "reverbOn",
                        { { "revMix", "Mix" }, { "revSize", "Size" }, { "revDecay", "Decay" } });

    // ---- preset manager (header, right side) ----
    for (auto* b : { &prevPresetButton, &nextPresetButton, &savePresetButton, &deletePresetButton })
    {
        b->setColour (juce::TextButton::buttonColourId, ChromeLook::panelGrey());
        b->setColour (juce::TextButton::textColourOnId, ChromeLook::textGrey());
        b->setColour (juce::TextButton::textColourOffId, ChromeLook::textGrey());
        addAndMakeVisible (*b);
    }
    prevPresetButton.onClick = [this] { selectPreset (presetBox.getSelectedId() - 2); };
    nextPresetButton.onClick = [this] { selectPreset (presetBox.getSelectedId()); };
    savePresetButton.onClick = [this] { onSavePreset(); };
    deletePresetButton.onClick = [this] { onDeletePreset(); };

    presetBox.setColour (juce::ComboBox::backgroundColourId, ChromeLook::panelGrey());
    presetBox.setColour (juce::ComboBox::textColourId, ChromeLook::textGrey());
    presetBox.setColour (juce::ComboBox::outlineColourId, juce::Colour (0xff888890));
    presetBox.onChange = [this] { onPresetSelected(); };
    addAndMakeVisible (presetBox);
    refreshPresetList();

    // ---- master strip ----
    addAndMakeVisible (masterPanel);
    masterPanel.layoutPedal ({}, "MASTER", {});

    masterSlider.setSliderStyle (juce::Slider::RotaryHorizontalVerticalDrag);
    masterSlider.setTextBoxStyle (juce::Slider::NoTextBox, true, 0, 0);
    addAndMakeVisible (masterSlider);
    masterAttachment = std::make_unique<juce::AudioProcessorValueTreeState::SliderAttachment> (
        processorRef.apvts, "master", masterSlider);

    masterLabel.setText ("OUT", juce::dontSendNotification);
    masterLabel.setJustificationType (juce::Justification::centred);
    masterLabel.setColour (juce::Label::textColourId, ChromeLook::textGrey().withAlpha (0.8f));
    masterLabel.setFont (juce::FontOptions (9.5f));
    addAndMakeVisible (masterLabel);

    setSize (1180, 430);
    startTimerHz (30);
}

TheDGVSTAudioProcessorEditor::~TheDGVSTAudioProcessorEditor()
{
    setLookAndFeel (nullptr);
}

void TheDGVSTAudioProcessorEditor::paint (juce::Graphics& g)
{
    ChromeLook::paintPanelBackground (g, getLocalBounds());

    // header plate (brushed dark metal strip with chrome edge)
    auto header = getLocalBounds().removeFromTop (64).reduced (10, 6);
    juce::ColourGradient plate = juce::ColourGradient::vertical (
        juce::Colour (0xff34343a), header.getY(),
        juce::Colour (0xff1c1c20), header.getBottom());
    g.setGradientFill (plate);
    g.fillRoundedRectangle (header.toFloat(), 8.0f);
    g.setGradientFill (ChromeLook::chromeGradient (header.toFloat(), false));
    g.drawRoundedRectangle (header.toFloat(), 8.0f, 1.5f);
}

void TheDGVSTAudioProcessorEditor::resized()
{
    auto area = getLocalBounds().reduced (10);

    // header row: title on the left, preset manager on the right
    auto header = area.removeFromTop (64);
    titleLabel.setBounds (header.removeFromLeft (420).reduced (10, 6));
    subtitleLabel.setBounds (header.removeFromTop (0)); // subtitle sits under title via paint layout below
    subtitleLabel.setBounds (titleLabel.getX(), titleLabel.getBottom() + 2,
                             titleLabel.getWidth(), 18);

    auto presetArea = header.reduced (4);
    const int btnW = 34;
    deletePresetButton.setBounds (presetArea.removeFromRight (btnW).reduced (2));
    savePresetButton.setBounds (presetArea.removeFromRight (52).reduced (2));
    nextPresetButton.setBounds (presetArea.removeFromRight (btnW).reduced (2));
    prevPresetButton.setBounds (presetArea.removeFromRight (btnW).reduced (2));
    presetBox.setBounds (presetArea.reduced (2));

    auto pedalRow = area.removeFromTop (area.getHeight() - 8);
    pedalRow = pedalRow.reduced (2);

    // 11 pedals + master cell across the row
    juce::Component* panels[] = {
        &wahPanel, &compPanel, &fuzzPanel, &octavePanel, &vibePanel,
        &phaserPanel, &flangerPanel, &chorusPanel, &tremPanel, &delayPanel, &reverbPanel
    };
    const int n = 11;
    int cellW = pedalRow.getWidth() / n;
    for (int i = 0; i < n; ++i)
        panels[i]->setBounds (pedalRow.removeFromLeft (cellW).reduced (3));

    // master cell at the right end
    auto masterCell = area.removeFromRight (86).reduced (2);
    masterPanel.setBounds (masterCell.removeFromTop (masterCell.getHeight()));
    masterSlider.setBounds (masterCell.withSizeKeepingCentre (56, 56));
    masterLabel.setBounds (masterCell.removeFromBottom (14));

    // fuzz mode selector sits inside the fuzz pedal area (bottom of its knobs row)
    auto fuzzArea = fuzzPanel.getBounds();
    fuzzModeBox.setBounds (fuzzArea.getX() + 8, fuzzArea.getY() + 26,
                           fuzzArea.getWidth() - 16, 20);
}

void TheDGVSTAudioProcessorEditor::timerCallback()
{
    // keep footswitch LEDs / panel state in sync (cheap repaint of toggles)
    wahPanel.repaint(); compPanel.repaint(); fuzzPanel.repaint(); octavePanel.repaint();
    vibePanel.repaint(); phaserPanel.repaint(); flangerPanel.repaint(); chorusPanel.repaint();
    tremPanel.repaint(); delayPanel.repaint(); reverbPanel.repaint();

    // keep preset box in sync with host-driven program changes
    const int prog = processorRef.getCurrentProgram();
    if (presetBox.getSelectedId() != prog + 1)
        presetBox.setSelectedId (prog + 1, juce::dontSendNotification);
}

void TheDGVSTAudioProcessorEditor::refreshPresetList()
{
    const int current = presetBox.getSelectedId();
    presetBox.clear();
    auto names = processorRef.getAllPresetNames();
    for (int i = 0; i < names.size(); ++i)
        presetBox.addItem (names[i], i + 1);
    presetBox.setSelectedId (juce::jlimit (1, names.size(), current > 0 ? current : processorRef.getCurrentProgram() + 1),
                             juce::dontSendNotification);
}

void TheDGVSTAudioProcessorEditor::selectPreset (int itemId) // itemId is 1-based combo id
{
    if (itemId < 1 || itemId > processorRef.getNumPrograms())
        return;
    processorRef.setCurrentProgram (itemId - 1);
    presetBox.setSelectedId (itemId, juce::dontSendNotification);
}

void TheDGVSTAudioProcessorEditor::onPresetSelected()
{
    selectPreset (presetBox.getSelectedId());
}

void TheDGVSTAudioProcessorEditor::onSavePreset()
{
    // simple inline prompt via AlertWindow with an editable text field
    auto* aw = new juce::AlertWindow ("Save preset", "Preset name:", juce::MessageBoxIconType::NoIcon);
    aw->addTextEditor ("name", "My preset");
    aw->addButton ("Save", 1, juce::KeyPress (juce::KeyPress::returnKey));
    aw->addButton ("Cancel", 0, juce::KeyPress (juce::KeyPress::escapeKey));
    aw->enterModalState (true, juce::ModalCallbackFunction::create ([this, aw] (int result)
    {
        if (result == 1)
        {
            auto name = aw->getTextEditorContents ("name").trim();
            if (name.isNotEmpty())
            {
                processorRef.saveCurrentAsUserPreset (name);
                refreshPresetList();
                presetBox.setSelectedId (processorRef.getCurrentProgram() + 1, juce::dontSendNotification);
            }
        }
        delete aw;
    }), true);
}

void TheDGVSTAudioProcessorEditor::onDeletePreset()
{
    const int prog = processorRef.getCurrentProgram();
    const int nFactory = processorRef.getNumFactoryPresets();
    if (prog < nFactory)
    {
        juce::AlertWindow::showMessageBoxAsync (juce::MessageBoxIconType::InfoIcon,
            "Delete preset", "Factory presets (Pink Floyd songs) cannot be deleted.");
        return;
    }
    processorRef.deleteUserPreset (prog - nFactory);
    processorRef.setCurrentProgram (0);
    refreshPresetList();
    presetBox.setSelectedId (1, juce::dontSendNotification);
}

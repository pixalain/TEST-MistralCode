#pragma once

#include <juce_gui_basics/juce_gui_basics.h>

// Chrome / dark-grey hardware look for THE DG VST.
class ChromeLook : public juce::LookAndFeel_V4
{
public:
    ChromeLook();

    // dark grey brushed background
    static void paintPanelBackground (juce::Graphics&, juce::Rectangle<int> area);

    void drawRotarySlider (juce::Graphics&, int x, int y, int width, int height,
                           float sliderPosProportional, float rotaryStartAngle,
                           float rotaryEndAngle, juce::Slider&) override;

    void drawToggleButton (juce::Graphics&, juce::ToggleButton&, bool shouldDrawButtonAsHighlighted,
                           bool shouldDrawButtonAsDown) override;

    void drawButtonBackground (juce::Graphics&, juce::Button&, const juce::Colour& backgroundColour,
                               bool shouldDrawButtonAsHighlighted, bool shouldDrawButtonAsDown) override;

    void drawLabel (juce::Graphics&, juce::Label&) override;

    juce::Font getLabelFont (juce::Label&) override;

    // shared chrome gradient for a vertical band of height h
    static juce::ColourGradient chromeGradient (juce::Rectangle<float> area, bool pressed);

    static juce::Colour darkBackground()  { return juce::Colour (0xff1a1a1c); }
    static juce::Colour panelGrey()       { return juce::Colour (0xff2a2a2e); }
    static juce::Colour textGrey()        { return juce::Colour (0xffc8c8cc); }
    static juce::Colour ledGreen()        { return juce::Colour (0xff4dff6a); }
    static juce::Colour ledRed()          { return juce::Colour (0xffff4040); }
};

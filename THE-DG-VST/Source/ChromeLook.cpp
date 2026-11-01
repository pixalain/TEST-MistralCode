#include "ChromeLook.h"

ChromeLook::ChromeLook()
{
    setColour (juce::Slider::textBoxTextColourId, textGrey());
    setColour (juce::Label::textColourId, textGrey());
    setColour (juce::ToggleButton::textColourId, textGrey());
    setColour (juce::ComboBox::textColourId, textGrey());
}

juce::ColourGradient ChromeLook::chromeGradient (juce::Rectangle<float> area, bool pressed)
{
    // classic "chrome" vertical bands: dark -> bright highlight -> dark -> light
    auto top = pressed ? juce::Colour (0xff6a6a70) : juce::Colour (0xffe8e8ee);
    juce::ColourGradient g (top, 0.0f, area.getY(),
                            juce::Colour (0xffc9c9d1), 0.0f, area.getBottom(), false);
    g.addColour (0.28, juce::Colour (0xff3c3c42));
    g.addColour (0.52, juce::Colour (0xfff4f4f8));
    g.addColour (0.78, juce::Colour (0xff55555c));
    return g;
}

void ChromeLook::paintPanelBackground (juce::Graphics& g, juce::Rectangle<int> area)
{
    // dark charcoal background with a subtle vertical falloff
    juce::ColourGradient bg = juce::ColourGradient::vertical (
        juce::Colour (0xff232326), area.getY(),
        juce::Colour (0xff121214), area.getBottom());
    g.setGradientFill (bg);
    g.fillRect (area);

    // faint brushed-metal horizontal lines
    g.setColour (juce::Colour (0x0affffff));
    for (int y = area.getY(); y < area.getBottom(); y += 3)
        g.drawHorizontalLine (y, float (area.getX()), float (area.getRight()));

    // inner bevel
    g.setColour (juce::Colour (0x66000000));
    g.drawRect (area.reduced (1), 1.0f);
    g.setColour (juce::Colour (0x33ffffff));
    g.drawRect (area.reduced (2), 1.0f);
}

void ChromeLook::drawRotarySlider (juce::Graphics& g, int x, int y, int width, int height,
                                   float sliderPosProportional, float rotaryStartAngle,
                                   float rotaryEndAngle, juce::Slider& slider)
{
    auto bounds = juce::Rectangle<int> (x, y, width, height).toFloat();
    auto radius = juce::jmin (bounds.getWidth(), bounds.getHeight()) * 0.5f;
    auto centre = bounds.getCentre();
    auto angle = rotaryStartAngle + sliderPosProportional * (rotaryEndAngle - rotaryStartAngle);

    // outer chrome ring
    {
        auto ring = bounds.withSizeKeepingCentre (radius * 2.0f, radius * 2.0f);
        g.setGradientFill (chromeGradient (ring, slider.isEnabled() == false));
        g.fillEllipse (ring);
    }
    // dark inner cap
    {
        auto cap = bounds.withSizeKeepingCentre (radius * 1.62f, radius * 1.62f);
        juce::ColourGradient capGrad = juce::ColourGradient::vertical (
            juce::Colour (0xff3a3a40), cap.getY(),
            juce::Colour (0xff151517), cap.getBottom());
        g.setGradientFill (capGrad);
        g.fillEllipse (cap);
    }
    // chrome pointer line
    juce::Path pointer;
    auto pr = radius * 0.72f;
    pointer.addRectangle (-1.6f, -pr, 3.2f, pr * 0.9f);
    pointer.applyTransform (juce::AffineTransform::rotation (angle).translated (centre.x, centre.y));
    g.setGradientFill (chromeGradient (bounds, false));
    g.fillPath (pointer);

    // tick marks around the arc
    g.setColour (juce::Colour (0xff9a9aa2));
    for (int i = 0; i <= 10; ++i)
    {
        float a = rotaryStartAngle + (float (i) / 10.0f) * (rotaryEndAngle - rotaryStartAngle);
        auto p1 = centre + juce::Point<float> (std::sin (a), -std::cos (a)) * (radius * 1.06f);
        auto p2 = centre + juce::Point<float> (std::sin (a), -std::cos (a)) * (radius * 1.18f);
        g.drawLine (p1.x, p1.y, p2.x, p2.y, 1.2f);
    }
}

void ChromeLook::drawToggleButton (juce::Graphics& g, juce::ToggleButton& button,
                                   bool shouldDrawButtonAsHighlighted, bool shouldDrawButtonAsDown)
{
    auto bounds = button.getLocalBounds().toFloat();
    auto isOn = button.getToggleState();

    // chrome footswitch body (rounded rect)
    auto body = bounds.reduced (2.0f);
    g.setGradientFill (chromeGradient (body, shouldDrawButtonAsDown));
    g.fillRoundedRectangle (body, 6.0f);
    g.setColour (juce::Colour (0xff0c0c0e));
    g.drawRoundedRectangle (body, 6.0f, 1.4f);

    // LED
    auto led = juce::Rectangle<float> (body.getWidth() * 0.5f - 5.0f, 5.0f, 10.0f, 10.0f)
                   .translated (body.getX(), body.getY());
    auto ledColour = isOn ? ledGreen() : juce::Colour (0xff3a1410);
    if (isOn)
    {
        g.setColour (ledColour.withAlpha (0.35f));
        g.fillEllipse (led.expanded (4.0f));
    }
    g.setColour (ledColour);
    g.fillEllipse (led);
    g.setColour (juce::Colour (0x88000000));
    g.drawEllipse (led, 1.0f);

    // label under the switch
    g.setColour (isOn ? textGrey() : textGrey().withAlpha (0.55f));
    g.setFont (juce::FontOptions (11.0f).withStyle ("Bold"));
    auto labelArea = juce::Rectangle<float> (body.getX(), body.getBottom() - 16.0f, body.getWidth(), 14.0f);
    g.drawFittedText (button.getButtonText(), labelArea.toNearestInt(),
                      juce::Justification::centred, 1);
}

void ChromeLook::drawButtonBackground (juce::Graphics& g, juce::Button& button,
                                       const juce::Colour&, bool highlighted, bool down)
{
    auto bounds = button.getLocalBounds().toFloat().reduced (2.0f);
    g.setGradientFill (chromeGradient (bounds, down));
    g.fillRoundedRectangle (bounds, 8.0f);
    g.setColour (highlighted ? juce::Colour (0xffffffff) : juce::Colour (0xff101013));
    g.drawRoundedRectangle (bounds, 8.0f, highlighted ? 2.0f : 1.2f);
    g.setColour (textGrey());
    g.setFont (juce::FontOptions (13.0f).withStyle ("Bold"));
    g.drawFittedText (button.getButtonText(), button.getLocalBounds(),
                      juce::Justification::centred, 1);
}

void ChromeLook::drawLabel (juce::Graphics& g, juce::Label& label)
{
    g.setColour (label.findColour (juce::Label::textColourId));
    g.setFont (label.getFont());
    g.drawFittedText (label.getText(), label.getLocalBounds(),
                      label.getJustificationType(), 1);
}

juce::Font ChromeLook::getLabelFont (juce::Label& label)
{
    return juce::FontOptions (12.0f).withStyle ("Regular");
}

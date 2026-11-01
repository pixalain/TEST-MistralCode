#include "PluginProcessor.h"
#include "PluginEditor.h"

using Parameter = juce::AudioProcessorValueTreeState::Parameter;
using Layout = juce::AudioProcessorValueTreeState::ParameterLayout;

static Layout createParameterLayout()
{
    Layout layout;

    auto onOff = [] (const juce::String& id, bool def = false)
    {
        return std::make_unique<juce::AudioParameterBool> (juce::ParameterID { id, 1 }, id, def);
    };
    auto knob = [] (const juce::String& id, float def, float lo, float hi)
    {
        return std::make_unique<juce::AudioParameterFloat> (
            juce::ParameterID { id, 1 }, id,
            juce::NormalisableRange<float> (lo, hi), def);
    };

    // Wah (Cry Baby)
    layout.add (onOff ("wahOn"));
    layout.add (knob ("wahPedal", 0.7f, 0.0f, 1.0f));
    // Compressor (MXR Dyna Comp / Boss CS-2)
    layout.add (onOff ("compOn"));
    layout.add (knob ("compSustain", 0.4f, 0.0f, 1.0f));
    layout.add (knob ("compLevel", 0.6f, 0.0f, 1.0f));
    // Fuzz (Fuzz Face / Big Muff)
    layout.add (onOff ("fuzzOn"));
    layout.add (std::make_unique<juce::AudioParameterChoice> (
        juce::ParameterID { "fuzzMode", 1 }, "fuzzMode",
        juce::StringArray { "Fuzz Face", "Big Muff" }, 1));
    layout.add (knob ("fuzzDrive", 0.7f, 0.0f, 1.0f));
    layout.add (knob ("fuzzTone", 0.5f, 0.0f, 1.0f));
    layout.add (knob ("fuzzVol", 0.8f, 0.0f, 1.0f));
    // Octave
    layout.add (onOff ("octaveOn"));
    layout.add (knob ("octUp", 0.6f, 0.0f, 1.0f));
    layout.add (knob ("octSub", 0.0f, 0.0f, 1.0f));
    // Uni-Vibe
    layout.add (onOff ("vibeOn"));
    layout.add (knob ("vibeRate", 0.3f, 0.0f, 1.0f));
    layout.add (knob ("vibeDepth", 0.7f, 0.0f, 1.0f));
    layout.add (knob ("vibeMix", 0.6f, 0.0f, 1.0f));
    // Phaser (Phase 90)
    layout.add (onOff ("phaserOn"));
    layout.add (knob ("phRate", 0.25f, 0.0f, 1.0f));
    layout.add (knob ("phDepth", 0.7f, 0.0f, 1.0f));
    layout.add (knob ("phMix", 0.5f, 0.0f, 1.0f));
    // Flanger (Electric Mistress)
    layout.add (onOff ("flangerOn"));
    layout.add (knob ("flRate", 0.2f, 0.0f, 1.0f));
    layout.add (knob ("flDepth", 0.6f, 0.0f, 1.0f));
    layout.add (knob ("flRegen", 0.4f, 0.0f, 1.0f));
    layout.add (knob ("flMix", 0.5f, 0.0f, 1.0f));
    // Chorus (CE-2)
    layout.add (onOff ("chorusOn"));
    layout.add (knob ("chRate", 0.25f, 0.0f, 1.0f));
    layout.add (knob ("chDepth", 0.5f, 0.0f, 1.0f));
    layout.add (knob ("chMix", 0.5f, 0.0f, 1.0f));
    // Tremolo (Tremulator)
    layout.add (onOff ("tremOn"));
    layout.add (knob ("trRate", 0.3f, 0.0f, 1.0f));
    layout.add (knob ("trDepth", 0.6f, 0.0f, 1.0f));
    // Delay (Binson Echorec / TC 2290)
    layout.add (onOff ("delayOn"));
    layout.add (knob ("delayTime", 450.0f, 20.0f, 1500.0f));
    layout.add (knob ("delayFb", 0.35f, 0.0f, 0.85f));
    layout.add (knob ("delayMix", 0.3f, 0.0f, 1.0f));
    // Reverb (SPX90-style)
    layout.add (onOff ("reverbOn", true));
    layout.add (knob ("revMix", 0.3f, 0.0f, 1.0f));
    layout.add (knob ("revSize", 0.7f, 0.0f, 1.0f));
    layout.add (knob ("revDecay", 0.75f, 0.0f, 1.0f));
    // Master
    layout.add (knob ("master", 0.8f, 0.0f, 1.0f));

    return layout;
}

TheDGVSTAudioProcessor::TheDGVSTAudioProcessor()
    : AudioProcessor (BusesProperties()
          .withInput  ("Input",  juce::AudioChannelSet::stereo(), true)
          .withOutput ("Output", juce::AudioChannelSet::stereo(), true)),
      apvts (*this, nullptr, "PARAMETERS", createParameterLayout())
{
}

TheDGVSTAudioProcessor::~TheDGVSTAudioProcessor() = default;

const juce::String TheDGVSTAudioProcessor::getName() const { return JucePlugin_Name; }
bool TheDGVSTAudioProcessor::acceptsMidi() const { return true; }  // CC1 drives the wah
bool TheDGVSTAudioProcessor::producesMidi() const { return false; }
bool TheDGVSTAudioProcessor::isMidiEffect() const { return false; }
double TheDGVSTAudioProcessor::getTailLengthSeconds() const { return 3.0; }

int TheDGVSTAudioProcessor::getNumPrograms() { return 1; }
int TheDGVSTAudioProcessor::getCurrentProgram() { return 0; }
void TheDGVSTAudioProcessor::setCurrentProgram (int) {}
const juce::String TheDGVSTAudioProcessor::getProgramName (int) { return "Default"; }
void TheDGVSTAudioProcessor::changeProgramName (int, const juce::String&) {}

void TheDGVSTAudioProcessor::prepareToPlay (double sampleRate, int)
{
    dspL.setSampleRate (sampleRate);
    dspR.setSampleRate (sampleRate);
    updateChainFromParameters();
}

void TheDGVSTAudioProcessor::releaseResources()
{
    dspL.reset();
    dspR.reset();
}

bool TheDGVSTAudioProcessor::isBusesLayoutSupported (const BusesLayout& layouts) const
{
    if (layouts.getMainOutputChannelSet() != juce::AudioChannelSet::mono()
        && layouts.getMainOutputChannelSet() != juce::AudioChannelSet::stereo())
        return false;
    return layouts.getMainInputChannelSet() == layouts.getMainOutputChannelSet();
}

static float param (juce::AudioProcessorValueTreeState& apvts, const char* id)
{
    return apvts.getRawParameterValue (id)->load();
}
static bool paramBool (juce::AudioProcessorValueTreeState& apvts, const char* id)
{
    return apvts.getRawParameterValue (id)->load() > 0.5f;
}

void TheDGVSTAudioProcessor::updateChainFromParameters()
{
    auto& p = chainParams;
    p.wahOn     = paramBool (apvts, "wahOn");
    p.wahPedal  = param (apvts, "wahPedal");
    p.compOn    = paramBool (apvts, "compOn");
    p.compSustain = param (apvts, "compSustain");
    p.compLevel   = param (apvts, "compLevel");
    p.fuzzOn    = paramBool (apvts, "fuzzOn");
    p.fuzzMode  = (int) param (apvts, "fuzzMode");
    p.fuzzDrive = param (apvts, "fuzzDrive");
    p.fuzzTone  = param (apvts, "fuzzTone");
    p.fuzzVol   = param (apvts, "fuzzVol");
    p.octaveOn  = paramBool (apvts, "octaveOn");
    p.octUp     = param (apvts, "octUp");
    p.octSub    = param (apvts, "octSub");
    p.vibeOn    = paramBool (apvts, "vibeOn");
    p.vibeRate  = param (apvts, "vibeRate");
    p.vibeDepth = param (apvts, "vibeDepth");
    p.vibeMix   = param (apvts, "vibeMix");
    p.phaserOn  = paramBool (apvts, "phaserOn");
    p.phRate    = param (apvts, "phRate");
    p.phDepth   = param (apvts, "phDepth");
    p.phMix     = param (apvts, "phMix");
    p.flangerOn = paramBool (apvts, "flangerOn");
    p.flRate    = param (apvts, "flRate");
    p.flDepth   = param (apvts, "flDepth");
    p.flRegen   = param (apvts, "flRegen");
    p.flMix     = param (apvts, "flMix");
    p.chorusOn  = paramBool (apvts, "chorusOn");
    p.chRate    = param (apvts, "chRate");
    p.chDepth   = param (apvts, "chDepth");
    p.chMix     = param (apvts, "chMix");
    p.tremOn    = paramBool (apvts, "tremOn");
    p.trRate    = param (apvts, "trRate");
    p.trDepth   = param (apvts, "trDepth");
    p.delayOn   = paramBool (apvts, "delayOn");
    p.delayTime = param (apvts, "delayTime");
    p.delayFb   = param (apvts, "delayFb");
    p.delayMix  = param (apvts, "delayMix");
    p.reverbOn  = paramBool (apvts, "reverbOn");
    p.revMix    = param (apvts, "revMix");
    p.revSize   = param (apvts, "revSize");
    p.revDecay  = param (apvts, "revDecay");
    p.master    = param (apvts, "master");

    dspL.update (p);
    dspR.update (p);
}

void TheDGVSTAudioProcessor::processBlock (juce::AudioBuffer<float>& buffer,
                                           juce::MidiBuffer& midiMessages)
{
    juce::ScopedNoDenormals noDenormals;

    // MIDI CC1 (mod wheel) -> wah pedal position
    for (const auto metadata : midiMessages)
    {
        const auto m = metadata.getMessage();
        if (m.isController() && m.getControllerNumber() == 1)
            wahCc.store (m.getControllerValue() / 127.0f);
    }
    const float cc = wahCc.load();
    if (cc >= 0.0f && paramBool (apvts, "wahOn"))
        chainParams.wahPedal = cc;

    updateChainFromParameters();

    const int numSamples = buffer.getNumSamples();
    const int numChannels = buffer.getNumChannels();

    if (numChannels >= 2)
    {
        dspL.processBlock (buffer.getWritePointer (0), numSamples);
        dspR.processBlock (buffer.getWritePointer (1), numSamples);
    }
    else if (numChannels == 1)
    {
        dspL.processBlock (buffer.getWritePointer (0), numSamples);
    }
}

bool TheDGVSTAudioProcessor::hasEditor() const { return true; }
juce::AudioProcessorEditor* TheDGVSTAudioProcessor::createEditor()
{
    return new TheDGVSTAudioProcessorEditor (*this);
}

void TheDGVSTAudioProcessor::getStateInformation (juce::MemoryBlock& destData)
{
    auto state = apvts.copyState();
    if (auto xml = state.createXml())
        copyXmlToBinary (*xml, destData);
}

void TheDGVSTAudioProcessor::setStateInformation (const void* data, int sizeInBytes)
{
    if (auto xml = getXmlFromBinary (data, sizeInBytes))
        apvts.replaceState (juce::ValueTree::fromXml (*xml));
}

juce::AudioProcessor* JUCE_CALLTYPE createPluginFilter()
{
    return new TheDGVSTAudioProcessor();
}

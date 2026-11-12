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

static float param (const juce::AudioProcessorValueTreeState& apvts, const char* id)
{
    return apvts.getRawParameterValue (id)->load();
}
static bool paramBool (const juce::AudioProcessorValueTreeState& apvts, const char* id)
{
    return apvts.getRawParameterValue (id)->load() > 0.5f;
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

int TheDGVSTAudioProcessor::getNumPrograms()
{
    return (int) dg::factoryPresets().size() + (int) userPresets.size();
}

int TheDGVSTAudioProcessor::getNumFactoryPresets() const
{
    return (int) dg::factoryPresets().size();
}

int TheDGVSTAudioProcessor::getCurrentProgram()
{
    return currentProgram.load();
}

void TheDGVSTAudioProcessor::setCurrentProgram (int index)
{
    const int nFactory = (int) dg::factoryPresets().size();
    if (index >= 0 && index < nFactory)
        loadFactoryPreset (index);
    else if (index >= nFactory && index < getNumPrograms())
        loadUserPreset (index - nFactory);
}

const juce::String TheDGVSTAudioProcessor::getProgramName (int index)
{
    const int nFactory = (int) dg::factoryPresets().size();
    if (index >= 0 && index < nFactory)
        return dg::factoryPresets()[(size_t) index].name;
    if (index >= nFactory && index < getNumPrograms())
        return userPresets[(size_t) (index - nFactory)];
    return {};
}

void TheDGVSTAudioProcessor::changeProgramName (int index, const juce::String& newName)
{
    const int nFactory = (int) dg::factoryPresets().size();
    if (index >= nFactory && index < getNumPrograms() && newName.isNotEmpty())
        userPresets[(size_t) (index - nFactory)] = newName;
}

void TheDGVSTAudioProcessor::loadFactoryPreset (int index)
{
    if (index < 0 || index >= (int) dg::factoryPresets().size())
        return;
    applyChainParamsToParameters (dg::factoryPresets()[(size_t) index].params);
    currentProgram = index;
    // NOTE: only atomic parameter writes here - the audio thread picks them up
    // on the next block. Touching the DSP from this thread would race with it.
}

void TheDGVSTAudioProcessor::loadUserPreset (int index)
{
    if (index < 0 || index >= (int) userPresetStates.size())
        return;
    apvts.replaceState (userPresetStates[(size_t) index]);
    currentProgram = (int) dg::factoryPresets().size() + index;
}

juce::String TheDGVSTAudioProcessor::getUserPresetName (int index) const
{
    if (index >= 0 && index < (int) userPresets.size())
        return userPresets[(size_t) index];
    return {};
}

void TheDGVSTAudioProcessor::saveCurrentAsUserPreset (const juce::String& name)
{
    auto state = apvts.copyState();
    state.setProperty ("presetName", name, nullptr);
    userPresets.push_back (name);
    userPresetStates.push_back (state);
    currentProgram = (int) dg::factoryPresets().size() + (int) userPresets.size() - 1;
}

void TheDGVSTAudioProcessor::deleteUserPreset (int index)
{
    if (index < 0 || index >= (int) userPresets.size())
        return;
    userPresets.erase (userPresets.begin() + index);
    userPresetStates.erase (userPresetStates.begin() + index);
}

juce::StringArray TheDGVSTAudioProcessor::getAllPresetNames() const
{
    juce::StringArray names;
    for (const auto& p : dg::factoryPresets())
        names.add (p.name);
    for (const auto& n : userPresets)
        names.add (n);
    return names;
}

void TheDGVSTAudioProcessor::applyChainParamsToParameters (const dg::ChainParams& p)
{
    auto set = [this] (const char* id, float v)
    {
        if (auto* param = apvts.getParameter (id))
            param->setValueNotifyingHost (param->convertTo0to1 (v));
    };
    set ("wahOn",     p.wahOn ? 1.0f : 0.0f);
    set ("wahPedal",  p.wahPedal);
    set ("compOn",    p.compOn ? 1.0f : 0.0f);
    set ("compSustain", p.compSustain);
    set ("compLevel",   p.compLevel);
    set ("fuzzOn",    p.fuzzOn ? 1.0f : 0.0f);
    set ("fuzzMode",  (float) p.fuzzMode);
    set ("fuzzDrive", p.fuzzDrive);
    set ("fuzzTone",  p.fuzzTone);
    set ("fuzzVol",   p.fuzzVol);
    set ("octaveOn",  p.octaveOn ? 1.0f : 0.0f);
    set ("octUp",     p.octUp);
    set ("octSub",    p.octSub);
    set ("vibeOn",    p.vibeOn ? 1.0f : 0.0f);
    set ("vibeRate",  p.vibeRate);
    set ("vibeDepth", p.vibeDepth);
    set ("vibeMix",   p.vibeMix);
    set ("phaserOn",  p.phaserOn ? 1.0f : 0.0f);
    set ("phRate",    p.phRate);
    set ("phDepth",   p.phDepth);
    set ("phMix",     p.phMix);
    set ("flangerOn", p.flangerOn ? 1.0f : 0.0f);
    set ("flRate",    p.flRate);
    set ("flDepth",   p.flDepth);
    set ("flRegen",   p.flRegen);
    set ("flMix",     p.flMix);
    set ("chorusOn",  p.chorusOn ? 1.0f : 0.0f);
    set ("chRate",    p.chRate);
    set ("chDepth",   p.chDepth);
    set ("chMix",     p.chMix);
    set ("tremOn",    p.tremOn ? 1.0f : 0.0f);
    set ("trRate",    p.trRate);
    set ("trDepth",   p.trDepth);
    set ("delayOn",   p.delayOn ? 1.0f : 0.0f);
    set ("delayTime", p.delayTime);
    set ("delayFb",   p.delayFb);
    set ("delayMix",  p.delayMix);
    set ("reverbOn",  p.reverbOn ? 1.0f : 0.0f);
    set ("revMix",    p.revMix);
    set ("revSize",   p.revSize);
    set ("revDecay",  p.revDecay);
    set ("master",    p.master);
}

void TheDGVSTAudioProcessor::readParametersInto (dg::ChainParams& p) const
{
    p.wahOn = paramBool (apvts, "wahOn"); p.wahPedal = param (apvts, "wahPedal");
    p.compOn = paramBool (apvts, "compOn"); p.compSustain = param (apvts, "compSustain"); p.compLevel = param (apvts, "compLevel");
    p.fuzzOn = paramBool (apvts, "fuzzOn"); p.fuzzMode = (int) param (apvts, "fuzzMode");
    p.fuzzDrive = param (apvts, "fuzzDrive"); p.fuzzTone = param (apvts, "fuzzTone"); p.fuzzVol = param (apvts, "fuzzVol");
    p.octaveOn = paramBool (apvts, "octaveOn"); p.octUp = param (apvts, "octUp"); p.octSub = param (apvts, "octSub");
    p.vibeOn = paramBool (apvts, "vibeOn"); p.vibeRate = param (apvts, "vibeRate"); p.vibeDepth = param (apvts, "vibeDepth"); p.vibeMix = param (apvts, "vibeMix");
    p.phaserOn = paramBool (apvts, "phaserOn"); p.phRate = param (apvts, "phRate"); p.phDepth = param (apvts, "phDepth"); p.phMix = param (apvts, "phMix");
    p.flangerOn = paramBool (apvts, "flangerOn"); p.flRate = param (apvts, "flRate"); p.flDepth = param (apvts, "flDepth"); p.flRegen = param (apvts, "flRegen"); p.flMix = param (apvts, "flMix");
    p.chorusOn = paramBool (apvts, "chorusOn"); p.chRate = param (apvts, "chRate"); p.chDepth = param (apvts, "chDepth"); p.chMix = param (apvts, "chMix");
    p.tremOn = paramBool (apvts, "tremOn"); p.trRate = param (apvts, "trRate"); p.trDepth = param (apvts, "trDepth");
    p.delayOn = paramBool (apvts, "delayOn"); p.delayTime = param (apvts, "delayTime"); p.delayFb = param (apvts, "delayFb"); p.delayMix = param (apvts, "delayMix");
    p.reverbOn = paramBool (apvts, "reverbOn"); p.revMix = param (apvts, "revMix"); p.revSize = param (apvts, "revSize"); p.revDecay = param (apvts, "revDecay");
    p.master = param (apvts, "master");
}

void TheDGVSTAudioProcessor::prepareToPlay (double sampleRate, int)
{
    dspL.setSampleRate (sampleRate);
    dspR.setSampleRate (sampleRate);
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
    // Build the parameter snapshot ON THE AUDIO THREAD from the atomic raw
    // values only (lock-free, no shared struct with the message thread).
    dg::ChainParams p;
    readParametersInto (p);

    const float cc = wahCc.load();
    if (cc >= 0.0f && p.wahOn)
        p.wahPedal = cc;

    dspL.update (p);
    dspR.update (p);

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

    auto userBank = juce::ValueTree ("UserPresets");
    for (size_t i = 0; i < userPresetStates.size(); ++i)
    {
        auto copy = userPresetStates[i].createCopy();
        copy.setProperty ("presetName", userPresets[i], nullptr);
        userBank.addChild (copy, -1, nullptr);
    }
    state.addChild (userBank, -1, nullptr);
    state.setProperty ("currentProgram", currentProgram.load(), nullptr);

    if (auto xml = state.createXml())
        copyXmlToBinary (*xml, destData);
}

void TheDGVSTAudioProcessor::setStateInformation (const void* data, int sizeInBytes)
{
    if (auto xml = getXmlFromBinary (data, sizeInBytes))
    {
        auto state = juce::ValueTree::fromXml (*xml);
        auto bank = state.getChildWithName ("UserPresets");
        if (bank.isValid())
        {
            userPresets.clear();
            userPresetStates.clear();
            for (int i = 0; i < bank.getNumChildren(); ++i)
            {
                auto child = bank.getChild (i);
                userPresetStates.push_back (child);
                userPresets.push_back (child.getProperty ("presetName").toString());
            }
            state.removeChild (bank, nullptr);
        }
        if (state.hasProperty ("currentProgram"))
            currentProgram = (int) state.getProperty ("currentProgram");
        state.removeProperty ("currentProgram", nullptr);
        apvts.replaceState (state);
    }
}

juce::AudioProcessor* JUCE_CALLTYPE createPluginFilter()
{
    return new TheDGVSTAudioProcessor();
}

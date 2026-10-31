#include "PluginProcessor.h"
#include "PluginEditor.h"

namespace
{
juce::AudioProcessorValueTreeState::ParameterLayout createParameterLayout()
{
    std::vector<std::unique_ptr<juce::RangedAudioParameter>> params;

    params.push_back (std::make_unique<juce::AudioParameterFloat> (
        juce::ParameterID { "gain", 1 },
        "Gain",
        juce::NormalisableRange<float> (0.0f, 1.0f, 0.01f),
        0.8f));

    params.push_back (std::make_unique<juce::AudioParameterFloat> (
        juce::ParameterID { "reverbMix", 1 },
        "Reverb Mix",
        juce::NormalisableRange<float> (0.0f, 1.0f, 0.01f),
        0.25f));

    params.push_back (std::make_unique<juce::AudioParameterFloat> (
        juce::ParameterID { "reverbRoom", 1 },
        "Room Size",
        juce::NormalisableRange<float> (0.0f, 1.0f, 0.01f),
        0.6f));

    params.push_back (std::make_unique<juce::AudioParameterFloat> (
        juce::ParameterID { "reverbDamping", 1 },
        "Damping",
        juce::NormalisableRange<float> (0.0f, 1.0f, 0.01f),
        0.4f));

    return { params.begin(), params.end() };
}
}

MonPremierVSTAudioProcessor::MonPremierVSTAudioProcessor()
    : AudioProcessor (BusesProperties()
          .withInput  ("Input",  juce::AudioChannelSet::stereo(), true)
          .withOutput ("Output", juce::AudioChannelSet::stereo(), true)),
      parameters (*this, nullptr, "PARAMETERS", createParameterLayout())
{
}

MonPremierVSTAudioProcessor::~MonPremierVSTAudioProcessor() = default;

const juce::String MonPremierVSTAudioProcessor::getName() const { return "VST by IA"; }

bool MonPremierVSTAudioProcessor::acceptsMidi() const   { return false; }
bool MonPremierVSTAudioProcessor::producesMidi() const  { return false; }
bool MonPremierVSTAudioProcessor::isMidiEffect() const  { return false; }
double MonPremierVSTAudioProcessor::getTailLengthSeconds() const { return 3.0; }

int MonPremierVSTAudioProcessor::getNumPrograms()   { return 1; }
int MonPremierVSTAudioProcessor::getCurrentProgram() { return 0; }
void MonPremierVSTAudioProcessor::setCurrentProgram (int) {}
const juce::String MonPremierVSTAudioProcessor::getProgramName (int) { return {}; }
void MonPremierVSTAudioProcessor::changeProgramName (int, const juce::String&) {}

void MonPremierVSTAudioProcessor::prepareToPlay (double sampleRate, int)
{
    reverb.setSampleRate (sampleRate);
    reverbParams.roomSize = 0.6f;
    reverbParams.damping  = 0.4f;
    reverbParams.wetLevel = 1.0f;
    reverbParams.dryLevel = 1.0f;
    reverbParams.width    = 1.0f;
    reverb.setParameters (reverbParams);
}
void MonPremierVSTAudioProcessor::releaseResources() { reverb.reset(); }

bool MonPremierVSTAudioProcessor::isBusesLayoutSupported (const BusesLayout& layouts) const
{
    return layouts.getMainInputChannelSet()  == juce::AudioChannelSet::stereo()
        && layouts.getMainOutputChannelSet() == juce::AudioChannelSet::stereo();
}

void MonPremierVSTAudioProcessor::processBlock (juce::AudioBuffer<float>& buffer, juce::MidiBuffer&)
{
    juce::ScopedNoDenormals noDenormals;

    const auto gain    = parameters.getRawParameterValue ("gain")->load();
    const auto mix     = parameters.getRawParameterValue ("reverbMix")->load();
    const auto room    = parameters.getRawParameterValue ("reverbRoom")->load();
    const auto damping = parameters.getRawParameterValue ("reverbDamping")->load();

    reverbParams.roomSize = room;
    reverbParams.damping  = damping;
    reverb.setParameters (reverbParams);

    const int numSamples  = buffer.getNumSamples();
    const int numChannels = buffer.getNumChannels();

    float* channels[2] = { nullptr, nullptr };

    if (numChannels == 1)
    {
        channels[0] = buffer.getWritePointer (0);
        reverb.processMono (channels[0], numSamples);
    }
    else
    {
        channels[0] = buffer.getWritePointer (0);
        channels[1] = buffer.getWritePointer (1);
        reverb.processStereo (channels[0], channels[1], numSamples);
    }

    reverbParams.wetLevel = mix;
    reverbParams.dryLevel = 1.0f - mix;
    reverb.setParameters (reverbParams);

    buffer.applyGain (gain);
}

bool MonPremierVSTAudioProcessor::hasEditor() const { return true; }

juce::AudioProcessorEditor* MonPremierVSTAudioProcessor::createEditor()
{
    return new MonPremierVSTAudioProcessorEditor (*this);
}

void MonPremierVSTAudioProcessor::getStateInformation (juce::MemoryBlock& destData)
{
    if (auto xml = parameters.copyState().createXml())
        copyXmlToBinary (*xml, destData);
}

void MonPremierVSTAudioProcessor::setStateInformation (const void* data, int sizeInBytes)
{
    if (auto xml = getXmlFromBinary (data, sizeInBytes))
        parameters.replaceState (juce::ValueTree::fromXml (*xml));
}

juce::AudioProcessor* JUCE_CALLTYPE createPluginFilter()
{
    return new MonPremierVSTAudioProcessor();
}

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
        0.5f));

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

const juce::String MonPremierVSTAudioProcessor::getName() const { return "MonPremierVST"; }

bool MonPremierVSTAudioProcessor::acceptsMidi() const   { return false; }
bool MonPremierVSTAudioProcessor::producesMidi() const  { return false; }
bool MonPremierVSTAudioProcessor::isMidiEffect() const  { return false; }
double MonPremierVSTAudioProcessor::getTailLengthSeconds() const { return 0.0; }

int MonPremierVSTAudioProcessor::getNumPrograms()   { return 1; }
int MonPremierVSTAudioProcessor::getCurrentProgram() { return 0; }
void MonPremierVSTAudioProcessor::setCurrentProgram (int) {}
const juce::String MonPremierVSTAudioProcessor::getProgramName (int) { return {}; }
void MonPremierVSTAudioProcessor::changeProgramName (int, const juce::String&) {}

void MonPremierVSTAudioProcessor::prepareToPlay (double, int) {}
void MonPremierVSTAudioProcessor::releaseResources() {}

bool MonPremierVSTAudioProcessor::isBusesLayoutSupported (const BusesLayout& layouts) const
{
    return layouts.getMainInputChannelSet()  == juce::AudioChannelSet::stereo()
        && layouts.getMainOutputChannelSet() == juce::AudioChannelSet::stereo();
}

void MonPremierVSTAudioProcessor::processBlock (juce::AudioBuffer<float>& buffer, juce::MidiBuffer&)
{
    juce::ScopedNoDenormals noDenormals;

    const auto gain = parameters.getRawParameterValue ("gain")->load();

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

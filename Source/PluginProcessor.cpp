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
        juce::ParameterID { "delayTime", 1 },
        "Delay Time",
        juce::NormalisableRange<float> (0.01f, 1.0f, 0.001f),
        0.35f,
        juce::AudioParameterFloatAttributes().withLabel ("s")));

    params.push_back (std::make_unique<juce::AudioParameterFloat> (
        juce::ParameterID { "delayFeedback", 1 },
        "Feedback",
        juce::NormalisableRange<float> (0.0f, 0.9f, 0.01f),
        0.35f));

    params.push_back (std::make_unique<juce::AudioParameterFloat> (
        juce::ParameterID { "delayMix", 1 },
        "Delay Mix",
        juce::NormalisableRange<float> (0.0f, 1.0f, 0.01f),
        0.3f));

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

VSTbyIAAudioProcessor::VSTbyIAAudioProcessor()
    : AudioProcessor (BusesProperties()
          .withInput  ("Input",  juce::AudioChannelSet::stereo(), true)
          .withOutput ("Output", juce::AudioChannelSet::stereo(), true)),
      parameters (*this, nullptr, "PARAMETERS", createParameterLayout())
{
}

VSTbyIAAudioProcessor::~VSTbyIAAudioProcessor() = default;

const juce::String VSTbyIAAudioProcessor::getName() const { return "VST by IA"; }

bool VSTbyIAAudioProcessor::acceptsMidi() const   { return false; }
bool VSTbyIAAudioProcessor::producesMidi() const  { return false; }
bool VSTbyIAAudioProcessor::isMidiEffect() const  { return false; }
double VSTbyIAAudioProcessor::getTailLengthSeconds() const { return 4.0; }

int VSTbyIAAudioProcessor::getNumPrograms()    { return 1; }
int VSTbyIAAudioProcessor::getCurrentProgram() { return 0; }
void VSTbyIAAudioProcessor::setCurrentProgram (int) {}
const juce::String VSTbyIAAudioProcessor::getProgramName (int) { return {}; }
void VSTbyIAAudioProcessor::changeProgramName (int, const juce::String&) {}

void VSTbyIAAudioProcessor::prepareToPlay (double sampleRate, int samplesPerBlock)
{
    reverb.setSampleRate (sampleRate);
    reverbParams.roomSize = 0.6f;
    reverbParams.damping  = 0.4f;
    reverbParams.wetLevel = 1.0f;
    reverbParams.dryLevel = 1.0f;
    reverbParams.width    = 1.0f;
    reverb.setParameters (reverbParams);

    juce::dsp::ProcessSpec spec { sampleRate, (juce::uint32) samplesPerBlock, 2 };
    delayLineL.prepare (spec);
    delayLineR.prepare (spec);
    delayLineL.reset();
    delayLineR.reset();

    const float initialDelay = juce::jlimit (1.0f, maxDelaySamples - 1.0f, 0.35f * (float) sampleRate);
    delaySmoothL.reset (sampleRate, 0.03);
    delaySmoothR.reset (sampleRate, 0.03);
    delaySmoothL.setCurrentAndTargetValue (initialDelay);
    delaySmoothR.setCurrentAndTargetValue (initialDelay);
}

void VSTbyIAAudioProcessor::releaseResources()
{
    reverb.reset();
    delayLineL.reset();
    delayLineR.reset();
}

bool VSTbyIAAudioProcessor::isBusesLayoutSupported (const BusesLayout& layouts) const
{
    return layouts.getMainInputChannelSet()  == juce::AudioChannelSet::stereo()
        && layouts.getMainOutputChannelSet() == juce::AudioChannelSet::stereo();
}

void VSTbyIAAudioProcessor::processBlock (juce::AudioBuffer<float>& buffer, juce::MidiBuffer&)
{
    juce::ScopedNoDenormals noDenormals;

    const auto gain          = parameters.getRawParameterValue ("gain")->load();
    const auto delayTime     = parameters.getRawParameterValue ("delayTime")->load();
    const auto delayFeedback = parameters.getRawParameterValue ("delayFeedback")->load();
    const auto delayMix      = parameters.getRawParameterValue ("delayMix")->load();
    const auto revMix        = parameters.getRawParameterValue ("reverbMix")->load();
    const auto room          = parameters.getRawParameterValue ("reverbRoom")->load();
    const auto damping       = parameters.getRawParameterValue ("reverbDamping")->load();

    reverbParams.roomSize = room;
    reverbParams.damping  = damping;
    reverbParams.wetLevel = revMix;
    reverbParams.dryLevel = 1.0f - revMix;
    reverb.setParameters (reverbParams);

    const float sr = (float) getSampleRate();
    const float targetDelay = juce::jlimit (1.0f, maxDelaySamples - 1.0f, delayTime * sr);
    delaySmoothL.setTargetValue (targetDelay);
    delaySmoothR.setTargetValue (targetDelay);

    const int numSamples  = buffer.getNumSamples();
    const int numChannels = buffer.getNumChannels();

    DelayLineType* lines[2]                 = { &delayLineL, &delayLineR };
    juce::SmoothedValue<float>* smooths[2]  = { &delaySmoothL, &delaySmoothR };

    for (int ch = 0; ch < numChannels && ch < 2; ++ch)
    {
        auto* data = buffer.getWritePointer (ch);
        auto* line = lines[ch];
        auto* sm   = smooths[ch];

        for (int i = 0; i < numSamples; ++i)
        {
            const float in      = data[i];
            const float delayed = line->pop();

            data[i] = in + delayed * delayMix;
            line->push (in + delayed * delayFeedback);
            line->setDelay (sm->getNextValue());
        }
    }

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

    buffer.applyGain (gain);
}

bool VSTbyIAAudioProcessor::hasEditor() const { return true; }

juce::AudioProcessorEditor* VSTbyIAAudioProcessor::createEditor()
{
    return new VSTbyIAAudioProcessorEditor (*this);
}

void VSTbyIAAudioProcessor::getStateInformation (juce::MemoryBlock& destData)
{
    if (auto xml = parameters.copyState().createXml())
        copyXmlToBinary (*xml, destData);
}

void VSTbyIAAudioProcessor::setStateInformation (const void* data, int sizeInBytes)
{
    if (auto xml = getXmlFromBinary (data, sizeInBytes))
        parameters.replaceState (juce::ValueTree::fromXml (*xml));
}

juce::AudioProcessor* JUCE_CALLTYPE createPluginFilter()
{
    return new VSTbyIAAudioProcessor();
}

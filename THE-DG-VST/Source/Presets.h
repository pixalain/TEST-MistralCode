#pragma once

#include "DspChain.h"
#include <vector>

// ============================================================================
// Factory presets for THE DG VST — Pink Floyd flagship songs.
// Pure C++ (no JUCE), so the DSP unit tests can exercise them too.
// Settings are inspired by the documented Gilmour rigs per song
// (kitrae.net, gilmourish.com, equipboard): clean Hiwatt + pedals.
// ============================================================================

namespace dg
{

struct FactoryPreset
{
    const char* name;
    ChainParams params;
};

inline const std::vector<FactoryPreset>& factoryPresets()
{
    static const std::vector<FactoryPreset> presets = {
        { "Comfortably Numb (Solo)", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.5f;  p.compLevel = 0.65f;
            p.fuzzOn = true;  p.fuzzMode = 1; p.fuzzDrive = 0.8f; p.fuzzTone = 0.5f; p.fuzzVol = 0.85f;
            p.chorusOn = true; p.chRate = 0.2f; p.chDepth = 0.4f; p.chMix = 0.25f;
            p.delayOn = true; p.delayTime = 430.0f; p.delayFb = 0.45f; p.delayMix = 0.35f;
            p.reverbOn = true; p.revMix = 0.3f; p.revSize = 0.75f; p.revDecay = 0.8f;
            p.master = 0.8f;
            return p; }() },

        { "Echoes (Seagull Screams)", [] {
            ChainParams p;
            p.wahOn = true;   p.wahPedal = 0.85f;
            p.compOn = true;  p.compSustain = 0.4f; p.compLevel = 0.6f;
            p.phaserOn = true; p.phRate = 0.12f; p.phDepth = 0.85f; p.phMix = 0.65f;
            p.delayOn = true; p.delayTime = 480.0f; p.delayFb = 0.5f; p.delayMix = 0.4f;
            p.reverbOn = true; p.revMix = 0.45f; p.revSize = 0.9f; p.revDecay = 0.85f;
            p.master = 0.8f;
            return p; }() },

        { "Time (Breathe / Clocks)", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.45f; p.compLevel = 0.6f;
            p.chorusOn = true; p.chRate = 0.25f; p.chDepth = 0.4f; p.chMix = 0.2f;
            p.delayOn = true; p.delayTime = 375.0f; p.delayFb = 0.4f; p.delayMix = 0.35f; // dotted-eighth
            p.reverbOn = true; p.revMix = 0.3f;
            p.master = 0.8f;
            return p; }() },

        { "Money", [] {
            ChainParams p;
            p.fuzzOn = true;  p.fuzzMode = 0; p.fuzzDrive = 0.6f; p.fuzzTone = 0.55f; p.fuzzVol = 0.8f; // Fuzz Face
            p.delayOn = true; p.delayTime = 300.0f; p.delayFb = 0.3f; p.delayMix = 0.3f;
            p.reverbOn = true; p.revMix = 0.25f;
            p.master = 0.8f;
            return p; }() },

        { "Shine On You Crazy Diamond", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.45f; p.compLevel = 0.6f;
            p.vibeOn = true;  p.vibeRate = 0.18f; p.vibeDepth = 0.85f; p.vibeMix = 0.7f;
            p.chorusOn = true; p.chRate = 0.3f; p.chDepth = 0.5f; p.chMix = 0.3f;
            p.delayOn = true; p.delayTime = 450.0f; p.delayFb = 0.4f; p.delayMix = 0.3f;
            p.reverbOn = true; p.revMix = 0.4f; p.revSize = 0.85f;
            p.master = 0.8f;
            return p; }() },

        { "Wish You Were Here", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.4f; p.compLevel = 0.6f;
            p.chorusOn = true; p.chRate = 0.3f; p.chDepth = 0.55f; p.chMix = 0.5f;
            p.delayOn = true; p.delayTime = 340.0f; p.delayFb = 0.3f; p.delayMix = 0.25f;
            p.reverbOn = true; p.revMix = 0.35f;
            p.master = 0.8f;
            return p; }() },

        { "Another Brick In The Wall (Pt.2)", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.4f; p.compLevel = 0.6f;
            p.delayOn = true; p.delayTime = 375.0f; p.delayFb = 0.35f; p.delayMix = 0.3f;
            p.reverbOn = true; p.revMix = 0.3f;
            p.master = 0.8f;
            return p; }() },

        { "Welcome To The Machine", [] {
            ChainParams p;
            p.phaserOn = true; p.phRate = 0.35f; p.phDepth = 0.9f; p.phMix = 0.75f;
            p.flangerOn = true; p.flRate = 0.15f; p.flDepth = 0.5f; p.flRegen = 0.5f; p.flMix = 0.3f;
            p.delayOn = true; p.delayTime = 500.0f; p.delayFb = 0.5f; p.delayMix = 0.4f;
            p.reverbOn = true; p.revMix = 0.4f;
            p.master = 0.8f;
            return p; }() },

        { "Brain Damage", [] {
            ChainParams p;
            p.phaserOn = true; p.phRate = 0.22f; p.phDepth = 0.8f; p.phMix = 0.7f;
            p.delayOn = true; p.delayTime = 420.0f; p.delayFb = 0.45f; p.delayMix = 0.4f;
            p.reverbOn = true; p.revMix = 0.5f; p.revSize = 0.9f; p.revDecay = 0.85f;
            p.master = 0.8f;
            return p; }() },

        { "Mother", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.4f; p.compLevel = 0.6f;
            p.delayOn = true; p.delayTime = 300.0f; p.delayFb = 0.25f; p.delayMix = 0.25f;
            p.reverbOn = true; p.revMix = 0.3f;
            p.master = 0.8f;
            return p; }() },

        { "The Great Gig In The Sky", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.3f; p.compLevel = 0.5f;
            p.reverbOn = true; p.revMix = 0.5f; p.revSize = 1.0f; p.revDecay = 0.9f;
            p.master = 0.8f;
            return p; }() },

        { "On The Run", [] {
            ChainParams p;
            p.tremOn = true;  p.trRate = 0.8f; p.trDepth = 0.7f;
            p.phaserOn = true; p.phRate = 0.5f; p.phDepth = 0.6f; p.phMix = 0.5f;
            p.delayOn = true; p.delayTime = 200.0f; p.delayFb = 0.3f; p.delayMix = 0.3f;
            p.reverbOn = true; p.revMix = 0.3f;
            p.master = 0.8f;
            return p; }() },

        { "Dogs", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.45f; p.compLevel = 0.6f;
            p.delayOn = true; p.delayTime = 450.0f; p.delayFb = 0.4f; p.delayMix = 0.3f;
            p.reverbOn = true; p.revMix = 0.35f;
            p.master = 0.8f;
            return p; }() },

        { "Hey You", [] {
            ChainParams p;
            p.compOn = true;  p.compSustain = 0.4f; p.compLevel = 0.6f;
            p.chorusOn = true; p.chRate = 0.25f; p.chDepth = 0.45f; p.chMix = 0.25f;
            p.delayOn = true; p.delayTime = 375.0f; p.delayFb = 0.35f; p.delayMix = 0.3f;
            p.reverbOn = true; p.revMix = 0.4f;
            p.master = 0.8f;
            return p; }() },
    };
    return presets;
}

} // namespace dg

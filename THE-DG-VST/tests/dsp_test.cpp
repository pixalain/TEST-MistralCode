// Native unit test for the THE DG VST DSP core (no JUCE needed).
// Run: ./dg_dsp_test   — exits non-zero on failure.
#include "../Source/DspChain.h"
#include <cstdio>
#include <cmath>
#include <random>

using namespace dg;

static int failures = 0;

static void check (bool ok, const char* name)
{
    std::printf ("[%s] %s\n", ok ? "PASS" : "FAIL", name);
    if (!ok) ++failures;
}

static bool allFinite (const std::vector<float>& v)
{
    for (float x : v) if (!std::isfinite (x)) return false;
    return true;
}

static float rms (const std::vector<float>& v)
{
    double s = 0.0;
    for (float x : v) s += double (x) * double (x);
    return float (std::sqrt (s / double (v.size())));
}

// simple guitar-ish pluck: decaying sum of harmonics
static std::vector<float> makePluck (int n, double sr, double freq = 110.0)
{
    std::vector<float> out (static_cast<size_t> (n));
    std::mt19937 rng (1234);
    std::uniform_real_distribution<float> noise (-0.3f, 0.3f);
    double phase = 0.0;
    for (int i = 0; i < n; ++i)
    {
        phase += 2.0 * kPi * freq / sr;
        double env = std::exp (-3.0 * double (i) / sr);
        double s = std::sin (phase) + 0.5 * std::sin (2.0 * phase) + 0.25 * std::sin (3.0 * phase);
        out[static_cast<size_t> (i)] = static_cast<float> (env * s * 0.5 + noise (rng) * env);
    }
    return out;
}

int main()
{
    const double sr = 44100.0;
    DspChain chain;
    chain.setSampleRate (sr);

    auto signal = makePluck (int (sr * 2), sr);

    // 1. Bypass chain: output must equal input scaled by master only
    {
        ChainParams p;
        p.master = 1.0f;
        p.reverbOn = false;
        chain.update (p);
        std::vector<float> l = signal, r = signal;
        chain.processBlock (l.data(), static_cast<int> (l.size()));
        chain.processBlock (r.data(), static_cast<int> (r.size()));
        bool ok = allFinite (l) && allFinite (r);
        // dry path = softClip(x * master) * 0.95 with master = 1
        float maxErr = 0.0f;
        for (size_t i = 0; i < l.size(); ++i)
            maxErr = std::max (maxErr, std::fabs (l[i] - dg::softClip (signal[i] * 1.0f) * 0.95f));
        check (ok && maxErr < 1e-5f, "bypass path is bit-transparent (softClip(master) only)");
    }

    // 2. Each effect individually: finite output and actually changes the signal
    struct Case { const char* name; void (*fn)(ChainParams&); };
    auto runCase = [&] (const char* name, void (*fn)(ChainParams&))
    {
        ChainParams p;
        p.reverbOn = false;
        fn (p);
        p.master = 0.8f;
        chain.update (p);
        chain.reset();
        std::vector<float> l = signal, r = signal;
        chain.processBlock (l.data(), static_cast<int> (l.size()));
        chain.processBlock (r.data(), static_cast<int> (r.size()));
        bool finite = allFinite (l);
        // compare waveforms directly: effect must alter the shape, not just level
        float maxDiff = 0.0f;
        for (size_t i = 0; i < l.size(); ++i)
            maxDiff = std::max (maxDiff, std::fabs (l[i] - signal[i] * 0.8f * 0.95f));
        bool changed = maxDiff > 1e-3f;
        check (finite && changed, name);
    };

    runCase ("wah active produces finite, altered signal",  [] (ChainParams& p) { p.wahOn = true; p.wahPedal = 0.8f; });
    runCase ("compressor active produces finite, altered signal", [] (ChainParams& p) { p.compOn = true; });
    runCase ("fuzz face active produces finite, altered signal",  [] (ChainParams& p) { p.fuzzOn = true; p.fuzzMode = 0; });
    runCase ("big muff active produces finite, altered signal",   [] (ChainParams& p) { p.fuzzOn = true; p.fuzzMode = 1; });
    runCase ("octave active produces finite, altered signal",     [] (ChainParams& p) { p.octaveOn = true; p.octUp = 0.8f; p.octSub = 0.5f; });
    runCase ("uni-vibe active produces finite, altered signal",   [] (ChainParams& p) { p.vibeOn = true; });
    runCase ("phaser active produces finite, altered signal",     [] (ChainParams& p) { p.phaserOn = true; });
    runCase ("flanger active produces finite, altered signal",    [] (ChainParams& p) { p.flangerOn = true; });
    runCase ("chorus active produces finite, altered signal",     [] (ChainParams& p) { p.chorusOn = true; });
    runCase ("tremolo active produces finite, altered signal",    [] (ChainParams& p) { p.tremOn = true; });
    runCase ("delay active produces finite, altered signal",      [] (ChainParams& p) { p.delayOn = true; });
    runCase ("reverb active produces finite, altered signal",     [] (ChainParams& p) { p.reverbOn = true; });

    // 3. Full Gilmour chain (all on) — the Comfortably Numb-ish rig
    {
        ChainParams p;
        p.wahOn = true;  p.wahPedal = 0.65f;
        p.compOn = true; p.compSustain = 0.5f; p.compLevel = 0.6f;
        p.fuzzOn = true; p.fuzzMode = 1; p.fuzzDrive = 0.75f; p.fuzzTone = 0.5f; p.fuzzVol = 0.8f;
        p.octaveOn = false;
        p.vibeOn = false;
        p.phaserOn = false;
        p.flangerOn = false;
        p.chorusOn = true; p.chMix = 0.3f;
        p.tremOn = false;
        p.delayOn = true; p.delayTime = 430.0f; p.delayFb = 0.4f; p.delayMix = 0.35f;
        p.reverbOn = true; p.revMix = 0.25f;
        p.master = 0.8f;
        chain.update (p);
        chain.reset();
        std::vector<float> l = signal, r = signal;
        chain.processBlock (l.data(), static_cast<int> (l.size()));
        chain.processBlock (r.data(), static_cast<int> (r.size()));
        check (allFinite (l) && allFinite (r), "full Gilmour chain stays finite");
        check (rms (l) > 1e-4f, "full chain produces audible output");
    }

    // 4. Silence in -> silence out (no denormal blowup / DC)
    {
        ChainParams p;
        p.wahOn = p.compOn = p.fuzzOn = p.octaveOn = p.vibeOn = p.phaserOn =
        p.flangerOn = p.chorusOn = p.tremOn = p.delayOn = p.reverbOn = true;
        chain.update (p);
        chain.reset();
        std::vector<float> l (4096, 0.0f), r (4096, 0.0f);
        chain.processBlock (l.data(), static_cast<int> (l.size()));
        chain.processBlock (r.data(), static_cast<int> (r.size()));
        float peak = 0.0f;
        for (float x : l) peak = std::max (peak, std::fabs (x));
        check (allFinite (l) && peak < 1e-3f, "silence in -> silence out (no noise floor blowup)");
    }

    // 5. Impulse through delay produces echoes at expected time
    {
        ChainParams p;
        p.reverbOn = false;
        p.delayOn = true; p.delayTime = 10.0f; p.delayFb = 0.0f; p.delayMix = 1.0f; p.master = 1.0f; // 10 ms = 441 samples @44.1k
        chain.update (p);
        chain.reset();
        std::vector<float> l (4096, 0.0f), r (4096, 0.0f);
        l[0] = 1.0f; r[0] = 1.0f;
        chain.processBlock (l.data(), static_cast<int> (l.size()));
        chain.processBlock (r.data(), static_cast<int> (r.size()));
        // 10 ms @ 44.1 kHz = 441 samples of delay; the impulse written at t=0
        // comes back out at t = 442 (one-sample pipeline offset), tap2 at ~332
        check (std::fabs (l[442]) > 0.5f, "delay echo appears at programmed time (441 samples = 10 ms @44.1k)");
        check (std::fabs (l[332]) > 0.1f, "dotted-eighth tap (0.75x) present at ~332 samples");
        // and the dry component must be absent when mix == 1
        check (std::fabs (l[0]) < 1e-3f, "delay mix=1 removes dry component at t=0");
    }

    if (failures == 0)
        std::printf ("\nALL TESTS PASSED\n");
    else
        std::printf ("\n%d TEST(S) FAILED\n", failures);
    return failures == 0 ? 0 : 1;
}

#pragma once

#include <array>
#include <cmath>
#include <cstdint>
#include <vector>
#include <algorithm>
#include <cstring>

// ============================================================================
// THE DG VST — DSP core (pure C++, no JUCE dependency, unit-testable natively)
//
// Signal chain modelled on David Gilmour's pedalboards / racks
// (kitrae.net, gilmourish.com, equipboard):
//   IN -> Wah (Cry Baby) -> Compressor (MXR Dyna Comp / Boss CS-2)
//      -> Fuzz (Fuzz Face / Big Muff selector) -> Octave (sub + oct-up)
//      -> Uni-Vibe -> Phaser (Phase 90) -> Flanger (Electric Mistress)
//      -> Chorus (Boss CE-2) -> Tremolo (Demeter Tremulator)
//      -> Delay (Binson Echorec / TC 2290) -> Reverb (SPX90-style) -> OUT
// ============================================================================

namespace dg
{

static constexpr double kPi = 3.14159265358979323846;

inline float softClip (float x)
{
    // tanh-style saturator, stable for all |x|
    if (x >  6.0f) return 1.0f;
    if (x < -6.0f) return -1.0f;
    return std::tanh (x);
}

// One-pole parameter smoother. First call snaps to the target (no ramp on
// init), afterwards it glides -> kills zipper noise / clicks on param changes.
struct Smooth1
{
    float v = 0.0f;
    bool primed = false;
    float next (float target, float coeff)
    {
        if (!primed) { v = target; primed = true; }
        v += (target - v) * coeff;
        return v;
    }
    void snap (float target) { v = target; primed = true; }
    void reset() { primed = false; }
};

// One-pole lowpass. Callers pass c = exp(-2*pi*fc/fs); the actual smoothing
// coefficient is (1 - c) so that the -3 dB cutoff really is fc.
struct OnePoleLP
{
    float y = 0.0f;
    float a = 0.0f;
    void setCoeff (float c) { a = 1.0f - std::clamp (c, 0.0f, 0.99999f); }
    float process (float x) { y += a * (x - y); return y; }
    void reset() { y = 0.0f; }
};

// Standard one-pole highpass / DC blocker: y[n] = a*(y[n-1] + x[n] - x[n-1]),
// with a = exp(-2*pi*fc/fs). Passes audio above fc at ~unity, kills DC.
struct OnePoleHP
{
    float y = 0.0f, x1 = 0.0f;
    float a = 0.0f;
    void setCoeff (float c) { a = std::clamp (c, 0.0f, 0.99999f); }
    float process (float x)
    {
        float out = a * (y + x - x1);
        x1 = x;
        y = out;
        return out;
    }
    void reset() { y = 0.0f; x1 = 0.0f; }
};

// State-variable filter (Chamberlin), stable for f < ~0.4*fs
struct SVF
{
    float low = 0.0f, band = 0.0f;
    float f = 0.0f;   // 2*sin(pi*fc/fs), must stay < ~1.0 for stability
    float q = 0.0f;
    void set (float fc, float qq, float fs)
    {
        float x = float (kPi * fc / fs);
        float fRaw = 2.0f * std::sin (std::clamp (x, 0.0f, 0.45f));
        q = std::clamp (qq, 0.05f, 20.0f);
        // stability of the Chamberlin SVF requires f*q < ~2: clamp hard.
        f = std::min (fRaw, 1.7f / std::max (1.0f, q));
    }
    // returns lowpass output; band available for phaser
    float processLP (float x)
    {
        low += f * band;
        float high = x - low - q * band;
        band += f * high;
        return low;
    }
    float processBP (float x)
    {
        low += f * band;
        float high = x - low - q * band;
        band += f * high;
        return band;
    }
    void reset() { low = band = 0.0f; }
};

// ---------------------------------------------------------------------------
// Wah — Cry Baby style: swept resonant bandpass + mild lowpass
struct Wah
{
    static constexpr int kStages = 2; // two cascaded SVF bands for the vocal "quack"
    std::array<SVF, kStages> svf;
    OnePoleLP tone;
    float pedal = 0.5f; // 0..1
    float smooth = 0.0f;
    float fs = 44100.0f;

    void setSampleRate (float sampleRate) { fs = sampleRate; reset(); }
    void reset()
    {
        for (auto& s : svf) s.reset();
        tone.reset();
        smooth = pedal;
    }
    float process (float x)
    {
        // per-sample smoothing of the pedal position (expression feel)
        smooth += 0.0005f * (pedal - smooth);
        float p = std::clamp (smooth, 0.0f, 1.0f);
        // Cry Baby sweeps roughly 300 Hz .. 2.0 kHz (heel..toe)
        float fc = 300.0f + p * p * 1700.0f;
        float out = x;
        for (auto& s : svf)
        {
            s.set (fc, 4.5f, fs);
            out = s.processBP (out) * 1.6f + s.low * 0.4f; // band + a bit of low
        }
        tone.setCoeff (std::exp (-2.0f * float (kPi) * (4000.0f + p * 4000.0f) / fs));
        return tone.process (out) * 2.2f;
    }
};

// ---------------------------------------------------------------------------
// Compressor — MXR Dyna Comp / Boss CS-2 style: envelope follower + gain
// reduction, plus make-up gain.
struct Compressor
{
    float env = 0.0f;
    float attack = 0.005f, release = 0.12f;
    float ratio = 4.0f;
    float threshold = 0.25f; // linear
    float makeup = 1.6f;
    float fs = 44100.0f;
    Smooth1 threshS, makeupS;

    void setSampleRate (float sampleRate)
    {
        fs = sampleRate;
        attack  = std::exp (-1.0f / (0.003f * fs));
        release = std::exp (-1.0f / (0.180f * fs));
        reset();
    }
    void reset() { env = 0.0f; threshS.reset(); makeupS.reset(); }

    float process (float x)
    {
        float ax = std::fabs (x);
        float c = (ax > env) ? attack : release;
        env += c * (ax - env);
        float th = threshS.next (threshold, 0.002f);
        float over = std::max (0.0f, env - th) / std::max (th, 1e-6f);
        float grDb = -over * (1.0f - 1.0f / ratio) * 18.0f; // soft knee-ish
        float gain = std::pow (10.0f, grDb * 0.05f) * makeupS.next (makeup, 0.002f);
        return x * std::clamp (gain, 0.05f, 4.0f);
    }
};

// ---------------------------------------------------------------------------
// Fuzz — selectable between Dallas-Arbiter Fuzz Face (2 transistors, germanium
// vibe: soft asymmetric clip, lots of harmonics, cleans up with input level)
// and EHX Big Muff Pi (4 cascaded stages: hard symmetric clip + mid scoop tone).
struct Fuzz
{
    int mode = 1; // 0 = Fuzz Face, 1 = Big Muff
    float drive = 0.7f;   // sustain
    float tone  = 0.5f;   // 0 = dark, 1 = bright
    float volume = 0.8f;
    float fs = 44100.0f;
    OnePoleLP lpIn, lpTone;
    OnePoleHP dc; // removes the asymmetric-clip DC offset before it hits delay/reverb
    float hpState = 0.0f, hpA = 0.0f;

    void setSampleRate (float sampleRate)
    {
        fs = sampleRate;
        hpA = std::exp (-2.0f * float (kPi) * 90.0f / fs);
        dc.setCoeff (std::exp (-2.0f * float (kPi) * 25.0f / fs));
        reset();
    }
    void reset() { lpIn.reset(); lpTone.reset(); hpState = 0.0f; dc.reset(); }

    float process (float x)
    {
        float g = 4.0f + drive * 26.0f;
        float y = x * g;

        if (mode == 0) // Fuzz Face: asymmetric-ish soft clip, input cap rolls off top
        {
            lpIn.setCoeff (std::exp (-2.0f * float (kPi) * 7000.0f / fs));
            y = lpIn.process (y);
            // germanium-ish: gentle knee, asymmetric via bias
            y = softClip (y * 0.9f + 0.08f * y / (1.0f + std::fabs (y)));
            y *= 0.9f;
        }
        else // Big Muff: 4 cascaded hard clip stages w/ inter-stage tone shaping
        {
            for (int i = 0; i < 4; ++i)
            {
                float c = 1.2f + drive * 3.0f;
                y = std::clamp (y * c, -1.2f, 1.2f);
                y = softClip (y * 0.85f);
                // inter-stage highpass keeps it tight
                hpState += hpA * (y - hpState);
                y = y - hpState * 0.6f;
            }
        }

        // tone stack: lowpass whose cutoff depends on tone knob (mid scoop for muff)
        float cut = 1200.0f + tone * tone * 9000.0f;
        lpTone.setCoeff (std::exp (-2.0f * float (kPi) * cut / fs));
        y = lpTone.process (y);

        float vol = volume * (mode == 1 ? 2.2f : 1.6f);
        return dc.process (softClip (y * vol * 0.8f) * 1.2f);
    }
};

// ---------------------------------------------------------------------------
// Octave — Gilmour used octave-up (e.g. for seagull sounds / shimmer).
// Simple rectifier-based octave-up + sub-octave via flip-flop divider.
struct Octave
{
    float octUp = 0.0f;   // 0..1 amount
    float sub   = 0.0f;   // 0..1 amount
    float ff = 0.0f;      // flip-flop state for sub
    int   signPrev = 1;
    OnePoleLP lp;

    void setSampleRate (float sampleRate) { lp.setCoeff (std::exp (-2.0f * float (kPi) * 5000.0f / sampleRate)); reset(); }
    void reset() { ff = 0.0f; signPrev = 1; lp.reset(); }

    float process (float x)
    {
        float up = std::fabs (x);           // full-wave rect = octave up component
        up = lp.process (up);
        // sub octave: toggle flip-flop on zero crossings of input
        int s = (x >= 0.0f) ? 1 : -1;
        if (s != signPrev) { signPrev = s; ff = -ff; }
        float subSignal = (ff >= 0.0f ? 1.0f : -1.0f) * std::fabs (x) * 0.7f;
        float dry = 1.0f - 0.6f * octUp - 0.6f * sub;
        return x * dry + up * octUp * 0.9f + subSignal * sub;
    }
};

// ---------------------------------------------------------------------------
// Generic LFO-driven modulator base (uni-vibe / phaser / flanger / chorus)
struct Lfo
{
    float phase = 0.0f;
    float rate = 1.0f; // Hz
    float fs = 44100.0f;
    void setSampleRate (float sampleRate) { fs = sampleRate; }
    void reset() { phase = 0.0f; }
    // returns 0..1
    float next()
    {
        phase += rate / fs;
        if (phase >= 1.0f) phase -= 1.0f;
        return phase;
    }
    float sine()  { return 0.5f + 0.5f * std::sin (2.0f * float (kPi) * phase); }
    float tri()   { return phase < 0.5f ? (phase * 4.0f - 1.0f) : (3.0f - phase * 4.0f); }
};

// ---------------------------------------------------------------------------
// Uni-Vibe — 4-stage phaser with lamp-like throb (triangle LFO), chorus/vibrato
struct UniVibe
{
    static constexpr int kStages = 4;
    std::array<SVF, kStages> stages;
    Lfo lfo;
    float rate = 0.3f;     // 0..1 -> 0.5..6 Hz
    float depth = 0.7f;    // 0..1
    float mix = 0.5f;      // wet
    float fs = 44100.0f;

    void setSampleRate (float sampleRate) { fs = sampleRate; lfo.setSampleRate (sampleRate); reset(); }
    void reset() { for (auto& s : stages) s.reset(); lfo.reset(); }

    float process (float x)
    {
        lfo.rate = 0.5f + rate * 5.5f;
        lfo.next();
        float lfoV = 0.5f + 0.5f * lfo.tri(); // 0..1
        float fc = 500.0f + depth * lfoV * 2200.0f; // 500..2700 Hz: stable, musical sweep
        float y = x;
        for (auto& s : stages)
        {
            s.set (fc, 6.0f, fs);
            float bp = s.processBP (y);
            y = y - bp * 1.6f; // phaser-style notches
        }
        return x * (1.0f - mix) + y * mix;
    }
};

// ---------------------------------------------------------------------------
// Phaser — MXR Phase 90: 4 allpass stages, swept by LFO
struct Phaser
{
    static constexpr int kStages = 4;
    std::array<SVF, kStages> stages;
    Lfo lfo;
    float rate = 0.25f, depth = 0.7f, mix = 0.5f;
    float feedback = 0.35f;
    float fbState = 0.0f;
    float fs = 44100.0f;

    void setSampleRate (float sampleRate) { fs = sampleRate; lfo.setSampleRate (sampleRate); reset(); }
    void reset() { for (auto& s : stages) s.reset(); lfo.reset(); fbState = 0.0f; }

    float process (float x)
    {
        lfo.rate = 0.1f + rate * 4.0f;
        lfo.next();
        float lfoV = lfo.sine();
        float fc = 250.0f + depth * lfoV * 1500.0f; // 250..1750 Hz
        float y = x + fbState * feedback;
        for (auto& s : stages)
        {
            s.set (fc, 1.0f, fs);
            s.processLP (y);          // advance filter state
            float bp = s.band;
            y = y - 2.0f * bp;        // allpass approximation
        }
        fbState = y;
        return x * (1.0f - mix) + y * mix;
    }
};

// ---------------------------------------------------------------------------
// Flanger — EHX Electric Mistress: short swept delay line + feedback + regen
struct Flanger
{
    static constexpr int kMaxDelay = 1024; // ~23 ms @44.1k
    std::array<float, kMaxDelay> buf{};
    int write = 0;
    Lfo lfo;
    float rate = 0.2f, depth = 0.6f, regen = 0.4f, mix = 0.5f;
    float fs = 44100.0f;

    void setSampleRate (float sampleRate) { fs = sampleRate; lfo.setSampleRate (sampleRate); reset(); }
    void reset() { buf.fill (0.0f); write = 0; lfo.reset(); }

    float process (float x)
    {
        lfo.rate = 0.05f + rate * 2.5f;
        lfo.next();
        float lfoV = lfo.sine();
        float delay = 20.0f + depth * lfoV * 480.0f; // 20..500 samples (~0.45..11 ms)
        float rp = float (write) - delay;
        while (rp < 0.0f) rp += float (kMaxDelay);
        int i0 = int (rp) % kMaxDelay;
        int i1 = (i0 + 1) % kMaxDelay;
        float frac = rp - std::floor (rp);
        float wet = buf[i0] * (1.0f - frac) + buf[i1] * frac;
        float y = x + wet * regen;
        buf[write] = y;
        write = (write + 1) % kMaxDelay;
        return x * (1.0f - mix) + wet * mix;
    }
};

// ---------------------------------------------------------------------------
// Chorus — Boss CE-2 style: two slightly detuned swept delays, gentle
struct Chorus
{
    static constexpr int kMaxDelay = 2048;
    std::array<float, kMaxDelay> bufL{}, bufR{};
    int write = 0;
    Lfo lfoL, lfoR;
    float rate = 0.25f, depth = 0.5f, mix = 0.5f;
    float fs = 44100.0f;

    void setSampleRate (float sampleRate) { fs = sampleRate; lfoL.setSampleRate (sampleRate); lfoR.setSampleRate (sampleRate); reset(); }
    void reset() { bufL.fill (0.0f); bufR.fill (0.0f); write = 0; lfoL.reset(); lfoR.reset(); }

    float readDelay (std::array<float, kMaxDelay>& buf, float delay)
    {
        float rp = float (write) - delay;
        while (rp < 0.0f) rp += float (kMaxDelay);
        int i0 = int (rp) % kMaxDelay;
        int i1 = (i0 + 1) % kMaxDelay;
        float frac = rp - std::floor (rp);
        return buf[i0] * (1.0f - frac) + buf[i1] * frac;
    }

    // returns mono (L==R here for simplicity) — stereo handled by caller duplicating
    float process (float x)
    {
        lfoL.rate = 0.3f + rate * 2.0f;
        lfoR.rate = lfoL.rate * 1.13f; // slight detune = CE-2 lushness
        lfoL.next(); lfoR.next();
        float base = 700.0f; // ~16 ms
        float dL = base + depth * lfoL.sine() * 220.0f;
        float dR = base + depth * lfoR.sine() * 220.0f;
        float wetL = readDelay (bufL, dL);
        float wetR = readDelay (bufR, dR);
        float y = (wetL + wetR) * 0.5f;
        float in = x;
        bufL[write] = in;
        bufR[write] = in;
        write = (write + 1) % kMaxDelay;
        return x * (1.0f - mix) + y * mix;
    }
};

// ---------------------------------------------------------------------------
// Tremolo — Demeter Tremulator style: smooth sine amplitude modulation
struct Tremolo
{
    Lfo lfo;
    float rate = 0.3f, depth = 0.6f;
    float fs = 44100.0f;
    void setSampleRate (float sampleRate) { fs = sampleRate; lfo.setSampleRate (sampleRate); reset(); }
    void reset() { lfo.reset(); }
    float process (float x)
    {
        lfo.rate = 1.0f + rate * 9.0f;
        lfo.next();
        float m = 1.0f - depth * (0.5f + 0.5f * lfo.sine());
        return x * m;
    }
};

// ---------------------------------------------------------------------------
// Delay — Binson Echorec (multi-tap tape) / TC 2290 style: feedback delay with
// lowpass in the loop (tape darkening) + dotted-eighth second tap (Gilmour).
struct Delay
{
    static constexpr int kMaxDelay = 1 << 16; // ~1.5 s @44.1k
    std::vector<float> buf;
    int write = 0;              // points at the NEXT slot to write
    float timeMs = 450.0f;
    float feedback = 0.35f;
    float mix = 0.3f;
    float fs = 44100.0f;
    OnePoleLP loopLp;
    float tap2Gain = 0.4f;      // dotted-eighth second tap
    Smooth1 timeS, fbS, mixS;

    void setSampleRate (float sampleRate)
    {
        fs = sampleRate;
        buf.assign (size_t (kMaxDelay), 0.0f);
        loopLp.setCoeff (std::exp (-2.0f * float (kPi) * 3500.0f / fs));
        reset();
    }
    void reset()
    {
        std::fill (buf.begin(), buf.end(), 0.0f);
        write = 0; loopLp.reset();
        timeS.snap (timeMs); fbS.snap (feedback); mixS.snap (mix);
    }

    // interpolated read of the sample written `samplesBack` samples ago.
    // write points at the slot that will be written NEXT, so the most recent
    // sample lives at (write - 1).
    float readBack (float samplesBack) const
    {
        float rp = float (write) - 1.0f - samplesBack;
        while (rp < 0.0f) rp += float (buf.size());
        while (rp >= float (buf.size())) rp -= float (buf.size());
        size_t i0 = size_t (rp) % buf.size();
        size_t i1 = (i0 + 1) % buf.size();
        float frac = rp - std::floor (rp);
        return buf[i0] * (1.0f - frac) + buf[i1] * frac;
    }

    float process (float x)
    {
        // smoothed delay time: the read offset glides instead of jumping
        float t  = timeS.next (timeMs, 0.0008f);
        float fb = fbS.next (feedback, 0.002f);
        float m  = mixS.next (mix, 0.002f);
        float d1 = std::clamp (t * 0.001f * fs, 1.0f, float (buf.size() - 2));
        float d2 = d1 * 0.75f; // dotted eighth
        float tap1 = readBack (d1);
        float tap2 = readBack (d2);
        buf[size_t (write)] = x + loopLp.process (tap1) * fb;
        write = (write + 1) % int (buf.size());
        float wet = tap1 + tap2 * tap2Gain;
        return x * (1.0f - m) + wet * m;
    }
};

// ---------------------------------------------------------------------------
// Reverb - SPX90-style: Schroeder/Moorer. 4 parallel comb filters + 2 allpass.
// Combs use fixed max-size buffers (NO allocation on the audio thread); the
// delay length and feedback are smoothed per-sample so size/decay changes are
// click-free, and a DC blocker sits in each comb loop.
struct Reverb
{
    struct Comb
    {
        static constexpr int kMax = 4096;
        std::array<float, kMax> buf{};
        int write = 0;
        float lenTarget = 1116.0f;
        float fbTarget = 0.8f;
        float damp = 0.25f, dampState = 0.0f;
        Smooth1 lenS, fbS;
        OnePoleHP dc;

        void init (float baseLen, float fb, float d, float fsr)
        {
            lenTarget = std::clamp (baseLen, 8.0f, float (kMax - 2));
            fbTarget = fb;
            damp = d;
            dc.setCoeff (std::exp (-2.0f * float (kPi) * 35.0f / fsr));
            clear();
        }
        float process (float x)
        {
            float len = std::clamp (lenS.next (lenTarget, 0.0008f), 8.0f, float (kMax - 2));
            float fb  = std::clamp (fbS.next (fbTarget, 0.002f), 0.0f, 0.97f);
            float rp = float (write) - len;
            while (rp < 0.0f) rp += float (kMax);
            int i0 = int (rp) % kMax;
            int i1 = (i0 + 1) % kMax;
            float fr = rp - std::floor (rp);
            float out = buf[i0] * (1.0f - fr) + buf[i1] * fr;
            dampState = out * (1.0f - damp) + dampState * damp;
            buf[size_t (write)] = dc.process (x + dampState * fb);
            write = (write + 1) % kMax;
            return out;
        }
        void clear()
        {
            buf.fill (0.0f);
            write = 0;
            dampState = 0.0f;
            dc.reset();
            lenS.snap (lenTarget);
            fbS.snap (fbTarget);
        }
    };
    struct Allpass
    {
        static constexpr int kMax = 1024;
        std::array<float, kMax> buf{};
        int idx = 0, len = 556;
        float g = 0.5f;
        void init (int size, float gg)
        {
            len = std::clamp (size, 8, kMax - 2);
            g = gg;
            clear();
        }
        float process (float x)
        {
            int r = idx - len;
            if (r < 0) r += kMax;
            float bufOut = buf[size_t (r)];
            float y = bufOut - g * x;
            buf[size_t (idx)] = x + bufOut * g;
            idx = (idx + 1) % kMax;
            return y;
        }
        void clear() { buf.fill (0.0f); idx = 0; }
    };

    std::array<Comb, 4> combs;
    std::array<Allpass, 2> allpasses;
    std::array<float, 4> baseComb { 1116.0f, 1188.0f, 1277.0f, 1356.0f };
    std::array<int, 2> baseAllp { 556, 441 };
    Smooth1 mixS;
    float mix = 0.3f;
    float size = 0.7f;    // scales comb delay lengths (live)
    float decay = 0.75f;  // scales comb feedback (live)
    float fs = 44100.0f;

    void setSampleRate (float sampleRate)
    {
        fs = sampleRate;
        for (int i = 0; i < 4; ++i)
            combs[size_t (i)].init (baseComb[size_t (i)] * 2.0f, 0.78f + decay * 0.19f, 0.25f, fs);
        for (int i = 0; i < 2; ++i)
            allpasses[size_t (i)].init (int (baseAllp[size_t (i)] * 1.5f), 0.5f);
        applySizeDecay();
        reset();
    }
    // called from DspChain::update so size/decay respond live (smoothed inside)
    void applySizeDecay()
    {
        for (int i = 0; i < 4; ++i)
        {
            combs[size_t (i)].lenTarget = std::clamp (baseComb[size_t (i)] * (1.0f + size), 8.0f, float (Comb::kMax - 2));
            combs[size_t (i)].fbTarget  = std::clamp (0.78f + decay * 0.19f, 0.0f, 0.97f);
        }
    }
    void reset()
    {
        for (auto& c : combs) c.clear();
        for (auto& a : allpasses) a.clear();
        mixS.snap (mix);
    }

    float process (float x)
    {
        float y = 0.0f;
        for (auto& c : combs) y += c.process (x);
        y *= 0.25f;
        for (auto& a : allpasses) y = a.process (y);
        float m = mixS.next (mix, 0.002f);
        return x * (1.0f - m) + y * m * 1.6f;
    }
};

// ===========================================================================
// Full chain
struct ChainParams
{
    // bypass flags
    bool wahOn = false, compOn = false, fuzzOn = false, octaveOn = false,
         vibeOn = false, phaserOn = false, flangerOn = false, chorusOn = false,
         tremOn = false, delayOn = false, reverbOn = true;

    // wah
    float wahPedal = 0.7f;
    // comp
    float compSustain = 0.4f, compLevel = 0.6f;
    // fuzz
    int   fuzzMode = 1; // 0 face, 1 muff
    float fuzzDrive = 0.7f, fuzzTone = 0.5f, fuzzVol = 0.8f;
    // octave
    float octUp = 0.6f, octSub = 0.0f;
    // vibe
    float vibeRate = 0.3f, vibeDepth = 0.7f, vibeMix = 0.6f;
    // phaser
    float phRate = 0.25f, phDepth = 0.7f, phMix = 0.5f;
    // flanger
    float flRate = 0.2f, flDepth = 0.6f, flRegen = 0.4f, flMix = 0.5f;
    // chorus
    float chRate = 0.25f, chDepth = 0.5f, chMix = 0.5f;
    // tremolo
    float trRate = 0.3f, trDepth = 0.6f;
    // delay
    float delayTime = 450.0f, delayFb = 0.35f, delayMix = 0.3f;
    // reverb
    float revMix = 0.3f, revSize = 0.7f, revDecay = 0.75f;
    // master
    float master = 0.8f;
};

class DspChain
{
public:
    void setSampleRate (double sr)
    {
        sampleRate = float (sr);
        dcOut.setCoeff (std::exp (-2.0f * float (kPi) * 20.0f / sampleRate));
        wah.setSampleRate (sampleRate);
        comp.setSampleRate (sampleRate);
        fuzz.setSampleRate (sampleRate);
        oct.setSampleRate (sampleRate);
        vibe.setSampleRate (sampleRate);
        phaser.setSampleRate (sampleRate);
        flanger.setSampleRate (sampleRate);
        chorus.setSampleRate (sampleRate);
        trem.setSampleRate (sampleRate);
        delay.setSampleRate (sampleRate);
        reverb.setSampleRate (sampleRate);
    }

    void reset()
    {
        wah.reset(); comp.reset(); fuzz.reset(); oct.reset();
        vibe.reset(); phaser.reset(); flanger.reset(); chorus.reset();
        trem.reset(); delay.reset(); reverb.reset();
        // snap bypass crossfaders + master to the CURRENT targets so a reset
        // (transport stop / test) is immediate, while live param changes still
        // glide smoothly via update().
        wahB.snap (params.wahOn ? 1.0f : 0.0f);
        compB.snap (params.compOn ? 1.0f : 0.0f);
        fuzzB.snap (params.fuzzOn ? 1.0f : 0.0f);
        octB.snap (params.octaveOn ? 1.0f : 0.0f);
        vibeB.snap (params.vibeOn ? 1.0f : 0.0f);
        phB.snap (params.phaserOn ? 1.0f : 0.0f);
        flB.snap (params.flangerOn ? 1.0f : 0.0f);
        chB.snap (params.chorusOn ? 1.0f : 0.0f);
        trB.snap (params.tremOn ? 1.0f : 0.0f);
        dlyB.snap (params.delayOn ? 1.0f : 0.0f);
        revB.snap (params.reverbOn ? 1.0f : 0.0f);
        masterS.snap (params.master);
    }

    void update (const ChainParams& p)
    {
        params = p;
        wah.pedal = p.wahPedal;
        comp.ratio = 2.0f + p.compSustain * 8.0f;
        comp.threshold = 0.4f - p.compSustain * 0.25f;
        comp.makeup = 1.0f + p.compLevel * 1.4f;
        fuzz.mode = p.fuzzMode;
        fuzz.drive = p.fuzzDrive;
        fuzz.tone = p.fuzzTone;
        fuzz.volume = p.fuzzVol;
        oct.octUp = p.octUp;
        oct.sub = p.octSub;
        vibe.rate = p.vibeRate; vibe.depth = p.vibeDepth; vibe.mix = p.vibeMix;
        phaser.rate = p.phRate; phaser.depth = p.phDepth; phaser.mix = p.phMix;
        flanger.rate = p.flRate; flanger.depth = p.flDepth; flanger.regen = p.flRegen; flanger.mix = p.flMix;
        chorus.rate = p.chRate; chorus.depth = p.chDepth; chorus.mix = p.chMix;
        trem.rate = p.trRate; trem.depth = p.trDepth;
        delay.timeMs = p.delayTime; delay.feedback = p.delayFb; delay.mix = p.delayMix;
        reverb.mix = p.revMix; reverb.size = p.revSize; reverb.decay = p.revDecay;
        reverb.applySizeDecay();
    }

    // process one sample. NOTE: this advances the internal state (LFOs, delay
    // lines...), so for stereo use TWO DspChain instances (one per channel) —
    // sharing one instance between L and R would run the modulators at 2x speed.
    // Every effect ALWAYS runs; the bypass is a ~11 ms crossfade toward the
    // on/off target, so footswitching never clicks. Gain/time params are
    // smoothed per-sample (Smooth1) for the same reason.
    static float bypassMix (Smooth1& s, bool on, float dry, float wet)
    {
        float b = s.next (on ? 1.0f : 0.0f, 0.002f);
        return dry + (wet - dry) * b;
    }

    float processSample (float x)
    {
        const auto& p = params;
        float y = x;
        y = bypassMix (wahB,   p.wahOn,     y, wah.process (y));
        y = bypassMix (compB,  p.compOn,    y, comp.process (y));
        y = bypassMix (fuzzB,  p.fuzzOn,    y, fuzz.process (y));
        y = bypassMix (octB,   p.octaveOn,  y, oct.process (y));
        y = bypassMix (vibeB,  p.vibeOn,    y, vibe.process (y));
        y = bypassMix (phB,    p.phaserOn,  y, phaser.process (y));
        y = bypassMix (flB,    p.flangerOn, y, flanger.process (y));
        y = bypassMix (chB,    p.chorusOn,  y, chorus.process (y));
        y = bypassMix (trB,    p.tremOn,    y, trem.process (y));
        y = bypassMix (dlyB,   p.delayOn,   y, delay.process (y));
        y = bypassMix (revB,   p.reverbOn,  y, reverb.process (y));
        float m = masterS.next (p.master, 0.002f);
        return dcOut.process (softClip (y * m) * 0.95f);
    }

    // mono buffer processing
    void processBlock (float* data, int numSamples)
    {
        for (int i = 0; i < numSamples; ++i)
            data[i] = processSample (data[i]);
    }

    ChainParams params;
    float sampleRate = 44100.0f;

private:
    // per-effect bypass crossfaders + master smoother + output DC blocker
    Smooth1 wahB, compB, fuzzB, octB, vibeB, phB, flB, chB, trB, dlyB, revB, masterS;
    OnePoleHP dcOut;
    Wah wah;
    Compressor comp;
    Fuzz fuzz;
    Octave oct;
    UniVibe vibe;
    Phaser phaser;
    Flanger flanger;
    Chorus chorus;
    Tremolo trem;
    Delay delay;
    Reverb reverb;
};

} // namespace dg

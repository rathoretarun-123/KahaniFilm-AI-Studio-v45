package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Synth;
import com.tarun.kahani.core.VoiceFx;
import org.junit.Test;

/**
 * v47.13 (the user: "web search regarding voice modulation and incorporate them"): the pitch moved without the formants
 * (TD-PSOLA), the formants moved on their own for a child's or an old man's voice, and a line's loudness by its feeling.
 */
public class VoiceModulationTest {
    static final int SR = Synth.SR;

    /** A vowel: pulses at f0 through one resonance at formant Hz. */
    static float[] vowel(float f0, float formant, float seconds) {
        int n = (int) (SR * seconds);
        float[] x = new float[n];
        double r = Math.exp(-Math.PI * 120.0 / SR), th = 2 * Math.PI * formant / SR, a1 = 2 * r * Math.cos(th), a2 = -r * r, y1 = 0, y2 = 0;
        int period = Math.round(SR / f0);
        for (int i = 0; i < n; i++) {
            double e = i % period == 0 ? 1 : 0, y = e + a1 * y1 + a2 * y2;
            y2 = y1; y1 = y; x[i] = (float) (0.2 * y);
        }
        return x;
    }

    static float f0(float[] x) {
        // (the shortest period that repeats nearly as well as the best: twice the period repeats as well, an octave lower)
        int win = SR / 25, a = x.length / 2 - win / 2, lo = SR / 500, hi = SR / 60;
        double[] r = new double[hi + 1];
        double bc = -1;
        for (int L = lo; L <= hi; L++) {
            double c = 0, e1 = 0, e2 = 0;
            for (int i = 0; i < win; i++) { c += x[a + i] * x[a + i + L]; e1 += x[a + i] * x[a + i]; e2 += x[a + i + L] * x[a + i + L]; }
            r[L] = c / Math.sqrt(e1 * e2 + 1e-12);
            bc = Math.max(bc, r[L]);
        }
        for (int L = lo + 1; L < hi; L++) if (r[L] >= 0.9 * bc && r[L] >= r[L - 1] && r[L] >= r[L + 1]) return SR / (float) L;
        return 0;
    }

    /** The strongest frequency between 300 and 3000 Hz of the middle of the sound (where the resonance is). */
    static float peak(float[] x) {
        int n = 4096, a = x.length / 2 - n / 2; float bf = 0; double bp = -1;
        for (float f = 300; f <= 3000; f += 10) {
            double re = 0, im = 0;
            for (int i = 0; i < n; i++) { double w = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)); re += w * x[a + i] * Math.cos(2 * Math.PI * f * i / SR); im += w * x[a + i] * Math.sin(2 * Math.PI * f * i / SR); }
            double p = re * re + im * im;
            if (p > bp) { bp = p; bf = f; }
        }
        return bf;
    }

    @Test public void thePitchMovesAndTheVoiceStaysTheSameSize() {
        float[] x = vowel(120, 1000, 1.2f);
        float[] up = VoiceFx.pitch(x, 1.25f);
        assertEquals("the length is kept", x.length, up.length);
        assertEquals("the pitch rises by a quarter", 150, f0(up), 150 * 0.06);
        // (the strongest harmonic near the resonance: within half a harmonic spacing of it)
        assertEquals("the formant stays where it was (no chipmunk)", 1000, peak(up), 100);
        float[] chip = VoiceFx.pitchAndTone(x, 1.25f);
        assertTrue("the old way moved the formant with the pitch: " + peak(chip), peak(chip) > 1150);
    }

    @Test public void aChildsVoiceHasASmallerThroat() {
        float[] x = vowel(120, 1000, 1.2f);
        float[] child = VoiceFx.voiceAge(x, SR, 1.3f, 1.12f);
        assertEquals(x.length, child.length);
        assertEquals("higher", 156, f0(child), 156 * 0.06);
        assertEquals("the formant up by its own factor", 1120, peak(child), 100);
    }

    @Test public void aLinesLoudnessFollowsItsFeeling() {
        assertTrue(VoiceFx.emotionLoudness(Pose.ANGRY) > VoiceFx.emotionLoudness(Pose.NEUTRAL));
        assertTrue(VoiceFx.emotionLoudness(Pose.SAD) < VoiceFx.emotionLoudness(Pose.NEUTRAL));
        assertTrue(VoiceFx.emotionLoudness(Pose.WHISPER) < VoiceFx.emotionLoudness(Pose.SAD));
        float[] sad = VoiceFx.emotionFactors(Pose.SAD), scared = VoiceFx.emotionFactors(Pose.SCARED);
        assertTrue("sad is slower and lower", sad[0] < 1 && sad[1] < 1);
        assertTrue("fear is faster and higher", scared[0] > 1 && scared[1] > 1);
    }
}

package com.tarun.kahani.core;

/**
 * Voice processing used to make generated dialogue sound like a character's voice sample:
 * pitch estimation, pitch shifting that keeps the duration (resample + WSOLA time-stretch),
 * and a tone (timbre) match that copies the average spectrum shape of the sample.
 */
public final class VoiceFx {
    private VoiceFx() {}

    /** Median fundamental frequency (Hz) of voiced parts, by autocorrelation. 0 if unvoiced. */
    public static float medianPitch(float[] x, int sr) {
        int win = sr / 25, hop = sr / 100;
        int minLag = sr / 450, maxLag = sr / 70;
        float[] found = new float[Math.max(1, x.length / hop)];
        int n = 0;
        for (int start = 0; start + win + maxLag < x.length; start += hop) {
            double energy = 0;
            for (int i = 0; i < win; i++) energy += x[start + i] * x[start + i];
            if (energy / win < 1e-4) continue;
            double best = 0;
            int bestLag = 0;
            for (int lag = minLag; lag <= maxLag; lag++) {
                double s = 0, e2 = 0;
                for (int i = 0; i < win; i += 2) { s += x[start + i] * x[start + i + lag]; e2 += x[start + i + lag] * x[start + i + lag]; }
                double c = s / Math.sqrt(energy / 2 * e2 + 1e-12);
                if (c > best) { best = c; bestLag = lag; }
            }
            if (best > 0.55 && bestLag > 0) found[n++] = sr / (float) bestLag;
        }
        if (n == 0) return 0;
        java.util.Arrays.sort(found, 0, n);
        return found[n / 2];
    }

    /** Changes length without changing pitch (WSOLA). factor > 1 = longer/slower. */
    public static float[] stretch(float[] x, float factor) {
        if (Math.abs(factor - 1) < 0.01f || x.length < 2048) return x;
        int frame = 1024, overlap = 512, hopOut = frame - overlap;
        int hopIn = Math.max(1, Math.round(hopOut / factor));
        int tol = 256;
        int outLen = (int) (x.length * factor) + frame;
        float[] out = new float[outLen];
        float[] win = new float[frame];
        for (int i = 0; i < frame; i++) win[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (frame - 1)));
        int inPos = 0, outPos = 0, prev = 0;
        while (inPos + frame + tol < x.length && outPos + frame < outLen) {
            int bestOff = 0;
            if (outPos > 0) {
                double best = -1e18;
                for (int off = -tol; off <= tol; off += 4) {
                    int p = inPos + off;
                    if (p < 0) continue;
                    double c = 0;
                    for (int i = 0; i < overlap; i += 4) c += x[p + i] * x[prev + hopOut + i < x.length ? prev + hopOut + i : x.length - 1];
                    if (c > best) { best = c; bestOff = off; }
                }
            }
            int p = Math.max(0, inPos + bestOff);
            for (int i = 0; i < frame && p + i < x.length; i++) out[outPos + i] += x[p + i] * win[i];
            prev = p;
            inPos += hopIn;
            outPos += hopOut;
        }
        int n = Math.min(outLen, (int) (x.length * factor));
        float[] o = new float[n];
        System.arraycopy(out, 0, o, 0, n);
        return o;
    }

    /**
     * v47.13 (the user: "voice modulation properly"; the web's answer: plain resampling moves the formants with the
     * pitch — the "chipmunk" — while pitch-synchronous overlap-add, TD-PSOLA, keeps them for modest changes): the pitch
     * moved by pitch (1.2 = higher), the formants — the size of the vocal tract, what makes a voice a child's or a grown
     * man's — by formant (1.1 = a smaller throat), and the length by time (> 1 = slower), each on its own. Voiced parts
     * are cut into two-period grains at the voice's own pulses and laid down again closer together or further apart;
     * unvoiced parts (s, sh, breaths) keep their spacing.
     */
    public static float[] psola(float[] x, int sr, float pitch, float formant, float time) {
        if (x == null || x.length < sr / 20) return x;
        if (Math.abs(pitch - 1) < 0.005f && Math.abs(formant - 1) < 0.005f && Math.abs(time - 1) < 0.005f) return x;
        float[] in = x;
        int want = Math.max(1, Math.round(x.length * time));      // the length asked for, whatever the resampling rounds to
        if (Math.abs(formant - 1) >= 0.005f) {
            // the formants by resampling (which moves the pitch and the length with them), corrected below
            int n = Math.max(2, (int) (x.length / formant));
            in = new float[n];
            for (int i = 0; i < n; i++) {
                float q = i * formant; int a = (int) q; float f = q - a;
                float x0 = a < x.length ? x[a] : 0, x1 = a + 1 < x.length ? x[a + 1] : 0;
                in[i] = x0 + (x1 - x0) * f;
            }
            pitch /= formant;
            time *= formant;
        }
        int[] marks = pitchMarks(in, sr);
        int nm = marks.length;
        if (nm < 2) return in;
        boolean[] voiced = new boolean[nm];
        int[] period = new int[nm];
        int uv = sr / 100;
        for (int k = 0; k < nm; k++) {
            int prev = k > 0 ? marks[k] - marks[k - 1] : marks[1] - marks[0], next = k + 1 < nm ? marks[k + 1] - marks[k] : prev;
            period[k] = Math.max(16, (prev + next) / 2);
            voiced[k] = Math.abs(prev - next) < 0.35f * Math.max(prev, next) && period[k] < sr / 55 && period[k] != uv;
        }
        int outLen = want;
        float[] out = new float[outLen + sr / 25];
        float[] wsum = new float[out.length];
        float ts = marks[0] * time;
        int k = 0;
        while (ts < outLen) {
            float ta = ts / time;
            while (k + 1 < nm && Math.abs(marks[k + 1] - ta) <= Math.abs(marks[k] - ta)) k++;
            int P = period[k], m = marks[k];
            int c = Math.round(ts);
            for (int i = -P; i < P; i++) {
                int si = m + i, oi = c + i;
                if (si < 0 || si >= in.length || oi < 0 || oi >= out.length) continue;
                float w = 0.5f - 0.5f * (float) Math.cos(Math.PI * (i + P) / P);
                out[oi] += w * in[si];
                wsum[oi] += w;
            }
            ts += voiced[k] ? P / pitch : P;
        }
        float[] o = new float[outLen];
        for (int i = 0; i < outLen; i++) o[i] = wsum[i] > 0.15f ? out[i] / wsum[i] : out[i];
        return o;
    }

    /**
     * The voice's pulses (sample positions): in voiced parts one at each period's strongest peak, the period found by
     * autocorrelation (60–500 Hz); elsewhere every 10 ms.
     */
    static int[] pitchMarks(float[] x, int sr) {
        int hop = sr / 100, win = sr * 3 / 100, minLag = sr / 500, maxLag = sr / 60;
        int frames = Math.max(1, (x.length - win) / hop + 1);
        int[] per = new int[frames];
        double eMax = 1e-9;
        double[] en = new double[frames];
        for (int f = 0; f < frames; f++) {
            int a = f * hop; double e = 0;
            for (int i = 0; i < win && a + i < x.length; i++) e += x[a + i] * x[a + i];
            en[f] = e; eMax = Math.max(eMax, e);
        }
        for (int f = 0; f < frames; f++) {
            int a = f * hop;
            if (en[f] < eMax * 0.002) continue;
            double best = 0; int lag = 0;
            double[] rr = new double[maxLag + 2];
            for (int L = minLag; L <= maxLag; L++) {
                double c = 0, e1 = 0, e2 = 0;
                for (int i = 0; i + L < win && a + i + L < x.length; i += 2) { float u = x[a + i], v = x[a + i + L]; c += u * v; e1 += u * u; e2 += v * v; }
                double r = c / Math.sqrt(e1 * e2 + 1e-12);
                rr[L] = r;
                if (r > best) { best = r; lag = L; }
            }
            // (twice the period repeats as well as the period: the shortest lag that repeats nearly as well is the pitch)
            for (int L = minLag + 1; L < lag; L++) if (rr[L] >= 0.9 * best && rr[L] >= rr[L - 1] && rr[L] >= rr[L + 1]) { lag = L; break; }
            if (best > 0.55) per[f] = lag;
        }
        // (a lone voiced or unvoiced frame among the others is a mistake of the tracker)
        int[] sm = per.clone();
        for (int f = 1; f + 1 < frames; f++) {
            int a = per[f - 1], b = per[f], c = per[f + 1];
            int[] t = {a, b, c}; java.util.Arrays.sort(t); sm[f] = t[1];
        }
        java.util.ArrayList<Integer> m = new java.util.ArrayList<Integer>();
        int t = 0;
        while (t < x.length) {
            int f = Math.min(frames - 1, t / hop), P = sm[f];
            if (P <= 0) { m.add(t); t += hop; continue; }
            // the next pulse: the strongest peak a third of a period either side of one period after the last (at the
            // start of a voiced run, within its first period)
            int last = m.isEmpty() ? -1 : m.get(m.size() - 1);
            boolean run = last >= 0 && t - last <= P + P / 3 && t - last >= P - P / 3;
            int from = run ? last + P - P / 3 : t, to = Math.min(x.length - 1, run ? last + P + P / 3 : t + P);
            if (from > x.length - 1) break;
            int pk = from; float best = -1e9f;
            for (int i = from; i <= to; i++) if (x[i] > best) { best = x[i]; pk = i; }
            m.add(pk);
            t = pk + P;
        }
        int[] r = new int[m.size()];
        for (int i = 0; i < r.length; i++) r[i] = m.get(i);
        return r;
    }

    /**
     * v47.13: a voice made older or younger without the chipmunk: the pitch and the formants moved together but apart —
     * a child's voice higher with a smaller throat, an old man's lower and a little slower — the length kept.
     */
    public static float[] voiceAge(float[] x, int sr, float pitch, float formant) { return psola(x, sr, pitch, formant, 1f); }

    /**
     * v47.13 (the web's rule for emotional prosody: arousal raises rate, pitch and loudness, low valence and low arousal
     * lower them): how loud a line of this feeling is against a calm one — after every line has been brought to the same
     * speaking loudness. A shout carries, a sad line is quieter, a whisper quieter still.
     */
    public static float emotionLoudness(int emotion) {
        switch (emotion) {
            case Pose.ANGRY: return 1.25f;
            case Pose.SURPRISED: return 1.12f;
            case Pose.SCARED: case Pose.LAUGH: case Pose.PAIN: return 1.1f;
            case Pose.HAPPY: return 1.06f;
            case Pose.PROUD: return 1.04f;
            case Pose.EVIL: return 0.95f;
            case Pose.SAD: return 0.85f;
            case Pose.WHISPER: return 0.8f;
            default: return 1f;
        }
    }

    /** How a feeling changes a line's pace and pitch: {rate, pitch} factors (shared by the phone and the desktop). */
    public static float[] emotionFactors(int emotion) {
        switch (emotion) {
            case Pose.ANGRY: return new float[]{1.06f, 0.94f};
            case Pose.SAD: return new float[]{0.88f, 0.96f};
            case Pose.SCARED: return new float[]{1.1f, 1.06f};
            case Pose.LAUGH: return new float[]{1.04f, 1.07f};
            case Pose.HAPPY: return new float[]{1.02f, 1.04f};
            case Pose.SURPRISED: return new float[]{1.03f, 1.08f};
            case Pose.WHISPER: return new float[]{0.9f, 1f};
            case Pose.PAIN: return new float[]{1.1f, 1.08f};
            case Pose.EVIL: return new float[]{0.92f, 0.95f};
            case Pose.PROUD: return new float[]{0.96f, 0.98f};
            default: return new float[]{1f, 1f};
        }
    }

    /** The old way: the pitch and the formants moved together (a squeak, a chipmunk — what a mouse should sound like). */
    public static float[] pitchAndTone(float[] x, float factor) { return pitchResampled(x, factor); }

    /** Shifts pitch by a factor (1.2 = higher) keeping the same duration and the formants (TD-PSOLA). */
    public static float[] pitch(float[] x, float factor) {
        if (Math.abs(factor - 1) < 0.01f || x == null) return x;
        return psola(x, Synth.SR, factor, 1f, 1f);
    }

    static float[] pitchResampled(float[] x, float factor) {
        if (Math.abs(factor - 1) < 0.01f) return x;
        float[] s = stretch(x, factor);           // longer by factor
        int n = x.length;
        float[] o = new float[n];
        for (int i = 0; i < n; i++) {             // then play faster -> original length, pitch * factor
            float p = i * factor;
            int a = (int) p;
            float f = p - a;
            float x0 = a < s.length ? s[a] : 0, x1 = a + 1 < s.length ? s[a + 1] : 0;
            o[i] = x0 + (x1 - x0) * f;
        }
        return o;
    }

    // ---------------------------------------------------------------- timbre matching

    static final int BANDS = 24;

    /** Average energy in log-spaced bands (a "tone fingerprint" of a voice). */
    public static float[] toneProfile(float[] x, int sr) {
        float[] bands = new float[BANDS];
        int n = 1024;
        float[] re = new float[n], im = new float[n];
        int frames = 0;
        for (int start = 0; start + n < x.length; start += n / 2) {
            double e = 0;
            for (int i = 0; i < n; i++) e += x[start + i] * x[start + i];
            if (e / n < 1e-4) continue;
            for (int i = 0; i < n; i++) { re[i] = x[start + i] * (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / n)); im[i] = 0; }
            fft(re, im);
            for (int k = 1; k < n / 2; k++) {
                float f = k * sr / (float) n;
                int b = band(f);
                if (b >= 0) bands[b] += re[k] * re[k] + im[k] * im[k];
            }
            frames++;
        }
        if (frames == 0) return null;
        for (int b = 0; b < BANDS; b++) bands[b] = (float) Math.log10(bands[b] / frames + 1e-9);
        return bands;
    }

    static int band(float f) {
        if (f < 80 || f > 8000) return -1;
        double t = Math.log(f / 80.0) / Math.log(8000 / 80.0);
        return Math.min(BANDS - 1, (int) (t * BANDS));
    }

    /** Equalises x so its average tone moves toward the target profile (strength 0..1, limited to ±9 dB). */
    public static float[] matchTone(float[] x, int sr, float[] target, float strength) {
        float[] src = toneProfile(x, sr);
        if (src == null || target == null) return x;
        float[] gainDb = new float[BANDS];
        float mean = 0;
        for (int b = 0; b < BANDS; b++) { gainDb[b] = (target[b] - src[b]) * 10f; mean += gainDb[b]; }
        mean /= BANDS;
        for (int b = 0; b < BANDS; b++) gainDb[b] = Math.max(-9, Math.min(9, (gainDb[b] - mean) * strength));
        // smooth across bands
        float[] sm = new float[BANDS];
        for (int b = 0; b < BANDS; b++) {
            float s = gainDb[b] * 2, w = 2;
            if (b > 0) { s += gainDb[b - 1]; w++; }
            if (b < BANDS - 1) { s += gainDb[b + 1]; w++; }
            sm[b] = s / w;
        }
        int n = 1024, hop = n / 2;
        float[] out = new float[x.length + n];
        float[] re = new float[n], im = new float[n];
        float[] win = new float[n];
        for (int i = 0; i < n; i++) win[i] = (float) Math.sqrt(0.5 - 0.5 * Math.cos(2 * Math.PI * i / n));
        float[] g = new float[n / 2 + 1];
        for (int k = 0; k <= n / 2; k++) {
            int b = band(k * sr / (float) n);
            g[k] = b < 0 ? 1f : (float) Math.pow(10, sm[b] / 20);
        }
        for (int start = 0; start < x.length; start += hop) {
            for (int i = 0; i < n; i++) { re[i] = (start + i < x.length ? x[start + i] : 0) * win[i]; im[i] = 0; }
            fft(re, im);
            for (int k = 0; k <= n / 2; k++) {
                re[k] *= g[k]; im[k] *= g[k];
                if (k > 0 && k < n / 2) { re[n - k] = re[k]; im[n - k] = -im[k]; }
            }
            ifft(re, im);
            for (int i = 0; i < n && start + i < out.length; i++) out[start + i] += re[i] * win[i];
        }
        float[] o = new float[x.length];
        System.arraycopy(out, 0, o, 0, x.length);
        // keep loudness
        double e0 = 0, e1 = 0;
        for (int i = 0; i < x.length; i++) { e0 += x[i] * x[i]; e1 += o[i] * o[i]; }
        float k = e1 > 0 ? (float) Math.sqrt(e0 / e1) : 1;
        for (int i = 0; i < o.length; i++) o[i] *= k;
        return o;
    }

    /**
     * Measures a voice: {median pitch Hz, pitch liveliness (std in semitones), syllables per second,
     * brightness (spectral centre Hz), roughness 0..1 (how unclear / breathy / raspy the tone is)}.
     */
    public static float[] features(float[] x, int sr) {
        if (x.length > sr * 40) x = java.util.Arrays.copyOf(x, sr * 40);
        int win = sr / 25, hop = sr / 100, minLag = sr / 450, maxLag = sr / 60;
        java.util.List<Float> pitches = new java.util.ArrayList<Float>();
        double harmSum = 0;
        int voiced = 0, frames = 0;
        for (int start = 0; start + win + maxLag < x.length; start += hop) {
            double energy = 0;
            for (int i = 0; i < win; i++) energy += x[start + i] * x[start + i];
            frames++;
            if (energy / win < 1e-4) continue;
            double best = 0;
            int bestLag = 0;
            for (int lag = minLag; lag <= maxLag; lag++) {
                double s = 0, e2 = 0;
                for (int i = 0; i < win; i += 2) { s += x[start + i] * x[start + i + lag]; e2 += x[start + i + lag] * x[start + i + lag]; }
                double c = s / Math.sqrt(energy / 2 * e2 + 1e-12);
                if (c > best) { best = c; bestLag = lag; }
            }
            voiced++;
            harmSum += best;
            if (best > 0.55 && bestLag > 0) pitches.add(sr / (float) bestLag);
        }
        float[] out = new float[5];
        if (pitches.isEmpty()) return out;
        java.util.Collections.sort(pitches);
        float med = pitches.get(pitches.size() / 2);
        out[0] = med;
        double v = 0;
        for (float p : pitches) { double st = 12 * Math.log(p / med) / Math.log(2); v += st * st; }
        out[1] = (float) Math.sqrt(v / pitches.size());
        // syllables per second of speech: loudness peaks (in dB) that stand at least 3 dB above the dip
        // separating them from the previous syllable; pauses are not counted
        int ew = sr / 100;
        int n = x.length / ew;
        float[] pw = new float[n];
        for (int i = 0; i < n; i++) {
            double s = 0;
            for (int j = i * ew; j < (i + 1) * ew; j++) s += x[j] * x[j];
            pw[i] = (float) (s / ew);
        }
        float[] db = new float[n];
        float mx = -200;
        for (int i = 0; i < n; i++) {
            float a = 0;
            int c = 0;
            for (int k = Math.max(0, i - 1); k <= Math.min(n - 1, i + 1); k++) { a += pw[k]; c++; }
            db[i] = (float) (10 * Math.log10(a / c + 1e-10));
            mx = Math.max(mx, db[i]);
        }
        float floor = mx - 30;
        int speech = 0, syl = 0, last = -1;
        float lastV = 0;
        for (int i = 0; i < n; i++) if (db[i] > floor) speech++;
        for (int i = 1; i < n - 1; i++) {
            if (db[i] <= floor + 5 || db[i] < db[i - 1] || db[i] <= db[i + 1]) continue;
            if (last < 0) { syl++; last = i; lastV = db[i]; continue; }
            float dip = Float.MAX_VALUE;
            for (int k = last; k <= i; k++) dip = Math.min(dip, db[k]);
            if (Math.min(lastV, db[i]) - dip >= 3 && i - last >= 7) { syl++; last = i; lastV = db[i]; }
            else if (db[i] > lastV) { last = i; lastV = db[i]; }
        }
        out[2] = speech == 0 ? 0 : syl / (speech / 100f);
        // brightness: average spectral centre of loud frames
        int fn = 1024;
        float[] re = new float[fn], im = new float[fn];
        double cs = 0;
        int cf = 0;
        for (int start = 0; start + fn < x.length; start += fn) {
            double e = 0;
            for (int i = 0; i < fn; i++) e += x[start + i] * x[start + i];
            if (e / fn < 1e-4) continue;
            for (int i = 0; i < fn; i++) { re[i] = x[start + i] * (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / fn)); im[i] = 0; }
            fft(re, im);
            double num = 0, den = 0;
            for (int k = 1; k < fn / 2; k++) { double m = Math.sqrt(re[k] * re[k] + im[k] * im[k]); num += m * k * sr / (double) fn; den += m; }
            if (den > 0) { cs += num / den; cf++; }
        }
        out[3] = cf == 0 ? 0 : (float) (cs / cf);
        out[4] = voiced == 0 ? 0 : (float) Math.max(0, Math.min(1, 1 - harmSum / voiced));
        return out;
    }

    /** What we learn from a voice sample once: its median pitch and tone colour. */
    public static final class Profile {
        public float pitch;
        public float[] tone;
    }

    /** Analyses (at most the first 40 seconds of) a voice sample. */
    public static Profile profile(float[] sample, int sr) {
        float[] s = sample;
        if (s.length > sr * 40) s = java.util.Arrays.copyOf(s, sr * 40);
        Profile p = new Profile();
        p.pitch = medianPitch(s, sr);
        p.tone = toneProfile(s, sr);
        return p;
    }

    /** Makes generated speech closer to a voice sample: same median pitch and similar tone colour. */
    public static float[] matchVoice(float[] speech, float[] sample, int sr) { return matchVoice(speech, profile(sample, sr), sr); }

    public static float[] matchVoice(float[] speech, Profile target, int sr) {
        if (target == null) return speech;
        float p0 = medianPitch(speech, sr);
        float[] y = speech;
        if (p0 > 0 && target.pitch > 0) {
            // beyond about ±0.8 octave any pitch shifter sounds artificial, so stay within that
            float f = Math.max(0.6f, Math.min(1.8f, target.pitch / p0));
            y = pitch(y, f);
        }
        return matchTone(y, sr, target.tone, 0.6f);
    }

    // ---------------------------------------------------------------- FFT (radix 2, in place)

    static void fft(float[] re, float[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) { float t = re[i]; re[i] = re[j]; re[j] = t; t = im[i]; im[i] = im[j]; im[j] = t; }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            float wr = (float) Math.cos(ang), wi = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float cr = 1, ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k, b = i + k + len / 2;
                    float xr = re[b] * cr - im[b] * ci, xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr; im[a] += xi;
                    float nr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = nr;
                }
            }
        }
    }

    static void ifft(float[] re, float[] im) {
        for (int i = 0; i < im.length; i++) im[i] = -im[i];
        fft(re, im);
        int n = re.length;
        for (int i = 0; i < n; i++) { re[i] /= n; im[i] = -im[i] / n; }
    }
}

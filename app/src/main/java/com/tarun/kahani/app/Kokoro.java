package com.tarun.kahani.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * v47.13 (the user: "include full size kokoro in apk"): the phone speaks the film's lines offline with Kokoro-82M
 * (Apache-2.0, the full model, assets/kokoro/) through sherpa-onnx (Apache-2.0; its Hindi phonemiser is espeak-ng,
 * GPL-3.0 — see assets/kokoro/NOTICE.txt). The voice of each character is core.KokoroCast's, the same as the
 * desktop's. It is opened once per film, only on a phone with the memory for it (the model takes some 350 MB), and
 * never stops a film: any failure leaves the lines to the phone's own speech engine.
 */
final class Kokoro {
    static final String DIR = "kokoro";
    /** The model needs this much memory on the phone (total RAM). */
    static final long MIN_RAM = 3500L * 1024 * 1024;

    static final String PKG = "com.k2fsa.sherpa.onnx.";
    /** Why the last open gave no engine ("" when it opened): shown on the film's note, never a silent fallback. */
    static volatile String why = "not tried";
    private final Object tts;
    final boolean hindi;

    private Kokoro(Object tts, boolean hindi) { this.tts = tts; this.hindi = hindi; }

    /** The model is in this APK. */
    static boolean bundled(Context ctx) {
        try {
            String[] files = ctx.getAssets().list(DIR);
            if (files != null) for (String f : files) if (f.equals("model.onnx")) return true;
        } catch (Exception ignored) { }
        return false;
    }

    /** The engine for the story's language, or null (no model in the app, too little memory, no native library). */
    static Kokoro open(Context ctx, boolean hindi) {
        try {
            AssetManager am = ctx.getAssets();
            String[] files = am.list(DIR);
            boolean has = false;
            if (files != null) for (String f : files) if (f.equals("model.onnx")) has = true;
            if (!has) { why = "no model in this APK"; return null; }
            ActivityManager act = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (act != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                act.getMemoryInfo(mi);
                if (mi.totalMem > 0 && mi.totalMem < MIN_RAM) { why = "the phone has " + (mi.totalMem >> 20) + " MB of memory, the model needs 3500"; return null; }
            }
            // the phonemiser's data must be files on the phone: copied out of the app once
            File data = new File(ctx.getFilesDir(), DIR + "/espeak-ng-data");
            File done = new File(data, ".copied");
            if (!done.isFile()) {
                copyDir(am, DIR + "/espeak-ng-data", data);
                new FileOutputStream(done).close();
            }
            // (through reflection: the sherpa-onnx classes are in the APK only, not in the desktop and test builds)
            Object k = make("OfflineTtsKokoroModelConfig");
            set(k, "setModel", String.class, DIR + "/model.onnx");
            set(k, "setVoices", String.class, DIR + "/voices.bin");
            set(k, "setTokens", String.class, DIR + "/tokens.txt");
            set(k, "setDataDir", String.class, data.getAbsolutePath());
            set(k, "setLexicon", String.class, DIR + "/lexicon-us-en.txt");
            set(k, "setLang", String.class, hindi ? "hi" : "en-us");
            Object m = make("OfflineTtsModelConfig");
            set(m, "setKokoro", k.getClass(), k);
            set(m, "setNumThreads", int.class, 2);
            set(m, "setProvider", String.class, "cpu");
            Object c = make("OfflineTtsConfig");
            set(c, "setModel", m.getClass(), m);
            Object tts = Class.forName(PKG + "OfflineTts").getConstructor(AssetManager.class, c.getClass()).newInstance(am, c);
            why = "";
            android.util.Log.i("Kahani", "Kokoro opened (" + (hindi ? "hi" : "en-us") + ")");
            return new Kokoro(tts, hindi);
        } catch (Throwable e) {
            why = "could not open: " + e;
            android.util.Log.w("Kahani", "Kokoro: " + why);
            // (an emulator without the native library, a phone the model does not fit: the film goes on without it)
            return null;
        }
    }

    /** The line spoken by speaker at pace (1 = its own), as PCM at its own sample rate {samples, rate}, or null. */
    Object[] speak(String text, int speaker, float pace) {
        try {
            Object a = tts.getClass().getMethod("generate", String.class, int.class, float.class).invoke(tts, text, speaker, pace);
            if (a == null) return null;
            float[] x = (float[]) a.getClass().getMethod("getSamples").invoke(a);
            int rate = (Integer) a.getClass().getMethod("getSampleRate").invoke(a);
            if (x == null || x.length < rate / 10) return null;
            return new Object[]{x, rate};
        } catch (Throwable e) {
            return null;
        }
    }

    void release() {
        try { tts.getClass().getMethod("release").invoke(tts); } catch (Throwable ignored) { }
    }

    private static Object make(String cls) throws Exception { return Class.forName(PKG + cls).getConstructor().newInstance(); }

    private static void set(Object o, String setter, Class<?> type, Object value) throws Exception {
        o.getClass().getMethod(setter, type).invoke(o, value);
    }

    private static void copyDir(AssetManager am, String from, File to) throws java.io.IOException {
        String[] list = am.list(from);
        if (list == null || list.length == 0) {
            // a file
            to.getParentFile().mkdirs();
            InputStream in = am.open(from);
            FileOutputStream out = new FileOutputStream(to);
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            out.close();
            in.close();
            return;
        }
        to.mkdirs();
        for (String f : list) copyDir(am, from + "/" + f, new File(to, f));
    }
}

"""Speak a film's lines with Kokoro-82M (Apache-2.0) through sherpa-onnx, for the desktop renders (MakeFilm KOKORO=...).

usage: python kokoro_batch.py <kokoro model dir> <espeak-ng-data dir> <jobs.json>
jobs.json: [{"text": "...", "sid": 31, "speed": 1.0, "pitch": 1.0, "lang": "hi", "out": "/path/v0.wav"}, ...]
pitch: the voice raised (> 1, a child) or lowered (< 1, an old man) by that factor at the same pace.
Writes 16-bit mono WAVs; a line that cannot be spoken is skipped (MakeFilm falls back to espeak-ng for it).
"""
import json, sys, wave
import numpy as np
import sherpa_onnx

model_dir, espeak_data, jobs_path = sys.argv[1], sys.argv[2], sys.argv[3]
jobs = json.load(open(jobs_path, encoding="utf-8"))
engines = {}


def engine(lang):
    if lang not in engines:
        cfg = sherpa_onnx.OfflineTtsConfig(model=sherpa_onnx.OfflineTtsModelConfig(
            kokoro=sherpa_onnx.OfflineTtsKokoroModelConfig(
                model=model_dir + "/model.int8.onnx", voices=model_dir + "/voices.bin", tokens=model_dir + "/tokens.txt",
                data_dir=espeak_data, lexicon=model_dir + "/lexicon-us-en.txt", lang=lang),
            num_threads=2))
        engines[lang] = sherpa_onnx.OfflineTts(cfg)
    return engines[lang]


for j in jobs:
    try:
        pitch = float(j.get("pitch", 1.0))
        # spoken slower by the pitch factor, then played that much faster: the pace as asked, the voice higher or lower
        a = engine(j.get("lang", "hi")).generate(j["text"], sid=int(j["sid"]), speed=float(j.get("speed", 1.0)) / pitch)
        x = np.clip(np.asarray(a.samples, dtype=np.float32), -1, 1)
        if abs(pitch - 1) > 0.01 and x.size > 1:
            x = np.interp(np.arange(0, x.size - 1, pitch), np.arange(x.size), x).astype(np.float32)
        if x.size == 0 or float(np.abs(x).max()) < 1e-3:
            continue
        with wave.open(j["out"], "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(a.sample_rate)
            w.writeframes((x * 32767).astype(np.int16).tobytes())
        print("spoke", j["out"], round(x.size / a.sample_rate, 2), "s", flush=True)
    except Exception as e:  # one bad line never stops the others
        print("failed", j.get("out"), e, flush=True)

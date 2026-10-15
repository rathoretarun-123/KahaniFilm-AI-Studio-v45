package com.tarun.kahani.core;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v47: real-time dictionary lookup when the Reader or the Director hits a word or a sentence it does not know.
 *
 * Two free paths, in order of preference:
 *  1. Wikipedia's REST summary ({@code /api/rest_v1/page/summary/<title>}) — no key, open content, Hindi and English
 *     editions are both tried. Returns the first few sentences of the article.
 *  2. The user's own cloud (Gemini / Claude key) when configured — a one-sentence meaning.
 *
 * Results are cached in a per-session map so the same word is never fetched twice. Caller decides what to do with
 * the meaning (write it into a cue, offer it to the user, use it to guess the pose/emotion the word carries).
 */
public final class WordLookup {
    private WordLookup() {}

    public static final int MAX_CACHE = 500;
    private static final Map<String, String> CACHE = new LinkedHashMap<String, String>(32, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > MAX_CACHE; }
    };

    /** The meaning of a word, or null when nothing is known. Safe to call offline — returns the cached value or null. */
    public static synchronized String meaning(String word) {
        if (word == null) return null;
        String key = word.trim().toLowerCase(java.util.Locale.ROOT);
        return key.length() == 0 ? null : CACHE.get(key);
    }

    /** Look up a word now and cache it. Returns the meaning (or null when no source reaches). */
    public static String lookup(String word, Cloud cloud) {
        if (word == null) return null;
        String key = word.trim().toLowerCase(java.util.Locale.ROOT);
        if (key.length() == 0) return null;
        synchronized (CACHE) { String hit = CACHE.get(key); if (hit != null) return hit; }
        String meaning = null;
        try { meaning = wiki(word); } catch (Throwable ignored) { meaning = null; }
        if (meaning == null && cloud != null) {
            try { meaning = cloud.ask("You are a friendly dictionary. Return one short sentence that explains the given word's meaning in English, no quotes.", word, false); }
            catch (Throwable ignored) { meaning = null; }
            if (meaning != null) meaning = meaning.trim();
        }
        if (meaning != null && meaning.length() > 0) synchronized (CACHE) { CACHE.put(key, meaning); }
        return meaning;
    }

    /** Wikipedia's REST summary for one word. Tries the script's own language first (ASCII → en, Devanagari → hi). */
    private static String wiki(String word) throws IOException {
        boolean hindi = false;
        for (int i = 0; i < word.length(); i++) { int c = word.charAt(i); if (c >= 0x0900 && c <= 0x097F) { hindi = true; break; } }
        String[] langs = hindi ? new String[]{"hi", "en"} : new String[]{"en", "hi"};
        for (String lang : langs) {
            String url = "https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(word, "UTF-8");
            try {
                byte[] body = new Cloud().request("GET", url, null, null, new String[]{"Accept", "application/json"});
                Object o = Json.parseLoose(new String(body, "UTF-8"));
                if (o instanceof Map) {
                    Object extract = ((Map<?, ?>) o).get("extract");
                    if (extract instanceof String && ((String) extract).length() > 0) return (String) extract;
                }
            } catch (IOException ignored) { /* try the next language */ }
        }
        return null;
    }
}

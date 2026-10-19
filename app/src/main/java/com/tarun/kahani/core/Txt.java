package com.tarun.kahani.core;

import java.util.Locale;

/** Small text helpers that work for Hindi (Devanagari) and English scripts. */
public final class Txt {
    private Txt() {}

    public static String digitsToAscii(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '०' && c <= '९') b.append((char) ('0' + (c - '०')));
            else b.append(c);
        }
        return b.toString();
    }

    /** Normalises nukta / chandrabindu variations so "ज़" matches "ज", "हँसी" matches "हंसी" etc. */
    public static String norm(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '़') continue;                 // nukta
            if (c == 'ँ') c = 'ं';             // chandrabindu -> anusvara
            if (c == 'ं' && b.length() > 0 && b.charAt(b.length() - 1) == 'ो') continue;   // v38: "तीनो" = "तीनों", "दोनो" = "दोनों" (the dot is often left out)
            if (c == '‌' || c == '‍') continue; // zero width joiners
            b.append(c);
        }
        return b.toString().toLowerCase(Locale.ROOT);
    }

    public static boolean has(String hay, String... needles) {
        String h = norm(hay);
        for (String n : needles) if (h.contains(norm(n))) return true;
        return false;
    }

    /** True if any needle is in the text as a whole word (no letter right before or after it): "mall" is not in "small". */
    public static boolean hasWord(String hay, String... needles) {
        String h = norm(hay);
        for (String n : needles) {
            String x = norm(n);
            if (x.isEmpty()) continue;
            int i = h.indexOf(x);
            while (i >= 0) {
                boolean l = i == 0 || !Character.isLetterOrDigit(h.charAt(i - 1));
                int e = i + x.length();
                boolean r = e >= h.length() || !Character.isLetterOrDigit(h.charAt(e));
                if (l && r) return true;
                i = h.indexOf(x, i + 1);
            }
        }
        return false;
    }

    /** True when a complete word or phrase occurs, with Hindi combining marks treated as word characters. */
    public static boolean hasWholeWord(String hay, String... needles) {
        String h = norm(hay);
        for (String n : needles) {
            String x = norm(n).trim();
            if (x.isEmpty()) continue;
            int i = h.indexOf(x);
            while (i >= 0) {
                int e = i + x.length();
                boolean left = i == 0 || !wordChar(h.charAt(i - 1));
                boolean right = e >= h.length() || !wordChar(h.charAt(e));
                if (left && right) return true;
                i = h.indexOf(x, i + 1);
            }
        }
        return false;
    }

    /**
     * v43: true if any needle begins a word of the text (a stem may go on into its endings: "गिर" in "गिरता"), never
     * from inside another word — "गाता" is not in "लगाता", "राहत" not in "कराहता", "टन" not in "घुटने", "wind" not
     * in "window" is not caught (it begins the word); spaces round a needle are ignored.
     */
    public static boolean hasStart(String hay, String... needles) {
        String h = norm(hay);
        for (String n : needles) {
            String x = norm(n).trim();
            if (x.isEmpty()) continue;
            int i = h.indexOf(x);
            while (i >= 0) {
                if (i == 0 || !wordChar(h.charAt(i - 1))) return true;
                i = h.indexOf(x, i + 1);
            }
        }
        return false;
    }

    /** v43: a letter of a word (Devanagari letters and their vowel signs included; the danda is not). */
    static boolean wordChar(char c) {
        if (c >= '\u0900' && c <= '\u0963') return true;
        if (c == '\u0964' || c == '\u0965') return false;
        return Character.isLetterOrDigit(c);
    }

    public static int firstIndex(String hay, String... needles) {
        String h = norm(hay);
        int best = -1;
        for (String n : needles) {
            int i = h.indexOf(norm(n));
            if (i >= 0 && (best < 0 || i < best)) best = i;
        }
        return best;
    }

    public static boolean isDevanagari(char c) { return c >= 'ऀ' && c <= 'ॿ'; }

    /** v47.9: the text has Devanagari letters in it. */
    public static boolean isDevanagariText(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) if (isDevanagari(s.charAt(i))) return true;
        return false;
    }

    private static final String[][] ROMAN_CONS = {{"क", "k"}, {"ख", "kh"}, {"ग", "g"}, {"घ", "gh"}, {"ङ", "n"}, {"च", "ch"}, {"छ", "chh"}, {"ज", "j"},
            {"झ", "jh"}, {"ञ", "n"}, {"ट", "t"}, {"ठ", "th"}, {"ड", "d"}, {"ढ", "dh"}, {"ण", "n"}, {"त", "t"}, {"थ", "th"}, {"द", "d"}, {"ध", "dh"},
            {"न", "n"}, {"प", "p"}, {"फ", "ph"}, {"ब", "b"}, {"भ", "bh"}, {"म", "m"}, {"य", "y"}, {"र", "r"}, {"ल", "l"}, {"व", "v"}, {"श", "sh"},
            {"ष", "sh"}, {"स", "s"}, {"ह", "h"}, {"ळ", "l"}};
    private static final String[][] ROMAN_NUKTA = {{"क", "q"}, {"ख", "kh"}, {"ग", "gh"}, {"ज", "z"}, {"फ", "f"}, {"ड", "r"}, {"ढ", "rh"}};
    private static final String[][] ROMAN_VOWEL = {{"अ", "a"}, {"आ", "aa"}, {"इ", "i"}, {"ई", "ee"}, {"उ", "u"}, {"ऊ", "oo"}, {"ऋ", "ri"}, {"ए", "e"},
            {"ऐ", "ai"}, {"ओ", "o"}, {"औ", "au"}, {"ऑ", "o"}};
    private static final String[][] ROMAN_MATRA = {{"ा", "aa"}, {"ि", "i"}, {"ी", "ee"}, {"ु", "u"}, {"ू", "oo"}, {"ृ", "ri"}, {"े", "e"}, {"ै", "ai"},
            {"ो", "o"}, {"ौ", "au"}, {"ॉ", "o"}};

    private static String lookup(String[][] t, char c) {
        for (String[] r : t) if (r[0].charAt(0) == c) return r[1];
        return null;
    }

    /**
     * v47.9: Hindi in Latin letters, the way it is written in chat ("दीदी, ये चाँद" → "deedee, ye chaand"), for a
     * phone whose speech engine has no Hindi voice: an English voice can still say the line. A consonant carries its
     * "a" unless a vowel sign or a halant follows; the "a" at the end of a word is not said ("कमल" → "kamal").
     * Anything not Devanagari is kept as it is.
     */
    public static String romanize(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            String cons = lookup(ROMAN_CONS, c);
            if (cons != null) {
                int j = i + 1;
                if (j < n && s.charAt(j) == '़') { String nk = lookup(ROMAN_NUKTA, c); if (nk != null) cons = nk; j++; }
                out.append(cons);
                char next = j < n ? s.charAt(j) : ' ';
                String matra = lookup(ROMAN_MATRA, next);
                if (matra != null) { out.append(matra); j++; }
                else if (next == '्') { j++; }
                else {
                    // the inherent "a": not at the end of a word
                    boolean wordEnd = j >= n || !isDevanagari(next) || next == '।' || next == '॥';
                    boolean nasalEnd = (next == 'ं' || next == 'ँ') && (j + 1 >= n || !isDevanagari(s.charAt(j + 1)));
                    if (!wordEnd || nasalEnd) out.append('a');
                }
                i = j - 1;
                continue;
            }
            String v = lookup(ROMAN_VOWEL, c);
            if (v != null) { out.append(v); continue; }
            String m = lookup(ROMAN_MATRA, c);
            if (m != null) { out.append(m); continue; }
            if (c == 'ं' || c == 'ँ') { out.append('n'); continue; }
            if (c == 'ः') { out.append('h'); continue; }
            if (c == '्' || c == '़') continue;
            if (c == '।' || c == '॥') { out.append('.'); continue; }
            if (c >= '०' && c <= '९') { out.append((char) ('0' + (c - '०'))); continue; }
            out.append(c);
        }
        return out.toString();
    }

    public static boolean mostlyHindi(String s) {
        int dev = 0, lat = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isDevanagari(c)) dev++;
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) lat++;
        }
        return dev >= lat;
    }

    public static String stripQuotes(String s) {
        s = s.trim();
        while (s.length() > 0 && "\"“”'‘’«»".indexOf(s.charAt(0)) >= 0) s = s.substring(1).trim();
        while (s.length() > 0 && "\"“”'‘’«»".indexOf(s.charAt(s.length() - 1)) >= 0) s = s.substring(0, s.length() - 1).trim();
        return s;
    }

    /** Removes "(...)" groups. */
    public static String withoutParens(String s) {
        StringBuilder b = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { if (depth > 0) depth--; }
            else if (depth == 0) b.append(c);
        }
        return b.toString().replaceAll("\\s+", " ").trim();
    }

    public static String clean(String s) {
        return s.replace(' ', ' ').replaceAll("[ \\t]+", " ").trim();
    }

    /** Text suitable for a speech engine: drop stage noise, keep pauses. */
    public static String forSpeech(String s) {
        String t = s.replace("…", ", ").replace("...", ", ").replace("—", ", ").replace("–", ", ");
        t = t.replaceAll("[\"“”«»]", "");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }
}

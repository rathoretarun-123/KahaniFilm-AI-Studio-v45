package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v47.6: the studio's standard screenplay format, written from the reader's own reading of a script (the Story) — never
 * by replacing characters in the text — and checked line by line:
 * <ul>
 * <li>the title is plain text (no brackets, no quotation marks);</li>
 * <li>the lists of characters, things and places come first, numbered, the description on the line under the name;</li>
 * <li>a scene heading is in parentheses: "(दृश्य 1 — भीगा हुआ लेखक)";</li>
 * <li>every description is in parentheses; every spoken line is in double quotation marks after the speaker's name,
 *     and how it is said follows it, in parentheses of its own — never inside the quotation marks;</li>
 * <li>the ending is plain: "समाप्त" (or "THE END").</li>
 * </ul>
 * The words of every line are kept exactly; a script that would lose or change a line is not formatted at all
 * ({@link #formatScript}). Formatting a formatted script gives the same script again.
 */
public final class ScreenplayFormat {
    private ScreenplayFormat() {}

    /** The script in the standard format, from the story as the reader read it. */
    public static String format(Story st) {
        // (the headings in the script's own letters: a Hinglish story written in Roman letters keeps English headings)
        StringBuilder names = new StringBuilder(st.title);
        for (Story.CharacterDef c : st.characters) names.append(c.displayName);
        boolean hi = names.toString().matches("(?s).*[\\u0900-\\u097F].*") || (st.hindi && names.toString().trim().isEmpty());
        StringBuilder b = new StringBuilder();
        b.append(hi ? "शीर्षक: " : "Title: ").append(plain(st.title)).append("\n\n");
        b.append(hi ? "पात्र" : "Characters").append("\n\n");
        int n = 0;
        for (Story.CharacterDef c : st.characters) {
            if (!c.fromScript || c.voiceOnly && c.description.trim().isEmpty()) continue;
            b.append(++n).append(". ").append(plain(c.displayName));
            if (c.age > 0) b.append(" (").append(c.age).append(hi ? " साल" : " years").append(")");
            b.append('\n');
            String d = ownDescription(c);
            if (d.length() > 0) b.append(listText(d)).append('\n');
            b.append('\n');
        }
        // (a thing named with brackets of its own — "काला छाता (मीरा का)" — is written "1. name: description" on one
        // line, the form the user's own lists use for it, so the brackets stay part of its name)
        boolean oneLine = false;
        for (Story.PlaceDef t : st.things) if (t.name.indexOf('(') >= 0) oneLine = true;
        for (Story.PlaceDef p : st.places) if (p.name.indexOf('(') >= 0) oneLine = true;
        if (!st.things.isEmpty()) {
            b.append(hi ? "वस्तुएं" : "Objects").append("\n\n");
            n = 0;
            for (Story.PlaceDef t : st.things) entry(b, ++n, t, oneLine);
        }
        if (!st.places.isEmpty()) {
            b.append(hi ? "स्थान" : "Places").append("\n\n");
            n = 0;
            for (Story.PlaceDef p : st.places) entry(b, ++n, p, oneLine);
        }
        b.append(hi ? "कहानी" : "Story").append("\n\n");
        for (Story.Scene sc : st.scenes) {
            b.append('(').append(hi ? "दृश्य " : "Scene ").append(sc.number);
            String title = paren(sc.title).trim();
            if (title.length() > 0) b.append(" — ").append(title);
            b.append(")\n");
            for (Story.Beat bt : sc.beats) {
                if (bt.type == Story.Beat.DIALOGUE) {
                    b.append(plain(speakerOf(bt, hi))).append(": \"").append(quoted(bt.text)).append('"');
                    if (bt.manner != null && bt.manner.trim().length() > 0) b.append(" (").append(paren(bt.manner)).append(')');
                    b.append('\n');
                } else if (bt.text != null && bt.text.trim().length() > 0) {
                    // (a long direction of comma-joined clauses is read clause by clause, as the reader reads the format)
                    String d = paren(bt.text);
                    if (d.length() > 80) d = ScriptParser.clauseSentences(d);
                    b.append('(').append(d).append(")\n");
                }
            }
            b.append('\n');
        }
        b.append(hi ? "समाप्त" : "THE END").append('\n');
        return b.toString();
    }

    /** What the list says of a character, without the name and the age the reader keeps at its start. */
    static String ownDescription(Story.CharacterDef c) {
        String d = c.description == null ? "" : c.description.trim();
        if (c.fullName != null && c.fullName.length() > 0 && d.startsWith(c.fullName)) d = d.substring(c.fullName.length());
        else if (c.displayName != null && d.startsWith(c.displayName)) d = d.substring(c.displayName.length());
        d = d.replaceFirst("^[\\s,\\-–—:]*(?:\\(\\s*[0-9०-९]+\\s*(?:साल|वर्ष|years?|yrs?)?\\s*\\)|[0-9०-९]+\\s*(?:साल|वर्ष|years?|yrs?))[\\s,]*", "");
        return d.replaceFirst("^[\\s,\\-–—:]+", "").trim();
    }

    static void entry(StringBuilder b, int n, Story.PlaceDef p, boolean oneLine) {
        b.append(n).append(". ").append(plain(p.name));
        String d = listText(p.description);
        if (oneLine) { if (d.length() > 0) b.append(": ").append(d); b.append("\n\n"); return; }
        b.append('\n');
        if (d.length() > 0) b.append(d).append('\n');
        b.append('\n');
    }

    static String speakerOf(Story.Beat bt, boolean hi) {
        if (bt.speaker != null && bt.chorus.isEmpty()) return bt.speaker.displayName;
        if (bt.speakerRaw != null && bt.speakerRaw.trim().length() > 0) return bt.speakerRaw.trim();
        return hi ? "सूत्रधार" : "Narrator";
    }

    /** Text that must stand plain (a title, a name): no brackets or quotation marks around it. */
    static String plain(String s) {
        String t = s == null ? "" : s.trim();
        while (t.length() > 1 && ((t.startsWith("(") && t.endsWith(")")) || (t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("“") && t.endsWith("”"))))
            t = t.substring(1, t.length() - 1).trim();
        return t.replace("\n", " ");
    }

    /** A description's text as it sits inside one pair of parentheses: brackets inside it kept only when they pair up. */
    static String paren(String s) {
        String t = plain(s).replaceAll("\\s*\\n\\s*", " ");
        int depth = 0;
        boolean ok = true;
        for (int i = 0; i < t.length() && ok; i++) { char c = t.charAt(i); if (c == '(') depth++; else if (c == ')' && --depth < 0) ok = false; }
        if (!ok || depth != 0) t = t.replace('(', '[').replace(')', ']');
        return t;
    }

    /** The words of a line inside the quotation marks: a quotation inside them is written with the curly marks. */
    static String quoted(String s) {
        String t = s == null ? "" : s.trim().replaceAll("\\s*\\n\\s*", " ");
        StringBuilder o = new StringBuilder();
        boolean open = true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '"') { o.append(open ? '“' : '”'); open = !open; } else o.append(c);
        }
        return o.toString();
    }

    /** A list entry's description on its own line (one line; never mistaken for a numbered entry or a heading). */
    static String listText(String s) {
        String t = s.trim().replaceAll("\\s*\\n\\s*", ", ");
        if (t.startsWith("(") && t.endsWith(")")) t = t.substring(1, t.length() - 1).trim();
        t = t.replaceAll("[।.,\\s]+$", "");          // (the list's line ends without a stop, as the reader keeps it)
        // ("रूप: चश्मा, पोनीटेल" — a label inside a description reads as a comma once the lines are one entry)
        t = t.replaceAll("\\s*[:：]\\s+", ", ");
        if (t.matches("^[0-9०-९]{1,3}\\s*[.)].*")) t = "— " + t;
        return t;
    }

    /**
     * The script in the standard format, or null when formatting it would change what is said: the same lines, by the
     * same speakers, in the same order, and the same scenes — checked by reading the formatted script again.
     */
    public static String formatScript(String raw) {
        Story a = ScriptParser.parse(raw);
        String out = format(a);
        Story b = ScriptParser.parse(out);
        return sameStory(a, b) ? out : null;
    }

    /** Whether two readings say the same: scenes, and every line's speaker and words, in order. */
    public static boolean sameStory(Story a, Story b) {
        if (a.scenes.size() != b.scenes.size()) return false;
        List<String> la = lines(a), lb = lines(b);
        return la.equals(lb) && a.characters.size() == b.characters.size() && a.things.size() == b.things.size() && a.places.size() == b.places.size();
    }

    static List<String> lines(Story s) {
        List<String> out = new ArrayList<String>();
        for (Story.Scene sc : s.scenes) for (Story.Beat bt : sc.beats)
            if (bt.type == Story.Beat.DIALOGUE) out.add(sc.number + "|" + (bt.speaker != null ? bt.speaker.displayName : bt.speakerRaw) + "|" + Txt.norm(bt.text.replace('“', '"').replace('”', '"')).trim());
        return out;
    }

    /** What is wrong with the story as a screenplay (not with its punctuation): a missing title, empty scenes, unlisted speakers. */
    public static List<String> validate(Story st) {
        List<String> out = new ArrayList<String>();
        if (st.title == null || st.title.trim().isEmpty()) out.add("The script has no title (शीर्षक).");
        if (st.scenes.isEmpty()) out.add("The script has no scenes (दृश्य).");
        for (Story.Scene sc : st.scenes) {
            boolean any = false;
            for (Story.Beat bt : sc.beats) if (bt.text != null && bt.text.trim().length() > 0) any = true;
            if (!any) out.add("Scene " + sc.number + " is empty.");
            for (Story.Beat bt : sc.beats) {
                if (bt.type != Story.Beat.DIALOGUE) continue;
                if (bt.text == null || bt.text.trim().isEmpty()) out.add("Scene " + sc.number + ": a line of " + bt.speakerRaw + " has no words.");
                if (!bt.narrator && bt.speaker != null && !bt.speaker.fromScript)
                    out.add("Scene " + sc.number + ": \"" + bt.speakerRaw + "\" speaks but is not in the list of characters (पात्र).");
                if (!bt.narrator && bt.speaker == null && bt.chorus.isEmpty()) out.add("Scene " + sc.number + ": a line with no speaker: \"" + bt.text + "\".");
            }
        }
        return out;
    }

    static final Pattern HEADING = Pattern.compile("^(शीर्षक|title)\\s*[:：]\\s*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final Pattern SCENE_LINE = Pattern.compile("^\\(?\\s*(?:दृश्य|scene|सीन)\\s*[-–—:.]?\\s*[0-9०-९]+.*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static final Pattern SPEECH = Pattern.compile("^([^:：\"“”()\\[\\]]{1,40})[:：]\\s*(.*)$");

    /**
     * The format's rules checked on the text itself, line by line (the lists before the story are entries, not
     * descriptions). Each message names the line. An empty list: the script is in the standard format.
     */
    public static List<String> validateFormattedText(String text) {
        List<String> out = new ArrayList<String>();
        String[] ls = (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int first = -1, last = -1;
        for (int i = 0; i < ls.length; i++) if (ls[i].trim().length() > 0) { if (first < 0) first = i; last = i; }
        if (first < 0) { out.add("The script is empty."); return out; }
        // the title
        String t0 = ls[first].trim();
        Matcher hm = HEADING.matcher(t0);
        String title = hm.matches() ? hm.group(2).trim() : t0;
        if (title.isEmpty()) out.add("Line " + (first + 1) + ": the title is empty.");
        else if (title.startsWith("(") || title.startsWith("\"") || title.startsWith("“") || title.endsWith(")") || title.endsWith("\""))
            out.add("Line " + (first + 1) + ": the title is plain text — no brackets or quotation marks (" + title + ").");
        // the ending
        String end = ls[last].trim();
        String endWord = end.replaceAll("[\\s.।!]+$", "");
        String bare = plain(endWord).replaceAll("[\\s.।!]+$", "");
        boolean endWordOnly = bare.matches("(?iu)समाप्त|the end|end|इति|द एंड");
        if (!bare.equals(endWord) && endWordOnly) out.add("Line " + (last + 1) + ": the ending is plain text — no brackets or quotation marks.");
        else if (!endWordOnly) {
            if (endWord.matches("(?iu)(समाप्त|the end|end)\\s*[-–—:,|].*")) out.add("Line " + (last + 1) + ": the ending is the word alone (\"समाप्त\" / \"THE END\"); the rest belongs in the last scene's description.");
            else out.add("The script does not end with समाप्त (or THE END).");
        }
        // the story's lines
        boolean story = false;
        for (int i = first + 1; i < last; i++) {
            String l = ls[i].trim();
            if (l.isEmpty()) continue;
            String low = l.replaceAll("[:：\\-–—.]+$", "").trim().toLowerCase(java.util.Locale.ROOT);
            if (low.matches("कहानी|कहानी शुरू|story|the story|story begins")) { story = true; continue; }
            if (!story) {
                if (SCENE_LINE.matcher(l).matches()) story = true;
                else continue;            // the lists: names and their descriptions
            }
            int bal = 0;
            for (char c : l.toCharArray()) { if (c == '(') bal++; else if (c == ')') bal--; }
            if (bal != 0) out.add("Line " + (i + 1) + ": the parentheses do not pair up.");
            if (SCENE_LINE.matcher(l).matches()) {
                if (!(l.startsWith("(") && l.endsWith(")"))) out.add("Line " + (i + 1) + ": a scene heading goes in parentheses — (" + l + ").");
                continue;
            }
            if (l.startsWith("(") && l.endsWith(")")) continue;          // a description
            if (l.startsWith("[") && l.endsWith("]")) { out.add("Line " + (i + 1) + ": a description goes in parentheses ( ), not square brackets."); continue; }
            Matcher sp = SPEECH.matcher(l);
            if (sp.matches()) {
                String rest = sp.group(2).trim();
                // the note before the words is allowed by the reader, but the format puts it after: name: "words" (how)
                if (rest.startsWith("(")) {
                    int z = rest.indexOf(')');
                    out.add("Line " + (i + 1) + ": the note on how it is said comes after the words, in its own parentheses.");
                    rest = z > 0 ? rest.substring(z + 1).trim() : rest;
                }
                if (!(rest.startsWith("\"") || rest.startsWith("“"))) { out.add("Line " + (i + 1) + ": the spoken words go in double quotation marks."); continue; }
                int close = Math.max(rest.lastIndexOf('"'), rest.lastIndexOf('”'));
                if (close <= 0) { out.add("Line " + (i + 1) + ": the quotation marks do not close."); continue; }
                String after = rest.substring(close + 1).trim();
                if (after.length() > 0 && !(after.startsWith("(") && after.endsWith(")"))) out.add("Line " + (i + 1) + ": what follows the spoken words goes in parentheses.");
                continue;
            }
            out.add("Line " + (i + 1) + ": a description goes in parentheses.");
        }
        return out;
    }
}

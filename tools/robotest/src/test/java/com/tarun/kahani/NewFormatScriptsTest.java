package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.ProductionDocument;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * v47.6: the user's two scripts in the fixed format (आखिरी जोड़ी, आखिरी चाय): read exactly as written — the lists, every
 * line with its speaker in order (the note before the words is never spoken), the end ("समाप्त - समय: …", "END.") never a
 * line — and the environment each asks for, from the shared table: what the film carries out, what it cannot (said), what
 * no film can show (a smell). The same reader for every story: nothing here is special to these two.
 */
public class NewFormatScriptsTest {
    static final File JODI = new File("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_numbered.txt");
    static final File CHAI = new File("../../tools/testdata/aakhiri_chai/aakhiri_chai_numbered.txt");

    static String text(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    /** The lines read from the file on their own: "name: (note) "words"" or "name: "words" (note)". */
    static List<String[]> linesOf(String script) {
        List<String[]> out = new ArrayList<String[]>();
        boolean story = false;
        Matcher m = Pattern.compile("^([^:\"()\\[\\]]{1,30}):\\s*(?:\\([^)]*\\)\\s*)?\"([^\"]+)\"").matcher("");
        for (String l : script.split("\n")) {
            if (l.trim().equals("कहानी")) story = true;
            if (story && m.reset(l.trim()).find()) out.add(new String[]{m.group(1).trim(), m.group(2).trim()});
        }
        return out;
    }

    static final class Made { Story st; Film film; ProductionDocument doc; }

    static Made make(File f) throws Exception {
        Made m = new Made();
        m.st = ScriptParser.parse(text(f));
        m.doc = ProductionDocument.read(m.st, null);
        Director dir = new Director(m.st, new Director.Options());
        dir.prepare();
        m.film = dir.direct(new Art());
        m.doc.bind(m.st);
        m.doc.attach(m.film);
        m.doc.validate();
        return m;
    }

    static void sameLines(File f, ProductionDocument d, int n) throws Exception {
        List<String[]> want = linesOf(text(f));
        assertEquals(n, want.size());
        assertEquals(want.size(), d.lines.size());
        for (int i = 0; i < want.size(); i++) {
            assertEquals("speaker of line " + (i + 1), want.get(i)[0], d.lines.get(i).speakerName);
            assertEquals("line " + (i + 1), want.get(i)[1], d.lines.get(i).text.trim());
            assertFalse(d.lines.get(i).text.contains("("));
            assertFalse("a note is never prefixed by a stray comma", d.lines.get(i).delivery.startsWith(","));
        }
    }

    static ProductionDocument.Effect effect(ProductionDocument d, String scene, String kind) {
        for (ProductionDocument.Effect e : d.effects) if ((scene == null || e.sceneId.equals(scene)) && e.kind.equals(kind)) return e;
        return null;
    }

    @Test public void aakhiriJodiIsReadAsWritten() throws Exception {
        ProductionDocument d = make(JODI).doc;
        assertEquals(2, d.characters.size());
        assertEquals(7, d.props.size());
        assertEquals(3, d.locations.size());
        assertEquals(3, d.scenes.size());
        assertEquals("मोची का औजारों वाला बक्सा", d.props.get(1).name);
        // "समाप्त - समय: शाम 6:30 बजे" is the end, not a line spoken by "समाप्त - समय"
        sameLines(JODI, d, 6);
        for (ProductionDocument.Line l : d.lines) assertNotNull(l.speakerId);
        // the time with its minutes; "बक्सा नहीं खोलते" is read and never staged; "(दौड़ती हुई आती है …)" is her step
        assertTrue(effect(d, "SCENE_01", "TIME").behaviour.startsWith("6:30"));
        boolean notOpened = false, ranIn = false;
        for (ProductionDocument.Action a : d.actions) {
            if (a.verb.equals("OPEN") && a.negated && a.span.contains("बक्सा")) notOpened = true;
            if (a.verb.equals("RUN") && a.actorId.equals(d.characters.get(1).id)) ranIn = true;
        }
        assertTrue("\"इस्माइल चाचा बक्सा नहीं खोलते\" is a negated step", notOpened);
        assertTrue("the note with her first line is her own step", ranIn);
        assertNotNull(effect(d, "SCENE_01", "RAIN"));
        assertEquals(ProductionDocument.UNSUPPORTED, effect(d, "SCENE_03", "LIGHT_FLICKER").method);
    }

    @Test public void aakhiriChaiIsReadAsWritten() throws Exception {
        Made m = make(CHAI);
        ProductionDocument d = m.doc;
        assertEquals(3, d.characters.size());
        assertEquals(16, d.props.size());
        assertEquals(3, d.locations.size());
        assertEquals(4, d.scenes.size());
        sameLines(CHAI, d, 11);
        // "काला छाता (मीरा का)": whose it is, written; and the umbrella is in the scenes that name it
        ProductionDocument.Entity umbrella = d.props.get(10);
        assertEquals(d.characters.get(2).id, umbrella.fact("owner"));
        assertFalse(umbrella.sceneIds.isEmpty());
        // the scene headings' own times
        assertTrue(effect(d, "SCENE_01", "TIME").behaviour.startsWith("7:15"));
        assertTrue(effect(d, "SCENE_04", "TIME").behaviour.startsWith("7:35"));
        // the environment by the shared table: carried out, said to be unsupported, or not filmable — never claimed
        assertEquals("heavy rain", effect(d, "SCENE_01", "RAIN").behaviour);
        assertTrue(effect(d, "SCENE_01", "RAIN").scheduled);
        assertTrue("\"जोरदार बिजली\" flashes in the plan", effect(d, "SCENE_02", "LIGHTNING").scheduled);
        assertTrue(effect(d, "SCENE_04", "RAIN_STOPS").scheduled);
        assertEquals(ProductionDocument.NOT_FILMABLE, effect(d, "SCENE_01", "SMELL").method);
        assertEquals(ProductionDocument.UNSUPPORTED, effect(d, "SCENE_04", "SOUND_AZAAN").method);
        assertEquals(ProductionDocument.UNSUPPORTED, effect(d, "SCENE_01", "REFLECTION").method);
        boolean said = false;
        for (ProductionDocument.Issue i : d.issues) if (i.requirement.startsWith("SOUND_AZAAN")) said = true;
        assertTrue("an unsupported requirement is in the issue register", said);
        for (ProductionDocument.Issue i : d.issues) assertFalse(i.requirement.startsWith("SMELL"));
    }

    @Test public void waterThatFallsMakesNobodyFallAndTheyComeInWhenTheScriptSays() throws Exception {
        Made m = make(CHAI);
        Film.Seg first = null, second = null;
        for (Film.Seg sg : m.film.segs) if (sg.type == Film.S_SCENE) { if (first == null) first = sg; else if (second == null) second = sg; }
        for (Film.Seg sg : m.film.segs) for (Film.Actor a : sg.actors) for (Film.Act x : a.acts)
            assertNotEquals(a.c.displayName + " falls at " + x.t0 + " (water and tea fall, nobody does)", Film.G_FALL, x.type);
        for (Film.Actor a : first.actors) {
            if (a.c.displayName.equals("रघु काका")) assertEquals("\"भट्टी के पास बैठा\": seated", Pose.SIT, a.stateAt(first.t0 + 1).body);
            if (a.c.displayName.equals("आरव")) assertFalse("\"तभी आरव दौड़ता आता है\": not there at the start", a.stateAt(first.t0 + 0.05f).visible);
        }
        for (Film.Actor a : second.actors) if (a.c.displayName.equals("मीरा")) assertFalse("\"तभी मीरा आती है\"", a.stateAt(second.t0 + 0.05f).visible);
    }
}

package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.ProductionDocument;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * v47.6: the production document (Stage B of the studio's production protocol): stable ids kept with the project,
 * every approval bound to the revision it read, the director never signing off over an open blocker, and a render that
 * is never reported done unless its file is there and is not empty.
 */
public class ProductionDocumentTest {
    static String story(String things, String scene) {
        return "शीर्षक: आँगन की गेंद\n\nपात्र\n\n1. मीरा (10)\nएक छोटी लड़की, लाल फ्रॉक\n\nवस्तुएं\n\n" + things
                + "\nस्थान\n\n1. घर का आँगन\nईंटों का आँगन\n\nकहानी\n\nदृश्य 1\n(" + scene + ")\n\nमीरा: \"चलो खेलें!\" (खुश होकर)\n\nसमाप्त\n";
    }
    static final String BALL = "1. लाल गेंद\nरबर की गेंद\n\n", BOOK = "नीली किताब\nकागज की किताब\n\n", KITE = "पीली पतंग\nकागज की पतंग\n\n";

    static ProductionDocument planned(Story st, ProductionDocument.Ids ids) {
        ProductionDocument d = ProductionDocument.read(st, ids);
        Director dir = new Director(st, new Director.Options());
        dir.prepare();
        Film film = dir.direct(new Art());
        d.bind(st);
        d.attach(film);
        d.assets(new Art());
        d.validate();
        return d;
    }

    static String idOf(List<ProductionDocument.Entity> list, String name) {
        for (ProductionDocument.Entity e : list) if (e.name.equals(name)) return e.id;
        return null;
    }

    @Test public void idsStayWhenAPropIsInsertedBeforeAnother() {
        Story a = ScriptParser.parse(story(BALL + "2. " + BOOK, "मीरा आँगन में लाल गेंद उठाती है"));
        ProductionDocument first = ProductionDocument.read(a, null);
        assertEquals(2, first.props.size());
        assertEquals("PROP_001", idOf(first.props, "लाल गेंद"));
        assertEquals("PROP_002", idOf(first.props, "नीली किताब"));
        String kept = first.ids.write();

        // the kite is listed between the ball and the book: read with the project's own ids, nothing is renumbered
        Story b = ScriptParser.parse(story(BALL + "2. " + KITE + "3. " + BOOK, "मीरा आँगन में लाल गेंद उठाती है"));
        ProductionDocument again = ProductionDocument.read(b, ProductionDocument.Ids.parse(kept));
        assertEquals(3, again.props.size());
        assertEquals("PROP_001", idOf(again.props, "लाल गेंद"));
        assertEquals("PROP_002", idOf(again.props, "नीली किताब"));
        assertEquals("PROP_003", idOf(again.props, "पीली पतंग"));
        assertEquals(idOf(first.characters, "मीरा"), idOf(again.characters, "मीरा"));
        assertEquals(idOf(first.locations, "घर का आँगन"), idOf(again.locations, "घर का आँगन"));

        // (without the kept ids the book would have moved: that is why they are kept with the project)
        ProductionDocument fresh = ProductionDocument.read(b, null);
        assertEquals("PROP_003", idOf(fresh.props, "नीली किताब"));
    }

    @Test public void anApprovalIsBoundToItsRevision() {
        Story st = ScriptParser.parse(story(BALL, "मीरा आँगन में लाल गेंद उठाती है"));
        ProductionDocument d = planned(st, null);
        assertEquals("VALIDATED", d.reviews.get(ProductionDocument.TECHNICAL_DIRECTOR).status);
        assertEquals("APPROVED", d.review(ProductionDocument.DIRECTOR, "APPROVED", "the plan follows the script").status);
        assertTrue(d.approvedForRender());

        d.revise("a line changed");
        assertFalse("an approval of revision 1 approves nothing in revision 2", d.approvedForRender());
        ProductionDocument.Review r = d.review(ProductionDocument.DIRECTOR, "APPROVED", "looks fine");
        assertEquals("the technical director has not validated revision 2", "REFUSED", r.status);
        assertFalse(d.approvedForRender());

        d.validate();
        assertEquals(2, d.reviews.get(ProductionDocument.TECHNICAL_DIRECTOR).revision);
        assertEquals("APPROVED", d.review(ProductionDocument.DIRECTOR, "APPROVED", "re-read").status);
        assertTrue(d.approvedForRender());
    }

    @Test public void theDirectorNeverSignsOffOverAnOpenBlocker() {
        Story st = ScriptParser.parse(story(BALL, "मीरा आँगन में लाल गेंद उठाती है"));
        ProductionDocument d = planned(st, null);
        ProductionDocument.Issue b = d.addIssue(ProductionDocument.BLOCKER, ProductionDocument.QC, "SCENE_01", "the ball is seen", "the frame at 3 s is black", "render again");
        d.validate();
        assertEquals("BLOCKED", d.reviews.get(ProductionDocument.TECHNICAL_DIRECTOR).status);
        assertTrue("the QC blocker stays through the re-validation", d.issues.contains(b));
        ProductionDocument.Review r = d.review(ProductionDocument.DIRECTOR, "APPROVED", "creative sign-off");
        assertEquals("REFUSED", r.status);
        assertTrue(r.issueIds.contains(b.id));
        assertFalse(d.approvedForRender());

        int rev = d.revision;
        d.resolve(b.id, "rendered again, the frame is lit");
        assertEquals(rev + 1, d.revision);
        d.validate();
        assertEquals("VALIDATED", d.reviews.get(ProductionDocument.TECHNICAL_DIRECTOR).status);
        assertEquals("APPROVED", d.review(ProductionDocument.DIRECTOR, "APPROVED", "creative sign-off").status);
    }

    @Test public void aRenderIsDoneOnlyWithItsFile() throws Exception {
        Story st = ScriptParser.parse(story(BALL, "मीरा आँगन में लाल गेंद उठाती है"));
        ProductionDocument d = planned(st, null);
        File dir = Files.createTempDirectory("proddoc").toFile();
        File film = new File(dir, "film.mp4");
        try {
            // not approved: never verified, whatever the file
            Files.write(film.toPath(), new byte[4096]);
            assertEquals("UNAPPROVED", d.verifyRender(film, "test", 10).status);
            assertFalse(d.complete());

            d.review(ProductionDocument.DIRECTOR, "APPROVED", "ok");
            assertEquals("FAILED", d.verifyRender(new File(dir, "missing.mp4"), "test", 10).status);
            assertFalse(d.complete());
            Files.write(film.toPath(), new byte[10]);
            ProductionDocument.RenderRecord empty = d.verifyRender(film, "test", 10);
            assertEquals("FAILED", empty.status);
            assertTrue(empty.detail.contains("empty"));
            assertFalse(d.complete());

            Files.write(film.toPath(), new byte[4096]);
            assertEquals("VERIFIED", d.verifyRender(film, "test", 10).status);
            assertTrue(d.complete());
            d.revise("the ball is now blue");
            assertFalse("a render of an older revision is not this revision's film", d.complete());
        } finally {
            film.delete();
            dir.delete();
        }
    }

    @Test public void negationsAndHypotheticalsAreReadNotStaged() {
        Story st = ScriptParser.parse(story(BALL, "मीरा बिना आवाज किए लाल गेंद उठाती है, अगर वह दौड़ती तो गेंद गिर जाती"));
        ProductionDocument d = planned(st, null);
        boolean mustNot = false, hypothetical = false;
        for (ProductionDocument.Action a : d.actions) {
            if (a.verb.equals("MUST_NOT")) { mustNot = true; assertTrue(a.negated); }
            if (a.verb.equals("RUN")) { hypothetical = true; assertTrue(a.hypothetical); assertEquals("HYPOTHETICAL", a.status); }
        }
        assertTrue("\"बिना आवाज किए\" is a step that must not happen", mustNot);
        assertTrue("\"अगर वह दौड़ती\" is read as a condition", hypothetical);
    }

    @Test public void theDocumentIsJson() {
        Story st = ScriptParser.parse(story(BALL, "मीरा आँगन में लाल गेंद उठाती है"));
        ProductionDocument d = planned(st, null);
        Object o = Json.parseLoose(d.toJson());
        assertTrue(o instanceof Map);
        Map<?, ?> m = (Map<?, ?>) o;
        for (String k : new String[]{"schemaVersion", "productionId", "revision", "characters", "props", "locations", "scenes", "dialogueLines",
                "actionBeats", "environmentalRequirements", "objectStates", "shotPlans", "issueRegister", "agentReviews", "revisionHistory"})
            assertTrue(k, m.containsKey(k));
        assertEquals(1, ((List<?>) m.get("dialogueLines")).size());
    }
}

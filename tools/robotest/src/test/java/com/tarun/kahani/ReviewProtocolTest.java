package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.AssetDependency;
import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.ReferenceCoverage;
import com.tarun.kahani.core.ReviewConference;
import com.tarun.kahani.core.ReviewLedger;
import com.tarun.kahani.core.ReviewLedger.Issue;
import com.tarun.kahani.core.ReviewLedger.Role;
import com.tarun.kahani.core.ReviewLedger.Severity;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/** v46: the four-role review protocol — stable ids, severities, ownership, bounded rounds, an audit trail that round-trips. */
public class ReviewProtocolTest {
    static Story aakhiriChai() throws Exception {
        File assets = new File(System.getProperty("kahani.assets", "../../app/src/main/assets"));
        return ScriptParser.parse(new String(Files.readAllBytes(new File(assets, "aakhiri_chai_screenplay.txt").toPath()), StandardCharsets.UTF_8));
    }

    @Test public void blockersAreClosedOnlyByTheirOwnerAndNeverWaivedByTheDirector() {
        ReviewLedger l = new ReviewLedger("t");
        Issue b = l.open(Role.TECHNICAL_DIRECTOR, Role.TECHNICAL_DIRECTOR, "shot", "7", Severity.BLOCKER, "head cut by the top of the frame", "validate", "frame wider", "validate passes");
        Issue m = l.open(Role.EDITOR, Role.DIRECTOR, "scene", "2", Severity.MAJOR, "no reaction shot", "cut", "add one", "a reaction shot exists");
        assertEquals("RV-0001", b.id); assertEquals("RV-0002", m.id);
        assertFalse(l.readyToRender());
        assertFalse("director cannot waive a technical blocker", l.decide(b.id, "reject", "artistic choice"));
        assertFalse(l.resolve(b.id, Role.DIRECTOR, "looks fine"));
        assertFalse(l.resolve(b.id, Role.EDITOR, "looks fine"));
        assertFalse(b.resolved);
        assertFalse("TD's work is not validated while its blocker is open", l.validated(Role.TECHNICAL_DIRECTOR));
        assertTrue(l.resolve(b.id, Role.TECHNICAL_DIRECTOR, "reframed"));
        assertTrue(l.validated(Role.TECHNICAL_DIRECTOR));
        assertTrue(l.readyToRender());
        // the Director decides creative majors; the Editor cannot close the Director's issue
        assertFalse(l.resolve(m.id, Role.EDITOR, "never mind"));
        assertTrue(l.decide(m.id, "adapt", "a reaction held two seconds"));
        assertEquals(Role.DIRECTOR, m.resolvedBy);
        assertTrue(l.log.size() >= 6);
    }

    @Test public void roundsAreBoundedAndDependenciesGateResolution() {
        ReviewLedger l = new ReviewLedger("t");
        Issue a = l.open(Role.READER, Role.READER, "character", "मीरा", Severity.BLOCKER, "no picture", "", "", "");
        Issue c = l.open(Role.DIRECTOR, Role.DIRECTOR, "shot", "3", Severity.MINOR, "close-up of मीरा", "", "", "");
        c.dependsOn.add(a.id);
        assertFalse("blocked by the open dependency", l.resolve(c.id, Role.DIRECTOR, "done"));
        assertTrue(l.resolve(a.id, Role.READER, "uploaded"));
        assertTrue(l.resolve(c.id, Role.DIRECTOR, "done"));
        assertTrue(l.nextRound()); assertTrue(l.nextRound());
        assertEquals(ReviewLedger.MAX_ROUNDS, l.round);
        assertFalse(l.nextRound());
        assertEquals(ReviewLedger.MAX_ROUNDS, l.round);
    }

    @Test public void ledgerRoundTripsThroughJson() {
        ReviewLedger l = new ReviewLedger("आखिरी चाय");
        Issue i = l.open(Role.EDITOR, Role.DIRECTOR, "line", "2.3", Severity.MAJOR, "line \"quoted\" too long", "ev", "cut", "ok");
        l.decide(i.id, "accept", "cut to two seconds");
        l.open(Role.READER, Role.READER, "prop", "केतली", Severity.BLOCKER, "no picture", "", "", "");
        String json = l.toJson();
        ReviewLedger back = ReviewLedger.fromJson(json);
        assertEquals(l.storyId, back.storyId);
        assertEquals(2, back.issues.size());
        assertEquals(i.id, back.issues.get(0).id);
        assertTrue(back.issues.get(0).resolved);
        assertEquals(Role.DIRECTOR, back.issues.get(0).resolvedBy);
        assertEquals("line \"quoted\" too long", back.issues.get(0).finding);
        assertFalse(back.readyToRender());
        assertEquals(l.log, back.log);
        Issue next = back.open(Role.READER, Role.READER, "story", "", Severity.NOTE, "n", "", "", "");
        assertEquals("ids keep counting after a reload", "RV-0003", next.id);
        assertTrue(json.contains("\"schema\":\"kahani-review-ledger/1\""));
    }

    @Test public void assetDependenciesAreClassifiedFromTheManifestAndLibrary() throws Exception {
        Story st = aakhiriChai();
        List<AssetDependency.Dep> none = AssetDependency.classify(st, "", null, null, null);
        int essentialChars = 0, essentialPlaces = 0;
        for (AssetDependency.Dep d : none) {
            if (d.kind.equals("character")) { assertEquals(d.toString(), AssetDependency.State.MISSING_ESSENTIAL, d.state); essentialChars++; }
            if (d.kind.equals("place") && d.essential) { assertEquals(d.toString(), AssetDependency.State.MISSING_ESSENTIAL, d.state); essentialPlaces++; }
        }
        assertEquals("three characters on stage", 3, essentialChars);
        assertTrue("at least one place is essential", essentialPlaces >= 1);
        assertTrue(AssetDependency.request(none).contains("please upload"));
        // a manifest with pictures, a library candidate, a generated picture and a pending request
        String cast = "char|रघु काका|c1.png\nchar|आरव|c2.png\n";
        for (Story.Scene sc : st.scenes) cast += "scene|" + sc.number + "|s" + sc.number + ".jpg\n";
        Map<String, Float> cands = new HashMap<String, Float>(); cands.put("मीरा", 0.6f);
        Set<String> gen = new HashSet<String>(); gen.add("आरव");
        List<AssetDependency.Dep> deps = AssetDependency.classify(st, cast, cands, gen, null);
        Map<String, AssetDependency.State> by = new HashMap<String, AssetDependency.State>();
        for (AssetDependency.Dep d : deps) by.put(d.kind + ":" + d.key, d.state);
        assertEquals(AssetDependency.State.AVAILABLE_MATCHED, by.get("character:रघु काका"));
        assertEquals(AssetDependency.State.GENERATED, by.get("character:आरव"));
        assertEquals(AssetDependency.State.AVAILABLE_UNMATCHED, by.get("character:मीरा"));
        for (AssetDependency.Dep d : deps) if (d.kind.equals("place") && d.essential) assertEquals(d.toString(), AssetDependency.State.AVAILABLE_MATCHED, d.state);
        assertTrue("props were inventoried", AssetDependency.in(deps, AssetDependency.State.MISSING_ESSENTIAL).size() + AssetDependency.in(deps, AssetDependency.State.MISSING_NONESSENTIAL).size() > 0);
        Set<String> asked = new HashSet<String>(); asked.add("char:मीरा");
        for (AssetDependency.Dep d : AssetDependency.classify(st, cast, null, gen, asked)) if (d.key.equals("मीरा")) assertEquals(AssetDependency.State.BLOCKED, d.state);
    }

    @Test public void conferenceBlocksOnMissingEssentialsAndClearsWhenTheyArrive() throws Exception {
        Story st = aakhiriChai();
        ReviewLedger l = ReviewConference.run(st, "", null, null, null, null);
        assertFalse("no pictures at all: not ready", l.readyToRender());
        assertFalse(l.validated(Role.READER));
        assertEquals(ReviewLedger.MAX_ROUNDS, l.round);
        for (Issue i : l.open(Severity.BLOCKER)) assertEquals("only the Reader holds asset blockers here", Role.READER, i.owner);
        for (Issue i : l.issues) if (i.owner == Role.DIRECTOR && i.severity != Severity.BLOCKER) assertTrue("the Director dispositioned " + i, i.resolved);
        String cast = "char|रघु काका|c1.png\nchar|आरव|c2.png\nchar|मीरा|c3.png\n";
        for (Story.Scene sc : st.scenes) cast += "scene|" + sc.number + "|s" + sc.number + ".jpg\n";
        for (Story.PlaceDef th : st.things) cast += "shot||" + th.name.trim() + "|p.png|object\n";
        ReviewLedger ok = ReviewConference.run(st, cast, null, null, null, null);
        assertTrue(ok.summary(), ok.readyToRender());
        assertTrue(ok.validated(Role.READER) && ok.validated(Role.DIRECTOR) && ok.validated(Role.EDITOR) && ok.validated(Role.TECHNICAL_DIRECTOR));
        Map<String, List<String>> td = new HashMap<String, List<String>>();
        td.put("12", java.util.Arrays.asList("head cut by the top of the frame"));
        ReviewLedger blocked = ReviewConference.run(st, cast, null, null, null, td);
        assertFalse(blocked.readyToRender());
        assertEquals(1, blocked.open(Severity.BLOCKER).size());
        assertEquals("shot", blocked.open(Severity.BLOCKER).get(0).refKind);
        assertTrue(blocked.toJson().contains("\"ready_to_render\":false"));
    }

    @Test public void cloudConferenceReportIsNormalisedIntoTheLedger() {
        String report = "{\"final_decision_by\":\"director\",\"ready_to_render\":false,"
                + "\"technical_blockers\":[\"कुल्हड़ count changes between scene 3 and 4\"],"
                + "\"qc_matrix\":{\"identity\":\"PASS\",\"continuity\":\"FAIL\",\"lipsync\":\"NEEDS_HUMAN_REVIEW\"},"
                + "\"asset_requests\":[{\"name\":\"रूमाल\",\"category\":\"prop\"}],"
                + "\"role_disagreements\":[\"editor wants a longer hold on the kulhad\"],"
                + "\"director_decisions\":[{\"issue\":\"longer hold on the kulhad\",\"role\":\"editor\",\"decision\":\"adapt\",\"reason\":\"held one second more\"}],"
                + "\"human_questions\":[]}";
        ReviewLedger l = ReviewConference.fromCloud(new ReviewLedger("t"), Json.parseLoose(report));
        assertEquals(3, l.open(Severity.BLOCKER).size());
        boolean disagreementClosed = false;
        for (Issue i : l.issues) if (i.finding.startsWith("disagreement")) { disagreementClosed = i.resolved && i.resolvedBy == Role.DIRECTOR; }
        assertTrue("the director's recorded decision closed the disagreement", disagreementClosed);
        for (Issue i : l.open(Severity.MAJOR)) assertTrue(i.finding.contains("needs human review"));
        assertFalse(l.readyToRender());
    }

    @Test public void referenceCoverageNamesTheMissingViews() throws Exception {
        Story st = aakhiriChai();
        String cast = "char|मीरा|f.png\nview|मीरा|180|b.png\npose|मीरा|p1.png|90|0|0|1.0\npose|मीरा|p2.png|0|3|1|1.0\npose|मीरा|p3.png|0|0|0|1.0\n";
        List<ReferenceCoverage.Coverage> cs = ReferenceCoverage.characters(st, cast);
        assertEquals(3, cs.size());
        ReferenceCoverage.Coverage meera = null, raghu = null;
        for (ReferenceCoverage.Coverage c : cs) { if (c.key.equals("मीरा")) meera = c; if (c.key.equals("रघु काका")) raghu = c; }
        assertNotNull(meera); assertNotNull(raghu);
        assertTrue(meera.front);
        assertTrue(meera.views.contains("back") && meera.views.contains("side"));
        assertEquals(3, meera.poses);
        assertTrue(meera.toString(), meera.missing.contains("side") && meera.missing.contains("three-quarter ×2"));
        assertFalse(meera.missing.contains("back"));
        assertFalse(raghu.front);
        assertTrue(raghu.missing.get(0).startsWith("front"));
        assertEquals(18, ReferenceCoverage.limitFor("char"));
        assertEquals(10, ReferenceCoverage.limitFor("obj"));
        assertEquals(10, ReferenceCoverage.limitFor("scene"));
        assertTrue(ReferenceCoverage.fitsSheet(6)); assertFalse(ReferenceCoverage.fitsSheet(7));
        assertEquals("sure", ReferenceCoverage.splitConfidence(new long[]{100, 104, 98, 101, 99, 103}));
        assertEquals("check", ReferenceCoverage.splitConfidence(new long[]{100, 104, 30, 101, 99, 103}));
        assertEquals("check", ReferenceCoverage.splitConfidence(new long[]{100, 104, 98, 101, 99, 103, 100}));
        assertTrue(ReferenceCoverage.report(cs).contains("18 pictures per character"));
    }
}

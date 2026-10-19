package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.ProductionDocument;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/**
 * v47.8 (the user: "debug everything … reader, director etc and all their collaboration"; "no shaking or random
 * movements"): what the director's audit over the 2,100 stories and the frame audit of random films found in the plan —
 * a walk faster than a body walks (a jolt), two rules' keys at one moment (a teleport), a walk that ended on top of
 * someone standing still — and that it is gone.
 */
public class DirectorAuditTest {
    static Film plan(String path) throws Exception {
        File f = new File("../../" + path);
        assertTrue(path, f.isFile());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        Director d = new Director(story, new Director.Options());
        d.prepare();
        return d.direct(new Art());
    }

    static boolean riding(Film.Actor a, float t0, float t1) {
        for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && x.t0 < t1 && x.t1 > t0) return true;
        return false;
    }

    @Test public void aMoveNeverOutrunsABody() {
        float h = 300;
        assertEquals(0, Director.leastMoveTime(0, h, false), 0);
        // a walk of a body's height takes at least a second; a run less; a longer walk longer
        assertTrue(Director.leastMoveTime(h, h, false) >= 1f / Director.WALK_SPEED - 1e-3f);
        assertTrue(Director.leastMoveTime(4 * h, h, true) < Director.leastMoveTime(4 * h, h, false));
        assertTrue(Director.leastMoveTime(2 * h, h, false) > Director.leastMoveTime(h, h, false));
        // its eased start and end stay within the change of speed a body makes in one frame
        float d = 155, t = Director.leastMoveTime(d, 260, false);
        assertTrue(6 * d / (t * t) / (24 * 24) <= Director.MOVE_ACCEL * 260 * 1.001f);
    }

    /** story_00860: "The family gets into the car" — Ayaan's 155 px walk to its door took half a second (a 3.3% jolt). */
    @Test public void theWalkToTheCarTakesItsTime() throws Exception {
        Film film = plan("tools/testdata/v44forge/story_00860.txt");
        int checked = 0;
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) {
            if (a.look == null || !a.look.isHumanoid()) continue;
            float h = Renderer.actorHeight(a.look, a.c, new Art(), sg);
            for (int i = 1; i < a.keys.size(); i++) {
                Film.Key k = a.keys.get(i);
                if (k.moveDur <= 0 || k.anchor != Film.A_GROUND || !k.visible || riding(a, k.t, k.t + k.moveDur)) continue;
                float dist = Math.abs(k.x - Director.xAt(a, k.t));
                if (dist < 40 || a.trackAt(k.t + 0.01f) != null) continue;
                float end = i + 1 < a.keys.size() ? Math.min(a.keys.get(i + 1).t, k.t + k.moveDur) : k.t + k.moveDur;
                if (end < k.t + k.moveDur - 0.01f) continue;      // (cut short by the next instruction: its own start)
                checked++;
                assertTrue(a.c.displayName + " moves " + Math.round(dist) + " px in " + k.moveDur + " s at " + k.t,
                        k.moveDur >= Director.leastMoveTime(dist, h, k.run) - 0.05f || k.hardStop);
            }
        }
        assertTrue(checked > 0);
    }

    /** The app's own sample story: two rules' keys at one moment made "आंटी चुड़ैल" jump 1,096 px inside a shot. */
    @Test public void nobodyJumpsFromPlaceToPlace() throws Exception {
        Film film = plan("app/src/main/assets/sample_story.txt");
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors)
            for (int i = 1; i < a.keys.size(); i++)
                assertTrue(a.c.displayName + ": two keys at " + a.keys.get(i).t, a.keys.get(i).t - a.keys.get(i - 1).t > 1e-4f);
        Film.Actor witch = null;
        Film.Seg at = null;
        for (Film.Seg sg : film.segs) if (sg.t0 <= 233.5f && sg.t1 > 233.5f) for (Film.Actor a : sg.actors) if (a.c.displayName.equals("आंटी चुड़ैल")) { witch = a; at = sg; }
        assertNotNull(witch);
        float h = Renderer.actorHeight(witch.look, witch.c, new Art(), at);
        for (float t = Math.max(at.t0 + 0.1f, 230); t < Math.min(at.t1, 240); t += 1f / 24) {
            Film.Key k0 = witch.stateAt(t - 1f / 24), k1 = witch.stateAt(t);
            if (!k0.visible || !k1.visible || Director.offStage(Director.xAt(witch, t), h) || Director.offStage(Director.xAt(witch, t - 1f / 24), h)) continue;
            float jump = Math.abs(Director.xAt(witch, t) - Director.xAt(witch, t - 1f / 24));
            assertTrue("a jump of " + Math.round(jump) + " px at " + t, jump < 0.12f * h);
        }
        // the plan check says so too
        String src = new String(Files.readAllBytes(new File("../../app/src/main/assets/sample_story.txt").toPath()), StandardCharsets.UTF_8);
        Story story = ScriptParser.parse(src);
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film f2 = d.direct(new Art());
        ProductionDocument doc = ProductionDocument.read(story, null);
        doc.bind(story); doc.attach(f2); doc.validate();
        for (ProductionDocument.Issue i : doc.issues)
            assertFalse(i.evidence, i.requirement.equals("nobody jumps from place to place") && i.evidence.startsWith("आंटी चुड़ैल"));
    }

    /** The plan check finds a teleport when there is one: a key with no move puts someone far away mid-scene. */
    @Test public void thePlanCheckSeesATeleport() throws Exception {
        Film film = plan("tools/testdata/aakhiri_jodi/aakhiri_jodi_numbered.txt");
        Film.Seg sg = null;
        for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE) { sg = s; break; }
        Film.Actor a = sg.actors.get(0);
        float t = sg.t0 + (sg.t1 - sg.t0) / 2;
        Film.Key k = a.at(t);
        k.x = Director.xAt(a, t - 0.01f) > 640 ? 200 : 1100;
        k.moveDur = 0;
        String src = new String(Files.readAllBytes(new File("../../tools/testdata/aakhiri_jodi/aakhiri_jodi_numbered.txt").toPath()), StandardCharsets.UTF_8);
        Story story = film.story != null ? film.story : ScriptParser.parse(src);
        ProductionDocument doc = ProductionDocument.read(story, null);
        doc.bind(story); doc.attach(film); doc.validate();
        boolean seen = false;
        for (ProductionDocument.Issue i : doc.issues) if (i.requirement.equals("nobody jumps from place to place")) seen = true;
        assertTrue("a teleport is a physics finding", seen);
    }

    /** "Nikhil goes down the slide" ended on top of his mother before; walks now stop a body's width beside the still. */
    @Test public void aGirlRestsHerHeadOnTheOldMansShoulder() throws Exception {
        Film film = plan("tools/testdata/aakhiri_chitthi/aakhiri_chitthi_numbered.txt");
        boolean found = false;
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) {
            if (a.c == null || !a.c.displayName.equals("परी")) continue;
            for (Film.Act act : a.acts) {
                if (act.type == Film.G_HEAD_REST && act.target != null && act.target.c != null
                        && act.target.c.displayName.contains("रघुवीर")) found = true;
            }
        }
        assertTrue("the screenplay's head-on-shoulder direction becomes a targeted animation action", found);
    }

    @Test public void aWalkStopsBesideSomeoneStandingStill() throws Exception {
        Film film = plan("tools/testdata/aakhiri_chai/aakhiri_chai_numbered.txt");
        int arrivals = 0;
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) {
            if (a.look == null || !a.look.isHumanoid()) continue;
            float ha = Renderer.actorHeight(a.look, a.c, new Art(), sg);
            for (int i = 1; i < a.keys.size(); i++) {
                Film.Key k = a.keys.get(i);
                if (k.moveDur <= 0.2f || !k.visible || k.body != Pose.STAND || k.anchor != Film.A_GROUND) continue;
                float t1 = k.t + k.moveDur, x1 = Director.xAt(a, t1 + 0.02f);
                if (Math.abs(x1 - Director.xAt(a, k.t)) < ha * 0.3f || (i + 1 < a.keys.size() && a.keys.get(i + 1).t < t1 + 0.6f)) continue;
                arrivals++;
                for (Film.Actor o : sg.actors) {
                    if (o == a || o.look == null || !o.look.isHumanoid()) continue;
                    Film.Key ko = o.stateAt(t1 + 0.02f);
                    if (!ko.visible || ko.body != Pose.STAND || Math.abs(Director.xAt(o, t1 + 1) - Director.xAt(o, t1)) > 5 || Math.abs(Director.xAt(o, k.t) - Director.xAt(o, t1)) > 5) continue;
                    boolean touch = false;
                    for (Film.Act x : a.acts) if (x.target == o && x.t1 > k.t - 0.5f && x.t0 < t1 + 3) touch = true;
                    for (Film.Act x : o.acts) if (x.target == a && x.t1 > k.t - 0.5f && x.t0 < t1 + 3) touch = true;
                    if (touch) continue;
                    float ho = Renderer.actorHeight(o.look, o.c, new Art(), sg);
                    assertTrue(a.c.displayName + " stops " + Math.round(Math.abs(Director.xAt(o, t1) - x1)) + " px from " + o.c.displayName + " at " + t1,
                            Math.abs(Director.xAt(o, t1) - x1) >= 0.1f * (ha + ho) - 1);
                }
            }
        }
        assertTrue(arrivals > 0);
    }
}

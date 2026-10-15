package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * v46: the structured, machine-readable record the four production roles (Reader, Director, Technical Director,
 * Editor) exchange. Every finding is an {@link Issue} with a stable identifier, the thing it is about (story, scene,
 * shot, character, prop, location, dialogue line, asset, render attempt), a severity, evidence, a proposed correction,
 * dependencies, acceptance criteria and whether it is resolved. Review rounds are bounded ({@link #MAX_ROUNDS});
 * conflicts resolve deterministically:
 * <ul>
 * <li>a BLOCKER is closed only by the role that owns it (a technical blocker by the Technical Director, a missing
 *     essential asset by the Reader once the user supplied it) — the Director's creative authority never waives one;</li>
 * <li>the Director decides every creative (non-blocking) dispute: accept, adapt or reject with a reason;</li>
 * <li>a role never declares its own work validated while another role holds an unresolved issue in that role's
 *     responsibility ({@link #validated(Role)}).</li>
 * </ul>
 * The ledger serialises to JSON ({@link #toJson()} / {@link #fromJson(String)}) so it survives beside the film as an
 * audit trail. Pure Java: unit-tested on the desktop and run unchanged on the phone.
 */
public final class ReviewLedger {
    /** Review rounds never exceed this: the Editor may ask for one revision, the Technical Director one re-check. */
    public static final int MAX_ROUNDS = 3;

    public enum Role { READER, DIRECTOR, TECHNICAL_DIRECTOR, EDITOR }
    /** BLOCKER stops the render; MAJOR must be dispositioned by the owner; MINOR and NOTE are advisory. */
    public enum Severity { BLOCKER, MAJOR, MINOR, NOTE }

    public static final class Issue {
        public String id = "";
        public Role raisedBy = Role.READER;
        /** The role responsible for closing it. */
        public Role owner = Role.READER;
        /** story | scene | shot | character | prop | location | line | asset | render */
        public String refKind = "story";
        public String refId = "";
        public Severity severity = Severity.NOTE;
        public String finding = "", evidence = "", proposal = "", acceptance = "";
        public final List<String> dependsOn = new ArrayList<String>();
        public int round = 1;
        public boolean resolved;
        public Role resolvedBy;
        public String resolution = "";

        public String toString() {
            return id + " [" + severity + "/" + raisedBy + "→" + owner + "] " + refKind + ":" + refId + " — " + finding
                    + (resolved ? " ✔ " + resolvedBy + ": " + resolution : "");
        }
    }

    public String storyId = "";
    public int round = 1;
    public final List<Issue> issues = new ArrayList<Issue>();
    /** The audit trail, one line per event, in order. */
    public final List<String> log = new ArrayList<String>();
    private int seq;

    public ReviewLedger(String storyId) { this.storyId = storyId == null ? "" : storyId; }

    /** Opens an issue in the current round and returns it (its id is stable for the life of the ledger). */
    public Issue open(Role by, Role owner, String refKind, String refId, Severity sev, String finding, String evidence, String proposal, String acceptance) {
        Issue i = new Issue();
        i.id = String.format(Locale.US, "RV-%04d", ++seq);
        i.raisedBy = by; i.owner = owner; i.refKind = refKind == null ? "story" : refKind; i.refId = refId == null ? "" : refId;
        i.severity = sev; i.finding = s(finding); i.evidence = s(evidence); i.proposal = s(proposal); i.acceptance = s(acceptance);
        i.round = round;
        issues.add(i);
        log.add("r" + round + " " + by + " opened " + i.id + " " + sev + " " + i.refKind + ":" + i.refId + " — " + i.finding);
        return i;
    }

    public Issue get(String id) { for (Issue i : issues) if (i.id.equals(id)) return i; return null; }

    /**
     * Resolves an issue. Returns false (and logs the refusal) when the role may not close it: a BLOCKER only by its
     * owner; a MAJOR by its owner or by the Director as a creative decision; MINOR/NOTE by any role; and never while
     * an issue it depends on is still open.
     */
    public boolean resolve(String id, Role by, String note) {
        Issue i = get(id);
        if (i == null || i.resolved) return false;
        for (String d : i.dependsOn) { Issue dep = get(d); if (dep != null && !dep.resolved) { log.add("r" + round + " " + by + " cannot close " + id + ": depends on open " + d); return false; } }
        boolean allowed = i.severity == Severity.BLOCKER ? by == i.owner
                : i.severity == Severity.MAJOR ? (by == i.owner || by == Role.DIRECTOR)
                : true;
        if (!allowed) { log.add("r" + round + " " + by + " cannot close " + id + " (" + i.severity + " owned by " + i.owner + ")"); return false; }
        i.resolved = true; i.resolvedBy = by; i.resolution = s(note);
        log.add("r" + round + " " + by + " resolved " + id + ": " + i.resolution);
        return true;
    }

    /** The Director's disposition of someone else's finding: accept (closes it), adapt (closes it with the change) or reject (closes a non-blocker with the reason; a BLOCKER stays open). */
    public boolean decide(String id, String decision, String reason) {
        Issue i = get(id);
        if (i == null) return false;
        String d = decision == null ? "" : decision.trim().toLowerCase(Locale.ROOT);
        if (i.severity == Severity.BLOCKER) { log.add("r" + round + " DIRECTOR " + d + " " + id + " noted — a BLOCKER stays with " + i.owner + ": " + s(reason)); return false; }
        return resolve(id, Role.DIRECTOR, d + ": " + s(reason));
    }

    /** Advances the round; false (and no change) once {@link #MAX_ROUNDS} is reached. */
    public boolean nextRound() {
        if (round >= MAX_ROUNDS) { log.add("round limit " + MAX_ROUNDS + " reached"); return false; }
        round++;
        log.add("round " + round + " begins");
        return true;
    }

    public List<Issue> open() { List<Issue> o = new ArrayList<Issue>(); for (Issue i : issues) if (!i.resolved) o.add(i); return o; }
    public List<Issue> open(Role owner) { List<Issue> o = new ArrayList<Issue>(); for (Issue i : issues) if (!i.resolved && i.owner == owner) o.add(i); return o; }
    public List<Issue> open(Severity sev) { List<Issue> o = new ArrayList<Issue>(); for (Issue i : issues) if (!i.resolved && i.severity == sev) o.add(i); return o; }

    /** No unresolved BLOCKER anywhere. */
    public boolean readyToRender() { return open(Severity.BLOCKER).isEmpty(); }

    /** A role's work counts as validated only when nothing in its responsibility (BLOCKER or MAJOR) is still open, whoever raised it. */
    public boolean validated(Role role) {
        for (Issue i : issues) if (!i.resolved && i.owner == role && (i.severity == Severity.BLOCKER || i.severity == Severity.MAJOR)) return false;
        return true;
    }

    public String summary() {
        int b = open(Severity.BLOCKER).size(), m = open(Severity.MAJOR).size(), n = open().size();
        StringBuilder sb = new StringBuilder();
        sb.append("REVIEW LEDGER ").append(storyId).append(": ").append(issues.size()).append(" issues, ").append(n).append(" open (")
          .append(b).append(" blocking, ").append(m).append(" major), rounds ").append(round).append('/').append(MAX_ROUNDS)
          .append(", ready to render: ").append(readyToRender() ? "yes" : "NO").append('\n');
        for (Role r : Role.values()) sb.append("  ").append(r).append(": ").append(validated(r) ? "validated" : open(r).size() + " open in its responsibility").append('\n');
        for (Issue i : issues) sb.append("  ").append(i).append('\n');
        return sb.toString();
    }

    // ------------------------------------------------------------------ JSON

    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schema", "kahani-review-ledger/1");
        root.put("story", storyId);
        root.put("round", round);
        root.put("max_rounds", MAX_ROUNDS);
        root.put("ready_to_render", readyToRender());
        List<Object> list = new ArrayList<Object>();
        for (Issue i : issues) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", i.id); m.put("raised_by", i.raisedBy.name()); m.put("owner", i.owner.name());
            m.put("ref_kind", i.refKind); m.put("ref_id", i.refId); m.put("severity", i.severity.name());
            m.put("finding", i.finding); m.put("evidence", i.evidence); m.put("proposal", i.proposal); m.put("acceptance", i.acceptance);
            m.put("depends_on", new ArrayList<Object>(i.dependsOn)); m.put("round", i.round);
            m.put("resolved", i.resolved); m.put("resolved_by", i.resolvedBy == null ? null : i.resolvedBy.name()); m.put("resolution", i.resolution);
            list.add(m);
        }
        root.put("issues", list);
        root.put("log", new ArrayList<Object>(log));
        return Json.write(root);
    }

    public static ReviewLedger fromJson(String json) {
        Object o = Json.parseLoose(json);
        ReviewLedger l = new ReviewLedger(Json.str(o, "story", ""));
        if (!(o instanceof Map)) return l;
        l.round = (int) Json.num(o, "round", 1);
        for (Object x : Json.arr(o, "issues")) {
            Issue i = new Issue();
            i.id = Json.str(x, "id", ""); i.raisedBy = role(Json.str(x, "raised_by", "READER")); i.owner = role(Json.str(x, "owner", "READER"));
            i.refKind = Json.str(x, "ref_kind", "story"); i.refId = Json.str(x, "ref_id", "");
            try { i.severity = Severity.valueOf(Json.str(x, "severity", "NOTE")); } catch (IllegalArgumentException e) { i.severity = Severity.NOTE; }
            i.finding = Json.str(x, "finding", ""); i.evidence = Json.str(x, "evidence", ""); i.proposal = Json.str(x, "proposal", ""); i.acceptance = Json.str(x, "acceptance", "");
            for (Object d : Json.arr(x, "depends_on")) i.dependsOn.add(String.valueOf(d));
            i.round = (int) Json.num(x, "round", 1); i.resolved = Json.bool(x, "resolved", false);
            String rb = Json.str(x, "resolved_by", ""); i.resolvedBy = rb.length() == 0 || "null".equals(rb) ? null : role(rb);
            i.resolution = Json.str(x, "resolution", "");
            l.issues.add(i);
            try { int n = Integer.parseInt(i.id.substring(i.id.indexOf('-') + 1)); l.seq = Math.max(l.seq, n); } catch (RuntimeException ignored) { }
        }
        for (Object s : Json.arr(o, "log")) l.log.add(String.valueOf(s));
        return l;
    }

    private static Role role(String s) { try { return Role.valueOf(s); } catch (IllegalArgumentException e) { return Role.READER; } }
    private static String s(String s) { return s == null ? "" : s.trim(); }
}

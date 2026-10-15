package com.tarun.kahani.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Independent per-shot Reader, Technical Director, Editor and final Director visual conference. */
public final class ShotVisualConference {
    private ShotVisualConference() {}
    public static final int CONTRACT_VERSION = 2;

    public static final class Review {
        public int shotNumber;
        public boolean valid, readyForRender, offlineFallback;
        public String readerStatus="NEEDS_HUMAN_REVIEW", readerNotes="", readerEvidence="";
        public String editorStatus="NEEDS_HUMAN_REVIEW", editorNotes="", editorEvidence="";
        public String technicalStatus="NEEDS_HUMAN_REVIEW", technicalNotes="", technicalEvidence="";
        public String directorDecision="HOLD", directorResolution="", visualEvidence="", directorEvidence="";
        public String reportJson="", error="";
        public final List<String> hardBlockers=new ArrayList<String>();
        public final List<String> directorDispositionedBlockers=new ArrayList<String>();
        public final Map<String,String> directorDispositions=new LinkedHashMap<String,String>();

        public String summary() {
            StringBuilder b=new StringBuilder();
            b.append("Reader ").append(readerStatus).append("; Editor ").append(editorStatus)
             .append("; Technical Director ").append(technicalStatus).append("; Director ").append(directorDecision).append(".");
            if (!directorResolution.isEmpty()) b.append(" Resolution: ").append(directorResolution);
            if (!technicalNotes.isEmpty()) b.append(" Technical: ").append(technicalNotes);
            if (!hardBlockers.isEmpty()) b.append(" Blockers: ").append(hardBlockers);
            if (!error.isEmpty()) b.append(" Review status: ").append(error);
            if (offlineFallback) b.append(" OFFLINE INTEGRITY-ONLY; no semantic or identity review.");
            return b.toString();
        }
    }

    /** Each specialist receives the same rendered frame in a separate call; the Director inspects it again last. */
    public static Review inspect(Cloud cloud, int shotNumber, String shotDescription,
                                 String continuityContext, byte[] jpeg) throws IOException {
        if (shotNumber < 1) throw new IllegalArgumentException("shotNumber must be positive");
        if (cloud==null || jpeg==null || jpeg.length==0) {
            Review r=new Review(); r.shotNumber=shotNumber; r.error="Rendered frame or online vision service unavailable"; return r;
        }
        Map<String,Object> reader=roleCall(cloud, "STORY READER", "Check visible narrative beat, emotional legibility, causal action and whether required characters, props and locations are visibly present. Do not invent missing visual evidence.", shotNumber, shotDescription, continuityContext, jpeg, "PASS|REVISE|NEEDS_HUMAN_REVIEW");
        Map<String,Object> technical=roleCall(cloud, "TECHNICAL DIRECTOR", "Check visible identity/reference match when references are supplied, mesh/rig deformation, anatomy, clipping, feet-ground contact, prop contact, lighting continuity, frame integrity and physical plausibility. List every objective blocker.", shotNumber, shotDescription, continuityContext, jpeg, "PASS|FAIL|NEEDS_HUMAN_REVIEW");
        Map<String,Object> editor=roleCall(cloud, "FILM EDITOR", "Check framing, action readability, visual rhythm, useful coverage, screen direction and continuity with the previous and next shot. Identify redundancy or missing reaction coverage.", shotNumber, shotDescription, continuityContext, jpeg, "PASS|REVISE|NEEDS_HUMAN_REVIEW");

        String directorPrompt="You are the DIRECTOR and final authority. Independently inspect the same attached rendered JPEG, then consider the separate Reader, Technical Director and Editor reports below. Resolve every disagreement explicitly by role. You may overrule any role, including the Technical Director, only with concrete visual evidence and a reason; if overriding a technical blocker, copy that blocker verbatim into director_dispositioned_blockers and explain why it is not a real defect in this image. Do not dismiss an actual visible defect without evidence. Do not infer unseen motion or lip-sync from a still. Return ONLY JSON with fields: shot_number:number, director_decision:APPROVE|REVISE|HOLD, director_resolution:string, director_evidence:string, director_dispositions:[{role:reader|technical_director|editor,decision:accept|override|revise,reason:string}], director_dispositioned_blockers:[string], visual_evidence:string. Evidence must describe actual pixels, not generic praise. Approval requires all findings resolved; use HOLD if uncertain.\nSHOT NUMBER: "+shotNumber+"\nSHOT INTENT: "+limit(text(shotDescription),1200)+"\nCONTINUITY: "+limit(text(continuityContext),2400)+"\nREADER REPORT: "+Json.write(reader)+"\nTECHNICAL DIRECTOR REPORT: "+Json.write(technical)+"\nEDITOR REPORT: "+Json.write(editor);
        Object rawDirector=Cloud.jsonIn(cloud.vision(directorPrompt,jpeg));
        Map<String,Object> director=asMap(rawDirector);
        Map<String,Object> aggregate=new LinkedHashMap<String,Object>();
        aggregate.put("contract_version",CONTRACT_VERSION); aggregate.put("shot_number",shotNumber);
        aggregate.put("reader_status",reader.get("status")); aggregate.put("reader_notes",reader.get("notes")); aggregate.put("reader_evidence",reader.get("evidence"));
        aggregate.put("editor_status",editor.get("status")); aggregate.put("editor_notes",editor.get("notes")); aggregate.put("editor_evidence",editor.get("evidence"));
        aggregate.put("technical_director_status",technical.get("status")); aggregate.put("technical_director_notes",technical.get("notes")); aggregate.put("technical_director_evidence",technical.get("evidence"));
        aggregate.put("hard_blockers",technical.get("hard_blockers"));
        aggregate.put("director_decision",director.get("director_decision")); aggregate.put("director_resolution",director.get("director_resolution"));
        aggregate.put("director_evidence",director.get("director_evidence")); aggregate.put("visual_evidence",director.get("visual_evidence"));
        aggregate.put("director_dispositions",director.get("director_dispositions"));
        aggregate.put("director_dispositioned_blockers",director.get("director_dispositioned_blockers"));
        return parse(Json.write(aggregate),shotNumber);
    }

    private static Map<String,Object> roleCall(Cloud cloud,String role,String task,int shot,String desc,String context,byte[] jpeg,String allowed) throws IOException {
        String prompt="You are the independent "+role+". Inspect only the attached rendered JPEG. "+task
                +" Return ONLY JSON: {shot_number:number,status:"+allowed+",notes:string,evidence:string,findings:[string],required_fixes:[string],hard_blockers:[string]}. "
                +"Evidence must identify observable pixels; if the frame cannot establish a fact, say so and use NEEDS_HUMAN_REVIEW. Never fabricate approval.\nSHOT NUMBER: "+shot
                +"\nSHOT INTENT: "+limit(text(desc),1200)+"\nCONTINUITY CONTEXT: "+limit(text(context),2200);
        return asMap(Cloud.jsonIn(cloud.vision(prompt,jpeg)));
    }

    /** Pure validator; all role evidence and explicit Director dispositions are required. */
    public static Review parse(String raw,int expectedShot) {
        Review r=new Review(); r.shotNumber=expectedShot; r.reportJson=raw==null?"":raw;
        Object parsed=Cloud.jsonIn(raw);
        if (!(parsed instanceof Map)) { r.error="Invalid JSON from visual conference"; return r; }
        Map<?,?> m=(Map<?,?>)parsed;
        Object n=m.get("shot_number");
        if (!(n instanceof Number) || ((Number)n).intValue()!=expectedShot) { r.error="Shot number missing or mismatched"; return r; }
        r.readerStatus=status(m.get("reader_status")); r.readerNotes=text(m.get("reader_notes")); r.readerEvidence=text(m.get("reader_evidence"));
        r.editorStatus=status(m.get("editor_status")); r.editorNotes=text(m.get("editor_notes")); r.editorEvidence=text(m.get("editor_evidence"));
        r.technicalStatus=technicalStatus(m.get("technical_director_status")); r.technicalNotes=text(m.get("technical_director_notes")); r.technicalEvidence=text(m.get("technical_director_evidence"));
        r.directorDecision=decision(m.get("director_decision")); r.directorResolution=text(m.get("director_resolution"));
        r.directorEvidence=text(m.get("director_evidence")); r.visualEvidence=text(m.get("visual_evidence"));
        addStrings(r.hardBlockers,m.get("hard_blockers")); addStrings(r.directorDispositionedBlockers,m.get("director_dispositioned_blockers"));
        Object ds=m.get("director_dispositions");
        if(ds instanceof List) for(Object o:(List<?>)ds) if(o instanceof Map) {
            Map<?,?> d=(Map<?,?>)o; String role=text(d.get("role")).toLowerCase(java.util.Locale.ROOT);
            String dec=text(d.get("decision")).toLowerCase(java.util.Locale.ROOT), reason=text(d.get("reason"));
            if(("reader".equals(role)||"editor".equals(role)||"technical_director".equals(role))
                    && ("accept".equals(dec)||"override".equals(dec)||"revise".equals(dec)) && reason.length()>=8)
                r.directorDispositions.put(role,dec+":"+reason);
        }
        boolean statusesKnown=knownReaderEditor(r.readerStatus)&&knownReaderEditor(r.editorStatus)
                &&knownTechnical(r.technicalStatus)&&knownDecision(r.directorDecision);
        r.valid=statusesKnown && r.readerEvidence.length()>=8 && r.editorEvidence.length()>=8
                && r.technicalEvidence.length()>=8 && r.directorEvidence.length()>=8
                && r.visualEvidence.length()>=8 && r.directorResolution.length()>=8;
        boolean readerResolved="PASS".equals(r.readerStatus)||r.directorDispositions.containsKey("reader");
        boolean editorResolved="PASS".equals(r.editorStatus)||r.directorDispositions.containsKey("editor");
        boolean techResolved="PASS".equals(r.technicalStatus)||r.directorDispositions.containsKey("technical_director");
        boolean blockersResolved=true;
        for(String blocker:r.hardBlockers) if(!containsExact(r.directorDispositionedBlockers,blocker)) blockersResolved=false;
        boolean noHumanFallback=!"NEEDS_HUMAN_REVIEW".equals(r.readerStatus)&&!"NEEDS_HUMAN_REVIEW".equals(r.editorStatus)
                &&!"NEEDS_HUMAN_REVIEW".equals(r.technicalStatus);
        r.readyForRender=r.valid && noHumanFallback && "APPROVE".equals(r.directorDecision)
                && readerResolved && editorResolved && techResolved && blockersResolved;
        if(!r.valid && r.error.isEmpty()) r.error="Required role evidence, status or Director resolution is missing/invalid";
        else if(!r.readyForRender && r.error.isEmpty()) r.error="One or more role findings remain unresolved, or the Director did not approve";
        return r;
    }

    /** Offline fallback is deliberately labelled as a technical integrity gate, not an AI conference. */
    public static Review inspectLocal(int shotNumber,String description,int width,int height,int[] pixels) {
        Review r=new Review(); r.shotNumber=shotNumber; r.offlineFallback=true;
        if(pixels==null||width<2||height<2||(long)width*height>pixels.length) { r.error="Invalid offline pixel buffer"; return r; }
        long sum=0,sumSq=0,visible=0,n=0; int stride=Math.max(1,(width*height)/4096);
        for(int i=0;i<width*height;i+=stride) {
            int c=pixels[i]; int y=(int)(0.2126*((c>>16)&255)+0.7152*((c>>8)&255)+0.0722*(c&255));
            sum+=y; sumSq+=(long)y*y; if(y>12)visible++; n++;
        }
        double mean=n==0?0:sum/(double)n, variance=n==0?0:sumSq/(double)n-mean*mean, nonBlack=n==0?0:visible/(double)n;
        boolean ok=n>0&&variance>=4.0&&nonBlack>=0.005;
        r.valid=ok; r.readyForRender=ok; r.readerStatus="OFFLINE_NOT_RUN"; r.editorStatus="OFFLINE_NOT_RUN";
        r.technicalStatus=ok?"PASS":"FAIL"; r.directorDecision=ok?"OFFLINE_INTEGRITY_PASS":"HOLD";
        r.visualEvidence="sampled luminance variance="+variance+", non-black fraction="+nonBlack;
        r.directorEvidence=r.visualEvidence; r.error="OFFLINE LIMITATION: semantic, acting and identity review unavailable.";
        Map<String,Object> m=new LinkedHashMap<String,Object>();
        m.put("contract_version",CONTRACT_VERSION);m.put("shot_number",shotNumber);m.put("mode","offline_deterministic_integrity_only");
        m.put("shot_description",description==null?"":description);m.put("width",width);m.put("height",height);
        m.put("sampled_luminance_variance",variance);m.put("non_black_fraction",nonBlack);m.put("semantic_review",false);
        m.put("integrity_pass",ok);m.put("limitation",r.error);r.reportJson=Json.write(m);
        return r;
    }

    public static String prompt(int shotNumber,String shotDescription,String continuityContext) {
        return "Director final-decision contract v"+CONTRACT_VERSION+" for shot "+shotNumber+". Inspect the supplied frame and all separate role reports. The Director has final authority but must explicitly disposition every disagreement and blocker with pixel-grounded evidence. Intent: "+limit(text(shotDescription),1200)+" Context: "+limit(text(continuityContext),2200);
    }
    private static Map<String,Object> asMap(Object o) {
        Map<String,Object> out=new LinkedHashMap<String,Object>();
        if(o instanceof Map) for(Map.Entry<?,?> e:((Map<?,?>)o).entrySet()) if(e.getKey() instanceof String) out.put((String)e.getKey(),e.getValue());
        return out;
    }
    private static void addStrings(List<String> out,Object o) { if(o instanceof List) for(Object v:(List<?>)o) {String s=text(v);if(!s.isEmpty())out.add(s);} }
    private static boolean containsExact(List<String> values,String target) { for(String s:values)if(s.trim().equalsIgnoreCase(target.trim()))return true;return false; }
    private static String status(Object o) {
        String s=text(o).toUpperCase(java.util.Locale.ROOT).replace(' ','_');
        return "PASS".equals(s)||"REVISE".equals(s)||"NEEDS_HUMAN_REVIEW".equals(s)?s:"NEEDS_HUMAN_REVIEW";
    }
    private static String technicalStatus(Object o) {
        String s=text(o).toUpperCase(java.util.Locale.ROOT).replace(' ','_');
        return "PASS".equals(s)||"FAIL".equals(s)||"NEEDS_HUMAN_REVIEW".equals(s)?s:"NEEDS_HUMAN_REVIEW";
    }
    private static String decision(Object o) {
        String s=text(o).toUpperCase(java.util.Locale.ROOT);
        return "APPROVE".equals(s)||"REVISE".equals(s)||"HOLD".equals(s)?s:"HOLD";
    }
    private static boolean knownReaderEditor(String s) { return "PASS".equals(s)||"REVISE".equals(s)||"NEEDS_HUMAN_REVIEW".equals(s); }
    private static boolean knownTechnical(String s) { return "PASS".equals(s)||"FAIL".equals(s)||"NEEDS_HUMAN_REVIEW".equals(s); }
    private static boolean knownDecision(String s) { return "APPROVE".equals(s)||"REVISE".equals(s)||"HOLD".equals(s); }
    private static String text(Object o) { return o==null?"":String.valueOf(o).trim(); }
    private static String limit(String s,int max) { return s.length()<=max?s:s.substring(0,max)+"…"; }
}

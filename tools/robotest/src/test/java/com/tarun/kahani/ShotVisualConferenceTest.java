package com.tarun.kahani;

import static org.junit.Assert.*;
import com.tarun.kahani.core.ShotVisualConference;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class ShotVisualConferenceTest {
    private String report(String tdStatus, boolean disposition, boolean blockersDispositioned) {
        Map<String,Object> root=new LinkedHashMap<String,Object>();
        root.put("contract_version",ShotVisualConference.CONTRACT_VERSION); root.put("shot_number",1);
        root.put("reader_status","PASS"); root.put("reader_notes","The intended action reads clearly.");
        root.put("reader_evidence","A character reaches toward the visible cup.");
        root.put("editor_status","PASS"); root.put("editor_notes","The framing supports the next cut.");
        root.put("editor_evidence","The cup and character remain inside frame.");
        root.put("technical_director_status",tdStatus); root.put("technical_director_notes","The contact shadow is visible.");
        root.put("technical_director_evidence","Feet meet the floor line and the cup touches the table.");
        root.put("director_decision","APPROVE"); root.put("director_resolution","I inspected the same pixels and resolved the disagreement.");
        root.put("director_evidence","The hand, cup, table and contact shadow are visible.");
        java.util.List<Object> blockers=new java.util.ArrayList<Object>();
        if ("FAIL".equals(tdStatus)) blockers.add("Cup appears to float above the table");
        root.put("hard_blockers",blockers);
        java.util.List<Object> disp=new java.util.ArrayList<Object>();
        if(disposition) { Map<String,Object> d=new LinkedHashMap<String,Object>(); d.put("role","technical_director");d.put("decision","override");d.put("reason","The cup visibly touches the table in the reviewed frame.");disp.add(d); }
        root.put("director_dispositions",disp);
        java.util.List<Object> db=new java.util.ArrayList<Object>();
        if(blockersDispositioned) db.add("Cup appears to float above the table");
        root.put("director_dispositioned_blockers",db);
        return com.tarun.kahani.core.Json.write(root);
    }
    @Test public void allRolesAndDirectorSignoffRequired() {
        assertTrue(ShotVisualConference.parse(report("PASS",false,false),1).readyForRender);
    }
    @Test public void directorCanResolveTechnicalDisputeOnlyWithExplicitDisposition() {
        assertFalse(ShotVisualConference.parse(report("FAIL",false,false),1).readyForRender);
        assertTrue(ShotVisualConference.parse(report("FAIL",true,true),1).readyForRender);
    }
    @Test public void wrongShotMissingEvidenceAndHumanFallbackFailClosed() {
        assertFalse(ShotVisualConference.parse(report("PASS",false,false),2).readyForRender);
        String r=report("PASS",false,false).replace("The hand, cup, table and contact shadow are visible.","");
        assertFalse(ShotVisualConference.parse(r,1).readyForRender);
        assertFalse(ShotVisualConference.parse(report("NEEDS_HUMAN_REVIEW",false,false),1).readyForRender);
    }
    @Test public void offlineGateRejectsBlankAndAcceptsTexturedFrames() {
        int[] blank=new int[64*64]; java.util.Arrays.fill(blank,0xff000000);
        assertFalse(ShotVisualConference.inspectLocal(1,"blank",64,64,blank).readyForRender);
        int[] varied=new int[64*64];
        for(int y=0;y<64;y++)for(int x=0;x<64;x++){int v=((x/8+y/8)%2==0)?230:24;varied[y*64+x]=0xff000000|(v<<16)|(v<<8)|v;}
        ShotVisualConference.Review r=ShotVisualConference.inspectLocal(1,"checker",64,64,varied);
        assertTrue(r.readyForRender); assertTrue(r.offlineFallback); assertTrue(r.reportJson.contains("semantic_review"));
    }
}

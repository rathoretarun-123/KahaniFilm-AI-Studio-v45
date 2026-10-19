package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Pose;

import org.junit.Test;

/**
 * v47.12 (a new story made end to end): the feeling a line is played with, read from its manner. "(उदास होकर)" was
 * read as no feeling at all — the sad line was played calm, without a tear; worry, annoyance and plain surprise too.
 */
public class MannerFeelingsTest {
    static int of(String manner) { return Director.emotionOf(manner, "कुछ बात", null); }

    @Test public void plainWordsOfFeeling() {
        assertEquals(Pose.SAD, of("उदास होकर"));
        assertEquals(Pose.SAD, of("निराश स्वर में"));
        assertEquals(Pose.SAD, of("मायूस होकर, सिर झुकाए"));
        assertEquals(Pose.SAD, of("sadly"));
        assertEquals(Pose.SCARED, of("चिंतित होकर"));
        assertEquals(Pose.SCARED, of("worried"));
        assertEquals(Pose.ANGRY, of("नाराज़ होकर"));
        assertEquals(Pose.SURPRISED, of("shocked"));
        assertEquals(Pose.HAPPY, of("उत्साह से"));
        assertEquals(Pose.HAPPY, of("happily"));
        assertEquals("as before: crying is sad", Pose.SAD, of("रोते हुए"));
        assertEquals("as before: a smile is glad", Pose.HAPPY, of("मुस्कुराते हुए"));
        assertEquals("a look across the room is no anger", Pose.NEUTRAL, of("looking across the room"));
        assertEquals("no feeling named, none played", Pose.NEUTRAL, of("धीमी चाल से आगे बढ़ते हुए"));
    }
}

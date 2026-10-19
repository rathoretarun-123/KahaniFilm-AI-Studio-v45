package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Sets;

import org.junit.Test;

/**
 * v47.12 (a new story, "नीम के नीचे", made end to end): a park of today is a garden without the palace on the far hill
 * (the palace stood behind its title, its scenes and its end); a palace's garden keeps it.
 */
public class ParkSetTest {
    @Test public void aParkIsNoPalaceGarden() {
        assertEquals(Sets.GARDEN, Sets.detect("पार्क (नीम का पेड़, लकड़ी की बेंच)"));
        assertEquals(Sets.D_PARK, Sets.decorFor("पार्क (नीम का पेड़, लकड़ी की बेंच)"));
        assertEquals(Sets.D_PARK, Sets.decorFor("the city park, a playground"));
        assertNotEquals("a palace's garden keeps its palace", Sets.D_PARK, Sets.decorFor("महल का उद्यान"));
        assertNotEquals("a garden of a fairy tale is not a park", Sets.D_PARK, Sets.decorFor("राजा का बगीचा"));
    }
}

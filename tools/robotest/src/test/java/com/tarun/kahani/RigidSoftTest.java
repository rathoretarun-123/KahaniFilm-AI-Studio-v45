package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Rigidity;
import org.junit.Test;

/**
 * v47.13 (the user: "check rigid body objects and non rigid body objects are meshed properly, some objects can be
 * both"): what keeps its shape, what bends with the body and the wind, and what is rigid in its parts but opens.
 */
public class RigidSoftTest {
    @Test public void aLongerWordIsTheThing() {
        assertEquals("a bucket is rigid (not hair)", Rigidity.RIGID, Rigidity.kind("पानी की बाल्टी"));
        assertEquals("a fan is rigid (not a feather)", Rigidity.RIGID, Rigidity.kind("पंखा"));
        assertEquals("hair bends", Rigidity.SOFT, Rigidity.kind("बाल"));
        assertEquals("a feather bends", Rigidity.SOFT, Rigidity.kind("मोर का पंख"));
        assertEquals("cloth bends (not a cup)", Rigidity.SOFT, Rigidity.kind("कपड़ा"));
        assertEquals("a cup keeps its shape", Rigidity.RIGID, Rigidity.kind("कप"));
        assertEquals("a wooden handle keeps its shape", Rigidity.RIGID, Rigidity.kind("झंडे का डंडा"));
    }

    @Test public void heldThingsKeepTheirShapeAndClothBends() {
        for (String r : new String[]{"छड़ी", "लाठी", "बेंच", "थाली", "केतली", "walking stick", "plate", "bicycle"})
            assertEquals(r + " keeps its shape", Rigidity.RIGID, Rigidity.kind(r));
        for (String f : new String[]{"दुपट्टा", "साड़ी", "रस्सी", "rope", "saree", "flag"})
            assertNotEquals(f + " is not rigid", Rigidity.RIGID, Rigidity.kind(f));
    }

    @Test public void someThingsAreBoth() {
        // rigid in their parts, they open at a hinge, a fold or a spring
        assertTrue(Rigidity.isRigid("छाता") && Rigidity.opens("छाता"));
        assertEquals(Rigidity.HINGE_CANOPY, Rigidity.hinge("छाता"));
        assertTrue(Rigidity.isRigid("किताब") && Rigidity.opens("किताब"));
        assertEquals(Rigidity.HINGE_LEAVES, Rigidity.hinge("किताब"));
        assertEquals(Rigidity.HINGE_LID, Rigidity.hinge("टिफ़िन"));
        // paper: it opens, and as a material it bends
        assertTrue(Rigidity.opens("चिट्ठी"));
        assertEquals(Rigidity.FLEXIBLE, Rigidity.material("पुरानी चिट्ठी"));
        assertEquals(Rigidity.FLEXIBLE, Rigidity.material("स्कूल का बैग"));
        assertEquals(Rigidity.RIGID, Rigidity.material("पानी की बाल्टी"));
    }
}

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

    /** v47.13 (the user: "some objects may be rigid in one scene and non rigid in another, like chitthi, book"). */
    @Test public void theSceneDecidesForALetterOrABook() {
        assertEquals("a letter fluttering in the wind bends", Rigidity.SOFT, Rigidity.kindIn("चिट्ठी", "हवा में चिट्ठी फड़फड़ाती है"));
        assertEquals("a crumpled letter bends", Rigidity.SOFT, Rigidity.kindIn("letter", "she holds the crumpled letter"));
        assertEquals("a framed letter keeps its shape", Rigidity.RIGID, Rigidity.kindIn("चिट्ठी", "दीवार पर फ्रेम में चिट्ठी"));
        assertEquals("a book whose pages flutter bends", Rigidity.SOFT, Rigidity.kindIn("किताब", "किताब के पन्ने हवा में फड़फड़ाते हैं"));
        assertEquals("a book in the lap: by its name", Rigidity.kind("किताब"), Rigidity.kindIn("किताब", "गोद में किताब रखी है"));
        assertEquals("he turned (मुड़ा) is not a folded letter", Rigidity.kind("चिट्ठी"), Rigidity.kindIn("चिट्ठी", "वह मुड़ा और चिट्ठी पकड़ी"));
        assertEquals("a cup never bends", Rigidity.RIGID, Rigidity.kindIn("कप", "हवा में कप फड़फड़ाता है"));
        assertEquals("no scene: by its name", Rigidity.kind("चिट्ठी"), Rigidity.kindIn("चिट्ठी", null));
    }
}

package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * v47.3: what bends and what does not. The reader, the director and the technical director share one list of the
 * things a story can hold: a RIGID thing (a walking stick, an umbrella, a sword, a cricket bat, a plate, a bicycle) keeps
 * its shape — it is lifted out of a character's picture where it can be found (Art.findRod) and drawn straight from the
 * hand, and it never sways in the wind or folds with the body; a SOFT thing (a saree, a dupatta, a sheet, a rope, hair,
 * a flag) moves with the wind and the body through the rig's mesh. Anything not listed is treated by its words
 * ("…की लकड़ी", "…of wood", "…का कपड़ा", "…cloth") and otherwise as unknown (drawn as it comes). The user's own sample
 * pictures of such things (the studio's reference frames) are measured against the film by the quality check.
 */
public final class Rigidity {
    private Rigidity() {}

    public static final int UNKNOWN = 0, RIGID = 1, SOFT = 2;

    /** Things that keep their shape (Hindi, Hinglish and English words, where a word begins). */
    public static final String[] RIGID_WORDS = {
            // held: sticks, tools, weapons, instruments
            "छड़ी", "लाठी", "लठ", "डंडा", "डंडे", "बैसाखी", "छाता", "छतरी", "तलवार", "भाला", "बरछी", "धनुष", "तीर", "कुल्हाड़ी", "हथौड़ा", "हथौड़ी", "हँसिया", "हंसिया", "दराँती",
            "फावड़ा", "कुदाल", "झाड़ू", "बाँसुरी", "बांसुरी", "ढोल", "ढोलक", "तबला", "हारमोनियम", "सितार", "वीणा", "बल्ला", "गेंद", "हॉकी", "रैकेट", "बंदूक", "पिस्तौल", "राइफल", "चाकू", "छुरी",
            "कटार", "त्रिशूल", "गदा", "चाबुक", "लालटेन", "लैंप", "टॉर्च", "मशाल", "कलम", "पेंसिल", "स्लेट", "किताब", "पुस्तक", "कॉपी", "अख़बार", "अखबार", "चिट्ठी", "ख़त", "खत", "पत्र", "लिफ़ाफ़ा",
            "फ़ोन", "फोन", "मोबाइल", "घड़ी", "चश्मा", "चाबी", "ताला", "सिक्का", "सिक्के", "आईना", "शीशा", "कंघी", "कंघा",
            "stick", "cane", "crutch", "staff", "rod", "pole", "umbrella", "sword", "spear", "bow", "arrow", "axe", "hammer", "sickle", "spade", "shovel", "broom", "flute", "drum", "dhol", "tabla",
            "harmonium", "sitar", "bat", "ball", "hockey", "racket", "gun", "pistol", "rifle", "knife", "dagger", "trident", "mace", "whip", "lantern", "lamp", "torch", "pen", "pencil", "slate",
            "book", "notebook", "newspaper", "letter", "envelope", "phone", "mobile", "watch", "clock", "glasses", "spectacles", "key", "lock", "coin", "mirror", "comb",
            // vessels and the kitchen
            "थाली", "कटोरी", "कटोरा", "गिलास", "ग्लास", "कप", "प्याली", "प्याला", "केतली", "बोतल", "बाल्टी", "घड़ा", "मटका", "लोटा", "सुराही", "कलश", "डिब्बा", "डब्बा", "टिफ़िन", "टिफिन", "तवा", "कड़ाही",
            "कढ़ाई", "पतीला", "चम्मच", "करछी", "बेलन", "चकला", "चूल्हा", "सिल", "बट्टा",
            "plate", "bowl", "glass", "cup", "kettle", "bottle", "bucket", "pot", "pitcher", "jar", "box", "tin", "tiffin", "pan", "wok", "spoon", "ladle", "rolling pin", "stove",
            // furniture and the house
            "कुर्सी", "मेज़", "मेज", "टेबल", "पलंग", "खाट", "चारपाई", "बिस्तर", "बेंच", "स्टूल", "चौकी", "आलमारी", "अलमारी", "संदूक", "सन्दूक", "बक्सा", "सूटकेस", "ट्रंक", "सीढ़ी", "दरवाज़ा", "दरवाजा",
            "खिड़की", "दीवार", "छत", "खंभा", "खम्भा", "पंखा", "रेडियो", "टीवी", "टेलीविज़न", "घंटी", "घंटा", "तराज़ू", "तराजू",
            "chair", "table", "desk", "bed", "cot", "bench", "stool", "cupboard", "wardrobe", "chest", "trunk", "suitcase", "ladder", "door", "window", "wall", "pillar", "fan", "radio",
            "television", "bell", "scales",
            // things that roll
            "साइकिल", "साईकिल", "स्कूटर", "मोटरसाइकिल", "बाइक", "कार", "गाड़ी", "बस", "ट्रक", "ट्रैक्टर", "रिक्शा", "ठेला", "बैलगाड़ी", "पहिया", "नाव", "कश्ती",
            "bicycle", "cycle", "scooter", "motorbike", "bike", "car", "bus", "truck", "tractor", "rickshaw", "cart", "wheel", "boat",
            // stone, wood, metal
            "पत्थर", "चट्टान", "ईंट", "लकड़ी", "तख़्ता", "तख्ता", "लोहा", "लोहे", "पीतल", "ताँबा", "stone", "rock", "brick", "wood", "wooden", "plank", "iron", "brass", "copper", "steel", "metal"
    };

    /** Things that bend, fold, hang and fly with the wind. */
    public static final String[] SOFT_WORDS = {
            "साड़ी", "साडी", "दुपट्टा", "ओढ़नी", "चुन्नी", "चुनरी", "घूँघट", "घूंघट", "पल्लू", "आँचल", "आंचल", "शॉल", "शाल", "चादर", "कंबल", "कम्बल", "रज़ाई", "रजाई", "तकिया", "गद्दा", "पर्दा", "परदा",
            "कपड़ा", "कपड़े", "कुर्ता", "कुरता", "कमीज़", "कमीज", "धोती", "लुंगी", "पगड़ी", "साफ़ा", "साफा", "गमछा", "अंगोछा", "रूमाल", "तौलिया", "रस्सी", "डोरी", "धागा", "सूत", "डोर", "जाल",
            "झंडा", "झण्डा", "पताका", "पतंग", "थैला", "थैली", "झोला", "बोरी", "बोरा", "बाल", "चोटी", "लट", "पूँछ", "पूंछ", "पंख", "पत्ते", "पत्ता", "घास", "फूल", "माला", "लहर", "पानी", "धुआँ", "धुआं", "आग", "लपट", "बादल", "रेत",
            "saree", "sari", "dupatta", "veil", "shawl", "scarf", "sheet", "blanket", "quilt", "pillow", "mattress", "curtain", "cloth", "clothes", "kurta", "shirt", "dhoti", "lungi", "turban",
            "gamcha", "handkerchief", "towel", "rope", "string", "thread", "cord", "net", "flag", "banner", "kite", "bag", "sack", "hair", "braid", "plait", "tail", "feather", "wing", "leaves", "leaf",
            "grass", "flower", "garland", "wave", "water", "smoke", "fire", "flame", "cloud", "sand", "ribbon", "cloak", "cape", "skirt", "frock", "gown"
    };

    /** What the words say of a thing: RIGID, SOFT or UNKNOWN. The soft words win where both appear ("लकड़ी की तख़्ती पर कपड़ा": the cloth is what moves). */
    public static int kind(String text) {
        if (text == null || text.length() == 0) return UNKNOWN;
        String t = " " + text.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').replace('-', ' ') + " ";
        boolean rigid = Txt.hasStart(t, RIGID_WORDS), soft = Txt.hasStart(t, SOFT_WORDS);
        if (soft && rigid) return Txt.has(t, "की लकड़ी", "का डंडा", "की छड़ी", "handle", "wooden", "of wood") ? RIGID : SOFT;
        return soft ? SOFT : rigid ? RIGID : UNKNOWN;
    }

    /** v47.4: a thing that opens like a book in the hands — a book, a notebook, a diary, a letter, a newspaper. */
    public static final String[] BOOK_WORDS = {"किताब", "पुस्तक", "कॉपी", "नोटबुक", "डायरी", "अख़बार", "अखबार", "पत्रिका", "चिट्ठी", "ख़त", "खत", "पत्र", "लिफ़ाफ़ा", "लिफाफा", "एल्बम", "रजिस्टर", "book", "notebook", "diary", "letter", "newspaper", "magazine", "album", "register", "ledger"};
    public static boolean bookLike(String text) {
        if (text == null) return false;
        String t = " " + text.toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + " ";
        return Txt.hasStart(t, BOOK_WORDS);
    }

    /**
     * v47.4 (the user: "opening of book, laptop, umbrella etc makes them non rigid from rigid, include similar kinds of
     * things"): things that are rigid in their parts but open and close at a hinge, a fold or a spring — while they open
     * or close they are not one rigid piece: the book's leaves part, the umbrella's canopy spreads, the laptop's lid turns.
     */
    public static final String[] OPENS_WORDS = {"किताब", "पुस्तक", "कॉपी", "नोटबुक", "डायरी", "अख़बार", "अखबार", "पत्रिका", "चिट्ठी", "ख़त", "खत", "लिफ़ाफ़ा", "लिफाफा",
            "लैपटॉप", "लैपटाप", "छाता", "छतरी", "छाते", "पंखा", "हाथ-पंखा", "दरवाज़ा", "दरवाजा", "खिड़की", "अलमारी", "संदूक", "सन्दूक", "बक्सा", "डिब्बा", "डब्बा", "सूटकेस", "बटुआ", "पर्स",
            "कैंची", "चाकू", "फ़ोल्डिंग", "टिफ़िन", "टिफिन", "ढक्कन", "थैला", "बैग", "चश्मा", "फ़ोन", "फोन", "मोबाइल", "पिंजरा", "ताला", "नल",
            "book", "notebook", "diary", "newspaper", "magazine", "letter", "envelope", "laptop", "umbrella", "fan", "door", "window", "cupboard", "wardrobe", "chest", "trunk", "box",
            "suitcase", "wallet", "purse", "scissors", "pocket knife", "folding", "tiffin", "lid", "bag", "glasses", "spectacles", "phone", "cage", "lock", "tap"};
    public static boolean opens(String text) {
        if (text == null) return false;
        String t = " " + text.toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + " ";
        return Txt.hasStart(t, OPENS_WORDS);
    }

    /**
     * v47.6: the material classes the technical director works by (the user: rigid, flexible — cloth, paper, bags —,
     * liquid, granular — soil, dust —, soft, unknown). These are rules the director and renderer follow, not a physics
     * simulation: a RIGID thing keeps its shape; a FLEXIBLE one (cloth, paper, a bag, a ribbon) bends and hangs with the
     * hand and the wind; a LIQUID is poured or held in its vessel, never held on its own; a GRANULAR mass (dust, soil,
     * sand, scattered dry leaves) lies on the ground and lifts only as a few motes in the wind — nobody holds it, gives
     * it or takes it; SOFT is hair, fur, leaves on a branch, smoke, cloud and flame.
     */
    public static final int FLEXIBLE = 3, LIQUID = 4, GRANULAR = 5;
    public static final String[] FLEXIBLE_WORDS = {"कपड़ा", "कपड़े", "कपडा", "कपडे", "रिबन", "फीता", "थैला", "थैली", "झोला", "बैग", "बस्ता", "बोरी", "कागज", "कागज़", "चिट्ठी", "ख़त", "खत", "पत्र",
            "लिफ़ाफ़ा", "लिफाफा", "अख़बार", "अखबार", "रस्सी", "डोरी", "धागा", "चादर", "दुपट्टा", "साड़ी", "रूमाल", "तौलिया", "चप्पल", "पतंग", "झंडा",
            "cloth", "fabric", "ribbon", "bag", "backpack", "satchel", "sack", "paper", "letter", "envelope", "newspaper", "rope", "string", "thread", "sheet", "scarf", "towel", "slipper", "flip-flop", "kite", "flag"};
    public static final String[] LIQUID_WORDS = {"पानी", "दूध", "तेल", "शरबत", "रस", "स्याही", "आँसू", "आंसू", "पसीना", "बारिश", "water", "milk", "oil", "juice", "ink", "tears", "sweat", "rain"};
    public static final String[] GRANULAR_WORDS = {"धूल", "मिट्टी", "रेत", "बालू", "कंकड़", "कंकड", "राख", "अनाज", "चावल", "आटा", "गेहूं", "गेहूँ", "dust", "soil", "dirt", "sand", "gravel", "ash", "grain", "rice", "flour"};
    /** Words that say a thing is spread over the ground as a mass ("जमीन पर बिखरे सूखे पत्ते", "leaves strewn on the ground"). */
    public static final String[] SCATTERED_WORDS = {"बिखरे", "बिखरी", "बिखरा", "फैले", "फैली", "पड़े सूखे", "scattered", "strewn", "littered", "spread over"};

    /** The material class of a listed thing, by its head noun (the last word: "दादाजी की लकड़ी की छड़ी" is a छड़ी). */
    public static int material(String name) {
        if (name == null || name.trim().isEmpty()) return UNKNOWN;
        String t = " " + name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + " ";
        if (groundCover(name)) return GRANULAR;
        String[] ws = name.trim().split("[\\s,]+");
        String head = " " + ws[ws.length - 1].toLowerCase(java.util.Locale.ROOT) + " ";
        if (Txt.hasStart(head, GRANULAR_WORDS)) return GRANULAR;
        if (Txt.hasStart(head, LIQUID_WORDS)) return LIQUID;
        if (Txt.hasStart(head, FLEXIBLE_WORDS)) return FLEXIBLE;
        int k = kind(head);
        if (k != UNKNOWN) return k;
        if (Txt.hasStart(t, FLEXIBLE_WORDS) && !Txt.hasStart(t, RIGID_WORDS)) return FLEXIBLE;
        return kind(t);
    }

    /** A mass that lies over the ground (scattered leaves and dust, soil, sand): set dressing, never in a hand. */
    public static boolean groundCover(String name) {
        if (name == null) return false;
        String t = " " + name.toLowerCase(java.util.Locale.ROOT) + " ";
        if (Txt.has(t, SCATTERED_WORDS)) return true;
        String[] ws = name.trim().split("[\\s,]+");
        return ws.length > 0 && Txt.hasStart(" " + ws[ws.length - 1].toLowerCase(java.util.Locale.ROOT) + " ", GRANULAR_WORDS);
    }

    public static String materialName(int m) {
        switch (m) { case RIGID: return "rigid"; case SOFT: return "soft"; case FLEXIBLE: return "flexible"; case LIQUID: return "liquid"; case GRANULAR: return "granular"; default: return "unknown"; }
    }

    public static boolean isRigid(String text) { return kind(text) == RIGID; }
    public static boolean isSoft(String text) { return kind(text) == SOFT; }

    /** The words of the list found in a text, in order of appearance, each once. */
    public static List<String> found(String text, String[] words) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        String t = " " + text.toLowerCase(java.util.Locale.ROOT) + " ";
        Set<String> seen = new LinkedHashSet<String>();
        for (String w : words) if (t.contains(" " + w) || t.contains(w)) seen.add(w);
        out.addAll(seen);
        return out;
    }

    /**
     * The reader's ledger of the story's things: which keep their shape and which move with the body and the wind —
     * what the technical director rigs and renders by.
     */
    public static String ledger(Story story) {
        if (story == null) return "";
        StringBuilder all = new StringBuilder();
        for (Story.CharacterDef c : story.characters) all.append(' ').append(c.description == null ? "" : c.description);
        for (Story.Scene sc : story.scenes) { all.append(' ').append(sc.setting == null ? "" : sc.setting); for (Story.Beat b : sc.beats) all.append(' ').append(b.text == null ? "" : b.text); }
        List<String> rigid = found(all.toString(), RIGID_WORDS), soft = found(all.toString(), SOFT_WORDS);
        if (rigid.isEmpty() && soft.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Rigid and soft things (the technical director's ledger): ");
        if (!rigid.isEmpty()) sb.append("rigid — drawn straight, never bent by the rig or the wind: ").append(join(rigid)).append(". ");
        if (!soft.isEmpty()) sb.append("soft — move with the body and the wind through the mesh: ").append(join(soft)).append('.');
        // v47.4: what opens and closes — rigid parts, but not one rigid piece while it opens (the book's leaves, the umbrella's canopy, the laptop's lid)
        List<String> opening = found(all.toString(), OPENS_WORDS);
        if (!opening.isEmpty()) sb.append(" Opens and closes (hinged or folding — not rigid while it moves): ").append(join(opening)).append('.');
        // v47.6: each thing the script lists, by its material class
        if (!story.things.isEmpty()) {
            sb.append(" Listed things by material:");
            int n = 0;
            for (Story.PlaceDef th : story.things) sb.append(n++ == 0 ? " " : "; ").append(th.name).append(" — ").append(materialName(material(th.name)));
            sb.append('.');
        }
        return sb.toString();
    }

    private static String join(List<String> ws) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String w : ws) { if (n++ > 0) sb.append(", "); if (n > 14) { sb.append("…"); break; } sb.append(w); }
        return sb.toString();
    }
}

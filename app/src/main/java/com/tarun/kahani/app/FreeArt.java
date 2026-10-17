package com.tarun.kahani.app;

import com.tarun.kahani.core.Bible;
import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Story;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Free pictures of the story's important objects, fetched without any key from GitHub: Microsoft's Fluent Emoji
 * (MIT licence), rendered in 3D on a transparent background — a kite, a crown, a diya, a drum, a key, a kite…
 * Off unless the user turns it on (Settings): an emoji in a film of the user's own photographs looks out of place.
 * Only objects the script's object list names are fetched, and only when the emoji is that same thing; the film then
 * may cut to it for a moment (the handbook's insert: "an important object, clue or physical action"). Saved into the
 * story (and the library) like any picture.
 */
final class FreeArt {
    private FreeArt() {}

    static final String RAW = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/";
    static final String LICENCE = "Fluent Emoji 3D (Microsoft, MIT)";

    /**
     * {the names a script's object list may use (comma separated), the Fluent emoji's folder name}. Portable things only:
     * never the sun, a house or an animal of the cast. A name is here only when the emoji is that thing: a walking stick
     * (छड़ी) is not a magic wand, a letter (चिट्ठी, पत्र) is not a scroll, a postman's bag is not a backpack, a topi is not
     * a top hat, and a closed book is not an open one; those stay with the user's pictures or the director's drawing.
     */
    static final String[][] OBJECTS = {
            {"पतंग,kite", "Kite"}, {"मुकुट,ताज,crown", "Crown"}, {"तलवार,sword,swords", "Crossed swords"}, {"ढाल,shield", "Shield"},
            {"चाबी,चाभी,key,keys", "Key"}, {"दीया,दीपक,diya,oil lamp", "Diya lamp"}, {"ढोल,ढोलक,drum,drums", "Drum"}, {"घंटी,घंटा,bell,bells", "Bell"},
            {"गुब्बार,balloon,balloons", "Balloon"}, {"खुली किताब,open book", "Open book"}, {"सेब,apple,apples", "Red apple"}, {"mango,mangoes", "Mango"},
            {"केला,केले,banana,bananas", "Banana"}, {"नारियल,coconut", "Coconut"}, {"मछली,fish", "Fish"}, {"छाता,छतरी,umbrella", "Umbrella"},
            {"मोमबत्ती,candle,candles", "Candle"}, {"तोहफ़,तोहफा,उपहार,gift,present", "Wrapped gift"}, {"केक,cake", "Birthday cake"}, {"रोटी,bread,roti", "Bread"},
            {"हांडी,हाँडी,cooking pot", "Pot of food"}, {"धनुष,धनुष बाण,bow and arrow", "Bow and arrow"}, {"हीरा,रत्न,gem,diamond,jewel", "Gem stone"},
            {"अँगूठी,अंगूठी,ring", "Ring"}, {"सिक्क,coin,coins", "Coin"}, {"खज़ाना,खजाना,money bag,treasure", "Money bag"}, {"नक़्शा,नक्शा,map", "World map"},
            {"दूरबीन,telescope", "Telescope"}, {"फ़ोन,फोन,मोबाइल,phone,mobile", "Mobile phone"}, {"लैपटॉप,laptop", "Laptop"}, {"ताला,padlock", "Locked"},
            {"हड्डी,bone", "Bone"}, {"पंख,feather", "Feather"}, {"गिटार,guitar", "Guitar"}, {"तुरही,trumpet", "Trumpet"}, {"वायलिन,violin", "Violin"},
            {"फुटबॉल,football", "Soccer ball"}, {"बल्ला,cricket bat", "Cricket game"}, {"टेडी,teddy", "Teddy bear"},
            {"जादू की छड़ी,जादुई छड़ी,magic wand", "Magic wand"}, {"क्रिस्टल,crystal ball", "Crystal ball"}, {"top hat", "Top hat"}, {"चश्मा,spectacles,eyeglasses", "Glasses"},
            {"बस्ता,backpack", "Backpack"}, {"आवर्धक,magnifying glass", "Magnifying glass tilted left"}, {"कलम,pen", "Pen"},
            {"scroll", "Scroll"}, {"लालटेन,lantern", "Red paper lantern"}, {"टॉर्च,torch,flashlight", "Flashlight"}, {"बल्ब,bulb", "Light bulb"},
            {"हेडफ़ोन,हेडफोन,headphone,headphones", "Headphone"}, {"जॉयस्टिक,joystick,controller", "Joystick"}, {"अलार्म घड़ी,alarm clock", "Alarm clock"},
            {"रेतघड़ी,hourglass", "Hourglass done"}, {"मशरूम,mushroom", "Mushroom"}, {"सूरजमुखी,sunflower", "Sunflower"}, {"गुलदस्त,bouquet", "Bouquet"},
            {"रॉकेट,rocket", "Rocket"}, {"तंबू,tent", "Tent"}, {"नाव,boat,sailboat", "Sailboat"},
    };

    /** The raw URL of an emoji's 3D picture. */
    static String url(String name) {
        String file = name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_3d.png";
        return RAW + name.replace(" ", "%20") + "/3D/" + file;
    }

    /**
     * The objects of the script's own object list that have a picture here, {keys, name, the listed thing's name}. Only that list counts: words in
     * the directions ("दिया" is also "gave", "घड़ी भर" is a moment, "पंखा" is a fan) never become an insert, and a story
     * with no object list gets none. A thing matches by its whole name or by its head noun (the last word: "पुराना
     * लकड़ी का ताला" is a ताला), compared as whole words.
     */
    static List<String[]> wanted(Story story) {
        List<String[]> out = new ArrayList<String[]>();
        for (Story.PlaceDef th : story.things) {
            String name = th.name == null ? "" : th.name.replaceAll("\\([^)]*\\)", " ").trim();
            if (name.isEmpty() || Bible.intangible(name)) continue;
            String[] o = match(name);
            boolean dup = false;
            for (String[] w : out) if (w[1].equals(o == null ? "" : o[1])) dup = true;
            if (o != null && !dup) out.add(new String[]{o[0], o[1], th.name.trim()});
        }
        return out;
    }

    /** The picture for one listed thing: its whole name or its head noun is one of the names, else null. */
    static String[] match(String name) {
        String n = norm(name);
        String[] words = n.split(" ");
        String head = words[words.length - 1];
        for (String[] o : OBJECTS) {
            for (String k : o[0].split(",")) {
                String key = norm(k);
                if (key.isEmpty()) continue;
                if (key.equals(n) || key.equals(head) || (key.indexOf(' ') > 0 && (" " + n + " ").contains(" " + key + " "))) return o;
            }
        }
        return null;
    }

    /** Lower case, punctuation to spaces, one space between words; Devanagari vowel signs stay part of their word. */
    static String norm(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int t = Character.getType(c);
            boolean part = Character.isLetterOrDigit(c) || t == Character.NON_SPACING_MARK || t == Character.COMBINING_SPACING_MARK
                    || c == '\u200c' || c == '\u200d';
            b.append(part ? Character.toLowerCase(c) : ' ');
        }
        return b.toString().trim().replaceAll(" +", " ");
    }

    /** Fetches the pictures of up to 'max' objects the story has no picture for yet. Returns how many were added. */
    static int fetchFor(Project project, Story story, Cloud cloud, Library lib, int max, List<String> notes) {
        if (cloud == null) return 0;
        String cast = project.read("cast.txt");
        int added = 0;
        for (String[] o : wanted(story)) {
            if (added >= max) break;
            if (cast.contains("|" + o[0] + "|")) continue;          // already there
            if (o.length > 2 && cast.contains("|" + o[2] + "|")) continue;     // the user's own picture of that thing comes first
            // an animal word that is a character of the story is the character, not a picture of an object
            try {
                byte[] png = cloud.download(url(o[1]));
                if (png == null || png.length < 2000 || png.length > 3 * 1024 * 1024) continue;
                String file = project.savePicture(png, "obj");
                project.setManifest("shot", ":" + o[0], "shot||" + o[0] + "|" + file + "|object");
                try { lib.addBytes(Library.PIC, "object", o[1], o[0].replace(',', ' '), png, ".png", LICENCE); } catch (Exception ignored) {}
                added++;
                notes.add("picture of the " + o[1].toLowerCase(Locale.ROOT) + " (" + LICENCE + ")");
            } catch (Exception ignored) {
                // offline or the file is gone: the story goes on without the insert
            }
        }
        return added;
    }

    /** True when the look is an animal the script's words could also name (the object list keeps no animals, so always false for now). */
    static boolean isAnimal(Look l) { return l != null && (l.kind == Look.ANIMAL || l.kind == Look.BIRD || l.kind == Look.MONKEY); }
}

package com.tarun.kahani.app;

import android.content.Context;

import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.SoundLib;
import com.tarun.kahani.core.Txt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.security.MessageDigest;

/**
 * The studio library: everything the user ever uploaded, recorded, downloaded or generated (pictures, voice
 * samples, sounds), plus the pictures and sounds that come with the app. Saved in the app's private storage and
 * reusable in every film.
 */
public final class Library {
    public static final String PIC = "pic", VOICE = "voice", SOUND = "sound", SCRIPT = "script";

    public static final class Item {
        public String id, type, kind = "", name = "", tags = "", path = "", source = "", meta = "";
        public String meta(String key) {
            for (String kv : meta.split(";")) if (kv.startsWith(key + "=")) return kv.substring(key.length() + 1);
            return null;
        }
        public void setMeta(String key, String value) {
            StringBuilder b = new StringBuilder();
            for (String kv : meta.split(";")) if (kv.length() > 0 && !kv.startsWith(key + "=")) b.append(kv).append(';');
            b.append(key).append('=').append(value);
            meta = b.toString();
        }
        public boolean builtIn;
        public String label() { return name.length() > 0 ? name : new File(path).getName(); }
    }

    private final Context ctx;
    private final File dir, index;
    public final List<Item> items = new java.util.concurrent.CopyOnWriteArrayList<Item>();

    private static Library shared;

    /**
     * The one library of the app. Screens and the background film maker share it, so nothing added in one place
     * can ever be overwritten by another (everything is saved on the phone at once and stays after closing).
     */
    public static synchronized Library get(Context c) {
        if (shared == null) shared = new Library(c);
        return shared;
    }

    private Library(Context c) {
        ctx = c.getApplicationContext();
        dir = new File(ctx.getFilesDir(), "library");
        dir.mkdirs();
        index = new File(dir, "index.json");
        load();
        recoverOrphans();
        addBuiltIns();
        measureOldVoices();
    }

    /** Files in the library folders that the list does not know (e.g. after a crash) are taken back in. */
    private void recoverOrphans() {
        java.util.Set<String> known = new java.util.HashSet<String>();
        for (Item it : items) known.add(new File(it.path).getAbsolutePath());
        boolean changed = false;
        for (String type : new String[]{PIC, VOICE, SOUND, SCRIPT}) {
            File[] fs = new File(dir, type).listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                if (f.getName().endsWith(".json") || known.contains(f.getAbsolutePath()) || f.length() == 0) continue;
                Item it = new Item();
                it.type = type;
                it.path = f.getAbsolutePath();
                it.id = f.getName().replaceAll("\\.[A-Za-z0-9]+$", "");
                readSidecar(it);
                if (it.name.length() == 0) it.name = "recovered " + it.id.substring(Math.max(0, it.id.length() - 6));
                items.add(it);
                changed = true;
            }
        }
        if (changed) save();
    }

    static File sidecar(Item it) { return new File(it.path + ".json"); }

    private void readSidecar(Item it) {
        try {
            File sc = sidecar(it);
            if (!sc.exists()) return;
            Object x = Json.parseLoose(new String(AudioIO.readFile(sc), "UTF-8"));
            it.kind = Json.str(x, "kind", ""); it.name = Json.str(x, "name", ""); it.tags = Json.str(x, "tags", "");
            it.source = Json.str(x, "source", ""); it.meta = Json.str(x, "meta", "");
        } catch (Exception ignored) {
        }
    }

    static String describe(Item it) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", it.id); m.put("type", it.type); m.put("kind", it.kind); m.put("name", it.name);
        m.put("tags", it.tags); m.put("source", it.source); m.put("meta", it.meta); m.put("file", new File(it.path).getName());
        return Json.write(m);
    }

    public File dir() { return dir; }

    private void load() {
        if (!index.exists()) return;
        try {
            Object o = Json.parseLoose(new String(AudioIO.readFile(index), "UTF-8"));
            if (!(o instanceof List)) return;
            for (Object x : (List<?>) o) {
                Item it = new Item();
                it.id = Json.str(x, "id", ""); it.type = Json.str(x, "type", PIC); it.kind = Json.str(x, "kind", "");
                it.name = Json.str(x, "name", ""); it.tags = Json.str(x, "tags", ""); it.path = Json.str(x, "path", "");
                it.source = Json.str(x, "source", "");
                it.meta = Json.str(x, "meta", "");
                if (new File(it.path).exists()) items.add(it);
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void save() {
        List<Object> arr = new ArrayList<Object>();
        for (Item it : items) {
            if (it.builtIn) continue;
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", it.id); m.put("type", it.type); m.put("kind", it.kind); m.put("name", it.name);
            m.put("tags", it.tags); m.put("path", it.path); m.put("source", it.source); m.put("meta", it.meta);
            arr.add(m);
        }
        try {
            File tmp = new File(dir, "index.tmp");
            FileOutputStream o = new FileOutputStream(tmp);
            o.write(Json.write(arr).getBytes("UTF-8"));
            o.getFD().sync();
            o.close();
            if (!tmp.renameTo(index)) { index.delete(); tmp.renameTo(index); }
        } catch (IOException ignored) {
        }
        // a small description next to every file, so the library can always be rebuilt from the files alone
        for (Item it : items) {
            if (it.builtIn) continue;
            try {
                String d = describe(it);
                File sc = sidecar(it);
                if (sc.exists() && new String(AudioIO.readFile(sc), "UTF-8").equals(d)) continue;
                FileOutputStream o = new FileOutputStream(sc);
                o.write(d.getBytes("UTF-8"));
                o.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** Pictures and sounds bundled with the app. */
    private void addBuiltIns() {
        try {
            String[] pics = ctx.getAssets().list("sample");
            if (pics != null) for (String f : pics) {
                if (!(f.endsWith(".jpg") || f.endsWith(".png"))) continue;
                Item it = new Item();
                it.id = "asset:sample/" + f; it.type = PIC; it.builtIn = true; it.path = "asset:sample/" + f; it.source = "app";
                String base = f.replaceAll("\\.(jpg|png)$", "");
                boolean back = base.startsWith("char_") && base.endsWith("_back");
                it.kind = back ? "view" : base.startsWith("char_") ? "person" : base.startsWith("bg_") ? "place" : base.startsWith("shot_") ? "shot" : base;
                it.name = base.replace("char_", "").replace("bg_", "").replace("shot_", "").replace('_', ' ');
                if (back) {
                    // the back view of a character's picture: never a front picture, kept with the picture it belongs to
                    it.name = base.substring(5, base.length() - 5).replace('_', ' ') + " (back view)";
                    it.setMeta("view", "180");
                    it.setMeta("of", "asset:sample/" + base.substring(0, base.length() - 5) + ".jpg");
                }
                it.tags = it.name;
                items.add(it);
            }
            String json = Project.readAsset(ctx.getAssets(), "sounds/index.json");
            Object o = Json.parseLoose(json);
            if (o instanceof List) for (Object x : (List<?>) o) {
                Item it = new Item();
                it.path = "asset:sounds/" + Json.str(x, "file", ""); it.id = it.path; it.type = SOUND; it.builtIn = true;
                it.kind = Json.str(x, "type", "sfx"); it.name = Json.str(x, "title", ""); it.tags = Json.str(x, "words", ""); it.source = "app";
                items.add(it);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Imports story-pack images once into the app-private library. Multi-image pose/expression/contact sheets
     * are split using the same reader as phone uploads; each resulting pose/angle is stored in filesDir/library
     * and survives app restarts without touching the camera roll. Packaged originals remain read-only in assets.
     */
    public void importBundledFilmReferences() throws IOException {
        android.content.SharedPreferences prefs = ctx.getSharedPreferences("kahani", Context.MODE_PRIVATE);
        if (prefs.getBoolean("bundledFilmReferencesV2", false)) return;
        String[][] films = {
                {"aakhiri_chai", "आखिरी चाय"},
                {"babu_ke_liye", "बाबू के लिए"}
        };
        // Upgrade earlier generic imports in place. Never duplicate or delete a user's renamed/annotated references.
        if (prefs.getBoolean("bundledFilmReferencesV1", false)) {
            migrateBundledReferenceLabels(films);
            prefs.edit().putBoolean("bundledFilmReferencesV2", true).apply();
            return;
        }
        int importedFiles = 0;
        for (String[] film : films) {
            String filmId = film[0], storyTitle = film[1];
            String assetDir = "film_library/" + filmId + "/stills";
            String[] files = ctx.getAssets().list(assetDir);
            if (files == null) continue;
            java.util.Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
            Map<String, Map<String, Object>> semantic = bundledSemantics(filmId);
            int seq = 0;
            for (String name : files) {
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"))) continue;
                if (lower.contains("contact_sheet") || lower.contains("montage") || lower.contains("contact-sheet")) continue;
                byte[] data;
                java.io.InputStream in = ctx.getAssets().open(assetDir + "/" + name);
                try { data = Project.readAll(in); } finally { try { in.close(); } catch (Exception ignored) {} }
                if (data.length == 0) continue;
                seq++;
                importedFiles++;
                String originalName = originalBundledFilename(name);
                Map<String, Object> record = semantic.get(originalName);
                String label = semanticLabel(record, storyTitle + " reference " + seq);
                String kind = Json.str(record, "library_kind", "");
                String role = Json.str(record, "role", "");
                boolean autoAssign = semanticAutoAssign(record);
                String source = "bundled:" + filmId;
                List<Item> split = new ArrayList<Item>();
                boolean maySplit = !role.startsWith("storyboard") && !"reference".equals(kind);
                boolean sourceSheet = record != null && "true".equalsIgnoreCase(String.valueOf(record.get("source_sheet")));
                boolean attemptedSplit = false;
                try {
                    boolean detectedSheet = maySplit && SheetSaver.isSheet(data, "place".equals(kind));
                    if (maySplit && (sourceSheet || detectedSheet)) {
                        attemptedSplit = true;
                        split = SheetSaver.saveToLibrary(this, label, kind, data, source);
                    }
                } catch (Throwable ignored) {
                    split = new ArrayList<Item>();
                }
                if (split.isEmpty()) {
                    String ext = lower.endsWith(".webp") ? ".webp" : lower.endsWith(".png") ? ".png" : ".jpg";
                    // If a sheet was expected/detected but could not be split, never auto-assign the entire grid as a person/place/prop.
                    String storedKind = attemptedSplit ? "reference" : kind;
                    boolean storedAutoAssign = attemptedSplit ? false : autoAssign;
                    Item it = addBytes(PIC, storedKind, label, semanticTags(storyTitle, originalName, record), data, ext, source);
                    applyBundledSemanticMetadata(it, filmId, originalName, record, storedAutoAssign);
                    if (attemptedSplit) it.setMeta("splitFailed", "1");
                } else {
                    for (Item it : split) {
                        it.tags = (it.tags + " " + semanticTags(storyTitle, originalName, record)).trim();
                        applyBundledSemanticMetadata(it, filmId, originalName, record, autoAssign);
                    }
                    save();
                }
            }
        }
        if (importedFiles > 0) {
            save();
            prefs.edit().putBoolean("bundledFilmReferencesV2", true).apply();
        }
    }

    /** Maps the manifest's original upload filename to its provisional, reviewed semantic record. */
    private Map<String, Map<String, Object>> bundledSemantics(String filmId) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<String, Map<String, Object>>();
        try {
            Object root = Json.parseLoose(Project.readAsset(ctx.getAssets(), "film_library/" + filmId + "/manifest.json"));
            if (!(root instanceof Map)) return out;
            Object uploads = ((Map<?, ?>) root).get("uploads");
            if (!(uploads instanceof List)) return out;
            for (Object row : (List<?>) uploads) {
                if (!(row instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> record = (Map<String, Object>) row;
                String filename = Json.str(record, "original_filename", "");
                if (filename.length() > 0) out.put(filename, record);
            }
        } catch (Exception ignored) {
            // A pack without semantic records remains available as a generic, review-required reference library.
        }
        return out;
    }

    private static String originalBundledFilename(String safeName) {
        return safeName.startsWith("images__") ? safeName.substring("images__".length()) : safeName;
    }

    private static String semanticLabel(Map<String, Object> record, String fallback) {
        String label = Json.str(record, "entity_label", "");
        return label.trim().length() > 0 ? label.trim() : fallback;
    }

    private static boolean semanticAutoAssign(Map<String, Object> record) {
        return record != null && "true".equalsIgnoreCase(String.valueOf(record.get("auto_assign")));
    }

    private static String semanticTags(String storyTitle, String filename, Map<String, Object> record) {
        String label = Json.str(record, "entity_label", "");
        String role = Json.str(record, "role", "");
        String entity = Json.str(record, "entity_id", "");
        return (storyTitle + " " + label + " " + entity + " " + role + " " + filename.replace('_', ' ')).trim();
    }

    private static void applyBundledSemanticMetadata(Item it, String filmId, String filename,
            Map<String, Object> record, boolean autoAssign) {
        it.setMeta("bundledFilm", filmId);
        it.setMeta("sourceFilename", originalBundledFilename(filename));
        it.setMeta("assetRole", Json.str(record, "role", "unclassified_reference"));
        it.setMeta("entityId", Json.str(record, "entity_id", ""));
        it.setMeta("semanticConfidence", record == null ? "0" : String.valueOf(record.get("confidence")));
        it.setMeta("autoAssign", autoAssign ? "1" : "0");
        it.setMeta("reviewRequired", autoAssign ? "0" : "1");
        String declaredView = Json.str(record, "view", "");
        // Do not overwrite the per-panel view that SheetSaver inferred from a contact sheet.
        if (declaredView.length() > 0 && !"contact_sheet".equals(declaredView)) it.setMeta("view", declaredView);
    }

    /** Re-label only untouched auto-generated names; preserve user renames, notes and explicit corrections. */
    private boolean migrateBundledReferenceLabels(String[][] films) {
        boolean changed = false;
        for (String[] film : films) {
            String filmId = film[0], storyTitle = film[1];
            String assetDir = "film_library/" + filmId + "/stills";
            String[] files;
            try { files = ctx.getAssets().list(assetDir); } catch (IOException e) { files = null; }
            if (files == null) continue;
            java.util.Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
            Map<String, Map<String, Object>> semantic = bundledSemantics(filmId);
            int seq = 0;
            for (String name : files) {
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"))) continue;
                if (lower.contains("contact_sheet") || lower.contains("montage") || lower.contains("contact-sheet")) continue;
                seq++;
                String originalName = originalBundledFilename(name);
                Map<String, Object> record = semantic.get(originalName);
                if (record == null) continue;
                String oldPrefix = storyTitle + " reference " + seq;
                String newPrefix = semanticLabel(record, oldPrefix);
                if (newPrefix.equals(oldPrefix)) continue;
                for (Item it : items) {
                    if (!filmId.equals(it.meta("bundledFilm")) || !it.name.startsWith(oldPrefix)) continue;
                    String suffix = it.name.substring(oldPrefix.length());
                    if (suffix.length() > 0 && Character.isDigit(suffix.charAt(0))) continue; // reference 1 must not match reference 10
                    it.name = newPrefix + suffix;
                    if (suffix.length() == 0) it.kind = Json.str(record, "library_kind", it.kind);
                    it.tags = (it.tags + " " + semanticTags(storyTitle, originalName, record)).trim();
                    applyBundledSemanticMetadata(it, filmId, originalName, record, semanticAutoAssign(record));
                    changed = true;
                }
            }
        }
        if (changed) save();
        return changed;
    }

    /** Story-specific reference images shipped in the APK and exposed as read-only library items. */
    private void addFilmBuiltIns(String assetDir, String storyTitle) {
        try {
            String[] files = ctx.getAssets().list(assetDir);
            if (files == null) return;
            java.util.Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
            int sequence = 0;
            for (String f : files) {
                String lower = f.toLowerCase(java.util.Locale.ROOT);
                if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"))) continue;
                Item it = new Item();
                it.id = "asset:" + assetDir + "/" + f;
                it.type = PIC;
                it.kind = "reference";
                it.builtIn = true;
                it.path = "asset:" + assetDir + "/" + f;
                it.source = "app";
                it.name = storyTitle + " reference " + (++sequence);
                it.tags = storyTitle + " reference image " + f.replace('_', ' ').replace('-', ' ');
                it.setMeta("storyReference", assetDir.substring("film_library/".length(), assetDir.indexOf("/stills")));
                items.add(it);
            }
        } catch (IOException ignored) {
            // Standard library stays usable if optional film asset packs are absent.
        }
    }

    public Item add(String type, String kind, String name, String tags, File src, String ext, String source) throws IOException {
        Item it = new Item();
        it.id = type + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        it.type = type; it.kind = kind == null ? "" : kind; it.name = name == null ? "" : name; it.tags = tags == null ? "" : tags;
        it.source = source == null ? "" : source;
        File sub = new File(dir, type);
        sub.mkdirs();
        File dst = new File(sub, it.id + ext);
        if (!src.renameTo(dst)) {
            Project.copy(new java.io.FileInputStream(src), new FileOutputStream(dst));
            src.delete();
        }
        it.path = dst.getAbsolutePath();
        // measuring takes a few seconds (and may ask the AI): never while the library is locked, so the
        // screens stay responsive (if the app stops meanwhile, the file is taken back in on the next start)
        if (type.equals(VOICE)) analyseVoice(it);
        if (type.equals(PIC)) analysePicture(it);
        if (type.equals(SOUND)) analyseSound(it);
        synchronized (this) { items.add(0, it); }
        save();
        Backup.copy(ctx, it);
        // the picture lives only in the app's own library "tarunkahani" (this folder and its private backup);
        // it is never written to the camera or photos library
        return it;
    }

    /** Colours, figure/place, real-photo signs: lets the director match the picture to stories later. */
    public void analysePicture(Item it) {
        try {
            byte[] data = Project.readAll(open(it));
            android.graphics.Bitmap b = MainActivity.decodeSmall(data, 256);
            if (b == null) return;
            int w = b.getWidth(), h = b.getHeight();
            int[] px = new int[w * h];
            b.getPixels(px, 0, w, 0, 0, w, h);
            b.recycle();
            com.tarun.kahani.core.PicSense.Info in = com.tarun.kahani.core.PicSense.analyse(px, w, h);
            StringBuilder keep = new StringBuilder();
            for (String kv : it.meta.split(";")) {
                if (kv.length() == 0 || kv.startsWith("photo=") || kv.startsWith("figure=") || kv.startsWith("skin=") || kv.startsWith("hue=")
                        || kv.startsWith("place=") || kv.startsWith("tr=")) continue;
                keep.append(kv).append(';');
            }
            it.meta = keep + in.toMeta();
            if (in.figure) {
                // dress style, moustache, bindi, turban, spear, fur… read from a slightly bigger copy of the figure
                android.graphics.Bitmap big = MainActivity.decodeSmall(data, 420);
                if (big != null) {
                    int bw = big.getWidth(), bh = big.getHeight();
                    int[] bp = new int[bw * bh];
                    big.getPixels(bp, 0, bw, 0, 0, bw, bh);
                    big.recycle();
                    com.tarun.kahani.core.Cutout.Result cr = com.tarun.kahani.core.Cutout.process(bp, bw, bh);
                    it.meta += ";" + com.tarun.kahani.core.PicSense.traits(cr).toMeta();
                }
            }
            if (cameraPhoto(data)) it.setMeta("camera", "1");
        } catch (Throwable ignored) {
        }
    }

    /** A real camera photo carries its exposure, aperture, sensitivity or focal length in its EXIF data. */
    public static boolean cameraPhoto(byte[] data) {
        try {
            // v38: what a camera writes when it takes a photo — the exposure, the aperture, the sensitivity, the focal
            // length. A phone's make and model alone are no sign: a phone writes them into every picture its gallery
            // saves or edits, drawn ones too (that refused every animated character picture from a phone's gallery)
            android.media.ExifInterface ex = new android.media.ExifInterface(new java.io.ByteArrayInputStream(data));
            return ex.getAttribute(android.media.ExifInterface.TAG_EXPOSURE_TIME) != null || ex.getAttribute(android.media.ExifInterface.TAG_F_NUMBER) != null
                    || ex.getAttribute(android.media.ExifInterface.TAG_ISO_SPEED_RATINGS) != null || ex.getAttribute(android.media.ExifInterface.TAG_FOCAL_LENGTH) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    public com.tarun.kahani.core.PicSense.Info info(Item it) {
        com.tarun.kahani.core.PicSense.Info in = com.tarun.kahani.core.PicSense.Info.fromMeta(it.meta);
        // pictures analysed by an older version get the newer reading (dress style, moustache, bindi…) once
        boolean old = in == null || (in.figure && !it.meta.contains("tr=")) || !it.meta.contains("place=") || it.meta.split("place=")[1].split(";")[0].split(",").length < 10;
        if (old && !it.builtIn && it.type.equals(PIC)) { analysePicture(it); save(); in = com.tarun.kahani.core.PicSense.Info.fromMeta(it.meta); }
        return in;
    }

    /**
     * Measures a voice sample once (pitch, how lively, how fast, how bright, how raspy) so the director can
     * match it to characters whose voice the script describes ("a deep, slow voice", "मीठी आवाज़"…).
     */
    void analyseVoice(Item it) {
        float[] pcm = AudioIO.decode(ctx, it.path, 40);
        float[] vf = pcm == null ? null : com.tarun.kahani.core.VoiceFx.features(pcm, com.tarun.kahani.core.Synth.SR);
        if (vf == null || vf[0] <= 0) { it.setMeta("vf", "-"); return; }   // no voice heard in it: not tried again
        it.setMeta("pitch", String.valueOf(Math.round(vf[0])));
        it.setMeta("vf", com.tarun.kahani.core.VoiceMatch.format(vf));
        // the words shown with the voice: "female, smooth, lively voice" (older single words are replaced)
        String d = com.tarun.kahani.core.VoiceMatch.describe(vf) + " voice";
        String tags = it.tags.replaceAll("(^|, )(deep male|male|female|child) voice", "").replaceAll("^, ", "");
        if (it.meta("vdesc") != null) tags = tags.replace(it.meta("vdesc"), "").replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
        it.tags = tags.length() > 0 ? tags + ", " + d : d;
        it.setMeta("vdesc", d);
    }

    /** The stored measurements of a voice, or null (built-in voices and voices not measured yet). */
    public static float[] voiceFeatures(Item it) {
        return it == null ? null : com.tarun.kahani.core.VoiceMatch.parse(it.meta("vf"));
    }

    /** Voices and sounds saved by an older version are measured once in the background. */
    private void measureOldVoices() {
        new Thread(new Runnable() {
            public void run() {
                boolean changed = false;
                for (Item it : items) {
                    if (it.builtIn) continue;
                    try {
                        if (VOICE.equals(it.type) && it.meta("vf") == null) { analyseVoice(it); changed = true; }
                        if (SOUND.equals(it.type) && it.meta("sk") == null) { analyseSound(it); changed = true; }
                    } catch (Throwable ignored) {}
                }
                if (changed) save();
            }
        }, "measure-voices").start();
    }

    /**
     * Listens to a sound once: is it a background that loops under a scene, a one-off effect, music or people
     * talking, and what does it sound like (water, wind, birds, bells…). A kind the user chose is kept.
     */
    void analyseSound(Item it) {
        float[] pcm = AudioIO.decode(ctx, it.path, 30);
        if (pcm == null) { it.setMeta("sk", "-"); return; }
        com.tarun.kahani.core.SoundSense.Info in = com.tarun.kahani.core.SoundSense.analyse(pcm, com.tarun.kahani.core.Synth.SR);
        it.setMeta("sk", in.kind);
        it.setMeta("sl", in.words());
        it.setMeta("sec", String.valueOf(Math.round(in.seconds * 10) / 10f));
        if (!"1".equals(it.meta("kindSet"))) it.kind = in.kind.equals("voices") ? "amb" : in.kind;
        // with a Gemini key the AI listens too: it names the sound far better than the offline guess
        if (Prefs.online(ctx) && it.meta("ai") == null) {
            com.tarun.kahani.core.Cloud cl = Prefs.cloud(ctx);
            if (cl != null && cl.hasGemini()) {
                try {
                    com.tarun.kahani.core.ScriptAI.Heard h = com.tarun.kahani.core.ScriptAI.listen(cl, pcm, com.tarun.kahani.core.Synth.SR);
                    if (h.words.length() > 0) it.setMeta("ai", h.words.replace(';', ','));
                    if (h.kind.length() > 0) {
                        it.setMeta("sk", h.kind);
                        if (!"1".equals(it.meta("kindSet"))) it.kind = h.kind.equals("voices") ? "amb" : h.kind;
                    }
                } catch (Exception ignored) {
                    // offline guess stays
                }
            }
        }
    }

    /** Drops words that say nothing about a sound: numbers and file-name parts like "AUD-2025…", "recording 3". */
    static String meaningful(String words) {
        StringBuilder b = new StringBuilder();
        for (String w : words.split("[,;_\\-.\\s]+")) {
            String t = w.trim().toLowerCase(java.util.Locale.ROOT);
            if (t.length() < 2 || t.matches(".*\\d.*") || t.matches("aud|audio|recording|record|rec|sound|sounds|voice|file|new|wa|whatsapp|ptt|mp3|m4a|wav|ogg|aac|opus|clip|track|untitled")) continue;
            if (b.length() > 0) b.append(',');
            b.append(w.trim());
        }
        return b.toString();
    }

    /** "Background", "Effect", "Music" for a sound item. */
    public static String soundKindLabel(Item it) {
        String k = it.kind;
        if ("voices".equals(it.meta("sk")) && "amb".equals(k)) return "Background voices";
        return "sfx".equals(k) ? "Effect (plays once)" : "music".equals(k) ? "Music" : "Background (loops)";
    }

    /** Puts a backed-up item back with its original id (used by restore). */
    synchronized Item restoreItem(String type, String id, byte[] data, String ext) throws IOException {
        Item it = new Item();
        it.id = id;
        it.type = type;
        File sub = new File(dir, type);
        sub.mkdirs();
        File dst = new File(sub, id + ext);
        FileOutputStream o = new FileOutputStream(dst);
        o.write(data);
        o.close();
        it.path = dst.getAbsolutePath();
        items.add(0, it);
        return it;
    }

    /**
     * Imports a reusable user asset bundle into the private library. Original ZIP and an entry manifest are retained.
     * Media items are indexed with archive path, bundle fingerprint and filmmaker metadata; never tied to a single film.
     */
    public synchronized int importZipArchive(InputStream input, String bundleName, String userMetadata) throws IOException {
        if (input == null) throw new IOException("No archive selected");
        byte[] archive = Project.readAll(input);
        if (archive.length == 0 || archive.length > 100 * 1024 * 1024) throw new IOException("Archive must be between 1 byte and 100 MiB");
        if (archive.length < 4 || archive[0] != 'P' || archive[1] != 'K') throw new IOException("Selected file is not a ZIP archive");
        final String digest;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(archive);
            StringBuilder h = new StringBuilder();
            for (byte b : hash) h.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            digest = h.toString();
        } catch (Exception e) { throw new IOException("Could not fingerprint archive", e); }
        int already = 0;
        for (Item item : items) if (digest.equals(item.meta("bundleId"))) already++;
        File bundles = new File(dir, "bundles");
        if (!bundles.exists() && !bundles.mkdirs()) throw new IOException("Cannot create library bundle directory");
        File saved = new File(bundles, digest + ".zip");
        File manifestFile = new File(bundles, digest + ".json");
        if (already > 0 && saved.exists() && manifestFile.exists()) return already;
        if (!saved.exists()) {
            FileOutputStream out = new FileOutputStream(saved);
            try { out.write(archive); out.getFD().sync(); } finally { out.close(); }
        }
        List<Object> entries = new ArrayList<Object>();
        int imported = 0, totalBytes = 0, entryCount = 0;
        ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive));
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) { zip.closeEntry(); continue; }
                String raw = entry.getName().replace('\\', '/');
                String[] segments = raw.split("/");
                boolean unsafe = raw.startsWith("/") || raw.matches("^[A-Za-z]:.*");
                for (String segment : segments) if ("..".equals(segment)) unsafe = true;
                if (unsafe) { zip.closeEntry(); continue; }
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                byte[] block = new byte[8192]; int n;
                while ((n = zip.read(block)) != -1) {
                    totalBytes += n;
                    if (totalBytes > 300 * 1024 * 1024) throw new IOException("Archive expands beyond the 300 MiB safety limit");
                    buf.write(block, 0, n);
                }
                zip.closeEntry();
                byte[] data = buf.toByteArray();
                if (data.length == 0) continue;
                entryCount++;
                String leaf = segments[segments.length - 1];
                String lower = leaf.toLowerCase(java.util.Locale.ROOT);
                String ext = leaf.lastIndexOf('.') >= 0 ? leaf.substring(leaf.lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT) : "";
                String type = null, kind = "";
                if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext)) { type = PIC; kind = "reference"; }
                else if (".wav".equals(ext) || ".mp3".equals(ext) || ".m4a".equals(ext) || ".ogg".equals(ext) || ".aac".equals(ext)) { type = SOUND; kind = "audio"; }
                else if (".txt".equals(ext) || ".md".equals(ext) || ".srt".equals(ext)) { type = SCRIPT; kind = "screenplay-or-text"; }
                Map<String,Object> rec = new LinkedHashMap<String,Object>();
                rec.put("archivePath", raw); rec.put("bytes", data.length);
                if (type != null) {
                    String label = leaf.replaceAll("\\.[^.]+$", "").replace('_', ' ');
                    Item it = addBytes(type, kind, label, (bundleName == null ? "user bundle" : bundleName) + " " + raw, data, ext, "bundle:" + digest);
                    it.setMeta("bundleId", digest);
                    it.setMeta("archiveName", bundleName == null ? "user bundle" : bundleName.replace(';', ' '));
                    it.setMeta("archivePath", raw.replace(';', ','));
                    it.setMeta("userMetadata", userMetadata == null ? "" : userMetadata.replace(';', ','));
                    it.setMeta("assetRole", type.equals(SCRIPT) ? "reusable-screenplay" : type.equals(PIC) ? "visual-reference" : "sound-or-voice");
                    rec.put("libraryItemId", it.id); rec.put("type", type); rec.put("kind", kind);
                    imported++;
                } else rec.put("type", "retained-in-original-archive");
                entries.add(rec);
            }
        } finally { try { zip.close(); } catch (Exception ignored) {} }
        if (entryCount == 0) throw new IOException("ZIP archive contains no readable files");
        Map<String,Object> manifest = new LinkedHashMap<String,Object>();
        manifest.put("schemaVersion", 1); manifest.put("bundleId", digest);
        manifest.put("archiveName", bundleName == null ? "user bundle" : bundleName);
        manifest.put("userMetadata", userMetadata == null ? "" : userMetadata);
        manifest.put("archivePath", saved.getAbsolutePath()); manifest.put("archiveBytes", archive.length);
        manifest.put("entryCount", entryCount); manifest.put("indexedMediaCount", imported); manifest.put("entries", entries);
        FileOutputStream out = new FileOutputStream(manifestFile);
        try { out.write(Json.write(manifest).getBytes("UTF-8")); out.getFD().sync(); } finally { out.close(); }
        save();
        return imported;
    }

    public Item addBytes(String type, String kind, String name, String tags, byte[] data, String ext, String source) throws IOException {
        File tmp = File.createTempFile("incoming", ext, dir);
        FileOutputStream o = new FileOutputStream(tmp);
        o.write(data);
        o.close();
        return add(type, kind, name, tags, tmp, ext, source);
    }

    public synchronized void remove(Item it) {
        if (it.builtIn) return;
        items.remove(it);
        new File(it.path).delete();
        sidecar(it).delete();
        save();
    }

    public synchronized void rename(Item it, String name) {
        it.name = name;
        save();
    }

    /** Items of a type (and kind, if given), best matches for the query first. */
    public synchronized List<Item> find(String type, String kind, String query) {
        List<Item> out = new ArrayList<Item>();
        final String q = query == null ? "" : Txt.norm(query);
        for (Item it : items) {
            if (!it.type.equals(type)) continue;
            if (kind != null && kind.length() > 0 && !kind.equals(it.kind)) continue;
            out.add(it);
        }
        if (q.length() > 0) {
            java.util.Collections.sort(out, new java.util.Comparator<Item>() {
                public int compare(Item a, Item b) { return score(b, q) - score(a, q); }
            });
        }
        return out;
    }

    static int score(Item it, String q) {
        String t = Txt.norm(it.name + " " + it.tags + " " + it.kind);
        int s = it.builtIn ? 0 : 1;
        for (String w : q.split("\\s+")) if (w.length() >= 2 && t.contains(w)) s += 10 + w.length();
        for (String w : t.split("[\\s,]+")) if (w.length() >= 3 && q.contains(w)) s += 5;
        return s;
    }

    public synchronized Item byId(String id) {
        if (id == null) return null;
        for (Item it : items) if (it.id.equals(id)) return it;
        return null;
    }

    /** All sounds (built-in + user's) for the mixer. User sounds get the user's tags as keywords. */
    public SoundLib soundLib() {
        SoundLib lib = new SoundLib(AudioIO.decoder(ctx));
        try { lib.addIndex(Project.readAsset(ctx.getAssets(), "sounds/index.json"), "asset:sounds/"); } catch (IOException ignored) {}
        for (Item it : items) {
            if (it.builtIn || !it.type.equals(SOUND)) continue;
            SoundLib.Entry e = new SoundLib.Entry();
            e.path = it.path;
            e.type = it.kind.length() > 0 ? it.kind : "sfx";
            if (e.type.equals("voices")) e.type = "amb";
            e.title = it.label();
            e.user = true;
            // the name and words the user gave (or the AI heard), in English and Hindi; the offline guess
            // only when nothing else describes the sound
            String words = meaningful(it.name + "," + it.tags) + (it.meta("ai") != null ? "," + it.meta("ai") : "");
            if (words.replace(",", "").trim().length() < 3 && it.meta("sl") != null) words += "," + it.meta("sl");
            e.words = com.tarun.kahani.core.SoundWords.expand(words).toArray(new String[0]);
            try { e.seconds = Float.parseFloat(it.meta("sec")); } catch (Exception ignored) {}
            lib.entries.add(e);
        }
        return lib;
    }

    /** Opens a library picture as a stream (assets or files). */
    public InputStream open(Item it) throws IOException {
        if (it.path.startsWith("asset:")) return ctx.getAssets().open(it.path.substring(6));
        return new java.io.FileInputStream(it.path);
    }
}

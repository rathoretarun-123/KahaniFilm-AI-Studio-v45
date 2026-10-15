package com.tarun.kahani;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.PoseSense;
import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Loads the committed semantic reference manifest into the same Art model used by the production renderer. */
final class ReferenceArtLoader {
    private ReferenceArtLoader() {}

    static Art load(String filmId, Story story, File assetsRoot) throws IOException {
        File manifestFile = new File(assetsRoot, "film_library/" + filmId + "/manifest.json");
        if (!manifestFile.isFile()) throw new IOException("Missing semantic reference manifest: " + manifestFile);
        Object parsed = Json.parseLoose(new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8));
        if (!(parsed instanceof Map)) throw new IOException("Invalid reference manifest: " + manifestFile);
        Object rawUploads = ((Map<?, ?>) parsed).get("uploads");
        if (!(rawUploads instanceof List)) throw new IOException("Reference manifest has no uploads list: " + manifestFile);
        List<Map<String, Object>> uploads = new ArrayList<Map<String, Object>>();
        for (Object row : (List<?>) rawUploads) {
            if (row instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> item = (Map<String, Object>) row;
                uploads.add(item);
            }
        }

        File referenceRoot = new File(System.getProperty("kahani.referenceImages", "build/reference_images"));
        StringBuilder text = new StringBuilder();
        Set<String> characters = new HashSet<String>();
        Set<String> views = new HashSet<String>();
        Set<String> props = new HashSet<String>();
        Map<String, Map<String, Object>> locations = new LinkedHashMap<String, Map<String, Object>>();
        Map<String, Integer> poseSheetCounts = new HashMap<String, Integer>();
        List<String> reviewNotes = new ArrayList<String>();
        PngLoader loader = new PngLoader(referenceRoot);
        // v46: the reviewed scene plan and canonical fronts in the manifest (scene_plan) beat the generic rules below
        Map<String, Object> scenePlan = Json.obj(parsed, "scene_plan");
        Map<String, Object> canonicalFronts = scenePlan == null ? null : Json.obj(scenePlan, "canonical_fronts");

        for (Map<String, Object> row : uploads) {
            if (!bool(row, "auto_assign")) continue;
            String role = str(row, "role");
            String entityId = str(row, "entity_id");
            String label = str(row, "entity_label");
            String kind = str(row, "library_kind");
            String view = str(row, "view");
            boolean sheet = bool(row, "source_sheet");
            String image = imagePath(referenceRoot, filmId, str(row, "original_filename"));
            if (label.length() == 0 || entityId.length() == 0 || !new File(image).isFile()) continue;

            if ("character_reference".equals(role) && "person".equals(kind) && "front".equals(view) && !sheet) {
                String canonical = canonicalFronts == null ? "" : str(canonicalFronts, entityId);
                boolean other = canonical.length() > 0 && !new File(canonical).getName().equals(new File(str(row, "original_filename")).getName());
                if (!other && ScriptParser.resolve(story, label) != null && characters.add(label.toLowerCase(Locale.ROOT))) {
                    text.append("char|").append(label).append("|").append(image).append('\n');
                }
            } else if ("character_reference".equals(role) && "view".equals(kind) && !sheet
                    && ("back".equals(view) || "side".equals(view))) {
                if (ScriptParser.resolve(story, label) != null) {
                    String key = label.toLowerCase(Locale.ROOT) + "|" + view;
                    if (views.add(key)) text.append("view|").append(label).append("|").append(view).append("|").append(image).append('\n');
                }
            } else if (kind.equals("place") && (role.equals("location_reference") || role.equals("location_sheet"))) {
                Map<String, Object> previous = locations.get(entityId);
                if (previous == null || (bool(previous, "source_sheet") && !sheet)) locations.put(entityId, row);
            } else if (kind.equals("object") && role.startsWith("prop_sheet") && props.add(entityId)) {
                String propImage = sheet ? cropPanel(image, referenceRoot, filmId, entityId, 0) : image;
                text.append("objview|").append(label).append("|").append(propImage).append('\n');
            }

            boolean poseSheet = "character_pose_sheet".equals(role);
            boolean expressionSheet = "character_expression_sheet".equals(role);
            if ((poseSheet || expressionSheet) && sheet && "person".equals(kind)
                    && ScriptParser.resolve(story, label) != null) {
                int cap = expressionSheet ? 1 : 2;
                String poseKey = entityId + "|" + role;
                Integer seen = poseSheetCounts.get(poseKey);
                int used = seen == null ? 0 : seen;
                if (used < cap) {
                    poseSheetCounts.put(poseKey, used + 1);
                    for (int panel = 0; panel < 6; panel++) {
                        String poseImage;
                        try { poseImage = cropPanel(image, referenceRoot, filmId, entityId, panel); }
                        catch (IOException e) { reviewNotes.add("Pose sheet could not be split: " + label + " / " + str(row, "original_filename")); continue; }
                        int[] decoded = loader.decode(poseImage, 700);
                        if (decoded == null || decoded.length < 3) { reviewNotes.add("Pose panel could not be decoded: " + poseImage); continue; }
                        int w = decoded[0], h = decoded[1];
                        int[] pixels = new int[w * h];
                        System.arraycopy(decoded, 2, pixels, 0, pixels.length);
                        Cutout.Result cutout = Cutout.process(pixels, w, h, false);
                        PoseSense.Tag tag = PoseSense.tag(cutout, 0, false);
                        if (tag == null || tag.unsure()) {
                            reviewNotes.add("Pose panel needs human confirmation: " + label + " / " + str(row, "original_filename") + " panel " + panel);
                            continue;
                        }
                        text.append("pose|").append(label).append("|").append(poseImage).append("|")
                                .append(tag.angle).append("|").append(tag.pose).append("|").append(tag.emotion)
                                .append("|1|0|0|0|0|0|0|0|0\n");
                    }
                }
            }
        }

        String[] scenePlanDefault = "aakhiri_chai".equals(filmId)
                ? new String[]{"tea_stall", "rainy_lane", "tea_stall", "old_delhi_lane", "tea_stall"}
                : new String[]{"old_post_office", "old_post_office_lane", "tree_shade", "village_lane", "old_post_office"};
        Map<String, Integer> panels = new HashMap<String, Integer>();
        Map<Integer, Map<String, Object>> reviewed = new HashMap<Integer, Map<String, Object>>();
        if (scenePlan != null) for (Object o : Json.arr(scenePlan, "scenes")) if (o instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> rs = (Map<String, Object>) o;
            reviewed.put((int) Json.num(rs, "scene", -1), rs);
        }
        for (int i = 0; i < story.scenes.size(); i++) {
            Map<String, Object> rs = reviewed.get(i + 1);
            if (rs != null) {
                String image = imagePath(referenceRoot, filmId, str(rs, "original_filename"));
                if (!new File(image).isFile()) throw new IOException("Reviewed scene reference missing: " + image);
                int panel = (int) Json.num(rs, "panel", -1);
                if (panel >= 0) image = cropPanel(image, referenceRoot, filmId, str(rs, "entity_id"), panel);
                text.append("scene|").append(i + 1).append("|").append(image).append('\n');
                continue;
            }
            String entityId = scenePlanDefault[i % scenePlanDefault.length];
            Map<String, Object> location = locations.get(entityId);
            if (location == null && !locations.isEmpty()) location = locations.values().iterator().next();
            if (location == null) throw new IOException("No reviewed location references found for " + filmId);
            String image = imagePath(referenceRoot, filmId, str(location, "original_filename"));
            if (bool(location, "source_sheet")) {
                int panel = panels.containsKey(entityId) ? panels.get(entityId) : 0;
                panels.put(entityId, (panel + 1) % 6);
                image = cropPanel(image, referenceRoot, filmId, entityId, panel);
            }
            text.append("scene|").append(i + 1).append("|").append(image).append('\n');
        }

        Art art = Art.fromManifest(text.toString(), story, loader);
        if (art == null) throw new IOException("Art.fromManifest returned null for " + filmId);
        art.notes.addAll(reviewNotes);
        Object rawMissing = ((Map<?, ?>) parsed).get("missing_assets");
        if (rawMissing instanceof List) for (Object row : (List<?>) rawMissing) if (row instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> missing = (Map<String, Object>) row;
            if (bool(missing, "review_required")) {
                art.notes.add("MISSING_REFERENCE: " + str(missing, "entity_label") + " — " + str(missing, "reason"));
            }
        }
        return art;
    }

    /**
     * The converted PNG of an upload: the bundled still keeps the packet's path as a "__"-joined prefix
     * ("reference_pack__…__images__<name>.webp" → "…__images__<name>.png"), so the file is found by its last
     * "__" segment, never by a guessed prefix.
     */
    private static String imagePath(File root, String filmId, String originalFilename) {
        String name = new File(originalFilename).getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) name = name.substring(0, dot);
        File dir = new File(root, filmId);
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            String stem = f.getName();
            if (!stem.endsWith(".png")) continue;
            stem = stem.substring(0, stem.length() - 4);
            int sep = stem.lastIndexOf("__");
            if ((sep >= 0 ? stem.substring(sep + 2) : stem).equals(name)) return f.getAbsolutePath();
        }
        return new File(dir, name + ".png").getAbsolutePath();
    }

    private static String cropPanel(String source, File root, String filmId, String entityId, int panel) throws IOException {
        File dir = new File(root, "crops");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create crop directory: " + dir);
        String sourceToken = new File(source).getName().replaceAll("\\.png$", "");
        File out = new File(dir, filmId + "_" + entityId + "_" + sourceToken + "_" + panel + ".png");
        if (out.isFile() && out.length() > 0) return out.getAbsolutePath();
        BufferedImage sheet = ImageIO.read(new File(source));
        if (sheet == null) throw new IOException("Cannot decode reference sheet: " + source);
        int cols = 3, rows = 2, col = panel % cols, row = panel / cols;
        int x = col * sheet.getWidth() / cols, y = row * sheet.getHeight() / rows;
        int right = (col + 1) * sheet.getWidth() / cols, bottom = (row + 1) * sheet.getHeight() / rows;
        BufferedImage crop = sheet.getSubimage(x, y, Math.max(1, right - x), Math.max(1, bottom - y));
        ImageIO.write(crop, "png", out);
        return out.getAbsolutePath();
    }

    private static String str(Map<String, Object> row, String key) {
        Object value = row == null ? null : row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> row, String key) {
        return Boolean.parseBoolean(str(row, key));
    }

    private static final class PngLoader implements Art.Loader {
        private final File root;
        PngLoader(File root) { this.root = root; }

        private BufferedImage read(String name, int maxSide) {
            try {
                File file = new File(name);
                if (!file.isAbsolute()) file = new File(root, name);
                BufferedImage source = ImageIO.read(file);
                if (source == null) return null;
                int longest = Math.max(source.getWidth(), source.getHeight());
                if (maxSide <= 0 || longest <= maxSide) return source;
                float scale = maxSide / (float) longest;
                int w = Math.max(1, Math.round(source.getWidth() * scale));
                int h = Math.max(1, Math.round(source.getHeight() * scale));
                BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(source, 0, 0, w, h, null);
                g.dispose();
                return scaled;
            } catch (IOException e) {
                return null;
            }
        }

        @Override public int[] decode(String name, int maxSide) {
            BufferedImage image = read(name, maxSide);
            if (image == null) return null;
            int w = image.getWidth(), h = image.getHeight();
            int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
            int[] out = new int[pixels.length + 2];
            out[0] = w; out[1] = h;
            System.arraycopy(pixels, 0, out, 2, pixels.length);
            return out;
        }

        @Override public Object create(int[] argb, int w, int h) {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, w, h, argb, 0, w);
            return image;
        }

        @Override public Object load(String name, int maxSide) { return read(name, maxSide); }
        @Override public int width(Object img) { return ((BufferedImage) img).getWidth(); }
        @Override public int height(Object img) { return ((BufferedImage) img).getHeight(); }
    }
}

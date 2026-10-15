package com.tarun.kahani;

import com.tarun.kahani.core.Art;
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
        Map<String, Map<String, Object>> locations = new HashMap<String, Map<String, Object>>();

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
                if (ScriptParser.resolve(story, label) != null && characters.add(label.toLowerCase(Locale.ROOT))) {
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
        }

        String[] scenePlan = "aakhiri_chai".equals(filmId)
                ? new String[]{"tea_stall", "rainy_lane", "tea_stall", "old_delhi_lane", "tea_stall"}
                : new String[]{"old_post_office", "old_post_office_lane", "tree_shade", "village_lane", "old_post_office"};
        Map<String, Integer> panels = new HashMap<String, Integer>();
        for (int i = 0; i < story.scenes.size(); i++) {
            String entityId = scenePlan[i % scenePlan.length];
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

        Art art = Art.fromManifest(text.toString(), story, new PngLoader(referenceRoot));
        if (art == null) throw new IOException("Art.fromManifest returned null for " + filmId);
        return art;
    }

    private static String imagePath(File root, String filmId, String originalFilename) {
        String prefix = "aakhiri_chai".equals(filmId) ? "images__" : "stills__";
        String name = new File(originalFilename).getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) name = name.substring(0, dot);
        return new File(new File(root, filmId), prefix + name + ".png").getAbsolutePath();
    }

    private static String cropPanel(String source, File root, String filmId, String entityId, int panel) throws IOException {
        File dir = new File(root, "crops");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create crop directory: " + dir);
        File out = new File(dir, filmId + "_" + entityId + "_" + panel + ".png");
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

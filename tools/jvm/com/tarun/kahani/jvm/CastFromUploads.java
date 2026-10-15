package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Angles;
import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.PoseSense;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * v47: a folder of a filmmaker's uploads turned into the studio's own manifest (cast.txt) the way the phone does it,
 * so a new story can be rendered on the desktop from its real pictures: figure sheets are split by the studio's
 * splitter and every piece is pose-tagged (PoseSense) into a pose line; place and prop sheets are cut at their gutters
 * (Angles.sheetPanels); single pictures pass through. Nothing is invented: a piece the tagger is unsure of is listed in
 * review.txt instead of being cast.
 *
 * Usage: CastFromUploads plan.txt outDir
 * plan lines (paths relative to the plan's folder):
 *   char|label|file                 the character's front picture
 *   view|label|angle|file           another angle of the character (90 side, 180 back, -45 three-quarter)
 *   figure|label|file               one cut-out picture of the character in a pose
 *   figsheet|label|file             a sheet of the character's poses, angles or expressions
 *   prop|label|file[|panel]         a thing (a sheet's panel, in reading order from 0)
 *   place|n|file[|panel]            scene n's place picture
 *   title|file  end|file            the title page and the end page (their own lettering kept)
 */
public final class CastFromUploads {
    public static void main(String[] a) throws Exception {
        File plan = new File(a[0]), out = new File(a[1]);
        File base = plan.getAbsoluteFile().getParentFile();
        out.mkdirs();
        List<String> cast = new ArrayList<String>(), review = new ArrayList<String>();
        for (String raw : new String(Files.readAllBytes(plan.toPath()), StandardCharsets.UTF_8).split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\\|");
            String kind = f[0];
            if (kind.equals("char")) cast.add("char|" + f[1] + "|" + png(base, f[2], out));
            else if (kind.equals("view")) cast.add("view|" + f[1] + "|" + f[2] + "|" + png(base, f[3], out));
            else if (kind.equals("title") || kind.equals("end")) cast.add(kind + "|" + png(base, f[1], out) + "|0");
            else if (kind.equals("figure") || kind.equals("figsheet")) {
                BufferedImage im = ImageIO.read(new File(base, f[2]));
                int w = im.getWidth(), h = im.getHeight();
                int[] px = im.getRGB(0, 0, w, h, null, 0, w);
                List<Angles.Piece> pieces;
                if (kind.equals("figsheet")) pieces = Angles.figures(Angles.split(px, w, h), w, h);
                else { Angles.Piece p = new Angles.Piece(); p.px = px; p.w = w; p.h = h; pieces = new ArrayList<Angles.Piece>(); pieces.add(p); }
                int k = 0;
                for (Angles.Piece p : pieces) {
                    k++;
                    Cutout.Result r = Cutout.process(p.px.clone(), p.w, p.h, false);
                    PoseSense.Tag t = PoseSense.tag(r, 0, false);
                    String name = stem(f[2]) + "_" + k + ".png";
                    BufferedImage o = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_ARGB);
                    o.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
                    ImageIO.write(o, "png", new File(out, name));
                    if (t == null || t.unsure()) { review.add("unsure pose, not cast: " + f[1] + " " + name); continue; }
                    cast.add("pose|" + f[1] + "|" + name + "|" + (int) t.angle + "|" + t.pose + "|" + t.emotion + "|1|0|0|0|0|0|0|0|0");
                }
                if (pieces.isEmpty()) review.add("no figure found in " + f[2]);
            } else if (kind.equals("dress") || kind.equals("look")) {
                cast.add(kind + "|" + f[1] + "|" + f[2] + "|" + png(base, f[3], out));
            } else if (kind.equals("prop") || kind.equals("place")) {
                int panel = f.length > 3 ? Integer.parseInt(f[3].trim()) : -1;
                String file = panel >= 0 ? panel(base, f[2], panel, out, review) : png(base, f[2], out);
                if (kind.equals("prop")) cast.add("shot||" + f[1] + "|" + file + "|object");
                else cast.add("scene|" + f[1] + "|" + file);
            } else review.add("unknown plan line: " + line);
        }
        Files.write(new File(out, "cast.txt").toPath(), (String.join("\n", cast) + "\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(out, "review.txt").toPath(), (String.join("\n", review) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println(cast.size() + " manifest lines, " + review.size() + " review notes -> " + out);
        for (String s : review) System.out.println("  " + s);
    }

    static String stem(String f) { String n = new File(f).getName(); int d = n.lastIndexOf('.'); return d > 0 ? n.substring(0, d) : n; }

    /** The picture as a PNG in the output folder (a cut-out keeps its transparency; a jpg stays what it is). */
    static String png(File base, String file, File out) throws Exception {
        BufferedImage im = ImageIO.read(new File(base, file));
        String name = stem(file) + ".png";
        ImageIO.write(im, "png", new File(out, name));
        return name;
    }

    /** Panel n of a sheet, cut at the sheet's gutters; the whole picture when the sheet has none (noted). */
    static String panel(File base, String file, int panel, File out, List<String> review) throws Exception {
        BufferedImage im = ImageIO.read(new File(base, file));
        int w = im.getWidth(), h = im.getHeight();
        int[] px = im.getRGB(0, 0, w, h, null, 0, w);
        List<int[]> panels = Angles.sheetPanels(px, w, h);
        String name = stem(file) + "_p" + panel + ".png";
        if (panels == null || panel >= panels.size()) {
            // a sheet of cut-outs on black has no gutters of its own: its pieces are the panels
            List<Angles.Piece> pieces = Angles.order(Angles.split(px, w, h, false), h);
            if (pieces.size() > panel) {
                Angles.Piece p = pieces.get(panel);
                BufferedImage o = new BufferedImage(p.w, p.h, BufferedImage.TYPE_INT_ARGB);
                o.setRGB(0, 0, p.w, p.h, p.px, 0, p.w);
                ImageIO.write(o, "png", new File(out, name));
                return name;
            }
            review.add("no panel " + panel + " in " + file + ": the whole picture is used");
            return png(base, file, out);
        }
        int[] b = panels.get(panel);
        ImageIO.write(im.getSubimage(b[0], b[1], b[2] - b[0], b[3] - b[1]), "png", new File(out, name));
        return name;
    }
}

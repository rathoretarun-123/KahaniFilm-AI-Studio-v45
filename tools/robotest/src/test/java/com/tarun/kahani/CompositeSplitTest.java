package com.tarun.kahani;

import static org.junit.Assert.*;

import com.tarun.kahani.core.Angles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Test;

/**
 * v47.9 (the user: "while splitting you do not split images of combined characters, or characters with object — they
 * are created as per story"): a sheet of one girl's six views is split; the two sisters side by side, and a story-board
 * sheet of the two of them on the sofa, are kept whole. Made from the user's own pictures.
 */
public class CompositeSplitTest {
    static final String PACK = "../../docs/reference/user_uploads/sofe_wali_jadu_ki_baaten/";
    static final String[] VIEWS = {"front", "back", "side_facing_left", "side_facing_right", "three_quarter_facing_left", "three_quarter_facing_right"};

    static int[] pixels(BufferedImage im) { return im.getRGB(0, 0, im.getWidth(), im.getHeight(), null, 0, im.getWidth()); }

    static BufferedImage fit(BufferedImage im, int w, int h) {
        float sc = Math.min(w / (float) im.getWidth(), h / (float) im.getHeight());
        BufferedImage o = new BufferedImage(Math.round(im.getWidth() * sc), Math.round(im.getHeight() * sc), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = o.createGraphics();
        g.setColor(Color.MAGENTA);                       // the user's pictures are on magenta (transparent there)
        g.fillRect(0, 0, o.getWidth(), o.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(im, 0, 0, o.getWidth(), o.getHeight(), null);
        g.dispose();
        return o;
    }

    static List<Angles.Piece> pieces(BufferedImage sheet) {
        int w = sheet.getWidth(), h = sheet.getHeight();
        return Angles.figures(Angles.split(pixels(sheet), w, h), w, h);
    }

    static BufferedImage turnaround(String who) throws Exception {
        int cw = 400, chh = 600;
        BufferedImage sheet = new BufferedImage(cw * 3, chh * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < 6; i++) {
            BufferedImage v = fit(ImageIO.read(new File(PACK + "characters/" + who + "/" + VIEWS[i] + ".png")), cw - 20, chh - 20);
            g.drawImage(v, (i % 3) * cw + (cw - v.getWidth()) / 2, (i / 3) * chh + (chh - v.getHeight()) / 2, null);
        }
        g.dispose();
        return sheet;
    }

    @Test public void aSheetOfOneGirlsViewsIsSplit() throws Exception {
        for (String who : new String[]{"older_girl", "toddler"}) {
            List<Angles.Piece> p = pieces(turnaround(who));
            assertEquals(who + ": six views", 6, p.size());
            assertFalse(who + ": views of one character are not a group", Angles.composite(p, false));
        }
    }

    @Test public void theTwoSistersTogetherAreKeptWhole() throws Exception {
        BufferedImage a = fit(ImageIO.read(new File(PACK + "characters/older_girl/front.png")), 300, 700);
        BufferedImage b = fit(ImageIO.read(new File(PACK + "characters/toddler/front.png")), 300, 560);
        BufferedImage pic = new BufferedImage(a.getWidth() + b.getWidth() + 200, 740, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pic.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, pic.getWidth(), pic.getHeight());
        g.drawImage(a, 40, 20, null);
        g.drawImage(b, a.getWidth() + 160, 20 + a.getHeight() - b.getHeight(), null);
        g.dispose();
        List<Angles.Piece> p = pieces(pic);
        assertEquals(2, p.size());
        assertTrue("an 11-year-old and her 7-year-old sister are two people, not two views", Angles.composite(p, false));
    }

    /** The story-board sheets whose moments are wide (the two on the sofa, seen whole) are not split into figures. */
    @Test public void storyBoardMomentsAreKeptWhole() throws Exception {
        for (int i = 1; i <= 3; i++) {
            BufferedImage sheet = ImageIO.read(new File(PACK + "story_board/sheet_0" + i + ".png"));
            BufferedImage rgb = fit(sheet, 1400, 1400);
            List<Angles.Piece> p = pieces(rgb);
            if (p.size() < 2) continue;
            assertTrue("story-board sheet " + i + " (the girls on the sofa) is not split into figures", Angles.composite(p, false));
        }
    }
}

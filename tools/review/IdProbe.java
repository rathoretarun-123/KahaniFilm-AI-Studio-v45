import com.tarun.kahani.core.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
/** Scores pictures against a script's characters as the app does offline (colour * 0.75, traits blended). */
public class IdProbe {
  static int[] px(BufferedImage src, int side, int[] wh) {
    float k = Math.min(1f, side / (float) Math.max(src.getWidth(), src.getHeight()));
    int w = Math.max(1, Math.round(src.getWidth() * k)), h = Math.max(1, Math.round(src.getHeight() * k));
    BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    java.awt.Graphics2D g = b.createGraphics(); g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(src, 0, 0, w, h, null); g.dispose();
    int[] p = new int[w * h]; b.getRGB(0, 0, w, h, p, 0, w); wh[0] = w; wh[1] = h; return p;
  }
  public static void main(String[] a) throws Exception {
    Story st = ScriptParser.parse(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8"));
    for (int i = 1; i < a.length; i++) {
      BufferedImage src = javax.imageio.ImageIO.read(new File(a[i]));
      int[] wh = new int[2];
      int[] p = px(src, 256, wh);
      PicSense.Info in = PicSense.analyse(p, wh[0], wh[1]);
      PicSense.Traits tr = null;
      if (in.figure) { int[] bp = px(src, 420, wh); tr = PicSense.traits(Cutout.process(bp, wh[0], wh[1])); }
      System.out.println("== " + new File(a[i]).getName() + " figure=" + in.figure + " skin=" + in.skin + " traits=" + (tr == null ? "-" : tr.toMeta()));
      List<String> rows = new ArrayList<String>();
      for (Story.CharacterDef c : st.characters) {
        float col = PicSense.matchCharacter(in, c), all = PicSense.matchCharacter(in, tr, c);
        float[] tm = tr == null ? new float[]{0, 0} : PicSense.traitMatch(tr, c);
        rows.add(String.format(Locale.US, "%.2f  col=%.2f trait=%.2f/w%.1f  %s", all * 0.75f, col, tm[0], tm[1], c.shown()));
      }
      Collections.sort(rows, Collections.reverseOrder());
      for (String r : rows.subList(0, Math.min(5, rows.size()))) System.out.println("   " + r);
    }
  }
}

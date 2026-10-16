import com.tarun.kahani.core.*;
public class P2 { public static void main(String[] a) throws Exception {
  String t = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8");
  Story s = ScriptParser.parse(t);
  System.out.println("title=" + s.title);
  for (Story.CharacterDef c : s.characters) System.out.println("char: [" + c.displayName + "] age=" + c.age + " | " + c.description.replace('\n',' ').substring(0, Math.min(60, c.description.length())));
  for (Story.PlaceDef p : s.things) System.out.println("thing: [" + p.name + "] intangible=" + Bible.intangible(p.name));
  for (Story.PlaceDef p : s.places) System.out.println("place: [" + p.name + "] intangible=" + Bible.intangible(p.name));
  System.out.println("listedPlaces=" + Bible.listedPlaces(s).size());
  for (String[] lp : Bible.listedPlaces(s)) System.out.println("   asked place: " + lp[0]);
  for (Story.Scene sc : s.scenes) { System.out.println("scene " + sc.number + " [" + sc.title + "] setting=" + sc.setting.substring(0, Math.min(50, sc.setting.length()))); for (Story.Beat b : sc.beats) System.out.println("   " + (b.speaker == null ? "-" : b.speaker.displayName) + " manner=[" + (b.manner == null ? "" : b.manner.substring(0, Math.min(30, b.manner.length()))) + "] " + b.text.substring(0, Math.min(60, b.text.length()))); }
  for (String w : s.warnings) System.out.println("warn: " + w);
}}

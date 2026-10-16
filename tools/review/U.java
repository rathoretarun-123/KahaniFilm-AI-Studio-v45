public class U { public static void main(String[] a) throws Exception {
  String t = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8");
  java.lang.reflect.Method m = Class.forName("com.tarun.kahani.core.ScriptParser").getDeclaredMethod("userFormat", String.class); m.setAccessible(true);
  for (String l : ((String) m.invoke(null, t)).split("\n")) System.out.println(l.length() > 110 ? l.substring(0, 110) : l);
}}

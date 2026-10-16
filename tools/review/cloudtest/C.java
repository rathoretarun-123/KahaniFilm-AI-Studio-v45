import com.tarun.kahani.core.Cloud;
public class C { public static void main(String[] a) throws Exception {
  Cloud c = new Cloud(); c.geminiBase = "http://127.0.0.1:18765/gem"; c.openaiBase = "http://127.0.0.1:18765/oai"; c.pollinationsText = "http://127.0.0.1:18765/none";
  c.geminiKey = "AIza-test-0123456789abcdefghijk"; c.openaiKey = "sk-test-0123456789abcdefghijk";
  System.out.println("1 gemini model retired -> " + c.ask(null, "hi", false) + " | model now " + c.textModel);
  new Cloud().request("POST", "http://127.0.0.1:18765/mode/down", "application/json", "{}".getBytes());
  long t0 = System.currentTimeMillis();
  System.out.println("2 gemini down -> " + c.ask(null, "hi", false) + " in " + (System.currentTimeMillis() - t0) + " ms; lastError=" + c.lastError);
  System.out.println("3 vision, gemini down -> " + c.vision("what?", new byte[]{1,2,3}));
}}

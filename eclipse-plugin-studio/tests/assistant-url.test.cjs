// Run with: node eclipse-plugin-studio/tests/assistant-url.test.cjs <httpclient.jar> <httpcore.jar>
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const source = fs.readFileSync(path.join(__dirname,
  '../src/com/twinsoft/convertigo/eclipse/views/assistant/AssistantView.java'), 'utf8');
function method(name) {
  const start = source.indexOf('\tprivate static String ' + name + '(');
  const end = source.indexOf('\n\t}\n', start);
  if (start < 0 || end < 0) throw new Error('Missing method: ' + name);
  return source.slice(start, end + 4);
}
const classpath = process.argv.slice(2).join(path.delimiter);
if (!classpath) throw new Error('Pass the existing Apache HttpClient and HttpCore JAR paths');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'assistant-url-test-'));
try {
  // Compile the production methods; stub only the Eclipse host and authentication service.
  fs.writeFileSync(path.join(dir, 'AssistantUrlTest.java'), `
import java.net.URI;
import java.util.Objects;
import org.apache.http.client.utils.URIBuilder;
public class AssistantUrlTest {
  static final String STARTUP_URL = "https://assistant.convertigo.com/";
  static class StringUtils {
    static boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
    static String defaultIfBlank(String s, String fallback) { return isNotBlank(s) ? s : fallback; }
  }
  static class SwtUtils { static boolean dark = true; static boolean isDark() { return dark; } }
  static class ConvertigoPlugin {
    static String setting;
    static final String PREFERENCE_ASSISTANT_URL = "url";
    static String getProperty(String key) { return setting; }
    static String resolveStudioUrl(String url) { return url; }
    static void logStudioWarn(String message) { }
  }
  static class AdminView {
    static String getAuthenticatedUrl(String path) {
      return getLocalConvertigoUrl() + path + "#authToken=test-token";
    }
  }
  static String getLocalConvertigoUrl() { return "http://localhost:18082/convertigo"; }
  static boolean isLocalConvertigoUrl(String url, String base) { return url.startsWith(base + "/"); }
  ${method('addDarkThemeParameter')}
  ${method('removeAgentProfileParameters')}
  ${method('resolveAssistantStartupUrl')}
  static int count;
  static void check(String input, String expected) {
    String actual = addDarkThemeParameter(input);
    if (!expected.equals(actual)) throw new AssertionError(input + " -> " + actual + "; expected " + expected);
    count++;
  }
  public static void main(String[] args) throws Exception {
    String base = STARTUP_URL;
    check(null, base + "?dark-theme=true");
    check(" ", base + "?dark-theme=true");
    check(base, base + "?dark-theme=true");
    check(base + "#early-access-agent", base + "?dark-theme=true#early-access-agent");
    check(base + "?studio=1#early-access-agent", base + "?studio=1&dark-theme=true#early-access-agent");
    check(base + "?dark-theme=false#early-access-agent", base + "?dark-theme=false#early-access-agent");
    check(base + "?dark-theme=#fragment", base + "?dark-theme=#fragment");
    check(base + "?%64ark-theme=false#fragment", base + "?%64ark-theme=false#fragment");
    check(base + "?tag=a&tag=b", base + "?tag=a&tag=b&dark-theme=true");
    check(base + "?redirect=a%23b%26c#early-access-agent&x=a%26b", base + "?redirect=a%23b%26c&dark-theme=true#early-access-agent&x=a%26b");
    check(base + "#route?dark-theme=false", base + "?dark-theme=true#route?dark-theme=false");
    SwtUtils.dark = false;
    check(base + "#early-access-agent", base + "?dark-theme=false#early-access-agent");
    check("http://[invalid", "http://[invalid");
    SwtUtils.dark = true;
    ConvertigoPlugin.setting = base + "#early-access-agent";
    if (!resolveAssistantStartupUrl().equals(base + "?dark-theme=true#early-access-agent")) throw new AssertionError("Remote startup");
    String local = getLocalConvertigoUrl() + "/projects/lib_ConvertigoAssistant/DisplayObjects/mobile/";
    ConvertigoPlugin.setting = local + "?dark-theme=false&custom=a%26b#early-access-agent";
    if (!resolveAssistantStartupUrl().equals(local + "?dark-theme=false&custom=a%26b#authToken=test-token&early-access-agent")) throw new AssertionError("Local fragment or query lost");
    ConvertigoPlugin.setting = local;
    if (!resolveAssistantStartupUrl().equals(local + "?dark-theme=true#authToken=test-token")) throw new AssertionError("Local startup");
    System.out.println((count + 3) + " Assistant URL tests passed");
  }
}
`);
  execFileSync('javac', ['-cp', classpath, path.join(dir, 'AssistantUrlTest.java')], { stdio: 'inherit' });
  execFileSync('java', ['-cp', dir + path.delimiter + classpath, 'AssistantUrlTest'], { stdio: 'inherit' });
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}

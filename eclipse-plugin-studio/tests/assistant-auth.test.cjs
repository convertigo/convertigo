const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.join(__dirname, '../src/com/twinsoft/convertigo/eclipse');
const source = fs.readFileSync(path.join(root, 'views/assistant/AssistantView.java'), 'utf8');
const helper = fs.readFileSync(path.join(root, 'swt/C8oBrowserPostMessageHelper.java'), 'utf8');
const start = source.indexOf('\tprivate static boolean isTrustedAssistantAuthenticationUrl(');
const end = source.indexOf('\n\t}\n', start);
assert.ok(start >= 0 && end > start);
const method = source.slice(start, end + 4);
assert.match(helper, /new BrowserInterface\(frame\)/);
assert.match(helper, /mainFrame\(\)\.filter\(sourceFrame::equals\)\.isPresent\(\)/);
assert.match(source, /generation != assistantLoadGeneration/);
assert.match(source, /mainFrame\(\)\.filter\(frame::equals\)\.isPresent\(\)/);
assert.match(source, /!requestUrl\.equals\(frame\.browser\(\)\.url\(\)\)/);
assert.match(source, /window === window.top && window.location.href ===/);
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'assistant-auth-test-'));
try {
  fs.writeFileSync(path.join(dir, 'AssistantAuthTest.java'), `
import java.net.URI;
import java.util.Objects;
public class AssistantAuthTest {
  ${method}
  public static void main(String[] args) {
    String root = "http://localhost:18082/convertigo/projects/lib_ConvertigoAssistant/DisplayObjects/mobile/";
    String[] allowed = {root, root.substring(0, root.length() - 1), root + "path-to-xfirst?agentBridge=1#early-access-agent", root + "?dark-theme=false"};
    String[] denied = {
      "https://assistant.convertigo.com/", root.replace(":18082", ":18080"),
      root.replace("http:", "https:"), root.replace("localhost", "127.0.0.1"),
      root.replace("localhost", "localhost.evil.example"), root.replace("localhost", "user@localhost"),
      root.replace("lib_ConvertigoAssistant", "OtherProject"), root.replace("mobile/", "mobile-evil/"),
      root + "../other", root + "%2e%2e/other", root + "%2Fother", root + "%5cother", root + "a/../../other",
      "file:///tmp/assistant.html", "about:blank", "javascript:alert(1)", "http://[invalid", "//localhost:18082/convertigo/"
    };
    for (String url : allowed) if (!isTrustedAssistantAuthenticationUrl(url, root)) throw new AssertionError("Rejected " + url);
    for (String url : denied) if (isTrustedAssistantAuthenticationUrl(url, root)) throw new AssertionError("Trusted " + url);
    String https = root.replace("http:", "https:").replace(":18082", "");
    if (!isTrustedAssistantAuthenticationUrl(https.replace("localhost", "localhost:443"), https)) throw new AssertionError("Default port");
    System.out.println((allowed.length + denied.length + 1) + " Assistant authentication URL checks passed; frame and navigation guards checked");
  }
}`);
  execFileSync('javac', ['--release', '21', path.join(dir, 'AssistantAuthTest.java')], {stdio: 'inherit'});
  execFileSync('java', ['-cp', dir, 'AssistantAuthTest'], {stdio: 'inherit'});
} finally {
  fs.rmSync(dir, {recursive: true, force: true});
}

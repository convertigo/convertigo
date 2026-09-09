const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {execFileSync} = require('node:child_process');
const source = fs.readFileSync(path.join(__dirname, '../src/com/twinsoft/convertigo/eclipse/views/assistant/AssistantView.java'), 'utf8');
const start = source.indexOf('\t\thandler.onLoad(event -> {');
const end = source.indexOf('\n\t\t});', start);
assert.ok(start >= 0 && end > start);
const callback = source.slice(start, end + '\n\t\t});'.length);
const methodStart = source.indexOf('\tprivate void postAssistantInitWhenReady(');
const methodEnd = source.indexOf('\n\t}\n', methodStart);
const method = source.slice(methodStart, methodEnd + 4);
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'assistant-init-test-'));
try {
  fs.writeFileSync(path.join(dir, 'AssistantInitTest.java'), `
import java.util.*;
import java.util.function.Consumer;
public class AssistantInitTest {
  static boolean ui;
  static final Queue<Runnable> tasks = new ArrayDeque<>();
  static void checkThread() { if (!ui) throw new AssertionError("Invalid thread access"); }
  static class ConvertigoPlugin {
    static void asyncExec(Runnable r) { tasks.add(r); }
    static void logStudioWarn(String message) { }
  }
  record Frame(boolean isMain) {}
  record Event(Frame frame) {}
  static class Display {
    void timerExec(int delay, Runnable r) { checkThread(); tasks.add(r); }
  }
  static class Browser {
    boolean disposed;
    boolean isDisposed() { return disposed; }
    Display getDisplay() { checkThread(); return new Display(); }
  }
  static class JSONObject {
    void put(String key, String value) { }
    boolean has(String key) { return false; }
    String getString(String key) { return ""; }
  }
  static class Handler {
    Consumer<Event> listener;
    int messages;
    void onLoad(Consumer<Event> listener) { this.listener = listener; }
    void postMessage(JSONObject message) { checkThread(); messages++; }
  }
  Browser browser = new Browser();
  Handler handler = new Handler();
  JSONObject jsonMessage = new JSONObject();
  int assistantLoadGeneration;
  boolean ready;
  boolean isAssistantMessageBridgeReady() { checkThread(); return ready; }
  void install() { ${callback} }
  ${method}
  static void drainOne() { ui = true; tasks.remove().run(); ui = false; }
  public static void main(String[] args) {
    AssistantInitTest test = new AssistantInitTest();
    test.install();
    test.handler.listener.accept(new Event(new Frame(false)));
    if (!tasks.isEmpty()) throw new AssertionError("Iframe scheduled init");
    test.handler.listener.accept(new Event(new Frame(true)));
    if (test.assistantLoadGeneration != 0) throw new AssertionError("Render thread updated UI generation");
    drainOne();
    if (test.assistantLoadGeneration != 1 || tasks.size() != 1) throw new AssertionError("Missing retry");
    test.ready = true;
    drainOne();
    if (test.handler.messages != 1) throw new AssertionError("Init not delivered");
    test.handler.listener.accept(new Event(new Frame(true)));
    test.browser.disposed = true;
    drainOne();
    if (test.handler.messages != 1 || test.assistantLoadGeneration != 1) throw new AssertionError("Disposed view initialized");
    test.browser.disposed = false;
    ui = true;
    test.postAssistantInitWhenReady(0, 0);
    ui = false;
    if (test.handler.messages != 1) throw new AssertionError("Stale retry delivered init");
    System.out.println("Assistant init threading tests passed: main frame, SWT dispatch, retry, disposal, stale generation");
  }
}`);
  execFileSync('javac', ['--release', '21', path.join(dir, 'AssistantInitTest.java')], {stdio: 'inherit'});
  execFileSync('java', ['-cp', dir, 'AssistantInitTest'], {stdio: 'inherit'});
} finally {
  fs.rmSync(dir, {recursive: true, force: true});
}

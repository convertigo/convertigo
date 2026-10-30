// Compile and exercise the production notification queue/helper without building Eclipse.
// Run with a JDK 17+ on PATH: node eclipse-plugin-studio/tests/message-box-queue.test.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const source = fs.readFileSync(path.join(__dirname,
  '../src/com/twinsoft/convertigo/eclipse/ConvertigoPlugin.java'), 'utf8');
const start = source.indexOf('\t/** A modal SWT box pumps UI events:');
const end = source.indexOf('\n\tpublic static void configureDeployConfiguration()', start);
assert(start >= 0 && end > start, 'Production queue/helper not found');
function method(name) {
  const match = source.match(new RegExp(`\\tpublic static (?:void|int) ${name}\\([^]*?\\n\\t\\}`));
  assert(match, `Missing production method ${name}`);
  return match[0];
}
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'studio-message-box-test-'));
try {
  fs.writeFileSync(path.join(dir, 'MessageBoxTest.java'), `
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class MessageBoxTest {
  static int checks;
  static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
    checks++;
  }
  static void fails(Runnable action, String message) {
    try { action.run(); throw new AssertionError(message); }
    catch (IllegalStateException expected) { checks++; }
  }
  static class SWT {
    static final int OK = 1, ICON_ERROR = 2, ICON_WARNING = 4, ICON_INFORMATION = 8;
    static final int YES = 16, NO = 32, ICON_QUESTION = 64;
  }
  static class Shell { }
  record Shown(String message, int options) { }
  static class Ui {
    static final Queue<Runnable> events = new ConcurrentLinkedQueue<>();
    static final List<Shown> shown = new ArrayList<>();
    static Consumer<String> onOpen = message -> { };
    static int depth, maxDepth;
    static boolean failOpen;
    static int logs;
    static void pump() {
      for (Runnable event; (event = events.poll()) != null;) event.run();
    }
    static void reset() {
      check(events.isEmpty(), "previous queue drained");
      check(ConvertigoPlugin.messageBoxes.outstanding.isEmpty(), "no old message key retained");
      check(!ConvertigoPlugin.messageBoxes.scheduled, "no pending dispatch retained");
      shown.clear();
      onOpen = message -> { };
      depth = maxDepth = logs = 0;
      failOpen = false;
    }
  }
  static class MessageBox {
    final int options;
    String text, message;
    MessageBox(Shell shell, int options) { this.options = options; }
    void setText(String value) { text = value; }
    void setMessage(String value) { message = value; }
    int open() {
      if (Ui.failOpen) { Ui.failOpen = false; throw new IllegalStateException("disposed shell"); }
      check("Convertigo".equals(text), "dialog title preserved");
      Ui.shown.add(new Shown(message, options));
      Ui.maxDepth = Math.max(Ui.maxDepth, ++Ui.depth);
      try {
        Ui.onOpen.accept(message);
        // SWT MessageBox.open() runs its own event loop until it is dismissed.
        Ui.pump();
        return SWT.YES;
      } finally { Ui.depth--; }
    }
  }
  static class ConvertigoPlugin {
    static void asyncExec(Runnable run) { Ui.events.add(run); }
    static Shell getMainShell() { return new Shell(); }
    static void logException(Throwable error, String message, boolean dialog) {
      check(!dialog, "logging a display failure must not open another error box");
      Ui.logs++;
    }
    ${method('errorMessageBox')}
    ${method('warningMessageBox')}
    ${method('infoMessageBox')}
    ${method('questionMessageBox')}
    ${source.slice(start, end)}
  }

  static void burst(String message) throws Exception {
    var pool = Executors.newFixedThreadPool(16);
    try {
      var jobs = new ArrayList<Callable<Void>>();
      for (int i = 0; i < 64; i++) jobs.add(() -> {
        for (int j = 0; j < 1000; j++) ConvertigoPlugin.errorMessageBox(message);
        return null;
      });
      for (var future : pool.invokeAll(jobs, 10, TimeUnit.SECONDS)) future.get();
    } finally { pool.shutdownNow(); }
  }

  public static void main(String[] args) throws Exception {
    String unauthorized = "You are not authorized to access this db.";
    burst(unauthorized);
    check(Ui.events.size() == 1, "64,000 simultaneous FullSync notifications schedule one UI callback");
    check(Ui.shown.isEmpty(), "background notifications never open a dialog on the caller thread");
    Ui.onOpen = message -> {
      try { burst(unauthorized); } catch (Exception e) { throw new AssertionError(e); }
      check(Ui.events.isEmpty(), "64,000 active duplicates schedule no second modal callback");
    };
    Ui.pump();
    check(Ui.shown.size() == 1, "one authorization dialog for pending and active duplicates");
    check(Ui.maxDepth == 1, "modal event loop never nests notifications");
    check(Ui.shown.get(0).options() == (SWT.OK | SWT.ICON_ERROR), "error icon is preserved");
    check(ConvertigoPlugin.messageBoxes.outstanding.isEmpty(), "active dedup key released after closing");

    Ui.onOpen = message -> { };
    ConvertigoPlugin.errorMessageBox(unauthorized);
    Ui.pump();
    check(Ui.shown.size() == 2, "a later independent recurrence is visible after dismissal");

    Ui.reset();
    ConvertigoPlugin.errorMessageBox("A");
    ConvertigoPlugin.errorMessageBox("A");
    ConvertigoPlugin.warningMessageBox("B");
    ConvertigoPlugin.infoMessageBox("C");
    Ui.onOpen = message -> {
      if (Ui.shown.size() != 1) return;
      ConvertigoPlugin.errorMessageBox("A");
      ConvertigoPlugin.warningMessageBox("B");
      ConvertigoPlugin.warningMessageBox("A"); // Same text, different severity: keep it.
      ConvertigoPlugin.asyncExec(() -> {
        ConvertigoPlugin.errorMessageBox("A");
        ConvertigoPlugin.infoMessageBox("D");
      });
    };
    Ui.pump();
    check(Ui.maxDepth == 1, "different messages also stay flat during nested UI event loops");
    check(Ui.shown.equals(List.of(
      new Shown("A", SWT.OK | SWT.ICON_ERROR), new Shown("B", SWT.OK | SWT.ICON_WARNING),
      new Shown("C", SWT.OK | SWT.ICON_INFORMATION), new Shown("A", SWT.OK | SWT.ICON_WARNING),
      new Shown("D", SWT.OK | SWT.ICON_INFORMATION))), "distinct messages and severities keep FIFO order");

    Ui.reset();
    ConvertigoPlugin.infoMessageBox(null);
    ConvertigoPlugin.infoMessageBox("(null message)");
    ConvertigoPlugin.infoMessageBox("");
    Ui.pump();
    check(Ui.shown.equals(List.of(new Shown("(null message)", SWT.OK | SWT.ICON_INFORMATION),
      new Shown("", SWT.OK | SWT.ICON_INFORMATION))), "null normalization matches the displayed message; empty text is preserved");

    Ui.reset();
    ConvertigoPlugin.asyncExec(() -> {
      ConvertigoPlugin.errorMessageBox("UI thread sender");
      check(Ui.shown.isEmpty(), "UI callers also schedule, rather than opening inline");
    });
    Ui.pump();
    check(Ui.shown.size() == 1, "UI-thread notification is delivered");

    Ui.reset();
    Ui.failOpen = true;
    ConvertigoPlugin.errorMessageBox("bad shell");
    ConvertigoPlugin.infoMessageBox("next");
    Ui.pump();
    check(Ui.logs == 1 && Ui.shown.size() == 1, "display failure is logged and the next message is shown");
    ConvertigoPlugin.errorMessageBox("bad shell");
    Ui.pump();
    check(Ui.shown.size() == 2, "display failure does not poison the message key");

    Ui.reset();
    int answer = ConvertigoPlugin.questionMessageBox(new Shell(), "Confirm?");
    check(answer == SWT.YES && Ui.shown.size() == 1 && Ui.events.isEmpty(), "questions remain synchronous and keep their return code");
    check(Ui.shown.get(0).options() == (SWT.YES | SWT.NO | SWT.ICON_QUESTION), "question flags are unchanged");

    var callbacks = new ArrayDeque<Runnable>();
    var failDispatch = new AtomicBoolean(true);
    var delivered = new ArrayList<String>();
    var queue = new ConvertigoPlugin.MessageBoxQueue(run -> {
      if (failDispatch.get()) throw new IllegalStateException("disposed Display");
      callbacks.add(run);
    }, (message, options) -> delivered.add(message));
    fails(() -> queue.add("lost display", 1), "dispatch failure must be reported");
    check(queue.outstanding.isEmpty() && queue.pending.isEmpty() && !queue.scheduled, "dispatch failure clears all retained state");
    failDispatch.set(false);
    queue.add("lost display", 1);
    callbacks.remove().run();
    check(delivered.equals(List.of("lost display")), "queue can recover from a failed dispatch");

    delivered.clear();
    var failShow = new AtomicBoolean(true);
    var throwing = new ConvertigoPlugin.MessageBoxQueue(callbacks::add, (message, options) -> {
      if (failShow.getAndSet(false)) throw new IllegalStateException("show failed");
      delivered.add(message);
    });
    throwing.add("first", 1);
    throwing.add("second", 1);
    fails(callbacks.remove(), "show failure must reach the UI dispatcher");
    check(callbacks.size() == 1, "remaining notification is dispatched after show failure");
    callbacks.remove().run();
    throwing.add("first", 1);
    callbacks.remove().run();
    check(delivered.equals(List.of("second", "first")), "show failure preserves pending messages and releases active key");
    check(!throwing.scheduled && throwing.outstanding.isEmpty(), "successful drain leaves no scheduled work or message keys");

    System.out.println("Studio message boxes: " + checks + " checks passed (including two 64,000-notification bursts)");
  }
}
`);
  const jdk = process.env.JAVA_HOME;
  const binary = name => jdk ? path.join(jdk, 'bin', name) : name;
  execFileSync(binary('javac'), ['--release', '17', path.join(dir, 'MessageBoxTest.java')], { stdio: 'inherit' });
  execFileSync(binary('java'), ['-cp', dir, 'MessageBoxTest'], { stdio: 'inherit', timeout: 30000 });
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}

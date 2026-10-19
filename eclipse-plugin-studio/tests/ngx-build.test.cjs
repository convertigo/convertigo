// Compile the actual progress/parser classes without building or launching Eclipse.
// Run: node eclipse-plugin-studio/tests/ngx-build.test.cjs
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const source = fs.readFileSync(path.join(__dirname,
  '../src/com/twinsoft/convertigo/eclipse/editors/ngx/ApplicationComponentEditor.java'), 'utf8');
function method(name) {
  const start = source.indexOf('\tprivate static int ' + name + '(');
  const end = source.indexOf('\n\t}\n', start);
  if (start < 0 || end < 0) throw new Error('Missing method: ' + name);
  return source.slice(start, end + 4);
}
const start = source.indexOf('\tprivate static final class StandaloneBuildProgress {');
const end = source.indexOf('\n\tprivate static void finishBuildTask(', start);
if (start < 0 || end < 0) throw new Error('Missing production progress classes');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'ngx-build-test-'));
try {
  fs.writeFileSync(path.join(dir, 'BuildProgressTest.java'), `
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.IntConsumer;
public class BuildProgressTest {
  static final int TIME_TO_99_SECONDS = 60;
  ${method('computeTimeProgress')}
  ${source.slice(start, end)}
  static int checks;
  static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
    checks++;
  }
  public static void main(String[] args) throws Exception {
    AtomicInteger value = new AtomicInteger();
    AtomicInteger cycles = new AtomicInteger();
    var progress = new StandaloneBuildProgress(value::set, cycles::incrementAndGet);
    try {
      progress.onLine("❯ Building...", false);
      check(progress.onLine("Application bundle generation complete. [1.2 seconds]", false)
        == StandaloneBuildProgress.CycleResult.BUNDLED, "disk bundle completion is not success");
      check(value.get() == 99, "must wait below 100% for disk writes and npm hooks");
      check(progress.onLine("Output location: /tmp/mobile", false)
        == StandaloneBuildProgress.CycleResult.NONE, "output location is not success");
      check(progress.onLine("Watch mode enabled. Watching for file changes...", false)
        == StandaloneBuildProgress.CycleResult.NONE, "watch setup precedes writes");
      check(progress.onLine("An unhandled exception occurred: ERR_FS_CP_EINVAL", false)
        == StandaloneBuildProgress.CycleResult.FAILED, "late disk failure");
      progress.onLine("Changes detected. Rebuilding...", false);
      check(progress.onLine("Application bundle generation failed.", false)
        == StandaloneBuildProgress.CycleResult.FAILED, "recoverable compilation failure");
      progress.onLine("Changes detected. Rebuilding...", false);
      check(progress.onLine("Application bundle generation complete.", false)
        == StandaloneBuildProgress.CycleResult.BUNDLED, "watch can recover after compilation failure");
      check(value.get() == 99, "recovered watch cycle still awaits disk writes");
      check(cycles.get() >= 3, "watch progress restarts");
      check(progress.onLine("npm error post-build failed", false)
        == StandaloneBuildProgress.CycleResult.FAILED, "npm hook error");
      check(progress.onLine("npm ERR! code 1", false)
        == StandaloneBuildProgress.CycleResult.FAILED, "older npm error");
      check(progress.onLine("Application bundle generation complete.")
        == StandaloneBuildProgress.CycleResult.SUCCESS, "in-memory dev server remains supported");
      check(value.get() == 100, "dev server cycle completes");
      var tail = new BuildOutputTail();
      for (int i = 0; i < 10000; i++) tail.append("output line " + i);
      check(tail.tail.length() <= 16384, "bounded watch history");
      check(tail.failure(7).contains("output line 9999"), "last diagnostics retained");
      check(tail.failure(7).contains("code 7"), "exit code included");
      tail.append("x".repeat(100000));
      check(tail.tail.length() <= 16384, "individual huge lines are bounded");
      tail.clear();
      check(tail.tail.length() == 0, "history resets per cycle");
      // A real npm lifecycle: bundle log, followed by a failing post-build hook.
      for (String script : List.of("ok", "late", "post")) {
        var p = new ProcessBuilder(args[0], "run", script).directory(new File(args[1]))
          .redirectErrorStream(true).start();
        tail.clear();
        try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            tail.append(line);
            check(progress.onLine(line, false) != StandaloneBuildProgress.CycleResult.SUCCESS,
              "no success before npm exits, including post-build output");
          }
        }
        int code = p.waitFor();
        check(code == (script.equals("ok") ? 0 : script.equals("late") ? 7 : 9), "npm exit code: " + script);
        if (!script.equals("ok")) check(tail.failure(code).contains("FAILURE DETAIL"), "late diagnostic captured");
      }
      System.out.println("NGX build progress/output: " + checks + " checks passed");
    } finally { progress.shutdown(); }
  }
}
`);
  fs.writeFileSync(path.join(dir, 'package.json'), JSON.stringify({ scripts: {
    ok: 'node -e "console.log(\'Application bundle generation complete.\')"',
    late: 'node -e "console.log(\'Application bundle generation complete.\'); console.error(\'An unhandled exception occurred: FAILURE DETAIL\'); process.exit(7)"',
    post: 'node -e "console.log(\'Application bundle generation complete.\')"',
    postpost: 'node -e "console.error(\'FAILURE DETAIL in post-build\'); process.exit(9)"'
  }}));
  execFileSync('javac', ['--release', '17', path.join(dir, 'BuildProgressTest.java')], { stdio: 'inherit' });
  execFileSync('java', ['-cp', dir, 'BuildProgressTest', process.platform === 'win32' ? 'npm.cmd' : 'npm', dir], { stdio: 'inherit' });
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}

// Exercise the production build Job, stubbing only the Eclipse host (not npm).
// node eclipse-plugin-studio/tests/ngx-build-job.test.cjs
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const source = fs.readFileSync(path.join(__dirname,
  '../src/com/twinsoft/convertigo/eclipse/editors/ngx/ApplicationComponentEditor.java'), 'utf8');
function section(start, end) {
  const a = source.indexOf(start), b = source.indexOf(end, a);
  if (a < 0 || b < 0) throw new Error('Missing production section: ' + start);
  return source.slice(a, b);
}
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'ngx-build-job-test-'));
try {
  fs.writeFileSync(path.join(dir, 'BuildJobTest.java'), `
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;
public class BuildJobTest {
  static final int TIME_TO_99_SECONDS = 60;
  ${section('\tprivate static int computeTimeProgress(', '\n\tprivate static boolean isStandaloneRebuildStartLine(')}
  ${section('\tprivate static boolean isStandaloneRebuildStartLine(', '\n\tprivate static boolean isStandaloneWatchNoiseLine(')}
  ${section('\tprivate static final class StandaloneBuildProgress {', '\n\tprivate static void finishBuildTask(')}
  enum NgxBuilderBuildMode {
    fast, prod, watch;
    String label() { return name(); }
    String command() { return "ionic:build:" + name(); }
  }
  enum MobileBuilderBuildMode { production, fast }
  static class MobileBuilder { void setAppBuildMode(MobileBuilderBuildMode mode) {} }
  static class Project {
    File root;
    String getName() { return "Regression"; }
    String getDirPath() { return root.toString(); }
    File getDirFile() { return root; }
    MobileBuilder getMobileBuilder() { return new MobileBuilder(); }
  }
  static class MobileApplication {
    String getComputedEndpoint() { return ""; }
    String getComputedApplicationName() { return "Regression"; }
  }
  static class Application {
    MobileApplication getParent() { return new MobileApplication(); }
    boolean isTplStandalone() { return true; }
  }
  static class EditorInput { Application application = new Application(); }
  static class Display { void asyncExec(Runnable runnable) { runnable.run(); } }
  static class BuildItem {
    boolean selected = true;
    Display getDisplay() { return new Display(); }
    boolean isDisposed() { return false; }
    void setSelection(boolean selected) { this.selected = selected; }
  }
  static class Monitor {
    volatile boolean canceled, done;
    volatile String status;
    volatile int worked;
    void beginTask(String status, int size) { this.status = status; worked = 0; }
    void subTask(String status) { this.status = status; }
    boolean isCanceled() { return canceled; }
    void worked(int n) { worked += n; }
    void done() { done = true; }
  }
  static class IStatus { static final int ERROR = 4; }
  static class Status {
    String message;
    Throwable cause;
    Status(int severity, String plugin, String message) { this.message = message; }
    Status(int severity, String plugin, String message, Throwable cause) { this.message = message; this.cause = cause; }
  }
  static class CoreException extends Exception {
    CoreException(Status status) { super(status.message, status.cause); }
  }
  interface CoreRunnable { void run(Monitor monitor) throws CoreException; }
  static class Job {
    Monitor monitor = new Monitor();
    Thread thread;
    volatile CoreException failure;
    CoreRunnable runnable;
    static Job create(String name, CoreRunnable runnable) {
      var job = new Job(); job.runnable = runnable; return job;
    }
    void setUser(boolean user) {}
    void schedule() {
      thread = new Thread(() -> {
        try { runnable.run(monitor); } catch (CoreException error) { failure = error; }
      });
      thread.start();
    }
    void cancel() { monitor.canceled = true; }
  }
  static class Engine {
    static class Log {
      void debug(String message) {}
      void info(String message) {}
      void error(String message) {}
      void error(String message, Throwable error) {}
    }
    static Log logStudio = new Log();
    static void execute(Runnable runnable) { var thread = new Thread(runnable); thread.setDaemon(true); thread.start(); }
  }
  static class ConvertigoPlugin { static final String PLUGIN_UNIQUE_ID = "test"; }
  static class StringUtils { static boolean isNotBlank(String value) { return !value.isBlank(); } }
  static class ProcessUtils {
    static ProcessBuilder getNpmProcessBuilder(String directory, String... args) { return new ProcessBuilder(args); }
  }
  Project project = new Project();
  EditorInput applicationEditorInput = new EditorInput();
  File nodeDir = new File("."), ionicDir;
  BuildItem buildItem = new BuildItem();
  Collection<Process> processes = new LinkedList<>();
  NgxBuilderBuildMode buildMode;
  Pattern pRemoveEchap = Pattern.compile("\\\\x1b\\\\[[0-9;]*m");
  void terminateNode(boolean productionOnly) {}
  ${section('\tJob prodJob = null;', '\n\tpublic String getCurrentUrl()')}
  static int checks;
  static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
    checks++;
  }
  public static void main(String[] args) throws Exception {
    for (String scenario : List.of("ok", "late", "post", "cancel", "fatal")) {
      var host = new BuildJobTest();
      host.project.root = new File(args[0], scenario);
      host.ionicDir = new File(host.project.root, "_private/ionic");
      host.buildMode = scenario.equals("post") ? NgxBuilderBuildMode.prod
        : scenario.equals("fatal") ? NgxBuilderBuildMode.watch : NgxBuilderBuildMode.fast;
      host.handleProdBuild();
      var job = host.prodJob;
      long deadline = System.currentTimeMillis() + 15000;
      while (job.thread.isAlive() && !Files.exists(new File(host.ionicDir, "bundled").toPath())
          && System.currentTimeMillis() < deadline) Thread.sleep(10);
      check(Files.exists(new File(host.ionicDir, "bundled").toPath()), "npm reached bundle stage: " + scenario);
      // Production parser must not close the Job at the bundle message.
      Thread.sleep(50);
      check(!job.monitor.done, "Job remains active after bundle generation: " + scenario);
      check(job.monitor.worked < 100, "progress including cleanup stays below 100%: " + scenario);
      if (scenario.equals("cancel")) job.cancel();
      job.thread.join(15000);
      check(!job.thread.isAlive(), "Job terminates: " + scenario);
      check(job.monitor.done && !host.buildItem.selected, "monitor and toolbar cleaned up: " + scenario);
      check(host.processes.isEmpty(), "process reference released: " + scenario);
      check(new File(host.project.root, "DisplayObjects/mobile/assets/preserved.txt").exists(), "assets preserved");
      if (scenario.equals("ok")) {
        check(job.failure == null && job.monitor.status.equals("Build finished"), "successful exit reported");
        check(new File(host.ionicDir, "post-ran").exists(), "success waits for npm post-build");
      } else if (scenario.equals("cancel")) {
        check(job.failure == null, "cancellation is not a build failure");
      } else {
        check(job.failure != null, "failed exit becomes a failed Eclipse Job: " + scenario);
        check(job.failure.getMessage().length() < 256, "main error dialog stays compact");
        check(job.failure.getCause().getMessage().contains("FAILURE DETAIL"), "diagnostic reaches Eclipse Details: " + scenario);
        check(job.failure.getCause().getMessage().contains(scenario.equals("fatal") ? "npm exited with code"
          : "code " + (scenario.equals("post") ? 9 : 7)), "exit code retained");
      }
    }
    System.out.println("NGX production Job: " + checks + " checks passed");
  }
}
`);
  const fileUtils = path.join(dir, 'com/twinsoft/convertigo/engine/util/FileUtils.java');
  fs.mkdirSync(path.dirname(fileUtils), { recursive: true });
  fs.writeFileSync(fileUtils, `package com.twinsoft.convertigo.engine.util;
public class FileUtils {
  public static void deleteQuietly(java.io.File file) {
    try { java.nio.file.Files.deleteIfExists(file.toPath()); } catch (java.io.IOException e) { throw new RuntimeException(e); }
  }
}`);
  for (const scenario of ['ok', 'late', 'post', 'cancel', 'fatal']) {
    const work = path.join(dir, scenario, '_private/ionic');
    fs.mkdirSync(work, { recursive: true });
    const assets = path.join(dir, scenario, 'DisplayObjects/mobile/assets');
    fs.mkdirSync(assets, { recursive: true });
    fs.writeFileSync(path.join(assets, 'preserved.txt'), 'preserved');
    fs.writeFileSync(path.join(assets, '../stale.txt'), 'stale');
    fs.writeFileSync(path.join(work, 'build.cjs'), `
const fs = require('node:fs');
console.log('Application bundle generation complete.');
fs.writeFileSync('bundled', '');
setTimeout(() => {
  ${scenario === 'fatal'
    ? "console.error('An unhandled exception occurred: FAILURE DETAIL'); setInterval(() => {}, 60000);"
    : scenario === 'late' ? "console.error('An unhandled exception occurred: FAILURE DETAIL'); process.exit(7);" : "process.exit(0);"}
}, ${scenario === 'cancel' ? 60000 : 500});
`);
    fs.writeFileSync(path.join(work, 'post.cjs'), scenario === 'post'
      ? "console.error('FAILURE DETAIL in npm post-build'); process.exit(9);"
      : "require('node:fs').writeFileSync('post-ran', '');");
    fs.writeFileSync(path.join(work, 'package.json'), JSON.stringify({ scripts: {
      'ionic:build:fast': 'node build.cjs', 'postionic:build:fast': 'node post.cjs',
      'ionic:build:watch': 'node build.cjs',
      'ionic:build:prod': 'node build.cjs', 'postionic:build:prod': 'node post.cjs'
    }}));
  }
  execFileSync('javac', ['--release', '17', '-d', dir, fileUtils, path.join(dir, 'BuildJobTest.java')], { stdio: 'inherit' });
  execFileSync('java', ['-cp', dir, 'BuildJobTest', dir], { stdio: 'inherit', timeout: 60000 });
} finally { fs.rmSync(dir, { recursive: true, force: true }); }

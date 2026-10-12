// Exercise production startup and explorer routing without starting SWT/OSGi.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const source = fs.readFileSync(path.join(__dirname, '../src/com/twinsoft/convertigo/eclipse/ConvertigoPlugin.java'), 'utf8');
function method(signature) {
  const start = source.indexOf('\t' + signature);
  const end = source.indexOf('\n\t}', start);
  assert.ok(start >= 0 && end > start, signature);
  return source.slice(start, end + 4);
}
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'tigo-perspective-test-'));
const javaBin = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin') : '';
try {
  fs.writeFileSync(path.join(dir, 'ConvertigoPlugin.java'), `
public class ConvertigoPlugin {
  static final String PLUGIN_PERSPECTIVE_ID = "com.twinsoft.convertigo.eclipse.ConvertigoPerspective";
  static class TigoPerspective { static final String ID = "com.twinsoft.convertigo.eclipse.TigoPerspective"; }
  static class WorkbenchException extends Exception {}
  static class Perspective {
    String id;
    Perspective(String id) { this.id = id; }
    String getId() { return id; }
  }
  interface IViewPart {}
  static class ProjectExplorerView implements IViewPart {}
  static class IWorkbenchPage {
    Perspective perspective;
    ProjectExplorerView explorer;
    boolean failView;
    int viewCalls;
    Perspective getPerspective() { return perspective; }
    IViewPart findView(String id) { return explorer; }
    IViewPart showView(String id) throws WorkbenchException {
      viewCalls++;
      if (failView) throw new WorkbenchException();
      return explorer = new ProjectExplorerView();
    }
  }
  static class IPartService { void addPartListener(ConvertigoPartListener listener) {} }
  static class IWorkbenchWindow {
    IWorkbenchPage page;
    IWorkbenchPage getActivePage() { return page; }
    void addPerspectiveListener(ConvertigoPerspectiveListener listener) {}
    IPartService getPartService() { return new IPartService(); }
  }
  static class IWorkbench {
    IWorkbenchWindow window;
    int perspectiveCalls;
    IWorkbenchWindow getActiveWorkbenchWindow() { return window; }
    IWorkbenchPage showPerspective(String id, IWorkbenchWindow window) throws WorkbenchException {
      perspectiveCalls++;
      if (window.page == null) window.page = new IWorkbenchPage();
      window.page.perspective = new Perspective(id);
      if (window.page.explorer == null) window.page.explorer = new ProjectExplorerView();
      return window.page;
    }
  }
  static class PlatformUI {
    static IWorkbench workbench;
    static IWorkbench getWorkbench() { return workbench; }
  }
  static class ConvertigoPerspectiveListener {}
  static class ConvertigoPartListener {}
  static class Log { void message(String message) {} void error(String message) {} }
  Log studioLog = new Log();
  ConvertigoPerspectiveListener perspectiveListener;
  ConvertigoPartListener partListener;
  static void asyncExec(Runnable task) { task.run(); }
  void updateGifIconURIs(IWorkbench workbench) {}
  void applyLoadedFonts() {}
  ${method('public void earlyStartup()')}
  ${method('private IWorkbenchPage getActivePage()')}
  ${method('public ProjectExplorerView getProjectExplorerView()')}

  static IWorkbench setup(String perspective) {
    var wb = new IWorkbench();
    wb.window = new IWorkbenchWindow();
    wb.window.page = new IWorkbenchPage();
    wb.window.page.perspective = perspective == null ? null : new Perspective(perspective);
    PlatformUI.workbench = wb;
    return wb;
  }
  static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    var plugin = new ConvertigoPlugin();
    var wb = setup(TigoPerspective.ID);
    plugin.earlyStartup();
    check(wb.perspectiveCalls == 0, "Restored Tigo must stay active");
    for (String id : new String[] { PLUGIN_PERSPECTIVE_ID, "other", null }) {
      wb = setup(id);
      plugin.earlyStartup();
      check(wb.perspectiveCalls == 1 && PLUGIN_PERSPECTIVE_ID.equals(wb.window.page.perspective.id),
          "Existing startup fallback must still open Convertigo");
    }
    wb = setup(null);
    wb.window.page = null;
    plugin.earlyStartup();
    check(wb.perspectiveCalls == 1, "Startup with no page must open Convertigo");
    wb = setup(null);
    wb.window = null;
    plugin.earlyStartup();
    check(wb.perspectiveCalls == 0, "No window must be tolerated");

    wb = setup(TigoPerspective.ID);
    var explorer = new ProjectExplorerView();
    wb.window.page.explorer = explorer;
    check(plugin.getProjectExplorerView() == explorer && wb.perspectiveCalls == 0 && wb.window.page.viewCalls == 0,
        "Reuse the existing explorer");
    wb.window.page.explorer = null;
    check(plugin.getProjectExplorerView() != null && wb.perspectiveCalls == 0 && wb.window.page.viewCalls == 1
        && TigoPerspective.ID.equals(wb.window.page.perspective.id), "Reopen a closed explorer within Tigo");
    wb = setup("other");
    check(plugin.getProjectExplorerView() != null && wb.perspectiveCalls == 1
        && PLUGIN_PERSPECTIVE_ID.equals(wb.window.page.perspective.id), "Keep the legacy explorer fallback");
    wb = setup(TigoPerspective.ID);
    wb.window.page.failView = true;
    check(plugin.getProjectExplorerView() == null && wb.perspectiveCalls == 0,
        "A failed view must not switch away from Tigo");
    System.out.println("10 Tigo startup and explorer scenarios passed");
  }
}
`);
  execFileSync(path.join(javaBin, 'javac'), ['--release', '21', path.join(dir, 'ConvertigoPlugin.java')], { stdio: 'inherit' });
  execFileSync(path.join(javaBin, 'java'), ['-cp', dir, 'ConvertigoPlugin'], { stdio: 'inherit' });
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}

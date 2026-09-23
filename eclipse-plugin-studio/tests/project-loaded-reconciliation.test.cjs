// Execute the production reconciliation method with lifecycle doubles (no SWT/OSGi).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.join(__dirname, '../src/com/twinsoft/convertigo/eclipse');
const source = fs.readFileSync(path.join(root, 'views/projectexplorer/ProjectExplorerView.java'), 'utf8');
const signature = '\tpublic void reconcileLoadedProject(Project project)';
const start = source.indexOf(signature);
const end = source.indexOf('\n\t}\n', start);
assert.ok(start >= 0 && end > start, 'Production reconciliation method must exist');
const method = source.slice(start, end + 4);
const plugin = fs.readFileSync(path.join(root, 'ConvertigoPlugin.java'), 'utf8');
const callback = plugin.slice(plugin.indexOf('public void projectLoaded(Project project)'), plugin.indexOf('public void launchStartupPage('));
assert.match(callback, /pew\.reconcileLoadedProject\(project\)/);
assert.doesNotMatch(callback, /pew\.reloadProject\(/);
const reload = source.slice(source.indexOf('private class ReloadWithProgress'), source.indexOf('private void createDirsAndFiles'));
assert.match(reload, /viewer\.refresh\(parentTreeObject, true\)/,
  'Restoration needs a synchronous refresh, not the queued single-argument override');
assert.ok(reload.indexOf('viewer.refresh(parentTreeObject, true)') < reload.indexOf('viewer.setExpandedElements('));
assert.ok(reload.indexOf('viewer.setExpandedElements(') < reload.indexOf('viewer.setSelection('));
assert.doesNotMatch(reload, /refreshTreeObject\(parentTreeObject/);

const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'project-loaded-test-'));
const javaBin = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin') : '';
try {
  fs.writeFileSync(path.join(dir, 'ProjectLoadedTest.java'), `
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class ProjectLoadedTest {
  static List<String> calls = new ArrayList<>();
  static class CoreException extends Exception {}
  static class EngineException extends Exception {}
  static class Project {
    String getName() { return "AnyProject"; }
  }
  static class TreeObject {
    Object object;
    Object getObject() { return object; }
  }
  static class ProjectTreeObject extends TreeObject {
    ProjectTreeObject(Project project) { object = project; }
    void closeAllEditors(boolean save) { calls.add("editors:" + save); }
    void setObject(Project project) { object = project; calls.add("bind"); }
  }
  static class UnloadedProjectTreeObject extends TreeObject {}
  static class Manager {
    Project loaded;
    Project getLoadedProjectByName(String name) { return loaded; }
  }
  static class Engine {
    static Engine theApp = new Engine();
    Manager databaseObjectsManager = new Manager();
  }
  static class ConvertigoPlugin {
    static ConvertigoPlugin getDefault() { return new ConvertigoPlugin(); }
    void refreshPropertiesView() { calls.add("properties"); }
    void refreshPaletteView() { calls.add("palette"); }
  }
  TreeObject root;
  TreeObject getProjectRootObject(String name) { return root; }
  void importProjectTreeObject(String name) { calls.add("import"); }
  void loadProject(UnloadedProjectTreeObject tree) { calls.add("loadTree"); }
  void reload(ProjectTreeObject tree, Project model, boolean force) {
    if (tree.getObject() != model || model != Engine.theApp.databaseObjectsManager.loaded || !force)
      throw new AssertionError("Rebuild must use the already loaded, bound model");
    calls.add("rebuild");
  }
  ${method}
  static void expect(String... expected) {
    if (!calls.equals(List.of(expected))) throw new AssertionError(calls + " != " + List.of(expected));
    calls.clear();
  }
  public static void main(String[] args) throws Exception {
    var view = new ProjectLoadedTest();
    var old = new Project();
    var current = new Project();
    var wrapper = new ProjectTreeObject(old);
    view.root = wrapper;
    Engine.theApp.databaseObjectsManager.loaded = current;
    view.reconcileLoadedProject(current);
    expect("editors:false", "bind", "rebuild", "properties", "palette");
    if (view.root != wrapper || wrapper.getObject() != current) throw new AssertionError("Root wrapper lost");
    view.reconcileLoadedProject(current); // Duplicate callback is a no-op.
    expect();
    view.reconcileLoadedProject(old); // Delayed callback cannot restore an older model.
    expect();
    Engine.theApp.databaseObjectsManager.loaded = null; // Closed in the meantime.
    view.reconcileLoadedProject(current);
    expect();
    Engine.theApp.databaseObjectsManager.loaded = current;
    view.root = null;
    view.reconcileLoadedProject(current);
    expect("import");
    view.root = new UnloadedProjectTreeObject();
    view.reconcileLoadedProject(current);
    expect("loadTree");
    System.out.println("6 loaded-project lifecycle scenarios passed; callback routing checked");
  }
}
`);
  execFileSync(path.join(javaBin, 'javac'), ['--release', '21', path.join(dir, 'ProjectLoadedTest.java')], { stdio: 'inherit' });
  execFileSync(path.join(javaBin, 'java'), ['-cp', dir, 'ProjectLoadedTest'], { stdio: 'inherit' });
} finally {
  // Only this newly created test directory is removed.
  fs.rmSync(dir, { recursive: true, force: true });
}

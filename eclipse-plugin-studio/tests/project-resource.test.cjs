// Real Eclipse workspace regression tests without SWT or a graphical workbench.
// Build the plugin's Maven target first, then run with JAVA_HOME and MAVEN_REPO if needed.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const assert = require('node:assert/strict');
const source = fs.readFileSync(process.env.PROJECT_RESOURCE_SOURCE || path.join(__dirname,
  '../src/com/twinsoft/convertigo/eclipse/ConvertigoPlugin.java'), 'utf8');
function section(start, end) {
  const a = source.indexOf(start), b = source.indexOf(end, a);
  assert(a >= 0 && b > a, 'Missing production section: ' + start);
  return source.slice(a, b);
}
const repo = process.env.MAVEN_REPO || path.join(os.homedir(), '.m2/repository');
const bundles = path.join(repo, 'p2/osgi/bundle');
// Pin the branch's Eclipse 2026-09 target instead of mixing other installed Eclipse releases.
const target = {
  "org.eclipse.osgi": "3.24.300.v20260721-1251",
  "org.eclipse.equinox.common": "3.21.0.v20260604-1221",
  "org.eclipse.core.jobs": "3.15.900.v20260604-0622",
  "org.eclipse.equinox.registry": "3.12.600.v20250906-0651",
  "org.eclipse.equinox.preferences": "3.12.100.v20251111-0704",
  "org.osgi.service.prefs": "1.1.2.202109301733",
  "org.eclipse.equinox.app": "1.7.600.v20251211-1038",
  "org.eclipse.core.runtime": "3.35.0.v20260623-1631",
  "org.eclipse.core.contenttype": "3.9.900.v20260617-0626",
  "org.eclipse.core.filesystem": "1.11.500.v20260721-1509",
  "org.eclipse.core.expressions": "3.9.700.v20260710-0925",
  "org.eclipse.core.resources": "3.24.100.v20260812-0827",
  "com.sun.jna": "5.19.1.v20260612-1000",
  "com.sun.jna.platform": "5.19.1",
  "org.apache.felix.scr": "2.2.18",
  "org.osgi.service.component": "1.5.1.202212101352",
  "org.osgi.util.promise": "1.3.0.202212101352",
  "org.osgi.util.function": "1.2.0.202109301733",
  "org.osgi.service.event": "1.4.1.202109301733"
};
const jars = Object.entries(target).map(([name, version]) => {
  const jar = path.join(bundles, name, version, name + '-' + version + '.jar');
  assert(fs.existsSync(jar), 'Build the Eclipse 2026-09 Maven target first: missing ' + jar);
  return jar;
});
const java = name => process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', name) : name;
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'project-resource-test-'));
try {
  fs.mkdirSync(path.join(root, 'src/repair'), { recursive: true });
  fs.mkdirSync(path.join(root, 'classes'));
  // Compile the actual production methods. Only logging is replaced; all resource APIs are real.
  fs.writeFileSync(path.join(root, 'src/repair/ProjectResourceTest.java'), `
package repair;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

public class ProjectResourceTest implements BundleActivator {
  ${section('\tpublic static final String PLUGIN_UNIQUE_ID =', '\n\n\tstatic final String PLUGIN_PERSPECTIVE_ID')}
  ${source.match(/\tprivate static final QualifiedName qnInit =[^\n]+/)[0]}
  static void logInfo(String message) { System.out.println(message); }
  ${section('\tpublic IProject createProjectPluginResource(String projectName, String projectDir)', '\n\tpublic IProject getProjectPluginResource(String projectName)')}
  static int checks;
  static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
    checks++;
  }
  static java.nio.file.Path directory(IProject project) { return project.getLocation().toFile().toPath(); }
  static void payload(IProject project) throws Exception {
    var dir = directory(project);
    Files.createDirectories(dir.resolve(".settings"));
    Files.writeString(dir.resolve("c8oProject.yaml"), "project payload");
    Files.writeString(dir.resolve("public.txt"), "public payload");
    Files.writeString(dir.resolve(".gitignore"), "# keep exactly this\\n");
    Files.writeString(dir.resolve(".settings/custom.prefs"), "preferences payload");
    var target = dir.getParent().resolve(project.getName() + "-linked-target");
    Files.createDirectories(target);
    Files.writeString(target.resolve("data.txt"), "linked payload");
    Files.createSymbolicLink(dir.resolve("linked-data"), target);
  }
  static void verify(IProject project, java.nio.file.Path expected) throws Exception {
    check(project.exists() && project.isOpen(), "project registered and open: " + project.getName());
    check(directory(project).equals(expected), "original project location: " + project.getName());
    var xml = Files.readString(expected.resolve(".project"));
    check(xml.contains("<name>" + project.getName() + "</name>"), "correct persisted Eclipse name");
    check(Files.readString(expected.resolve("c8oProject.yaml")).equals("project payload"), "YAML unchanged");
    check(Files.readString(expected.resolve("public.txt")).equals("public payload"), "public file unchanged");
    check(Files.readString(expected.resolve(".gitignore")).equals("# keep exactly this\\n"), "Git rules unchanged");
    check(Files.readString(expected.resolve(".settings/custom.prefs")).equals("preferences payload"), "preferences unchanged");
    check(Files.isSymbolicLink(expected.resolve("linked-data")), "symbolic link preserved");
    check(Files.readString(expected.resolve("linked-data/data.txt")).equals("linked payload"), "linked contents untouched");
    var args = new HashSet<Object>();
    for (var filter : project.getFilters()) args.add(filter.getFileInfoMatcherDescription().getArguments());
    check(args.contains("1.0-name-matches-false-false-node_modules"), "node_modules filter restored");
    check(args.contains(symLinkFilter), "symbolic link filter restored");
    check(project.getFilters().length == 2, "no duplicate filters");
  }
  public static class Nature implements IProjectNature {
    IProject project;
    public void configure() {}
    public void deconfigure() {}
    public IProject getProject() { return project; }
    public void setProject(IProject value) { project = value; }
  }
  @Override public void start(BundleContext context) throws Exception {
    var workspace = ResourcesPlugin.getWorkspace();
    var settings = workspace.getDescription(); settings.setAutoBuilding(false); workspace.setDescription(settings);
    var root = java.nio.file.Path.of(System.getProperty("repair.root"));
    if (System.getProperty("repair.phase").equals("prepare")) {
      for (boolean external : new boolean[] {false, true}) {
        String name = external ? "Reference" : "Default";
        String location = external ? root.resolve("external").toString() : null;
        IProject project = createProjectPluginResource(name, location);
        var expected = directory(project); payload(project);
        var description = project.getDescription();
        description.setComment("cached comment");
        description.setNatureIds(new String[] {"repair.tests.nature"});
        project.setDescription(description, null);
        byte[] original = Files.readAllBytes(expected.resolve(".project"));
        var modified = Files.getLastModifiedTime(expected.resolve(".project"));
        createProjectPluginResource(name, location); openProject(project, null);
        check(Arrays.equals(original, Files.readAllBytes(expected.resolve(".project"))), "healthy description unchanged");
        check(modified.equals(Files.getLastModifiedTime(expected.resolve(".project"))), "healthy timestamp unchanged");
        Files.delete(expected.resolve(".project"));
        check(project.exists() && project.isOpen(), "reproduce already declared open project");
        check(project.getDescription().getName().equals(name), "reproduce cached description despite missing file");
        createProjectPluginResource(name, location);
        verify(project, expected);
        check(Arrays.equals(project.getDescription().getNatureIds(), new String[] {"repair.tests.nature"}), "cached nature preserved");
        check(project.getDescription().getComment().equals("cached comment"), "cached description settings preserved");
        // Exercise the private opening path independently of declaration, including the session init flag.
        Files.delete(expected.resolve(".project")); openProject(project, null); verify(project, expected);
        // A closed external project must keep its location even when no location is supplied by the caller.
        project.close(null); Files.delete(expected.resolve(".project"));
        createProjectPluginResource(name, null); verify(project, expected);
      }
      // These descriptions are removed only after Eclipse has fully stopped, then restored next process.
      for (String name : List.of("RestartDefault", "RestartReference")) {
        var project = createProjectPluginResource(name, name.endsWith("Reference") ? root.resolve("restart-external").toString() : null);
        payload(project);
      }
    } else {
      for (String name : List.of("RestartDefault", "RestartReference")) {
        var project = workspace.getRoot().getProject(name);
        var expected = name.endsWith("Reference") ? root.resolve("restart-external") : workspace.getRoot().getLocation().toFile().toPath().resolve(name);
        check(project.exists(), "workspace retained the declaration across restart");
        check(Files.notExists(expected.resolve(".project")), "description missing before restart repair");
        createProjectPluginResource(name, null); verify(project, expected);
      }
    }
    workspace.save(true, null);
    System.out.println("Project resources (" + System.getProperty("repair.phase") + "): " + checks + " checks passed");
    System.setProperty("repair.passed", "true");
  }
  @Override public void stop(BundleContext context) {}
}
`);
  fs.writeFileSync(path.join(root, 'src/Bootstrap.java'), `
import java.nio.file.*;
import java.util.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
public class Bootstrap {
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]);
    System.setProperty("repair.root", root.toString());
    System.setProperty("repair.phase", args[1]);
    Map<String,String> config = new HashMap<>();
    config.put("org.osgi.framework.storage", root.resolve("osgi").toString());
    config.put("osgi.instance.area", root.resolve("workspace").toUri().toString());
    config.put("osgi.user.area", root.resolve("user").toUri().toString());
    Framework framework = new org.eclipse.osgi.launch.EquinoxFactory().newFramework(config);
    try {
      framework.start();
      BundleContext context = framework.getBundleContext();
      List<Bundle> bundles = new ArrayList<>();
      for (int i=3; i<args.length; i++) bundles.add(context.installBundle(Path.of(args[i]).toUri().toString()));
      Bundle test = context.installBundle(Path.of(args[2]).toUri().toString());
      for (Bundle b: bundles) if (b.getSymbolicName().equals("org.apache.felix.scr")) b.start();
      for (Bundle b: bundles) {
        if (b.getSymbolicName().equals("org.eclipse.core.runtime") || b.getSymbolicName().equals("org.eclipse.core.resources")) b.start();
      }
      test.start();
      if (!"true".equals(System.getProperty("repair.passed"))) throw new AssertionError("Test activator did not run");
      test.stop();
    } finally {
      framework.stop(); framework.waitForStop(10000);
    }
  }
}
`);
  const cp = jars.join(path.delimiter);
  execFileSync(java('javac'), ['-encoding', 'UTF-8', '-cp', cp, '-d', path.join(root, 'classes'),
    path.join(root, 'src/repair/ProjectResourceTest.java'), path.join(root, 'src/Bootstrap.java')], { stdio: 'inherit' });
  fs.writeFileSync(path.join(root, 'manifest.mf'), 'Manifest-Version: 1.0\nBundle-ManifestVersion: 2\nBundle-SymbolicName: repair.tests;singleton:=true\nBundle-Version: 1.0.0\nBundle-Activator: repair.ProjectResourceTest\nRequire-Bundle: org.eclipse.core.resources,org.eclipse.core.runtime\nImport-Package: org.osgi.framework\n\n');
  fs.writeFileSync(path.join(root, 'classes/plugin.xml'), '<plugin><extension point="org.eclipse.core.resources.natures" id="nature" name="Test nature"><runtime><run class="repair.ProjectResourceTest$Nature"/></runtime></extension></plugin>');
  const testJar = path.join(root, 'tests.jar');
  execFileSync(java('jar'), ['cfm', testJar, path.join(root, 'manifest.mf'), '-C', path.join(root, 'classes'), 'repair', '-C', path.join(root, 'classes'), 'plugin.xml']);
  for (const phase of ['prepare', 'restart']) {
    if (phase === 'restart') {
      fs.unlinkSync(path.join(root, 'workspace/RestartDefault/.project'));
      fs.unlinkSync(path.join(root, 'restart-external/.project'));
    }
    execFileSync(java('java'), ['-cp', path.join(root, 'classes') + path.delimiter + jars[0], 'Bootstrap', root, phase, testJar, ...jars.slice(1)], { stdio: 'inherit', timeout: 120000 });
  }
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}

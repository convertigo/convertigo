// Run the production ClipboardManager with real engine models and the shared web failure scenarios.
// Prerequisites: ./gradlew :engine:testClasses; ./gradlew :eclipse-plugin-studio:syncLib; mvn -DskipTests install
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const root = path.resolve(__dirname, '../..');
const studioClasses = path.join(root, 'eclipse-plugin-studio/target/classes');
const studioLib = path.join(root, 'eclipse-plugin-studio/lib');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'tag-clipboard-test-'));
const javaBin = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin') : '';
try {
  if (!fs.existsSync(path.join(studioClasses, 'com/twinsoft/convertigo/eclipse/views/projectexplorer/ClipboardManager.class')))
    throw new Error('Build the Eclipse Studio first (mvn -DskipTests install).');
  const init = path.join(dir, 'classpath.gradle');
  fs.writeFileSync(init, `gradle.projectsEvaluated {
    def engineProject = gradle.rootProject.project(':engine')
    engineProject.tasks.register('tagClipboardTestClasspath') {
      doLast { println 'TAG_TEST_CP=' + engineProject.sourceSets.test.runtimeClasspath.asPath }
    }
  }`);
  const output = execFileSync(path.join(root, 'gradlew'), ['-q', '--no-configure-on-demand', '-I', init, ':engine:tagClipboardTestClasspath'], { cwd: root, encoding: 'utf8' });
  const engineClasspath = output.split(/\r?\n/).find(line => line.startsWith('TAG_TEST_CP='))?.slice('TAG_TEST_CP='.length);
  if (!engineClasspath) throw new Error('Engine test classpath was not returned.');
  // ClipboardManager references Eclipse types even when no display or UI plug-in is started.
  const platformPlugins = [];
  const collectPlugins = (parent) => {
    if (!fs.existsSync(parent)) return;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      const filename = path.join(parent, entry.name);
      if (entry.isDirectory()) collectPlugins(filename);
      else if (entry.name.endsWith('.jar') && path.basename(parent) === 'plugins') platformPlugins.push(filename);
    }
  };
  collectPlugins(path.join(root, 'eclipse-repository/target/products/com.convertigo.studio'));
  const classpath = [dir, studioClasses, engineClasspath, ...fs.readdirSync(studioLib).filter(name => name.endsWith('.jar')).map(name => path.join(studioLib, name)), ...platformPlugins].join(path.delimiter);
  const source = path.join(__dirname, 'java/com/twinsoft/convertigo/eclipse/views/projectexplorer/TagClipboardPasteTest.java');
  execFileSync(path.join(javaBin, 'javac'), ['-cp', classpath, '-d', dir, source], { stdio: 'inherit' });
  execFileSync(path.join(javaBin, 'java'), ['-cp', classpath, 'org.junit.runner.JUnitCore', 'com.twinsoft.convertigo.eclipse.views.projectexplorer.TagClipboardPasteTest'], { stdio: 'inherit' });
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}

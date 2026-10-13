// Compile the current palette and exercise its selection and filtering without a native display.
// Prerequisite: a Maven-built Studio. STUDIO_TEST_BASE may point at a checkout with compatible build outputs.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const root = path.resolve(__dirname, '../..');
const base = process.env.STUDIO_TEST_BASE ? path.resolve(process.env.STUDIO_TEST_BASE) : root;
const studioClasses = path.join(base, 'eclipse-plugin-studio/target/classes');
const studioLib = path.join(base, 'eclipse-plugin-studio/lib');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'palette-selection-test-'));
const javaBin = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin') : '';
const tests = ['PaletteSelectionTest', 'PaletteComponentFilterTest', 'PaletteFilterKeyTest'];

function jars(parent) {
  if (!fs.existsSync(parent)) return [];
  return fs.readdirSync(parent, { withFileTypes: true }).flatMap(entry => {
    const filename = path.join(parent, entry.name);
    return entry.isDirectory() ? jars(filename) : entry.name.endsWith('.jar') ? [filename] : [];
  });
}

try {
  if (!fs.existsSync(path.join(studioClasses, 'com/twinsoft/convertigo/eclipse/views/palette/PaletteView.class'))) {
    throw new Error('Build the Eclipse Studio first, or set STUDIO_TEST_BASE to a compatible built checkout.');
  }
  const platform = jars(path.join(base, 'eclipse-repository/target/products/com.convertigo.studio'));
  const eclipseJars = platform.length ? platform : jars(path.join(os.homedir(), '.m2/repository/p2/osgi/bundle'));
  const classpath = [dir, studioClasses, ...jars(studioLib), ...eclipseJars].join(path.delimiter);
  const packagePath = 'com/twinsoft/convertigo/eclipse/views/palette';
  const sources = [path.join(root, 'eclipse-plugin-studio/src', packagePath, 'PaletteView.java'),
    ...tests.map(name => path.join(__dirname, 'java', packagePath, `${name}.java`))];
  execFileSync(path.join(javaBin, 'javac'), ['--release', '21', '-cp', classpath, '-d', dir, ...sources], { stdio: 'inherit' });
  for (const name of tests) {
    execFileSync(path.join(javaBin, 'java'), ['-cp', classpath, `com.twinsoft.convertigo.eclipse.views.palette.${name}`], { stdio: 'inherit' });
  }
} finally {
  fs.rmSync(dir, { recursive: true, force: true });
}

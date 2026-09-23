#!/usr/bin/env node
// A native, separately identifiable macOS launcher for an existing PDE runtime.
// Generated bundle only: no change to the installed Eclipse or its PDE configuration.
import fs from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';
import { pathToFileURL } from 'node:url';

const { values } = parseArgs({ options: {
  'eclipse-app': { type: 'string' },
  'pde-config': { type: 'string' },
  workspace: { type: 'string' },
  output: { type: 'string' },
  'java-home': { type: 'string' }
} });
for (const key of ['eclipse-app', 'pde-config', 'workspace', 'output', 'java-home']) {
  if (!values[key] || !path.isAbsolute(values[key]) || /[\r\n]/.test(values[key])) {
    throw new Error(`--${key} requires a single-line absolute path`);
  }
}
if (process.platform !== 'darwin') throw new Error('This launcher is for macOS only');
const app = values.output;
if (!app.endsWith('.app') || fs.existsSync(app)) throw new Error('Output must be a NEW .app directory');
const eclipse = path.join(values['eclipse-app'], 'Contents', 'Eclipse');
const oldConfig = values['pde-config'];
const config = path.join(app, 'Contents', 'Eclipse', 'configuration');
const vm = path.join(values['java-home'], 'lib', 'libjli.dylib');
const executable = path.join(values['eclipse-app'], 'Contents', 'MacOS', 'eclipse');
const originalIni = fs.readFileSync(path.join(eclipse, 'eclipse.ini'), 'utf8').split(/\r?\n/);
const option = key => {
  const index = originalIni.indexOf(key);
  if (index < 0 || !originalIni[index + 1]) throw new Error(`Missing ${key} in Eclipse launcher ini`);
  // macOS launcher ini paths are relative to Contents/MacOS.
  return path.resolve(values['eclipse-app'], 'Contents', 'MacOS', originalIni[index + 1]);
};
const startup = option('-startup');
const library = option('--launcher.library');
for (const required of [values.workspace, vm, executable, startup, library,
  path.join(oldConfig, 'config.ini'), path.join(oldConfig, 'dev.properties'),
  path.join(oldConfig, 'org.eclipse.equinox.simpleconfigurator', 'bundles.info')]) {
  if (!fs.existsSync(required)) throw new Error(`Missing ${required}`);
}
const xml = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const iniFile = path.join(app, 'Contents', 'Eclipse', 'eclipse.ini');
const configText = fs.readFileSync(path.join(oldConfig, 'config.ini'), 'utf8');
if (!configText.includes(oldConfig)) throw new Error('PDE configuration has no expected absolute configuration path');
fs.mkdirSync(path.join(app, 'Contents', 'MacOS'), { recursive: true });
fs.mkdirSync(path.join(config, 'org.eclipse.equinox.simpleconfigurator'), { recursive: true });
fs.copyFileSync(executable, path.join(app, 'Contents', 'MacOS', 'eclipse'));
fs.chmodSync(path.join(app, 'Contents', 'MacOS', 'eclipse'), 0o755);
for (const file of ['dev.properties', 'org.eclipse.equinox.simpleconfigurator/bundles.info']) {
  fs.copyFileSync(path.join(oldConfig, file), path.join(config, file));
}
fs.writeFileSync(path.join(config, 'config.ini'), configText.replaceAll(oldConfig, config));
const args = ['-name', 'ConvertigoStudio Develop', '-startup', startup, '--launcher.library', library,
  '-vm', vm, '-configuration', config, '-dev', pathToFileURL(path.join(config, 'dev.properties')).href,
  '-product', 'com.twinsoft.convertigo.studio.product.ConvertigoProduct', '-data', values.workspace,
  '-os', 'macosx', '-ws', 'cocoa', '-arch', process.arch === 'arm64' ? 'aarch64' : 'x86_64',
  '-consoleLog', '-vmargs', '-XstartOnFirstThread', '-Xmx4096m', '--enable-native-access=ALL-UNNAMED',
  '-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true'];
fs.writeFileSync(iniFile, args.join('\n') + '\n');
fs.writeFileSync(path.join(app, 'Contents', 'Info.plist'), `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleExecutable</key><string>eclipse</string>
<key>CFBundleIdentifier</key><string>com.convertigo.studio.develop.local</string>
<key>CFBundleName</key><string>ConvertigoStudio Develop</string>
<key>CFBundleDisplayName</key><string>ConvertigoStudio Develop</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleVersion</key><string>8.5.0</string>
<key>NSHighResolutionCapable</key><true/>
<key>NSRequiresAquaSystemAppearance</key><true/>
<key>Eclipse</key><array><string>--launcher.ini</string><string>${xml(iniFile)}</string></array>
<key>LSEnvironment</key><dict><key>PATH</key><string>${xml(process.env.PATH || '/usr/bin:/bin')}</string></dict>
</dict></plist>\n`);
console.log(`Created ${app}`);
console.log(`Workspace: ${values.workspace}`);
console.log('Uses existing compiled PDE classes. Stop this app before recompiling; never run another Studio on the same workspace.');

// Opt-in real Angular regression, using existing dependencies read-only.
// node eclipse-plugin-studio/tests/ngx-angular-build.cjs /path/to/ionic/node_modules
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync, spawn } = require('node:child_process');
const modules = path.resolve(process.argv[2] || 'missing-node-modules');
assert.ok(fs.existsSync(path.join(modules, '@angular/build/package.json')), 'Pass an existing NGX node_modules');
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'c8o-1234-angular-'));
const work = path.join(root, 'A/_private/ionic');
const mobile = path.join(root, 'A/DisplayObjects/mobile');
const asset = { glob: '**/*', input: '../../../A/DisplayObjects/mobile/assets/A/icon', output: '/assets/A/icon/' };
const options = {
  outputPath: { base: '../../DisplayObjects/mobile', browser: '' },
  deleteOutputPath: false, index: 'src/index.html', browser: 'src/main.ts',
  polyfills: ['zone.js'], tsConfig: 'tsconfig.json', styles: ['src/styles.css'],
  assets: [asset], optimization: false, outputHashing: 'all'
};
const config = { version: 1, cli: { cache: { enabled: false }, analytics: false }, projects: { app: {
  root: '', sourceRoot: 'src', projectType: 'application', architect: { build: {
    builder: '@angular-devkit/build-angular:application', options,
    configurations: { production: { optimization: true, serviceWorker: 'ngsw-config.json' }, fastbuild: {} }
  }, serve: {
    builder: '@angular-devkit/build-angular:dev-server', options: { buildTarget: 'app:build:fastbuild' }
  }}
}}};
function write(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, typeof value === 'string' ? value : JSON.stringify(value));
}
function configure(assets) {
  options.assets = assets;
  write(path.join(work, 'angular.json'), config);
}
const env = { ...process.env, NG_CLI_ANALYTICS: 'false', NG_BUILD_MAX_WORKERS: '2', CI: 'true', NO_COLOR: '1' };
function run(args) {
  const result = spawnSync(process.execPath, [path.join(modules, '@angular/cli/bin/ng.js'), ...args],
    { cwd: work, env, encoding: 'utf8', timeout: 90000 });
  const log = (result.stdout || '') + (result.stderr || '');
  if (result.error) throw result.error;
  return { code: result.status, log };
}
function bundles() {
  const files = fs.readdirSync(mobile);
  for (const prefix of ['main-', 'polyfills-', 'styles-']) assert.ok(files.some((f) => f.startsWith(prefix)), prefix);
}
async function watch(assets, fail) {
  configure(assets);
  const child = spawn(process.execPath, [path.join(modules, '@angular/cli/bin/ng.js'), 'build', 'app', '--watch', '--configuration=fastbuild'],
    { cwd: work, env, detached: process.platform !== 'win32' });
  let log = '';
  child.stdout.on('data', (data) => { log += data; });
  child.stderr.on('data', (data) => { log += data; });
  let exited = false;
  child.on('exit', () => { exited = true; });
  async function until(predicate) {
    const start = Date.now();
    while (!predicate()) {
      if (Date.now() - start > 60000 || exited) throw new Error('Unexpected watch state:\n' + log);
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
  }
  try {
    if (fail) {
      await until(() => log.includes('An unhandled exception occurred'));
      assert.match(log, /ERR_FS_CP_EINVAL|src and dest cannot be the same/);
      console.log('PASS: fatal watch self-copy detected (Angular may leave its watcher alive)');
    } else {
      await until(() => log.includes('Application bundle generation complete') && fs.existsSync(path.join(mobile, 'index.html')));
      bundles();
      write(path.join(work, 'src/main.ts'), 'const broken: number = "wrong";');
      await until(() => log.includes('Application bundle generation failed'));
      assert.equal(exited, false, 'compile errors must leave watch running');
      const before = log;
      write(path.join(work, 'src/main.ts'), 'console.log("recovered");');
      await until(() => log.slice(before.length).includes('Application bundle generation complete'));
      bundles();
      console.log('PASS: watch builds, reports compile failure, and rebuilds after repair');
    }
  } finally {
    if (!exited) {
      if (process.platform === 'win32') child.kill();
      else process.kill(-child.pid, 'SIGTERM');
      await new Promise((resolve) => child.once('exit', resolve));
    }
  }
}
async function serve() {
  configure([asset]);
  const child = spawn(process.execPath, [path.join(modules, '@angular/cli/bin/ng.js'), 'serve', 'app',
    '--host=127.0.0.1', '--port=0'], { cwd: work, env, detached: process.platform !== 'win32' });
  let log = '';
  child.stdout.on('data', (data) => { log += data; });
  child.stderr.on('data', (data) => { log += data; });
  let exited = false;
  child.on('exit', () => { exited = true; });
  try {
    const start = Date.now();
    let url;
    while (!(url = log.match(/http:\/\/127\.0\.0\.1:\d+\//)?.[0])) {
      if (exited || Date.now() - start > 60000) throw new Error('Dev server did not start:\n' + log);
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
    const result = await fetch(url + 'assets/A/icon/x.svg');
    assert.equal(result.status, 200);
    assert.match(await result.text(), /<svg/);
    console.log('PASS: dev server still serves the owner asset in memory (HTTP 200)');
  } finally {
    if (!exited) {
      if (process.platform === 'win32') child.kill();
      else process.kill(-child.pid, 'SIGTERM');
      await new Promise((resolve) => child.once('exit', resolve));
    }
  }
}
(async () => {
  try {
    fs.mkdirSync(work, { recursive: true });
    fs.symlinkSync(modules, path.join(work, 'node_modules'), 'junction');
    write(path.join(work, 'package.json'), { name: 'issue-1234-disposable', private: true });
    write(path.join(work, 'tsconfig.json'), {
      compilerOptions: { target: 'ES2022', module: 'ES2022', moduleResolution: 'bundler',
        lib: ['ES2022', 'DOM'], types: [], skipLibCheck: true }, files: ['src/main.ts']
    });
    write(path.join(work, 'src/main.ts'), 'console.log("issue 1234");');
    write(path.join(work, 'src/index.html'), '<html><head><base href="/"></head><body>Issue 1234</body></html>');
    write(path.join(work, 'src/styles.css'), 'body { color: navy; }');
    write(path.join(work, 'ngsw-config.json'), { index: '/index.html', assetGroups: [{
      name: 'app', installMode: 'prefetch', resources: { files: ['/*.html', '/*.js', '/*.css', '/assets/**'] }
    }] });
    write(path.join(mobile, 'assets/A/icon/x.svg'), '<svg xmlns="http://www.w3.org/2000/svg"/>');
    for (const mode of ['fastbuild', 'production']) {
      configure([asset]);
      const failed = run(['build', 'app', '--configuration=' + mode]);
      assert.notEqual(failed.code, 0, failed.log);
      assert.match(failed.log, /Application bundle generation complete/);
      assert.match(failed.log, /ERR_FS_CP_EINVAL|src and dest cannot be the same/);
      console.log('PASS: reproduced late ' + mode + ' failure after bundle generation');
      // NgxBuilder's independently tested predicate removes this self-copy only.
      configure([]);
      const success = run(['build', 'app', '--configuration=' + mode]);
      assert.equal(success.code, 0, success.log);
      bundles();
      assert.ok(fs.existsSync(path.join(mobile, 'assets/A/icon/x.svg')), 'preserved owner asset');
      if (mode === 'production') {
        assert.ok(fs.existsSync(path.join(mobile, 'ngsw.json')));
        const installed = path.dirname(modules);
        const postBuild = path.join(installed, 'fix-basehref-and-ngsw.mjs');
        const worker = path.join(installed, 'ngsw-worker-custom.js');
        if (fs.existsSync(postBuild) && fs.existsSync(worker)) {
          fs.copyFileSync(worker, path.join(work, 'ngsw-worker-custom.js'));
          const fixed = spawnSync(process.execPath, [postBuild], { cwd: work, encoding: 'utf8' });
          assert.equal(fixed.status, 0, fixed.stdout + fixed.stderr);
          assert.match(fs.readFileSync(path.join(mobile, 'index.html'), 'utf8'), /base href="\.\/"/);
          console.log('PASS: existing TPL post-build script completes without modification');
        }
      }
      console.log('PASS: ' + mode + ' produces all bundles without redundant self-copy');
    }
    // A different source project must still be copied.
    const external = path.join(root, 'Library/DisplayObjects/mobile/assets/Library/icon');
    write(path.join(external, 'external.svg'), '<svg/>');
    configure([{ ...asset, input: external, output: '/assets/Library/icon/' }]);
    const copied = run(['build', 'app', '--configuration=fastbuild']);
    assert.equal(copied.code, 0, copied.log);
    assert.equal(fs.readFileSync(path.join(mobile, 'assets/Library/icon/external.svg'), 'utf8'), '<svg/>');
    console.log('PASS: shared assets from another project are still copied');
    await watch([asset], true);
    await watch([], false);
    await serve();
    console.log('Real Angular regression passed with Node ' + process.version + ', Angular build ' +
      JSON.parse(fs.readFileSync(path.join(modules, '@angular/build/package.json'))).version);
  } finally {
    // Only this freshly-created fixture is removed. The linked dependencies are not followed.
    fs.rmSync(root, { recursive: true, force: true });
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });

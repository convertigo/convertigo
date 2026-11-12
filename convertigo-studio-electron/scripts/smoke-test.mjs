// Starts a packaged Electron Studio with Playwright, on scratch data and workspace folders that leave
// the user's alone, and checks that the Studio opens, authenticated, and that its engine logged no
// error but the known one of the JSP servlet, which the embedded Tomcat has not.
//
// Usage: node scripts/smoke-test.mjs [<target> | <application executable>]   (the host target by default)
// SMOKE_TEST_DIR may name the folder of the run, kept for its logs and screenshot.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { _electron as electron } from 'playwright-core';

import { electronDir, hostTarget, parseTarget, TARGETS } from './common.mjs';

const NAME = 'Convertigo Studio';
// an engine without Jasper does not find the JspServlet of the default web.xml of Tomcat
const KNOWN_ERRORS = [
	/ClassNotFoundException: org\.apache\.jasper\.servlet\.JspServlet/,
	/Servlet \[jsp\] in web application \[\/convertigo\] threw load\(\) exception/
];

/**
 * @param {string} target
 * @returns {string} the executable of the application packaged for a target in dist
 */
function executableOf(target) {
	const { platform, arch } = parseTarget(target);
	const appPath = path.join(electronDir, 'dist', `${NAME}-${platform}-${arch}`);
	return {
		darwin: path.join(appPath, `${NAME}.app`, 'Contents', 'MacOS', NAME),
		win32: path.join(appPath, `${NAME}.exe`),
		linux: path.join(appPath, 'convertigo-studio')
	}[platform];
}

const arg = process.argv[2] ?? hostTarget;
const executablePath = TARGETS.includes(arg) ? executableOf(arg) : path.resolve(arg);
// the data, workspace, log and screenshot of the run: SMOKE_TEST_DIR, emptied first, or a new folder
const scratch = process.env.SMOKE_TEST_DIR
	? path.resolve(process.env.SMOKE_TEST_DIR)
	: fs.mkdtempSync(path.join(os.tmpdir(), 'convertigo-studio-smoke-'));
if (process.env.SMOKE_TEST_DIR) {
	fs.rmSync(scratch, { recursive: true, force: true });
	fs.mkdirSync(scratch, { recursive: true });
}
const dataDir = path.join(scratch, 'data');
const workspaceDir = path.join(scratch, 'workspace');
const logFile = path.join(dataDir, 'engine.log');

const failures = [];
const app = await electron.launch({
	executablePath,
	env: {
		...process.env,
		CONVERTIGO_STUDIO_DATA: dataDir,
		CONVERTIGO_STUDIO_WORKSPACE: workspaceDir,
		// the levels of the Tomcat log in English, whatever the language of the machine
		JAVA_TOOL_OPTIONS: '-Duser.language=en -Duser.country=US'
	},
	timeout: 60_000
});
try {
	const page = await app.firstWindow();
	// the splash, then the Studio once the engine answers, opened by the token of the application
	await page.waitForURL(/\/convertigo\/studio\//, { timeout: 240_000 });
	await page.getByRole('navigation', { name: 'Studio panels' }).waitFor({ timeout: 120_000 });
	await page.locator('.studio-shell__dock').waitFor({ timeout: 30_000 });
	if (/\/login/.test(page.url())) {
		failures.push(`the login page opened instead of the Studio: ${page.url()}`);
	}
	console.log(`Studio opened: ${page.url().replace(/#authToken=.*/, '#authToken=…')} (${await page.title()})`);
	await page.screenshot({ path: path.join(scratch, 'studio.png') });
} catch (error) {
	failures.push(`the Studio did not open: ${error}`);
} finally {
	await app.close().catch(() => {});
}

// the output of the engine process, and the log of the engine in its workspace
for (const file of [logFile, path.join(workspaceDir, 'logs', 'engine.log')]) {
	const log = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
	if (!log) {
		failures.push(`no engine log in ${file}`);
		continue;
	}
	const errors = log
		.split('\n')
		.filter((line) => /\b(ERROR|SEVERE|FATAL)\b|Exception\b/.test(line))
		.filter((line) => !KNOWN_ERRORS.some((known) => known.test(line)));
	if (errors.length) {
		failures.push(`${file} has errors:\n${errors.slice(0, 20).join('\n')}`);
	}
	console.log(`Engine log: ${file}`);
}

console.log(`Screenshot: ${path.join(scratch, 'studio.png')}`);
if (failures.length) {
	console.error(`Smoke test failed:\n- ${failures.join('\n- ')}`);
	process.exit(1);
}
console.log('Smoke test passed');

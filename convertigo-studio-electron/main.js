// Convertigo Studio: an Electron window on the web Studio, served by a Convertigo engine that the
// application starts on a free port, with its own workspace, and stops when it quits. The Studio opens
// authenticated, with a secret the application gives the engine.
const { app, BrowserWindow, Menu, dialog, ipcMain, session, shell } = require('electron');
const { spawn, spawnSync } = require('node:child_process');
const crypto = require('node:crypto');
const fs = require('node:fs');
const http = require('node:http');
const net = require('node:net');
const path = require('node:path');

// the engine, its Java runtime and the icon, which scripts/engine.mjs and scripts/jre.mjs prepare
// for `npm start` and the packaging copies in the resources of the application
const resources = app.isPackaged ? process.resourcesPath : path.join(__dirname, 'resources');
const engineDir = path.join(resources, 'engine');
const jreDir = app.isPackaged
	? path.join(resources, 'jre')
	: path.join(resources, `${process.platform}-${process.arch}`, 'jre');
const java = path.join(jreDir, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
// the data of the application, or another folder for a test that must leave them alone
if (process.env.CONVERTIGO_STUDIO_DATA) {
	app.setPath('userData', process.env.CONVERTIGO_STUDIO_DATA);
}
const dataDir = app.getPath('userData');
const tomcatDir = path.join(dataDir, 'tomcat');
// the workspace of the projects, chosen as in the Eclipse Studio and kept with the recent ones
const settingsFile = path.join(dataDir, 'settings.json');
const settings = readSettings();
let workspaceDir = path.resolve(
	process.env.CONVERTIGO_STUDIO_WORKSPACE || settings.workspace || path.join(dataDir, 'workspace')
);
const logFile = path.join(dataDir, 'engine.log');
const icon = path.join(resources, 'icon.png');
// the token that opens the Studio without its login page, valid while the engine runs
const authToken = crypto.randomBytes(32).toString('hex');

/** @type {import('node:child_process').ChildProcess | null} */
let engine = null;
let enginePort = 0;
let quitting = false;
/** the engine stops for another workspace, not for a failure */
let switching = false;
/** @type {BrowserWindow | null} */
let mainWindow = null;
/** the local port of the shutdown command of the Tomcat of the engine */
let shutdownPort = 0;
/** @type {BrowserWindow | null} the license, until the Studio window opens */
let licenseWindow = null;

/**
 * @returns {{ workspace?: string, recent?: string[], askAtStartup?: boolean, licenseAccepted?: boolean, ports?: Record<string, number>, updateCheckedAt?: number }}
 */
function readSettings() {
	try {
		return JSON.parse(fs.readFileSync(settingsFile, 'utf8'));
	} catch {
		return {};
	}
}

/**
 * Keeps the workspace in use first among the recent ones.
 */
function saveSettings() {
	// the workspace of a test is not kept
	if (process.env.CONVERTIGO_STUDIO_WORKSPACE && path.resolve(process.env.CONVERTIGO_STUDIO_WORKSPACE) === workspaceDir) {
		return;
	}
	settings.workspace = workspaceDir;
	settings.recent = [workspaceDir, ...(settings.recent ?? []).filter((dir) => dir !== workspaceDir)].slice(0, 8);
	fs.mkdirSync(dataDir, { recursive: true });
	fs.writeFileSync(settingsFile, JSON.stringify(settings, null, 2));
}

/**
 * @param {string} dir
 * @returns {string} the folder shown shorter, from the home folder
 */
function displayPath(dir) {
	const home = app.getPath('home');
	return dir.startsWith(home + path.sep) ? `~${dir.slice(home.length)}` : dir;
}

if (!app.requestSingleInstanceLock()) {
	app.quit();
}

/** the registration page of Convertigo Cloud, which the Studio shows in a frame, as the Eclipse Studio */
const REGISTRATION_ORIGIN = 'https://c8ocloud.convertigo.net';

/**
 * @param {string} url
 * @param {{ url?: string } | undefined} referrer
 * @returns {boolean} whether a window opens from the registration page, as the login of a provider that answers
 * to its opener
 */
function fromRegistration(url, referrer) {
	const originOf = (/** @type {string | undefined} */ value) => {
		try {
			return new URL(String(value)).origin;
		} catch {
			return '';
		}
	};
	return originOf(referrer?.url) === REGISTRATION_ORIGIN || originOf(url) === REGISTRATION_ORIGIN;
}

/**
 * @param {number} port
 * @param {string} host
 * @returns {Promise<boolean>}
 */
function canListen(port, host) {
	return new Promise((resolve) => {
		const server = net.createServer();
		// a machine without IPv6 has no listener on it either
		server.once('error', (error) => resolve(['EAFNOSUPPORT', 'EADDRNOTAVAIL'].includes(error?.code)));
		server.once('listening', () => server.close(() => resolve(true)));
		server.listen(port, host);
	});
}

/**
 * A port is free when nothing listens on it, on any interface: an engine that listens on all of them in
 * IPv6 takes the connections to localhost, which the Studio opens. A server on the loopback only, as a
 * Docker container published on 127.0.0.1, takes them too, and the wildcard addresses do not see it.
 * @param {number} port
 * @returns {Promise<boolean>}
 */
async function isFree(port) {
	for (const host of ['::', '0.0.0.0', '127.0.0.1', '::1']) {
		if (!(await canListen(port, host))) {
			return false;
		}
	}
	return true;
}

/**
 * The port of the engine of the workspace: the one it had, when free, else a port no other workspace had. The
 * Studio keeps its preferences, its layout and its devices in the storage of its page, whose origin is this port:
 * they stay with the workspace, as the preferences of the Eclipse Studio do.
 * @returns {Promise<number>}
 */
async function workspacePort() {
	const ports = settings.ports ?? {};
	const remembered = ports[workspaceDir];
	let port = remembered && (await isFree(remembered)) ? remembered : 0;
	if (!port) {
		const taken = new Set(Object.entries(ports).filter(([dir]) => dir !== workspaceDir).map(([, used]) => used));
		for (let candidate = 28080; candidate < 28280 && !port; candidate++) {
			if (!taken.has(candidate) && (await isFree(candidate))) {
				port = candidate;
			}
		}
		port ||= await freePort(28080);
	}
	settings.ports = { ...ports, [workspaceDir]: port };
	return port;
}

async function freePort(from) {
	for (let port = from; port < from + 200; port++) {
		if (await isFree(port)) {
			return port;
		}
	}
	throw new Error('No free port for the engine');
}

/**
 * @param {number} port
 * @returns {Promise<boolean>}
 */
function engineAnswers(port) {
	return new Promise((resolve) => {
		const request = http.get(
			{ host: '127.0.0.1', port, path: '/convertigo/admin/services/engine.CheckAuthentication', timeout: 2000 },
			(response) => {
				response.resume();
				resolve(response.statusCode === 200);
			}
		);
		request.on('error', () => resolve(false));
		request.on('timeout', () => request.destroy());
	});
}

function startEngine(port) {
	fs.mkdirSync(workspaceDir, { recursive: true });
	// the PouchDB server an engine that stopped abruptly left running
	stopWorkspacePouchDB();
	if (!fs.existsSync(path.join(tomcatDir, 'conf'))) {
		fs.cpSync(path.join(engineDir, 'base', 'conf'), path.join(tomcatDir, 'conf'), { recursive: true });
	}
	const libs = fs
		.readdirSync(path.join(engineDir, 'lib'))
		.filter((name) => name.endsWith('.jar'))
		.map((name) => path.join(engineDir, 'lib', name));
	const classpath = [path.join(engineDir, 'classes'), ...libs].join(path.delimiter);
	const log = fs.openSync(logFile, 'w');
	engine = spawn(
		java,
		[
			'-Xmx2g',
			`-Dconvertigo.cems.user_workspace_path=${workspaceDir}`,
			'-Djava.awt.headless=true',
			// the engine of develop serves the web Studio and runs its builds only when the server allows them;
			// a desktop Studio also debugs its JavaScript, and serves its one user as the Eclipse Studio
			// (Engine.isDesktopStudio: its PSC, the local agent of the Assistant)
			'-Dconvertigo.studio.desktop=true',
			// the URL the engine gives of itself (local agents, MCP, NGX applications, HTTP steps), on the port it
			// listens to, as the Eclipse Studio sets it: 28080 may be another Convertigo
			`-Dconvertigo.engine.application_server.convertigo.url=http://localhost:${port}/convertigo`,
			'-Dconvertigo.engine.web_studio=enabled',
			'-Dconvertigo.engine.server_build=studio',
			'-Dconvertigo.engine.javascript.debugger=true',
			'--enable-native-access=ALL-UNNAMED',
			'--add-opens',
			'java.base/java.lang=ALL-UNNAMED',
			'--add-opens',
			'java.base/java.util=ALL-UNNAMED',
			'-cp',
			classpath,
			'DevEngine',
			String(port),
			path.join(engineDir, 'war'),
			tomcatDir
		],
		{
			cwd: tomcatDir,
			stdio: ['ignore', log, log],
			env: {
				...process.env,
				CONVERTIGO_STUDIO_AUTH_TOKEN: authToken,
				CONVERTIGO_STUDIO_SHUTDOWN_PORT: String(shutdownPort)
			}
		}
	);
	engine.on('exit', (code) => {
		engine = null;
		if (!quitting && !switching) {
			dialog.showErrorBox(
				'Convertigo engine stopped',
				`The engine stopped (code ${code}). Its log is in:\n${logFile}`
			);
			app.quit();
		}
	});
}

function splashPage(message = 'Starting the Convertigo engine…') {
	const iconTag = fs.existsSync(icon)
		? `<img alt="" src="data:image/png;base64,${fs.readFileSync(icon).toString('base64')}">`
		: '';
	const html = `<!doctype html><html><head><meta charset="utf-8"><style>
		:root { color-scheme: light dark; }
		body { margin: 0; height: 100vh; display: grid; place-content: center; gap: 14px; text-align: center;
			font: 14px -apple-system, system-ui, sans-serif; background: light-dark(#f3f3f5, #151516);
			color: light-dark(#3b3b3b, #cccccc); }
		h1 { margin: 0; font-size: 20px; font-weight: 600; color: light-dark(#1f1f1f, #ffffff); }
		img { width: 72px; height: 72px; margin: 0 auto; }
		.spinner { width: 26px; height: 26px; margin: 0 auto; border: 3px solid rgb(24 207 255 / 0.25);
			border-top-color: #18cfff; border-radius: 50%; animation: spin 0.8s linear infinite; }
		@keyframes spin { to { transform: rotate(360deg); } }
		.workspace { font-size: 12px; opacity: 0.6; }
	</style></head><body>${iconTag}<h1>Convertigo Studio</h1><div class="spinner"></div>
	<div>${message}</div><div class="workspace">${displayPath(workspaceDir)}</div></body></html>`;
	return `data:text/html;charset=utf-8,${encodeURIComponent(html)}`;
}

/**
 * The license of Convertigo, which the user accepts at the first start, as in the Convertigo Setup wizard of the
 * Eclipse Studio: the application quits when it is not accepted.
 * @returns {Promise<boolean>} whether the license is accepted
 */
function acceptLicense() {
	if (settings.licenseAccepted === true) {
		return Promise.resolve(true);
	}
	let text;
	try {
		text = fs.readFileSync(path.join(engineDir, 'war', 'licenses', 'license.txt'), 'utf8');
	} catch (error) {
		text = `Unable to get the license text!\n${error.message}`;
	}
	const escaped = text.replace(/[&<>]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[c]);
	const html = `<!doctype html><html><head><meta charset="utf-8"><title>Product license agreement</title><style>
		:root { color-scheme: light dark; }
		body { margin: 0; height: 100vh; display: grid; grid-template-rows: auto minmax(0, 1fr) auto auto; gap: 12px;
			box-sizing: border-box; padding: 20px; font: 13px -apple-system, system-ui, sans-serif;
			background: light-dark(#f3f3f5, #151516); color: light-dark(#3b3b3b, #cccccc); }
		h1 { margin: 0; font-size: 17px; font-weight: 600; color: light-dark(#1f1f1f, #ffffff); }
		pre { margin: 0; overflow: auto; padding: 12px; border: 1px solid light-dark(#d4d4d8, #3f3f46); border-radius: 6px;
			background: light-dark(#ffffff, #1e1e20); font: 12px ui-monospace, Menlo, monospace; white-space: pre-wrap; }
		p { margin: 0; font-size: 12px; }
		footer { display: flex; align-items: center; gap: 10px; }
		label { flex: 1; display: flex; align-items: center; gap: 6px; }
		button { padding: 6px 16px; border-radius: 6px; border: 1px solid light-dark(#d4d4d8, #3f3f46);
			background: light-dark(#ffffff, #27272a); color: inherit; font: inherit; }
		button.primary { background: #18cfff; border-color: #18cfff; color: #0b2a33; font-weight: 600; }
		button:disabled { opacity: 0.5; }
	</style></head><body>
		<h1>Product license agreement</h1>
		<pre>${escaped}</pre>
		<p>BY INDICATING YOUR ACCEPTANCE BY CLICKING “Accept license” BELOW, OR INSTALLING OR USING THE SOFTWARE, YOU
		ARE AGREEING TO BE BOUND BY THE TERMS OF THIS AGREEMENT.</p>
		<footer>
			<label><input type="checkbox" id="accept"> Accept license</label>
			<button onclick="location.href='convertigo-license:decline'">Quit</button>
			<button class="primary" id="continue" disabled onclick="location.href='convertigo-license:accept'">Continue</button>
		</footer>
		<script>document.getElementById('accept').onchange = (e) => (document.getElementById('continue').disabled = !e.target.checked);</script>
	</body></html>`;
	const window = new BrowserWindow({
		width: 820,
		height: 720,
		minWidth: 600,
		minHeight: 480,
		title: 'Convertigo Studio',
		icon,
		backgroundColor: '#151516'
	});
	return new Promise((resolve) => {
		let answered = false;
		const answer = (accepted) => {
			if (answered) {
				return;
			}
			answered = true;
			if (accepted) {
				settings.licenseAccepted = true;
				fs.mkdirSync(dataDir, { recursive: true });
				fs.writeFileSync(settingsFile, JSON.stringify(settings, null, 2));
			}
			resolve(accepted);
		};
		window.webContents.on('will-navigate', (event, url) => {
			if (url.startsWith('convertigo-license:')) {
				event.preventDefault();
				answer(url === 'convertigo-license:accept');
			}
		});
		window.on('closed', () => answer(false));
		licenseWindow = window;
		void window.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
	});
}

async function createWindow() {
	const window = new BrowserWindow({
		width: 1440,
		height: 900,
		minWidth: 900,
		minHeight: 600,
		title: windowTitle(),
		icon,
		backgroundColor: '#151516',
		show: false,
		// the preload runs in the frames too, for the Marketplace
		webPreferences: { preload: path.join(__dirname, 'preload.js'), nodeIntegrationInSubFrames: true }
	});
	mainWindow = window;
	window.on('closed', () => {
		if (mainWindow === window) {
			mainWindow = null;
		}
	});
	window.once('ready-to-show', () => window.show());
	// the title names the workspace, rather than the page
	window.on('page-title-updated', (event) => {
		event.preventDefault();
		window.setTitle(windowTitle());
	});
	// a Studio with projects not saved asks before its page leaves, as the Eclipse Studio before it quits
	window.webContents.on('will-prevent-unload', (event) => {
		const choice = dialog.showMessageBoxSync(window, {
			type: 'question',
			buttons: ['Leave', 'Stay'],
			defaultId: 1,
			cancelId: 1,
			title: 'Convertigo Studio',
			message: 'Projects have changes not saved.',
			detail: 'They are lost if the Studio leaves now.'
		});
		if (choice === 0) {
			event.preventDefault();
		}
	});
	window.webContents.setWindowOpenHandler(({ url, referrer }) => {
		// the views moved to a window of their own, which the Studio fills and can put on another screen
		if (url.startsWith(`http://localhost:${enginePort}/`) && new URL(url).pathname.endsWith('/studio-popout.html')) {
			return {
				action: 'allow',
				overrideBrowserWindowOptions: { icon, backgroundColor: '#151516', minWidth: 320, minHeight: 240 }
			};
		}
		// the pages of the engine open in the application, the others in the browser
		if (url.startsWith(`http://localhost:${enginePort}`)) {
			return { action: 'allow' };
		}
		// the logins of the registration page of Convertigo Cloud keep their opener, as in the Eclipse Studio
		if (fromRegistration(url, referrer)) {
			return { action: 'allow', overrideBrowserWindowOptions: { icon, autoHideMenuBar: true } };
		}
		void shell.openExternal(url);
		return { action: 'deny' };
	});
	// the login page of an expired session gives way to the Studio, authenticated again
	let lastLogin = 0;
	const reopenStudio = (url) => {
		if (
			url.startsWith(`http://localhost:${enginePort}/convertigo/login`) &&
			Date.now() - lastLogin > 5000
		) {
			lastLogin = Date.now();
			void window.loadURL(studioUrl());
		}
	};
	window.webContents.on('did-navigate', (event, url) => reopenStudio(url));
	window.webContents.on('did-navigate-in-page', (event, url) => reopenStudio(url));
	await window.loadURL(splashPage());
	await openStudio(window);
}

function windowTitle() {
	return `Convertigo Studio — ${path.basename(workspaceDir)}`;
}

function studioUrl() {
	return `http://localhost:${enginePort}/convertigo/studio/#authToken=${authToken}`;
}

/**
 * Opens the Studio once its engine answers.
 * @param {BrowserWindow} window
 */
async function openStudio(window) {
	const started = Date.now();
	while (!(await engineAnswers(enginePort))) {
		if (!engine || Date.now() - started > 180_000) {
			dialog.showErrorBox('Convertigo engine', `The engine did not start. Its log is in:\n${logFile}`);
			app.quit();
			return;
		}
		await new Promise((resolve) => setTimeout(resolve, 500));
	}
	window.setTitle(windowTitle());
	await window.loadURL(studioUrl());
}

/**
 * Stops the engine, saving its sessions, and the node processes of its NGX builds.
 * @returns {Promise<void>}
 */
/**
 * Stops the PouchDB server of FullSync the engine started with the Node.js of the workspace: the engine starts it
 * again while it stops on a signal, and a server left running keeps the lock of the databases of the workspace,
 * which the next one then cannot open.
 */
function stopWorkspacePouchDB() {
	if (process.platform === 'win32') {
		return;
	}
	const nodes = `${path.join(workspaceDir, 'nodes')}${path.sep}`.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
	spawnSync('pkill', ['-f', `^${nodes}[^/]+/bin/node node_modules/pouchdb-server/`]);
}

/**
 * Sends the shutdown command of its Tomcat to the engine.
 * @returns {Promise<boolean>} whether the command was sent
 */
function sendShutdown() {
	return new Promise((resolve) => {
		if (!shutdownPort) {
			resolve(false);
			return;
		}
		const socket = net.connect(shutdownPort, '127.0.0.1', () => socket.end(authToken, () => resolve(true)));
		socket.on('error', () => resolve(false));
	});
}

/**
 * Stops the node processes the engine started from the workspace (the NGX builds and their servers, the
 * PouchDB server of FullSync), which the engine stops, even when it was killed. Windows has no pkill: PowerShell finds
 * them by their command line.
 */
function stopWorkspaceNodes() {
	if (process.platform === 'win32') {
		const dir = workspaceDir.replace(/'/g, "''");
		spawnSync('powershell.exe', [
			'-NoProfile',
			'-NonInteractive',
			'-Command',
			`Get-CimInstance Win32_Process -Filter "Name='node.exe'" | Where-Object { $_.CommandLine -and ($_.CommandLine.Contains('${dir}') -or ($_.ExecutablePath -and $_.ExecutablePath.StartsWith('${dir}'))) } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }`
		], { windowsHide: true });
		return;
	}
	const projects = `${path.join(workspaceDir, 'projects')}${path.sep}`.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
	spawnSync('pkill', ['-f', `^(node|npm|ng) .*${projects}`]);
	stopWorkspacePouchDB();
}

function stopEngine() {
	const running = engine;
	if (!running) {
		return Promise.resolve();
	}
	return new Promise((resolve) => {
		// killed after 15 s, its process tree with it on Windows
		const timer = setTimeout(() => {
			if (process.platform === 'win32') {
				spawnSync('taskkill', ['/pid', String(running.pid), '/T', '/F'], { windowsHide: true });
			} else {
				running.kill('SIGKILL');
			}
		}, 15_000);
		running.once('exit', () => {
			clearTimeout(timer);
			stopWorkspaceNodes();
			resolve();
		});
		// gracefully, as the Eclipse Studio stops its engine: the shutdown command of its Tomcat, else SIGTERM,
		// which Windows does not have (Node kills the process there)
		void sendShutdown().then((sent) => {
			if (!sent && process.platform !== 'win32') {
				running.kill('SIGTERM');
			}
		});
	});
}

/**
 * Asks a folder for the workspace.
 * @param {string} title
 * @param {string} buttonLabel
 * @returns {Promise<string>} the folder chosen, empty when none
 */
async function chooseWorkspace(title, buttonLabel) {
	const result = await dialog.showOpenDialog(mainWindow ?? undefined, {
		title,
		buttonLabel,
		defaultPath: workspaceDir,
		properties: ['openDirectory', 'createDirectory']
	});
	return result.canceled ? '' : (result.filePaths[0] ?? '');
}

/**
 * Serves the projects of another workspace: the Studio leaves, asking for its projects not saved, and its
 * engine starts again on the other folder.
 * @param {string} dir
 */
async function switchWorkspace(dir) {
	const next = dir ? path.resolve(dir) : '';
	const window = mainWindow;
	if (!next || next === workspaceDir || !window || switching) {
		return;
	}
	try {
		await window.loadURL(splashPage('Switching the workspace…'));
	} catch {
		// the page kept its projects not saved
		return;
	}
	if (!window.webContents.getURL().startsWith('data:')) {
		return;
	}
	switching = true;
	try {
		await stopEngine();
		workspaceDir = next;
		saveSettings();
		setMenu();
		window.setTitle(windowTitle());
		await window.loadURL(splashPage());
		enginePort = await workspacePort();
		shutdownPort = await freePort(enginePort + 1000);
		saveSettings();
		startEngine(enginePort);
	} finally {
		switching = false;
	}
	await openStudio(window);
}

/**
 * The menus of the application: ⌘W (Ctrl+W) closes the editor of the Studio, as in the Eclipse Studio, and
 * no longer the window, whose close would quit the Studio. ⌘Z and ⇧⌘Z (Ctrl+Z and Ctrl+Y) undo and redo the
 * text of a field, as the Edit menu does, and elsewhere go to the page: the Studio undoes the last change
 * of the project, a code editor its own text.
 */
function setMenu() {
	const isMac = process.platform === 'darwin';
	const close = { role: 'close', accelerator: 'Shift+CmdOrCtrl+W' };
	const modifier = isMac ? 'meta' : 'control';
	/** a field of the page, or of one of its frames, has its own undo; the code editors take the keys */
	const FIELD_FOCUSED = `(() => {
		const element = document.activeElement;
		return Boolean(element && !element.closest('.monaco-editor')
			&& (element.isContentEditable || /^(INPUT|TEXTAREA|SELECT|IFRAME)$/.test(element.tagName)));
	})()`;
	const edit = (action, keyCode, modifiers) => async (item, window) => {
		const contents = (window ?? BrowserWindow.getFocusedWindow())?.webContents;
		if (!contents) {
			return;
		}
		if (await contents.executeJavaScript(FIELD_FOCUSED).catch(() => true)) {
			contents[action]();
			return;
		}
		contents.sendInputEvent({ type: 'keyDown', keyCode, modifiers });
		contents.sendInputEvent({ type: 'keyUp', keyCode, modifiers });
	};
	Menu.setApplicationMenu(
		Menu.buildFromTemplate([
			...(isMac ? [{ role: 'appMenu' }] : []),
			{
				label: 'File',
				submenu: [
					{
						label: 'Switch Workspace',
						submenu: [
							...(settings.recent ?? [])
								.filter((dir) => dir !== workspaceDir)
								.map((dir) => ({ label: displayPath(dir), click: () => void switchWorkspace(dir) })),
							...((settings.recent ?? []).some((dir) => dir !== workspaceDir) ? [{ type: 'separator' }] : []),
							{
								label: 'Other…',
								click: async () =>
									void switchWorkspace(await chooseWorkspace('Switch the workspace', 'Switch'))
							},
							{ type: 'separator' },
							{
								label: 'Ask at Startup',
								type: 'checkbox',
								checked: settings.askAtStartup === true,
								click: (item) => {
									settings.askAtStartup = item.checked;
									saveSettings();
								}
							}
						]
					},
					{
						label: isMac ? 'Show Workspace in Finder' : 'Show Workspace Folder',
						click: () => void shell.openPath(workspaceDir)
					},
					{ type: 'separator' },
					close,
					...(isMac ? [] : [{ role: 'quit' }])
				]
			},
			{
				label: 'Edit',
				submenu: [
					{ label: 'Undo', accelerator: 'CmdOrCtrl+Z', click: edit('undo', 'Z', [modifier]) },
					isMac
						? { label: 'Redo', accelerator: 'Shift+Cmd+Z', click: edit('redo', 'Z', ['meta', 'shift']) }
						: { label: 'Redo', accelerator: 'Ctrl+Y', click: edit('redo', 'Y', ['control']) },
					{ type: 'separator' },
					{ role: 'cut' },
					{ role: 'copy' },
					{ role: 'paste' },
					...(isMac ? [{ role: 'pasteAndMatchStyle' }] : []),
					{ role: 'delete' },
					{ type: 'separator' },
					{ role: 'selectAll' }
				]
			},
			{ role: 'viewMenu' },
			{
				label: 'Window',
				submenu: [{ role: 'minimize' }, { role: 'zoom' }, ...(isMac ? [{ type: 'separator' }, { role: 'front' }] : [])]
			},
			{
				role: 'help',
				submenu: [
					{ label: 'Check for Updates…', click: () => void checkForUpdates(true) },
					{ label: 'Release Notes', click: () => void shell.openExternal(RELEASES_URL) }
				]
			}
		])
	);
}

/** the releases of Convertigo, whose Studio this application is */
const RELEASES_URL = 'https://github.com/convertigo/convertigo/releases';

/**
 * @param {string} version
 * @returns {number[]} its major, minor and patch numbers
 */
function versionNumbers(version) {
	return String(version)
		.replace(/^v/, '')
		.split(/[.-]/)
		.slice(0, 3)
		.map((part) => Number.parseInt(part, 10) || 0);
}

/**
 * @param {string} version
 * @param {string} current
 * @returns {boolean} whether a version is newer than the current one
 */
function isNewer(version, current) {
	const [a, b] = [versionNumbers(version), versionNumbers(current)];
	for (let i = 0; i < 3; i++) {
		if (a[i] !== b[i]) {
			return a[i] > b[i];
		}
	}
	return false;
}

/**
 * Tells when a newer Convertigo is released, as the Check for Updates of the Eclipse Studio: at a start, once a
 * day, of the packaged application, or when the user asks.
 * @param {boolean} asked
 */
async function checkForUpdates(asked) {
	const current = app.getVersion();
	try {
		const response = await fetch('https://api.github.com/repos/convertigo/convertigo/releases/latest', {
			headers: { accept: 'application/vnd.github+json', 'user-agent': 'Convertigo-Studio' },
			signal: AbortSignal.timeout(15_000)
		});
		if (!response.ok) {
			throw new Error(`HTTP ${response.status}`);
		}
		const release = await response.json();
		const version = String(release?.tag_name ?? '').replace(/^v/, '');
		settings.updateCheckedAt = Date.now();
		saveSettings();
		if (version && isNewer(version, current)) {
			const { response: choice } = await dialog.showMessageBox(mainWindow ?? undefined, {
				type: 'info',
				buttons: ['Download', 'Later'],
				defaultId: 0,
				cancelId: 1,
				title: 'Convertigo Studio',
				message: `Convertigo Studio ${version} is available`,
				detail: `This Studio is ${current}.`
			});
			if (choice === 0) {
				void shell.openExternal(String(release?.html_url ?? RELEASES_URL));
			}
		} else if (asked) {
			await dialog.showMessageBox(mainWindow ?? undefined, {
				type: 'info',
				title: 'Convertigo Studio',
				message: 'Convertigo Studio is up to date',
				detail: `This Studio is ${current}; the latest release is ${version || 'unknown'}.`
			});
		}
	} catch (error) {
		if (asked) {
			await dialog.showMessageBox(mainWindow ?? undefined, {
				type: 'warning',
				title: 'Convertigo Studio',
				message: 'The latest release cannot be checked',
				detail: String(error?.message ?? error)
			});
		}
	}
}

// the DevTools button of the preview inspects the application it shows, as the one of the Eclipse Studio
ipcMain.on('studio:inspect', (event, x, y) => {
	if (Number.isFinite(x) && Number.isFinite(y)) {
		event.sender.inspectElement(Math.round(x), Math.round(y));
	}
});

/** @type {BrowserWindow | null} */
let debugWindow = null;

// the DevTools of an application in a window of its own show its code only, rather than the Studio too
ipcMain.on('studio:debug-application', (event, url, storage) => {
	const origin = `http://localhost:${enginePort}/`;
	if (typeof url !== 'string' || !url.startsWith(origin)) {
		return;
	}
	if (!debugWindow || debugWindow.isDestroyed()) {
		const window = new BrowserWindow({
			width: 1280,
			height: 860,
			title: 'Application — Convertigo Studio',
			icon,
			backgroundColor: '#151516',
			show: false
		});
		window.once('ready-to-show', () => window.show());
		window.on('closed', () => {
			if (debugWindow === window) {
				debugWindow = null;
			}
		});
		window.webContents.setWindowOpenHandler(({ url: next }) => {
			if (next.startsWith(`http://localhost:${enginePort}`)) {
				return { action: 'allow' };
			}
			void shell.openExternal(next);
			return { action: 'deny' };
		});
		debugWindow = window;
	}
	const window = debugWindow;
	const contents = window.webContents;
	const entries = Object.entries(storage ?? {}).filter(([key, value]) => typeof key === 'string' && typeof value === 'string');
	// the application loads again with the session data of the preview, as its dataset
	if (entries.length) {
		contents.once('did-finish-load', async () => {
			await contents
				.executeJavaScript(
					`${entries.map(([key, value]) => `sessionStorage.setItem(${JSON.stringify(key)}, ${JSON.stringify(value)});`).join('')}true`
				)
				.catch(() => false);
			contents.reload();
		});
	}
	void window.loadURL(url);
	if (!contents.isDevToolsOpened()) {
		contents.openDevTools({ mode: 'right' });
	}
	window.show();
	window.focus();
});

app.whenReady().then(async () => {
	if (!app.isPackaged && process.platform === 'darwin' && fs.existsSync(icon)) {
		app.dock.setIcon(icon);
	}
	setMenu();
	// the Capture Manager of the Studio captures its own window, cropped to the preview
	session.defaultSession.setDisplayMediaRequestHandler((request, callback) => {
		callback({ video: request.frame });
	});
	try {
		if (settings.askAtStartup && !process.env.CONVERTIGO_STUDIO_WORKSPACE) {
			const chosen = await chooseWorkspace('Select the workspace of the Convertigo Studio', 'Launch');
			if (chosen) {
				workspaceDir = path.resolve(chosen);
			}
		}
		saveSettings();
		setMenu();
		if (!(await acceptLicense())) {
			app.quit();
			return;
		}
		enginePort = await workspacePort();
		shutdownPort = await freePort(enginePort + 1000);
		saveSettings();
		startEngine(enginePort);
		await createWindow();
		licenseWindow?.destroy();
		licenseWindow = null;
		if (app.isPackaged && Date.now() - (settings.updateCheckedAt ?? 0) > 24 * 3600_000) {
			void checkForUpdates(false);
		}
	} catch (error) {
		dialog.showErrorBox('Convertigo Studio', String(error?.stack ?? error));
		app.quit();
	}
});

app.on('second-instance', () => {
	const [window] = BrowserWindow.getAllWindows();
	if (window) {
		if (window.isMinimized()) window.restore();
		window.focus();
	}
});

app.on('window-all-closed', () => app.quit());

// the engine stops with the application, saving its sessions
app.on('before-quit', (event) => {
	const windows = BrowserWindow.getAllWindows();
	if (windows.length) {
		// the windows close first: a Studio with projects not saved may stay open, its engine running
		event.preventDefault();
		windows.forEach((window) => window.close());
		return;
	}
	if (!engine) {
		return;
	}
	quitting = true;
	event.preventDefault();
	void stopEngine().then(() => app.quit());
});

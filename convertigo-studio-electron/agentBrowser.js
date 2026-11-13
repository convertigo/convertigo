// The browser the agents of the Assistant drive to check the applications they build, as the browser of a project
// in the Eclipse Studio: a process of its own, which the Studio starts with the remote debugging port it gives its
// engine (-Dconvertigo.studio.cdp.port), on the loopback only. Its one page shows the applications of the Studio,
// authenticated with the token of the Studio, and never the Studio itself, which the agents then cannot drive.
// Its window shows as soon as an agent opens a page in it, without taking the focus, and hides when closed.
const { app, BrowserWindow, net, session, shell } = require('electron');
const path = require('node:path');

const port = Number(process.env.CONVERTIGO_AGENT_BROWSER_PORT);
const enginePort = Number(process.env.CONVERTIGO_STUDIO_ENGINE_PORT);
const authToken = process.env.CONVERTIGO_STUDIO_AUTH_TOKEN ?? '';
const parentPid = Number(process.env.CONVERTIGO_STUDIO_PID);
const engineOrigin = `http://localhost:${enginePort}`;
/** the pages of the development servers, which the engine serves to a user of the Studio only */
const DEVELOPMENT_PAGE = /^\/convertigo\/projects\/[^/]+\/DisplayObjects\/dev\d+\//;

if (!(port > 0) || !(enginePort > 0) || !authToken) {
	app.exit(1);
}
// its own data, beside the ones of the Studio, which holds them
app.setPath('userData', path.join(process.env.CONVERTIGO_STUDIO_DATA || app.getPath('userData'), 'agent-browser'));
app.commandLine.appendSwitch('remote-debugging-port', String(port));
// it stops with the Studio, even killed
if (parentPid > 0) {
	setInterval(() => {
		try {
			process.kill(parentPid, 0);
		} catch {
			app.exit(0);
		}
	}, 2000).unref();
}

let authenticatedAt = 0;

/**
 * Authenticates the session of the browser as the Studio, with its token: the engine serves the development
 * servers to a user of the Studio only.
 * @returns {Promise<boolean>}
 */
function authenticate() {
	return new Promise((resolve) => {
		const request = net.request({
			method: 'POST',
			url: `${engineOrigin}/convertigo/admin/services/engine.Authenticate`,
			session: session.defaultSession,
			useSessionCookies: true
		});
		request.setHeader('content-type', 'application/x-www-form-urlencoded');
		request.on('response', (response) => {
			response.on('data', () => {});
			response.on('end', () => {
				authenticatedAt = response.statusCode === 200 ? Date.now() : 0;
				resolve(authenticatedAt > 0);
			});
		});
		request.on('error', () => resolve(false));
		request.end(new URLSearchParams({ authToken, authType: 'login' }).toString());
	});
}

/** @param {string} url */
function isDevelopmentPage(url) {
	try {
		const parsed = new URL(url);
		return parsed.origin === engineOrigin && DEVELOPMENT_PAGE.test(parsed.pathname);
	} catch {
		return false;
	}
}

app.dock?.hide();

app.whenReady().then(async () => {
	// the engine starts with the Studio: authenticated once it answers
	void (async () => {
		for (let tries = 0; tries < 150 && !(await authenticate()); tries++) {
			await new Promise((resolve) => setTimeout(resolve, 2000));
		}
	})();
	// a page of the engine opens authenticated, again after a minute, as the session may have ended
	session.defaultSession.webRequest.onBeforeRequest((details, callback) => {
		if (details.resourceType === 'mainFrame' && details.url.startsWith(`${engineOrigin}/`) && Date.now() - authenticatedAt > 60_000) {
			void authenticate().finally(() => callback({}));
		} else {
			callback({});
		}
	});
	const window = new BrowserWindow({
		width: 1280,
		height: 860,
		title: 'Agent Browser',
		backgroundColor: '#ffffff',
		show: false,
		// it shows without taking the focus: the first click on it, on macOS, goes to its page as well
		acceptFirstMouse: true,
		webPreferences: { backgroundThrottling: false }
	});
	let quitting = false;
	app.on('before-quit', () => {
		quitting = true;
	});
	// closed, it hides: the agents keep their page
	window.on('close', (event) => {
		if (!quitting) {
			event.preventDefault();
			window.hide();
		}
	});
	window.on('page-title-updated', (event, title) => {
		event.preventDefault();
		window.setTitle(title ? `Agent Browser — ${title}` : 'Agent Browser');
	});
	const contents = window.webContents;
	let retried = '';
	contents.on('did-navigate', (event, url, httpResponseCode) => {
		if (url !== 'about:blank' && !window.isVisible()) {
			window.showInactive();
		}
		// a session that ended answers 404 to a page of a development server: once more, authenticated
		if (httpResponseCode === 404 && isDevelopmentPage(url) && retried !== url) {
			retried = url;
			void authenticate().then((done) => done && contents.reload());
		} else if (httpResponseCode !== 404) {
			retried = '';
		}
	});
	// the windows a page opens show in its window, the external pages in the browser of the system
	contents.setWindowOpenHandler(({ url }) => {
		if (url.startsWith(`${engineOrigin}/`)) {
			void contents.loadURL(url);
		} else if (/^https?:/.test(url)) {
			void shell.openExternal(url);
		}
		return { action: 'deny' };
	});
	await window.loadURL('about:blank');
});

// its window only hides
app.on('window-all-closed', () => {});

// The browser of the agents, shown in the Studio as its Agent view: the launcher reads the frames its page paints, as
// its remote debugging port gives them while they change, hands them to the Studio page, and gives the page back the
// clicks, the wheel and the keys of the user in the view, as the screencast of the developer tools does. The Studio
// never gets that port itself: only the frames, the address of the page and the input it sends.
const { ipcMain } = require('electron');

/** how long the Studio waits before it asks the browser of the agents again, when it is off or restarts */
const RETRY_MS = 2000;

/**
 * @typedef {Object} AgentScreenHost
 * @property {() => number} port the remote debugging port of the browser of the agents, 0 when it has none
 * @property {() => boolean} running whether it runs
 * @property {() => void} show brings its window forward
 */

/**
 * @param {AgentScreenHost} host
 */
function connectAgentScreen(host) {
	/** @type {Map<number, AgentScreenViewer>} the Studio pages watching, by their web contents */
	const viewers = new Map();
	/** @type {WeakSet<import('electron').WebContents>} the web contents whose end stops their viewer */
	const followed = new WeakSet();

	/**
	 * @param {import('electron').WebContents} contents
	 * @param {boolean} frames whether the view shows the page, else it only follows its address
	 */
	function watch(contents, frames) {
		// a page loaded again in the same web contents watches anew: the state, and a frame, come at once
		viewers.get(contents.id)?.stop();
		const viewer = new AgentScreenViewer(contents, host, frames);
		viewers.set(contents.id, viewer);
		if (!followed.has(contents)) {
			followed.add(contents);
			const end = () => {
				viewers.get(contents.id)?.stop();
				viewers.delete(contents.id);
			};
			contents.once('destroyed', end);
			// the page loaded again stops watching, until it asks again
			contents.on('did-navigate', end);
		}
		viewer.start();
	}

	ipcMain.on('studio:agent-screen', (event, action) => {
		const contents = event.sender;
		if (action === 'start' || action === 'watch') {
			watch(contents, action === 'start');
		} else if (action === 'stop') {
			viewers.get(contents.id)?.stop();
			viewers.delete(contents.id);
		} else if (action === 'show') {
			host.show();
		}
	});

	ipcMain.on('studio:agent-screen-input', (event, input) => {
		viewers.get(event.sender.id)?.input(input);
	});
}

/** the keys whose code the page needs, beside their name, to act as typed */
const KEY_CODES = {
	Backspace: 8,
	Tab: 9,
	Enter: 13,
	Escape: 27,
	PageUp: 33,
	PageDown: 34,
	End: 35,
	Home: 36,
	ArrowLeft: 37,
	ArrowUp: 38,
	ArrowRight: 39,
	ArrowDown: 40,
	Delete: 46
};

/**
 * @param {any} value
 * @returns {number}
 */
function number(value) {
	const result = Number(value);
	return Number.isFinite(result) ? result : 0;
}

class AgentScreenViewer {
	/**
	 * @param {import('electron').WebContents} contents the Studio page watching
	 * @param {AgentScreenHost} host
	 * @param {boolean} frames whether it gets the frames, else the address of the page only
	 */
	constructor(contents, host, frames) {
		this.contents = contents;
		this.host = host;
		this.frames = frames;
		/** @type {WebSocket | null} */
		this.socket = null;
		/** @type {ReturnType<typeof setTimeout> | undefined} */
		this.timer = undefined;
		this.stopped = false;
		this.state = '';
		this.id = 0;
	}

	start() {
		this.stopped = false;
		void this.connect();
	}

	stop() {
		this.stopped = true;
		clearTimeout(this.timer);
		const socket = this.socket;
		this.socket = null;
		try {
			socket?.close();
		} catch {
			// already closed
		}
	}

	/**
	 * @param {string} method
	 * @param {any} [params]
	 * @returns {number} the id of the command
	 */
	send(method, params = {}) {
		const socket = this.socket;
		if (socket?.readyState === WebSocket.OPEN) {
			socket.send(JSON.stringify({ id: ++this.id, method, params }));
		}
		return this.id;
	}

	/**
	 * The input of the user in the view, for the page, its coordinates in the pixels of the page.
	 * @param {any} input
	 */
	input(input) {
		if (!this.frames || !input || typeof input !== 'object') {
			return;
		}
		const modifiers = number(input.modifiers) & 15;
		if (input.type === 'mouse' && ['mousePressed', 'mouseReleased', 'mouseMoved'].includes(input.action)) {
			this.send('Input.dispatchMouseEvent', {
				type: input.action,
				x: number(input.x),
				y: number(input.y),
				button: ['left', 'middle', 'right'].includes(input.button) ? input.button : 'none',
				clickCount: Math.max(0, Math.min(3, number(input.clickCount))),
				modifiers
			});
		} else if (input.type === 'wheel') {
			this.send('Input.dispatchMouseEvent', {
				type: 'mouseWheel',
				x: number(input.x),
				y: number(input.y),
				deltaX: number(input.deltaX),
				deltaY: number(input.deltaY),
				modifiers
			});
		} else if (input.type === 'key' && ['keyDown', 'keyUp'].includes(input.action)) {
			const key = String(input.key ?? '').slice(0, 32);
			// a shortcut with Control or Command types nothing: the page takes it as a command
			const typing = input.action === 'keyDown' && !(modifiers & 6);
			const text = typing ? (key === 'Enter' ? '\r' : key.length === 1 ? key : '') : '';
			this.send('Input.dispatchKeyEvent', {
				type: text ? 'keyDown' : input.action === 'keyDown' ? 'rawKeyDown' : 'keyUp',
				key,
				code: String(input.code ?? '').slice(0, 32),
				windowsVirtualKeyCode: KEY_CODES[/** @type {keyof typeof KEY_CODES} */ (key)] ?? (key.length === 1 ? key.toUpperCase().charCodeAt(0) : 0),
				text,
				unmodifiedText: text,
				modifiers
			});
		}
	}

	/** @param {string} state on, off (not running) or waiting (no page yet) */
	tell(state, url = '') {
		if (this.contents.isDestroyed()) {
			return;
		}
		if (state !== this.state || url) {
			this.state = state;
			this.contents.send('studio:agent-screen-state', { state, url });
		}
	}

	retry() {
		clearTimeout(this.timer);
		if (!this.stopped) {
			this.timer = setTimeout(() => void this.connect(), RETRY_MS);
		}
	}

	async connect() {
		if (this.stopped) {
			return;
		}
		const port = this.host.port();
		if (!(port > 0) || !this.host.running()) {
			this.tell('off');
			this.retry();
			return;
		}
		let page;
		try {
			const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
			page = targets.find((/** @type {any} */ target) => target.type === 'page');
		} catch {
			page = null;
		}
		if (this.stopped) {
			return;
		}
		if (!page?.webSocketDebuggerUrl) {
			this.tell('off');
			this.retry();
			return;
		}
		const socket = new WebSocket(page.webSocketDebuggerUrl);
		this.socket = socket;
		/** the main frame of the page, whose address the view shows */
		let mainFrame = '';
		let frameTreeRequest = 0;
		const showAddress = (/** @type {string} */ url) =>
			url && url !== 'about:blank' ? this.tell('on', url) : this.tell('waiting');
		socket.onopen = () => {
			showAddress(page.url ?? '');
			this.send('Page.enable');
			// the page as it is now, which may have navigated since the list of the targets was read
			frameTreeRequest = this.send('Page.getFrameTree');
			if (this.frames) {
				// the frames as the page paints them, only when it changes, small enough for a view of the Studio
				this.send('Page.startScreencast', { format: 'jpeg', quality: 70, maxWidth: 1600, maxHeight: 1200 });
			}
		};
		socket.onmessage = (event) => {
			let message;
			try {
				message = JSON.parse(String(event.data));
			} catch {
				return;
			}
			if (message.id && message.id === frameTreeRequest) {
				const frame = message.result?.frameTree?.frame;
				mainFrame = String(frame?.id ?? '');
				showAddress(String(frame?.url ?? ''));
			} else if (message.method === 'Page.screencastFrame') {
				const { data, metadata, sessionId } = message.params;
				this.send('Page.screencastFrameAck', { sessionId });
				if (!this.contents.isDestroyed()) {
					this.contents.send('studio:agent-screen-frame', {
						data,
						width: Math.round(metadata?.deviceWidth ?? 0),
						height: Math.round(metadata?.deviceHeight ?? 0)
					});
				}
			} else if (message.method === 'Page.frameNavigated' && !message.params?.frame?.parentId) {
				mainFrame = String(message.params.frame.id ?? mainFrame);
				showAddress(String(message.params.frame.url ?? ''));
			} else if (
				message.method === 'Page.navigatedWithinDocument' &&
				(!mainFrame || message.params?.frameId === mainFrame)
			) {
				// the routes of an application, which do not load a document
				showAddress(String(message.params.url ?? ''));
			}
		};
		socket.onclose = () => {
			if (this.socket === socket) {
				this.socket = null;
				// the browser of the agents stopped, or restarts: the view waits for it
				this.tell('off');
				this.retry();
			}
		};
		socket.onerror = () => {
			try {
				socket.close();
			} catch {
				// closing
			}
		};
	}
}

module.exports = { connectAgentScreen };

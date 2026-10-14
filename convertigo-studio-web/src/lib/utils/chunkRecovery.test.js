import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { afterEach, describe, expect, it, vi } from 'vitest';

const source = readFileSync(new URL('../../../static/chunk-recovery.js', import.meta.url), 'utf8');
const chunk = 'https://cloud.test/convertigo/monaco/vs/editor-KLE6jdfb.js';

function runtime({ status = 404, storage = new Map(), online = true } = {}) {
	class Element {
		constructor(tagName) {
			this.tagName = tagName;
			this.children = [];
			this.style = {};
			this.listeners = {};
		}
		setAttribute(name, value) {
			this[name] = value;
		}
		append(...children) {
			this.children.push(...children);
		}
		prepend(child) {
			this.children.unshift(child);
		}
		addEventListener(type, listener) {
			this.listeners[type] = listener;
		}
	}
	class Script extends Element {}
	const script = new Script('script');
	script.src = 'https://cloud.test/convertigo/chunk-recovery.js';
	const document = {
		currentScript: script,
		baseURI: 'https://cloud.test/convertigo/dashboard/Demo/backend/',
		body: new Element('body'),
		createElement: (tag) => new Element(tag),
		addEventListener: vi.fn()
	};
	const listeners = {};
	const window = {
		addEventListener: (name, fn) => {
			listeners[name] = fn;
		},
		setTimeout,
		clearTimeout,
		confirm: vi.fn(() => true)
	};
	const location = {
		origin: 'https://cloud.test',
		href: 'https://cloud.test/convertigo/dashboard/Demo/backend/?view=json#response',
		reload: vi.fn()
	};
	const sessionStorage = {
		getItem: (key) => storage.get(key) ?? null,
		setItem: (key, value) => {
			storage.set(key, value);
		}
	};
	const fetch = vi.fn(async () => ({ status, redirected: false }));
	const performance = { getEntriesByType: () => [], now: () => 1000 };
	vm.runInNewContext(source, {
		window,
		document,
		location,
		sessionStorage,
		fetch,
		navigator: { onLine: online },
		HTMLScriptElement: Script,
		URL,
		AbortController,
		performance,
		Date
	});
	return {
		document,
		window,
		location,
		storage,
		sessionStorage,
		fetch,
		listeners,
		performance,
		report: window.convertigoChunkRecovery.report,
		banner: () => document.body.children[0],
		reloadButton: () => document.body.children[0].children[1]
	};
}

afterEach(() => vi.useRealTimers());

describe('controlled recovery of missing application chunks', () => {
	it.each([404, 410])('offers recovery only after a confirmed %i', async (status) => {
		const r = runtime({ status });
		expect(await r.report(new Error(`Failed to fetch dynamically imported module: ${chunk}`))).toBe(
			true
		);
		expect(r.banner().role).toBe('alert');
		expect(r.fetch).toHaveBeenCalledWith(
			chunk,
			expect.objectContaining({
				method: 'HEAD',
				cache: 'no-store',
				credentials: 'same-origin',
				redirect: 'error'
			})
		);
		expect(r.location.reload).not.toHaveBeenCalled();
	});

	it.each([200, 401, 403, 500, 502, 503])(
		'does not treat HTTP %i as an obsolete chunk',
		async (status) => {
			const r = runtime({ status });
			expect(await r.report(chunk)).toBe(false);
			expect(r.banner()).toBeUndefined();
		}
	);

	it('ignores APIs, other origins and other applications', async () => {
		const r = runtime();
		for (const path of [
			'https://elsewhere.test/convertigo/monaco/vs/editor-ABC12345.js',
			'https://cloud.test/convertigo/admin/services/engine.GetStatus',
			'https://cloud.test/another/_app/immutable/chunks/main.12345678.js',
			'https://cloud.test/convertigo/projects/Demo/DisplayObjects/mobile/main-ABC12345.js',
			'https://cloud.test/convertigo/monaco/vs/../../admin/services/test.js'
		]) {
			expect(await r.report(path)).toBe(false);
		}
		expect(r.fetch).not.toHaveBeenCalled();
	});

	it('ignores offline, redirected and failed probes', async () => {
		const offline = runtime({ online: false });
		expect(await offline.report(chunk)).toBe(false);
		expect(offline.fetch).not.toHaveBeenCalled();
		const r = runtime();
		r.fetch.mockRejectedValueOnce(new Error('network unavailable'));
		expect(await r.report(chunk)).toBe(false);
		r.fetch.mockResolvedValueOnce({ status: 404, redirected: true });
		expect(await r.report(chunk)).toBe(false);
		expect(r.banner()).toBeUndefined();
	});

	it('captures the Monaco script error and Vite preload event without duplicate notices', async () => {
		const r = runtime();
		let resolve;
		r.fetch.mockReturnValueOnce(
			new Promise((done) => {
				resolve = done;
			})
		);
		r.listeners.error({ target: { src: chunk } });
		r.listeners['vite:preloadError']({ payload: new Error(`Loading ${chunk} failed`) });
		expect(r.fetch).toHaveBeenCalledTimes(1);
		resolve({ status: 404, redirected: false });
		await vi.waitFor(() => expect(r.document.body.children).toHaveLength(1));
	});

	it('uses recent resource timings if an import error does not contain its URL', async () => {
		const r = runtime();
		r.performance.getEntriesByType = () => [{ name: chunk, startTime: 500 }];
		expect(await r.report(new Error('error loading dynamically imported module'), true)).toBe(true);
	});

	it('does not leave the page when confirmation is cancelled, and Later preserves the document', async () => {
		const r = runtime();
		await r.report(chunk);
		r.window.confirm.mockReturnValueOnce(false);
		r.reloadButton().listeners.click();
		expect(r.location.reload).not.toHaveBeenCalled();
		expect(r.storage.size).toBe(0);
		r.banner().children[2].listeners.click();
		expect(r.banner().style.display).toBe('none');
	});

	it('reloads the same URL only once and keeps the guard across documents', async () => {
		vi.useFakeTimers();
		const r = runtime();
		await r.report(chunk);
		const href = r.location.href;
		r.reloadButton().listeners.click();
		r.reloadButton().listeners.click();
		expect(r.location.reload).toHaveBeenCalledTimes(1);
		expect(r.location.href).toBe(href);
		const next = runtime({ storage: r.storage });
		await next.report(chunk);
		expect(next.reloadButton().disabled).toBe(true);
		next.reloadButton().listeners.click();
		expect(next.location.reload).not.toHaveBeenCalled();
		vi.advanceTimersByTime(60_000);
		expect(next.reloadButton().disabled).toBe(false);
	});

	it('does not reload if the persistent loop guard cannot be stored', async () => {
		const r = runtime();
		await r.report(chunk);
		r.sessionStorage.setItem = () => {
			throw new Error('storage blocked');
		};
		r.reloadButton().listeners.click();
		expect(r.location.reload).not.toHaveBeenCalled();
		expect(r.reloadButton().disabled).toBe(true);
	});

	it('protects against an independent framework reload until the user confirms', async () => {
		vi.useFakeTimers();
		const r = runtime();
		await r.report(chunk);
		const event = { preventDefault: vi.fn(), returnValue: undefined };
		r.listeners.beforeunload(event);
		expect(event.preventDefault).toHaveBeenCalledOnce();
		r.reloadButton().listeners.click();
		const allowed = { preventDefault: vi.fn() };
		r.listeners.beforeunload(allowed);
		expect(allowed.preventDefault).not.toHaveBeenCalled();
		// A native unsaved-change dialog may veto navigation after our own confirmation.
		vi.advanceTimersByTime(1000);
		const cancelled = { preventDefault: vi.fn() };
		r.listeners.beforeunload(cancelled);
		expect(cancelled.preventDefault).toHaveBeenCalledOnce();
	});
});

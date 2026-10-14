/* Recovery is independent of the application bundles so it also works during bootstrap. */
(() => {
	const script = document.currentScript;
	if (!(script instanceof HTMLScriptElement)) return;
	const base = new URL('.', script.src).pathname.replace(/\/$/, '');
	const storageKey = `convertigo:chunk-recovery:${base}`;
	const cooldown = 60_000;
	const pending = new Set();
	let banner;
	let leaving = false;
	let missing = false;

	function assetUrl(value) {
		try {
			const url = new URL(value, document.baseURI);
			if (url.origin !== location.origin || url.username || url.password) return null;
			if (!/\.(?:m?js|css)$/.test(url.pathname)) return null;
			if (
				!url.pathname.startsWith(`${base}/_app/immutable/`) &&
				!url.pathname.startsWith(`${base}/monaco/vs/`)
			)
				return null;
			return url;
		} catch {
			return null;
		}
	}

	function candidates(error, useResourceTiming = false) {
		const urls = new Map();
		const seen = new Set();
		function visit(value, depth = 0) {
			if (!value || depth > 4 || seen.has(value)) return;
			if (typeof value === 'string') {
				for (const raw of value.match(
					/https?:\/\/[^\s"'<>`]+|\/(?:[^\s"'<>`]*\/)?(?:_app\/immutable|monaco\/vs)\/[^\s"'<>`]+/g
				) ?? []) {
					const url = assetUrl(raw.replace(/[),;]+$/, '').replace(/:\d+(?::\d+)?$/, ''));
					if (url) urls.set(url.href, url);
				}
				return;
			}
			if (typeof value !== 'object') return;
			seen.add(value);
			for (const field of [
				'src',
				'href',
				'filename',
				'message',
				'error',
				'cause',
				'reason',
				'detail',
				'target',
				'payload',
				'originalError'
			]) {
				visit(value[field], depth + 1);
			}
		}
		visit(error);
		// Firefox does not always put a failing import URL in the exception message.
		if (!urls.size && useResourceTiming) {
			for (const entry of performance.getEntriesByType('resource').slice(-12)) {
				if (performance.now() - entry.startTime > 5000) continue;
				const url = assetUrl(entry.name);
				if (url) urls.set(url.href, url);
			}
		}
		return [...urls.values()];
	}

	function recentlyReloaded() {
		try {
			const time = Number(sessionStorage.getItem(storageKey));
			const age = Date.now() - time;
			return Number.isFinite(time) && age >= 0 && age < cooldown;
		} catch {
			return false;
		}
	}

	function offerReload() {
		if (banner) return;
		if (!document.body) {
			document.addEventListener('DOMContentLoaded', offerReload, { once: true });
			return;
		}
		banner = document.createElement('aside');
		banner.id = 'convertigo-chunk-recovery';
		banner.setAttribute('role', 'alert');
		banner.className = 'layout-x-wrap-low px py-low preset-filled-warning-500 shadow-follow';
		// Keep the notice usable even if the stylesheet itself is the missing chunk.
		banner.style.cssText =
			'position:fixed;inset:0 0 auto;z-index:10000;display:flex;align-items:center;gap:1rem;padding:1rem;background:#fff4ce;color:#242424;font:14px sans-serif';
		const message = document.createElement('span');
		const reload = document.createElement('button');
		reload.type = 'button';
		reload.className = 'button-primary';
		reload.textContent = 'Reload';
		function updateNotice() {
			reload.disabled = recentlyReloaded();
			message.textContent = reload.disabled
				? 'Some resources are still unavailable. Please wait a minute before trying again.'
				: 'This page needs to be refreshed. Save your changes, then reload.';
		}
		updateNotice();
		reload.addEventListener('click', () => {
			if (leaving || recentlyReloaded()) return;
			if (!window.confirm('Reload this page? Any unsaved changes will be lost.')) return;
			try {
				sessionStorage.setItem(storageKey, String(Date.now()));
			} catch {
				message.textContent = 'Please save your changes and refresh this page manually.';
				reload.disabled = true;
				return;
			}
			leaving = true;
			reload.disabled = true;
			location.reload();
			// Another beforeunload guard can cancel the navigation after our confirmation.
			// If this document survives, restore its notice and protection instead of leaving it stuck.
			window.setTimeout(() => {
				leaving = false;
				updateNotice();
				window.setTimeout(updateNotice, cooldown);
			}, 1000);
		});
		const later = document.createElement('button');
		later.type = 'button';
		later.className = 'button-secondary';
		later.textContent = 'Later';
		later.addEventListener('click', () => {
			banner.hidden = true;
			banner.style.display = 'none';
		});
		banner.append(message, reload, later);
		document.body.prepend(banner);
		if (reload.disabled) window.setTimeout(updateNotice, cooldown);
	}

	async function probe(url) {
		if (pending.has(url.href) || pending.size >= 4 || navigator.onLine === false) return false;
		pending.add(url.href);
		const controller = new AbortController();
		const timeout = window.setTimeout(() => controller.abort(), 4000);
		try {
			const response = await fetch(url.href, {
				method: 'HEAD',
				cache: 'no-store',
				credentials: 'same-origin',
				redirect: 'error',
				signal: controller.signal
			});
			if ((response.status !== 404 && response.status !== 410) || response.redirected) return false;
			missing = true;
			offerReload();
			return true;
		} catch {
			return false;
		} finally {
			window.clearTimeout(timeout);
			pending.delete(url.href);
		}
	}

	async function report(error, useResourceTiming = false) {
		try {
			const results = await Promise.all(candidates(error, useResourceTiming).map(probe));
			return results.some(Boolean);
		} catch {
			return false;
		}
	}

	window.convertigoChunkRecovery = { report };
	window.addEventListener(
		'error',
		(event) => {
			void report(event);
		},
		true
	);
	window.addEventListener('unhandledrejection', (event) => {
		const importError = /dynamically imported|Importing a module|module script/i.test(
			event.reason?.message ?? ''
		);
		void report(event.reason, importError);
	});
	window.addEventListener('vite:preloadError', (event) => {
		void report(event.payload, true);
	});
	// SvelteKit also has its own native-navigation fallback. Keep that from silently
	// discarding edits while a chunk failure is being confirmed or has been confirmed.
	window.addEventListener('beforeunload', (event) => {
		if (!leaving && (missing || pending.size)) {
			event.preventDefault();
			event.returnValue = '';
		}
	});
})();

const DEFAULT_TTL_MS = 30_000;

/**
 * Correlate the delayed `projects.changed` event emitted by a local source
 * mutation. The Engine publishes the exact source path as `payload.reason`, so
 * Studio can consume its own event without hiding unrelated project changes.
 * @param {{ now?: () => number, ttlMs?: number }} [options]
 */
function createStudioMutationEventTracker(options = {}) {
	const now = options.now ?? Date.now;
	const ttlMs = options.ttlMs ?? DEFAULT_TTL_MS;
	/** @type {Map<string, { count: number, expiresAt: number }>} */
	const pending = new Map();

	/** @param {any} mutation */
	function remember(mutation) {
		const paths = new Set(
			[mutation?.selectionSourcePath, mutation?.projectedSourcePath]
				.map((path) => String(path ?? '').trim())
				.filter(Boolean)
		);
		const expiresAt = now() + ttlMs;
		for (const path of paths) {
			const entry = activeEntry(path);
			pending.set(path, { count: (entry?.count ?? 0) + 1, expiresAt });
		}
	}

	/** @param {any} event */
	function consume(event) {
		if (event?.topic !== 'projects.changed' || event?.payload?.scope !== 'flow') {
			return false;
		}
		const path = String(event.payload.reason ?? '').trim();
		const entry = activeEntry(path);
		if (!entry) {
			return false;
		}
		if (entry.count > 1) {
			pending.set(path, { ...entry, count: entry.count - 1 });
		} else {
			pending.delete(path);
		}
		return true;
	}

	/** @param {string} path */
	function activeEntry(path) {
		const entry = pending.get(path);
		if (entry && entry.expiresAt >= now()) {
			return entry;
		}
		pending.delete(path);
		return undefined;
	}

	function clear() {
		pending.clear();
	}

	return { remember, consume, clear };
}

export { createStudioMutationEventTracker };

/**
 * @param {unknown} value
 */
export function fullSyncPretty(value) {
	return JSON.stringify(value ?? {}, null, 2);
}

/**
 * @param {string} content
 * @param {any} fallback
 * @returns {any}
 */
export function parseFullSyncJsonSilent(content, fallback = null) {
	try {
		return JSON.parse(content);
	} catch {
		return fallback;
	}
}

/**
 * @param {string} content
 * @param {string} label
 * @param {(message: string) => void} onError
 */
export function parseFullSyncJson(content, label, onError) {
	try {
		return JSON.parse(content);
	} catch (error) {
		const message = error instanceof Error ? error.message : String(error);
		onError?.(`${label}: invalid JSON (${message})`);
		return null;
	}
}

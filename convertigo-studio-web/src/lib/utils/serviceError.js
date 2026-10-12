/**
 * The message of the error a service answered: a text, or the error of the engine, as
 * { message, exception, stacktrace }, which String() would show as "[object Object]".
 * @param {unknown} error
 * @param {string} [fallback]
 * @returns {string}
 */
export function serviceErrorMessage(error, fallback = '') {
	if (error == null || error === '') {
		return fallback;
	}
	if (typeof error !== 'object') {
		return String(error);
	}
	const { message, exception } = /** @type {{ message?: unknown, exception?: unknown }} */ (error);
	return message || exception ? String(message || exception) : fallback || JSON.stringify(error);
}

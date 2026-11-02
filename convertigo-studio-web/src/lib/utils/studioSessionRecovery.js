/** Recognize the servlet session errors without hiding unrelated IllegalStateExceptions. */
export function isInvalidatedSessionError(error) {
	return (
		typeof error == 'object' &&
		error != null &&
		error.exception == 'java.lang.IllegalStateException' &&
		/\bsession (?:already|has been) invalidated\b/i.test(error.message ?? '')
	);
}

/**
 * Ask AuthenticatedBrowser to load a fresh authenticated URL on its next reconnect check.
 * The failed service is never replayed: it may have already performed a mutation.
 * @param {any} target
 */
export function requestStudioSessionRecovery(target) {
	if (
		!target ||
		new URLSearchParams(target.location.search).get('studioMode') != 'true' ||
		typeof target.java?.receiveFromJS != 'function'
	) {
		return false;
	}
	target.__c8oSessionExpired = true;
	return true;
}

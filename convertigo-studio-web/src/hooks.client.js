/** @type {import('@sveltejs/kit/hooks').HandleClientError} */
export async function handleError({ error }) {
	// The recovery script is loaded before the bundles, including during bootstrap.
	try {
		await /** @type {any} */ (window).convertigoChunkRecovery?.report(error, true);
	} catch {
		// Recovery must not replace SvelteKit's original error with a second exception.
	}
}

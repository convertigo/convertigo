/**
 * A text asked to the user in a dialog of the Studio, as window.prompt, which the desktop Studio (Electron)
 * does not support: StudioPromptDialog shows the question the page asks.
 *
 * @typedef {{ message: string, value: string, resolve: (value: string | null) => void }} StudioPromptRequest
 */
export const studioPromptState = $state({
	request: /** @type {StudioPromptRequest | null} */ (null)
});

/**
 * @param {string} message
 * @param {string} [value] the text proposed
 * @returns {Promise<string | null>} the text entered, or null when the question is cancelled
 */
export function studioPrompt(message, value = '') {
	studioPromptState.request?.resolve(null);
	return new Promise((resolve) => {
		studioPromptState.request = { message, value, resolve };
	});
}

/**
 * @param {string | null} value
 */
export function answerStudioPrompt(value) {
	const request = studioPromptState.request;
	studioPromptState.request = null;
	request?.resolve(value);
}

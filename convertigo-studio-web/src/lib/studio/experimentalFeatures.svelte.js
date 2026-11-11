import { call } from '#lib/utils/service.js';
import { saveStudioPreferences, studioPreferences } from './studioPreferences.svelte.js';

/**
 * The new experimental features (native JSON sequences, native JSON steps, JSON path sources), shown as the
 * preference "Show new experimental features" of the Eclipse Studio shows them: the engine follows the
 * preference of the user for the calls of its session (palette, properties), the Studio for its menus and
 * pickers.
 */
export const experimentalFeatures = $state({
	/** shown to this user, by the preference or by the engine for everyone */
	shown: false,
	/** changes each time the palette and the properties have to be read again */
	serial: 0
});

/**
 * Gives the preference of this browser to the session of the engine, once the Studio opens.
 */
export async function syncExperimentalFeatures() {
	await applyExperimentalFeatures(studioPreferences.experimentalFeatures === true);
}

/**
 * @param {boolean} shown
 */
export async function setExperimentalFeatures(shown) {
	saveStudioPreferences({ experimentalFeatures: shown });
	await applyExperimentalFeatures(shown);
}

/**
 * @param {boolean} wanted
 */
async function applyExperimentalFeatures(wanted) {
	const current = await call('studio.preferences.Experimental', {}, { silentError: () => true });
	let state = current;
	if (current && typeof current.shown === 'boolean' && current.shown !== wanted) {
		state = await call(
			'studio.preferences.Experimental',
			{ shown: String(wanted) },
			{ silentError: () => true }
		);
	}
	const shown = state?.shown === true || state?.engine === true;
	if (shown !== experimentalFeatures.shown || state !== current) {
		experimentalFeatures.shown = shown;
		experimentalFeatures.serial += 1;
	}
}

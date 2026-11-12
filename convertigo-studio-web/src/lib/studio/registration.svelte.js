import { call } from '#lib/utils/service.js';

/** the page of Convertigo Cloud where the Studio registers or logs in, as in the Eclipse Studio */
export const REGISTRATION_URL =
	'https://c8ocloud.convertigo.net/convertigo/projects/convertigo_signup/DisplayObjects/mobile/login/true/';
export const REGISTRATION_ORIGIN = new URL(REGISTRATION_URL).origin;
/** the message the registration page sends through window.studio.setPSC in the Electron Studio */
export const PSC_MESSAGE_TYPE = 'convertigo.studio.psc';

/**
 * @typedef {{
 *   desktop: boolean,
 *   registered: boolean,
 *   anonymous?: boolean,
 *   owner?: string,
 *   servers?: string[],
 *   deployServers?: { index: number, server: string, https: boolean, user: string }[],
 *   error?: string,
 *   saved?: boolean,
 *   invalid?: string
 * }} RegistrationState
 */

/**
 * The Personal Studio Configuration (PSC) of a desktop Studio, as studio.registration.Psc tells it; open shows the
 * registration dialog, serial counts the PSC saved since the Studio opened.
 */
export const registration = $state({
	state: /** @type {RegistrationState | null} */ (null),
	open: false,
	serial: 0
});

/** @param {any} result */
function isState(result) {
	return Boolean(result) && typeof result === 'object' && 'registered' in result;
}

/** @returns {Promise<RegistrationState | null>} the PSC of the workspace, or null for an engine without it */
export async function loadRegistration() {
	const result = await call('studio.registration.Psc', {}, { silentError: () => true });
	registration.state = isState(result) ? result : null;
	return registration.state;
}

/**
 * Saves a PSC as the PSC of the workspace, once the engine decoded it.
 * @param {string} psc
 * @returns {Promise<RegistrationState>} the state, with saved, or invalid and why
 */
export async function savePsc(psc) {
	const result = await call(
		'studio.registration.Psc',
		{ action: 'save', psc: String(psc ?? '').trim() },
		{ silentError: () => true }
	);
	if (!isState(result)) {
		return {
			desktop: registration.state?.desktop === true,
			registered: false,
			invalid: String(result?.error?.message ?? result?.error ?? 'The PSC cannot be saved.')
		};
	}
	registration.state = result;
	if (result.saved) {
		registration.serial += 1;
	}
	return result;
}

/**
 * @param {MessageEvent} event
 * @param {Window | null | undefined} frame the window of the registration page
 * @returns {string} the PSC the registration page gives, or ''
 */
export function pscOfMessage(event, frame) {
	if (!frame || event.source !== frame || event.origin !== REGISTRATION_ORIGIN) {
		return '';
	}
	const data = event.data;
	return data && data.type === PSC_MESSAGE_TYPE && typeof data.psc === 'string'
		? data.psc.trim()
		: '';
}

/** @param {boolean} dark */
export function registrationUrl(dark) {
	return `${REGISTRATION_URL}${dark ? 'true' : 'false'}`;
}

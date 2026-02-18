import { getContext } from 'svelte';

export function getFullSyncConfirmModal() {
	try {
		return getContext('modalYesNo');
	} catch {
		return undefined;
	}
}

/**
 * @param {any} modalYesNo
 * @param {unknown} event
 * @param {string} title
 * @param {string} message
 */
export async function openFullSyncConfirmation(modalYesNo, event, title, message) {
	if (modalYesNo?.open) {
		return await modalYesNo.open({ event, title, message });
	}
	return window.confirm(`${title}\n\n${message}`);
}

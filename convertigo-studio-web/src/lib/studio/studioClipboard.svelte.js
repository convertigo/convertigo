import { copyDbo, cutDbo, pasteDbo } from '$lib/utils/service';

/**
 * The objects copied or cut in the Studio tree, as the XML the engine gives for them: a copy can be
 * pasted several times, a cut is pasted once, where it moves the objects.
 *
 * @typedef {{ kind: '' | 'copy' | 'cut', xml: string, ids: string[] }} StudioClipboardContent
 */

/** @type {StudioClipboardContent} */
export const studioClipboard = $state({ kind: '', xml: '', ids: [] });

/**
 * @returns {boolean}
 */
export function hasStudioClipboard() {
	return Boolean(studioClipboard.kind && studioClipboard.xml);
}

/**
 * Copies or cuts objects of the tree into the Studio clipboard.
 *
 * @param {'copy' | 'cut'} kind
 * @param {string[]} ids
 * @returns {Promise<boolean>} whether the clipboard holds these objects
 */
export async function putInStudioClipboard(kind, ids) {
	const objectIds = ids.filter(Boolean);
	if (!objectIds.length) {
		return false;
	}
	const request = JSON.stringify(objectIds);
	const result = kind === 'cut' ? await cutDbo(request) : await copyDbo(request);
	if (!result?.done || !result?.xml) {
		return false;
	}
	studioClipboard.kind = kind;
	studioClipboard.xml = String(result.xml);
	studioClipboard.ids = objectIds;
	return true;
}

/**
 * Pastes the Studio clipboard into an object of the tree. A cut is pasted only once.
 *
 * @param {string} target the id of the object that receives the pasted objects
 * @returns {Promise<{ done: boolean, ids: string[], kind: string, sourceIds: string[], error?: string }>}
 */
export async function pasteStudioClipboard(target) {
	const { kind, xml, ids } = studioClipboard;
	if (!target || !kind || !xml) {
		return { done: false, ids: [], kind, sourceIds: [] };
	}
	const result = await pasteDbo(target, xml);
	const pastedIds = Array.isArray(result?.ids) ? result.ids.map(String) : [];
	if (kind === 'cut' && pastedIds.length) {
		clearStudioClipboard();
	}
	return {
		done: Boolean(result?.done),
		ids: pastedIds,
		kind,
		sourceIds: [...ids],
		error: result?.error ? String(result.error) : undefined
	};
}

export function clearStudioClipboard() {
	studioClipboard.kind = '';
	studioClipboard.xml = '';
	studioClipboard.ids = [];
}

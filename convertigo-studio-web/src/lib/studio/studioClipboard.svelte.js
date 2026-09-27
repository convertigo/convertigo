import { copyDbo, cutDbo, pasteDbo } from '$lib/utils/service';

/**
 * The objects copied or cut in the Studio tree, as the XML the engine gives for them: a copy can be
 * pasted several times, a cut is pasted once, where it moves the objects. A copy also goes to the system
 * clipboard as text, as in the Eclipse Studio, so that another Studio can paste it.
 *
 * @typedef {{ kind: '' | 'copy' | 'cut', xml: string, ids: string[], text?: string }} StudioClipboardContent
 */

/** @type {StudioClipboardContent} */
export const studioClipboard = $state({ kind: '', xml: '', ids: [], text: '' });

/**
 * @returns {boolean}
 */
export function hasStudioClipboard() {
	return Boolean(studioClipboard.kind && studioClipboard.xml);
}

/**
 * @returns {boolean} whether objects copied by another Studio can be read from the system clipboard
 */
export function canReadSystemClipboard() {
	return Boolean(globalThis.navigator?.clipboard?.readText);
}

/**
 * @param {string} text
 * @returns {boolean} whether the text holds objects copied by a Studio, the web one or the Eclipse one
 */
export function isStudioClipboardText(text) {
	const value = String(text ?? '').trim();
	if (value.startsWith('{')) {
		try {
			return JSON.parse(value)?.protocol === 'convertigo.flow.virtual.clipboard.v1';
		} catch {
			return false;
		}
	}
	return /^(<\?xml[^>]*\?>\s*)?(<convertigo\s[^>]*\bclipboard="copy"|<convertigo-clipboard[\s>])/.test(
		value
	);
}

/**
 * Writes text to the system clipboard, while the copy that gives it is running: the browser lets a page
 * write there only during the gesture of the user.
 *
 * @param {Promise<string>} text
 * @returns {Promise<boolean>}
 */
async function writeSystemClipboard(text) {
	const clipboard = globalThis.navigator?.clipboard;
	if (!clipboard) {
		return false;
	}
	try {
		if (typeof ClipboardItem !== 'undefined' && clipboard.write) {
			const blob = text.then((value) => {
				if (!value) {
					throw new Error('nothing copied');
				}
				return new Blob([value], { type: 'text/plain' });
			});
			await clipboard.write([new ClipboardItem({ 'text/plain': blob })]);
			return true;
		}
	} catch {
		// the browser may not take a pending text, it takes it once ready below
	}
	try {
		const value = await text;
		if (value && clipboard.writeText) {
			await clipboard.writeText(value);
			return true;
		}
	} catch {
		// the Studio clipboard still holds the copy
	}
	return false;
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
	const pending = kind === 'cut' ? cutDbo(request) : copyDbo(request);
	if (kind === 'copy') {
		void writeSystemClipboard(
			pending.then((result) => (result?.done ? String(result.text || result.xml || '') : ''))
		);
	}
	const result = await pending;
	if (!result?.done || !result?.xml) {
		return false;
	}
	studioClipboard.kind = kind;
	studioClipboard.xml = String(result.xml);
	studioClipboard.ids = objectIds;
	studioClipboard.text = kind === 'copy' ? String(result.text || result.xml) : '';
	return true;
}

/**
 * The objects to paste: the cut ones, which only this Studio can move, else the objects copied in the
 * system clipboard, by this Studio or another one, as the Eclipse Studio pastes, else the copied ones.
 *
 * @param {string} [text] the text of the system clipboard, when a paste event gives it
 * @returns {Promise<StudioClipboardContent>}
 */
export async function studioClipboardContent(text) {
	const { kind, xml, ids } = studioClipboard;
	if (kind !== 'cut') {
		let system = text;
		if (system === undefined && canReadSystemClipboard()) {
			try {
				system = await navigator.clipboard.readText();
			} catch {
				// the user refused to let the Studio read the clipboard
			}
		}
		if (system && isStudioClipboardText(system)) {
			return system.trim() === studioClipboard.text?.trim()
				? { kind: 'copy', xml, ids: [...ids] }
				: { kind: 'copy', xml: system.trim(), ids: [] };
		}
	}
	return { kind, xml, ids: [...ids] };
}

/**
 * Pastes the Studio clipboard into an object of the tree. A cut is pasted only once.
 *
 * @param {string} target the id of the object that receives the pasted objects
 * @param {'inside' | 'sibling' | 'auto'} [position] inside the target, next to it, or inside when it
 *  accepts the objects and next to it else, as the Eclipse Studio pastes an object on one of its type
 * @param {StudioClipboardContent} [content] the objects to paste, the Studio clipboard by default
 * @returns {Promise<{ done: boolean, ids: string[], kind: string, sourceIds: string[], target: string, error?: string }>}
 */
export async function pasteStudioClipboard(target, position = 'inside', content = studioClipboard) {
	const { kind, xml, ids } = content;
	if (!target || !kind || !xml) {
		return { done: false, ids: [], kind, sourceIds: [], target };
	}
	const result = await pasteDbo(target, xml, position);
	const pastedIds = Array.isArray(result?.ids) ? result.ids.map(String) : [];
	if (kind === 'cut' && pastedIds.length) {
		clearStudioClipboard();
	}
	return {
		done: Boolean(result?.done),
		ids: pastedIds,
		kind,
		sourceIds: [...ids],
		target: result?.target ? String(result.target) : target,
		error: result?.error ? String(result.error) : undefined
	};
}

export function clearStudioClipboard() {
	studioClipboard.kind = '';
	studioClipboard.xml = '';
	studioClipboard.ids = [];
	studioClipboard.text = '';
}

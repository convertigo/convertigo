import { call } from '$lib/utils/service';

const FOLDER_TYPE_IDS = new Set(['sq', 'cn', 'tr', 'st', 'vr', 'tc', 'ref', 'url', 'app', 'mob']);

/**
 * @typedef {Object} PaletteItem
 * @property {string=} id
 * @property {string=} name
 * @property {string=} classname
 * @property {string=} description
 * @property {string=} shortDescriptionHtml
 * @property {string=} longDescriptionText
 * @property {string=} longDescriptionHtml
 * @property {string=} shortDescriptionText
 * @property {string=} propertiesDescriptionHtml
 * @property {string=} icon
 * @property {boolean=} builtin
 * @property {boolean=} additional
 */

/**
 * @typedef {Object} PaletteCategory
 * @property {string=} name
 * @property {PaletteItem[]=} items
 */

/**
 * @param {string} id
 * @param {{ signal?: AbortSignal, timeoutMs?: number }} [options]
 * @returns {Promise<PaletteCategory[]>}
 */
async function loadPaletteCategories(id, options = {}) {
	if (!id) {
		return [];
	}
	const response = await call('studio.palette.Get', { id }, options);
	if (response?.aborted) {
		const error = new Error(
			response.timedOut ? 'Palette request timed out' : 'Palette request cancelled'
		);
		error.name = response.timedOut ? 'TimeoutError' : 'AbortError';
		throw error;
	}
	if (response?.isError || response?.error || response?.transportError || response?.offline) {
		const message = response?.error?.message ?? response?.error ?? 'Unable to load palette';
		throw new Error(String(message));
	}
	return Array.isArray(response?.categories) ? response.categories : [];
}

/**
 * @param {string} id
 * @param {(id: string, options?: { signal?: AbortSignal, timeoutMs?: number }) => Promise<PaletteCategory[]>} [loadCategories]
 * @param {{ signal?: AbortSignal, timeoutMs?: number }} [options]
 * @returns {Promise<{ id: string, fallbackFrom: string, categories: PaletteCategory[] }>}
 */
async function loadPaletteContext(id, loadCategories = loadPaletteCategories, options = {}) {
	const visited = [];
	let currentId = id;
	while (currentId && !visited.includes(currentId)) {
		visited.push(currentId);
		const categories = await loadCategories(currentId, options);
		if (hasPaletteItems(categories)) {
			return {
				id: currentId,
				fallbackFrom: currentId === id ? '' : id,
				categories
			};
		}
		currentId = parentPaletteId(currentId);
	}
	return { id, fallbackFrom: '', categories: [] };
}

/**
 * @param {PaletteCategory[]} categories
 * @returns {boolean}
 */
function hasPaletteItems(categories) {
	return categories.some((category) => (category.items ?? []).length > 0);
}

/**
 * @param {string} id
 * @returns {string}
 */
function paletteContextLabel(id) {
	const segment =
		String(id ?? '')
			.split('.')
			.at(-1) ?? '';
	const name = (segment.includes(':') ? segment.slice(segment.indexOf(':') + 1) : segment)
		.replace(/^authoring_(?:route_)?/, '')
		.replaceAll('_', ' ')
		.replace(/([a-z0-9])([A-Z])/g, '$1 $2')
		.trim();
	return name ? `${name[0].toUpperCase()}${name.slice(1)}` : 'selection';
}

/**
 * @param {string} id
 * @returns {string}
 */
function parentPaletteId(id) {
	if (!id) {
		return '';
	}
	const virtualFolder = id.match(/^(.*):([a-z]{2,4})$/);
	if (virtualFolder?.[1] && FOLDER_TYPE_IDS.has(virtualFolder[2])) {
		return virtualFolder[1];
	}
	const typedObject = id.match(/^(.*)\.([a-z]{2,4}):[^.:]+$/);
	if (typedObject?.[1] && FOLDER_TYPE_IDS.has(typedObject[2])) {
		if (typedObject[2] === 'st' && typedObject[1].includes('.st:')) {
			return typedObject[1];
		}
		return `${typedObject[1]}:${typedObject[2]}`;
	}
	const complexTypedObject = id.match(/^(.*)\.([a-z]{2,4}):(.+)$/);
	if (complexTypedObject?.[1] && FOLDER_TYPE_IDS.has(complexTypedObject[2])) {
		const parentId = complexTypedObject[1];
		const objectName = complexTypedObject[3];
		if (!objectName.includes('.')) {
			if (complexTypedObject[2] === 'st' && parentId.includes('.st:')) {
				return parentId;
			}
			return `${parentId}:${complexTypedObject[2]}`;
		}
	}
	const lastDot = id.lastIndexOf('.');
	if (lastDot > 0) {
		return id.slice(0, lastDot);
	}
	const lastColon = id.lastIndexOf(':');
	if (lastColon > 0) {
		return id.slice(0, lastColon);
	}
	return '';
}

export {
	hasPaletteItems,
	loadPaletteCategories,
	loadPaletteContext,
	paletteContextLabel,
	parentPaletteId
};

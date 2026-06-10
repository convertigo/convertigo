import { call } from '$lib/utils/service';

/**
 * @typedef {import('./types').PaletteGroup} PaletteGroup
 * @typedef {import('./types').PaletteItem} PaletteItem
 * @typedef {{ items: Map<string, PaletteItem>, groups: PaletteGroup[] }} PaletteModel
 * @typedef {{ name?: string, items?: StudioPaletteItem[] }} StudioPaletteCategory
 * @typedef {{ id?: string, name?: string, classname?: string, icon?: string }} StudioPaletteItem
 */

/** @type {Map<string, Promise<PaletteModel>>} */
const palettePromises = new Map();

const colors = ['#3b82f6', '#22c55e', '#a855f7', '#f59e0b', '#ef4444', '#06b6d4', '#eab308'];
const bottomPortContainers = new Set([
	'JsonArrayStep',
	'JsonObjectStep',
	'ElementStep',
	'XMLComplexStep',
	'XMLElementStep',
	'XMLErrorStep'
]);

/**
 * @param {string} parentId
 * @returns {Promise<PaletteModel>}
 */
function loadPaletteModel(parentId) {
	const cacheKey = parentId || '';
	if (!palettePromises.has(cacheKey)) {
		palettePromises.set(
			cacheKey,
			call('studio.palette.Get', { id: parentId }).then((response) => {
				if (response?.isError) {
					throw new Error(String(response.error ?? 'Unable to load Convertigo step palette'));
				}
				return buildStepPalette(response?.categories);
			})
		);
	}
	const promise = palettePromises.get(cacheKey);
	if (!promise) {
		throw new Error('Unable to initialize Convertigo step palette cache');
	}
	return promise;
}

/**
 * @param {string} parentId
 * @returns {Promise<Map<string, PaletteItem>>}
 */
async function loadStepPalette(parentId) {
	return (await loadPaletteModel(parentId)).items;
}

/**
 * @param {string} parentId
 * @returns {Promise<PaletteGroup[]>}
 */
async function loadStepPaletteGroups(parentId) {
	return (await loadPaletteModel(parentId)).groups;
}

/**
 * @param {unknown} categories
 * @returns {PaletteModel}
 */
function buildStepPalette(categories) {
	const colorByGroup = new Map();
	const items = new Map();
	const groups = new Map();
	let colorIndex = 0;

	for (const category of normalizeCategories(categories)) {
		const group = category.name || 'Steps';
		if (!colorByGroup.has(group)) {
			colorByGroup.set(group, colors[colorIndex % colors.length]);
			colorIndex += 1;
		}

		for (const studioItem of normalizeItems(category.items)) {
			const classname = studioItem.classname || studioItem.id || '';
			const item = toPaletteItem(
				studioItem,
				classname,
				group,
				colorByGroup.get(group) ?? colors[0]
			);
			if (!item || items.has(item.type)) {
				continue;
			}
			items.set(item.type, item);
			items.set(classname, item);
			if (!groups.has(group)) {
				groups.set(group, []);
			}
			groups.get(group)?.push(item);
		}
	}

	return {
		items,
		groups: Array.from(groups, ([name, groupItems]) => ({ name, items: groupItems }))
	};
}

/**
 * @param {unknown} categories
 * @returns {StudioPaletteCategory[]}
 */
function normalizeCategories(categories) {
	return Array.isArray(categories) ? categories : [];
}

/**
 * @param {unknown} items
 * @returns {StudioPaletteItem[]}
 */
function normalizeItems(items) {
	return Array.isArray(items) ? items : [];
}

/**
 * @param {StudioPaletteItem} studioItem
 * @param {string} classname
 * @param {string} group
 * @param {string} color
 * @returns {PaletteItem | null}
 */
function toPaletteItem(studioItem, classname, group, color) {
	if (!classname.includes('.beans.steps.')) {
		return null;
	}
	const simple = classname.split('.').pop();
	if (!simple) {
		return null;
	}
	const simpleLower = simple.toLowerCase();
	const isIteratorLike = simpleLower.includes('iterator') || simpleLower.endsWith('loopstep');
	const isWhileLike = simpleLower.includes('while');
	const isIfClass = simple.startsWith('If') || classname.includes('.steps.If');
	const hasBottomPorts = bottomPortContainers.has(simple);
	const outputs = isIteratorLike || isWhileLike || isIfClass ? 2 : 1;
	const outputLabels = isIfClass
		? ['true', 'false']
		: isIteratorLike || isWhileLike
			? ['loop', 'done']
			: void 0;

	return {
		type: simple,
		label: studioItem.name || simple,
		color,
		inputs: 1,
		outputs,
		group,
		classname,
		icon: studioItem.icon,
		outputLabels,
		bottomOutputs: hasBottomPorts ? 1 : 0,
		bottomOutputLabels: hasBottomPorts ? ['children'] : void 0,
		bottomInputs: hasBottomPorts ? 1 : 0,
		bottomInputLabels: hasBottomPorts ? ['next'] : void 0
	};
}

export { loadStepPalette, loadStepPaletteGroups };

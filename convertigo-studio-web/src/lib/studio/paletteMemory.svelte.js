/**
 * The favorite and the last used objects of the palette, as the palette of the Eclipse Studio keeps
 * them, stored in the browser of the user by the keys of their palette items.
 */

const STORAGE_KEY = 'convertigo.studio.palette.v1';
const MAX_HISTORY = 50;

/**
 * @typedef {{ favorites: string[], history: string[], builtin: boolean, shared: boolean }} PaletteMemory
 */

/** @returns {PaletteMemory} */
function defaults() {
	return { favorites: [], history: [], builtin: true, shared: true };
}

/** @returns {PaletteMemory} */
function load() {
	try {
		const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
		return {
			favorites: Array.isArray(stored?.favorites) ? stored.favorites.map(String) : [],
			history: Array.isArray(stored?.history) ? stored.history.map(String) : [],
			// the objects of Convertigo and the ones shared by the projects, as the palette of Eclipse shows them
			builtin: stored?.builtin !== false,
			shared: stored?.shared !== false
		};
	} catch {
		return defaults();
	}
}

export const paletteMemory = $state(typeof localStorage === 'undefined' ? defaults() : load());

function store() {
	try {
		localStorage.setItem(STORAGE_KEY, JSON.stringify(paletteMemory));
	} catch {
		// the palette remembers until the page reloads
	}
}

/**
 * @param {string} key the palette item added to a project
 */
export function rememberPaletteUse(key) {
	if (!key || paletteMemory.history[0] === key) {
		return;
	}
	paletteMemory.history = [key, ...paletteMemory.history.filter((used) => used !== key)].slice(
		0,
		MAX_HISTORY
	);
	store();
}

/**
 * @param {'builtin' | 'shared'} kind the objects to show or to hide
 */
export function togglePaletteVisibility(kind) {
	paletteMemory[kind] = !paletteMemory[kind];
	store();
}

/**
 * @param {string} key the palette item to add to the favorites, or to remove from them
 */
export function togglePaletteFavorite(key) {
	if (!key) {
		return;
	}
	paletteMemory.favorites = paletteMemory.favorites.includes(key)
		? paletteMemory.favorites.filter((favorite) => favorite !== key)
		: [...paletteMemory.favorites, key];
	store();
}

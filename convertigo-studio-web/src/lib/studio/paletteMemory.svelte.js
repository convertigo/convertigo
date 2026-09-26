/**
 * The favorite and the last used objects of the palette, as the palette of the Eclipse Studio keeps
 * them, stored in the browser of the user by the keys of their palette items.
 */

const STORAGE_KEY = 'convertigo.studio.palette.v1';
const MAX_HISTORY = 50;

/** @returns {{ favorites: string[], history: string[] }} */
function load() {
	try {
		const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
		return {
			favorites: Array.isArray(stored?.favorites) ? stored.favorites.map(String) : [],
			history: Array.isArray(stored?.history) ? stored.history.map(String) : []
		};
	} catch {
		return { favorites: [], history: [] };
	}
}

export const paletteMemory = $state(
	typeof localStorage === 'undefined' ? { favorites: [], history: [] } : load()
);

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

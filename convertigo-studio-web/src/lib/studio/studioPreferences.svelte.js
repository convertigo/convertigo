/**
 * The preferences of the Studio, as the Studio preference page of the Eclipse Studio keeps them, stored
 * in the browser of the user.
 */

const STORAGE_KEY = 'convertigo.studio.preferences.v1';

export const DEFAULT_MARKETPLACE_URL = 'https://marketplace.convertigo.com/';

const defaults = {
	/** a new project gets a Git repository with an initial commit */
	gitRepositoryForNewProjects: true,
	/** the save of a project writes its readme.md file again */
	readmeOnSave: false,
	/** the site of the Convertigo Marketplace */
	marketplaceUrl: DEFAULT_MARKETPLACE_URL,
	/** @type {'ask' | 'always' | 'never'} the references of the used projects a save adds */
	projectReferences: 'ask'
};

/**
 * @returns {typeof defaults}
 */
function load() {
	try {
		const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');
		return { ...defaults, ...(stored && typeof stored === 'object' ? stored : {}) };
	} catch {
		return { ...defaults };
	}
}

export const studioPreferences = $state(
	typeof localStorage === 'undefined' ? { ...defaults } : load()
);

/**
 * @param {Partial<typeof defaults>} changes
 */
export function saveStudioPreferences(changes) {
	Object.assign(studioPreferences, changes);
	try {
		localStorage.setItem(STORAGE_KEY, JSON.stringify(studioPreferences));
	} catch {
		// the preferences last until the page reloads
	}
}

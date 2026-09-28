/**
 * The types of the folders of the tree, as the last part of their ids, as "Project.cn:Http:tr": the short
 * names of the FolderType enum of the engine, and the folders of the Flow trees.
 */
export const FOLDER_TYPE_IDS = new Set([
	'ac',
	'at',
	'ah',
	'cn',
	'ct',
	'cr',
	'dc',
	'ev',
	'er',
	'fn',
	'id',
	'in',
	'ls',
	'mp',
	'mn',
	'op',
	'pg',
	'pr',
	'pf',
	'pl',
	'rf',
	'rs',
	'rt',
	'sc',
	'sq',
	'sa',
	'sp',
	'sh',
	'sr',
	'st',
	'sl',
	'tp',
	'tc',
	'tr',
	'vl',
	'vr',
	// the folders of the Flow trees
	'ref',
	'url',
	'app',
	'mob'
]);

/**
 * @param {string} id
 * @returns {boolean} whether the tree id is a folder, as `owner:type`, whereas an object is `owner.type:name`
 */
export function isFolderId(id) {
	const folder = String(id ?? '').match(/^(.*):([a-z]{2,4})$/);
	return Boolean(folder && FOLDER_TYPE_IDS.has(folder[2]) && !/\.[a-z]{2,4}$/.test(folder[1]));
}

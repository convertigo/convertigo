const FUNCTION_DECLARATION = /^(\s*function)\s*(?:[$A-Z_a-z][$\w]*\s*)?(?=\()/;
const NAMED_FUNCTION_DECLARATION = /^(\s*function)\s+[$A-Z_a-z][$\w]*\s*(?=\()/;

/**
 * Adds a stable name to a CouchDB function so JavaScript editors can parse it as a declaration.
 *
 * @param {string} source
 * @param {string} name
 */
export function nameFullSyncFunction(source, name) {
	return String(source ?? '').replace(
		FUNCTION_DECLARATION,
		(_match, functionKeyword) => `${functionKeyword} ${name}`
	);
}

/**
 * Restores the anonymous function form expected in CouchDB design documents.
 *
 * @param {string} source
 */
export function anonymizeFullSyncFunction(source) {
	return String(source ?? '').replace(
		NAMED_FUNCTION_DECLARATION,
		(_match, functionKeyword) => `${functionKeyword} `
	);
}

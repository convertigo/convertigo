const QNAME_ROUTE_SEPARATOR = '~';
const QNAME_INTERNAL_SEPARATOR = ':';

/**
 * @param {string} id
 * @returns {string}
 */
function encodeStudioSelectionId(id = '') {
	return encodeURIComponent(String(id).replaceAll(QNAME_INTERNAL_SEPARATOR, QNAME_ROUTE_SEPARATOR));
}

/**
 * @param {string} routeId
 * @returns {string}
 */
function decodeStudioSelectionId(routeId = '') {
	return String(routeId).replaceAll(QNAME_ROUTE_SEPARATOR, QNAME_INTERNAL_SEPARATOR);
}

/**
 * @param {string} base
 * @returns {string}
 */
function normalizeStudioBase(base) {
	return base.endsWith('/') ? base : `${base}/`;
}

/**
 * @param {string} base
 * @param {string} id
 * @returns {string}
 */
function studioSelectionPath(base, id = '') {
	const studioBase = normalizeStudioBase(base);
	return id ? `${studioBase}${encodeStudioSelectionId(id)}/` : studioBase;
}

/**
 * @param {string} base
 * @param {string} id
 * @param {URL} url
 * @returns {string}
 */
function studioSelectionUrl(base, id, url) {
	return `${studioSelectionPath(base, id)}${url.search}`;
}

/**
 * Read the selected object from the browser URL after a shallow Studio route
 * update. SvelteKit deliberately keeps `page.params` tied to the loaded route
 * during `replaceState`, while the address bar already contains the new qname.
 * @param {string} base
 * @param {URL} url
 * @returns {string}
 */
function studioSelectionIdFromUrl(base, url) {
	const basePath = new URL(normalizeStudioBase(base), url.origin).pathname;
	if (!url.pathname.startsWith(basePath)) {
		return '';
	}
	const routeId = url.pathname.slice(basePath.length).replace(/\/$/, '');
	try {
		return decodeStudioSelectionId(decodeURIComponent(routeId));
	} catch {
		return decodeStudioSelectionId(routeId);
	}
}

export {
	decodeStudioSelectionId,
	encodeStudioSelectionId,
	studioSelectionIdFromUrl,
	studioSelectionPath,
	studioSelectionUrl
};

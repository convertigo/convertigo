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

export {
	decodeStudioSelectionId,
	encodeStudioSelectionId,
	studioSelectionPath,
	studioSelectionUrl
};

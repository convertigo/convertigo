export const FULLSYNC_DOCS = {
	documentApi: 'https://docs.couchdb.org/en/stable/api/document/common.html',
	serverAllDbs: 'https://docs.couchdb.org/en/stable/api/server/common.html#get--_all_dbs',
	mango: 'https://docs.couchdb.org/en/stable/ddocs/mango.html'
};

/**
 * @param {string} url
 * @param {{noreferrer?: boolean}} [options]
 */
export function openFullSyncLink(url, options = {}) {
	if (!url || url == '#') return false;
	const rel = options.noreferrer ? 'noopener,noreferrer' : 'noopener';
	window.open(url, '_blank', rel);
	return true;
}

/**
 * @param {string} payload
 */
export function openFullSyncJsonPayload(payload) {
	const encoded = encodeURIComponent(payload ?? '{}');
	return openFullSyncLink(`data:application/json;charset=utf-8,${encoded}`, {
		noreferrer: true
	});
}

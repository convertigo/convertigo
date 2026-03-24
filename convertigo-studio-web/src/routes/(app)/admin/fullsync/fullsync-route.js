import { ensureTrailingSlash, resolve } from '$lib/utils/route';

export function decodeRouteParam(value = '') {
	const normalize = (input) => (input == '_' ? '' : input);
	try {
		return normalize(decodeURIComponent(String(value ?? '')));
	} catch {
		return normalize(String(value ?? ''));
	}
}

export function fullSyncHomeHref() {
	return resolve('/(app)/admin/fullsync');
}

function encodeRouteSegment(value = '') {
	return encodeURIComponent(String(value ?? ''));
}

export function fullSyncDbHref(database) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	return resolve('/(app)/admin/fullsync/[database]', { database: db });
}

export function fullSyncDbAllDocsHref(database) {
	return fullSyncDbHref(database);
}

export function fullSyncDbMangoHref(database) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	return resolve('/(app)/admin/fullsync/[database]/_find', { database: db });
}

export function fullSyncDbIndexHref(database) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	return resolve('/(app)/admin/fullsync/[database]/_index', { database: db });
}

export function fullSyncDbTabHref(database, tab = 'all') {
	if (tab == 'mango') {
		return fullSyncDbMangoHref(database);
	}
	if (tab == 'index') {
		return fullSyncDbIndexHref(database);
	}
	return fullSyncDbAllDocsHref(database);
}

export function fullSyncDbViewHref(database, designDocId, viewName) {
	const db = String(database ?? '').trim();
	const design = String(designDocId ?? '')
		.replace(/^_design\//, '')
		.trim();
	const view = String(viewName ?? '').trim();
	if (!db || !design || !view) {
		return fullSyncDbAllDocsHref(database);
	}
	return resolve('/(app)/admin/fullsync/[database]/_design/[design]/_view/[view]', {
		database: db,
		design,
		view
	});
}

export function fullSyncDbViewEditHref(database, designDocId, viewName) {
	const db = String(database ?? '').trim();
	const design = String(designDocId ?? '')
		.replace(/^_design\//, '')
		.trim();
	const view = String(viewName ?? '').trim();
	if (!db || !design || !view) {
		return fullSyncDbAllDocsHref(database);
	}
	return resolve('/(app)/admin/fullsync/[database]/_design/[design]/_view/[view]/edit', {
		database: db,
		design,
		view
	});
}

export function fullSyncDocHref(database, docId) {
	const db = String(database ?? '').trim();
	const id = String(docId ?? '').trim();
	if (!db || !id) {
		return fullSyncDbHref(database);
	}
	return ensureTrailingSlash(`${fullSyncDbHref(db)}${encodeRouteSegment(id)}`);
}

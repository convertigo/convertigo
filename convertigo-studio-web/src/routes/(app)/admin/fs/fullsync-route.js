import { resolve } from '$app/paths';

export function decodeRouteParam(value = '') {
	try {
		return decodeURIComponent(String(value ?? ''));
	} catch {
		return String(value ?? '');
	}
}

export function fullSyncHomeHref() {
	return resolve('/(app)/admin/fs');
}

function encodeRouteSegment(value = '') {
	return encodeURIComponent(String(value ?? ''));
}

export function fullSyncDbHref(database) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	return resolve('/(app)/admin/fs/[database]', { database: db });
}

export function fullSyncDbAllDocsHref(database) {
	return fullSyncDbHref(database);
}

export function fullSyncDbMangoHref(database) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	return resolve('/(app)/admin/fs/[database]/_find', { database: db });
}

export function fullSyncDbIndexHref(database) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	return resolve('/(app)/admin/fs/[database]/_index', { database: db });
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
	return resolve('/(app)/admin/fs/[database]/_design/[design]/_view/[view]', {
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
	return resolve('/(app)/admin/fs/[database]/_design/[design]/_view/[view]/_edit', {
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
	return `${fullSyncDbHref(db)}/${encodeRouteSegment(id)}`;
}

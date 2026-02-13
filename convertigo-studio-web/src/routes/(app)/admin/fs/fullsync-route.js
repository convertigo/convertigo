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

function withQuery(base, params = {}) {
	const search = new URLSearchParams();
	for (const [key, value] of Object.entries(params)) {
		if (value == null || value === '' || value === false) continue;
		search.set(key, String(value));
	}
	const query = search.toString();
	return query ? `${base}?${query}` : base;
}

function encodeRouteSegment(value = '') {
	return encodeURIComponent(String(value ?? ''));
}

export function fullSyncDbHref(database, params = {}) {
	const db = String(database ?? '').trim();
	if (!db) {
		return fullSyncHomeHref();
	}
	const base = resolve('/(app)/admin/fs/[database]', { database: db });
	return withQuery(base, params);
}

export function fullSyncDbTabHref(database, tab = 'all') {
	return fullSyncDbHref(database, tab == 'mango' ? { tab: 'mango' } : {});
}

export function fullSyncDbViewHref(database, designDocId, viewName) {
	return fullSyncDbHref(database, {
		ddoc: designDocId,
		view: viewName
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

import { browser } from '$app/environment';
import Instances from '$lib/admin/Instances.svelte';
import { getUrl } from '$lib/utils/service';

const FULLSYNC_BASE = getUrl('fullsync/');

function withQuery(path, query = {}) {
	const params = new URLSearchParams();
	for (const [key, value] of Object.entries(query)) {
		if (value == null || value === '' || value === false) {
			continue;
		}
		params.set(key, String(value));
	}
	const queryString = params.toString();
	return queryString ? `${path}?${queryString}` : path;
}

export function encodeFullSyncDocPath(docId) {
	const raw = String(docId ?? '');
	if (raw.startsWith('_design/')) {
		return `_design/${encodeURIComponent(raw.slice('_design/'.length))}`;
	}
	return raw
		.split('/')
		.map((part) => encodeURIComponent(part))
		.join('/');
}

export function encodeFullSyncDesignDocPath(designDocId) {
	const raw = String(designDocId ?? '').replace(/^_design\//, '');
	return `_design/${encodeURIComponent(raw)}`;
}

async function parseResponse(response) {
	if (response.status === 204) {
		return {};
	}
	const contentType = response.headers.get('content-type') ?? '';
	if (contentType.includes('json')) {
		try {
			return await response.json();
		} catch {
			return {};
		}
	}
	return await response.text();
}

function normalizePath(path) {
	return String(path ?? '').replace(/^\/+/, '');
}

function getErrorMessage(payload, status) {
	if (typeof payload == 'string' && payload.length) {
		return payload;
	}
	if (payload?.reason) {
		return payload.reason;
	}
	if (payload?.error) {
		return payload.error;
	}
	return `HTTP ${status}`;
}

function buildHeaders(accept = 'application/json') {
	const headers = {
		Accept: accept,
		'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
	};
	Instances.apply(headers);
	return headers;
}

async function ensureAuthenticated() {
	try {
		await fetch(getUrl('admin/services/engine.CheckAuthentication'), {
			method: 'POST',
			headers: buildHeaders(),
			credentials: 'include'
		});
	} catch {
		// Best effort: keep original request error if auth refresh fails.
	}
}

function applyResponseMetadata(response) {
	const xsrfToken = response.headers.get('x-xsrf-token');
	if (xsrfToken != null) {
		localStorage.setItem('x-xsrf-token', xsrfToken);
	}
	Instances.update(response);
}

async function request(path = '', { method = 'GET', query = null, body = undefined } = {}) {
	if (!browser) {
		return {};
	}

	const normalized = normalizePath(path);
	const url = withQuery(`${FULLSYNC_BASE}${normalized}`, query ?? {});

	const headers = buildHeaders();

	let payload = undefined;
	if (body !== undefined) {
		if (typeof body == 'string') {
			payload = body;
			headers['Content-Type'] = 'application/json';
		} else if (body instanceof FormData) {
			payload = body;
		} else {
			payload = JSON.stringify(body);
			headers['Content-Type'] = 'application/json';
		}
	}

	const response = await fetch(url, {
		method,
		headers,
		body: payload,
		credentials: 'include'
	});

	applyResponseMetadata(response);

	let resolvedResponse = response;
	if ((method == 'GET' || method == 'HEAD') && (response.status == 401 || response.status == 403)) {
		await ensureAuthenticated();
		resolvedResponse = await fetch(url, {
			method,
			headers,
			body: payload,
			credentials: 'include'
		});
		applyResponseMetadata(resolvedResponse);
	}

	const data = await parseResponse(resolvedResponse);
	if (!resolvedResponse.ok) {
		const error = new Error(getErrorMessage(data, resolvedResponse.status));
		error.status = resolvedResponse.status;
		error.payload = data;
		throw error;
	}
	return data;
}

function dbPath(dbName) {
	return encodeURIComponent(String(dbName ?? ''));
}

export function fullSyncBaseUrl() {
	return FULLSYNC_BASE;
}

export async function listDatabases() {
	const data = await request('_all_dbs');
	return Array.isArray(data) ? data : [];
}

export async function getUuids(count = 1) {
	const data = await request('_uuids', {
		query: {
			count: Math.max(1, Number(count) || 1)
		}
	});
	return Array.isArray(data?.uuids) ? data.uuids : [];
}

export async function createDatabase(dbName) {
	return request(dbPath(dbName), { method: 'PUT' });
}

export async function removeDatabase(dbName) {
	return request(dbPath(dbName), { method: 'DELETE' });
}

export async function getDatabaseInfo(dbName) {
	return request(dbPath(dbName));
}

export async function listDocuments(
	dbName,
	{
		limit = 100,
		skip = 0,
		includeDocs = true,
		descending = false,
		stable = false,
		update = 'true',
		startkey = undefined,
		endkey = undefined,
		conflicts = false,
		omitSkip = false
	} = {}
) {
	const query = {
		include_docs: includeDocs || undefined,
		limit,
		skip: omitSkip ? undefined : Math.max(0, Number(skip) || 0),
		descending: descending || undefined,
		conflicts: conflicts || undefined,
		stable: stable || undefined,
		update: update == 'true' ? undefined : update
	};
	if (startkey !== undefined) {
		query.startkey = JSON.stringify(startkey);
	}
	if (endkey !== undefined) {
		query.endkey = JSON.stringify(endkey);
	}
	return request(`${dbPath(dbName)}/_all_docs`, {
		query
	});
}

/**
 * @param {string} dbName
 * @param {string} designDocId
 * @param {string} viewName
 * @param {{
 * 	limit?: number,
 * 	skip?: number,
 * 	includeDocs?: boolean,
 * 	descending?: boolean,
 * 	stable?: boolean,
 * 	update?: string,
 * 	key?: any,
 * 	keys?: any[],
 * 	startkey?: any,
 * 	endkey?: any,
 * 	conflicts?: boolean,
 * 	reduce?: boolean,
 * 	group?: boolean,
 * 	groupLevel?: number
 * }} [options]
 */
export async function runViewQuery(
	dbName,
	designDocId,
	viewName,
	{
		limit = 100,
		skip = 0,
		includeDocs = false,
		descending = false,
		stable = false,
		update = 'true',
		key = undefined,
		keys = undefined,
		startkey = undefined,
		endkey = undefined,
		conflicts = false,
		reduce = false,
		group = undefined,
		groupLevel = undefined
	} = {}
) {
	const reduceEnabled = reduce === true || reduce === 'true';
	const includeDocsEnabled = includeDocs === true;
	const effectiveIncludeDocs = includeDocsEnabled && !reduceEnabled;
	const effectiveReduce = reduceEnabled && !includeDocsEnabled;
	const query = {
		include_docs: effectiveIncludeDocs || undefined,
		limit,
		skip: skip || 0,
		descending: descending || undefined,
		conflicts: effectiveIncludeDocs && conflicts ? true : undefined,
		stable: stable || undefined,
		update: update == 'true' ? undefined : update
	};
	if (reduce !== undefined || includeDocs !== undefined) {
		query.reduce = effectiveReduce ? 'true' : 'false';
	}
	if (effectiveReduce && group === true) {
		query.group = 'true';
	}
	if (effectiveReduce && Number.isInteger(Number(groupLevel)) && Number(groupLevel) >= 0) {
		query.group_level = String(Number(groupLevel));
	}
	if (key !== undefined) {
		query.key = JSON.stringify(key);
	}
	if (startkey !== undefined) {
		query.startkey = JSON.stringify(startkey);
	}
	if (endkey !== undefined) {
		query.endkey = JSON.stringify(endkey);
	}

	const path = `${dbPath(dbName)}/${encodeFullSyncDesignDocPath(designDocId)}/_view/${encodeURIComponent(viewName)}`;
	const usePostKeys = Array.isArray(keys) && keys.length > 0;
	return request(path, {
		method: usePostKeys ? 'POST' : 'GET',
		query,
		body: usePostKeys ? { keys } : undefined
	});
}

export async function getDocument(dbName, docId, { rev = '' } = {}) {
	return request(`${dbPath(dbName)}/${encodeFullSyncDocPath(docId)}`, {
		query: rev ? { rev } : undefined
	});
}

export async function createDocument(dbName, content) {
	return request(dbPath(dbName), { method: 'POST', body: content });
}

export async function updateDocument(dbName, docId, content) {
	return request(`${dbPath(dbName)}/${encodeFullSyncDocPath(docId)}`, {
		method: 'PUT',
		body: content
	});
}

export async function cloneDocument(dbName, sourceDocument, newId) {
	const source = sourceDocument && typeof sourceDocument == 'object' ? sourceDocument : {};
	const targetId = String(newId ?? '').trim();
	if (!targetId) {
		throw new Error('Missing target document id');
	}
	const payload = {
		...source,
		_id: targetId
	};
	delete payload._rev;
	return updateDocument(dbName, targetId, payload);
}

export async function removeDocument(dbName, docId, rev) {
	const id = String(docId ?? '').trim();
	const revision = String(rev ?? '').trim();
	if (!id || !revision) {
		throw new Error('Missing document id or revision');
	}

	const result = await removeDocuments(dbName, [{ _id: id, _rev: revision }]);
	if (Array.isArray(result)) {
		const first = result[0] ?? {};
		if (first?.ok) {
			return first;
		}
		const message = first?.reason || first?.error || 'Unable to delete document';
		const error = new Error(message);
		error.payload = first;
		throw error;
	}
	return result;
}

export async function uploadAttachment(dbName, docId, rev, file) {
	if (!browser) {
		return {};
	}
	if (!file) {
		throw new Error('Missing file to upload');
	}
	const normalized = normalizePath(
		`${dbPath(dbName)}/${encodeFullSyncDocPath(docId)}/${encodeURIComponent(file.name)}`
	);
	const url = withQuery(`${FULLSYNC_BASE}${normalized}`, { rev });
	const headers = buildHeaders();
	headers['Content-Type'] = file.type || 'application/octet-stream';

	const response = await fetch(url, {
		method: 'PUT',
		headers,
		body: file,
		credentials: 'include'
	});
	applyResponseMetadata(response);

	const data = await parseResponse(response);
	if (!response.ok) {
		const error = new Error(getErrorMessage(data, response.status));
		error.status = response.status;
		error.payload = data;
		throw error;
	}
	return data;
}

export async function removeDocuments(dbName, documents = []) {
	const docs = (Array.isArray(documents) ? documents : [])
		.map((document) => {
			const id = document?._id ?? document?.id;
			const rev = document?._rev ?? document?.rev;
			if (!id || !rev) return null;
			return {
				_id: String(id),
				_rev: String(rev),
				_deleted: true
			};
		})
		.filter(Boolean);

	if (docs.length == 0) {
		return [];
	}

	return request(`${dbPath(dbName)}/_bulk_docs`, {
		method: 'POST',
		body: { docs }
	});
}

export async function runMangoQuery(dbName, content) {
	return request(`${dbPath(dbName)}/_find`, { method: 'POST', body: content });
}

export async function explainMangoQuery(dbName, content) {
	return request(`${dbPath(dbName)}/_explain`, { method: 'POST', body: content });
}

export async function listMangoIndexes(dbName) {
	return request(`${dbPath(dbName)}/_index`);
}

export async function createMangoIndex(dbName, content) {
	return request(`${dbPath(dbName)}/_index`, { method: 'POST', body: content });
}

export async function removeMangoIndexes(dbName, docIds = []) {
	const ids = Array.from(
		new Set(
			(Array.isArray(docIds) ? docIds : [])
				.map((id) => String(id ?? '').trim())
				.filter((id) => id.length > 0 && id != '_all_docs')
		)
	);
	if (ids.length == 0) return { ok: true };
	return request(`${dbPath(dbName)}/_index/_bulk_delete`, {
		method: 'POST',
		body: { docids: ids }
	});
}

export async function listActiveTasks() {
	const data = await request('_active_tasks');
	return Array.isArray(data) ? data : [];
}

export async function listReplicationJobs({ limit = 50 } = {}) {
	return request('_scheduler/docs', {
		query: {
			limit,
			include_docs: true,
			descending: true
		}
	});
}

export async function getClusterSetupStatus() {
	return request('_cluster_setup');
}

export async function listDesignDocuments(dbName, { limit = 500, includeDocs = true } = {}) {
	const perPage = Math.max(1, Number(limit) || 500);
	const response = await listDocuments(dbName, {
		limit: perPage + 1,
		includeDocs,
		startkey: '_design/',
		endkey: '_design0',
		omitSkip: true
	});
	const rows = Array.isArray(response?.rows) ? response.rows : [];
	return rows.slice(0, perPage);
}

export async function getDesignDocument(dbName, designDocId) {
	return getDocument(dbName, designDocId);
}

export async function saveDesignDocument(dbName, designDoc) {
	const doc = designDoc && typeof designDoc == 'object' ? designDoc : {};
	if (!doc?._id || !String(doc._id).startsWith('_design/')) {
		throw new Error('Missing design document id');
	}
	return updateDocument(dbName, doc._id, doc);
}

export async function removeDesignDocument(dbName, designDocId, rev) {
	return removeDocument(dbName, designDocId, rev);
}

import { browser } from '$app/environment';
import Instances from '$lib/admin/Instances.svelte';
import { getUrl } from '$lib/utils/service';

const FULLSYNC_BASE = getUrl('fullsync/');

function withQuery(path, query = {}) {
	const params = new URLSearchParams();
	for (const [key, value] of Object.entries(query)) {
		if (value == null || value === '') {
			continue;
		}
		params.set(key, String(value));
	}
	const queryString = params.toString();
	return queryString ? `${path}?${queryString}` : path;
}

function encodeDocId(docId) {
	const raw = String(docId ?? '');
	if (raw.startsWith('_design/')) {
		return `_design/${encodeURIComponent(raw.slice('_design/'.length))}`;
	}
	return raw
		.split('/')
		.map((part) => encodeURIComponent(part))
		.join('/');
}

function encodeDesignDocPath(designDocId) {
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

	const data = await parseResponse(response);
	if (!response.ok) {
		const error = new Error(getErrorMessage(data, response.status));
		error.status = response.status;
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
		descending = true,
		stable = false,
		update = 'true',
		startkey = undefined,
		endkey = undefined
	} = {}
) {
	const query = {
		include_docs: includeDocs,
		limit,
		skip,
		descending,
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
		startkey = undefined,
		endkey = undefined
	} = {}
) {
	const query = {
		include_docs: includeDocs,
		limit,
		skip,
		descending,
		stable: stable || undefined,
		update: update == 'true' ? undefined : update
	};
	if (key !== undefined) {
		query.key = JSON.stringify(key);
	}
	if (startkey !== undefined) {
		query.startkey = JSON.stringify(startkey);
	}
	if (endkey !== undefined) {
		query.endkey = JSON.stringify(endkey);
	}

	return request(
		`${dbPath(dbName)}/${encodeDesignDocPath(designDocId)}/_view/${encodeURIComponent(viewName)}`,
		{
			query
		}
	);
}

export async function getDocument(dbName, docId, { rev = '' } = {}) {
	return request(`${dbPath(dbName)}/${encodeDocId(docId)}`, {
		query: rev ? { rev } : undefined
	});
}

export async function createDocument(dbName, content) {
	return request(dbPath(dbName), { method: 'POST', body: content });
}

export async function updateDocument(dbName, docId, content) {
	return request(`${dbPath(dbName)}/${encodeDocId(docId)}`, { method: 'PUT', body: content });
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
	return request(`${dbPath(dbName)}/${encodeDocId(docId)}`, {
		method: 'DELETE',
		query: { rev }
	});
}

export async function uploadAttachment(dbName, docId, rev, file) {
	if (!browser) {
		return {};
	}
	if (!file) {
		throw new Error('Missing file to upload');
	}
	const normalized = normalizePath(
		`${dbPath(dbName)}/${encodeDocId(docId)}/${encodeURIComponent(file.name)}`
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

export async function listDesignDocuments(dbName, { limit = 1000, includeDocs = false } = {}) {
	const response = await listDocuments(dbName, {
		limit,
		skip: 0,
		includeDocs,
		descending: false,
		startkey: '_design/',
		endkey: '_design0'
	});
	return Array.isArray(response?.rows) ? response.rows : [];
}

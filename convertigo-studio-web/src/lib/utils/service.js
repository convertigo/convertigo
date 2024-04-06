import { XMLBuilder, XMLParser } from 'fast-xml-parser';
import { loading } from '$lib/utils/loadingStore';
import { getToastStore } from '@skeletonlabs/skeleton';

let cpt = 0;
loading.subscribe((n) => (cpt = n));
/**
 * @param {string} service
 * @param {any} data
 */
export async function call(service, data = {}) {
	let url = getUrl() + service;
	let body;
	let headers = {
		'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
	};
	if (data instanceof FormData) {
		let files = new FormData();
		for (let [key, value] of data.entries()) {
			key = /** @type {string} */ (key);
			if (value instanceof File) {
				files.append(key, value);
				data.delete(key);
			}
		}
		if (!files.keys().next().done) {
			body = files;
			// @ts-ignore
			let query = new URLSearchParams(data).toString();
			if (query.length) {
				url += `${url.includes('?') ? '&' : '?'}${query}`;
			}
		}
	} else if (data?.['@_xml']) {
		body = new XMLBuilder({ ignoreAttributes: false, suppressBooleanAttributes: false }).build(
			data
		);
		headers['Content-Type'] = 'application/xml';
	}

	if (!body) {
		body = new URLSearchParams(data);
		headers['Content-Type'] = 'application/x-www-form-urlencoded';
	}

	loading.set(cpt + 1);
	let res = await fetch(url, {
		method: 'POST',
		headers,
		body,
		credentials: 'include'
	});
	loading.set(cpt - 1);
	var xsrf = res.headers.get('x-xsrf-token');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf-token', xsrf);
	}

	const contentType = res.headers.get('content-type');
	let dataContent;

	if (contentType?.includes('xml')) {
		dataContent = new XMLParser({ ignoreAttributes: false }).parse(await res.text());
	} else {
		dataContent = await res.json();
	}

	handleStateMessage(dataContent);
	return dataContent;
}

function handleStateMessage(dataContent) {
	try {
		const toastNotif = getToastStore();
		//Miss stateMessage for config
		//Missing for projects reload & delete services
		let stateMessage =
			dataContent?.admin?.response ||
			dataContent?.admin?.keys?.key ||
			dataContent?.admin ||
			dataContent?.admin?.message;
		let modalStateBody =
			stateMessage?.['@_message'] || stateMessage?.['@_errorMessage'] || stateMessage?.message;

		if (modalStateBody) {
			let background =
				stateMessage?.['@_state'] === 'error'
					? 'bg-error-400-500-token'
					: 'bg-success-400-500-token';

			toastNotif.trigger({
				message: modalStateBody,
				timeout: 8000,
				background: background
			});
		} else {
			console.warn('No valid message found in the response data.');
			return;
		}
	} catch (err) {
		console.error('Error handling state message:', err);
	}
}

export function getUrl() {
	const m = window.location.pathname.match('^(.+?)/studio/');
	return `${window.location.origin}${m ? m[1] : '/convertigo'}/admin/services/`;
}

// $lib/utils/xmlConverter.js

export function toXml(data) {
	let xml = '<?xml version="1.0" encoding="UTF-8"?>';
	xml += '<configurations>';
	xml += '</configurations>';
	return xml;
}

/* studio.dbo service methods */

/**
 * @param {string} target - the id of the parent dbo in tree
 * @param {string} position - the position relative to target inside|first|after
 * @param {any} data - the json data object
 */
export async function addDbo(
	target = '',
	position = 'inside',
	data = { kind: '', data: { id: '' } }
) {
	let result = await call('studio.dbo.Add', { target, position, data: JSON.stringify(data) });
	return result;
}

/**
 * @param {string} target - the id of the parent dbo in tree
 * @param {string} position - the position relative to target inside|first|after
 * @param {any} data - the json data object
 */
export async function moveDbo(
	target = '',
	position = 'inside',
	data = { kind: '', data: { id: '' } }
) {
	let result = await call('studio.dbo.Move', { target, position, data: JSON.stringify(data) });
	return result;
}

/**
 * @param {string} action - the drag action (move|copy)
 * @param {string} target - the id of the parent dbo in tree
 * @param {string} position - the position relative to target inside|first|after
 * @param {any} data - the json data object
 */
export async function acceptDbo(
	action = 'move',
	target = '',
	position = 'inside',
	data = { kind: '', data: { id: '' } }
) {
	let result = await call('studio.dbo.Accept', {
		action,
		target,
		position,
		data: JSON.stringify(data)
	});
	return result;
}

/**
 * @param {string} id - the id of the dbo in tree
 */
export async function removeDbo(id = '') {
	let result = await call('studio.dbo.Remove', { id });
	return result;
}

/**
 * @param {any} ids - the array of tree dbo ids
 */
export async function cutDbo(ids = []) {
	let result = await call('studio.dbo.Cut', { ids });
	return result;
}

/**
 * @param {any} ids - the array of tree dbo ids
 */
export async function copyDbo(ids = []) {
	let result = await call('studio.dbo.Copy', { ids });
	return result;
}

/**
 * @param {string} target - the id of the target dbo in tree
 * @param {string} xml - the xml string
 */
export async function pasteDbo(target = '', xml = '') {
	let result = await call('studio.dbo.Paste', { target, xml });
	return result;
}

/**
 * @param {string} id - the id of the target dbo in tree
 * @param {string} name - the dbo new name
 * @param {string} update - UPDATE_ALL | UPDATE_LOCAL | UPDATE_NONE
 */
export async function renameDbo(id = '', name = '', update = 'UPDATE_NONE') {
	let result = await call('studio.dbo.Rename', { id, name, update });
	return result;
}

/**
 * @param {any} obj
 */
export function copyObj(obj) {
	return JSON.parse(JSON.stringify(obj));
}

/**
 * @param {any} o1
 * @param {any} o2
 */
export function equalsObj(o1, o2) {
	console.log('o1', JSON.stringify(o1));
	console.log('o2', JSON.stringify(o2));
	return JSON.stringify(o1) == JSON.stringify(o2);
}

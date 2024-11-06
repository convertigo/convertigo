import { XMLBuilder, XMLParser } from 'fast-xml-parser';
import { loading } from '$lib/utils/loadingStore';

let toastNotif = null;
let modalLoading = null;
export function setToastStore(toastStore) {
	toastNotif = toastStore;
}

export function setModalStore(modalStore) {
	modalLoading = modalStore;
}

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

	handleStateMessage(dataContent, service);
	return dataContent;
}

export async function callRequestable(mode, project, data = {}) {
	let url = getUrl(`/projects/${project}/.${mode.toLowerCase()}`);
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
	}

	if (!body) {
		body = new URLSearchParams(data);
		headers['Content-Type'] = 'application/x-www-form-urlencoded';
	}

	let res = await fetch(url, {
		method: 'POST',
		headers,
		body,
		credentials: 'include'
	});

	var xsrf = res.headers.get('x-xsrf-token');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf-token', xsrf);
	}

	return res;
}

/**
 * Handles the display and hide of the loading modal based on the service call status.
 * @param {boolean} isLoading - Flag indicating if the service is loading or not.
 * @param {string} serviceName - Optional. The name of the service being called, if exclusion is needed.
 */
function handleServiceLoading(isLoading, serviceName = '') {
	if (serviceName === 'engine.JsonMonitor') {
		return;
	}
	if (isLoading) {
		loading.set(cpt + 1);
		if (modalLoading) {
			modalLoading.trigger({
				type: 'component',
				component: 'modalLoading',
				meta: { mode: 'Loading' }
			});
		}
	} else {
		loading.set(cpt - 1);
		if (modalLoading) {
			modalLoading.close();
		}
	}
}

function handleStateMessage(dataContent, service) {
	try {
		if (!toastNotif) {
			return;
		}

		let stateMessage =
			dataContent?.admin?.response ||
			dataContent?.admin?.keys?.key ||
			dataContent?.admin ||
			dataContent?.error?.message ||
			dataContent?.error;

		let toastStateBody =
			stateMessage?.['@_message'] ||
			stateMessage?.['@_errorMessage'] ||
			stateMessage?.message ||
			stateMessage?.problem ||
			stateMessage?.error ||
			(service.endsWith('.List') || service.endsWith('Authentication')
				? false
				: typeof stateMessage == 'string'
					? stateMessage
					: !toastNotif);

		if (!toastStateBody) {
			if (service == 'engine.PerformGC') {
				toastStateBody = 'GC performed successfully';
			}
		}
		if (toastStateBody) {
			let isError =
				stateMessage?.['@_state'] === 'error' ||
				!!stateMessage?.['@_errorMessage'] ||
				stateMessage?.error ||
				dataContent?.error;
			let problem = stateMessage?.problem;

			let background;
			let timeout = 2000;
			if (problem) {
				background = 'bg-tertiary-400-500-token';
				timeout = 5000;
			} else if (isError) {
				background = 'bg-error-400-500-token';
				timeout = 10000;
			} else {
				background = 'bg-success-400-500-token';
			}
			toastNotif.trigger({
				message: toastStateBody,
				timeout,
				background: background
			});
		} else if (
			[
				'engine.JsonMonitor',
				'engine.JsonStatus',
				'engine.CheckAuthentication',
				'engine.Authenticate'
			].includes(service)
		) {
			// ignore
		} else {
			console.warn(`No valid message found in the response data: ${service}`);
		}
	} catch (err) {
		console.error('Error handling state message:', err);
	}
}

export function getUrl(path = '/admin/services/') {
	const m = window.location.pathname.match('^(.+?)/studio/');
	return `${window.location.origin}${m ? m[1] : '/convertigo'}${path}`;
}

export function getQuery(query) {
	return `?${new URLSearchParams(query).toString()}`;
}

export function getFrontendUrl(projectName) {
	return getUrl(`/projects/${projectName}/DisplayObjects/mobile/index.html`);
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
	return JSON.stringify(o1) == JSON.stringify(o2);
}

export function createFormDataFromParent(parent) {
	const formData = new FormData();
	const formElements = parent.querySelectorAll('input, select, textarea');
	formElements.forEach((element) => {
		if (element.type === 'checkbox' || element.type === 'radio') {
			if (element.checked) {
				formData.append(element.name, element.value);
			}
		} else {
			formData.append(element.name, element.value);
		}
	});
	return formData;
}

/**
 * @param {any} array
 */
export function checkArray(array) {
	return Array.isArray(array) ? array : (array ?? false) ? [array] : [];
}

export function deepObject(obj) {
	for (let key in obj) {
		if (!key.startsWith('@_')) {
			try {
				obj[key] = checkArray(obj[key]).reduce((acc, obj) => {
					if (!('@_name' in obj)) {
						throw 'break';
					}
					acc[obj['@_name']] = obj;
					return acc;
				}, {});
				for (let item of Object.values(obj[key])) {
					deepObject(item);
				}
			} catch (e) {}
		}
	}
	return obj;
}

export function capitalize(str) {
	return str.charAt(0).toUpperCase() + str.slice(1);
}

export function debounce(fn, delay) {
	let timeout;
	return (...args) => {
		clearTimeout(timeout);
		timeout = setTimeout(() => {
			fn(...args);
		}, delay);
	};
}

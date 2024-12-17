import { XMLBuilder, XMLParser } from 'fast-xml-parser';
import { resolveRoute } from '$app/paths';

let currentCalls = 0;

/** @type { import('@skeletonlabs/skeleton-svelte').ToastContext } */
let toast;

export function setToastContext(toastContext) {
	toast = toastContext;
}

/**
 * @param {string} service
 * @param {any} data
 */
export async function call(service, data = {}) {
	let url = getUrl() + service;
	let body;
	let headers = {
		'x-xsrf': localStorage.getItem('x-xsrf') ?? 'Fetch'
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

	currentCalls++;
	let res = await fetch(url, {
		method: 'POST',
		headers,
		body,
		credentials: 'include'
	});
	currentCalls--;
	var xsrf = res.headers.get('x-xsrf');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf', xsrf);
	}

	const contentType = res.headers.get('content-type');
	let dataContent;

	if (contentType?.includes('xml')) {
		dataContent = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '' }).parse(
			await res.text()
		);
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
		'x-xsrf': localStorage.getItem('x-xsrf') ?? 'Fetch'
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

	var xsrf = res.headers.get('x-xsrf');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf', xsrf);
	}

	return res;
}

/**
 * Handles the display and hide of the loading modal based on the service call status.
 * @param {boolean} isLoading - Flag indicating if the service is loading or not.
 * @param {string} serviceName - Optional. The name of the service being called, if exclusion is needed.
 */
function handleServiceLoading(isLoading, serviceName = '') {
	// if (serviceName === 'engine.JsonMonitor') {
	// 	return;
	// }
	// if (isLoading) {
	// 	loading.set(cpt + 1);
	// 	if (modalLoading) {
	// 		modalLoading.trigger({
	// 			type: 'component',
	// 			component: 'modalLoading',
	// 			meta: { mode: 'Loading' }
	// 		});
	// 	}
	// } else {
	// 	loading.set(cpt - 1);
	// 	if (modalLoading) {
	// 		modalLoading.close();
	// 	}
	// }
}

function findDeepKey(obj, key, depth = 3) {
	if (depth < 0) {
		return null;
	}
	if (obj[key]) {
		return obj[key];
	}
	for (let k in obj) {
		if (typeof obj[k] == 'object') {
			let res = findDeepKey(obj[k], key, depth - 1);
			if (res) {
				return res;
			}
		}
	}
	return null;
}

function handleStateMessage(res, service) {
	try {
		if (!toast) {
			return;
		}

		let error = findDeepKey(res, 'error');
		if (error) {
			toast.create({
				description: '' + error,
				duration: 1000,
				type: 'error'
			});
			return;
		}
		return;

		// let message;
		// ['message', 'status'].find((key) => (message = findDeepKey(res, key)));
		// console.log('message', message);
		// if (message) {
		// 	toast.create({
		// 		description: '' + message,
		// 		duration: 1000,
		// 		type: 'success'
		// 	});
		// }
		// return;

		// let stateMessage =
		// 	res?.admin?.response ||
		// 	res?.admin?.keys?.key ||
		// 	res?.admin ||
		// 	res?.error?.message ||
		// 	res?.error;

		// let toastStateBody =
		// 	stateMessage?.message ||
		// 	stateMessage?.errorMessage ||
		// 	stateMessage?.message ||
		// 	stateMessage?.problem ||
		// 	stateMessage?.error ||
		// 	(service.endsWith('.List') || service.endsWith('Authentication')
		// 		? false
		// 		: typeof stateMessage == 'string'
		// 			? stateMessage
		// 			: false);

		// if (!toastStateBody) {
		// 	if (service == 'engine.PerformGC') {
		// 		toastStateBody = 'GC performed successfully';
		// 	}
		// }
		// if (toastStateBody) {
		// 	let isError =
		// 		stateMessage?.['@_state'] === 'error' ||
		// 		!!stateMessage?.['@_errorMessage'] ||
		// 		stateMessage?.error ||
		// 		res?.error;
		// 	let problem = stateMessage?.problem;

		// 	/** @type {"error" | "success" | "info" | undefined} */
		// 	let type = 'success';
		// 	let duration = 2000;
		// 	if (problem) {
		// 		type = 'info';
		// 		duration = 5000;
		// 	} else if (isError) {
		// 		type = 'error';
		// 		duration = 10000;
		// 	}
		// 	toast.create({
		// 		description: toastStateBody,
		// 		duration,
		// 		type
		// 	});
		// } else if (
		// 	[
		// 		'engine.JsonMonitor',
		// 		'engine.JsonStatus',
		// 		'engine.CheckAuthentication',
		// 		'engine.Authenticate'
		// 	].includes(service)
		// ) {
		// 	// ignore
		// } else {
		// 	console.warn(`No valid message found in the response data: ${service}`);
		// }
	} catch (err) {
		console.error('Error handling state message:', err);
	}
}

export function getUrl(path = '/admin/services/') {
	let prefix = resolveRoute('/', {});
	prefix = prefix.endsWith('/studio/') ? prefix.replace(/\/studio\/$/, '') : prefix + 'convertigo';
	return `${prefix}${path}`;
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

export function getNestedProperty(obj, path) {
	return path.split('.').reduce((acc, key) => acc?.[key], obj);
}

export function setNestedProperty(obj, path, value) {
	const keys = path.split('.');
	let current = obj;

	for (let i = 0; i < keys.length - 1; i++) {
		const key = keys[i];

		if (!current[key] || typeof current[key] !== 'object') {
			current[key] = {};
		}
		current = current[key];
	}

	current[keys[keys.length - 1]] = value;
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
		if (delay >= 0) {
			clearTimeout(timeout);
			timeout = setTimeout(() => {
				fn(...args);
			}, delay);
		} else {
			fn(...args);
		}
	};
}

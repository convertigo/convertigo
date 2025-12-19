import { createToaster } from '@skeletonlabs/skeleton-svelte';
import { browser, dev } from '$app/environment';
import { resolve } from '$app/paths';
import Instances from '$lib/admin/Instances.svelte';
import Authentication from '$lib/common/Authentication.svelte';
import { XMLBuilder, XMLParser } from 'fast-xml-parser';

export const toaster = createToaster();

let currentCalls = 0;

let modalAlert;
export function setModalAlert(alert) {
	modalAlert = alert;
}

/**
 * @param {string} service
 * @param {any} data
 */
export async function call(service, data = {}) {
	if (!browser) {
		return {};
	}
	let dataContent;
	try {
		let url = getUrl() + service;
		let body;
		let headers = {
			'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
		};
		Instances.apply(headers);
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
		var xsrf = res.headers.get('x-xsrf-token');
		if (xsrf != null) {
			localStorage.setItem('x-xsrf-token', xsrf);
		}
		Instances.update(res);

		const disposition = /filename=("|')?(.*)\1/.exec(res.headers.get('content-disposition') ?? '');
		if (disposition) {
			const link = document.createElement('a');
			link.href = URL.createObjectURL(await res.blob());
			link.download = disposition[2];
			link.click();
			URL.revokeObjectURL(link.href);
			return {};
		}

		const contentType = res.headers.get('content-type');
		if (contentType?.includes('xml')) {
			const parser = new XMLParser({
				ignoreAttributes: false,
				attributeNamePrefix: '',
				processEntities: true
			});
			parser.addEntity('#10', '\n');
			dataContent = parser.parse(await res.text());
		} else {
			dataContent = await res.json();
		}
	} catch (err) {
		dataContent = { error: err?.['message'] ?? err };
	}

	handleStateMessage(dataContent, service);
	return dataContent;
}

export async function callRequestable(mode, project, data = {}) {
	let url = getUrl(`projects/${project}/.${mode.toLowerCase()}`);
	let body;
	let headers = {
		'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
	};
	Instances.apply(headers);
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
	Instances.update(res);

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

function findDeepKeys(obj, keys, depth = 3) {
	let res = null;
	for (const key of keys) {
		res = findDeepKey(obj, key, depth);
		if (res != null) {
			break;
		}
	}
	return res;
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

function stringilight(obj) {
	return typeof obj == 'object' ? JSON.stringify(obj).replace(/(^\W+)|(\W+$)/g, '') : obj;
}

function handleStateMessage(res, service) {
	try {
		if (!toaster) {
			return;
		}

		let error = findDeepKeys(res, ['error', 'errorMessage']);
		if (!error && findDeepKeys(res, ['state']) == 'error') {
			error = findDeepKeys(res, ['message']);
		}
		if (error) {
			if (service == 'mobiles.GetBuildStatus' && res.admin?.build?.error) {
				return;
			}
			res.isError = true;

			if (!Authentication.authenticated) {
				return;
			}

			const { message, stacktrace, exception } = error;
			if (stacktrace || exception) {
				modalAlert.open({ message, exception, stacktrace });
			} else {
				error = stringilight(error);
				toaster.error({
					description: error,
					duration: 10000
				});
			}
			return;
		}

		if (
			/(CheckAuthentication|Monitor|Status|List|Options|CronCalculator|ShowProperties|\.Get.*)$/.exec(
				service
			)
		) {
			return;
		}

		let message = findDeepKeys(res, ['success', 'message', 'status']);

		if (message) {
			if (message == 'ok' && service == 'configuration.Update') {
				message = 'GC performed successfully';
			}
		}
		if (message) {
			message = stringilight(message);
			toaster.success({
				description: message,
				duration: 3000
			});
			return;
		}

		if (/(logs.Purge)$/.exec(service)) {
			return;
		}

		console.log('service without toast message', service);
		return;
	} catch (err) {
		console.error('Error handling state message:', err);
	}
}

export function getUrl(path = 'admin/services/') {
	let prefix = resolve('/');
	if (dev) {
		prefix += 'convertigo/';
	}
	return `${prefix}${path}`;
}

export function getQuery(query) {
	return `?${new URLSearchParams(query).toString()}`;
}

export function getFrontendUrl(projectName) {
	return getUrl(`projects/${projectName}/DisplayObjects/mobile/index.html`);
}

export function getThumbnailUrl(projectName) {
	return `${getUrl()}projects.Thumbnail?projectName=${projectName}`;
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

export function addInArray(array, value) {
	if (!array.includes(value)) array.push(value);
}

export function removeInArray(array, value) {
	const index = array.indexOf(value);
	if (index !== -1) array.splice(index, 1);
}

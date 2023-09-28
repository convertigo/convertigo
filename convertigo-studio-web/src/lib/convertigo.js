import { XMLParser } from 'fast-xml-parser';
import { loading } from '$lib/loadingStore';

/**
 * @param {string} service
 * @param {string | Record<string, string> | string[][] | URLSearchParams | undefined} data
 */
export async function callService(service, data = {}) {
	let url = getServiceUrl() + service;
	loading.set(true);
	let res = await fetch(url, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
		},
		body: new URLSearchParams(data),
		credentials: 'include'
	});
	loading.set(false);
	var xsrf = res.headers.get('x-xsrf-token');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf-token', xsrf);
	}

	const contentType = res.headers.get('content-type');
	if (contentType?.includes('xml')) {
		return new XMLParser({ ignoreAttributes: false }).parse(await res.text());
	} else {
		return await res.json();
	}
}

export function getServiceUrl() {
	return window.location.href.includes('/convertigo')
		? `../admin/services/`
		: `/convertigo/admin/services/`;
}

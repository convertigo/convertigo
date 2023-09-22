import { XMLParser } from 'fast-xml-parser';

/**
 * @param {string} service
 * @param {string | Record<string, string> | string[][] | URLSearchParams | undefined} data
 */
export async function callService(service, data = {}) {
	let url = getServiceUrl() + service;
	let res = await fetch(url, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded'
		},
		body: new URLSearchParams(data),
		credentials: 'include'
	});
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

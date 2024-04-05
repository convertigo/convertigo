import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export const locale = writable('');
export const timezone = writable('');
export const product = writable('');
export const beans = writable('');
export const licenceType = writable('');
export const licenceNumber = writable('');
export const licenceEnd = writable('');
export const licenceExpired = writable('');
export const javaVersion = writable('');
export const javaClassVersion = writable('');
export const javaVendor = writable('');
export const hostName = writable('');
export const hostAddresses = writable('');
export const osName = writable('');
export const osVersion = writable('');
export const osArchitecture = writable('');
export const osAvailableProcessors = writable(0);
export const browser = writable('');
export const cloud = writable(null);
export const id = writable('');

const all = {
	locale,
	timezone,
	product,
	beans,
	licenceType,
	licenceNumber,
	licenceEnd,
	licenceExpired,
	javaVersion,
	javaClassVersion,
	javaVendor,
	hostName,
	hostAddresses,
	osName,
	osVersion,
	osArchitecture,
	osAvailableProcessors,
	browser,
	cloud,
	id
};

let init = false;

export async function statusCheck() {
	if (!init) {
		init = true;
		const response = await call('engine.JsonStatus');
		if ('javaVersion' in response) {
			for (let k in all) {
				if (k in response) {
					all[k].set(response[k]);
				}
			}
		}
		console.log('status', response);
	}
}

import Bezels from '#lib/dashboard/Bezels.js';
import { studioPrompt } from './studioPrompt.svelte.js';

/**
 * The devices the user adds to the previews and the OS the NGX applications show, as the custom devices
 * and the device OS of the application editor of the Eclipse Studio, kept in the browser.
 *
 * @typedef {{ id: string, title: string, type: string, custom: true, iframe: { width: number, height: number } }} CustomDevice
 */

const CUSTOM_DEVICES = 'studio.preview.customDevices';
const DEVICE_OS = 'studio.preview.deviceOs';

/** @returns {CustomDevice[]} */
function loadCustomDevices() {
	try {
		const devices = JSON.parse(localStorage.getItem(CUSTOM_DEVICES) ?? '[]');
		return (Array.isArray(devices) ? devices : []).filter(
			(device) =>
				typeof device?.id === 'string' && device.iframe?.width > 0 && device.iframe?.height > 0
		);
	} catch {
		return [];
	}
}

/** @returns {'auto' | 'android' | 'ios'} */
function loadDeviceOs() {
	try {
		const os = localStorage.getItem(DEVICE_OS);
		return os === 'android' || os === 'ios' ? os : 'auto';
	} catch {
		return 'auto';
	}
}

export const previewDevices = $state({
	custom: typeof localStorage === 'undefined' ? [] : loadCustomDevices(),
	/** @type {'auto' | 'android' | 'ios'} */
	os: typeof localStorage === 'undefined' ? 'auto' : loadDeviceOs()
});

function storeCustomDevices() {
	try {
		localStorage.setItem(CUSTOM_DEVICES, JSON.stringify(previewDevices.custom));
	} catch {
		// no storage
	}
}

/**
 * @param {string} id
 * @returns {any} the device, a custom one included, the responsive one when unknown
 */
export function deviceById(id) {
	return Bezels[id] ?? previewDevices.custom.find((device) => device.id === id) ?? Bezels.none;
}

/**
 * @param {any} device
 * @returns {'android' | 'ios'} the OS of the device
 */
export function nativeOsOf(device) {
	return /^iP(hone|ad)/i.test(String(device?.id ?? '')) ? 'ios' : 'android';
}

/**
 * @param {any} device
 * @returns {'android' | 'ios'} the OS an application shows on the device
 */
export function deviceOsOf(device) {
	return previewDevices.os === 'auto' ? nativeOsOf(device) : previewDevices.os;
}

/**
 * @param {'auto' | 'android' | 'ios'} os
 */
export function selectDeviceOs(os) {
	previewDevices.os = os;
	try {
		localStorage.setItem(DEVICE_OS, os);
	} catch {
		// no storage
	}
}

/**
 * Asks the name and the size of a device and adds it.
 * @returns {Promise<string>} the id of the device added, none when the user cancels
 */
export async function addCustomDevice() {
	const title = (await studioPrompt('Name of the device'))?.trim();
	if (!title) {
		return '';
	}
	const size = /^\s*(\d+)\s*[x×*,]\s*(\d+)\s*$/.exec(
		(await studioPrompt('Width and height of its screen, in CSS pixels', '390x844')) ?? ''
	);
	if (!size || !Number(size[1]) || !Number(size[2])) {
		return '';
	}
	const id = `custom-${title.replace(/[^\w-]+/g, '-')}`;
	previewDevices.custom = [
		...previewDevices.custom.filter((device) => device.id !== id),
		{
			id,
			title,
			type: 'custom',
			custom: true,
			iframe: { width: Number(size[1]), height: Number(size[2]) }
		}
	];
	storeCustomDevices();
	return id;
}

/**
 * @param {string} id
 * @returns {boolean} whether the user removed the device
 */
export function removeCustomDevice(id) {
	const device = previewDevices.custom.find((custom) => custom.id === id);
	if (!device || !window.confirm(`Remove the device ${device.title}?`)) {
		return false;
	}
	previewDevices.custom = previewDevices.custom.filter((custom) => custom.id !== id);
	storeCustomDevices();
	return true;
}

/**
 * The URL of an application of the engine with the Ionic mode of its OS, which the Ionic applications
 * read at their start, as the Eclipse Studio gives them the user agent of the device. The Android mode
 * is the one of a desktop browser, which needs no parameter. The application opens from its folder, as
 * once deployed: Angular leaves index.html in the path of its router when a query follows it.
 * @param {string} url
 * @param {'android' | 'ios'} os
 * @returns {string}
 */
export function withDeviceOs(url, os) {
	if (!url || os !== 'ios' || typeof location === 'undefined') {
		return url;
	}
	try {
		const address = new URL(url, location.href);
		if (address.origin !== location.origin || !/\/projects\/[^/]+\//.test(address.pathname)) {
			return url;
		}
		address.pathname = address.pathname.replace(/\/index\.html$/, '/');
		address.searchParams.set('ionic:mode', 'ios');
		return address.href;
	} catch {
		return url;
	}
}

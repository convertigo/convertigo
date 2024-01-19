import { call } from '$lib/utils/service';
import { writable, get } from 'svelte/store';

export const delay = writable(2000);
export const maxSaved = writable(100);
export const labels = writable([]);
export const memoryMaximal = writable([]);
export const memoryTotal = writable([]);
export const memoryUsed = writable([]);
export const threads = writable([]);
export const contexts = writable([]);
export const sessions = writable([]);
export const sessionMaxCV = writable([]);
export const availableSessions = writable([]);
export const requests = writable([]);
export const engineState = writable(null);
export const startTime = writable(0);
export const time = writable(0);
export const isLoading = writable(true);

const allArrays = {
	labels,
	memoryMaximal,
	memoryTotal,
	memoryUsed,
	threads,
	contexts,
	sessions,
	sessionMaxCV,
	availableSessions,
	requests
};
const allValues = { engineState, startTime, time };

let interval = null;

export function monitorCheck() {
	const _delay = get(delay);
	if (interval == null) {
		if (_delay > 0) {
			isLoading.set(true);
			interval = window.setInterval(async () => {
				const response = await call('engine.JsonMonitor');
				isLoading.set(false);
				if ('engineState' in response) {
					const max = get(maxSaved);
					response.labels = new Date().toTimeString().split(' ')[0];
					for (let k in allArrays) {
						if (k in response) {
							allArrays[k].update((/** @type {any[]} */ v) => {
								while (v.length + 1 > max) {
									v.shift();
								}
								v.push(response[k]);
								return v;
							});
						}
					}
					for (let k in allValues) {
						if (k in response) {
							allValues[k].set(response[k]);
						}
					}
				}
				if (_delay != get(delay)) {
					window.clearInterval(interval);
					interval = null;
					monitorCheck();
				}
			}, _delay);
		}
	} else {
		if (_delay <= 0) {
			window.clearInterval(interval);
			interval = null;
		}
	}
}

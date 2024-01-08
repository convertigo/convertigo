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
export const requests = writable([]);

const all = { labels, memoryMaximal, memoryTotal, memoryUsed, threads, contexts, requests };

let interval = null;
let count = 0;

export function check() {
	const _delay = get(delay);
	if (interval == null) {
		if (_delay > 0) {
			interval = window.setInterval(async () => {
				const response = await call('engine.Monitor');
				if (response.admin) {
					const max = get(maxSaved);
					response.admin.labels = new Date().toTimeString().split(' ')[0];
					for (let k in all) {
						if (k in response.admin) {
							all[k].update((/** @type {any[]} */ v) => {
								while (v.length + 1 > max) {
									v.shift();
								}
								v.push(response.admin[k]);
								return v;
							});
						}
					}
				}
				if (_delay != get(delay)) {
					window.clearInterval(interval);
					interval = null;
					check();
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

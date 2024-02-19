import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';
let interval = null;
let secInterval = null;
export const monitorData = writable({
	delay: 2000,
	maxSaved: 100,
	labels: [],
	memoryMaximal: [],
	memoryTotal: [],
	memoryUsed: [],
	threads: [],
	contexts: [],
	sessions: [],
	sessionMaxCV: [],
	availableSessions: [],
	requests: [],
	engineState: null,
	startTime: 0,
	time: 0
});
export const isLoading = writable(true);

export function monitorCheck() {
	monitorData.subscribe(($monitorData) => {
		const _delay = $monitorData.delay;
		if (interval == null && _delay > 0) {
			interval = window.setInterval(async () => {
				window.clearInterval(secInterval);
				const response = await call('engine.JsonMonitor');

				if ('engineState' in response) {
					const max = $monitorData.maxSaved;
					response.labels = new Date().getTime();
					Object.keys($monitorData).forEach((key) => {
						if (key in response && Array.isArray($monitorData[key])) {
							while ($monitorData[key].length + 1 > max) {
								$monitorData[key].shift();
							}
							$monitorData[key].push(response[key]);
						} else if (key in response) {
							$monitorData[key] = response[key];
						}
					});
					secInterval = window.setInterval(() => {
						$monitorData.time += 1000;
						monitorData.set($monitorData);
					}, 1000);
				}
				monitorData.set($monitorData);
				isLoading.set(false);
				if (_delay != $monitorData.delay) {
					window.clearInterval(interval);
					interval = null;
					monitorCheck();
				}
			}, _delay);
		} else if (_delay <= 0) {
			window.clearInterval(interval);
			interval = null;
		}
	});
}

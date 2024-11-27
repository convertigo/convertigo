import { browser } from '$app/environment';
import Time from '$lib/common/Time.svelte';
import { call } from '$lib/utils/service';

let monitor = $state({
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

let interval = null;

function monitorCheck() {
	if (browser && interval == null && monitor.delay > 0) {
		interval = window.setInterval(async () => {
			const response = await call('engine.JsonMonitor');

			if ('engineState' in response) {
				const max = monitor.maxSaved;
				response.labels = new Date().getTime();
				Object.keys(monitor).forEach((key) => {
					if (key in response && Array.isArray(monitor[key])) {
						while (monitor[key].length + 1 > max) {
							monitor[key].shift();
						}
						monitor[key].push(response[key]);
					} else if (key in response) {
						monitor[key] = response[key];
					}
				});
				Time.serverTimestamp = monitor.time;
			}
		}, monitor.delay);
	} else if (monitor.delay <= 0) {
		window.clearInterval(interval);
		interval = null;
	}
}

export default {
	get delay() {
		monitorCheck();
		return monitor.delay;
	},
	get maxSaved() {
		monitorCheck();
		return monitor.maxSaved;
	},
	get labels() {
		monitorCheck();
		return monitor.labels;
	},
	get memoryMaximal() {
		monitorCheck();
		return monitor.memoryMaximal;
	},
	get memoryTotal() {
		monitorCheck();
		return monitor.memoryTotal;
	},
	get memoryUsed() {
		monitorCheck();
		return monitor.memoryUsed;
	},
	get threads() {
		monitorCheck();
		return monitor.threads;
	},
	get contexts() {
		monitorCheck();
		return monitor.contexts;
	},
	get sessions() {
		monitorCheck();
		return monitor.sessions;
	},
	get sessionMaxCV() {
		monitorCheck();
		return monitor.sessionMaxCV;
	},
	get availableSessions() {
		monitorCheck();
		return monitor.availableSessions;
	},
	get requests() {
		monitorCheck();
		return monitor.requests;
	},
	get engineState() {
		monitorCheck();
		return monitor.engineState;
	},
	get startTime() {
		monitorCheck();
		return monitor.startTime;
	},
	get time() {
		monitorCheck();
		return monitor.time;
	}
};

import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import Time from '$lib/common/Time.svelte';
import { call } from '$lib/utils/service';

const defValues = {
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
};
const history = { ...defValues };

let maxSaved = 100;

function resetHistory() {
	for (const key of Object.keys(history)) {
		if (Array.isArray(history[key])) {
			history[key].length = 0;
		} else {
			history[key] = defValues[key];
		}
	}
}

export default ServiceHelper({
	values: {
		get maxSaved() {
			return maxSaved;
		},
		set maxSaved(v) {
			maxSaved = v;
		}
	},
	defValues,
	delay: 2000,
	onInstanceChange: resetHistory,
	service: async () => {
		const res = await call('engine.JsonMonitor');
		if ('engineState' in res) {
			const max = maxSaved;
			res.labels = Date.now();
			Object.keys(history).forEach((key) => {
				if (key in res && Array.isArray(history[key])) {
					while (history[key].length + 1 > max) {
						history[key].shift();
					}
					history[key].push(res[key]);
					res[key] = history[key];
				}
			});
			Time.serverTimestamp = res.time;
		}
		return res;
	}
});

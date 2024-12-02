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
const values = { ...defValues };

let maxSaved = 100;

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
	service: async () => {
		const res = await call('engine.JsonMonitor');
		if ('engineState' in res) {
			const max = maxSaved;
			res.labels = new Date().getTime();
			Object.keys(values).forEach((key) => {
				if (key in res && Array.isArray(values[key])) {
					while (values[key].length + 1 > max) {
						values[key].shift();
					}
					values[key].push(res[key]);
					res[key] = values[key];
				}
			});
			Time.serverTimestamp = res.time;
		}
		return res;
	}
});

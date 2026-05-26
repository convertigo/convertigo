import { browser } from '$app/environment';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import Time from '$lib/common/Time.svelte';
import { call } from '$lib/utils/service';

const storageKey = 'admin.monitor.history';
const storageVersion = 2;
const storageMaxAge = 15 * 60 * 1000;
const metricKeys = [
	'memoryMaximal',
	'memoryTotal',
	'memoryUsed',
	'threads',
	'contexts',
	'sessions',
	'sessionMaxCV',
	'availableSessions',
	'requests'
];
const historyKeys = ['labels', ...metricKeys];

function createValues() {
	return {
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
}

const defValues = createValues();
const history = createValues();

let maxSaved = 450;
let needInit = true;

function asNumber(value, fallback = 0) {
	const number = Number(value);
	return Number.isFinite(number) ? number : fallback;
}

function asMetricNumber(key, value) {
	const number = asNumber(value);
	return key == 'availableSessions' ? Math.max(0, number) : number;
}

function resetHistory(clearStorage = true) {
	for (const key of Object.keys(defValues)) {
		if (Array.isArray(history[key])) {
			history[key].length = 0;
		} else {
			history[key] = defValues[key];
		}
	}
	needInit = true;
	if (browser && clearStorage) {
		sessionStorage.removeItem(storageKey);
	}
}

function shiftHistory() {
	for (const key of historyKeys) {
		history[key].shift();
	}
}

function clearHistoryArrays() {
	for (const key of historyKeys) {
		history[key].length = 0;
	}
}

function pruneHistory(max = maxSaved, now = Date.now()) {
	const minTime = now - storageMaxAge;
	while (
		history.labels.length &&
		(history.labels.length > max || asNumber(history.labels[0]) < minTime)
	) {
		shiftHistory();
	}
}

function addSample(sample, max = maxSaved) {
	const label = asNumber(sample.labels ?? sample.time ?? Date.now(), NaN);
	if (!Number.isFinite(label)) {
		return;
	}
	const lastLabel = history.labels[history.labels.length - 1];
	if (lastLabel === label) {
		return;
	}
	while (history.labels.length + 1 > max) {
		shiftHistory();
	}
	history.labels.push(label);
	for (const key of metricKeys) {
		history[key].push(asMetricNumber(key, sample[key]));
	}
	pruneHistory(max, label);
}

function replaceHistory(savedHistory, max = maxSaved) {
	if (!savedHistory || !Array.isArray(savedHistory.labels)) {
		return false;
	}
	const sortedSamples = [];
	for (let i = 0; i < savedHistory.labels.length; i++) {
		const sample = { labels: savedHistory.labels[i] };
		for (const key of metricKeys) {
			sample[key] = savedHistory[key]?.[i];
		}
		const label = asNumber(sample.labels, NaN);
		if (Number.isFinite(label)) {
			sortedSamples.push({ ...sample, labels: label });
		}
	}
	sortedSamples.sort((a, b) => a.labels - b.labels);
	clearHistoryArrays();
	for (const sample of sortedSamples) {
		if (history.labels[history.labels.length - 1] === sample.labels) {
			shiftHistory();
		}
		addSample(sample, max);
	}
	return history.labels.length > 0;
}

function loadHistory() {
	if (!browser) {
		return false;
	}
	try {
		const savedHistory = JSON.parse(sessionStorage.getItem(storageKey) ?? 'null');
		if (
			!savedHistory ||
			savedHistory.version !== storageVersion ||
			Date.now() - asNumber(savedHistory.savedAt) > storageMaxAge
		) {
			sessionStorage.removeItem(storageKey);
			return false;
		}
		history.startTime = asNumber(savedHistory.startTime);
		return replaceHistory(savedHistory);
	} catch (error) {
		void error;
		sessionStorage.removeItem(storageKey);
		return false;
	}
}

function saveHistory() {
	if (!browser) {
		return;
	}
	try {
		if (!history.labels.length) {
			sessionStorage.removeItem(storageKey);
			return;
		}
		const savedHistory = {
			version: storageVersion,
			savedAt: Date.now(),
			startTime: history.startTime
		};
		for (const key of historyKeys) {
			savedHistory[key] = [...history[key]];
		}
		sessionStorage.setItem(storageKey, JSON.stringify(savedHistory));
	} catch (error) {
		void error;
	}
}

needInit = !loadHistory();

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
		let requestInit = needInit;
		let res = await call('engine.JsonMonitor', requestInit ? { init: 'true' } : {});
		if ('engineState' in res) {
			const max = maxSaved;
			const startTime = asNumber(res.startTime);
			if (history.startTime && startTime && history.startTime !== startTime) {
				resetHistory();
				requestInit = true;
				res = await call('engine.JsonMonitor', { init: 'true' });
				if (!('engineState' in res)) {
					return res;
				}
			}
			history.startTime = asNumber(res.startTime);
			if (requestInit) {
				replaceHistory(res.history, max);
			}
			needInit = false;
			addSample({ ...res, labels: res.time }, max);
			for (const key of historyKeys) {
				res[key] = history[key];
			}
			saveHistory();
			Time.serverTimestamp = res.time;
		}
		return res;
	}
});

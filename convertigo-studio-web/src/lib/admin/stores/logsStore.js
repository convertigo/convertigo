import { call, checkArray } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let logs = writable([]);
export let startDate = writable('');
export let endDate = writable('');
export let realtime = writable(false);
export let nbLines = writable(100);
export let moreResults = writable(false);
export let calling = writable(false);

let _startDate, _endDate, _realtime, _nbLines;
startDate.subscribe((value) => (_startDate = value));
endDate.subscribe((value) => (_endDate = value));
realtime.subscribe((value) => (_realtime = value));
nbLines.subscribe((value) => (_nbLines = value));

let _moreResults = false;
let _calling = false;
let lastCall = 0;

export function formatDate(timestamp) {
	if (timestamp === null) return '';
	return new Date(timestamp).toISOString().split('T')[0];
}

export function formatTime(timestamp) {
	if (timestamp === null) return '';
	return new Date(timestamp).toISOString().split('T')[1].split('Z')[0].replace('.', ',');
}

export async function logsList(clear = false) {
	if (_realtime && _calling) {
		return;
	}
	if (clear) {
		logs.set([]);
		moreResults.set((_moreResults = false));
	}
	if (!_realtime && !_moreResults && !clear) {
		return;
	}
	calling.set((_calling = true));
	const currentCall = ++lastCall;
	try {
		const res = await call('logs.Get', {
			moreResults: _moreResults,
			startDate: _startDate,
			endDate: _endDate,
			realtime: _realtime,
			nbLines: _nbLines
		});
		if (currentCall == lastCall) {
			moreResults.set((_moreResults = res?.hasMoreResults ?? false));
			//@ts-ignore
			logs.update((l) => [...l, ...checkArray(res?.lines)]);
		}
	} finally {
		if (currentCall == lastCall) {
			calling.set((_calling = false));
		}
	}
}

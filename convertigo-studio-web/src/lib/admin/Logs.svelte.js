import { call, checkArray } from '$lib/utils/service';
import Instances from '$lib/admin/Instances.svelte';
import { formatDate, formatTime } from '$lib/utils/time';

let logs = $state([]);
let startDate = $state('');
let endDate = $state('');
let filter = $state('');
let live = $state(false);
let nbLines = $state(1000);
let moreResults = $state(false);
let calling = $state(false);

let lastCall = 0;
let instanceRevision = $state(Instances.revision);

$effect.root(() => {
	$effect(() => {
		const revision = Instances.revision;
		if (revision === instanceRevision) {
			return;
		}
		instanceRevision = revision;
		logs = [];
		moreResults = false;
		calling = false;
		lastCall += 1;
	});
});

async function list(clear = false) {
	if (!clear && live && calling) {
		return;
	}
	if (clear) {
		logs = [];
		moreResults = false;
	}
	if (!live && !moreResults && !clear) {
		return;
	}
	calling = true;
	const currentCall = ++lastCall;
	try {
		const res = await call('logs.Get', {
			moreResults,
			startDate,
			endDate,
			filter,
			live,
			nbLines,
			clear,
			timeout: 500
		});
		if (currentCall == lastCall) {
			moreResults = res?.hasMoreResults ?? false;
			const lines = checkArray(res?.lines);
			for (const line of lines) {
				line.push(line[4].trim().split('\n').length);
			}
			logs.push(...lines);
		}
	} catch (error) {
		console.error('Error while fetching logs:', error);
	} finally {
		if (currentCall == lastCall) {
			calling = false;
		}
	}
}

export default {
	get logs() {
		return logs;
	},
	get startDate() {
		return startDate;
	},
	set startDate(value) {
		startDate = value;
	},
	get endDate() {
		return endDate;
	},
	set endDate(value) {
		endDate = value;
	},
	get filter() {
		return filter;
	},
	set filter(value) {
		filter = value;
	},
	get live() {
		return live;
	},
	set live(value) {
		live = value;
	},
	get nbLines() {
		return nbLines;
	},
	get moreResults() {
		return moreResults;
	},
	get calling() {
		return calling;
	},
	formatDate,
	formatTime,
	list
};

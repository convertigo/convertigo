import Instances from '$lib/admin/Instances.svelte';
import { call, checkArray } from '$lib/utils/service';
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
const DEFAULT_LIVE_TIMEOUT_MS = 55000;
const QUICK_FAILURE_THRESHOLD_MS = 10000;
const PROXY_TIMEOUT_MARGIN_MS = 5000;
const MIN_ADAPTED_LIVE_TIMEOUT_MS = 10000;
const DEFAULT_RETRY_DELAY = 200;
const QUICK_FAILURE_RETRY_DELAY = 2000;
let liveTimeout = DEFAULT_LIVE_TIMEOUT_MS;

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
		return { skipped: true };
	}
	if (clear) {
		logs = [];
		moreResults = false;
	}
	if (!live && !moreResults && !clear) {
		return { skipped: true };
	}
	calling = true;
	const currentCall = ++lastCall;
	const startedAt = Date.now();
	try {
		const res = await call(
			'logs.Get',
			{
				moreResults,
				startDate,
				endDate,
				filter,
				live,
				nbLines,
				clear,
				timeout: live ? liveTimeout : 500
			},
			{
				silentStatuses: live ? [504] : undefined,
				silentNetworkAfterMs: live ? QUICK_FAILURE_THRESHOLD_MS : undefined,
				silentError: live
					? (error) => /java\.io\.IOException/i.test(error) && /stream closed/i.test(error)
					: undefined
			}
		);
		if (currentCall != lastCall || res?.aborted) {
			return { canceled: true };
		}
		if (live && (res?.silentStatus || res?.transportError || res?.offline || res?.silentError)) {
			const elapsed = res.elapsed ?? Date.now() - startedAt;
			if ((res?.silentStatus || res?.transportError) && elapsed >= QUICK_FAILURE_THRESHOLD_MS) {
				liveTimeout = Math.max(
					MIN_ADAPTED_LIVE_TIMEOUT_MS,
					Math.floor((elapsed - PROXY_TIMEOUT_MARGIN_MS) / 1000) * 1000
				);
			}
			return {
				retryDelay:
					elapsed >= QUICK_FAILURE_THRESHOLD_MS ? DEFAULT_RETRY_DELAY : QUICK_FAILURE_RETRY_DELAY
			};
		}
		if (currentCall == lastCall) {
			moreResults = res?.hasMoreResults ?? false;
			const lines = checkArray(res?.lines);
			for (const line of lines) {
				line.push(line[4].trim().split('\n').length);
			}
			logs.push(...lines);
		}
		return { retryDelay: DEFAULT_RETRY_DELAY };
	} catch (error) {
		console.error('Error while fetching logs:', error);
		return { retryDelay: QUICK_FAILURE_RETRY_DELAY };
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

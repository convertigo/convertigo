import { call, checkArray, deepObject } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let jobsStore = writable(/** @type {any[]} */ ([]));
export let schedulesStore = writable(/** @type {any[]} */ ([]));
export let scheduledStore = writable(/** @type {any[]} */ ([]));

export async function schedulerList() {
	let res = await call('scheduler.List');
	const elements = checkArray(res?.admin?.element);
	for (let el of elements) {
		deepObject(el);
	}
	jobsStore.set(elements.filter((el) => el['@_category'] == 'jobs'));
	scheduledStore.set(elements.filter((el) => el['@_category'] == 'scheduledJobs'));

	const schedules = elements.filter((el) => el['@_category'] == 'schedules');
	schedulesStore.set(schedules);
	for (let sched of schedules) {
		if (sched['@_cron']) {
			res = await call('scheduler.CronCalculator', {
				name: sched['@_name'],
				input: sched['@_info'],
				iteration: 20
			});
			sched.next = checkArray(res?.admin?.crons?.nextTime);
		} else {
			sched.next = ['n/a'];
		}
		scheduledStore.update((s) => s);
	}
}

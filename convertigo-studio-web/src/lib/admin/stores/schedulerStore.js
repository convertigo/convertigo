import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let jobsStore = writable([]);
export let schedulesStore = writable([]);
export let scheduledStore = writable([]);

export async function schedulerList() {
	const res = await call('scheduler.List');
	const elements = res?.admin?.element ?? [];
	const elementArray = Array.isArray(elements) ? elements : [elements];

	const jobs = elementArray.filter((el) => el['@_category'] === 'jobs');
	const schedules = elementArray.filter((el) => el['@_category'] === 'schedules');
	const scheduledJobs = elementArray.filter((el) => el['@_category'] === 'scheduledJobs');

	//@ts-ignore
	jobsStore.set(jobs);
	//@ts-ignore
	schedulesStore.set(schedules);
	//@ts-ignore
	scheduledStore.set(scheduledJobs);

	console.log('scheduler list res:', res);
}

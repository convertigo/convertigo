import { writable, get } from 'svelte/store';

function createCronStore() {
	const store = writable({
		minute: '0',
		hour: '0',
		dayOfMonth: '*',
		month: '*',
		dayOfWeek: '?'
	});

	return {
		subscribe: store.subscribe,
		updateCronPart: (part, value) => {
			store.update((cron) => {
				cron[part] = value;
				return cron;
			});
		},
		compileCronExpression: () => {
			const cron = get(store);
			return `${cron.minute} ${cron.hour} ${cron.dayOfMonth} ${cron.month} ${cron.dayOfWeek}`;
		}
	};
}

export const cronStore = createCronStore();

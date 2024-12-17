import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call } from '$lib/utils/service';

const defValues = {
	dates: []
};

let value = $state([-1]);

let values = {
	get value() {
		return value;
	},
	set value(v) {
		value = v;
	},
	get date() {
		return values.dates[value[0]] ?? '';
	},
	async purge() {
		await call('logs.Purge', { action: 'delete_files', date: ` ${values.date} ` });
		value[0] = -1;
		await values.refresh();
	}
};

export default ServiceHelper({
	defValues,
	values,
	arrays: ['admin.dates.date'],
	mapping: { dates: 'admin.dates.date' },
	service: 'logs.Purge',
	params: { action: 'list_files' }
});

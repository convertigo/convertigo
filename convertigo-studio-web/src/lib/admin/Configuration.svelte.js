import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call } from '$lib/utils/service';

/** @type {any} */
let defValues = {
	categories: new Array(15).fill({
		name: '',
		displayName: null,
		property: new Array(10).fill({
			type: 'Text',
			description: null,
			value: ''
		})
	})
};

let values = {
	updateConfigurations: async (property) => {
		await call('configuration.Update', { '@_xml': true, configuration: { property } });
		await values.refresh();
	}
};

export default ServiceHelper({
	values,
	defValues,
	service: 'configuration.List',
	mapping: { categories: 'admin.category' }
});

import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call, checkArray } from '$lib/utils/service';

const SYMBOL_TOKEN = /\$\{[^}]+\}/;

function normalizeProperty(property) {
	if (!SYMBOL_TOKEN.test(property?.originalValue) || !property?.value) {
		return { ...property };
	}
	return {
		...property,
		value: property?.originalValue,
		title: `Resolved value\n${property?.value}`
	};
}

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
	mapping: { categories: 'admin.category' },
	beforeUpdate: (data) => ({
		...data,
		categories: checkArray(data.categories).map((category) => ({
			...category,
			property: checkArray(category.property).map(normalizeProperty)
		}))
	})
});

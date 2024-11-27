import { browser } from '$app/environment';
import { call } from '$lib/utils/service';

/** @type {any} */
let categories = $state(
	new Array(15).fill({
		name: '',
		displayName: null,
		property: new Array(10).fill({
			type: 'Text',
			description: null,
			value: ''
		})
	})
);

async function refresh() {
	calling = true;
	try {
		const res = await call('configuration.List');
		if (res?.admin?.category?.[0]?.property) {
			categories = res?.admin?.category;
			needRefresh = false;
		}
	} catch (error) {
		needRefresh = true;
		console.error(error);
	}
	calling = false;
}

let needRefresh = true;
let calling = false;

export default {
	get categories() {
		if (browser && needRefresh && !calling) {
			refresh();
		}
		return categories;
	},
	refresh,
	updateConfigurations: async (property) => {
		await call('configuration.Update', { '@_xml': true, configuration: { property } });
		await refresh();
	}
};

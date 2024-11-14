import { browser } from '$app/environment';
import { call } from '$lib/utils/service';

/** @type {any} */
let properties = $state({
	admin: {
		category: new Array(15).fill({
			'@_name': '',
			'@_displayName': null,
			property: new Array(10).fill({
				'@_type': 'Text',
				'@_description': null,
				'@_value': ''
			})
		})
	}
});

async function refresh() {
	calling = true;
	properties = await call('configuration.List');
	calling = false;
	needRefresh = false;
}

let needRefresh = true;
let calling = false;

export default {
	get categories() {
		if (browser && needRefresh && !calling) {
			refresh();
		}
		return properties?.admin?.category;
	},
	refresh,
	updateConfigurations: async (property) => {
		await call('configuration.Update', { '@_xml': true, configuration: { property } });
		await refresh();
	}
};

import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call, checkArray } from '$lib/utils/service';

const defValues = {
	categories: Array(12).fill({
		name: ' ',
		keys: Array(2).fill({
			text: null,
			value: null,
			expiration: null
		}),
		remaining: 0,
		total: 0
	}),
	nbValidKeys: 0,
	nbInvalidKeys: 0,
	firstStartDate: null
};

let calling = $state(false);

async function doCall(service, payload) {
	calling = true;
	try {
		const res = await call(service, payload);
		await values.refresh();
		return res;
	} finally {
		calling = false;
	}
}

let values = {
	get calling() {
		return calling;
	},

	async deleteKey(keyText) {
		return await doCall('keys.Remove', {
			'@_xml': true,
			key: { '@_text': keyText.trim() }
		});
	},

	async addKey(newKey) {
		return await doCall('keys.Update', {
			'@_xml': true,
			key: { '@_text': newKey.trim() }
		});
	}
};

export default ServiceHelper({
	defValues,
	values,
	service: 'keys.List',
	arrays: ['admin.category'],
	mapping: {
		categories: 'admin.category',
		nbValidKeys: 'admin.nb_valid_key',
		firstStartDate: 'admin.firstStartDate'
	},
	beforeUpdate: (res) => {
		let invalid = 0;
		for (const category of res.categories) {
			category.keys = checkArray(category.keys?.key);
			for (const key of category.keys) {
				if (key.expired == 'true') {
					invalid++;
				}
				if (key.expiration == 0) {
					key.expiration = 'Unlimited';
				} else {
					key.expiration = `Until ${new Date(key.expiration * 1000 * 3600 * 24).toISOString().split('T')[0]}`;
				}
				key.value = `${key.value} ${category.name == 'Standard Edition' ? ' session' : 'connection'}${key.value > 1 ? 's' : ''}`;
			}
		}
		res.nbInvalidKeys = invalid;
	}
});

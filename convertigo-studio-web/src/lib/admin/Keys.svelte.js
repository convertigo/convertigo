import { call, checkArray } from '$lib/utils/service';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';


const defValues = {
	categories: [],
	nbValidKeys: 0,
	firstStartDate: null,
};

let calling = $state(false);

/**
 * @param {string} service - The service name to call.
 * @param {Object} payload - The payload data for the service call.
 */
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
		const payload = {
			'@_xml': true,
			admin: {
				'@_service': 'keys.Remove',
				keys: {
					key: { '@_text': keyText },
				},
			},
		};
		return await doCall('keys.Remove', payload);
	},

	async addKey(newKey) {
		const payload = {
			'@_xml': true,
			admin: {
				'@_service': 'keys.Update',
				keys: {
					key: { '@_text': newKey },
				},
			},
		};
		return await doCall('keys.Update', payload);
	},

	formatExpiration(expirationCode) {
		if (expirationCode === '0') return 'Unlimited';

		let year = parseInt(expirationCode.substring(0, 2), 10);
		let dayOfYear = parseInt(expirationCode.substring(2), 10);
		year += year < 70 ? 2000 : 1900;

		let date = new Date(year, 0);
		date.setDate(date.getDate() + dayOfYear - 1);

		return date.toDateString();
	},
};

export default ServiceHelper({
	defValues, 
	values, 
	service: 'keys.List', 
	arrays: ['admin.category'], 
	mapping: {
		categories: 'admin.category', 
		nbValidKeys: 'admin.nb_valid_key',
		firstStartDate: 'admin.firstStartDate',
	},
	beforeUpdate: (res) => {
		res.categories = checkArray(res.categories).map((category) => ({
			...category,
			keys: { key: checkArray(category.keys?.key) },
		}));
	},
});
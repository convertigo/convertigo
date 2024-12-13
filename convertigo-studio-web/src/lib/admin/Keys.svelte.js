import { call, checkArray } from '$lib/utils/service';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';

const defValues = {
	categories: [], 
	nbValidKeys: 0, 
	firstStartDate: null, 
};

let values = {
	async deleteKey(keyText) {
		await call('keys.Remove', {
			'@_xml': true,
			admin: {
				'@_service': 'keys.Remove',
				keys: {
					key: {
						'@_text': keyText,
					},
				},
			},
		});
		await this.refresh(); 
	},

	async addKey(newKey) {
		await call('keys.Update', {
			'@_xml': true,
			admin: {
				'@_service': 'keys.Update',
				keys: {
					key: {
						'@_text': newKey,
					},
				},
			},
		});
		await this.refresh();
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
		console.log('Raw response from keys.List:', res); 

		res.categories = checkArray(res.categories).map((category) => {
			return {
				...category,
				keys: {
					key: checkArray(category.keys?.key),
				},
			};
		});

		console.log('Processed categories:', res.categories); 
	},
});
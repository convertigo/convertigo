import { browser } from '$app/environment';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call } from '$lib/utils/service';

const defValues = {
	confDefault: {
		cacheType: 'com.twinsoft.convertigo.engine.cache.FileCacheManager',
		databaseType: 'mariadb',
		serverName: 'dbhost',
		port: 3306,
		databaseName: 'c8ocache',
		userName: 'cache_user',
		userPassword: '',
		cacheTableName: 'c8ocache'
	}
};

defValues.conf = { ...defValues.confDefault };
defValues.confOriginal = { ...defValues.confDefault };

let values = {
	get hasChanged() {
		return hasChanged;
	},
	async clear(event) {
		event?.preventDefault();
		return await call('cache.Clear');
	},
	cancel(event) {
		event?.preventDefault();
		values.conf = { ...values.confOriginal };
	},
	async configure(event) {
		event.preventDefault();
		let formData = new FormData(event.target);
		if (event.submitter.textContent == 'Create Table and Apply') {
			formData.append('create', '');
		}
		await call('cache.Configure', formData);
		await values.refresh();
	}
};

let hasChanged = $derived(
	Object.entries(values.conf ?? []).some(([k, v]) => v != values.confOriginal?.[k])
);

export default ServiceHelper({
	defValues,
	values,
	service: 'cache.ShowProperties',
	beforeUpdate: (data) => ({
		conf: { ...defValues.confDefault, ...data.admin },
		confDefault: defValues.confDefault,
		confOriginal: { ...defValues.confDefault, ...data.admin }
	})
});

// /** @type {any} */
// let conf = $state({
// 	...confDefault
// });

// /** @type {any} */
// let confOriginal = $state({});

// let hasChanges = $derived(Object.entries(conf).some(([k, v]) => v != confOriginal[k]));

// let needRefresh = $state(true);
// let calling = $state(false);

// export default {
// 	get loading() {
// 		return needRefresh || calling;
// 	},
// 	get conf() {
// 		if (browser && needRefresh && !calling) {
// 			this.refresh();
// 		}
// 		return conf;
// 	},
// 	get confOriginal() {
// 		return confOriginal;
// 	},
// 	get confDefault() {
// 		return confDefault;
// 	},
// 	get hasChanged() {
// 		return hasChanges;
// 	},
// 	async refresh() {
// 		calling = true;
// 		try {
// 			const res = await call('cache.ShowProperties');
// 			if (res?.admin?.service) {
// 				needRefresh = false;
// 				conf = {
// 					...conf,
// 					...res.admin
// 				};
// 				confOriginal = { ...conf };
// 			}
// 		} catch (error) {
// 			needRefresh = true;
// 		}
// 		calling = false;
// 	},
// 	async clear(event) {
// 		event?.preventDefault();
// 		return await call('cache.Clear');
// 	},
// 	cancel(event) {
// 		event?.preventDefault();
// 		conf = { ...confOriginal };
// 	}
// };

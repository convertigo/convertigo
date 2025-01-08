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
	async clear() {
		return await call('cache.Clear');
	},
	cancel() {
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

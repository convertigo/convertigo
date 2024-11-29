import { browser } from '$app/environment';
import { call } from '$lib/utils/service';

const confDefault = {
	cacheType: 'com.twinsoft.convertigo.engine.cache.FileCacheManager',
	databaseType: 'mariadb',
	serverName: 'dbhost',
	port: 3306,
	databaseName: 'c8ocache',
	userName: 'cache_user',
	userPassword: '',
	cacheTableName: 'c8ocache'
};

/** @type {any} */
let conf = $state({
	...confDefault
});

/** @type {any} */
let confOriginal = $state({});

let hasChanges = $derived(Object.entries(conf).some(([k, v]) => v != confOriginal[k]));

let needRefresh = $state(true);
let calling = $state(false);

export default {
	get loading() {
		return needRefresh || calling;
	},
	get conf() {
		if (browser && needRefresh && !calling) {
			this.refresh();
		}
		return conf;
	},
	get confOriginal() {
		return confOriginal;
	},
	get confDefault() {
		return confDefault;
	},
	get hasChanged() {
		return hasChanges;
	},
	async refresh() {
		calling = true;
		try {
			const res = await call('cache.ShowProperties');
			if (res?.admin?.service) {
				needRefresh = false;
				conf = {
					...conf,
					...res.admin
				};
				confOriginal = { ...conf };
			}
		} catch (error) {
			needRefresh = true;
		}
		calling = false;
	},
	async clear(event) {
		event?.preventDefault();
		return await call('cache.Clear');
	},
	cancel(event) {
		event?.preventDefault();
		conf = { ...confOriginal };
	}
};

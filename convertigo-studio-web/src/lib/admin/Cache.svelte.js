import { browser } from '$app/environment';
import { call } from '$lib/utils/service';

/** @type {any} */
let conf = $state({
	cacheType: 'com.twinsoft.convertigo.engine.cache.FileCacheManager',
	databaseType: 'mariadb',
	serverName: 'dbhost',
	port: 3306,
	databaseName: 'c8ocache',
	userName: 'cache_user',
	userPassword: '',
	cacheTableName: 'c8ocache'
});
let oriConf = $state({});

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
				oriConf = { ...conf };
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
		conf = { ...oriConf };
	}
};

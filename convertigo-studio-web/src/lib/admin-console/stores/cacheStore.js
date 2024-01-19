import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export const cacheProperties = writable({});
export const cacheConfiguration = writable({});
export const cacheClearMessage = writable('');

export const cacheType = writable('file');

export async function showCacheProperties() {
	const response = await call('cache.ShowProperties');
	if (response.admin.cacheType === 'com.twinsoft.convertigo.engine.cache.FileCacheManager') {
		cacheType.set('file');
	} else {
		cacheType.set('database');
	}
	console.log('response for cache properties:', response);
}

export async function cacheConfig() {
	const response = await call('cache.Configure');

	console.log('response for cache config:', response);
}

export async function cacheClear() {
	const response = await call('cache.Clear');

	console.log('response for cache Clear:', response);
}

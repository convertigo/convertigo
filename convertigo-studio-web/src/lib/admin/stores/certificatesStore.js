import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let candidates = writable([]);
export let certificates = writable([]);
export let anonymousBinding = writable([]);
export let cariocaBinding = writable([]);

function prepareAndSetData(data, store) {
	let dataArray = Array.isArray(data) ? data : [data];
	dataArray.push('new');
	store.set(dataArray);
}

export async function certificatesList() {
	const res = await call('certificates.List');
	console.log('certif Store', res);

	prepareAndSetData(res?.admin?.candidates?.candidate ?? [], candidates);
	prepareAndSetData(res?.admin?.certificates?.certificate ?? [], certificates);
	prepareAndSetData(res?.admin?.bindings?.anonymous?.binding ?? [], anonymousBinding);
	prepareAndSetData(res?.admin?.bindings?.carioca?.binding ?? [], cariocaBinding);
}

import { call, checkArray } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let candidates = writable(/** @type {any[]} */ ([]));
export let certificates = writable(/** @type {any[]} */ ([]));
export let anonymousBinding = writable(/** @type {any[]} */ ([]));
export let cariocaBinding = writable(/** @type {any[]} */ ([]));

export async function certificatesList() {
	const res = await call('certificates.List');
	candidates.set(checkArray(res?.admin?.candidates?.candidate));
	certificates.set(checkArray(res?.admin?.certificates?.certificate));
	anonymousBinding.set(checkArray(res?.admin?.bindings?.anonymous?.binding));
	cariocaBinding.set(checkArray(res?.admin?.bindings?.carioca?.binding));
}

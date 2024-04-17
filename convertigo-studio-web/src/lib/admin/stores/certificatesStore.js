import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let candidates = writable([]);
export let certificates = writable([]);
export let anonymousBinding = writable([]);
export let cariocaBinding = writable([]);

let init = false;

export async function certificatesList() {
	const res = await call('certificates.List');
	console.log('certif Store', res);
	let candidateArray = res?.admin?.candidates?.candidate ?? [];
	if (!Array.isArray(candidateArray)) {
		candidateArray = [candidateArray];
	}
	candidates.set(candidateArray);

	let certificateArray = res?.admin?.certificates?.certificate ?? [];
	if (!Array.isArray(certificateArray)) {
		certificateArray = [certificateArray];
	}
	certificateArray.push('new');
	certificates.set(certificateArray);

	let anonymousArray = res?.admin?.bindings?.anonymous?.binding ?? [];
	if (!Array.isArray(anonymousArray)) {
		anonymousArray = [anonymousArray];
	}
	anonymousArray.push('new');
	anonymousBinding.set(anonymousArray);

	let cariocaArray = res?.admin?.bindings?.carioca?.binding ?? [];
	if (!Array.isArray(cariocaArray)) {
		cariocaArray = [cariocaArray];
	}
	cariocaArray.push('new');
	cariocaBinding.set(cariocaArray);
}

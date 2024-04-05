import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let candidates = writable([]);
export let certificates = writable([]);

export async function candidatesList() {
	const res = await call('certificates.List');

	let candidateArray = res?.admin?.candidates?.candidate ?? [];
	if (!Array.isArray(candidateArray)) {
		candidateArray = [candidateArray];
	}

	candidates.set(candidateArray);
	console.log('candidatesList res', res);
}

export async function certificatesList() {
	const res = await call('certificates.List');

	let certificateArray = res?.admin?.certificates?.certificate ?? [];
	if (!Array.isArray(certificateArray)) {
		certificateArray = [certificateArray];
	}

	certificates.set(certificateArray);
	console.log('certificatesList res', res);
}

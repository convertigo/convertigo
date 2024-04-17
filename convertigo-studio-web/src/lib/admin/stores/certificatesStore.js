import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let candidates = writable([]);
export let certificates = writable([]);

let init = false;

export async function certificatesList() {
	if (!init) {
		init = true;

		const res = await call('certificates.List');

		let candidateArray = res?.admin?.candidates?.candidate ?? [];
		if (!Array.isArray(candidateArray)) {
			candidateArray = [candidateArray];
		}

		candidates.set(candidateArray);
		console.log('candidatesList res', res);

		let certificateArray = res?.admin?.certificates?.certificate ?? [];
		if (!Array.isArray(certificateArray)) {
			certificateArray = [certificateArray];
		}
		certificateArray.push('new');
		certificates.set(certificateArray);
		console.log('certificatesList res', res);
	}

}

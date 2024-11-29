import { browser } from '$app/environment';
import { call, checkArray } from '$lib/utils/service';

/** @type {any[]} */
let certificates = $state([]);
/** @type {any[]} */
let candidates = $state([]);
/** @type {any[]} */
let bindings = $state([]);

let needRefresh = $state(true);
let calling = $state(false);

async function refresh() {
	calling = true;
	try {
		const res = await call('certificates.List');
		if (res?.admin?.certificates) {
			needRefresh = false;
			certificates = checkArray(res?.admin?.certificates?.certificate);
			candidates = checkArray(res?.admin?.candidates?.candidate);
			bindings = checkArray(res?.admin?.bindings?.binding);
		}
	} catch (error) {
		needRefresh = true;
	}
	calling = false;
}

if (browser) {
	refresh();
}

export default {
	get loading() {
		return needRefresh || calling;
	},
	get certificates() {
		return certificates;
	},
	get candidates() {
		return candidates;
	},
	get bindings() {
		return bindings;
	},
	refresh
};

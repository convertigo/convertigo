import { call } from '$lib/utils/service';

let variables = $state(Array(10).fill({ name: null, value: null }));
let init = false;

async function check() {
	if (!init) {
		init = true;
		try {
			const res = await call('engine.GetEnvironmentVariablesJson');
			variables = res?.variables ?? [];
		} catch (e) {
			init = false;
		}
	}
}

export default {
	get variables() {
		check();
		return variables;
	}
};

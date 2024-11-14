import { call, checkArray } from '$lib/utils/service';
import { decode } from 'html-entities';

/** @type {any[]} */
let projects = $state(new Array(10).fill({}));

async function refresh() {
	calling = true;
	const res = await call('projects.List');
	const _projects = checkArray(res?.admin?.projects?.project);
	for (const project of _projects) {
		project['@_comment'] = decode(project['@_comment']);
		project.ref = checkArray(project.ref);
	}
	projects = _projects;
	calling = false;
	needRefresh = false;
}

let needRefresh = true;
let calling = false;

export default {
	get projects() {
		// if (browser && needRefresh && !calling) {
		// 	refresh();
		// }
		return projects;
	},
	refresh,
	updateConfigurations: async (property) => {
		await call('configuration.Update', { '@_xml': true, configuration: { property } });
		await refresh();
	}
};

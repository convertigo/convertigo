import { browser } from '$app/environment';
import { call, checkArray } from '$lib/utils/service';
import { decode } from 'html-entities';

let projects = $state(
	new Array(15).fill({
		'@_name': null,
		'@_displayName': null,
		property: new Array(10).fill({
			'@_type': 'Text',
			'@_description': null,
			'@_value': ''
		})
	})
);

async function refresh() {
	calling = true;
	try {
		let res = await call('projects.List');
		if (res?.admin?.projects) {
			const prjs = checkArray(res?.admin?.projects?.project);
			for (const project of prjs) {
				project['@_comment'] = decode(project['@_comment']);
				project.ref = checkArray(project.ref);
			}
			projects = prjs;
			needRefresh = false;
		}
	} catch (error) {
		needRefresh = true;
		console.error(error);
	}
	calling = false;
}

let needRefresh = true;
let calling = false;

export default {
	get projects() {
		if (browser && needRefresh && !calling) {
			refresh();
		}
		return projects;
	},
	refresh
};

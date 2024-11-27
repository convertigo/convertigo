import { browser } from '$app/environment';
import { call, checkArray } from '$lib/utils/service';
import { decode } from 'html-entities';

let projects = $state(
	new Array(15).fill({
		name: null,
		displayName: null,
		property: new Array(10).fill({
			type: 'Text',
			description: null,
			value: ''
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
				project.comment = decode(project.comment);
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

async function remove(projectName) {
	await call('projects.Delete', { projectName });
	await refresh();
}

async function reload(projectName) {
	await call('projects.Reload', { projectName });
	await refresh();
}

async function exportOptions(projectName) {
	const res = await call('projects.ExportOptions', { projectName });
	return checkArray(res?.admin?.options?.option);
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
	refresh,
	remove,
	reload,
	exportOptions
};

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

let needRefresh = true;
let calling = false;

export default {
	get projects() {
		if (browser && needRefresh && !calling) {
			this.refresh();
		}
		return projects;
	},
	async refresh() {
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
	},
	async remove(projectName) {
		await call('projects.Delete', { projectName });
		await this.refresh();
	},
	async reload(projectName) {
		await call('projects.Reload', { projectName });
		await this.refresh();
	},
	async exportOptions(projectName) {
		const res = await call('projects.ExportOptions', { projectName });
		return checkArray(res?.admin?.options?.option);
	},
	async undefinedSymbols(projectName) {
		const res = await call('projects.GetUndefinedSymbols', { projectName });
		return checkArray(res?.admin?.undefined_symbols?.symbol);
	},
	async createSymbols(projectName) {
		return await call('global_symbols.Create', { projectName });
	}
};

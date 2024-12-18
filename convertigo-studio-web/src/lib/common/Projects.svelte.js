import { call, checkArray } from '$lib/utils/service';
import { decode } from 'html-entities';
import ServiceHelper from './ServiceHelper.svelte';

const defValues = {
	projects: new Array(10).fill({
		name: null,
		comment: null,
		exported: null,
		exportedTs: null,
		deployDate: null,
		deployDateTs: null,
		version: null,
		hasFrontend: null,
		hasPlatform: null,
		ref: []
	})
};

let waiting = $state(false);

async function doCall(action, param) {
	try {
		waiting = true;
		param?.preventDefault?.();
		await call(`projects.${action}`, param?.target ? new FormData(param?.target) : param);
		await values.refresh();
	} finally {
		waiting = false;
	}
}

let values = {
	async deploy(event) {
		await doCall('Deploy', event);
	},
	async importURL(event) {
		await doCall('ImportURL', event);
	},
	async remove(projectName) {
		await doCall('Delete', { projectName });
	},
	async reload(projectName) {
		await doCall('Reload', { projectName });
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
	},
	get waiting() {
		return waiting;
	}
};

export default ServiceHelper({
	defValues,
	values,
	service: 'projects.List',
	arrays: ['admin.projects.project'],
	mapping: { projects: 'admin.projects.project' },
	beforeUpdate: (res) => {
		for (const project of res.projects) {
			project.comment = decode(project.comment);
			project.ref = checkArray(project.ref);
		}
	}
});

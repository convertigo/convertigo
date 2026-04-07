import Authentication from '$lib/common/Authentication.svelte';
import { call, checkArray } from '$lib/utils/service';
import { decode } from 'html-entities';
import ServiceHelper from './ServiceHelper.svelte';

const defValues = {
	accessDenied: false,
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
		const res = await call(
			`projects.${action}`,
			param?.target ? new FormData(param?.target) : param
		);
		if (!res?.isError) {
			await values.refresh();
			return true;
		}
		return false;
	} finally {
		waiting = false;
	}
}

let values = {
	async deploy(event) {
		await doCall('Deploy', event);
	},
	async exportProject(event) {
		await doCall('Export', event);
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
	needAuth: false,
	service: async () => {
		const res = await call('projects.List');
		if (
			res?.isError &&
			Authentication.authenticated &&
			!Authentication.canAccessDashboard &&
			/authentication failure/i.test(
				String(res?.error?.message ?? res?.message ?? res?.error ?? '')
			)
		) {
			return {
				accessDenied: true,
				admin: { projects: { project: [] } }
			};
		}
		return res;
	},
	arrays: ['admin.projects.project'],
	mapping: { projects: 'admin.projects.project', accessDenied: 'accessDenied' },
	beforeUpdate: (res) => {
		for (const project of res.projects) {
			project.comment = decode(project.comment);
			project.ref = checkArray(project.ref);
		}
	}
});

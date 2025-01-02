import { call, getQuery, getUrl } from '$lib/utils/service';
import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { browser } from '$app/environment';

const defValues = {
	symbols: new Array(5).fill({
		name: null,
		value: null
	})
	// defaults: new Array(10).fill({
	// 	project: null,
	// 	name: null,
	// 	value: null
	// })
};

let waiting = $state(false);

async function doCall(action, param) {
	waiting = true;
	try {
		param?.preventDefault?.();
		const res = await call(
			`global_symbols.${action}`,
			param?.target ? new FormData(param?.target) : param
		);
		if (!res.isError) {
			values.refresh();
		}
		return res;
	} finally {
		waiting = false;
	}
}

let values = {
	get waiting() {
		return waiting;
	},

	get exportURL() {
		return browser
			? `${getUrl()}roles.Export${getQuery({
					__xsrfToken: localStorage.getItem('x-xsrf') ?? '',
					users: JSON.stringify(
						values.users.filter((user) => user.export).map((user) => ({ name: user.name }))
					).replace(/(^\[)|(\]$)/g, '')
				})}`
			: '';
	},

	async addUser(event, row) {
		const res = await doCall(row ? 'Edit' : 'Add', event);
		return !res.isError;
	},

	async deleteSymbol(symbolName) {
		return await doCall('Delete', { symbolName });
	},

	async importRoles(event) {
		const res = await doCall('Import', event);
		return !res.isError;
	},

	formatRoleName(roleName) {
		return roleName
			.toLowerCase()
			.replace(/_/g, ' ')
			.replace(/(?:^|\s)\S/g, (match) => match.toUpperCase());
	}
};

export default ServiceHelper({
	defValues,
	values,
	service: 'global_symbols.List',
	arrays: ['admin.symbols.symbol', 'admin.defaultSymbols.defaultSymbol'],
	mapping: {
		symbols: 'admin.symbols.symbol',
		defaults: 'admin.defaultSymbols.defaultSymbol'
	},
	beforeUpdate: (res) => {
		for (const d of res.defaults) {
			res.symbols.push(d);
		}
		res.symbols.sort((a, b) => a.name.localeCompare(b.name));
		for (const s of res.symbols) {
			s.export = false;
		}
		delete res.defaults;
		// defaults.sort((a, b) => a.name.localeCompare(b.name));
	}
});

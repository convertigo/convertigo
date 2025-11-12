import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call } from '$lib/utils/service';

const defValues = {
	symbols: new Array(5)
		.fill({
			name: null,
			value: null,
			index: 0
		})
		.map((s, i) => ({ ...s, index: i }))
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

	async addSymbol(event, row) {
		const edit = row && !row.project;
		const res = await doCall(edit ? 'Edit' : 'Add', event);
		return !res.isError;
	},

	async deleteSymbol(symbolName) {
		return await doCall('Delete', { symbolName });
	},

	async deleteAllSymbols() {
		return await doCall('DeleteAll');
	},

	async importSymbols(event) {
		const res = await doCall('Import', event);
		return !res.isError;
	},

	async exportSymbols() {
		const data = values.symbols.filter((s) => s.export).map(({ name }) => ({ name }));
		const res = await doCall('Export', { symbols: JSON.stringify(data) });
		return !res.isError;
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
		res.symbols.forEach((s, i) => {
			s.index = i;
			s.export = false;
		});
		delete res.defaults;
	}
});

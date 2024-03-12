import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let environmentVariables = writable([]);
export let globalSymbolsList = writable([]);
export let defaultSymbolList = writable([]);

export async function getEnvironmentVar() {
	const res = await call('engine.GetEnvironmentVariablesJson');
	if (res?.variables) {
		environmentVariables.set(res.variables);
		return res.variables;
	}
}

export async function globalSymbols() {
	const res = await call('global_symbols.List');
	console.log('global symbols list:', res);

	let symbolList = res.admin.symbols.symbol;
	let defaultSymbol = res.admin.defaultSymbols.defaultSymbol;

	if (!Array.isArray(symbolList)) {
		symbolList = [symbolList];
	}
	if (res?.admin) {
		globalSymbolsList.set(symbolList);
	}

	if (!Array.isArray(defaultSymbolList)) {
		defaultSymbol = [defaultSymbol];
	}
	if (res?.admin) {
		defaultSymbolList.set(defaultSymbol);
	}
}

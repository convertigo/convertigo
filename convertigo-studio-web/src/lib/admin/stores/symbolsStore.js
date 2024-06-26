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

	// Handling undefined or empty symbolList
	let symbolList = res.admin?.symbols?.symbol ?? [];
	// Ensure it's an array
	if (!Array.isArray(symbolList)) {
		symbolList = symbolList ? [symbolList] : [];
	}
	globalSymbolsList.set(symbolList);

	// Handling undefined or empty defaultSymbol
	let defaultSymbol = res.admin?.defaultSymbols?.defaultSymbol ?? [];
	// Ensure it's an array
	if (!Array.isArray(defaultSymbol)) {
		defaultSymbol = defaultSymbol ? [defaultSymbol] : [];
	}
	defaultSymbolList.set(defaultSymbol);
}

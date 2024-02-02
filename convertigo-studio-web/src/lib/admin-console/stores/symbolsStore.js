import { call } from "$lib/utils/service"
import { writable } from "svelte/store";

export let environmentVariables = writable([]);
export let globalSymbolsList = writable([]);


export async function getEnvironmentVar() {
    const res = await call('engine.GetEnvironmentVariablesList')
    console.log('env:', res);
    if (res?.admin) {
        environmentVariables.set(res.admin.environmentVariables.environmentVariable)
    }
}

export async function globalSymbols() {
    const res = await call('global_symbols.List')
    console.log('global symbols list:', res)

    let symbolList = res.admin.symbols.symbol

    if(!Array.isArray(symbolList)){
        symbolList = [symbolList];
    }
    if (res?.admin) {
        globalSymbolsList.set(symbolList)
    }
}



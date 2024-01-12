import { call } from '$lib/utils/service';
import { writable } from 'svelte/store';

export let configurations = writable(/** @type {any []} */ {});

export async function refreshConfigurations() {
	configurations.set(await call('configuration.List', {}));
}

export function updateConfiguration(categoryIndex, propertyIndex, newValue) {
    if (isValid(newValue)) { 
        configurations.update(currentConfigs => {
            currentConfigs.admin.category[categoryIndex].property[propertyIndex]['@_value'] = newValue;
            console.log("New Value:", newValue);
            return currentConfigs;
        });
        
    } else {
        console.error("invalid Value:", newValue);
    }
}

function isValid(value) {
    if (typeof value === 'string') {
        return value.trim().length > 0;
    } else if (typeof value === 'number') {
        return !isNaN(value);
    } else {
        return value != null; 
    }
}


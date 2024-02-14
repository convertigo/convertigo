import { call, callXml } from '$lib/utils/service';
import { get, writable } from 'svelte/store';

export let configurations = writable(/** @type {any []} */ {});

export async function refreshConfigurations() {
	configurations.set(await call('configuration.List', {}));
}

export function getPropertyKey(categoryIndex, propertyIndex) {
	const currentConfigs = get(configurations);
	const category = currentConfigs.admin?.category[categoryIndex];
	const property = category?.property[propertyIndex];

	return property ? property['@_name'] : null;
}

export async function updateConfiguration(categoryIndex, propertyIndex, newValue) {
	const propertyKey = getPropertyKey(categoryIndex, propertyIndex);

	if (!propertyKey) {
		console.error('key not founded');
		return;
	}

	const xmlPayload = `<configuration><property key="${propertyKey}" value="${newValue}"/></configuration>`;

	const response = await callXml('configuration.Update', xmlPayload);

	console.log('update service:', response);
	if (response) {
		configurations.update((currentConfigs) => {
			currentConfigs.admin.category[categoryIndex].property[propertyIndex]['@_value'] = newValue;
			console.log('New Value:', newValue);
			return currentConfigs;
		});
	} else {
		console.error('invalid Value:', newValue);
	}
}

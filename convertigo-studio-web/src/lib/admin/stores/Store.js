import { call } from '$lib/utils/service';

export async function fetchEngineStatus() {
	try {
		const response = await call('engine.GetStatus');
		return response;
	} catch (error) {
		console.error('Error fetching engine status:', error);
		throw error;
	}
}

export async function fetchSystemInformation() {
	try {
		const response = await call('engine.GetSystemInformation');
		return response;
	} catch (error) {
		console.error('Error fetching engine status:', error);
		throw error;
	}
}

/**
 * Fetch the main parameters data from the configuration.Update service.
 * @returns {Promise<any>} - Returns the parsed XML data as a JavaScript object.
 */
export async function fetchMainParameters() {
	try {
		const response = await call('configuration.List', {});
		return response;
	} catch (error) {
		console.error('Error fetching main parameters:', error);
		throw error;
	}
}

/**
 * @param {any} settingKey
 * @param {any} settingValue
 */
export async function updateServerSetting(settingKey, settingValue) {
	try {
		const response = await call('configuration.Update', {
			'@_xml': true,
			configuration: { '@_key': settingKey, '@_value': settingValue }
		});

		if (response.admin?.update['@_status'] === 'ok') {
			return true;
		} else {
			return false;
		}
	} catch (error) {
		return false;
	}
}

export async function fetchConnectionsList() {
	try {
		const response = await call('connections.List');
		return response;
	} catch (error) {
		console.error('Error fetching data connections list:', error);
		throw error;
	}
}

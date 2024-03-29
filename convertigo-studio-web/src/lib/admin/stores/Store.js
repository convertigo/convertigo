import { call } from '$lib/utils/service';

export async function fetchEngineStatus() {
	try {
		const response = await call('engine.GetStatus');
		//console.log('Response from Engine.GetStatus:', response);
		return response;
	} catch (error) {
		console.error('Error fetching engine status:', error);
		throw error;
	}
}

export async function fetchSystemInformation() {
	try {
		const response = await call('engine.GetSystemInformation');
		/* console.log("Response from Engine.GetSystemInformation:", response); */
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
		console.log('Response from configuration.List:', response);
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

		console.log('Réponse complète:', response);

		if (response.admin?.update['@_status'] === 'ok') {
			console.log('Mise à jour réussie');
			return true;
		} else {
			console.error('Échec de la mise à jour, statut:', response.admin?.update?.status);
			return false;
		}
	} catch (error) {
		console.error('Erreur lors de la mise à jour des paramètres du serveur:', error);
		return false;
	}
}

export async function fetchConnectionsList() {
	try {
		const response = await call('connections.List');
		console.log('Response from connections.List:', response);
		return response;
	} catch (error) {
		console.error('Error fetching data connections list:', error);
		throw error;
	}
}

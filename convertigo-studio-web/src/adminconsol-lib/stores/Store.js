import { call, callXml } from "$lib/utils/service";
import { XMLParser } from 'fast-xml-parser';

/**
 * Fetch the monitoring data from engine.Monitor
 * @returns {Promise<any>} - Returns the parsed XML data as a JavaScript object
 */
export async function fetchEngineMonitorData() {
	try {
		const response = await call('engine.Monitor');
		/* console.log("Response from engine.Monitor:", response); */
		return response;
	} catch (error) {
		console.error('Error fetching engine.Monitor data:', error);
		throw error;
	}
}

export async function fetchEngineStatus() {
	try {
		const response = await call('engine.GetStatus');
		/* console.log("Response from Engine.GetStatus:", response); */
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
		console.log("Response from configuration.List:", response);
		return response;
	} catch (error) {
		console.error('Error fetching main parameters:', error);
		throw error;
	}
}



export async function updateServerSetting(settingKey, settingValue) {
	try {
		const xmlPayload = `<configuration><property key="${settingKey}" value="${settingValue}"/></configuration>`;
		const response = await callXml('configuration.Update', xmlPayload);

		// Imprimez la réponse complète pour déboguer
		console.log("Réponse complète:", response);

		// Vérifiez que la propriété 'update' et la propriété 'status' existent dans la réponse
		if (response.admin?.update['@_status'] === 'ok') {
			console.log('Mise à jour réussie');
			return true;
		} else {
			// Si la propriété 'status' n'est pas 'ok' ou n'est pas présente, imprimez ce que vous avez
			console.error("Échec de la mise à jour, statut:", response.admin?.update?.status);
			return false;
		}
	} catch (error) {
		console.error('Erreur lors de la mise à jour des paramètres du serveur:', error);
		return false;
	}
}

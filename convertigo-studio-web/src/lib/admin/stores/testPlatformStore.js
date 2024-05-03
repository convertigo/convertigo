import { writable } from 'svelte/store';
import { call } from '$lib/utils/service';

export let projectStore = writable({
	connectors: [],
	sequences: []
});

//Ensures anything is treated as an array
const asArray = (data) => (Array.isArray(data) ? data : data ? [data] : []);

const processEntities = (entities) =>
	entities.map((entity) => ({
		name: entity['@_name'],
		transactions: entity.transaction ? processEntities(asArray(entity.transaction)) : [],
		variables: entity.variable ? asArray(entity.variable) : []
	}));

export async function getProjectTestPlatform(projectName) {
	try {
		const res = await call('projects.GetTestPlatform', { projectName });
		const project = res?.admin?.project;

		const projectData = {
			connectors: processEntities(asArray(project?.connector)),
			sequences: processEntities(asArray(project?.sequence))
		};

		projectStore.set(projectData);
	} catch (error) {
		console.error('Error fetching project test platform:', error);
	}
}

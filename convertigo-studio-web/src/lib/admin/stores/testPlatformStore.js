import { writable } from 'svelte/store';
import { call } from '$lib/utils/service';

export let projectStore = writable({
	connectors: [],
	transactions: [],
	sequences: []
});

export async function getProjectTestPlatform(projectName) {
	try {
		const res = await call('projects.GetTestPlatform', {
			projectName
		});
		let projectData = {
			connectors: [],
			sequences: []
		};

		// Ensure the connector data is properly handled
		const connectors = res?.admin?.project?.connector
			? Array.isArray(res.admin.project.connector)
				? res.admin.project.connector
				: [res.admin.project.connector]
			: [];

		projectData.connectors = connectors.map((connector) => {
			const transactions = connector.transaction
				? Array.isArray(connector.transaction)
					? connector.transaction
					: [connector.transaction]
				: [];

			return {
				name: connector['@_name'],
				transactions: transactions.map((transaction) => {
					const variables = transaction.variable
						? Array.isArray(transaction.variable)
							? transaction.variable
							: [transaction.variable]
						: [];
					return {
						name: transaction['@_name'],
						variables
					};
				})
			};
		});
		// Process sequences
		const sequences = res?.admin?.project?.sequence
			? Array.isArray(res.admin.project.sequence)
				? res.admin.project.sequence
				: [res.admin.project.sequence]
			: [];

		projectData.sequences = sequences.map((sequence) => ({
			name: sequence['@_name'],
			variables: sequence.variable
				? Array.isArray(sequence.variable)
					? sequence.variable
					: [sequence.variable]
				: []
		}));

		//@ts-ignore
		projectStore.set(projectData);
	} catch (error) {
		console.error('Error fetching project test platform:', error);
	}
}

import { checkArray } from '$lib/utils/service';
import ServiceHelper from './ServiceHelper.svelte';

const defValues = {
	name: null,
	connector: Array(2).fill({
		name: null,
		transaction: Array(10).fill({
			name: null,
			variable: Array(10).fill({
				name: null,
				value: null
			})
		})
	}),
	sequence: Array(2).fill({
		name: null,
		variable: Array(10).fill({
			name: null,
			value: null
		})
	})
};
let projects = $state({});

export default function (projectName) {
	if (!projects[projectName]) {
		projects[projectName] = ServiceHelper({
			defValues,
			service: 'projects.GetTestPlatform',
			params: { projectName },
			mapping: { '': 'admin.project' },
			beforeUpdate: (res) => {
				res.connector = checkArray(res.connector);
				for (const connector of res.connector) {
					connector.transaction = checkArray(connector.transaction);
					for (const transaction of connector.transaction) {
						transaction.variable = checkArray(transaction.variable);
					}
				}
				res.sequence = checkArray(res.sequence);
				for (const sequence of res.sequence) {
					sequence.variable = checkArray(sequence.variable);
				}
			}
		});
	}
	return projects[projectName];
}

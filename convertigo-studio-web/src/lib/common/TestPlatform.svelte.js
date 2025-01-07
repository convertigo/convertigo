import { checkArray } from '$lib/utils/service';
import { check } from 'prettier';
import ServiceHelper from './ServiceHelper.svelte';

const defValues = {
	name: null,
	comment: null,
	connector: Array(2).fill({
		name: null,
		comment: null,
		transaction: Array(10).fill({
			name: null,
			comment: null,
			accessibility: 'Public',
			variable: Array(10).fill({
				name: null,
				value: null,
				send: false,
				comment: null
			})
		})
	}),
	sequence: Array(2).fill({
		name: null,
		comment: null,
		accessibility: 'Public',
		variable: Array(10).fill({
			name: null,
			value: null,
			send: false,
			comment: null
		})
	}),
	mobileapplication: null
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
						for (const variable of transaction.variable) {
							variable.send = false;
						}
					}
				}
				res.sequence = checkArray(res.sequence);
				for (const sequence of res.sequence) {
					sequence.variable = checkArray(sequence.variable);
					for (const variable of sequence.variable) {
						variable.send = false;
					}
				}
				if (res.mobileapplication) {
					res.mobileapplication.mobileplatform = checkArray(res.mobileapplication.mobileplatform);
				}
			}
		});
	}
	return projects[projectName];
}

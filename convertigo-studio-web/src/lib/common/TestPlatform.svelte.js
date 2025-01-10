import { call, checkArray } from '$lib/utils/service';
import ServiceHelper from './ServiceHelper.svelte';

function checkRequestables(parent, key) {
	parent[key] = checkArray(parent[key]);
	for (const requestable of parent[key]) {
		requestable.variable = checkArray(requestable.variable);
		for (const variable of requestable.variable) {
			variable.send = false;
		}
		if (key != 'testcase') {
			checkRequestables(requestable, 'testcase');
		}
	}
}

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
					checkRequestables(connector, 'transaction');
				}
				checkRequestables(res, 'sequence');
				if (res.mobileapplication) {
					res.mobileapplication.mobileplatform = checkArray(res.mobileapplication.mobileplatform);
					for (const mobileplatform of res.mobileapplication.mobileplatform) {
						mobileplatform.local = ServiceHelper({
							defValues: { revision: null },
							service: 'mobiles.GetLocalRevision',
							params: { project: projectName, platform: mobileplatform.name },
							mapping: { '': 'admin' }
						});
						let waiting = $state(false);
						mobileplatform.built = ServiceHelper({
							values: {
								get waiting() {
									return waiting;
								}
							},
							defValues: {
								revision: null,
								endpoint: null,
								phonegap_version: null,
								status: null,
								version: null,
								error: null
							},
							service: 'mobiles.GetBuildStatus',
							params: { project: projectName, platform: mobileplatform.name },
							mapping: { '': 'admin.build' },
							beforeUpdate: (res) => {
								if (res.status == 'pending') {
									window.setTimeout(mobileplatform.built.refresh, 2000);
								}
								return res;
							}
						});
						mobileplatform.build = async () => {
							waiting = true;
							try {
								await call('mobiles.LaunchBuild', {
									project: projectName,
									platform: mobileplatform.name
								});
							} finally {
								waiting = false;
							}
							mobileplatform.built.refresh();
						};
					}
				}
			}
		});
	}
	return projects[projectName];
}

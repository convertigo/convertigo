// import {
//     writable
// } from 'svelte/store';
// import {
//     call,
//     checkArray,
//     deepObject
// } from '$lib/utils/service';

// export let testPlatformStore = writable({});
// export let connectorsStore = writable([]);
// export let sequencesStore = writable([]);

// export async function checkTestPlatform(projectName) {
//     const res = await call('projects.GetTestPlatform', {
//         projectName
//     });

//     const projectData = checkArray(res?.admin?.project).map((project) => ({
//         ...project,
//     }))
//     testPlatformStore.set(projectData);

//     // Process and store connectors data
//     const connectorsData = checkArray(res?.admin?.project?.connector).map((connector) => {
//         const processedTransactions = checkArray(connector.transaction).map((transaction) => ({
//             ...transaction,
//             variables: checkArray(transaction.variable)
//         }));
//         return {
//             ...connector,
//             variables: checkArray(connector.variable),
//             transactions: processedTransactions
//         };
//     });
//     console.log('Processed Connectors:', connectorsData);
//     //@ts-ignore
//     connectorsStore.set(connectorsData.map(deepObject));
//     console.log('ConnecotrStore', connectorsStore)
//     // Process and store sequences data
//     const sequencesData = checkArray(res?.admin?.project?.sequence).map((sequence) => {
//         const processedTestcases = checkArray(sequence.testcase).map((testcase) => ({
//             ...testcase,
//             variables: checkArray(testcase.variable)
//         }));
//         console.log('Processed Testcases for sequence', sequence['@_name'], processedTestcases);
//         return {
//             ...sequence,
//             variables: checkArray(sequence.variable),
//             testcases: processedTestcases
//         };
//     });

//     //@ts-ignore
//     sequencesStore.set(sequencesData.map(deepObject));
//     console.log('sequencesStore', sequencesStore);
//     // Update testPlatformStore
//     testPlatformStore.update((tp) => {
//         tp[projectName] = deepObject(res?.admin?.project ?? {});
//         return tp;
//     });
// }

import { writable } from 'svelte/store';
import { call, checkArray } from '$lib/utils/service';

export let testPlatformStore = writable([]);
export let connectorsStore = writable([]);
export let sequencesStore = writable([]);

export async function checkTestPlatform(projectName) {
	const res = await call('projects.GetTestPlatform', {
		projectName
	});

	// Process and store project data
	const projectData = checkArray(res?.admin?.project).map((project) => ({
		...project,
		connectors: checkArray(project.connector).map((connector) => {
			const processedTransactions = checkArray(connector.transaction).map((transaction) => ({
				...transaction,
				variables: checkArray(transaction.variable)
			}));
			return {
				...connector,
				variables: checkArray(connector.variable),
				transactions: processedTransactions
			};
		}),
		sequences: checkArray(project.sequence).map((sequence) => {
			const processedTestcases = checkArray(sequence.testcase).map((testcase) => ({
				...testcase,
				variables: checkArray(testcase.variable)
			}));
			return {
				...sequence,
				variables: checkArray(sequence.variable),
				testcases: processedTestcases
			};
		})
	}));

	//@ts-ignore
	testPlatformStore.set(projectData);
	console.log('testPlatform proeject Data', testPlatformStore);
	//@ts-ignore
	connectorsStore.set(projectData.flatMap((project) => project.connectors));
	//@ts-ignore
	sequencesStore.set(projectData.flatMap((project) => project.sequences));
}

// // src/lib/stores/testPlatform.js
// import { writable } from 'svelte/store';
// import { call, checkArray, deepObject } from '$lib/utils/service';

// export let testPlatformStore = writable({});
// export let connectorsStore = writable([]);
// export let transactionStore = writable([]);
// export let sequenceStore = writable([]);
// export async function checkTestPlatform(projectName) {
// 	const res = await call('projects.GetTestPlatform', {
// 		projectName
// 	});
// 	const sequencesData = checkArray(res?.admin?.project?.sequence);
// 	const connectorsData = checkArray(res?.admin?.project?.connector);
//     const transactionData = checkArray(res?.admin?.project?.connector?.variable)

// 	connectorsData.unshift({ '@_name': 'Sequences', transaction: sequencesData });
// 	console.log('connectors', connectorsData);
// 	// Process and store connectors data
// 	//@ts-ignore
// 	connectorsStore.set(connectorsData.map(deepObject));
//     //@ts-ignore
//     sequenceStore.set(sequencesData.map(deepObject))
// 	// Update testPlatformStore
//     console.log('sequences', sequencesData)

//     // transactionStore.set()
// 	testPlatformStore.update((tp) => {
// 		tp[projectName] = deepObject(res?.admin?.project ?? {});
// 		return tp;
// 	});
// }

import { writable } from 'svelte/store';
import { call, checkArray, deepObject } from '$lib/utils/service';

export let testPlatformStore = writable({});
export let connectorsStore = writable([]);
export let sequencesStore = writable([]);

export async function checkTestPlatform(projectName) {
	const res = await call('projects.GetTestPlatform', { projectName });

	// Process and store connectors data
	const connectorsData = checkArray(res?.admin?.project?.connector).map((connector) => {
		const processedTransactions = checkArray(connector.transaction).map((transaction) => ({
			...transaction,
			variables: checkArray(transaction.variable)
		}));
		return {
			...connector,
			variables: checkArray(connector.variable),
			transactions: processedTransactions
		};
	});
	console.log('Processed Connectors:', connectorsData);
	//@ts-ignore
	connectorsStore.set(connectorsData.map(deepObject));

	// Process and store sequences data
	const sequencesData = checkArray(res?.admin?.project?.sequence).map((sequence) => {
		const processedTestcases = checkArray(sequence.testcase).map((testcase) => ({
			...testcase,
			variables: checkArray(testcase.variable)
		}));
		console.log('Processed Testcases for sequence', sequence['@_name'], processedTestcases);
		return {
			...sequence,
			variables: checkArray(sequence.variable),
			testcases: processedTestcases
		};
	});

	//@ts-ignore
	sequencesStore.set(sequencesData.map(deepObject));
	console.log('sequencesStore', sequencesStore);
	// Update testPlatformStore
	testPlatformStore.update((tp) => {
		tp[projectName] = deepObject(res?.admin?.project ?? {});
		return tp;
	});
}

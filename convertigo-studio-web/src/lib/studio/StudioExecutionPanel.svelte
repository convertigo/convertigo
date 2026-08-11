<script>
	import RequestableExecution from '$lib/admin/components/RequestableExecution.svelte';
	import { checkArray } from '$lib/utils/service';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * @typedef {Object} SequenceRequestable
	 * @property {string=} name
	 * @property {string=} comment
	 * @property {any[]=} variable
	 * @property {any[]=} testcase
	 * @property {string=} response
	 * @property {string=} language
	 * @property {boolean=} loading
	 * @property {any=} tc
	 */

	/**
	 * @type {{
	 *  projectName?: string,
	 *  requestable?: SequenceRequestable | null,
	 *  requestableKind?: string,
	 *  connectorName?: string
	 * }}
	 */
	let {
		projectName = '',
		requestable: selectedRequestable = null,
		requestableKind = 'sequence',
		connectorName = ''
	} = $props();

	const modes = ['JSON', 'XML', 'BIN', 'CXML'];
	let mode = $state('JSON');
	let executionKind = $derived(selectedRequestable ? requestableKind : 'sequence');
	let requestable = $state(/** @type {SequenceRequestable | null} */ (null));
	let requestableSourceKey = $derived.by(() =>
		buildRequestableSourceKey(projectName, selectedRequestable, executionKind, connectorName)
	);
	let appliedRequestableSourceKey = '';
	let requestableKey = $derived(
		requestable
			? `${projectName}\u0000${executionKind}\u0000${connectorName}\u0000${requestable.name ?? ''}`
			: ''
	);

	/**
	 * @param {any} value
	 * @returns {'true' | 'false'}
	 */
	function normalizeSend(value) {
		return value === true || value == 'true' ? 'true' : 'false';
	}

	/**
	 * @param {any} variable
	 * @returns {any}
	 */
	function cloneVariable(variable) {
		return {
			...variable,
			send: normalizeSend(variable?.send)
		};
	}

	/**
	 * @param {any} testcase
	 * @returns {any}
	 */
	function cloneTestcase(testcase) {
		return {
			...testcase,
			variable: checkArray(testcase?.variable).map(cloneVariable)
		};
	}

	/**
	 * @param {SequenceRequestable | null | undefined} source
	 * @returns {SequenceRequestable | null}
	 */
	function cloneRequestable(source) {
		if (!source) {
			return null;
		}
		return {
			...source,
			variable: checkArray(source.variable).map(cloneVariable),
			testcase: checkArray(source.testcase).map(cloneTestcase)
		};
	}

	/**
	 * @param {string} project
	 * @param {SequenceRequestable | null | undefined} source
	 * @param {string} kind
	 * @param {string} connector
	 * @returns {string}
	 */
	function buildRequestableSourceKey(project, source, kind, connector) {
		if (!source) {
			return '';
		}
		return JSON.stringify({
			project,
			kind,
			connector,
			name: source.name ?? '',
			variable: checkArray(source.variable).map((variable) => [
				variable?.name,
				variable?.value,
				variable?.isMultivalued,
				variable?.isMasked,
				variable?.isFileUpload
			]),
			testcase: checkArray(source.testcase).map((testcase) => [
				testcase?.name,
				checkArray(testcase?.variable).map((variable) => [variable?.name, variable?.value])
			])
		});
	}

	$effect(() => {
		if (requestableSourceKey == appliedRequestableSourceKey) {
			return;
		}
		appliedRequestableSourceKey = requestableSourceKey;
		requestable = cloneRequestable(selectedRequestable);
	});
</script>

<div class="studio-execution">
	{#if !projectName}
		<StudioEmptyState message="No project selected" full />
	{:else if !requestable}
		<StudioEmptyState message="No requestable selected" full />
	{:else}
		<RequestableExecution
			{projectName}
			bind:requestable
			kind={executionKind}
			{connectorName}
			bind:mode
			{modes}
			showComment={true}
			showTestcaseEdit={true}
			stickyActions={true}
			freshContext={true}
			testcaseValue={`${requestableKey}.testcases`}
		/>
	{/if}
</div>

<style>
	.studio-execution {
		height: 100%;
		min-height: 0;
		overflow: auto;
		padding: 0.75rem;
	}
</style>

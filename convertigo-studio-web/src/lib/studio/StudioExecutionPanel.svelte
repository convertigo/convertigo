<script>
	import RequestableExecution from '$lib/admin/components/RequestableExecution.svelte';

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
	let requestable = $derived(selectedRequestable);
	let executionKind = $derived(selectedRequestable ? requestableKind : 'sequence');
	let requestableKey = $derived(
		requestable
			? `${projectName}\u0000${executionKind}\u0000${connectorName}\u0000${requestable.name ?? ''}`
			: ''
	);
</script>

<div class="studio-execution">
	{#if !projectName}
		<div class="studio-execution__empty">No project selected</div>
	{:else if !requestable}
		<div class="studio-execution__empty">No requestable selected</div>
	{:else}
		<RequestableExecution
			{projectName}
			{requestable}
			kind={executionKind}
			{connectorName}
			bind:mode
			{modes}
			showComment={true}
			showTestcaseEdit={true}
			stickyActions={true}
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

	.studio-execution__empty {
		display: grid;
		height: 100%;
		place-items: center;
		color: var(--color-surface-600-400);
		font-size: 0.86rem;
	}
</style>

<script>
	import { call, getStudioContextMenu } from '$lib/utils/service';
	import StudioActionBuilderPanel from './StudioActionBuilderPanel.svelte';
	import { resolveStudioBuilder } from './studioBuilder';
	import StudioBuilderPanel from './StudioBuilderPanel.svelte';

	/** @type {Record<string, any>} */
	let { projectName = '', ...props } = $props();
	let result = $derived(
		resolveStudioBuilder(projectName, {
			menu: getStudioContextMenu,
			children: (id) => call('studio.treeview.Get', { id })
		})
	);
</script>

{#key projectName}
	{#await result}
		<p class="p">Connecting to the builder…</p>
	{:then descriptor}
		{#if descriptor.transport === 'ngx'}
			<StudioBuilderPanel {projectName} {...props} />
		{:else if descriptor.transport === 'actions'}
			<StudioActionBuilderPanel {projectName} builders={descriptor.builders} {...props} />
		{:else}
			<p class="p">The project {projectName || 'selected'} has no frontend builder.</p>
		{/if}
	{:catch error}
		<p class="p" role="alert">Unable to connect to the builder: {error.message}</p>
	{/await}
{/key}

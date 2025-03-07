<script>
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Icon from '@iconify/svelte';
	import { onMount, tick } from 'svelte';

	/** @type {{definition: any, data: any, showHeaders?: boolean, title?: string, comment?: string, class?: string, title_1?: import('svelte').Snippet, children?: import('svelte').Snippet<[any]>, tr?: import('svelte').Snippet<[any]>}} */
	let {
		definition,
		data,
		showHeaders = true,
		title = '',
		comment = '',
		class: cls = '',
		children,
		tr
	} = $props();

	let isCardView = $state(false);
	let container;

	function checkOverflow() {
		isCardView = container.scrollWidth > container.clientWidth;
		tick().then(() => {
			isCardView = isCardView || container?.scrollWidth > container?.clientWidth;
		});
	}

	onMount(() => {
		window.addEventListener('resize', checkOverflow);
		return () => window.removeEventListener('resize', checkOverflow);
	});

	$effect(() => {
		if (data) {
			checkOverflow();
		}
	});
</script>

<div bind:this={container} class="table-container {cls}" class:autocard={isCardView}>
	{#if title.length > 0}
		<h1 class="text-[16px] font-normal text-surface-800-200">{title}</h1>
	{/if}
	{#if comment.length > 0}
		<h1 class="font-bold text-surface-700-300 p-3">{comment}</h1>
	{/if}

	<table>
		{#if showHeaders}
			<thead>
				<tr>
					{#each definition as def}
						<th class={def.th}>
							{#if def.icon}
								<Icon icon={def.icon} class="h-7 w-7" />
							{:else}
								{def.name ?? ''}
							{/if}
						</th>
					{/each}
				</tr>
			</thead>
		{/if}
		{#if data && data.length > 0}
			<tbody>
				{#each data as row, rowIdx}
					{#snippet _tr({ row, rowIdx })}
						<tr>
							{#each definition as def}
								<td
									class={def.class
										? typeof def.class == 'function'
											? def.class(row)
											: def.class
										: ''}
									data-label={showHeaders ? (def.name ?? '') : ''}
								>
									{#if def.custom}
										{#if children}
											{@render children({ row, def, rowIdx })}
										{:else}
											{row[def.key] ?? ''}
										{/if}
									{:else}
										<AutoPlaceholder loading={row[def.key] == null}
											>{row[def.key] ?? ''}</AutoPlaceholder
										>
									{/if}
								</td>
							{/each}
						</tr>
					{/snippet}
					{#if tr}
						{@render tr({ row, rowIdx, tr: _tr, definition })}
					{:else}
						{@render _tr({ row, rowIdx })}
					{/if}
				{/each}
			</tbody>
		{:else}
			<tbody>
				<tr>
					<td colspan={definition.length}>
						<div class="layout-x">
							<Ico icon="line-md:coffee-loop" size={20} />
							<p class="font-bold text-surface-300">There is no data to display ...</p>
						</div>
					</td>
				</tr>
			</tbody>
		{/if}
	</table>
</div>

<style>
	@reference "../../../app.css";

	.table-container {
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
		width: 100%;
		container-type: inline-size;
	}

	.autocard {
		th,
		td {
			@apply text-sm text-wrap;
		}

		table {
			background-color: unset;
		}

		tr {
			display: block;
			@apply rounded layout-grid-low-48;
		}

		thead {
			display: none;
		}
		td {
			@apply layout-y-start-low overflow-x-auto;
		}
		td[data-label]:not([data-label='']):before {
			content: attr(data-label);
			display: inline-block;
			font-weight: bold;
			line-height: 1.5;
			width: 100%;
			z-index: 1;
		}
	}
</style>

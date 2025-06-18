<script>
	import Icon from '@iconify/svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { onMount, tick } from 'svelte';
	import { fly } from 'svelte/transition';

	/** @type {{definition: any, data: any, showHeaders?: boolean, title?: string, comment?: string, class?: string, trClass?: string, title_1?: import('svelte').Snippet, children?: import('svelte').Snippet<[any]>, tr?: import('svelte').Snippet<[any]>}} */
	let {
		definition,
		data,
		showHeaders = true,
		title = '',
		comment = '',
		class: cls = '',
		trClass = 'even:preset-filled-surface-200-800 odd:preset-filled-surface-300-700 hover:preset-filled-surface-400-600',
		children,
		tr
	} = $props();

	const [duration, y, opacity] = [200, -50, 1];
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
		<h1 class="ztext-surface-800-200 text-[16px] font-normal">{title}</h1>
	{/if}
	{#if comment.length > 0}
		<h1 class="ztext-surface-700-300 p-3 font-bold">{comment}</h1>
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
						<tr class={trClass} data-custom={row.name} transition:fly={{ duration, y, opacity }}>
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
							<p class="ztext-surface-300 font-bold">There is no data to display ...</p>
						</div>
					</td>
				</tr>
			</tbody>
		{/if}
	</table>
</div>

<style>
	@reference "../../../app.css";

	table {
		width: 100%;
		border-collapse: collapse;
		border: 4px;
		@apply overflow-hidden rounded-sm;
	}
	th,
	td {
		text-align: left;
		font-weight: 300;
		font-size: 15px;
		@apply p-2! align-middle!;
	}
	th {
		@apply preset-filled-surface-200-800 font-bold;
	}
	thead {
		@apply border-b-[0.5px] border-surface-900-100;
	}
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
			@apply layout-grid-low-48 rounded;
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

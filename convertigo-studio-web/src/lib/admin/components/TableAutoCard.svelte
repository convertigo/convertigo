<script>
	import Icon from '@iconify/svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { onMount, tick } from 'svelte';
	import { fade } from 'svelte/transition';

	/** @type {{definition: any, data: any, showHeaders?: boolean, showNothing?: boolean, title?: string, comment?: string, class?: string, thClass?: string, trClass?: string, fnRowId?: function, animationProps?: any, children?: import('svelte').Snippet<[any]>, rowChildren?: import('svelte').Snippet<[any]>, thead?: import('svelte').Snippet<[any]>}} */
	let {
		definition,
		data,
		showHeaders = true,
		showNothing = true,
		title = '',
		comment = '',
		class: cls = '',
		thClass = 'preset-filled-surface-100-900',
		trClass = 'even:preset-filled-surface-100-900 odd:preset-filled-surface-200-800 hover:preset-filled-surface-300-700',
		fnRowId = (row, i) => row.name ?? i,
		animationProps = { duration: 100 },
		children,
		rowChildren,
		thead
	} = $props();

	const duration = 50;
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
		<h1 class="ztext-surface-700-300 p-3 font-medium">{comment}</h1>
	{/if}

	<table>
		{#if showHeaders}
			{#if thead}
				{@render thead({ definition })}
			{:else}
				<thead>
					<tr class={thClass}>
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
		{/if}
		{#if data && data.length > 0}
			<tbody>
				{#each data as row, rowIdx (fnRowId(row, rowIdx))}
					<tr class={trClass} data-custom={row.name} transition:fade={animationProps}>
						{#snippet rowRender()}
							{#each definition as def}
								<td
									class={def.class
										? typeof def.class == 'function'
											? def.class(row, def)
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
						{/snippet}
						{#if rowChildren}
							{@render rowChildren({ row, rowIdx, definition, rowRender })}
						{:else}
							{@render rowRender()}
						{/if}
					</tr>
				{/each}
			</tbody>
		{:else if showNothing}
			<tbody>
				<tr>
					<td colspan={definition.length}>
						<div class="layout-x">
							<Ico icon="mdi:coffee" size={20} />
							<p class="ztext-surface-300 font-medium">There is no data to display ...</p>
						</div>
					</td>
				</tr>
			</tbody>
		{/if}
	</table>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	table {
		width: 100%;
		border-collapse: collapse;
		border: 4px;
		@apply overflow-hidden rounded-base;
	}
	th {
		@apply font-normal;
	}
	th,
	td {
		@apply p-2! align-middle!;
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
			@apply text-wrap;
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

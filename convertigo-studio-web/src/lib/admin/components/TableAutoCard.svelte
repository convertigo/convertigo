<script>
	import Icon from '@iconify/svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { fromAction } from 'svelte/attachments';

	/** @type {{definition: any, data: any, showHeaders?: boolean, showNothing?: boolean, title?: string, comment?: string, class?: string, thClass?: string, trClass?: string, fnRowId?: function, animationProps?: any, children?: import('svelte').Snippet<[any]>, rowChildren?: import('svelte').Snippet<[any]>, thead?: import('svelte').Snippet<[any]>}} */
	let {
		definition,
		data,
		showHeaders = true,
		showNothing = true,
		title = '',
		comment = '',
		class: cls = '',
		thClass = 'text-left text-strong text-[14px] font-semibold',
		trClass = 'even:preset-filled-surface-100-900 odd:preset-filled-surface-200-800 transition-surface hover:bg-primary-100/60 dark:hover:bg-primary-500/15',
		fnRowId = (row, i) => row.name ?? i,
		children,
		rowChildren,
		thead
	} = $props();

	const duration = 50;
	const overflowThreshold = 8;
	let isCardView = $state(false);
	let tableMinWidth = $state(0);
	const attachContainer = $derived(fromAction(observeContainer));

	/** @param {HTMLDivElement} node */
	function observeContainer(node) {
		let rafId = 0;
		const update = () => {
			if (!node.isConnected) return;
			const table = node.querySelector('table');
			if (!table) return;
			const containerWidth = Math.floor(node.clientWidth);
			if (!isCardView) {
				const measured = Math.ceil(table.scrollWidth);
				if (measured > 0) {
					tableMinWidth = measured;
				}
			} else if (tableMinWidth === 0) {
				tableMinWidth = Math.ceil(table.scrollWidth);
			}
			if (!isCardView && containerWidth + overflowThreshold < tableMinWidth) {
				isCardView = true;
			} else if (isCardView && containerWidth >= tableMinWidth + overflowThreshold) {
				isCardView = false;
			}
		};
		const schedule = () => {
			if (rafId) return;
			rafId = requestAnimationFrame(() => {
				rafId = 0;
				update();
			});
		};

		update();

		const resizeObserver = new ResizeObserver(schedule);
		resizeObserver.observe(node);

		const mutationObserver = new MutationObserver(schedule);
		mutationObserver.observe(node, { childList: true, subtree: true, characterData: true });

		return {
			destroy() {
				if (rafId) {
					cancelAnimationFrame(rafId);
				}
				resizeObserver.disconnect();
				mutationObserver.disconnect();
			}
		};
	}
</script>

<div class="table-container {cls}" class:autocard={isCardView} {@attach attachContainer}>
	{#if title.length > 0}
		<h1 class="text-[16px] font-normal">{title}</h1>
	{/if}
	{#if comment.length > 0}
		<h1 class="p-3 font-medium">{comment}</h1>
	{/if}

	<div class="table-frame">
		<table class="w-full border-separate border-spacing-0">
			{#if showHeaders}
				{#if thead}
					{@render thead({ definition })}
				{:else}
					<thead>
						<tr class={thClass}>
							{#each definition as def (def.key ?? def.name ?? def)}
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
						<tr class={trClass} data-custom={row.name}>
							{#snippet rowRender()}
								{#each definition as def (def.key ?? def.name ?? def)}
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
				<tbody class="preset-filled-surface-200-800">
					<tr>
						<td colspan={definition.length}>
							<div class="layout-x">
								<p class="font-medium">This table is empty</p>
							</div>
						</td>
					</tr>
				</tbody>
			{/if}
		</table>
	</div>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	table {
		width: 100%;
		border-collapse: separate;
		border-spacing: 0;
	}
	th,
	td {
		@apply p-2! align-middle!;
	}
	thead {
		@apply border-b border-surface-300-700;
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
			color: var(--convertigo-text-strong);
			font-size: 14px;
			font-weight: 600;
			line-height: 1.5;
			width: 100%;
			z-index: 1;
		}
	}
</style>

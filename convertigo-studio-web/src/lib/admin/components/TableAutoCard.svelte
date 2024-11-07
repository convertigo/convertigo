<script>
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import Icon from '@iconify/svelte';

	/** @type {{definition: any, data: any, showHeaders?: boolean, title?: string, comment?: string, class?: string, title_1?: import('svelte').Snippet, children?: import('svelte').Snippet<[any]>}} */
	let {
		definition,
		data,
		showHeaders = true,
		title = '',
		comment = '',
		class: cls = '',
		title_1,
		children
	} = $props();
</script>

<div class="table-container {cls}">
	{#if title.length > 0}
		<h1 class="tableTitle">{title}</h1>
	{/if}
	{#if comment.length > 0}
		<h1 class="font-bold text-surface-300 p-3">{comment}</h1>
	{/if}
	{@render title_1?.()}

	<table class="rounded table">
		{#if showHeaders}
			<thead class="rounded">
				<tr>
					{#each definition as def}
						<th class="dark:bg-surface-800">
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
				{#each data as row}
					<tr>
						{#each definition as def}
							<td
								class={def.class
									? typeof def.class == 'function'
										? def.class(row)
										: def.class
									: ''}
								data-label={def.name ?? ''}
							>
								{#if def.custom}
									{#if children}
										{@render children({ row, def })}
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
				{/each}
			</tbody>
		{:else}
			<tbody>
				<tr>
					<td colspan={definition.length}>
						<div class="flex gap-5 items-center">
							<Ico icon="line-md:coffee-loop" class="w-20 h-20" />
							<p class="font-bold text-surface-300">There is no data to display ...</p>
						</div>
					</td>
				</tr>
			</tbody>
		{/if}
	</table>
</div>

<style lang="postcss">
	.tableTitle {
		@apply text-[16px] font-normal text-surface-200 mb-3 mt-5;
	}
	.table-container {
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
	}

	@media (max-width: 640px), (max-width: 30vw) {
		th,
		td {
			@apply text-sm text-wrap;
		}

		table {
			background-color: unset;
		}

		tr {
			display: block;
			@apply rounded;
		}

		tr + tr {
			margin-top: 1.5rem;
		}

		thead {
			display: none;
		}
		td {
			justify-content: flex-start;
			align-items: center;
			position: relative;
			@apply border flex;
		}
		td[data-label]:not([data-label='']) {
			padding-left: 50%;
		}
		td[data-label]:not([data-label='']):before {
			content: attr(data-label);
			display: inline-block;
			font-weight: bold;
			line-height: 1.5;
			margin-left: -100%;
			width: 100%;
			/* width: var(--witdh, 100px); */
			position: relative;
			z-index: 1;
		}
	}
</style>

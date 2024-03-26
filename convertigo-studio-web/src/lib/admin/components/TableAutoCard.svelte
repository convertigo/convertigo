<script>
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { loading } from '$lib/utils/loadingStore';
	import Icon from '@iconify/svelte';

	export let definition;
	export let data;
	export let showHeaders = true;
	let cls = '';
	export { cls as class };
</script>

<div class={`table-container ${cls}`}>
	<table class="rounded-token table">
		{#if showHeaders}
			<thead class="rounded-token">
				<tr>
					{#each definition as def}
						<th class="header dark:bg-surface-800">
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
		<tbody>
			{#each data as row}
				<tr>
					{#each definition as def}
						<td data-label={def.name ?? ''}>
							{#if def.custom}
								<slot {row} {def}>{row[def.key] ?? ''}</slot>
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
	</table>
</div>

<style lang="postcss">
	.table-container {
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
	}

	@media (max-width: 640px) {
		th,
		td {
			@apply text-sm text-wrap;
		}

		table {
			background-color: unset;
		}

		tr {
			display: block;
			@apply rounded-token;
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
			@apply border-token flex;
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

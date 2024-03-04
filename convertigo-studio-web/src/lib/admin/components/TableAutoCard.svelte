<script>
	export let definition;
	export let data;
	let cls = '';
	export { cls as class };
</script>

<div class={`table-container ${cls}`}>
	<table class="rounded-token table">
		<thead class="rounded-token">
			<tr>
				{#each definition as def}
					<th class="header dark:bg-surface-800">{def.name}</th>
				{/each}
			</tr>
		</thead>
		<tbody>
			{#each data as row}
				<tr>
					{#each definition as def}
						<td data-label={def.name}>
							{#if def.custom}
								<slot {row} {def}>{row[def.key] ?? ''}</slot>
							{:else}
								{row[def.key] ?? ''}
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
			padding-left: 50%;
			position: relative;
			@apply border-token flex;
		}
		td:before {
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

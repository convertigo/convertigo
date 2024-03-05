<script>
	export let headers = [];
</script>

<table class="rounded-xl table overflow-hidden" {...$$restProps}>
	<thead class="rounded-xl">
		<tr>
			{#each headers as header}
				<th class="header dark:text-surface-100 text-surface-800 font-normal">{header}</th>
			{/each}
		</tr>
	</thead>
	<tbody>
		<slot />
	</tbody>
</table>

<style lang="postcss">
	.header {
		@apply dark:bg-surface-800 bg-white border-b-[1px] dark:border-primary-100 border-primary-100;
	}
</style>

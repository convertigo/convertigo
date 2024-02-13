<script>
	export let headers = [];
</script>

<table class="rounded-xl table">
	<thead class="rounded-xl">
		<tr>
			{#each headers as header}
				<th class="header dark:text-surface-100 text-white font-normal">{header}</th>
			{/each}
		</tr>
	</thead>
	<tbody>
		<slot />
	</tbody>
</table>

<style lang="postcss">
	.header {
		@apply dark:bg-surface-600 bg-surface-300 border-b-[1px] dark:border-primary-100 border-primary-100;
	}
</style>

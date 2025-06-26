<script>
	import VirtualList from 'svelte-tiny-virtual-list';

	let data = $state([
		['A', '1'],
		['B', '2'],
		['C', '3'],
		['D', '4']
	]);

	let filter = $state('');

	let lines = $derived(data.filter((item) => item[0].toLowerCase().includes(filter.toLowerCase())));

	let bug = $state(true);

	function itemSize(index) {
		return 50;
	}
</script>

<div>Bug: <input type="checkbox" bind:checked={bug} /></div>
<div>Filter: <input type="text" placeholder="filter" bind:value={filter} /></div>
<!-- <VirtualList height={400} width="auto" itemCount={lines.length} itemSize={50}>
	{#snippet children({ style, index })}
		{@const line = lines[index]}
		<div {style}>
			{#if line || bug}
				{line[0]} : {line[1]}
			{:else}
				<span>Loading...</span>
			{/if}
		</div>
	{/snippet}
{#snippet header()}
head
{/snippet}
{#snippet footer()}
foot
{/snippet}
</VirtualList> -->
<VirtualList height={400} width="auto" itemCount={lines.length} itemSize={50}>
	<div slot="item" let:index let:style {style}>
		{@const line = lines[index]}
		{line[0]} : {line[1]}
	</div>
</VirtualList>

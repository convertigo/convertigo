<script>
	import {
		getLocalTimeZone,
		now,
		toCalendar,
		toCalendarDate,
		toTime
	} from '@internationalized/date';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import DateRangePicker from '$lib/common/components/DateRangePicker.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import TreeView from '$lib/common/components/TreeView.svelte';
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

<LightSwitch></LightSwitch>
<div>Bug: <input type="checkbox" bind:checked={bug} /></div>
<div>Filter: <input type="text" placeholder="filter" bind:value={filter} /></div>
<div>Now: {now(getLocalTimeZone()).toString()}</div>
<div>Now: {toTime(now(getLocalTimeZone()))}</div>
<div>Now: {toCalendarDate(now(getLocalTimeZone()))}</div>
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

<!-- <VirtualList height={400} width="auto" itemCount={lines.length} itemSize={50}>
	<div slot="item" let:index let:style {style}>
		{@const line = lines[index]}
		{line[0]} : {line[1]}
	</div>
</VirtualList> -->

<div class="max-w-96"><PropertyType></PropertyType></div>
<!-- <DateRangePicker></DateRangePicker> -->

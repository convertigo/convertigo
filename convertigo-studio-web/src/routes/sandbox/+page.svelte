<script>
	import {
		getLocalTimeZone,
		now,
		toCalendar,
		toCalendarDate,
		toTime
	} from '@internationalized/date';
	import { SegmentedControl } from '@skeletonlabs/skeleton-svelte';
	import CheckState from '$lib/admin/components/CheckState.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import DateRangePicker from '$lib/common/components/DateRangePicker.svelte';
	import LightSwitch from '$lib/common/components/LightSwitch.svelte';
	import TreeView from '$lib/common/components/TreeView.svelte';
	import { tick } from 'svelte';
	import VirtualList from 'svelte-tiny-virtual-list';

	let data = $state([
		['A', '1'],
		['B', '2'],
		['C', '3'],
		['D', '4']
	]);

	let data2 = $state(
		Array.from({ length: 10 }, (_, i) => ({
			name: `Item ${i + 1}`,
			value: `Value for item ${i + 1}`
		}))
	);

	let filter = $state('');

	let lines = $derived(data.filter((item) => item[0].toLowerCase().includes(filter.toLowerCase())));

	let bug = $state(true);

	function itemSize(index) {
		return 50;
	}
	let value = $state('item-2');
	let propertyChecked = $state(true);
	let booleanValue = $state('true');
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
<section class="layout-y-low max-w-96 rounded-base bg-surface-100-900 p-low">
	<h3 class="text-sm font-semibold text-surface-500">Toggle comparison</h3>
	<div class="layout-y-low">
		<PropertyType
			label="PropertyType (type=check)"
			type="check"
			name="sandbox-check"
			bind:checked={propertyChecked}
		/>
		<CheckState name="boolean-check" values={['false', 'true']} bind:value={booleanValue}>
			CheckState (boolean)
		</CheckState>
		<div class="text-xs text-surface-500">
			PropertyType checked: {propertyChecked ? 'true' : 'false'} — CheckState value: {booleanValue}
		</div>
	</div>
</section>
<!-- <DateRangePicker></DateRangePicker> -->
{value}
<SegmentedControl
	defaultValue="item-1"
	{value}
	onValueChange={async (event) => {
		await tick();
		value = event.value ?? '';
	}}
>
	<SegmentedControl.Control>
		<SegmentedControl.Indicator />
		<SegmentedControl.Item value="item-1">
			<SegmentedControl.ItemText>Item 1</SegmentedControl.ItemText>
			<SegmentedControl.ItemHiddenInput />
		</SegmentedControl.Item>
		<SegmentedControl.Item value="item-2">
			<SegmentedControl.ItemText>Item 2</SegmentedControl.ItemText>
			<SegmentedControl.ItemHiddenInput />
		</SegmentedControl.Item>
		<SegmentedControl.Item value="item-3">
			<SegmentedControl.ItemText>Item 3</SegmentedControl.ItemText>
			<SegmentedControl.ItemHiddenInput />
		</SegmentedControl.Item>
	</SegmentedControl.Control>
</SegmentedControl>
<TableAutoCard
	definition={[
		{ name: 'Name', key: 'name', class: 'break-all min-w-48' },
		{ name: 'Value', key: 'value', class: 'break-all min-w-48' }
	]}
	data={data2}
	class="max-h-[80vh]"
></TableAutoCard>

<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { DateRangePicker } from 'bits-ui';

	/** @type {{ start?: any, end?: any, live?: boolean }} */
	let { start = $bindable(), end = $bindable(), live = $bindable(false) } = $props();

	/** @type {("start" | "end")[]} */
	let dateTypes = $derived(live ? ['start'] : ['start', 'end']);

	let value = $state({ start, end });

	$effect(() => {
		value = { start, end: live ? start : end };
	});

	$effect(() => {
		if (value?.start && value.start !== start) {
			start = value.start;
		}
		if (!live && value?.end && value.end !== end) {
			end = value.end;
		}
		if (live && end !== start) {
			end = start;
		}
	});
</script>

<DateRangePicker.Root bind:value weekdayFormat="short" weekStartsOn={1} locale="fr-FR">
	<div class="max-w-92 border-common">
		<div
			class="input-text button layout-x-none input-common preset-filled-surface-200-800 light:bg-white"
		>
			<DateRangePicker.Trigger class="-m-1 rounded-base p-1 hover:bg-black/30">
				<Ico size="6" icon="mdi:calendar-range" />
			</DateRangePicker.Trigger>
			{#each dateTypes as type (type)}
				<DateRangePicker.Input {type}>
					{#snippet children({ segments })}
						{#each segments as { part, value }, i (part + i)}
							<div class="inline-block select-none">
								{#if part == 'literal'}
									<DateRangePicker.Segment {part} class="p-0.5">
										{value}
									</DateRangePicker.Segment>
								{:else}
									<DateRangePicker.Segment {part} class="rounded-base px-1 py-1 hover:bg-black/30">
										{value}
									</DateRangePicker.Segment>
								{/if}
							</div>
						{/each}
					{/snippet}
				</DateRangePicker.Input>
				{#if type == 'start'}
					<div aria-hidden="true" class="px-1">⇒</div>
				{/if}
			{/each}
			<button
				class="-m-1 cursor-pointer rounded-base p-1 hover:bg-black/30"
				class:hover:line-through={live}
				class:hover:no-underline={!live}
				class:line-through={!live}
				onclick={() => (live = !live)}>live</button
			>
		</div>
	</div>
	<DateRangePicker.Content sideOffset={6} class="z-50 select-none">
		<DateRangePicker.Calendar
			class="mt-6 inline-block card border border-surface-200-800 preset-filled-surface-200-800 p-4 shadow-xl"
		>
			{#snippet children({ months, weekdays })}
				<DateRangePicker.Header class="layout-x-between">
					<DateRangePicker.PrevButton class="btn-icon preset-filled-primary-500"
						>❮</DateRangePicker.PrevButton
					>
					<DateRangePicker.Heading class="text-lg font-medium" />
					<DateRangePicker.NextButton class="btn-icon preset-filled-primary-500"
						>❯</DateRangePicker.NextButton
					>
				</DateRangePicker.Header>
				<div class="layout-y-stretch pt-4 sm:layout-x-stretch">
					{#each months as month (month.value)}
						<DateRangePicker.Grid class="w-full border-collapse space-y-1 select-none">
							<DateRangePicker.GridHead>
								<DateRangePicker.GridRow class="mb-1 layout-x-between w-full">
									{#each weekdays as day (day)}
										<DateRangePicker.HeadCell
											class="w-10 text-xs font-normal! odd:preset-filled-primary-400-600 even:preset-filled-primary-500"
										>
											<div>{day.slice(0, 2)}</div>
										</DateRangePicker.HeadCell>
									{/each}
								</DateRangePicker.GridRow>
							</DateRangePicker.GridHead>
							<DateRangePicker.GridBody>
								{#each month.weeks as weekDates (weekDates)}
									<DateRangePicker.GridRow class="layout-x-stretch-none w-full">
										{#each weekDates as date (date)}
											<DateRangePicker.Cell
												{date}
												month={month.value}
												class="relative size-10 p-0! text-center text-sm"
											>
												<DateRangePicker.Day
													class="group relative inline-flex size-10 items-center justify-center p-0 text-sm font-normal whitespace-nowrap hover:preset-filled-surface-300-700 data-disabled:pointer-events-none data-disabled:text-surface-600-400 data-highlighted:preset-filled-primary-200-800 data-outside-month:pointer-events-none data-selected:preset-filled-primary-200-800 data-selected:font-medium data-selection-end:rounded-r-base data-selection-end:data-selected:preset-filled-primary-500 data-selection-start:rounded-l-base data-selection-start:data-selected:preset-filled-primary-500 data-unavailable:text-surface-600-400 data-unavailable:line-through"
												>
													<div
														class="absolute top-[4px] hidden size-1 rounded-full bg-primary-500 group-data-selected:bg-surface-50-950 group-data-today:block"
													></div>
													{date.day}
												</DateRangePicker.Day>
											</DateRangePicker.Cell>
										{/each}
									</DateRangePicker.GridRow>
								{/each}
							</DateRangePicker.GridBody>
						</DateRangePicker.Grid>
					{/each}
				</div>
			{/snippet}
		</DateRangePicker.Calendar>
	</DateRangePicker.Content>
</DateRangePicker.Root>

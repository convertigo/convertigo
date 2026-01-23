<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { DateRangePicker } from 'bits-ui';

	/** @type {{ start?: any, end?: any, live?: boolean}} */
	let { start = $bindable(), end = $bindable(), live = $bindable(false) } = $props();

	/** @type {("start" | "end")[]} */
	let dateTypes = $derived(live ? ['start'] : ['start', 'end']);

	let value = $derived.by(() => ({ start, end: live ? start : end }));

	const handleValueChange = (next) => {
		if (!next) return;
		const startChanged = next.start && next.start !== start;
		if (startChanged) {
			start = next.start;
		}
		if (!live) {
			if (next.end && next.end !== end) {
				end = next.end;
			} else if (startChanged && !next.end) {
				end = undefined;
			}
		}
	};

	$effect(() => {
		if (live && end !== start) {
			end = start;
		}
	});
</script>

<DateRangePicker.Root
	{value}
	onValueChange={handleValueChange}
	weekdayFormat="short"
	weekStartsOn={1}
	locale="fr-FR"
>
	<div class="flex h-9 max-w-92 justify-center">
		<div
			class="layout-x-none h-9 input-common items-center preset-filled-surface-200-800 text-[13px] leading-none placeholder:text-[13px] light:bg-white"
		>
			<DateRangePicker.Trigger class="rounded-base p-1 hover:bg-black/10">
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
				class="cursor-pointer rounded-base px-1.5 py-0.5 hover:bg-black/10"
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
						<DateRangePicker.Grid class="w-full select-none">
							<DateRangePicker.GridHead>
								<DateRangePicker.GridRow class="grid w-full grid-cols-7 gap-1">
									{#each weekdays as day (day)}
										<DateRangePicker.HeadCell
											class="flex h-8 w-10 items-center justify-center text-xs font-normal! odd:preset-filled-primary-400-600 even:preset-filled-primary-500"
										>
											<div>{day.slice(0, 2)}</div>
										</DateRangePicker.HeadCell>
									{/each}
								</DateRangePicker.GridRow>
							</DateRangePicker.GridHead>
							<DateRangePicker.GridBody>
								{#each month.weeks as weekDates (weekDates)}
									<DateRangePicker.GridRow class="grid w-full grid-cols-7 gap-1">
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

<script>
	import { DatePicker, Portal, Tooltip } from '@skeletonlabs/skeleton-svelte';
	import Ico from '#lib/utils/Ico.svelte';

	/** @type {{ start?: any, end?: any, live?: boolean}} */
	let { start = $bindable(), end = $bindable(), live = $bindable(false) } = $props();

	// live, the end follows the start: one date is chosen
	let value = $derived(live ? [start].filter(Boolean) : [start, end].filter(Boolean));
	let liveTooltip = $derived(
		live
			? 'Live mode enabled: the end date automatically follows the start date.'
			: 'Enable live mode: keep the end date synchronized with the start date.'
	);

	/** @param {{ value: any[] }} details */
	const handleValueChange = ({ value: next }) => {
		if (!next?.length) return;
		start = next[0];
		if (!live) {
			// a range chosen in the calendar has no end until its second date
			end = next[1];
		}
	};

	$effect(() => {
		if (live && end !== start) {
			end = start;
		}
	});
</script>

<DatePicker
	{value}
	onValueChange={handleValueChange}
	selectionMode={live ? 'single' : 'range'}
	locale="fr-FR"
	startOfWeek={1}
	positioning={{ placement: 'bottom-start', gutter: 6 }}
>
	<div class="flex h-9 max-w-92 justify-center">
		<DatePicker.Control
			class="layout-x-none h-9 input-common items-center text-[13px] leading-none placeholder:text-[13px]"
		>
			<!-- in the flow of the bar: the Skeleton style places it over the end of a single input -->
			<DatePicker.Trigger
				class="static rounded-base p-1 hover:bg-black/10"
				title="Open date range calendar"
				aria-label="Open date range calendar"
			>
				<Ico size="6" icon="mdi:calendar-range" />
			</DatePicker.Trigger>
			<DatePicker.Input
				index={0}
				aria-label="Start date"
				class="w-[6.6rem] rounded-base border-0! bg-transparent! px-1 py-1 text-[13px] shadow-none! ring-0! hover:bg-black/30!"
			/>
			{#if !live}
				<div aria-hidden="true" class="px-1">⇒</div>
				<DatePicker.Input
					index={1}
					aria-label="End date"
					class="w-[6.6rem] rounded-base border-0! bg-transparent! px-1 py-1 text-[13px] shadow-none! ring-0! hover:bg-black/30!"
				/>
			{/if}
			<Tooltip positioning={{ placement: 'top' }}>
				<Tooltip.Trigger>
					{#snippet element(attributes)}
						<button
							{...attributes}
							class="cursor-pointer rounded-base px-1.5 py-0.5 hover:bg-black/10"
							class:hover:line-through={live}
							class:hover:no-underline={!live}
							class:line-through={!live}
							aria-label={liveTooltip}
							onclick={() => (live = !live)}>live</button
						>
					{/snippet}
				</Tooltip.Trigger>
				<Portal>
					<Tooltip.Positioner class="z-[120]" style="z-index: 120;">
						<Tooltip.Content class="card preset-filled-surface-950-50 p-2 text-xs leading-none">
							<span>{liveTooltip}</span>
							<Tooltip.Arrow
								class="[--arrow-background:var(--color-surface-950-50)] [--arrow-size:--spacing(2)]"
							>
								<Tooltip.ArrowTip />
							</Tooltip.Arrow>
						</Tooltip.Content>
					</Tooltip.Positioner>
				</Portal>
			</Tooltip>
		</DatePicker.Control>
	</div>
	<Portal>
		<DatePicker.Positioner class="z-50">
			<DatePicker.Content
				class="inline-block card border border-surface-300-700 preset-filled-surface-50-950 p-4 shadow-xl select-none"
			>
				<DatePicker.View view="day">
					<DatePicker.Context>
						{#snippet children(datePicker)}
							<DatePicker.ViewControl class="layout-x-between">
								<DatePicker.PrevTrigger
									class="btn-icon preset-filled-primary-500"
									title="Previous month"
									aria-label="Previous month">❮</DatePicker.PrevTrigger
								>
								<DatePicker.ViewTrigger class="text-lg font-medium">
									<DatePicker.RangeText />
								</DatePicker.ViewTrigger>
								<DatePicker.NextTrigger
									class="btn-icon preset-filled-primary-500"
									title="Next month"
									aria-label="Next month">❯</DatePicker.NextTrigger
								>
							</DatePicker.ViewControl>
							<DatePicker.Table class="mt-4 w-full border-separate border-spacing-1 select-none">
								<DatePicker.TableHead>
									<DatePicker.TableRow>
										{#each datePicker().weekDays as weekDay (weekDay.long)}
											<DatePicker.TableHeader
												class="h-8 w-10 text-xs font-normal! odd:preset-filled-primary-400-600 even:preset-filled-primary-500"
											>
												{weekDay.short.slice(0, 2)}
											</DatePicker.TableHeader>
										{/each}
									</DatePicker.TableRow>
								</DatePicker.TableHead>
								<DatePicker.TableBody>
									{#each datePicker().weeks as week (week[0].toString())}
										<DatePicker.TableRow>
											{#each week as day (day.toString())}
												<DatePicker.TableCell value={day} class="p-0! text-center">
													<DatePicker.TableCellTrigger
														class="group relative inline-flex size-10 items-center justify-center text-sm whitespace-nowrap hover:preset-filled-surface-300-700 data-disabled:pointer-events-none data-disabled:text-surface-600-400 data-in-range:preset-filled-primary-200-800 data-outside-range:pointer-events-none data-outside-range:opacity-40 data-range-end:rounded-r-base data-range-end:preset-filled-primary-500 data-range-start:rounded-l-base data-range-start:preset-filled-primary-500 data-selected:preset-filled-primary-500 data-selected:font-medium data-unavailable:text-surface-600-400 data-unavailable:line-through"
													>
														<div
															class="absolute top-[4px] hidden size-1 rounded-full bg-primary-500 group-data-selected:bg-surface-50-950 group-data-today:block"
														></div>
														{day.day}
													</DatePicker.TableCellTrigger>
												</DatePicker.TableCell>
											{/each}
										</DatePicker.TableRow>
									{/each}
								</DatePicker.TableBody>
							</DatePicker.Table>
						{/snippet}
					</DatePicker.Context>
				</DatePicker.View>
			</DatePicker.Content>
		</DatePicker.Positioner>
	</Portal>
</DatePicker>

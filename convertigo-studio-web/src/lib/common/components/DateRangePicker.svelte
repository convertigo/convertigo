<script>
	import { getLocalTimeZone, parseDate, toCalendarDate, today } from '@internationalized/date';
	import Ico from '$lib/utils/Ico.svelte';
	import { DateRangePicker } from 'bits-ui';

	/** @type {("start" | "end")[]} */
	const dateTypes = ['start', 'end'];

	const localTimezone = getLocalTimeZone();
	/** @type {any} */
	let { start = $bindable(), end = $bindable() } = $props();
	let value = $derived({ start, end });
</script>

<DateRangePicker.Root
	bind:value
	weekdayFormat="short"
	weekStartsOn={1}
	locale='fr-FR'
	class="max-w-92 border-common preset-filled-surface-100-900"
>
	<div class="input-like input-text flex input-common">
		{#each dateTypes as type (type)}
			<DateRangePicker.Input {type}>
				{#snippet children({ segments })}
					{#each segments as { part, value }, i (part + i)}
						<div class="inline-block select-none">
							{#if part === 'literal'}
								<DateRangePicker.Segment {part} class="p-0.5">
									{value}
								</DateRangePicker.Segment>
							{:else}
								<DateRangePicker.Segment {part} class="rounded-base px-1 py-1 hover:bg-black/10">
									{value}
								</DateRangePicker.Segment>
							{/if}
						</div>
					{/each}
				{/snippet}
			</DateRangePicker.Input>
			{#if type === 'start'}
				<div aria-hidden="true" class="px-1">⇒</div>
			{/if}
		{/each}

		<DateRangePicker.Trigger class="-m-1 ml-auto rounded-base bg-black/5 p-1 hover:bg-black/10">
			<Ico size="6" icon="material-symbols-light:date-range-rounded" />
		</DateRangePicker.Trigger>
	</div>
	<DateRangePicker.Content sideOffset={6} class="z-50 select-none">
		<DateRangePicker.Calendar
			class="mt-6 inline-block card border border-surface-200-800 preset-filled-surface-200-800 p-4 shadow-xl"
		>
			{#snippet children({ months, weekdays })}
				<DateRangePicker.Header class="flex items-center justify-between">
					<DateRangePicker.PrevButton class="btn-icon preset-filled-primary-500">
						❮
					</DateRangePicker.PrevButton>
					<DateRangePicker.Heading class="text-lg font-bold" />
					<DateRangePicker.NextButton class="btn-icon preset-filled-primary-500">
						❯
					</DateRangePicker.NextButton>
				</DateRangePicker.Header>
				<div class="flex flex-col space-y-4 pt-4 sm:flex-row sm:space-y-0 sm:space-x-4">
					{#each months as month (month.value)}
						<DateRangePicker.Grid class="w-full border-collapse space-y-1 select-none">
							<DateRangePicker.GridHead>
								<DateRangePicker.GridRow class="mb-1 flex w-full justify-between">
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
									<DateRangePicker.GridRow class="flex w-full">
										{#each weekDates as date (date)}
											<DateRangePicker.Cell
												{date}
												month={month.value}
												class="relative size-10 p-0! text-center text-sm"
											>
												<DateRangePicker.Day
													class={[
														'group',
														'relative',
														'inline-flex',
														'size-10',
														'items-center',
														'justify-center',
														'p-0',
														'text-sm',
														'font-normal',
														'whitespace-nowrap',
														'hover:preset-filled-surface-300-700',
														'data-highlighted:preset-filled-primary-200-800',
														'data-selected:preset-filled-primary-200-800',
														'data-selection-start:rounded-l-base',
														'data-selection-start:data-selected:preset-filled-primary-500',
														'data-selection-end:rounded-r-base',
														'data-selection-end:data-selected:preset-filled-primary-500',
														'data-disabled:pointer-events-none',
														'data-disabled:text-surface-600-400',
														'data-outside-month:pointer-events-none',
														'data-selected:font-medium',
														'data-unavailable:text-surface-600-400',
														'data-unavailable:line-through'
													]}
												>
													<div
														class={[
															'bg-primary-500',
															'absolute',
															'top-[4px]',
															'hidden',
															'size-1',
															'rounded-full',
															'group-data-selected:bg-surface-50-950',
															'group-data-today:block'
														]}
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

<style lang="postcss">
	@reference '../../../app.css';

	.input-like {
		padding-top: 0.5rem;
		padding-right: 0.75rem;
		padding-bottom: 0.5rem;
		line-height: 1.5rem;
		--tw-shadow: 0 0 #0000;
	}
</style>

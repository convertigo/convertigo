<script>
	import { getLocalTimeZone } from '@internationalized/date';
	import Ico from '$lib/utils/Ico.svelte';
	import { DatePicker, DateRangePicker } from 'bits-ui';

	const localTimezone = getLocalTimeZone();
	/** @type {any} */
	let { start = $bindable(), end = $bindable(), live = $bindable(false) } = $props();

	/** @type {("start" | "end")[]} */
	let dateTypes = $derived(live ? ['start'] : ['start', 'end']);

	let value = $derived(live ? start : { start, end });

	let Api = $derived(live ? DatePicker : DateRangePicker);
</script>

<Api.Root bind:value weekdayFormat="short" weekStartsOn={1} locale="fr-FR">
	<div class="max-w-92 border-common">
		<div class="input-text button flex input-common preset-filled-surface-200-800 light:bg-white">
			<Api.Trigger class="-m-1 rounded-base p-1 hover:bg-black/30">
				<Ico size="6" icon="mdi:calendar-range" />
			</Api.Trigger>
			{#each dateTypes as type (type)}
				<Api.Input {type}>
					{#snippet children({ segments })}
						{#each segments as { part, value }, i (part + i)}
							<div class="inline-block select-none">
								{#if part == 'literal'}
									<Api.Segment {part} class="p-0.5">
										{value}
									</Api.Segment>
								{:else}
									<Api.Segment {part} class="rounded-base px-1 py-1 hover:bg-black/30">
										{value}
									</Api.Segment>
								{/if}
							</div>
						{/each}
					{/snippet}
				</Api.Input>
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
	<Api.Content sideOffset={6} class="z-50 select-none">
		<Api.Calendar
			class="mt-6 inline-block card border border-surface-200-800 preset-filled-surface-200-800 p-4 shadow-xl"
		>
			{#snippet children({ months, weekdays })}
				<Api.Header class="flex items-center justify-between">
					<Api.PrevButton class="btn-icon preset-filled-primary-500">❮</Api.PrevButton>
					<Api.Heading class="text-lg font-bold" />
					<Api.NextButton class="btn-icon preset-filled-primary-500">❯</Api.NextButton>
				</Api.Header>
				<div class="flex flex-col space-y-4 pt-4 sm:flex-row sm:space-y-0 sm:space-x-4">
					{#each months as month (month.value)}
						<Api.Grid class="w-full border-collapse space-y-1 select-none">
							<Api.GridHead>
								<Api.GridRow class="mb-1 flex w-full justify-between">
									{#each weekdays as day (day)}
										<Api.HeadCell
											class="w-10 text-xs font-normal! odd:preset-filled-primary-400-600 even:preset-filled-primary-500"
										>
											<div>{day.slice(0, 2)}</div>
										</Api.HeadCell>
									{/each}
								</Api.GridRow>
							</Api.GridHead>
							<Api.GridBody>
								{#each month.weeks as weekDates (weekDates)}
									<Api.GridRow class="flex w-full">
										{#each weekDates as date (date)}
											<Api.Cell
												{date}
												month={month.value}
												class="relative size-10 p-0! text-center text-sm"
											>
												<Api.Day
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
												</Api.Day>
											</Api.Cell>
										{/each}
									</Api.GridRow>
								{/each}
							</Api.GridBody>
						</Api.Grid>
					{/each}
				</div>
			{/snippet}
		</Api.Calendar>
	</Api.Content>
</Api.Root>

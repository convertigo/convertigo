<script>
	import { ListBox, ListBoxItem } from '@skeletonlabs/skeleton';
	import { compileCronExpression, cronData } from '../stores/cronStore';
	import ResponsiveContainer from './ResponsiveContainer.svelte';
	import Card from './Card.svelte';
	import Icon from '@iconify/svelte';
	import { onDestroy } from 'svelte';

	const months = [
		'January',
		'February',
		'March',
		'April',
		'May',
		'June',
		'July',
		'August',
		'September',
		'October',
		'November',
		'December'
	];
	const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

	let cronExpression = '0 0 0 * * ?';
	const unsubscribe = cronData.subscribe((values) => {
		// Assuming these values are indices, joined as strings in cron format
		cronExpression = compileCronExpression({
			seconds: '0' || '0',
			minutes: values.minutes.join('-') || '0',
			hours: values.hours.join('-') || '0',
			daysOfMonth: values.daysOfMonth.join('-') || '*',
			months: values.months.join('-') || '*',
			daysOfWeek: values.daysOfWeek.join('-') || '?'
		});
	});
	export { cronExpression };

	function updateCronSettings(part, values) {
		if (part && values) {
			cronData.update((current) => {
				current[part] = values;
				return current;
			});
		}
	}
	onDestroy(unsubscribe);
</script>

<Card cardBorder="border-none" title="Configure Cron Schedule" class="shadow-md">
	<div slot="cornerOption">
		<button class="bg-primary-400-500-token">
			Generate Cron
			<Icon icon="ph:gear-six-fill" class="ml-2 " />
		</button>
	</div>
	<ResponsiveContainer
		scrollable={false}
		smCols="sm:grid-cols-1"
		mdCols="md:grid-cols-5"
		lgCols="lg:grid-cols-5"
		class="mt-5"
	>
		<div class="col-span-1">
			<p class="mb-5 font-bold">Minutes</p>
			<ResponsiveContainer
				mdCols="md:grid-cols-1"
				lgCols="lg:grid-cols-1"
				class="h-[20vh]"
				containerGap="gap-0"
			>
				<ListBox multiple>
					{#each Array(60) as _, index (index)}
						<ListBoxItem
							active="gray-button"
							bind:group={$cronData.minutes}
							value={index.toString()}
							name="minutes"
							on:click={() => updateCronSettings('minutes', $cronData.minutes)}
						>
							{index}
						</ListBoxItem>
					{/each}
				</ListBox>
			</ResponsiveContainer>
		</div>
		<div class="col-span-1">
			<p class="mb-5 font-bold">Every hour</p>
			<ResponsiveContainer
				mdCols="md:grid-cols-1"
				lgCols="lg:grid-cols-1"
				class="h-[20vh]"
				containerGap="gap-0"
			>
				<ListBox multiple>
					{#each Array(24) as _, index (index)}
						<ListBoxItem
							active="gray-button"
							bind:group={$cronData.hours}
							name="hours"
							value={index.toString()}
							on:click={() => updateCronSettings('hours', $cronData.hours)}
						>
							{index}
						</ListBoxItem>
					{/each}
				</ListBox>
			</ResponsiveContainer>
		</div>
		<div class="col-span-1">
			<p class="mb-5 font-bold">Days of month</p>
			<ResponsiveContainer
				mdCols="md:grid-cols-1"
				lgCols="lg:grid-cols-1"
				class="h-[20vh]"
				containerGap="gap-0"
			>
				<ListBox multiple>
					{#each Array(31) as _, index (index)}
						<ListBoxItem
							active="gray-button"
							bind:group={$cronData.daysOfMonth}
							value={(index + 1).toString()}
							name="daysOfMonth"
							on:click={() => updateCronSettings('daysOfMonth', $cronData.daysOfMonth)}
						>
							{index}
						</ListBoxItem>
					{/each}
				</ListBox>
			</ResponsiveContainer>
		</div>
		<div class="col-span-1">
			<p class="mb-5 font-bold">Months</p>
			<ResponsiveContainer
				mdCols="md:grid-cols-1"
				lgCols="lg:grid-cols-1"
				class="h-[20vh]"
				containerGap="gap-0"
			>
				<ListBox multiple>
					{#each months as month, index (index)}
						<ListBoxItem
							active="gray-button"
							bind:group={$cronData.months}
							value={(index + 1).toString()}
							name="month"
							on:click={() => updateCronSettings('month', $cronData.months)}
						>
							{month}
						</ListBoxItem>
					{/each}
				</ListBox>
			</ResponsiveContainer>
		</div>
		<div class="col-span-1">
			<p class="mb-5 font-bold">Days of week</p>
			<ResponsiveContainer
				mdCols="md:grid-cols-1"
				lgCols="lg:grid-cols-1"
				class="h-[20vh]"
				containerGap="gap-0"
			>
				<ListBox multiple>
					{#each daysOfWeek as day, index (index)}
						<ListBoxItem
							active="gray-button"
							bind:group={$cronData.daysOfWeek}
							value={(index + 1).toString()}
							name="dayOfWeek"
							on:click={() => updateCronSettings('dayOfWeek', $cronData.daysOfWeek)}
						>
							{day}
						</ListBoxItem>
					{/each}
				</ListBox>
			</ResponsiveContainer>
		</div>
	</ResponsiveContainer>

	<label class="mt-5 font-bold" for={cronExpression}> Current Cron Expression: </label>
	<input class="input-common flex w-40" value={cronExpression} />
</Card>

<!--
 {#each Object.keys($cronStore) as key}
            <div>
                <label for={key}>{key.charAt(0).toUpperCase() + key.slice(1)}:</label>
                <input
                    type="text"
                    class="input-common"
                    id={key}
                    value={$cronStore[key]}
                    on:input={(event) => handleInput(key, event)}
                />
            </div>
        {/each} */

	/**
          {#each Object.keys($cronStore) as key}
		<div>
			<label for={key}>{key.charAt(0).toUpperCase() + key.slice(1)}:</label>
			<input
				type="text"
				class="input-common"
				id={key}
				value={$cronStore[key]}
				on:input={(event) => handleInput(key, event)}
			/>
		</div>
	{/each}
	<p>Current Cron Expression: {cronExpression}</p>
         
-->

<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { Tab, TabGroup, RangeSlider, getModalStore } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import { SlideToggle } from '@skeletonlabs/skeleton';
	import {
		refreshConfigurations,
		configurations,
		updateConfigurations
	} from '$lib/admin/stores/configurationStore';
	import { logs, logsList } from '$lib/admin/stores/logsStore';
	import { onMount } from 'svelte';
	import ResponsiveContainer from '$lib/admin/components/ResponsiveContainer.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { checkArray } from '$lib/utils/service';

	onMount(async () => {
		await refreshConfigurations();
		await logsList();
	});

	let tabSet = 0;
	let value = false;
	let rangeVal = 15;
	let max = 25;

	let logsCategory = null;

	const modalStore = getModalStore();

	// Subscribe to config and extract Logs category .. maybe easier than reuse Prprty comp
	// from configration page
	$: {
		const config = $configurations;
		if (config?.admin?.category) {
			logsCategory = config.admin.category.find((cat) => cat['@_name'] === 'Logs');
		}
	}

	const logsAction = {
		options: {
			name: 'Options',
			icon: 'grommet-icons:add',
			value: 0
		},
		purge: {
			name: 'Purge',
			icon: 'grommet-icons:add',
			value: 1
		},
		logLevels: {
			name: 'Log Levels',
			icon: 'grommet-icons:add',
			value: 2
		},
		help: {
			name: 'Help',
			icon: 'grommet-icons:add',
			value: 3
		}
	};

	const logsOptions = {
		fullscreen: {
			name: 'Fullscreen',
			icon: 'grommet-icons:add'
		},
		realTime: {
			name: 'Real Time',
			icon: 'grommet-icons:add'
		},
		autoScroll: {
			name: 'Auto Scroll',
			icon: 'grommet-icons:add'
		},
		download: {
			name: 'Download',
			icon: 'grommet-icons:add'
		},
		resetOptions: {
			name: 'Reset Options',
			icon: 'grommet-icons:add'
		},
		goToEnd: {
			name: 'Go to End',
			icon: 'grommet-icons:add'
		},
		applyOptions: {
			name: 'Apply Options',
			icon: 'grommet-icons:add'
		}
	};

	const optionsCheckbox = [
		{ id: 'selectedCol', name: 'Selected Column' },
		{ id: 'category', name: 'Category' },
		{ id: 'time', name: 'Time' },
		{ id: 'deltaTime', name: 'Delta Time' },
		{ id: 'level', name: 'Level' },
		{ id: 'thread', name: 'Threads' },
		{ id: 'message', name: 'Message' },
		{ id: 'extra', name: 'Extra' }
	];

	async function handleUpdate(property) {
		await updateConfigurations(property);
	}
</script>

<Card title="Logs">
	<div slot="cornerOption">
		{#if tabSet === 2}
			<ButtonsContainer class="flex">
				<button type="button" class="basic-button">
					<span><Ico icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
					<span>Save changes</span>
				</button>
				<button type="button" class="yellow-button" on:click={refreshConfigurations}>
					<span><Ico icon="material-symbols-light:cancel-outline" class="w-6 h-6" /></span>
					<span class="">Cancel changes</span>
				</button>
			</ButtonsContainer>
		{/if}
	</div>
	<TabGroup>
		{#each Object.entries(logsAction) as [key, { name, icon, value }]}
			<Tab bind:group={tabSet} name={key} {value} active="dark:bg-surface-500 bg-surface-50">
				<svelte:fragment slot="lead">
					<div class="flex items-center gap-2">
						<p>{name}</p>
						<Ico {icon} />
					</div>
				</svelte:fragment>
			</Tab>
		{/each}
		<svelte:fragment slot="panel">
			{#if tabSet === 0}
				<div class="logsCard">
					<ButtonsContainer>
						{#each Object.entries(logsOptions) as [opt, { name, icon }]}
							<button class="basic-button">
								<p>{name}</p>
								<Ico {icon} />
							</button>
						{/each}
					</ButtonsContainer>
					<ButtonsContainer class="mt-5">
						{#each optionsCheckbox as opt}
							<SlideToggle
								active="activeSlideToggle"
								background="unActiveSlideToggle"
								size="sm"
								name="slide"
								class="mr-5"
								bind:checked={value}
							>
								<p class="font-normal">{opt.name}</p>
							</SlideToggle>
						{/each}
					</ButtonsContainer>
				</div>
			{:else if tabSet === 1}
				<div class="logsCard">
					<RangeSlider
						accent="accent-tertiary-500 dark:accent-tertiary-500"
						name="range-slider"
						bind:value={rangeVal}
						max={25}
						step={1}
					>
						<div class="flex justify-between items-center">
							<div class="font-bold">Delete logs files older than 24/05/2024, 13:03:07</div>
							<div class="text-xs">{rangeVal} / {max}</div>
						</div>
					</RangeSlider>
				</div>
			{:else if tabSet === 2}
				<Card>
					{#if logsCategory && checkArray(logsCategory.property)}
						<ResponsiveContainer
							scrollable={false}
							maxHeight="h-auto"
							smCols="sm:grid-cols-1"
							mdCols="md:grid-cols-1"
							lgCols="lg:grid-cols-4"
						>
							{#each logsCategory.property as property}
								{#if property['@_description'] && property['@_description'].startsWith('Log4J')}
									{@html console.log(property['@_description'])}
									<PropertyType {property} />
								{/if}
							{/each}
						</ResponsiveContainer>
					{:else}
						<p>No logs category found or properties are not available.</p>
					{/if}
				</Card>
			{:else if tabSet === 3}
				<div>Tab Panel 4 Contents</div>
			{/if}
		</svelte:fragment>
	</TabGroup>
</Card>

{#if tabSet === 0}
	<Card class="mt-5">
		<TableAutoCard
			definition={[
				{ name: 'Category', custom: true },
				{ name: 'Time', custom: true },
				{ name: 'Thread', custom: true },
				{ name: 'Message', custom: true },
				{ name: 'Extra', custom: true }
			]}
			data={$logs}
			let:def
			let:row
		>
			{#if def.name === 'Category'}
				{row[0]}
			{:else if def.name === 'Time'}
				{row[1]}
			{:else if def.name === 'Thread'}
				{row[3]}
			{:else if def.name === 'Message'}
				{row[4]}
			{:else if def.name === 'Extra'}
				{row[2]}
			{/if}
		</TableAutoCard>
	</Card>
{/if}

<style lang="postcss">
	.logsCard {
		@apply bg-surface-50 dark:bg-surface-700 p-5 rounded-token;
	}
</style>

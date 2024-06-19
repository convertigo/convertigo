<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { Tab, TabGroup, RangeSlider, getModalStore } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';
	import DraggableValue from '$lib/admin/components/DraggableValue.svelte';
	import {
		refreshConfigurations,
		configurations,
		updateConfigurations
	} from '$lib/admin/stores/configurationStore';
	import { logs as allLogs, logsList } from '$lib/admin/stores/logsStore';
	import { onMount } from 'svelte';
	import ResponsiveContainer from '$lib/admin/components/ResponsiveContainer.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import { checkArray } from '$lib/utils/service';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { flip } from 'svelte/animate';
	import MovableContent from '$lib/admin/components/MovableContent.svelte';
	import { derived, writable } from 'svelte/store';
	import { slide } from 'svelte/transition';

	onMount(() => {
		refreshConfigurations();
		logsList();
	});

	let tabSet = 0;
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
			name: 'Viewer',
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

	let columnsOrder = [
		{ name: 'Date', show: true, width: 85 },
		{ name: 'Time', show: true, width: 90 },
		{ name: 'Delta', show: true, width: 45 },
		{ name: 'Level', show: false, width: 50 },
		{ name: 'Category', show: true, width: 110 },
		{ name: 'Thread', show: true, width: 200 },
		{ name: 'user', show: true, width: 100 },
		{ name: 'project', show: true, width: 100 },
		{ name: 'sequence', show: true, width: 100 },
		{ name: 'connector', show: true, width: 100 },
		{ name: 'transaction', show: true, width: 100 },
		{ name: 'contextid', show: true, width: 100 },
		{ name: 'uid', show: true, width: 100 },
		{ name: 'uuid', show: true, width: 100 },
		{ name: 'clientip', show: true, width: 100 },
		{ name: 'clienthostname', show: true, width: 100 }
	];

	const columnsConfiguration = {
		Date: { idx: 1, cls: 'font-bold', fn: (v) => v.split(' ')[0] },
		Time: { idx: 1, cls: 'font-bold', fn: (v) => v.split(' ')[1] },
		Delta: {
			idx: 1,
			fn: (v, i) => {
				const diff =
					// @ts-ignore
					i > 0 ? new Date(v.replace(',', '.')) - new Date($logs[i - 1][1].replace(',', '.')) : 0;
				return diff < 1000
					? diff + 'ms'
					: diff < 3600
						? (diff / 1000).toFixed(2) + 's'
						: new Date(diff).toISOString().substring(11, 19);
			}
		},
		Category: { idx: 0 },
		Level: { idx: 2 },
		Thread: { idx: 3 }
	};

	let extraLines = 1;
	let isDragging = false;
	let virtualList;

	async function itemsUpdated(event) {
		if (event.detail.end >= $logs.length - 1) {
			await logsList();
		}
	}

	function itemSize(index) {
		let height =
			26 + extraLines * 16 + Math.max(16, $logs[index][4].trim().split('\n').length * 16);
		return height;
	}

	function grabFlip(node, elts, options) {
		if (!isDragging) {
			return flip(node, elts, options);
		}
		return {
			delay: 0,
			duration: 0,
			easing: (t) => t,
			css: () => ''
		};
	}

	function getValue(name, log, index) {
		// {@const _value =
		// 						name in columnsConfiguration
		// 							? log[columnsConfiguration[name].idx]
		// 							: log.find((v) => v.startsWith(`${name}=`))?.substring(name.length + 1) ?? ''}
		// 					{@const value = columnsConfiguration[name]?.fn
		// 						? columnsConfiguration[name].fn(_value, index)
		// 						: _value}
		let logValue =
			name in columnsConfiguration
				? log[columnsConfiguration[name].idx]
				: // @ts-ignore
					log.find((v) => v.startsWith(`${name}=`))?.substring(name.length + 1) ?? '';
		return columnsConfiguration[name]?.fn
			? columnsConfiguration[name].fn(logValue, index)
			: logValue;
	}

	let filters = writable({});

	function addFilter(category, value = '', mode, index, not = false) {
		modalStore.trigger({
			type: 'component',
			component: 'modalLogs',
			meta: { category, value, mode, index, filters, not }
		});
	}

	const logs = derived(allLogs, ($logs) => {
		return Object.entries($filters).length == 0
			? $logs
			: $logs.filter((log, index) => {
					return Object.entries($filters).every(([name, array]) => {
						return array.length == 0
							? true
							: array.some(({ mode, value, not }) => {
									let logValue = getValue(name, log, index);
									let ret = mode == 'equals' ? logValue == value : logValue[mode](value);
									return not ? !ret : ret;
								});
					});
				});
	});

	filters.subscribe((f) => {
		$allLogs = $allLogs;
	});
</script>

<Card title="Logs">
	<div slot="cornerOption">
		{#if tabSet === 1}
			<ButtonsContainer class="flex">
				<button type="button" class="basic-button">
					<span><Ico icon="material-symbols-light:save-as-outline" class="w-6 h-6" /></span>
					<span>Purge</span>
				</button>
			</ButtonsContainer>
		{:else if tabSet === 2}
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
			{#if tabSet === 1}
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
					{#if checkArray(logsCategory.property)}
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
	{@const columns = columnsOrder
		.filter((c) => c.show)
		.map((c) => ({
			name: c.name,
			cls: columnsConfiguration[c.name]?.cls ?? '',
			style: `width: ${c.width}px; min-width: ${c.width}px;`
		}))}
	<Card class="mt-2 text-xs">
		<div class="flex flex-col gap-2">
			<div class="row-wrap">
				{#each columnsOrder as conf, index (conf.name)}
					{@const { name, show } = conf}
					<div animate:flip={{ duration: 500 }}>
						<MovableContent bind:items={columnsOrder} {index} grabClass="cursor-grab">
							<div
								class="mini-card"
								class:variant-filled-success={show}
								class:variant-filled-warning={!show}
							>
								<span>{name}</span>
								<span class="cursor-pointer" on:click={() => (conf.show = !show)}
									><Ico icon={show ? 'mdi:eye' : 'mdi:eye-off'} /></span
								>
								<DraggableValue
									class="cursor-col-resize"
									bind:deltaX={conf.width}
									bind:dragging={isDragging}><Ico icon="mdi:resize-horizontal" /></DraggableValue
								>
								<span class="cursor-pointer" on:click={() => addFilter(conf.name)}
									><Ico icon="mdi:filter" /></span
								>
								<span class="cursor-grab"><Ico icon="mdi:dots-vertical" /></span>
							</div>
						</MovableContent>
					</div>
				{/each}
			</div>
			<div class="row-wrap">
				{#each Object.entries($filters).filter(([_, a]) => a.length > 0) as [category, array], idx}
					{#if idx > 0}
						<div class="mini-card variant-ghost" transition:slide={{ axis: 'x' }}>AND</div>
					{/if}
					{#each array as { value, mode, not }, index}
						{#if index > 0}
							<div class="mini-card variant-ghost" transition:slide={{ axis: 'x' }}>OR</div>
						{/if}
						<div class="mini-card variant-filled" transition:slide={{ axis: 'x' }}>
							<span>{category} {not ? 'not' : ''} {mode} {value}</span>
							<span
								class="cursor-pointer"
								on:click={() => addFilter(category, value, mode, index, not)}
								><Ico icon="mdi:edit-outline" /></span
							>
							<span
								class="cursor-pointer"
								on:click={() => {
									array.splice(index, 1);
									$filters = $filters;
								}}><Ico icon="mingcute:delete-line" /></span
							>
						</div>
					{/each}
				{/each}
			</div>
			<div class="relative">
				<div class="absolute left-[-25px] mt-1 p-1 card variant-ghost-primary">
					<span
						class="cursor-pointer"
						on:click={() => {
							extraLines++;
							virtualList.recomputeSizes(0);
						}}><Ico icon="grommet-icons:add" /></span
					>
					{#if extraLines > 1}
						<span
							class="cursor-pointer"
							on:click={() => {
								extraLines--;
								virtualList.recomputeSizes(0);
							}}><Ico icon="grommet-icons:form-subtract" /></span
						>
					{/if}
				</div>
				<div
					class="flex flex-wrap overflow-y-hidden bg-surface-backdrop-token"
					style={`height: ${2 + extraLines * 20}px`}
				>
					{#each columns as { name, cls, style } (name)}
						<div
							{style}
							class={`p-1 ${cls} text-nowrap overflow-hidden max-h-[20px]`}
							animate:grabFlip={{ duration: 500 }}
						>
							<div class="font-semibold">{name}</div>
						</div>
					{/each}
				</div>
			</div>
		</div>

		<VirtualList
			height={800}
			width="auto"
			itemCount={$logs.length}
			{itemSize}
			on:itemsUpdated={itemsUpdated}
			bind:this={virtualList}
		>
			<div slot="item" let:index let:style {style}>
				{@const log = $logs[index]}
				<div class={`${log[2]} rounded`}>
					<div class="flex flex-wrap overflow-y-hidden" style={`height: ${extraLines * 16}px`}>
						{#each columns as { name, cls, style } (name)}
							{@const value = getValue(name, log, index)}
							<div
								{style}
								class={`px-1 ${cls} text-nowrap overflow-hidden`}
								animate:grabFlip={{ duration: 500 }}
								on:click={() => addFilter(name, value)}
							>
								{value}
							</div>
						{/each}
					</div>
					<div
						class={`p-1 whitespace-pre leading-4 font-mono overflow-x-scroll rounded-token variant-ghost`}
						style="scrollbar-width: thin;"
					>
						{log[4]}
					</div>
				</div>
			</div>
		</VirtualList>
	</Card>
{/if}

<style lang="postcss">
	.logsCard {
		@apply bg-surface-50 dark:bg-surface-700 p-5 rounded-token;
	}

	.row-wrap {
		@apply flex flex-wrap rounded-token variant-ghost;
	}

	.mini-card {
		@apply rounded-token m-1 p-1 flex gap-2 text-nowrap;
	}

	.FATAL {
		@apply bg-surface-backdrop-token;
		box-shadow: 2px 2px 5px 0px #404040;
	}

	.ERROR {
		@apply bg-error-backdrop-token;
		box-shadow: 2px 2px 5px 0px #ff3b30;
	}

	.WARN {
		@apply bg-warning-backdrop-token;
		box-shadow: 2px 2px 5px 0px #ff9500;
	}

	.INFO {
		@apply bg-secondary-backdrop-token;
		box-shadow: 2px 2px 5px 0px #71c287;
	}

	.DEBUG {
		@apply bg-primary-backdrop-token;
		box-shadow: 2px 2px 5px 0px #4285f4;
	}

	.TRACE {
		@apply bg-tertiary-backdrop-token;
		box-shadow: 2px 2px 5px 0px #fbbc05;
	}
</style>

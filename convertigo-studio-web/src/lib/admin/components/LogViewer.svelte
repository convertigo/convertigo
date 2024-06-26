<script>
	import Card from '$lib/admin/components/Card.svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import DraggableValue from '$lib/admin/components/DraggableValue.svelte';
	import { logs as allLogs, logsList, realtime, moreResults } from '$lib/admin/stores/logsStore';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { flip } from 'svelte/animate';
	import MovableContent from '$lib/admin/components/MovableContent.svelte';
	import { derived } from 'svelte/store';
	import { slide } from 'svelte/transition';
	import { persisted } from 'svelte-persisted-store';
	import MaxHeight from './MaxHeight.svelte';

	const duration = 400;
	const modalStore = getModalStore();

	const _columnsOrder = [
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

	const columnsOrder = persisted('adminLogsColumnsOrder', _columnsOrder, { syncTabs: false });

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
		Thread: { idx: 3 },
		Message: { idx: 4 }
	};

	let extraLines = 1;
	let isDragging = false;
	let virtualList;
	let pulsedCategory;
	let pulsedCategoryTimeout;
	let showedLines = { start: 0, end: 0 };
	let height = 200;
	let fullscreen = false;

	function doPulse(e) {
		if (e.type == 'click') {
			$showFilters = true;
		}
		clearTimeout(pulsedCategoryTimeout);
		pulsedCategory = e.target.innerText;
		pulsedCategoryTimeout = setTimeout(() => (pulsedCategory = ''), 2000);
	}

	async function itemsUpdated(event) {
		showedLines = event.detail;
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
		let logValue =
			name in columnsConfiguration
				? log[columnsConfiguration[name].idx]
				: // @ts-ignore
					log.find((v) => v.startsWith(`${name}=`))?.substring(name.length + 1) ?? '';
		return columnsConfiguration[name]?.fn
			? columnsConfiguration[name].fn(logValue, index)
			: logValue;
	}

	const filters = persisted('adminLogsFilters', {}, { syncTabs: false });
	const showFilters = persisted('adminLogsShowFilters', false, { syncTabs: false });

	export const filtersFlat = derived(filters, ($filters) => {
		const result = [];
		Object.entries($filters).forEach(([category, array]) => {
			array.forEach((filter, index) => {
				result.push({
					category,
					...filter,
					index
				});
			});
		});
		return result;
	});

	function addFilter(category, value = '', mode, ts = new Date().getTime(), not = false) {
		modalStore.trigger({
			type: 'component',
			component: 'modalLogs',
			meta: { filters, category, value, mode, ts, not }
		});
	}

	function removeFilter(category, index) {
		filters.update(($filters) => {
			$filters[category].splice(index, 1);
			if ($filters[category].length == 0) {
				delete $filters[category];
			}
			return $filters;
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

	$: columns = $columnsOrder
		.filter((c) => c.show)
		.map((c) => ({
			name: c.name,

			cls: columnsConfiguration[c.name]?.cls ?? '',
			style: `width: ${c.width}px; min-width: ${c.width}px;`
		}));
</script>

<div class="text-xs" class:fullscreen>
	<div class="flex flex-col">
		{#if $showFilters}
			<div class="row-wrap mb-2" transition:slide={{ axis: 'y' }}>
				{#each $columnsOrder as conf, index (conf.name)}
					{@const { name, show } = conf}
					<div animate:flip={{ duration }}>
						<MovableContent bind:items={$columnsOrder} {index} grabClass="cursor-grab">
							<div
								class="mini-card"
								class:variant-filled-success={show}
								class:variant-filled-warning={!show}
								class:animate-pulse={name == pulsedCategory}
							>
								<span>{name}</span>
								<span class="cursor-pointer" on:click={() => (conf.show = !show)}
									><Ico icon={show ? 'mdi:eye' : 'mdi:eye-off'} /></span
								>
								<DraggableValue
									class="cursor-col-resize"
									bind:delta={conf.width}
									bind:dragging={isDragging}><Ico icon="mdi:resize-horizontal" /></DraggableValue
								>
								<span class="cursor-cell" on:click={() => addFilter(conf.name)}
									><Ico icon="mdi:filter" /></span
								>
								<span class="cursor-grab"><Ico icon="mdi:dots-vertical" /></span>
							</div>
						</MovableContent>
					</div>
				{/each}
			</div>
		{/if}
		<div class="row-wrap">
			<div class="mini-card variant-filled-surface">
				<span class="cursor-pointer" on:mousedown={() => (fullscreen = !fullscreen)}
					><Ico icon={`mdi:fullscreen${fullscreen ? '-exit' : ''}`} /></span
				>
			</div>
			<div class="mini-card variant-filled-surface">
				<span class="cursor-pointer" on:mousedown={() => ($showFilters = !$showFilters)}
					><Ico icon={`mdi:filter-cog${$showFilters ? '' : '-outline'}`} /></span
				>
			</div>
			<div class="mini-card variant-filled-tertiary">
				<span>Message</span>
				<span
					class="cursor-cell"
					on:mousedown={() => addFilter('Message', window?.getSelection()?.toString() ?? '')}
					><Ico icon="mdi:filter" /></span
				>
			</div>
			{#each $filtersFlat as { category, value, mode, ts, not, index }, idx (ts)}
				<div
					class="flex flex-row"
					animate:flip={{ duration }}
					transition:slide={{ axis: 'x', duration }}
				>
					{#if idx != 0 && index == 0}
						<div class="mini-card variant-ghost">AND</div>
					{/if}
					{#if index > 0}
						<div class="mini-card variant-ghost">OR</div>
					{/if}
					<div class="mini-card variant-filled" class:variant-filled-error={not}>
						<span class="overflow-hidden max-w-xs"
							>{category} {not ? 'not' : ''} {mode} {value}</span
						>
						<span class="cursor-pointer" on:click={() => addFilter(category, value, mode, ts, not)}>
							<Ico icon="mdi:edit-outline" />
						</span>
						<span class="cursor-pointer" on:click={() => removeFilter(category, index)}>
							<Ico icon="mingcute:delete-line" />
						</span>
					</div>
				</div>
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
						animate:grabFlip={{ duration }}
					>
						<div class="font-semibold cursor-help" on:click={doPulse} on:mouseover={doPulse}>
							{name}
						</div>
					</div>
				{/each}
			</div>
		</div>
	</div>
	<MaxHeight bind:height>
		<VirtualList
			{height}
			width="auto"
			itemCount={$logs.length}
			estimatedItemSize={100}
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
								class={`px-1 ${cls} text-nowrap overflow-hidden cursor-cell`}
								animate:grabFlip={{ duration }}
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
	</MaxHeight>
	<div class="p-2 flex gap-4 rounded-token bg-surface-backdrop-token">
		<span
			>Lines {showedLines.start + 1}-{showedLines.end + 1} of {$logs.length}
			{#if !$realtime}({$moreResults ? 'More' : 'No more'} on server){/if}</span
		>
	</div>
</div>

<style lang="postcss">
	.fullscreen {
		position: fixed;
		top: 0px;
		left: 0px;
		height: 100%;
		@apply z-50 bg-surface-active-token min-w-full;
	}

	.row-wrap {
		@apply flex flex-wrap rounded-token variant-ghost;
	}

	.mini-card {
		@apply rounded-token m-1 p-1 flex gap-2 text-nowrap select-none;
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

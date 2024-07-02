<script>
	import { getModalStore, popup } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import DraggableValue from '$lib/admin/components/DraggableValue.svelte';
	import {
		logs as allLogs,
		logsList,
		realtime,
		moreResults,
		calling
	} from '$lib/admin/stores/logsStore';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { flip } from 'svelte/animate';
	import MovableContent from '$lib/admin/components/MovableContent.svelte';
	import { derived } from 'svelte/store';
	import { slide } from 'svelte/transition';
	import { persisted } from 'svelte-persisted-store';
	import MaxHeight from './MaxHeight.svelte';
	import { tick } from 'svelte';
	import { debounce } from '$lib/utils/service';

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

	export let autoScroll = false;
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

	function doAutoScroll() {
		if (autoScroll && $logs.length > 1) {
			founds = [];
			scrollToIndex = $logs.length - 1;
		}
	}

	async function itemsUpdated(event) {
		doAutoScroll();
		showedLines = event.detail;

		if (recenter) {
			recenter();
		}

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

	let scrollToIndex;
	$: if (scrollToIndex >= $logs.length) {
		scrollToIndex = undefined;
	}

	let searched = '';
	let founds = [];
	let foundsIndex = 0;

	function getCenterLine() {
		return Math.round(showedLines.start + (showedLines.end - showedLines.start) / 2);
	}

	function doSearch(e) {
		if (e.key == 'Enter') {
			doSearchNext();
		} else if (searched) {
			const centerLine = getCenterLine();
			const s = searched.toLowerCase();
			let nearest = -1;
			founds = $logs.reduce((acc, log, index) => {
				const l = log[4].toLowerCase();
				let start = l.indexOf(s, 0);
				if (start != -1) {
					if (
						nearest == -1 ||
						Math.abs(centerLine - acc[nearest].index) > Math.abs(centerLine - index)
					) {
						nearest = acc.length;
					}
				}
				while (start != -1) {
					const end = start + s.length;
					acc.push({ index, start, end });
					start = l.indexOf(s, end);
				}
				return acc;
			}, []);
			if (founds.length > 0) {
				scrollToIndex = founds[(foundsIndex = nearest)].index;
			}
		} else {
			founds = [];
		}
	}

	function doSearchNext() {
		if (founds.length > 0) {
			foundsIndex = (foundsIndex + 1) % founds.length;
			scrollToIndex = founds[foundsIndex].index;
		}
	}

	function doSearchPrev() {
		if (founds.length > 0) {
			foundsIndex = (foundsIndex - 1 + founds.length) % founds.length;
			scrollToIndex = founds[foundsIndex].index;
		}
	}

	function scrollIntoView(e) {
		let { left, width } = e.getBoundingClientRect();
		left = Math.max(0, left - (e.parentElement.getBoundingClientRect().width + width) / 2);
		e.parentElement.scrollTo({ left, behavior: 'smooth' });
	}

	let searchBox;
	let searchInput;
	let searchBoxOpened = false;

	const searchBoxSetting = {
		event: 'click',
		target: 'searchBox',
		placement: 'top',
		closeQuery: '.close-popup',
		state: async ({ state }) => {
			searchBoxOpened = state;
			if (state) {
				await tick();
				searchInput.focus();
				autoScroll = false;
			}
		}
	};

	let recenter;

	function addExtraLines(inc) {
		const centerLine = getCenterLine();
		extraLines += inc;
		virtualList.recomputeSizes(0);
		recenter = debounce(() => {
			scrollToIndex = centerLine;
			recenter = undefined;
		}, 333);
	}
</script>

<svelte:window
	on:keydown={(e) => {
		if ((e.ctrlKey || e.metaKey) && (e.key == 'f' || e.key == 'g')) {
			e.preventDefault();
			if (searchBoxOpened) {
				searchInput.select();
				if (e.key == 'g') {
					if (e.shiftKey) {
						doSearchPrev();
					} else {
						doSearchNext();
					}
				}
			} else {
				searchBox.click();
			}
		}
	}}
/>

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
			<div class="mini-card variant-filled-surface">
				<span bind:this={searchBox} class="cursor-pointer" use:popup={searchBoxSetting}
					><Ico icon="mdi:search" /></span
				>
				<div class="z-50" data-popup="searchBox">
					<div
						class="card p-2 bg-stone-200 dark:bg-stone-900 text-black dark:text-white flex flex-row gap-2"
					>
						<input
							type="text"
							class="rounded-md border-none bg-transparent"
							bind:this={searchInput}
							bind:value={searched}
							on:keyup={doSearch}
						/>
						<input
							type="text"
							class="rounded-md border-none bg-transparent"
							style="field-sizing: content;"
							readonly={true}
							value={`${Math.min(foundsIndex + 1, founds.length)}/${founds.length}`}
						/>
						<button class="btn-search" on:click={doSearchPrev}>↑</button>
						<button class="btn-search" on:click={doSearchNext}>↓</button>
						<button class="btn-search close-popup">X</button>
					</div>
					<div class="arrow bg-stone-200 dark:bg-stone-900" />
				</div>
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
						addExtraLines(1);
					}}><Ico icon="grommet-icons:add" /></span
				>
				{#if extraLines > 1}
					<span
						class="cursor-pointer"
						on:click={() => {
							addExtraLines(-1);
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
			{scrollToIndex}
			{itemSize}
			scrollToAlignment="center"
			scrollToBehaviour="smooth"
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
						{#if founds.length > 0}
							{@const _founds = founds.filter((f) => f.index == index)}
							{#if _founds.length > 0}
								{#each _founds as found, index}
									{@const { start, end } = found}
									{#if index == 0}
										{log[4].substring(0, start)}{/if}{#if founds[foundsIndex] == found}<span
											use:scrollIntoView
											class="searchedCurrent">{log[4].substring(start, end)}</span
										>{:else}<span class="searched">{log[4].substring(start, end)}</span
										>{/if}{#if index < _founds.length - 1}{log[4].substring(
											end,
											_founds[index + 1].start
										)}{:else}{log[4].substring(end)}{/if}{/each}{:else}
								{log[4]}
							{/if}
						{:else}
							{log[4]}
						{/if}
					</div>
				</div>
			</div>
		</VirtualList>
	</MaxHeight>
	<div class="flex gap-4 rounded-token bg-surface-backdrop-token justify-between items-center px-4">
		<span class="h-fit"
			>Lines {showedLines.start + 1}-{showedLines.end + 1} of {$logs.length}
			{#if !$realtime}({$moreResults ? 'More' : 'No more'} on server){/if}
			{#if $calling}Calling ...{/if}</span
		>
		<div
			class="mini-card variant-filled"
			class:variant-filled-success={!autoScroll}
			class:variant-filled-warning={autoScroll}
			on:click={() => {
				autoScroll = !autoScroll;
				doAutoScroll();
			}}
		>
			{autoScroll ? 'Disable' : 'Enable'} auto scroll
			<Ico icon={`mdi:download-${autoScroll ? 'off' : 'lock'}-outline`} />
		</div>
	</div>
</div>

<style lang="postcss">
	.fullscreen {
		position: fixed;
		top: 0px;
		left: 0px;
		height: 100%;
		@apply z-50 bg-surface-50-900-token dark:bg-surface-900-50-token min-w-full;
	}

	.row-wrap {
		@apply flex flex-wrap rounded-token variant-ghost;
	}

	.mini-card {
		@apply rounded-token m-1 p-1 flex gap-2 text-nowrap select-none;
	}

	.btn-search {
		@apply btn btn-sm text-black bg-surface-hover-token dark:text-white;
	}

	.searchedCurrent {
		color: black;
		background-color: orange;
		box-shadow: 2px 2px 5px 0px orange;
	}

	.searched {
		color: black;
		background-color: yellow;
		box-shadow: 2px 2px 5px 0px yellow;
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

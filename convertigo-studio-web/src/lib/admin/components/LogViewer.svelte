<script>
	import { Popover, Switch } from '@skeletonlabs/skeleton-svelte';
	import { browser } from '$app/environment';
	import DraggableValue from '$lib/admin/components/DraggableValue.svelte';
	import MovableContent from '$lib/admin/components/MovableContent.svelte';
	import Logs from '$lib/admin/Logs.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { checkArray, debounce } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import { persisted } from 'svelte-persisted-store';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { flip } from 'svelte/animate';
	import { slide } from 'svelte/transition';
	import Button from './Button.svelte';
	import Card from './Card.svelte';
	import MaxRectangle from './MaxRectangle.svelte';

	const duration = 400;

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

	let columnsOrder = $state(_columnsOrder);
	//let columnsOrder = fromStore(persisted('adminLogsColumnsOrder', _columnsOrder, { syncTabs: false }));

	const columnsConfiguration = {
		Date: { idx: 1, cls: 'font-bold', fn: (v) => v.split(' ')[0] },
		Time: { idx: 1, cls: 'font-bold', fn: (v) => v.split(' ')[1] },
		Delta: {
			idx: 1,
			fn: (v, i) => {
				const diff =
					// @ts-ignore
					i > 0 ? new Date(v.replace(',', '.')) - new Date(logs[i - 1][1].replace(',', '.')) : 0;
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

	/** @type {{autoScroll?: boolean}} */
	let { autoScroll = $bindable(false) } = $props();
	let extraLines = $state(1);
	let isDragging = $state(false);
	let virtualList = $state();
	let pulsedCategory = $state();
	let pulsedCategoryTimeout;
	let showedLines = $state({ start: 0, end: 0 });
	let clientHeight = $state(200);
	let fullscreen = $state(false);

	function doPulse(e) {
		if (e.type == 'click') {
			$showFilters = true;
		}
		clearTimeout(pulsedCategoryTimeout);
		pulsedCategory = e.target.innerText;
		pulsedCategoryTimeout = setTimeout(() => (pulsedCategory = ''), 2000);
	}

	function doAutoScroll() {
		if (autoScroll && logs.length > 1) {
			founds = [];
			scrollToIndex = logs.length - 1;
		}
	}

	async function itemsUpdated(event) {
		doAutoScroll();
		showedLines = event.detail;

		if (recenter) {
			recenter();
		}

		if (event.detail.end >= logs.length - 1) {
			await Logs.list();
		}
	}

	function itemSize(index) {
		let height =
			9 +
			scrollbarHeight +
			extraLines * 16 +
			Math.max(16, logs[index][4].trim().split('\n').length * 16);
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
					(log.find((v) => v.startsWith(`${name}=`))?.substring(name.length + 1) ?? '');
		return columnsConfiguration[name]?.fn
			? columnsConfiguration[name].fn(logValue, index)
			: logValue;
	}

	const filters = persisted('adminLogsFilters', {}, { syncTabs: false });
	const showFilters = persisted('adminLogsShowFilters', false, { syncTabs: false });

	const filtersFlat = $derived.by(() => {
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

	function addFilter({
		event,
		category,
		value = '',
		mode = false,
		ts = new Date().getTime(),
		not = false
	}) {
		modalFilterParams = { category, value, mode, ts, not };
		modalFilter.open({ event });
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

	const logs = $derived.by(() => {
		return Object.entries($filters).length == 0
			? Logs.logs
			: Logs.logs.filter((log, index) => {
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

	let columns = $derived(
		columnsOrder
			.filter((c) => c.show)
			.map((c) => ({
				name: c.name,

				cls: columnsConfiguration[c.name]?.cls ?? '',
				style: `width: ${c.width}px; min-width: ${c.width}px;`
			}))
	);

	let scrollToIndex = $state();
	$effect(() => {
		if (scrollToIndex >= logs.length) {
			scrollToIndex = undefined;
		}
	});

	let searched = $state('');
	let founds = $state([]);
	let foundsIndex = $state(0);

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
			founds = logs.reduce((acc, log, index) => {
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

	let searchInput = $state();
	let searchBoxOpened = $state(false);

	// const searchBoxSetting = {
	// 	event: 'click',
	// 	target: 'searchBox',
	// 	placement: 'top',
	// 	closeQuery: '.close-popup',
	// 	state: async ({ state }) => {
	// 		searchBoxOpened = state;
	// 		if (state) {
	// 			await tick();
	// 			searchInput.focus();
	// 			autoScroll = false;
	// 		}
	// 	}
	// };

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

	let modalFilter;
	/** @type {any} */
	let modalFilterParams = $state({});
	let modalFilterSubmit = (e) => {
		e.preventDefault();
		const { mode, category, value, not, ts } = modalFilterParams;
		filters.update((f) => {
			let array = checkArray(f[category]);
			const val = {
				mode: e.submitter.value,
				value,
				not,
				ts
			};
			if (mode) {
				array[array.findIndex((o) => o.ts == ts)] = val;
			} else {
				array.push(val);
			}
			f[category] = array;
			return f;
		});
		modalFilter.close();
	};
	let scrollbarHeight = $state(0);
	onMount(() => {
		const container = document.createElement('div');
		container.style.visibility = 'hidden';
		container.style.overflow = 'scroll';
		container.style.width = '100px';
		container.style.height = '100px';
		container.style.position = 'absolute';

		const inner = document.createElement('div');
		inner.style.width = '100%';
		inner.style.height = '100%';

		container.appendChild(inner);
		document.body.appendChild(container);

		scrollbarHeight = Math.max(container.offsetHeight - inner.clientHeight, 4);
		document.body.removeChild(container);
	});
	const size = '4';
</script>

<svelte:window
	onkeydown={(e) => {
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
				searchBoxOpened = true;
			}
		}
	}}
/>
<ModalDynamic bind:this={modalFilter}>
	{@const { mode, category, value, not } = modalFilterParams}
	<Card title="{mode ? 'Edit' : 'Add'} log filter for {category}">
		<form onsubmit={modalFilterSubmit} class="flex flex-col gap-2">
			{#if category == 'Message'}
				<textarea
					class="textarea overflow-auto"
					bind:value={modalFilterParams.value}
					wrap={null}
					rows={Math.min(10, value.split('\n').length)}
				></textarea>
			{:else}
				<input class="input" bind:value={modalFilterParams.value} />
			{/if}
			<Switch
				name="negate"
				checked={modalFilterParams.not}
				onCheckedChange={(e) => (modalFilterParams.not = e.checked)}
				controlActive="bg-error-400 dark:bg-error-700">{not ? 'not' : 'is'}</Switch
			>
			<div class="flex flex-wrap gap-2">
				{#each ['startsWith', 'equals', 'includes', 'endsWith'] as _mode}
					<button
						type="submit"
						class="btn"
						class:preset-filled-primary-500={mode != _mode}
						class:preset-filled-success-500={mode == _mode}
						value={_mode}
					>
						{_mode}
					</button>
				{/each}
			</div>
		</form>
	</Card>
</ModalDynamic>
<div class="layout-y-stretch-low h-full w-full text-xs" class:fullscreen>
	<div class="layout-y-stretch-low">
		{#if $showFilters}
			<div
				class="mx-low layout-x-low flex-wrap rounded-sm preset-filled-surface-200-800 p-1"
				transition:slide={{ axis: 'y' }}
			>
				{#each columnsOrder as conf, index (conf.name)}
					{@const { name, show } = conf}
					<div animate:flip={{ duration }}>
						<MovableContent bind:items={columnsOrder} {index} grabClass="cursor-grab">
							<div
								class="mini-card"
								class:preset-filled-success-500={show}
								class:preset-filled-warning-500={!show}
								class:animate-pulse={name == pulsedCategory}
							>
								<span>{name}</span>
								<Button
									{size}
									icon={show ? 'mdi:eye' : 'mdi:eye-off'}
									onclick={() => (conf.show = !show)}
								/>
								<DraggableValue
									class="cursor-col-resize"
									bind:delta={conf.width}
									bind:dragging={isDragging}><Ico icon="mdi:resize-horizontal" /></DraggableValue
								>
								<Button
									{size}
									icon="mdi:filter"
									class="cursor-cell"
									onclick={(event) => addFilter({ event, category: name })}
								/>
								<Button {size} icon="mdi:dots-vertical" class="cursor-grab" />
							</div>
						</MovableContent>
					</div>
				{/each}
			</div>
		{/if}
		<div class="mx-low layout-x-low flex-wrap rounded-sm preset-filled-surface-200-800 p-1">
			<div class="mini-card preset-filled-primary-500">
				<Button
					{size}
					icon="mdi:fullscreen{!browser && fullscreen ? '-exit' : ''}"
					onmousedown={() => (fullscreen = !fullscreen)}
				/>
			</div>
			<div class="mini-card preset-filled-primary-500">
				<Button
					{size}
					icon="mdi:filter-cog{!browser && $showFilters ? '' : '-outline'}"
					onmousedown={() => ($showFilters = !$showFilters)}
				/>
			</div>
			<div class="mini-card preset-filled-primary-500">
				<Popover
					open={searchBoxOpened}
					onInteractOutside={() => (searchBoxOpened = false)}
					arrow
					arrowBackground="bg-surface-50-950"
					triggerBase="block"
					positioning={{ placement: fullscreen ? 'bottom-start' : 'top-start' }}
				>
					{#snippet trigger()}<Ico icon="mdi:search" />{/snippet}
					{#snippet content()}
						<Card bg="bg-surface-50-950 text-black dark:text-white" class="p-low!">
							<div class="layout-x-stretch-low">
								<input
									type="text"
									class="rounded-md border-none bg-transparent"
									bind:this={searchInput}
									bind:value={searched}
									onkeyup={doSearch}
								/>
								<input
									type="text"
									class="rounded-md border-none bg-transparent"
									style="field-sizing: content;"
									readonly={true}
									value="{Math.min(foundsIndex + 1, founds.length)}/{founds.length}"
								/>
								<button class="basic-button" onclick={doSearchPrev}>↑</button>
								<button class="basic-button" onclick={doSearchNext}>↓</button>
							</div>
						</Card>
					{/snippet}
				</Popover>
			</div>
			<div class="mini-card preset-filled-tertiary-500">
				<span>Message</span>
				<Button
					{size}
					icon="mdi:filter"
					class="cursor-cell"
					onclick={(event) =>
						addFilter({
							event,
							category: 'Message',
							value: window?.getSelection()?.toString() ?? ''
						})}
				/>
			</div>
			{#each filtersFlat as { category, value, mode, ts, not, index }, idx (ts)}
				<div
					class="flex flex-row"
					animate:flip={{ duration }}
					transition:slide={{ axis: 'x', duration }}
				>
					{#if idx != 0 && index == 0}
						<div class="preset-ghost-500 mini-card">AND</div>
					{/if}
					{#if index > 0}
						<div class="preset-ghost-500 mini-card">OR</div>
					{/if}
					<div
						class="mini-card"
						class:preset-filled-surface-500={!not}
						class:preset-filled-error-500={not}
					>
						<span class="max-w-xs overflow-hidden"
							>{category} {not ? 'not' : ''} {mode} {value}</span
						>
						<Button
							{size}
							icon="mdi:edit-outline"
							class="w-fit!"
							onclick={(event) => addFilter({ event, category, value, mode, ts, not })}
						/>
						<Button
							{size}
							icon="mingcute:delete-line"
							class="w-fit!"
							onclick={() => removeFilter(category, index)}
						/>
					</div>
				</div>
			{/each}
		</div>
		<div class="relative">
			<div class="absolute left-[-25px] layout-y-low rounded-sm bg-primary-500 p-1">
				<Button {size} icon="grommet-icons:add" onclick={() => addExtraLines(1)} />
				{#if extraLines > 0}
					<Button {size} icon="grommet-icons:form-subtract" onclick={() => addExtraLines(-1)} />
				{/if}
			</div>
			<div
				class="flex flex-wrap overflow-y-hidden rounded-sm rounded-b-none bg-surface-200-800"
				style="height: {2 + extraLines * 20}px"
			>
				{#each columns as { name, cls, style } (name)}
					<div
						{style}
						class="p-1 {cls} max-h-[20px] overflow-hidden text-nowrap"
						animate:grabFlip={{ duration }}
					>
						<button class="cursor-help font-semibold" onclick={doPulse} onmouseover={doPulse}>
							{name}
						</button>
					</div>
				{/each}
			</div>
		</div>
	</div>
	<div class="grow">
		<MaxRectangle bind:clientHeight delay={300}>
			<VirtualList
				height={clientHeight}
				width="auto"
				itemCount={logs.length}
				estimatedItemSize={100}
				{scrollToIndex}
				{itemSize}
				scrollToAlignment="center"
				scrollToBehaviour="smooth"
				on:itemsUpdated={itemsUpdated}
				bind:this={virtualList}
			>
				<div slot="item" let:index let:style {style}>
					{@const log = logs[index]}
					<div class="{log[2]} rounded-sm">
						<div class="flex flex-wrap overflow-y-hidden" style="height: {extraLines * 16}px">
							{#each columns as { name, cls, style } (name)}
								{@const value = getValue(name, log, index)}
								<button
									{style}
									class="px-1 {cls} cursor-cell overflow-hidden text-left text-nowrap"
									animate:grabFlip={{ duration }}
									onclick={(event) => addFilter({ event, category: name, value })}
								>
									{value}
								</button>
							{/each}
						</div>
						<div
							class="border-b-none overflow-x-scroll rounded-sm preset-outlined p-1 font-mono leading-4 whitespace-pre"
							style="scrollbar-width: thin; --tw-ring-opacity: 0.3;"
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
		</MaxRectangle>
	</div>
	<div
		class="layout-x-p-none items-center justify-between rounded-sm rounded-t-none preset-filled-surface-200-800 py-1! px!"
	>
		<span class="h-fit"
			>Lines {showedLines.start + 1}-{showedLines.end + 1} of {logs.length}
			{#if !Logs.realtime}({Logs.moreResults ? 'More' : 'No more'} on server){/if}
			{#if Logs.calling}Calling ...{/if}</span
		>
		<button
			class="mini-card"
			class:preset-filled-success-500={autoScroll}
			class:preset-filled-warning-500={!autoScroll}
			onclick={() => {
				autoScroll = !autoScroll;
				doAutoScroll();
			}}
		>
			{autoScroll ? 'Enabled' : 'Disabled'} auto scroll
			<Ico icon="mdi:download-{autoScroll ? 'lock' : 'off'}-outline" />
		</button>
	</div>
</div>

<style>
	@reference "../../../app.css";

	:global([data-part='arrow-tip']:is(.dark *)) {
		--arrow-background: rgb(var(--color-surface-950));
	}

	:global([data-part='arrow-tip']) {
		--arrow-background: rgb(var(--color-surface-50));
	}

	.fullscreen {
		position: fixed;
		top: 0px;
		left: 0px;
		height: 100%;
		@apply z-50 min-w-full bg-surface-950-50;
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
		@apply bg-surface-800-200;
		box-shadow: 2px 2px 5px 0px rgb(var(--color-surface-500) / 0.6);
	}

	.ERROR {
		@apply bg-error-800-200;
		box-shadow: 2px 2px 5px 0px rgb(var(--color-error-500) / 0.6);
	}

	.WARN {
		@apply bg-warning-800-200;
		box-shadow: 2px 2px 5px 0px rgb(var(--color-warning-500) / 0.6);
	}

	.INFO {
		@apply bg-secondary-800-200;
		box-shadow: 2px 2px 5px 0px rgb(var(--color-secondary-500) / 0.6);
	}

	.DEBUG {
		@apply bg-primary-800-200;
		box-shadow: 2px 2px 5px 0px rgb(var(--color-primary-500) / 0.6);
	}

	.TRACE {
		@apply bg-tertiary-800-200;
		box-shadow: 2px 2px 5px 0px rgb(var(--color-tertiary-500) / 0.6);
	}
</style>

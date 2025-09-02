<script>
	import { Popover } from '@skeletonlabs/skeleton-svelte';
	import { browser } from '$app/environment';
	import DraggableValue from '$lib/admin/components/DraggableValue.svelte';
	import MovableContent from '$lib/admin/components/MovableContent.svelte';
	import Logs from '$lib/admin/Logs.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { checkArray, debounce } from '$lib/utils/service';
	import { tick } from 'svelte';
	import { persisted } from 'svelte-persisted-store';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { flip } from 'svelte/animate';
	import { slide } from 'svelte/transition';
	import Button from './Button.svelte';
	import Card from './Card.svelte';
	import MaxRectangle from './MaxRectangle.svelte';
	import PropertyType from './PropertyType.svelte';

	const duration = 400;
	const lineHeight = 14.1; // px
	const headerHeight = 16; // px

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
		Date: { idx: 1, cls: '', fn: (v) => v.split(' ')[0] }, //'font-bold'
		Time: { idx: 1, cls: '', fn: (v) => v.split(' ')[1] }, //'font-bold'
		Delta: {
			idx: 1,
			fn: (v, i) => {
				const diff =
					// @ts-ignore
					i > 0 ? new Date(v.replace(',', '.')) - new Date(logs[i - 1][1].replace(',', '.')) : 0;
				return diff < 1000
					? diff + 'ms'
					: diff < 3600000
						? (diff / 1000).toFixed(2) + 's'
						: new Date(diff).toISOString().substring(11, 19);
			}
		},
		Category: { idx: 0 },
		Level: { idx: 2 },
		Thread: { idx: 3 },
		Message: { idx: 4 }
	};

	/** @type {{autoScroll?: boolean, filters?: any, serverFilter?: string, startDate?: string, endDate?: string, live?: boolean}} */
	let {
		autoScroll = $bindable(false),
		filters = $bindable({}),
		serverFilter = $bindable(''),
		startDate = $bindable(''),
		endDate = $bindable(''),
		live = $bindable(false)
	} = $props();
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

	async function doAutoScroll() {
		if (autoScroll && logs.length > 1) {
			founds = [];
			_scrollToIndex = undefined;
			await tick();
			_scrollToIndex = logs.length - 1;
			if (showedLines.end == _scrollToIndex && !Logs.moreResults && !live && !Logs.calling) {
				autoScroll = false;
			}
		}
	}

	async function itemsUpdated({ detail }) {
		showedLines = detail;

		if (recenter) {
			recenter();
		} else {
			if (detail.end >= logs.length - 1 && !Logs.calling) {
				await list();
			}
			await doAutoScroll();
		}
	}

	async function afterScroll() {}

	function itemSize(index) {
		let height =
			4 +
			extraLines * headerHeight +
			Math.max(lineHeight, logs[index][logs[index].length - 1] * lineHeight);
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
					(log.find((v) => v.startsWith?.(`${name}=`))?.substring(name.length + 1) ?? '');
		return columnsConfiguration[name]?.fn
			? columnsConfiguration[name].fn(logValue, index)
			: logValue;
	}

	const showFilters = persisted('adminLogsShowFilters', false, { syncTabs: false });

	const filtersFlat = $derived.by(() => {
		const result = [];
		Object.entries(filters).forEach(([category, array]) => {
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
		not = false,
		sensitive = false
	}) {
		modalFilterParams = { category, value, mode, ts, not, sensitive };
		modalFilter.open({ event });
	}

	function removeFilter(category, index) {
		filters[category].splice(index, 1);
		if (filters[category].length == 0) {
			delete filters[category];
		}
	}

	const logs = $derived.by(() => {
		return Object.entries(filters).length == 0
			? Logs.logs
			: Logs.logs.filter((log, index) => {
					return Object.entries(filters).every(([name, array]) => {
						return array.length == 0
							? true
							: array.some(({ mode, value, not, sensitive }) => {
									let logValue = getValue(name, log, index);
									if (!sensitive) {
										logValue = logValue.toLowerCase();
										value = value.toLowerCase();
									}
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

	let _scrollToIndex = $state();
	let scrollToIndex = $derived.by(() => {
		if (_scrollToIndex == undefined || _scrollToIndex < 0 || _scrollToIndex >= logs.length) {
			return undefined;
		}
		return _scrollToIndex;
	});

	let searched = $state('');
	let founds = $state.raw([]);
	let foundsIndex = $state(0);

	function getCenterLine() {
		return Math.round(showedLines.start + (showedLines.end - showedLines.start) / 2);
	}

	let searchAbortController;

	let asyncSearch = debounce(async (s, centerLine) => {
		const batchSize = 100;
		let acc = [];
		let nearest = -1;

		for (let i = 0; i < logs.length; i += batchSize) {
			if (searchAbortController?.aborted) return;

			const batch = logs.slice(i, i + batchSize);
			for (let j = 0; j < batch.length; j++) {
				const index = i + j;
				const l = batch[j][4].toLowerCase();
				let start = l.indexOf(s, 0);

				if (start !== -1) {
					if (
						nearest === -1 ||
						Math.abs(centerLine - acc[nearest].index) > Math.abs(centerLine - index)
					) {
						nearest = acc.length;
					}
				}

				while (start !== -1) {
					const end = start + s.length;
					acc.push({ index, start, end });
					start = l.indexOf(s, end);
				}
			}
			founds = acc;
			if (founds.length > 0) {
				_scrollToIndex = founds[(foundsIndex = nearest)].index;
			}
			await tick();
		}
	}, 200);

	function doSearch(e) {
		if (e?.key === 'Enter') {
			doSearchNext();
		} else if (searched) {
			if (searchAbortController) searchAbortController.abort();
			searchAbortController = new AbortController();

			const s = searched.toLowerCase();
			const centerLine = getCenterLine();
			asyncSearch(s, centerLine);
		} else {
			founds = [];
		}
	}

	function doSearchNext() {
		if (founds.length > 0) {
			foundsIndex = (foundsIndex + 1) % founds.length;
			_scrollToIndex = founds[foundsIndex].index;
		}
	}

	function doSearchPrev() {
		if (founds.length > 0) {
			foundsIndex = (foundsIndex - 1 + founds.length) % founds.length;
			_scrollToIndex = founds[foundsIndex].index;
		}
	}

	function doSearchClear() {
		searched = '';
		doSearch();
	}

	function scrollIntoView(e) {
		let { left, width } = e.getBoundingClientRect();
		left = Math.max(0, left - (e.parentElement.getBoundingClientRect().width + width) / 2);
		e.parentElement.scrollTo({ left, behavior: 'smooth' });
	}

	let searchInput = $state();
	let searchBoxOpened = $state(false);

	let recenter;

	function addExtraLines(inc) {
		const centerLine = getCenterLine();
		extraLines += inc;
		virtualList.recomputeSizes(0);
		recenter = debounce(async () => {
			_scrollToIndex = undefined;
			recenter = undefined;
			await tick();
			_scrollToIndex = centerLine;
		}, 333);
	}

	let modalFilter;
	/** @type {any} */
	let modalFilterParams = $state({});
	let modalFilterSubmit = (e) => {
		e.preventDefault();
		const { mode, category, value, not, ts, sensitive } = modalFilterParams;
		let array = checkArray(filters[category]);
		const val = {
			mode: e.submitter.value,
			value,
			not,
			ts,
			sensitive
		};
		if (mode) {
			array[array.findIndex((o) => o.ts == ts)] = val;
		} else {
			array.push(val);
		}
		filters[category] = array;
		modalFilter.close();
	};
	const size = '4';

	function dragscroll(node) {
		let lastClickTime = 0;
		let startX = 0;
		let scrollLeft = 0;
		let isDown = false;
		let isDragging = false;
		let allowDrag = false;

		function setDraggingState(active) {
			document.body.classList.toggle('select-none', active);
		}

		function onMouseDown(e) {
			const now = Date.now();

			if (now - lastClickTime < 400) {
				allowDrag = true;
				startX = e.pageX - node.offsetLeft;
				scrollLeft = node.scrollLeft;
				node.style.cursor = 'grabbing';
				isDown = true;
				isDragging = false;
			} else {
				allowDrag = false;
				lastClickTime = now;
			}
		}

		function onMouseMove(e) {
			if (!isDown || !allowDrag) return;

			const x = e.pageX - node.offsetLeft;
			const rawDistance = x - startX;

			if (Math.abs(rawDistance) > 5) {
				isDragging = true;
				setDraggingState(true);
			}

			if (isDragging) {
				const speedFactor = Math.pow(Math.abs(rawDistance) / 10, 1.3);
				const direction = rawDistance < 0 ? -1 : 1;
				const adjustedWalk = direction * speedFactor * 10;
				node.scrollLeft = scrollLeft - adjustedWalk;
				e.preventDefault();
			}
		}

		function stop() {
			isDown = false;
			node.style.cursor = 'default';
			setDraggingState(false);
		}

		node.addEventListener('mousedown', onMouseDown);
		node.addEventListener('mousemove', onMouseMove);
		node.addEventListener('mouseup', stop);
		node.addEventListener('mouseleave', stop);

		return () => {
			node.removeEventListener('mousedown', onMouseDown);
			node.removeEventListener('mousemove', onMouseMove);
			node.removeEventListener('mouseup', stop);
			node.removeEventListener('mouseleave', stop);
			document.body.classList.remove('select-none');
		};
	}

	export async function list(renew = false) {
		Logs.live = live;
		Logs.autoScroll = autoScroll;
		Logs.filter = serverFilter;
		Logs.startDate = startDate;
		Logs.endDate = endDate;
		if (renew) {
			_scrollToIndex = undefined;
		}
		let len = renew ? 0 : logs.length;
		await Logs.list(renew);
		if (logs.length == len && (live || Logs.moreResults)) {
			await list(false);
		}
	}
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
	{@const { mode, category, value, not, sensitive } = modalFilterParams}
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
			<div class="flex flex-wrap justify-between gap">
				<PropertyType
					name="negate"
					type="check"
					label={not ? 'not' : 'is'}
					fit={true}
					bind:checked={() => !modalFilterParams.not, (v) => (modalFilterParams.not = !v)}
				/>
				<PropertyType
					name="case"
					type="check"
					label={sensitive ? 'case' : 'ignore case'}
					fit={true}
					bind:checked={() => modalFilterParams.sensitive, (v) => (modalFilterParams.sensitive = v)}
				/>
			</div>
			<div class="flex flex-wrap gap-2">
				{#each ['startsWith', 'equals', 'includes', 'endsWith'] as _mode}
					<button
						type="submit"
						class="btn"
						class:preset-filled-primary-100-900={mode != _mode}
						class:motif-primary={mode != _mode}
						class:preset-filled-success-100-900={mode == _mode}
						value={_mode}
					>
						{_mode}
					</button>
				{/each}
			</div>
		</form>
	</Card>
</ModalDynamic>
<div class="layout-y-stretch-none h-full w-full text-xs" class:fullscreen>
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
								class:preset-filled-success-100-900={show}
								class:preset-filled-warning-100-900={!show}
								class:motif-warning={!show}
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
			<div class="mini-card preset-filled-primary-100-900">
				<Button
					{size}
					icon="mdi:fullscreen{!browser && fullscreen ? '-exit' : ''}"
					onmousedown={() => (fullscreen = !fullscreen)}
				/>
			</div>
			<div class="mini-card preset-filled-success-100-900">
				<Popover
					open={searchBoxOpened}
					onOpenChange={(e) => {
						searchBoxOpened = e.open;
						if (!e.open) {
							searched = '';
							doSearch();
						}
					}}
					arrow
					arrowBackground="bg-surface-50-950"
					triggerBase="block"
					positioning={{ placement: fullscreen ? 'bottom-start' : 'top-start' }}
					zIndex="51"
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
								<button class="button-primary" onclick={doSearchPrev}>↑</button>
								<button class="button-primary" onclick={doSearchNext}>↓</button>
								<button class="button-error p-2" onclick={doSearchClear}
									><Ico icon="mingcute:delete-line" /></button
								>
							</div>
						</Card>
					{/snippet}
				</Popover>
			</div>
			<div
				class={{
					'mini-card': true,
					'preset-filled-secondary-100-900': !$showFilters,
					'preset-filled-warning-200-800': $showFilters,
					'motif-secondary': !$showFilters,
					'motif-warning': $showFilters
				}}
			>
				<Button
					{size}
					icon="mdi:filter-cog{$showFilters ? '' : '-outline'}"
					onmousedown={() => ($showFilters = !$showFilters)}
				/>
			</div>
			{#if $showFilters}
				<div class="mini-card preset-filled-secondary-100-900">
					<Button {size} icon="grommet-icons:add" onclick={() => addExtraLines(1)} />
					{#if extraLines > 0}
						<Button {size} icon="grommet-icons:form-subtract" onclick={() => addExtraLines(-1)} />
					{/if}
				</div>
			{/if}
			<div class="mini-card preset-filled-tertiary-100-900 motif-tertiary">
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
			{#each filtersFlat as { category, value, mode, ts, not, sensitive, index }, idx (ts)}
				<div
					class="flex flex-row"
					animate:flip={{ duration }}
					transition:slide={{ axis: 'x', duration }}
				>
					{#if idx != 0 && index == 0}
						<div class="mini-card preset-filled-surface-200-800">AND</div>
					{/if}
					{#if index > 0}
						<div class="mini-card preset-filled-surface-200-800">OR</div>
					{/if}
					<div
						class="mini-card"
						class:preset-filled-secondary-100-900={!not}
						class:motif-secondary={!not}
						class:preset-filled-error-100-900={not}
						class:motif-error={not}
					>
						<span class="max-w-xs overflow-hidden"
							>{category} {not ? 'not' : ''} {mode} {sensitive ? value : value.toLowerCase()}</span
						>
						<Button
							{size}
							icon="mdi:edit-outline"
							class="w-fit!"
							onclick={(event) => addFilter({ event, category, value, mode, ts, not, sensitive })}
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
		<div
			class="flex flex-wrap overflow-y-hidden rounded-sm rounded-b-none bg-surface-200-800"
			style="height: {2 + extraLines * 18}px"
		>
			{#each columns as { name, cls, style } (name)}
				<div
					{style}
					class="px-1 py-0 {cls} max-h-[18px] overflow-hidden text-nowrap"
					animate:grabFlip={{ duration }}
				>
					<button class="cursor-help font-semibold" onclick={doPulse} onmouseover={doPulse}>
						{name}
					</button>
				</div>
			{/each}
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
				scrollToBehaviour="instant"
				on:itemsUpdated={itemsUpdated}
				on:afterScroll={afterScroll}
				bind:this={virtualList}
			>
				{#snippet item({ style, index })}
					{@const log = logs[index]}
					<div {style}>
						<div class="{log[2]} rounded-sm" class:odd={index % 2 == 0} class:even={index % 2 == 1}>
							<div
								class={[
									'flex',
									'flex-wrap',
									'overflow-y-hidden',
									'items-baseline',
									'opacity-90',
									log[log.length - 1] > 1 && 'sticky'
								]}
								style="height: {extraLines * headerHeight}px"
							>
								{#each columns as { name, cls, style } (name)}
									{@const value = getValue(name, log, index)}
									<button
										{style}
										class="px-1 {cls} cursor-cell overflow-hidden pt-[3px] text-left leading-none font-semibold text-nowrap"
										animate:grabFlip={{ duration }}
										onclick={(event) => addFilter({ event, category: name, value })}
									>
										{value}
									</button>
								{/each}
							</div>
							<div
								class="overflow-x-scroll rounded-sm p-1 font-mono leading-4 whitespace-pre text-black dark:text-white"
								style="scrollbar-width: none; --tw-ring-opacity: 0.3;"
								{@attach dragscroll}
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
				{/snippet}
			</VirtualList>
		</MaxRectangle>
	</div>
	<div
		class="layout-x-p-none items-center justify-between rounded-sm rounded-t-none preset-filled-surface-200-800 py-1! px!"
	>
		<span class="h-fit"
			>Lines {showedLines.start + 1}-{showedLines.end + 1} of {logs.length}
			{#if Object.entries(filters).length > 0}[{Logs.logs.length} w/o filter]{/if}
			{#if !Logs.live}({Logs.moreResults ? 'More' : 'No more'} on server){/if}
			{#if Logs.calling}Calling…{/if}</span
		>
		<button
			class="mini-card"
			class:preset-filled-success-100-900={autoScroll}
			class:preset-filled-warning-100-900={!autoScroll}
			class:motif-warning={!autoScroll}
			onclick={async () => {
				_scrollToIndex = undefined;
				autoScroll = !autoScroll;
				await doAutoScroll();
			}}
		>
			{autoScroll ? 'Enabled' : 'Disabled'} auto tail
			<Ico icon="mdi:download-{autoScroll ? 'lock' : 'off'}-outline" />
		</button>
	</div>
</div>

<style lang="postcss">
	@reference "../../../app.css";

	:global([data-part='arrow-tip']:is(.dark *)) {
		--arrow-background: rgb(var(--color-surface-950));
	}

	:global([data-part='arrow-tip']) {
		--arrow-background: rgb(var(--color-surface-50));
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
		@apply preset-filled-secondary-100-900 motif-secondary;
	}

	.FATAL.even {
		@apply bg-secondary-100-900/80;
	}

	.ERROR {
		@apply preset-filled-error-100-900 motif-error;
	}

	.ERROR.even {
		@apply bg-error-100-900/80;
	}

	.WARN {
		@apply preset-filled-warning-100-900 motif-warning;
	}

	.WARN.even {
		@apply bg-warning-100-900/80;
	}

	.INFO {
		@apply preset-filled-success-100-900;
	}

	.INFO.even {
		@apply bg-success-100-900/80;
	}

	.DEBUG {
		@apply preset-filled-primary-100-900 motif-primary;
	}

	.DEBUG.even {
		@apply bg-primary-100-900/80;
	}

	.TRACE {
		@apply preset-filled-tertiary-100-900 motif-tertiary;
	}

	.TRACE.even {
		@apply bg-tertiary-100-900/80;
	}
</style>

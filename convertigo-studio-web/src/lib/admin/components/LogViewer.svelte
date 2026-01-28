<script>
	import { Popover } from '@skeletonlabs/skeleton-svelte';
	import { browser } from '$app/environment';
	import DraggableValue from '$lib/admin/components/DraggableValue.svelte';
	import MovableContent from '$lib/admin/components/MovableContent.svelte';
	import Logs from '$lib/admin/Logs.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { checkArray, debounce } from '$lib/utils/service';
	import { getContext, onDestroy, tick } from 'svelte';
	import { persistedState } from 'svelte-persisted-state';
	import VirtualList from 'svelte-tiny-virtual-list';
	import { flip } from 'svelte/animate';
	import { fromAction } from 'svelte/attachments';
	import { slide } from 'svelte/transition';
	import Button from './Button.svelte';
	import Card from './Card.svelte';
	import MaxRectangle from './MaxRectangle.svelte';
	import PropertyType from './PropertyType.svelte';

	const duration = 400;
	let lineHeight = $state(14.1); // px
	let headerHeight = $state(16); // px
	let messagePaddingY = $state(8); // px

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

	const columnsOrderState = persistedState('admin.logs.columns', _columnsOrder, {
		syncTabs: false
	});
	let columnsOrder = $derived(columnsOrderState.current);

	const columnsConfiguration = {
		Date: { idx: 1, cls: '', fn: (v) => v.split(' ')[0] }, //'font-medium'
		Time: { idx: 1, cls: '', fn: (v) => v.split(' ')[1] }, //'font-medium'
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

	let modalYesNo = getContext('modalYesNo');

	/** @type {{autoScroll?: boolean, filters?: any, serverFilter?: string, startDate?: string, endDate?: string, live?: boolean}} */
	let {
		autoScroll = $bindable(false),
		filters = $bindable({}),
		serverFilter = $bindable(''),
		startDate = $bindable(''),
		endDate = $bindable(''),
		live = $bindable(false)
	} = $props();
	const extraLinesState = persistedState('admin.logs.extraLines', 1, { syncTabs: false });
	let extraLines = $derived(extraLinesState.current);

	let isDragging = $state(false);
	let virtualList = $state();
	let pulsedCategory = $state();
	let pulsedCategoryTimeout;
	let showedLines = $state({ start: 0, end: 0 });
	let clientHeight = $state(200);
	let fullscreen = $state(false);
	const attachHeaderMetrics = $derived(fromAction(measureHeaderMetrics));
	const attachMessageMetrics = $derived(fromAction(measureMessageMetrics));
	const attachScrollIntoView = $derived(fromAction(scrollIntoView));

	/** @param {HTMLDivElement} node */
	function measureHeaderMetrics(node) {
		const update = () => {
			const next = node.getBoundingClientRect().height;
			if (next && Math.abs(next - headerHeight) > 0.1) {
				headerHeight = next;
				virtualList?.recomputeSizes?.(0);
			}
		};

		update();

		if (document?.fonts?.ready) {
			document.fonts.ready.then(update);
		}

		const resizeObserver = new ResizeObserver(update);
		resizeObserver.observe(node);

		return {
			destroy() {
				resizeObserver.disconnect();
			}
		};
	}

	/** @param {HTMLDivElement} node */
	function measureMessageMetrics(node) {
		const update = () => {
			const styles = getComputedStyle(node);
			const nextLineHeight = Number.parseFloat(styles.lineHeight) || lineHeight;
			const nextPaddingY =
				Number.parseFloat(styles.paddingTop) + Number.parseFloat(styles.paddingBottom);
			let changed = false;

			if (Math.abs(nextLineHeight - lineHeight) > 0.1) {
				lineHeight = nextLineHeight;
				changed = true;
			}

			if (!Number.isNaN(nextPaddingY) && Math.abs(nextPaddingY - messagePaddingY) > 0.1) {
				messagePaddingY = nextPaddingY;
				changed = true;
			}

			if (changed) {
				virtualList?.recomputeSizes?.(0);
			}
		};

		update();

		if (document?.fonts?.ready) {
			document.fonts.ready.then(update);
		}

		const resizeObserver = new ResizeObserver(update);
		resizeObserver.observe(node);

		return {
			destroy() {
				resizeObserver.disconnect();
			}
		};
	}

	function doPulse(e) {
		if (e.type == 'click') {
			showFilters.current = true;
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

	async function afterScroll({ detail }) {
		const offset = detail?.offset;
		const isUserScroll = detail?.event?.isTrusted;
		if (typeof offset !== 'number') return;
		if (autoScroll && isUserScroll && offset < lastScrollOffset - 2) {
			autoScroll = false;
		}
		lastScrollOffset = offset;
	}

	function itemSize(index) {
		const lines = Math.max(1, logs[index][logs[index].length - 1]);
		return extraLines * headerHeight + messagePaddingY + Math.max(lineHeight, lines * lineHeight);
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

	const showFilters = persistedState('admin.logs.showFilters', false, { syncTabs: false });

	const filtersFlat = $derived.by(() => {
		const result = [];
		Object.entries(filters).forEach(([category, array]) => {
			array.forEach((filter, index) => {
				result.push({
					category,
					disabled: !!filter.disabled,
					...filter,
					index
				});
			});
		});
		return result;
	});

	function buildActiveFilters() {
		return Object.entries(filters)
			.map(([category, array]) => ({
				category,
				active: (array ?? [])
					.filter((filter) => !filter?.disabled)
					.map((filter) => ({
						...filter,
						valueLower: filter?.value?.toLowerCase?.() ?? filter?.value
					}))
			}))
			.filter(({ active }) => active.length > 0);
	}

	let logs = $state.raw([]);
	let filterRunId = 0;

	function scheduleFiltering() {
		const baseLogs = Logs.logs;
		const filtersSnapshot = buildActiveFilters();
		const runId = ++filterRunId;
		const total = baseLogs.length;

		if (filtersSnapshot.length === 0) {
			logs = baseLogs;
			return;
		}

		logs = [];
		let index = 0;
		let lastLength = 0;
		const results = [];
		const chunkSize = 1000;
		const schedule = (cb) => (browser ? requestAnimationFrame(cb) : setTimeout(cb, 0));

		const processChunk = () => {
			if (runId !== filterRunId) return;

			const end = Math.min(index + chunkSize, total);
			for (; index < end; index++) {
				const log = baseLogs[index];
				const matches = filtersSnapshot.every(({ category, active }) => {
					return active.some(({ mode, value, valueLower, not, sensitive }) => {
						let logValue = getValue(category, log, index);
						let _value = value;
						if (!sensitive) {
							logValue = logValue.toLowerCase();
							_value = valueLower ?? _value.toLowerCase();
						}
						let ret = mode == 'equals' ? logValue == _value : logValue[mode](_value);
						return not ? !ret : ret;
					});
				});
				if (matches) {
					results.push(log);
				}
			}

			if (runId !== filterRunId) return;

			if (results.length !== lastLength) {
				logs = results.slice();
				lastLength = results.length;
			}

			if (index < total) {
				schedule(processChunk);
			} else if (results.length === 0) {
				logs = results;
			}
		};

		processChunk();
	}

	function addFilter({
		event,
		category,
		value = '',
		mode = false,
		ts = new Date().getTime(),
		not = false,
		sensitive = false,
		disabled = false
	}) {
		modalFilterParams = { category, value, mode, ts, not, sensitive, disabled };
		modalFilter.open({ event });
	}

	function removeFilter(category, index) {
		filters[category].splice(index, 1);
		if (filters[category].length == 0) {
			delete filters[category];
		}
		filters = { ...filters };
		scheduleFiltering();
	}

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
	let lastScrollOffset = 0;
	const attachSearchInput = $derived(fromAction(setSearchInput));

	/** @param {HTMLInputElement} node */
	function setSearchInput(node) {
		searchInput = node;
		return {
			destroy() {
				if (searchInput === node) searchInput = undefined;
			}
		};
	}

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
		const { mode, category, value, not, ts, sensitive, disabled } = modalFilterParams;
		let array = checkArray(filters[category]);
		const val = {
			mode: e.submitter.value,
			value,
			not,
			ts,
			sensitive,
			disabled: !!disabled
		};
		if (mode) {
			array[array.findIndex((o) => o.ts == ts)] = val;
		} else {
			array.push(val);
		}
		filters[category] = array;
		filters = { ...filters };
		scheduleFiltering();
		modalFilter.close();
	};
	const size = '4';

	async function restoreColumns(event) {
		const confirmed = modalYesNo
			? await modalYesNo.open({
					event,
					title: 'Restore log columns?',
					message: 'Reset column visibility and widths to their defaults?'
				})
			: true;
		if (confirmed) {
			columnsOrder = _columnsOrder.map((conf) => ({ ...conf }));
		}
	}

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

	// Avoid recursive synchronous loop when server returns no new lines.
	// We keep only one pending retry via a single timeout reference.
	let retryTimer;

	onDestroy(() => {
		if (retryTimer) clearTimeout(retryTimer);
	});

	export async function list(renew = false) {
		Logs.live = live;
		Logs.autoScroll = autoScroll;
		Logs.filter = serverFilter;
		Logs.startDate = startDate;
		Logs.endDate = endDate;
		if (renew) {
			_scrollToIndex = undefined;
		}
		let len = renew ? 0 : Logs.logs.length;
		await Logs.list(renew);
		scheduleFiltering();
		// If no new lines arrived but we are in tailing mode (live) or server says moreResults,
		// schedule a delayed retry instead of immediate recursion to prevent UI freeze.
		if (Logs.logs.length == len && (live || Logs.moreResults)) {
			if (retryTimer) clearTimeout(retryTimer);
			retryTimer = setTimeout(async () => {
				if (live || Logs.moreResults) {
					await list(false);
				}
			}, 200);
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
	{#snippet children({ close })}
		{@const { mode, category, value, not, sensitive } = modalFilterParams}
		<Card
			title="{mode ? 'Edit' : 'Add'} log filter for {category}"
			class="log-filter-card gap-low p-4"
		>
			{#snippet cornerOption()}
				<div class="layout-x-end">
					<button
						type="button"
						class="button-ico-secondary h-7 w-7 justify-center"
						onclick={() => close()}
						aria-label="Close log filter dialog"
					>
						<Ico icon="mdi:close" />
					</button>
				</div>
			{/snippet}
			<form onsubmit={modalFilterSubmit} class="layout-y-low">
				{#if category == 'Message'}
					<textarea
						class="min-h-[120px] input-common resize-y overflow-auto"
						bind:value={modalFilterParams.value}
						wrap={null}
						rows={Math.min(10, value.split('\n').length)}
					></textarea>
				{:else}
					<input class="h-10 input-common text-[15px]" bind:value={modalFilterParams.value} />
				{/if}
				<div class="layout-x-wrap justify-between gap">
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
						bind:checked={
							() => modalFilterParams.sensitive, (v) => (modalFilterParams.sensitive = v)
						}
					/>
				</div>
				<div class="layout-x-wrap-low">
					{#each ['startsWith', 'equals', 'includes', 'endsWith'] as _mode (_mode)}
						<button
							type="submit"
							class="log-filter-mode"
							class:log-filter-mode-active={mode == _mode}
							value={_mode}
						>
							{_mode}
						</button>
					{/each}
				</div>
			</form>
		</Card>
	{/snippet}
</ModalDynamic>
<div class="layout-y-stretch-none h-full w-full text-xs" class:fullscreen>
	<div
		aria-hidden="true"
		style="position: absolute; left: -9999px; top: -9999px; visibility: hidden;"
	>
		<div class="text-xs">
			<div class="px-1 pt-1 text-left leading-none text-nowrap" {@attach attachHeaderMetrics}>
				Ag
			</div>
			<div class="p-1 font-mono leading-4 whitespace-pre" {@attach attachMessageMetrics}>Ag</div>
		</div>
	</div>
	<div class="layout-y-stretch-low">
		{#if showFilters.current}
			<div
				class="mx-low layout-x-wrap-none gap-1 rounded-sm border border-surface-200-800 bg-surface-100-900 p-1"
				transition:slide={{ axis: 'y' }}
			>
				{#each columnsOrder as conf, index (conf.name)}
					{@const { name, show } = conf}
					<div animate:flip={{ duration }}>
						<MovableContent bind:items={columnsOrder} {index} grabClass="cursor-grab">
							<div
								class="log-columns-chip mini-card"
								class:preset-filled-success-100-900={show}
								class:preset-filled-warning-100-900={!show}
								class:motif-warning={!show}
								class:animate-pulse={name == pulsedCategory}
							>
								<span>{name}</span>
								<Button
									{size}
									icon={show ? 'mdi:eye' : 'mdi:eye-off'}
									onclick={() => {
										conf.show = !show;
										columnsOrder = [...columnsOrder];
									}}
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
				<div class="log-columns-chip mini-card preset-filled-warning-100-900 motif-warning">
					<span>Restore</span>
					<Button {size} icon="mdi:backup-restore" onclick={(event) => restoreColumns(event)} />
				</div>
			</div>
		{/if}
		<div
			class="log-toolbar-row mx-low rounded-sm border border-surface-200-800 bg-surface-100-900 px-low"
		>
			<div class="log-toolbar-chip mini-card">
				<Button
					{size}
					icon="mdi:fullscreen{!browser && fullscreen ? '-exit' : ''}"
					cls="log-toolbar-button"
					onmousedown={() => (fullscreen = !fullscreen)}
				/>
			</div>
			<div class="log-toolbar-chip mini-card">
				<Popover
					open={searchBoxOpened}
					onOpenChange={(e) => {
						searchBoxOpened = e.open;
						if (!e.open) {
							searched = '';
							doSearch();
						}
					}}
					positioning={{ placement: fullscreen ? 'bottom-start' : 'top-start' }}
				>
					<Popover.Trigger class="log-toolbar-button">
						<Ico icon="mdi:search" />
					</Popover.Trigger>
					<Popover.Positioner class="log-search-positioner" style="z-index: 20;">
						<Popover.Content class="border-none bg-transparent p-0 shadow-none">
							<Card bg="bg-surface-50-950 text-black dark:text-white" class="p-low!">
								<div class="layout-x-center-low">
									<input
										type="text"
										class="rounded-md border-none bg-transparent"
										bind:value={searched}
										onkeyup={doSearch}
										{@attach attachSearchInput}
									/>
									<input
										type="text"
										class="rounded-md border-none bg-transparent"
										style="field-sizing: content;"
										readonly={true}
										value="{Math.min(foundsIndex + 1, founds.length)}/{founds.length}"
									/>
									<button class="log-toolbar-button log-search-button" onclick={doSearchPrev}
										>↑</button
									>
									<button class="log-toolbar-button log-search-button" onclick={doSearchNext}
										>↓</button
									>
									<button class="log-toolbar-button-error log-search-button" onclick={doSearchClear}
										><Ico icon="mdi:delete-outline" /></button
									>
								</div>
							</Card>
							<Popover.Arrow class="fill-surface-50-950 dark:fill-surface-900" />
						</Popover.Content>
					</Popover.Positioner>
				</Popover>
			</div>
			<div class="log-toolbar-chip mini-card">
				<Button
					{size}
					icon="mdi:filter-cog{showFilters.current ? '' : '-outline'}"
					cls={showFilters.current ? 'log-toolbar-button-active' : 'log-toolbar-button'}
					onmousedown={() => (showFilters.current = !showFilters.current)}
				/>
			</div>
			{#if showFilters.current}
				<div class="log-toolbar-chip mini-card">
					<Button
						{size}
						icon="mdi:plus"
						cls="log-toolbar-button"
						onclick={() => addExtraLines(1)}
					/>
					{#if extraLines > 0}
						<Button
							{size}
							icon="mdi:minus"
							cls="log-toolbar-button"
							onclick={() => addExtraLines(-1)}
						/>
					{/if}
				</div>
			{/if}
			<div class="log-filter-chip mini-card preset-filled-tertiary-100-900 motif-tertiary">
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
			{#each filtersFlat as { category, value, mode, ts, not, sensitive, index, disabled }, idx (ts)}
				<div
					class="log-filter-group"
					animate:flip={{ duration }}
					transition:slide={{ axis: 'x', duration }}
				>
					{#if idx != 0 && index == 0}
						<div class="log-filter-chip mini-card preset-filled-surface-200-800">AND</div>
					{/if}
					{#if index > 0}
						<div class="log-filter-chip mini-card preset-filled-surface-200-800">OR</div>
					{/if}
					<div
						class="log-filter-chip mini-card"
						class:preset-filled-secondary-100-900={!not && !disabled}
						class:motif-secondary={!not && !disabled}
						class:preset-filled-error-100-900={not && !disabled}
						class:motif-error={not && !disabled}
						class:preset-filled-warning-100-900={disabled}
						class:motif-warning={disabled}
					>
						<span class="max-w-xs overflow-hidden"
							>{category} {not ? 'not' : ''} {mode} {sensitive ? value : value.toLowerCase()}</span
						>
						<Button
							{size}
							icon={disabled ? 'mdi:eye-off' : 'mdi:eye'}
							class="w-fit!"
							onclick={() => {
								const arr = checkArray(filters[category]);
								const current = arr[index];
								if (!current) return;
								arr[index] = { ...current, disabled: !current.disabled };
								filters[category] = arr;
								filters = { ...filters };
								scheduleFiltering();
							}}
						/>
						<Button
							{size}
							icon="mdi:edit-outline"
							class="w-fit!"
							onclick={(event) =>
								addFilter({ event, category, value, mode, ts, not, sensitive, disabled })}
						/>
						<Button
							{size}
							icon="mdi:delete-outline"
							class="w-fit!"
							onclick={() => removeFilter(category, index)}
						/>
					</div>
				</div>
			{/each}
		</div>
		<div
			class="layout-x-wrap content-start overflow-y-hidden rounded-sm rounded-b-none border-t border-surface-200-800"
			style="height: {2 + extraLines * headerHeight}px; column-gap: 1px; row-gap: 0;"
		>
			{#each columns as { name, cls, style } (name)}
				<div
					{style}
					class="px-px py-0 {cls} max-h-4.5 overflow-hidden text-nowrap"
					animate:grabFlip={{ duration }}
				>
					<button
						class="cursor-help font-semibold"
						onclick={doPulse}
						onmouseover={doPulse}
						onfocus={doPulse}
					>
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
				<svelte:fragment slot="item" let:style let:index>
					{@const log = logs[index]}
					<div {style}>
						<div
							class="{log[2]} log-row rounded-sm"
							class:odd={index % 2 == 0}
							class:even={index % 2 == 1}
						>
							<div
								class={[
									'layout-x-wrap',
									'overflow-y-hidden',
									'items-baseline',
									'content-start',
									'log-meta',
									log[log.length - 1] > 1 && 'sticky'
								]}
								style="height: {extraLines * headerHeight}px; column-gap: 2px; row-gap: 0;"
							>
								{#each columns as { name, cls, style } (name)}
									{@const value = getValue(name, log, index)}
									<button
										{style}
										class="px-[2px] {cls} cursor-cell overflow-hidden pt-[3px] text-left leading-none text-nowrap"
										animate:grabFlip={{ duration }}
										onclick={(event) => addFilter({ event, category: name, value })}
									>
										{value}
									</button>
								{/each}
							</div>
							<div
								class="log-message overflow-x-scroll rounded-sm p-1 font-mono leading-4 whitespace-pre"
								style="scrollbar-width: none; --tw-ring-opacity: 0.3;"
								{@attach dragscroll}
							>
								{#if founds.length > 0}
									{@const _founds = founds.filter((f) => f.index == index)}
									{#if _founds.length > 0}
										{#each _founds as found, idx (found.start)}
											{@const { start, end } = found}
											{#if idx == 0}
												{log[4].substring(0, start)}{/if}{#if founds[foundsIndex] == found}<span
													{@attach attachScrollIntoView}
													class="searchedCurrent">{log[4].substring(start, end)}</span
												>{:else}<span class="searched">{log[4].substring(start, end)}</span
												>{/if}{#if idx < _founds.length - 1}{log[4].substring(
													end,
													_founds[idx + 1].start
												)}{:else}{log[4].substring(end)}{/if}{/each}{:else}
										{log[4]}
									{/if}
								{:else}
									{log[4]}
								{/if}
							</div>
						</div>
					</div>
				</svelte:fragment>
			</VirtualList>
		</MaxRectangle>
	</div>
	<div
		class="layout-x-p-none items-center justify-between rounded-sm rounded-t-none preset-filled-surface-100-900 py-1! px!"
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

	.log-columns-chip {
		@apply border border-surface-200-800 shadow-sm/10 shadow-surface-900-100;
	}

	.log-toolbar-row {
		@apply flex flex-wrap items-center gap-1;
	}

	.log-filter-group {
		@apply flex items-center gap-1;
	}

	.log-filter-chip {
		@apply border border-surface-200-800 shadow-sm/10 shadow-surface-900-100;
	}

	.log-toolbar-chip {
		@apply gap-1 rounded-sm border border-surface-200-800 bg-surface-50-950 p-0.5;
	}

	.log-filter-card {
		@apply bg-surface-50-950;
	}

	.log-filter-mode {
		@apply button h-10 border border-surface-200-800 bg-surface-100-900 px-4 text-[14px] font-medium text-strong shadow-sm/10 shadow-surface-900-100 hover:bg-surface-200-800;
	}

	.log-filter-mode-active {
		@apply border-primary-600 bg-primary-600 text-white shadow-follow hover:border-primary-500 hover:bg-primary-500;
	}

	:global(.log-toolbar-button) {
		@apply button-primary h-7 w-7 justify-center p-0;
	}

	:global(.log-toolbar-button-active) {
		@apply button-primary h-7 w-7 justify-center p-0 ring-1 ring-primary-400/40;
	}

	:global(.log-toolbar-button-error) {
		@apply button-error h-7 w-7 justify-center p-0;
	}

	:global(.log-search-button) {
		@apply min-w-7;
	}

	:global(.log-search-positioner) {
		z-index: 20;
	}

	.log-meta {
		@apply text-surface-600-400;
	}

	.log-message {
		@apply font-medium text-surface-950-50;
	}

	.log-row {
		--log-tint: var(--color-surface-400);
		--log-tint-weak: 12%;
		--log-tint-strong: 18%;
		background-color: color-mix(
			in oklab,
			var(--log-tint) var(--log-tint-weak),
			var(--color-surface-50)
		);
		box-shadow: inset 0 -1px color-mix(in oklab, var(--color-surface-900) 18%, transparent);
	}

	.log-row.even {
		background-color: color-mix(
			in oklab,
			var(--log-tint) var(--log-tint-strong),
			var(--color-surface-50)
		);
	}

	:global(.dark) .log-row {
		background-color: color-mix(
			in oklab,
			var(--log-tint) var(--log-tint-weak),
			var(--color-surface-950)
		);
		box-shadow: inset 0 -1px color-mix(in oklab, var(--color-surface-50) 12%, transparent);
	}

	:global(.dark) .log-row.even {
		background-color: color-mix(
			in oklab,
			var(--log-tint) var(--log-tint-strong),
			var(--color-surface-950)
		);
	}
	.FATAL {
		--log-tint: var(--color-error-600);
		--log-tint-weak: 34%;
		--log-tint-strong: 44%;
	}

	.ERROR {
		--log-tint: var(--color-error-500);
		--log-tint-weak: 24%;
		--log-tint-strong: 32%;
	}

	.WARN {
		--log-tint: var(--color-warning-500);
		--log-tint-weak: 22%;
		--log-tint-strong: 30%;
	}

	.INFO {
		--log-tint: var(--color-success-500);
		--log-tint-weak: 20%;
		--log-tint-strong: 26%;
	}

	.DEBUG {
		--log-tint: var(--color-primary-500);
		--log-tint-weak: 20%;
		--log-tint-strong: 26%;
	}

	.TRACE {
		--log-tint: var(--color-surface-500);
		--log-tint-weak: 12%;
		--log-tint-strong: 18%;
	}
</style>

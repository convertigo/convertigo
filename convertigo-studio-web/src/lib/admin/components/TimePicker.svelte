<script>
	import { onMount, tick } from 'svelte';
	import { fromAction } from 'svelte/attachments';
	import { cubicOut } from 'svelte/easing';
	import { Tween } from 'svelte/motion';
	import { fly } from 'svelte/transition';

	/** @type {{inputValue?: string}} */
	let { inputValue = $bindable('00:00:00,000') } = $props();
	const size = 150;
	const centerX = size / 2;
	const centerY = size / 2;
	const radius = size / 2 - 15;
	let showClock = $state(false);
	let time = $state({ hour: 0, minute: 0, second: 0, millisecond: 0 });
	let selectedUnit = $state(0);
	let isDragging = $state(false);
	let root = $state();
	let input = $state();
	let timeout;
	const attachRoot = $derived(fromAction(bindRoot));
	const attachInput = $derived(fromAction(bindInput));

	const units = [
		{ name: 'hour', count: 24, markers: 12, prefix: '' },
		{ name: 'minute', count: 60, markers: 12, prefix: ':' },
		{ name: 'second', count: 60, markers: 12, prefix: ':' },
		{ name: 'millisecond', count: 1000, markers: 10, prefix: ',' }
	].map((unit) => ({
		...unit,
		devided: 360 / unit.count,
		angle: 360 / unit.markers,
		length: String(unit.count - 1).length
	}));

	const handPosition = new Tween({ angle: 0 }, { duration: 300, easing: cubicOut });
	const unitPosition = new Tween(-13, { duration: 300, easing: cubicOut });
	let handCoords = $derived.by(() =>
		polarToCartesian(centerX, centerY, radius, handPosition.current.angle)
	);

	function polarToCartesian(centerX, centerY, radius, angleInDegrees) {
		const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;
		return {
			x: centerX + radius * Math.cos(angleInRadians),
			y: centerY + radius * Math.sin(angleInRadians)
		};
	}

	function handleMove(event) {
		if (!isDragging) return;

		const rect = event.target.closest('svg')?.getBoundingClientRect();
		if (!rect) return;

		const x = event.clientX - rect.left - centerX;
		const y = event.clientY - rect.top - centerY;
		const angle = Math.atan2(y, x) * (180 / Math.PI) + 90;
		const adjustedAngle = angle < 0 ? 360 + angle : angle;
		time[units[selectedUnit].name] = Math.floor(adjustedAngle / units[selectedUnit].devided);
		updateHandPosition();
		updateInputValue();
	}

	function handleMouseDown(event) {
		event.preventDefault?.();
		isDragging = true;
		handleMove(event);
	}

	function handleMouseUp(event) {
		event.preventDefault?.();
		isDragging = false;
		const rect = event.target.closest('svg')?.getBoundingClientRect();
		if (!rect) return;

		const x = event.clientX - rect.left - centerX;
		const y = event.clientY - rect.top - centerY;
		if (Math.sqrt(x ** 2 + y ** 2) > radius + 10) {
			return;
		}
		selectedUnit = (selectedUnit + 1) % units.length;
		updateHandPosition();
		updateInputValue();
	}

	function handleTouchStart(event) {
		handleMouseDown(event.touches[0]);
	}

	function handleTouchMove(event) {
		handleMove(event.touches[0]);
	}

	function handleTouchEnd(event) {
		handleMouseUp(event.changedTouches[0]);
	}

	const markers = units.map((unit, j) =>
		Array.from({ length: unit.markers }, (_, i) => {
			return {
				value: (i * unit.count) / unit.markers,
				position: polarToCartesian(centerX, centerY, radius, i * unit.angle),
				inc: j + '_' + i
			};
		})
	);

	function updateHandPosition() {
		const adjustedAngle = time[units[selectedUnit].name] * units[selectedUnit].devided;
		const currentAngle = handPosition.current.angle;
		const diff = adjustedAngle - (currentAngle % 360);
		const nextAngle =
			(diff > 180 ? currentAngle - 360 : diff < -180 ? currentAngle + 360 : currentAngle) + diff;
		handPosition.target = { angle: nextAngle };
	}

	async function updateInputValue() {
		inputValue = units
			.map(
				(unit) =>
					unit.prefix + String(Math.min(time[unit.name], unit.count - 1)).padStart(unit.length, '0')
			)
			.join('');
		await tick();
		input.setSelectionRange(selectedUnit * 3, selectedUnit * 3 + (selectedUnit == 3 ? 3 : 2));
		updateHandPosition();
	}

	function updateTimeValue() {
		const split = inputValue.split(/[:,.]/);
		if (split.length == 4) {
			split.forEach((v, i) => {
				const n = parseInt(v, 10);
				if (!isNaN(n)) {
					time[units[i].name] = Math.min(units[i].count - 1, n);
				}
			});
		}
		updateHandPosition();
	}

	async function inputClick() {
		let idx = input.selectionStart;
		selectedUnit = Math.min(3, Math.floor(idx / 3));
		showClock = true;
		updateInputValue();
	}

	function inputKeyDown(e) {
		if (e.key == 'Enter') {
			close();
			return;
		} else if (e.key == 'ArrowLeft' || e.key == 'ArrowRight') {
			input.selectionStart = Math.max(
				0,
				e.key == 'ArrowLeft' ? input.selectionStart - 3 : input.selectionStart + 3
			);
			inputClick();
		} else if (e.key == 'ArrowUp' || e.key == 'ArrowDown') {
			time[units[selectedUnit].name] =
				(time[units[selectedUnit].name] +
					units[selectedUnit].count +
					(e.key == 'ArrowUp' ? 1 : -1)) %
				units[selectedUnit].count;
			inputClick();
		} else if (
			e.metaKey ||
			e.ctrlKey ||
			e.key == 'Tab' ||
			e.key == 'Backspace' ||
			e.key == 'Delete' ||
			(e.key >= '0' && e.key <= '9')
		) {
			return;
		}
		e.preventDefault();
	}

	function inputKeyUp(e) {
		if (e.ctrlKey && (e.key == 'a' || e.key == 'c' || e.key == 'v')) {
			return;
		}

		if (selectedUnit < 3 && input.selectionStart % 3 == 2) {
			clearTimeout(timeout);
			timeout = window.setTimeout(() => {
				selectedUnit = Math.min(3, selectedUnit + 1);
				updateInputValue();
			}, 200);
		}
		updateTimeValue();
	}

	function handleDocumentClick(e) {
		if (showClock && !root.contains(e.target)) {
			close();
		}
	}

	function close() {
		updateInputValue();
		input.blur();
		showClock = false;
	}

	/** @param {HTMLDivElement} node */
	function bindRoot(node) {
		root = node;
		return {
			destroy() {
				if (root === node) root = undefined;
			}
		};
	}

	/** @param {HTMLInputElement} node */
	function bindInput(node) {
		input = node;
		return {
			destroy() {
				if (input === node) input = undefined;
			}
		};
	}

	onMount(() => {
		document.addEventListener('click', handleDocumentClick);
		return () => document.removeEventListener('click', handleDocumentClick);
	});

	updateTimeValue();
	$effect(() => {
		unitPosition.target = handCoords.y > centerY ? -13 : 13;
	});
</script>

<div class="relative flex h-9 justify-center" {@attach attachRoot}>
	<input
		type="text"
		class="h-9 input-common w-[14ch] max-w-fit preset-filled-surface-200-800 text-[13px] leading-none placeholder:text-[13px] light:bg-white"
		maxlength="12"
		size="11"
		{@attach attachInput}
		bind:value={inputValue}
		onclick={inputClick}
		onkeydown={inputKeyDown}
		onkeyup={inputKeyUp}
		onblur={close}
		onchange={updateTimeValue}
	/>

	{#if showClock}
		<button
			type="button"
			class="clock cursor-pointer select-none"
			style="width: {size}px; height: {size}px; position: absolute; top: 35px; z-index: 1000; box-shadow: 5px 5px 10px 0px #404040;"
			onmousedown={handleMouseDown}
			onmousemove={handleMove}
			onmouseup={handleMouseUp}
			onmouseleave={(e) => {
				e.preventDefault();
				isDragging = false;
			}}
			ontouchstart={handleTouchStart}
			ontouchmove={handleTouchMove}
			ontouchend={handleTouchEnd}
			aria-label="Time picker clock"
			transition:fly={{ x: 0, y: -30, duration: 300 }}
		>
			<svg width={size} height={size}>
				<circle cx={centerX} cy={centerY} r={size / 2} class="shell" />
				<text class="unit-text" x={centerX} y={centerY + unitPosition.current}>
					{units[selectedUnit].name}
				</text>
				{#each markers[selectedUnit] as marker (marker.inc)}
					{@const f = {
						x: (centerX - marker.position.x) / 1.5,
						y: (centerY - marker.position.y) / 1.5,
						duration: 300
					}}
					<g in:fly={f} out:fly={{ ...f, x: -1 * f.x, y: -1 * f.y }}>
						<circle class="marker-circle" cx={marker.position.x} cy={marker.position.y} r="10" />
						<text class="marker-text" x={marker.position.x} y={marker.position.y}>
							{marker.value}
						</text>
					</g>
				{/each}
				<line class="hand" x1={centerX} y1={centerY} x2={handCoords.x} y2={handCoords.y} />
				<circle class="value-circle value-circle-center" cx={centerX} cy={centerY} r="5" />
				<circle class="value-circle value-circle-end" cx={handCoords.x} cy={handCoords.y} r="13" />
				<text class="value-text" x={handCoords.x} y={handCoords.y}>
					{time[units[selectedUnit].name]}
				</text>
			</svg>
		</button>
	{/if}
</div>

<style lang="postcss">
	@reference "../../../app.css";

	.shell {
		@apply fill-surface-50-950 stroke-surface-200-800;
		stroke-width: 1;
	}
	.clock {
		@apply rounded-full border border-surface-200-800 bg-surface-50-950 shadow-lg shadow-surface-900/15;
		position: relative;
		overflow: hidden;
	}
	.hand {
		@apply stroke-primary-500;
		stroke-linecap: round;
		stroke-width: 4;
		transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
	}
	.value-circle {
		@apply fill-primary-500;
	}
	.value-circle-center {
		@apply fill-surface-50-950 stroke-surface-200-800;
		stroke-width: 1;
	}
	.value-circle-end {
		@apply fill-primary-500 shadow-sm;
	}
	.value-text {
		@apply fill-white text-[11px] font-semibold;
		text-anchor: middle;
		alignment-baseline: middle;
	}
	.marker-circle {
		@apply fill-surface-200-800;
	}
	.marker-text {
		@apply fill-surface-700-300 text-[10px] font-medium;
		text-anchor: middle;
		alignment-baseline: middle;
	}
	.unit-text {
		@apply fill-surface-900-100 text-[14px] font-semibold tracking-wide;
		text-anchor: middle;
		alignment-baseline: middle;
		text-transform: uppercase;
	}
</style>

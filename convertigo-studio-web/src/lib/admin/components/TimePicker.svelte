<script>
	import { onMount } from 'svelte';
	import { tweened } from 'svelte/motion';
	import { cubicOut } from 'svelte/easing';
	import { fly } from 'svelte/transition';

	export let size = 150;
	let time = { hour: 0, minute: 0, second: 0, millisecond: 0 };

	const units = [
		{ name: 'hour', count: 24, markers: 12 },
		{ name: 'minute', count: 60, markers: 12 },
		{ name: 'second', count: 60, markers: 12 },
		{ name: 'millisecond', count: 1000, markers: 10 }
	].map((unit) => ({ ...unit, devided: 360 / unit.count, angle: 360 / unit.markers }));
	let selectedUnit = 0;

	let centerX = size / 2;
	let centerY = size / 2;
	let radius = size / 2 - 15;

	const handPosition = tweened({ angle: 0 }, { duration: 300, easing: cubicOut });

	function polarToCartesian(centerX, centerY, radius, angleInDegrees) {
		const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;
		return {
			x: centerX + radius * Math.cos(angleInRadians),
			y: centerY + radius * Math.sin(angleInRadians)
		};
	}

	function handleMouseMove(event) {
		const rect = event.target.closest('svg')?.getBoundingClientRect();
		if (!rect) return;

		const x = event.clientX - rect.left - centerX;
		const y = event.clientY - rect.top - centerY;
		const angle = Math.atan2(y, x) * (180 / Math.PI) + 90;
		const adjustedAngle = angle < 0 ? 360 + angle : angle;
		time[units[selectedUnit].name] = Math.floor(adjustedAngle / units[selectedUnit].devided);

		handPosition.update(({ angle }) => {
			const diff = adjustedAngle - angle;
			// adjustedAngle 357.62189544408903 angle 717.621895444089 diff -359.99999999999994 shortestAngle 717.621895444089
			// adjustedAngle 0.6042690196117917 angle 717.621895444089 diff -717.0176264244772 shortestAngle 360.6042690196118
			const shortestAngle =
				diff > 180 ? adjustedAngle - 360 : diff < -180 ? adjustedAngle + 360 : adjustedAngle;
			console.log(
				'adjustedAngle',
				adjustedAngle,
				'angle',
				angle,
				'diff',
				diff,
				'shortestAngle',
				shortestAngle
			);
			return { angle: shortestAngle };
		});
	}

	onMount(() => {
		const handleMouseMoveWrapper = (event) => handleMouseMove(event);
		document.addEventListener('mousemove', handleMouseMoveWrapper);

		return () => {
			document.removeEventListener('mousemove', handleMouseMoveWrapper);
		};
	});

	function handleClick() {
		selectedUnit = (selectedUnit + 1) % units.length;
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

	$: handCoords = polarToCartesian(centerX, centerY, radius, $handPosition.angle);
</script>

<div
	class="clock cursor-pointer select-none"
	style="width: {size}px; height: {size}px;"
	on:click={handleClick}
>
	<svg width={size} height={size}>
		<circle cx={centerX} cy={centerY} r={size / 2} class="shell" />
		<!-- Texte de l'unité courante centré au fond -->
		<text class="unit-text" x={centerX} y={centerY + 10}>
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
		<circle class="value-circle" cx={handCoords.x} cy={handCoords.y} r="13" />
		<text class="value-text" x={handCoords.x} y={handCoords.y}>
			{time[units[selectedUnit].name]}
		</text>
	</svg>
</div>

<p>Time: {time.hour}h {time.minute}m {time.second}s {time.millisecond}ms</p>

<style>
	.shell {
		fill: grey;
	}
	.clock {
		border-radius: 50%;
		position: relative;
		overflow: hidden;
	}
	.hand {
		stroke: white;
		stroke-linecap: round;
		stroke-width: 4;
		transition: transform 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
	}
	.value-circle {
		fill: white;
	}
	.value-text {
		font-size: 12px;
		text-anchor: middle;
		alignment-baseline: middle;
	}
	.marker-circle {
		fill: lightgray;
	}
	.marker-text {
		font-size: 10px;
		text-anchor: middle;
		alignment-baseline: middle;
	}
	.unit-text {
		font-size: 18px;
		text-anchor: middle;
		alignment-baseline: middle;
		fill: black;
	}
</style>

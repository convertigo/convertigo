<script>
	import { onMount } from 'svelte';
	// @ts-ignore
	import ApexCharts from 'apexcharts?client';
	import { modeCurrent } from '@skeletonlabs/skeleton';
	export let categories;
	export let series;
	export let title;
	export let isLoading;
	let chart;
	let chartEl;
	/** @type {any} */
	let options = {
		theme: {
			mode: $modeCurrent ? 'light' : 'dark'
		},
		title: {
			text: title
		},
		chart: {
			type: 'line',
			height: 300 - 32,
			toolbar: {
				show: false
			}
		},
		series,
		xaxis: {
			categories,
			type: 'datetime',
			labels: {
				format: 'HH:mm:ss',
				datetimeUTC: false
			}
		},
		yaxis: {
			min: 0,
			forceNiceScale: true
		},
		noData: {
			text: 'Loading...'
		},
		tooltip: {
			x: {
				show: true,
				format: 'HH:mm:ss'
			}
		},
		stroke: {
			curve: 'smooth'
		},
		legend: {
			position: 'top',
			horizontalAlign: 'right',
			floating: true,
			offsetY: -25,
			offsetX: 0
		}
	};

	$: if (categories && categories.length > 0 && series && series.length > 0 && chart != undefined) {
		delete options.chart.foreColor;
		delete options.chart.background;
		delete options.theme.palette;

		options.theme.mode = $modeCurrent ? 'light' : 'dark';
		options.xaxis.categories = categories;
		options.series = series;
		chart.updateOptions(options);
	}

	onMount(() => {
		chart = new ApexCharts(chartEl, options);
		chart.render();
	});
</script>

{#if $isLoading == true}
	<div
		class="h-full mt-5 flex"
		class:placeholder={$isLoading}
		class:animate-pulse={$isLoading}
	></div>
{/if}
<div
	bind:this={chartEl}
	class:placeholder={$isLoading}
	class:animate-pulse={$isLoading}
	class="transp {$isLoading ? 'invisible' : 'visible'}"
></div>

<style lang="postcss">
	:global(.apexcharts-svg) {
		background-color: transparent !important;
	}
</style>

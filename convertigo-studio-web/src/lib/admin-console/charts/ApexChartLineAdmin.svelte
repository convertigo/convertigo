<script>
	import { onMount } from 'svelte';
	// @ts-ignore
	import ApexCharts from 'apexcharts?client';
	export let categories;
	export let series;
	export let title;
	export let isLoading;
	let chart;
	let chartEl;
	/** @type {any} */
	let options = {
		theme: {
			mode: 'light',
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
		options.xaxis.categories = categories;
		options.series = series;
		chart.updateOptions(options);
	}
	onMount(async () => {
		chart = new ApexCharts(chartEl, options);
		chart.render();
	});
</script>

{#if $isLoading == true}
	<div class="h-6 mt-5 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-6 mt-2 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-6 mt-3 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-5 mt-2 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-4 mt-4 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-7 mt-2 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
{/if}
<div
	bind:this={chartEl}
	class:placeholder={$isLoading}
	class:animate-pulse={$isLoading}
	class="transp {$isLoading ? 'invisible' : 'visible'}"
></div>

<style>
	:global(.apexcharts-svg) {
		background-color: transparent !important;
	}
</style>

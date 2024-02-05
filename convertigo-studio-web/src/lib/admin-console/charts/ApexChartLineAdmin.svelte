<script>
	import { onMount } from 'svelte';
	import ApexCharts from 'apexcharts?client'
	export let _labels;
	export let _series;
	export let _title;
	export let _isLoading;
	let chart;
	let chartEl;
	/** @type {any} */
	let options = {
		theme: {
			mode: 'dark',
		}, 
		title:{
			text: _title
		},
		chart: {
			type: 'line',
			height: 300 - 32,
			toolbar: {
				show: false
			}
		},
		series: _series,
		xaxis: {
			categories: _labels,
			type: "datetime",
			labels: {
				format: 'HH:mm:ss',
				datetimeUTC: false
			}
		},
		yaxis: {
			min: 0,
			forceNiceScale: true,
		},
		noData: {
  			text: "Loading...",
		},
		tooltip: {
			x: {
			show: true,
			format: 'HH:mm:ss',
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
	$: if (_labels && _labels.length > 0 && _series && _series.length > 0 && chart != undefined) {
		options.xaxis.categories = _labels;
		options.series = _series;
		chart.updateOptions(options);
	}
	onMount(async() => {
		chart = new ApexCharts(chartEl, options);
		chart.render();
	});
</script>
{#if $_isLoading == true}
	<div class="h-6 mt-5 flex" class:placeholder={$_isLoading} class:animate-pulse={$_isLoading}></div>
	<div class="h-6 mt-2 flex" class:placeholder={$_isLoading} class:animate-pulse={$_isLoading}></div>
	<div class="h-6 mt-3 flex" class:placeholder={$_isLoading} class:animate-pulse={$_isLoading}></div>
	<div class="h-5 mt-2 flex" class:placeholder={$_isLoading} class:animate-pulse={$_isLoading}></div>
	<div class="h-4 mt-4 flex" class:placeholder={$_isLoading} class:animate-pulse={$_isLoading}></div>
	<div class="h-7 mt-2 flex" class:placeholder={$_isLoading} class:animate-pulse={$_isLoading}></div>
{/if}
<div 
	bind:this={chartEl}
	class:placeholder={$_isLoading}
	class:animate-pulse={$_isLoading}
	class="transp  {$_isLoading ? 'invisible' : 'visible'}"
></div>

<style>
	:global(.apexcharts-svg){
		background-color: transparent !important;
	}
</style>

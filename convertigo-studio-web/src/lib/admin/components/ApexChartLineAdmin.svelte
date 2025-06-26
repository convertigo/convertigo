<script>
	import { browser } from '$app/environment';
	import Light from '$lib/common/Light.svelte';
	import { onMount, untrack } from 'svelte';

	/** @type {{categories: any, series: any, title: any}} */
	let { categories, series, title } = $props();
	let chart = $state();
	let isLoading = $derived(!series?.[0]?.data?.length);

	let chartEl;
	const colors = { light: [], dark: [] };
	/** @type {any} */
	let options = $state({
		theme: {
			mode: Light.mode
		},
		title: {
			text: title
		},
		chart: {
			type: 'line',
			height: 300,
			zoom: {
				enabled: false
			},
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
			text: 'Loading…'
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
	});

	$effect(() => {
		if (series && series.length > 0 && categories && categories.length > 0 && chart != undefined) {
			delete options.chart.foreColor;
			delete options.chart.background;
			delete options.theme.palette;

			options.theme.mode = Light.mode;
			options.xaxis.categories = categories;
			options.series = series;
			options.colors = colors[Light.mode];
			untrack(() => {
				chart.updateOptions(options);
			});
		}
	});

	onMount(() => {
		let styles = window.getComputedStyle(chartEl);
		for (let m of [
			['light', 200],
			['dark', 600]
		]) {
			colors[m[0]] = [];
			['warning', 'primary', 'success'].forEach((color) => {
				colors[m[0]].push(styles.getPropertyValue(`--color-${color}-${m[1]}`));
			});
		}

		import('apexcharts').then(({ default: ApexCharts }) => {
			chart = new ApexCharts(chartEl, options);
			chart.render();
		});
		return () => {
			chart?.destroy();
			chart = undefined;
		};
	});
</script>

<div
	bind:this={chartEl}
	class:placeholder={isLoading}
	class:animate-pulse={isLoading}
	class="h-[315px]"
></div>

<style>
	:global(.apexcharts-svg) {
		background-color: transparent !important;
	}
</style>

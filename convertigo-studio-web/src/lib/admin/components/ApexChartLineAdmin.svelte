<script>
	import Light from '$lib/common/Light.svelte';
	import { onMount, untrack } from 'svelte';

	/** @type {{categories: any, series: any, title: any}} */
	let { categories, series, title } = $props();
	let chart = $state();
	let chartEl;
	let colors = $state({ light: [], dark: [] });

	let options = $state({});

	const normalizedSeries = $derived.by(() =>
		(series ?? []).map(({ name, data }) => ({
			name,
			data: Array.isArray(data) ? [...data] : []
		}))
	);

	const normalizedCategories = $derived.by(() =>
		Array.isArray(categories) ? [...categories] : []
	);

	const resolvedColors = $derived.by(() => [...(colors[Light.mode] ?? [])]);

	let isLoading = $derived(!normalizedSeries?.[0]?.data?.length);

	const baseOptions = $derived.by(() => ({
		theme: {
			mode: Light.mode
		},
		title: {
			text: title,
			offsetX: 8,
			offsetY: 8
		},
		chart: {
			type: 'line',
			height: 300,
			animations: {
				enabled: false,
				animateGradually: {
					enabled: false
				},
				dynamicAnimation: {
					enabled: false
				}
			},
			zoom: {
				enabled: false
			},
			toolbar: {
				show: false
			}
		},
		series: normalizedSeries,
		xaxis: {
			categories: normalizedCategories,
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
		dataLabels: {
			enabled: false
		},
		stroke: {
			curve: 'smooth',
			width: 2
		},
		legend: {
			position: 'top',
			horizontalAlign: 'right',
			floating: true,
			offsetY: -25,
			offsetX: 0
		}
	}));

	const buildOptions = () => ({
		...baseOptions,
		series: normalizedSeries.map(({ name, data }) => ({ name, data: [...data] })),
		xaxis: {
			...baseOptions.xaxis,
			categories: [...normalizedCategories]
		},
		colors: [...resolvedColors]
	});

	$effect(() => {
		options = buildOptions();
		if (chart && normalizedSeries.length > 0 && normalizedCategories.length > 0) {
			untrack(() => {
				chart.updateOptions(options, true, true);
			});
		}
	});

	onMount(() => {
		let styles = window.getComputedStyle(chartEl);
		const newColors = { light: [], dark: [] };
		for (let m of [
			['light', 200],
			['dark', 600]
		]) {
			['warning', 'primary', 'success'].forEach((color) => {
				newColors[m[0]].push(styles.getPropertyValue(`--color-${color}-${m[1]}`));
			});
		}
		colors = newColors;

		import('apexcharts').then(({ default: ApexCharts }) => {
			chart = new ApexCharts(chartEl, buildOptions());
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

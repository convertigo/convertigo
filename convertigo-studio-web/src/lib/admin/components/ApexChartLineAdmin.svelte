<script>
	import Light from '$lib/common/Light.svelte';
	import { onMount } from 'svelte';

	/** @type {{categories: any, series: any, title: any}} */
	let { categories, series, title } = $props();
	let chart = $state();
	let chartEl;
	/** @type {{ light: string[], dark: string[] }} */
	let colors = $state({ light: [], dark: [] });
	let chartTokens = $state({
		fontFamily: '',
		baseFontSize: '14px',
		labelFontSize: '12px',
		titleFontSize: '14px',
		titleFontWeight: 500,
		text: { light: '', dark: '' },
		muted: { light: '', dark: '' },
		grid: { light: '', dark: '' }
	});

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
	const resolvedTokens = $derived.by(() => ({
		fontFamily: chartTokens.fontFamily,
		baseFontSize: chartTokens.baseFontSize,
		labelFontSize: chartTokens.labelFontSize,
		titleFontSize: chartTokens.titleFontSize,
		titleFontWeight: chartTokens.titleFontWeight,
		textColor: chartTokens.text[Light.mode],
		mutedColor: chartTokens.muted[Light.mode],
		gridColor: chartTokens.grid[Light.mode]
	}));

	let isLoading = $derived(!normalizedSeries?.[0]?.data?.length);

	const baseOptions = $derived.by(() => ({
		theme: {
			mode: Light.mode
		},
		chart: {
			type: 'line',
			height: 300,
			fontFamily: resolvedTokens.fontFamily,
			foreColor: resolvedTokens.mutedColor,
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
		title: {
			text: title,
			offsetX: 8,
			offsetY: 8,
			style: {
				fontFamily: resolvedTokens.fontFamily,
				fontSize: resolvedTokens.titleFontSize,
				fontWeight: resolvedTokens.titleFontWeight,
				color: resolvedTokens.textColor
			}
		},
		series: normalizedSeries,
		xaxis: {
			categories: normalizedCategories,
			type: 'datetime',
			tickAmount: 5,
			labels: {
				format: 'HH:mm:ss',
				datetimeUTC: false,
				showDuplicates: false,
				hideOverlappingLabels: true,
				style: {
					fontFamily: resolvedTokens.fontFamily,
					fontSize: resolvedTokens.labelFontSize,
					colors: resolvedTokens.textColor
				}
			}
		},
		yaxis: {
			min: 0,
			forceNiceScale: true,
			labels: {
				style: {
					fontFamily: resolvedTokens.fontFamily,
					fontSize: resolvedTokens.labelFontSize,
					colors: resolvedTokens.textColor
				}
			}
		},
		noData: {
			text: 'Loading…',
			style: {
				fontFamily: resolvedTokens.fontFamily,
				fontSize: resolvedTokens.baseFontSize,
				color: resolvedTokens.mutedColor
			}
		},
		tooltip: {
			theme: Light.mode,
			x: {
				show: true,
				format: 'HH:mm:ss'
			},
			style: {
				fontFamily: resolvedTokens.fontFamily,
				fontSize: resolvedTokens.labelFontSize
			}
		},
		dataLabels: {
			enabled: false
		},
		stroke: {
			curve: 'smooth',
			width: 3,
			lineCap: 'round'
		},
		markers: {
			size: 0,
			strokeWidth: 0,
			strokeColors: resolvedColors,
			fillColors: resolvedColors,
			hover: {
				size: 0
			}
		},
		grid: {
			borderColor: resolvedTokens.gridColor,
			strokeDashArray: 3
		},
		legend: {
			position: 'top',
			horizontalAlign: 'right',
			floating: true,
			offsetY: -25,
			offsetX: 0,
			fontFamily: resolvedTokens.fontFamily,
			fontSize: resolvedTokens.labelFontSize,
			labels: {
				colors: resolvedTokens.textColor
			},
			markers: {
				fillColors: resolvedColors
			}
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

	const options = $derived.by(() => {
		const nextOptions = buildOptions();
		if (chart && normalizedSeries.length > 0 && normalizedCategories.length > 0) {
			chart.updateOptions(nextOptions, true, true);
		}
		return nextOptions;
	});

	const attachChart = (node) => {
		chartEl = node;
		return {
			destroy() {
				if (chartEl === node) {
					chartEl = undefined;
				}
			}
		};
	};

	onMount(() => {
		let styles = window.getComputedStyle(chartEl);
		const palette = [
			styles.getPropertyValue('--color-primary-500').trim(),
			styles.getPropertyValue('--color-success-500').trim(),
			styles.getPropertyValue('--color-warning-500').trim()
		].filter(Boolean);
		colors = { light: palette, dark: palette };
		const baseFontSize = parseFloat(styles.getPropertyValue('--base-font-size')) || 14;
		const titleFontSize = Math.round(baseFontSize * 1.285);
		chartTokens = {
			fontFamily: styles.getPropertyValue('--base-font-family').trim() || 'IBM Plex Sans Variable',
			baseFontSize: `${baseFontSize}px`,
			labelFontSize: `${Math.max(baseFontSize - 2, 11)}px`,
			titleFontSize: `${Math.max(titleFontSize, 16)}px`,
			titleFontWeight: 500,
			text: {
				light: styles.getPropertyValue('--base-font-color').trim(),
				dark: styles.getPropertyValue('--base-font-color-dark').trim()
			},
			muted: {
				light: styles.getPropertyValue('--convertigo-text-muted').trim(),
				dark: styles.getPropertyValue('--convertigo-text-muted-dark').trim()
			},
			grid: {
				light: styles.getPropertyValue('--color-surface-400').trim(),
				dark: styles.getPropertyValue('--color-surface-600').trim()
			}
		};

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
	{@attach attachChart}
	data-options={options ? 'ready' : ''}
	class:placeholder={isLoading}
	class:animate-pulse={isLoading}
	class="h-[315px]"
></div>

<style>
	:global(.apexcharts-svg) {
		background-color: transparent !important;
	}
</style>

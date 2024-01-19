<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	import { monitorCheck, threads, labels, isLoading } from '../stores/monitorStore';
	import { get } from 'svelte/store';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	threads.subscribe((data) => {
		if (!data.length || !chartCanvas) {
			return;
		}
		if (!chart) {
			chart = new Chart(chartCanvas, {
				type: 'line',
				data: {
					labels: get(labels),
					datasets: [
						{
							label: 'Threads',
							data,
							fill: false,
							borderColor: 'rgb(75, 192, 192)',
							tension: 0.1
						}
					]
				},
				options: {
					scales: {
						y: {
							beginAtZero: true
						},
						x: {
							ticks: {
								callback: function (value, index, values) {
									return '';
								}
							}
						}
					}
				}
			});
		} else {
			chart.update();
		}
	});

	onMount(() => {
		monitorCheck();
	});
</script>

{#if $isLoading == true}
	<div class="h-6 mt-5 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-6 mt-2 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-6 mt-3 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-5 mt-2 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-4 mt-4 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
	<div class="h-7 mt-2 flex" class:placeholder={$isLoading} class:animate-pulse={$isLoading}></div>
{:else}
	<canvas
		bind:this={chartCanvas}
		class:placeholder={$isLoading}
		class:animate-pulse={$isLoading}
		style="width: 100%; height: 100%"
	></canvas>
{/if}

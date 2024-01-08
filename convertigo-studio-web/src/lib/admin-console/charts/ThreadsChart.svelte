<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	import { check, threads, labels } from '../stores/monitorStore';
	import { get } from 'svelte/store';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	threads.subscribe((data) => {
		if (!data.length) {
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
		check();
	});
</script>

<canvas bind:this={chartCanvas} style="width: 100%; height: 100%"></canvas>

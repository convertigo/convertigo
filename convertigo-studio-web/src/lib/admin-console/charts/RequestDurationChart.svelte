<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	import { check, requests, labels } from '../stores/monitorStore';
	import { get } from 'svelte/store';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	requests.subscribe((data) => {
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
							label: 'Request Duration',
							data,
							fill: false,
							borderColor: 'rgb(153, 102, 255)',
							tension: 0.1
						}
					]
				},
				options: {
					scales: {
						y: {
							beginAtZero: true
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

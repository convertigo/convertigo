<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	Chart.register(...registerables);

	import { check, contexts, labels } from '../stores/monitorStore';
	import { get } from 'svelte/store';

	let chart = null;
	let chartCanvas;
	let updateCount = 0;

	contexts.subscribe((data) => {
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
							label: 'Contexts',
							data,
							fill: false,
							borderColor: 'rgb(255, 159, 64)',
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

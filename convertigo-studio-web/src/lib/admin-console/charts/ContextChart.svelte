<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	import { monitorCheck, contexts, labels } from '../stores/monitorStore';
	import { get } from 'svelte/store';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	contexts.subscribe((data) => {
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
		monitorCheck();
	});
</script>

<canvas bind:this={chartCanvas} style="width: 100%; height: 100%"></canvas>

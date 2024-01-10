<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	import {
		monitorCheck,
		memoryUsed,
		memoryMaximal,
		memoryTotal,
		labels
	} from '../stores/monitorStore';
	import { get } from 'svelte/store';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	memoryUsed.subscribe((data) => {
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
							label: 'Memory Maximal',
							data: get(memoryMaximal),
							borderColor: 'rgb(255, 99, 132)',
							backgroundColor: 'rgba(255, 99, 132, 0.5)'
						},
						{
							label: 'Memory Total',
							data: get(memoryTotal),
							borderColor: 'rgb(54, 162, 235)',
							backgroundColor: 'rgba(54, 162, 235, 0.5)'
						},
						{
							label: 'Memory Used',
							data,
							borderColor: 'rgb(75, 192, 192)',
							backgroundColor: 'rgba(75, 192, 192, 0.5)'
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

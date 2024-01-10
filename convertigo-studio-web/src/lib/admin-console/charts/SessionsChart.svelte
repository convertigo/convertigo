<script>
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	import {
		monitorCheck,
		sessionMaxCV,
		sessions,
		availableSessions,
		labels
	} from '../stores/monitorStore';
	import { get } from 'svelte/store';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	availableSessions.subscribe((data) => {
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
							label: 'Max',
							data: get(sessionMaxCV),
							borderColor: 'rgb(255, 99, 132)',
							backgroundColor: 'rgba(255, 99, 132, 0.5)'
						},
						{
							label: 'Sessions',
							data: get(sessions),
							borderColor: 'rgb(54, 162, 235)',
							backgroundColor: 'rgba(54, 162, 235, 0.5)'
						},
						{
							label: 'Available',
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

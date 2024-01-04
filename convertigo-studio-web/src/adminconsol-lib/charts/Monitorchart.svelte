<script>
	import { fetchEngineMonitorData } from '../stores/Store';
	import { onMount } from 'svelte';
	import { Chart, registerables } from 'chart.js';
	Chart.register(...registerables);

	let chart = null;
	let chartCanvas;

	async function loadMonitorData() {
		const data = await fetchEngineMonitorData();
		if (data && data.admin) {
			updateChartData(data.admin);
		}
	}

	function updateChartData(data) {
		if (chart) {
			chart.data.datasets[0].data.push(data.memoryMaximal);
			chart.data.datasets[1].data.push(data.memoryTotal);
			chart.data.datasets[2].data.push(data.memoryUsed);
			chart.update();
		} else {
			chart = new Chart(chartCanvas, {
				type: 'line',
				data: {
					labels: ['Memory Maximal', 'Memory Total', 'Memory Used'],
					datasets: [
						{
							label: 'Memory Maximal',
							data: [data.memoryMaximal],
							borderColor: 'rgb(255, 99, 132)',
							backgroundColor: 'rgba(255, 99, 132, 0.5)'
						},
						{
							label: 'Memory Total',
							data: [data.memoryTotal],
							borderColor: 'rgb(54, 162, 235)',
							backgroundColor: 'rgba(54, 162, 235, 0.5)'
						},
						{
							label: 'Memory Used',
							data: [data.memoryUsed],
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
		}
	}

	onMount(() => {
		loadMonitorData();
		const interval = setInterval(loadMonitorData, 4000);

		return () => {
			clearInterval(interval);
		};
	});
</script>

<canvas bind:this={chartCanvas} style="width: 100%; height: 100%"></canvas>

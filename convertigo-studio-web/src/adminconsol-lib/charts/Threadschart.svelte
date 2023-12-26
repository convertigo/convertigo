<script>
    import { onMount } from 'svelte';
    import { Chart, registerables } from 'chart.js';
    Chart.register(...registerables);

    import { fetchEngineMonitorData } from '../stores/Store';

    let chart = null;
    let chartCanvas;
    let updateCount = 0; 

    async function loadMonitorData() {
        const data = await fetchEngineMonitorData();
        if (data && data.admin && data.admin.threads !== undefined) {
            updateChartData(data.admin.threads);
        }
    }

    function updateChartData(threadsCount) {
        updateCount++; 
        if (!chart) {
            chart = new Chart(chartCanvas, {
                type: 'line',
                data: {
                    labels: [updateCount.toString()],
                    datasets: [{
                        label: 'Threads',
                        data: [threadsCount], 
                        fill: false,
                        borderColor: 'rgb(75, 192, 192)',
                        tension: 0.1
                    }]
                },
                options: {
                    scales: {
                        y: {
                            beginAtZero: true
                        },
                        x: {
                            
                            ticks: {
                               
                                callback: function(value, index, values) {
                                    return '';
                                }
                            }
                        }
                    }
                }
            });
        } else {
            chart.data.labels.push(updateCount.toString()); 
            chart.data.datasets[0].data.push(threadsCount);
            chart.update();
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

<script>
    import { onMount } from 'svelte';
    import { Chart, registerables } from 'chart.js';
    Chart.register(...registerables);

    // Assurez-vous que ce chemin d'importation est correct
    import { fetchEngineMonitorData } from '../stores/Store';

    let chart = null;
    let chartCanvas;
    let updateCount = 0; 

    async function loadMonitorData() {
        const data = await fetchEngineMonitorData();
        if (data && data.admin && data.admin.requests !== undefined) {
            updateChartData(data.admin.requests);
        }
    }

    function updateChartData(requestDuration) {
        updateCount++; 
        if (!chart) {
            chart = new Chart(chartCanvas, {
                type: 'line',
                data: {
                    labels: [updateCount.toString()], 
                    datasets: [{
                        label: 'Request Duration',
                        data: [requestDuration], 
                        fill: false,
                        borderColor: 'rgb(153, 102, 255)',
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
            chart.data.datasets[0].data.push(requestDuration);
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

<script>
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { cronStore } from '../stores/cronStore';
	import ResponsiveContainer from './ResponsiveContainer.svelte';
	import { jobsStore } from '../stores/schedulerStore';

	let cronExpression = '';

	$: cronExpression = cronStore.compileCronExpression();

	function handleInput(part, event) {
		cronStore.updateCronPart(part, event.target.value);
	}
	/**        {#each Object.keys($cronStore) as key}
            <div>
                <label for={key}>{key.charAt(0).toUpperCase() + key.slice(1)}:</label>
                <input
                    type="text"
                    class="input-common"
                    id={key}
                    value={$cronStore[key]}
                    on:input={(event) => handleInput(key, event)}
                />
            </div>
        {/each}
*/
</script>

<div class=" max-h-[30vh]">
	<h3>Configure Cron Schedule</h3>
	{#each Object.keys($cronStore) as key}
		<div>
			<label for={key}>{key.charAt(0).toUpperCase() + key.slice(1)}:</label>
			<input
				type="text"
				class="input-common"
				id={key}
				value={$cronStore[key]}
				on:input={(event) => handleInput(key, event)}
			/>
		</div>
	{/each}
	<p>Current Cron Expression: {cronExpression}</p>
</div>

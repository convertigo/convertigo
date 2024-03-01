<script context="module">
	let cpt = 0;
</script>

<script>
	import { RadioGroup, RadioItem, SlideToggle } from '@skeletonlabs/skeleton';
	import { configurations } from '../stores/configurationStore';
	import Ico from '$lib/utils/Ico.svelte';
	export let property;

	let id = `property-input-${cpt++}`;

	function update() {
		$configurations = $configurations;
	}

	/**
     * @param { Event } e
     */
	 function check(e) {
        // @ts-ignore
        property['@_value'] = '' + e.target.checked;
        update();
    }
</script>

<div class="flex flox-row items-center">
	{#if property['@_type'] == 'Boolean'}
		<div class="flex-1 flex items-center">
	<!--	<RadioGroup class="text-token">
				<RadioItem
					name={id}
					bind:group={property['@_value']}
					active="variant-filled-success text-token"
					value="true"
					on:change={update}
				>
					Yes
				</RadioItem>
				<RadioItem
					name={id}
					bind:group={property['@_value']}
					active="variant-filled-surface text-white"
					value="false"
					on:change={update}
				>
					No
				</RadioItem>
			</RadioGroup>
		
			<a
				class="label-common ml-5"
				href={''}
				on:click|preventDefault={() => {
					property['@_value'] = property['@_value'] == 'true' ? 'false' : 'true';
					update();
				}}>{property['@_description']}</a
			>
		-->	

			<SlideToggle size='md' name={id} active="bg-success-400 dark:bg-success-700" background="bg-error-400 dark:bg-error-700" checked={property['@_value'] == 'true'} on:change={check}>
                <span class="cursor-pointer">{property['@_description']}</span>
            </SlideToggle>

			
		</div>
	{:else if property['@_type'] == 'Text'}
		<div class="flex-1 flex flex-col justify-center border-common">
			<label class="label-common" for={id}>{property['@_description']}</label>
			<input
				type="text"
				{id}
				placeholder="Enter value ..."
				class="input-common placeholder:pl-1"
				bind:value={property['@_value']}
				on:input={update}
			/>
		</div>
	{:else if property['@_type'] == 'PasswordHash'}
		<div class="flex-1 flex flex-col justify-center border-common">
			<label class="label-common" for={id}>{property['@_description']}</label>
			<input
				type="text"
				{id}
				placeholder="Enter value ..."
				class="input-common input-text placeholder:pl-1"
				bind:value={property['@_value']}
				on:input={update}
			/>
		</div>
	{:else if property['@_type'] == 'PasswordPlain'}
		<div class="flex-1 flex flex-col justify-center border-common">
			<label class="label-common" for={id}>{property['@_description']}</label>
			<input
				type="password"
				{id}
				placeholder="Enter value ..."
				class="input-common input-text placeholder:pl-1"
				bind:value={property['@_value']}
				on:input={update}
			/>
		</div>
	{:else if property['@_type'] == 'Combo'}
		<div class="flex-1 flex flex-col justify-center border-common">
			<label class="label-common" for={id}>{property['@_description']}</label>

			<select class="input-common" bind:value={property['@_value']}>
				{#each property.item as option}
					<option {id} on:change={update} value={option['@_value']}>{option['@_value']}</option>
				{/each}
			</select>
		</div>
	{:else if property['@_type'] == 'Array'}
		<div class="flex-1 flex flex-col justify-center border-common">pas géré</div>
	{/if}

	<div class="flex-none btn-group-vertical shadow-md ml-10 mr-10">
		<button
			disabled={property['@_value'] == property['@_originalValue']}
			on:click={() => {
				property['@_value'] = property['@_originalValue'];
				update();
			}}
			title={`restore:${property['@_originalValue']}`}><Ico icon="mdi:arrow-u-left-top" /></button
		>
		<button
			disabled={property['@_value'] == property['@_defaultValue']}
			on:click={() => {
				property['@_value'] = property['@_defaultValue'];
				update();
			}}
			title={`reset:${property['@_defaultValue']}`}><Ico icon="mdi:backup-restore" /></button
		>
	</div>
</div>

<style lang="postcss">
	.btn-group-vertical button {
		@apply hover:variant-filled-primary;
	}

	.btn-group-vertical button:disabled {
		background-color: inherit;
	}
</style>

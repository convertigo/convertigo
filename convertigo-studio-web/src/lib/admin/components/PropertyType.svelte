<script context="module">
	let cpt = 0;
</script>

<script>
	import { configurations } from '../stores/configurationStore';
	import Ico from '$lib/utils/Ico.svelte';
	import CheckState from './CheckState.svelte';
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

<div class="flex items-center gap-x-2">
	{#if property['@_type'] == 'Boolean'}
		<CheckState class="grow" name={id} checked={property['@_value'] == 'true'} on:change={check}
			>{property['@_description']}</CheckState
		>
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

			<select class="input-common" {id} bind:value={property['@_value']} on:change={update}>
				{#each property.item as option}
					<option value={option['@_value']}>{option['#text']}</option>
				{/each}
			</select>
		</div>
	{:else if property['@_type'] == 'Array'}
		<div class="flex-1 flex flex-col justify-center border-common">Not handled</div>
	{/if}

	<div class="flex-none shadow-md">
		<button
			disabled={property['@_value'] == property['@_originalValue']}
			on:click={() => {
				property['@_value'] = property['@_originalValue'];
				update();
			}}
			title="restore:{property['@_originalValue']}"
			class="btn btn-sm"><Ico icon="mdi:arrow-u-left-top" /></button
		>
		<button
			disabled={property['@_value'] == property['@_defaultValue']}
			on:click={() => {
				property['@_value'] = property['@_defaultValue'];
				update();
			}}
			title="reset:{property['@_defaultValue']}"><Ico icon="mdi:backup-restore" /></button
		>
	</div>
</div>

<style lang="postcss">
	.btn-group-vertical button {
		@apply hover:bg-primary-400-500-token;
	}

	.btn-group-vertical button:disabled {
		background-color: inherit;
	}
</style>

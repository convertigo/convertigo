<script context="module">
	let cpt = 0;
</script>

<script>
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import { configurations } from '../stores/configurationStore';
	import Icon from '@iconify/svelte';
	export let property;

	let id = `property-input-${cpt++}`;

	function update() {
		$configurations = $configurations;
	}
</script>

<div class="flex flox-row items-center">
	{#if property['@_type'] == 'Boolean'}
		<div class="flex-1 flex items-center">
			<RadioGroup class="text-token" on:change={update}>
				<RadioItem
					name={id}
					bind:group={property['@_value']}
					active="bg-buttons text-white"
					value="true"
				>
					Yes
				</RadioItem>
				<RadioItem
					name={id}
					bind:group={property['@_value']}
					active="dark:bg-surface-400 bg-white"
					value="false"
				>
					No
				</RadioItem>
			</RadioGroup>
			<a
				class="label-common ml-5"
				href={''}
				on:click|preventDefault={() =>
					(property['@_value'] = property['@_value'] == 'true' ? 'false' : 'true')}
				>{property['@_description']}</a
			>
		</div>
	{:else if property['@_type'] == 'Text'}
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
	{:else}
		<div class="flex-1 flex flex-col justify-center border-common">
			Type {property['@_type']} not handled yet! ({property['@_description']})
		</div>
	{/if}
	<div class="flex-none btn-group-vertical variant-ghost ml-1">
		<button
			disabled={property['@_value'] == property['@_originalValue']}
			on:click={() => {
				property['@_value'] = property['@_originalValue'];
				update();
			}}
			title={`restore:${property['@_originalValue']}`}><Icon icon="mdi:arrow-u-left-top" /></button
		>
		<button
			disabled={property['@_value'] == property['@_defaultValue']}
			on:click={() => {
				property['@_value'] = property['@_defaultValue'];
				update();
			}}
			title={`reset:${property['@_defaultValue']}`}><Icon icon="mdi:backup-restore" /></button
		>
	</div>
</div>

<style lang="postcss">
	.btn-group-vertical button {
		@apply py-1 px-2;
		background-color: rgba(255, 162, 0, 0.5);
	}

	.btn-group-vertical button:disabled {
		background-color: inherit;
	}

	/**style for label*/
	.label-common {
		@apply text-[14px] cursor-pointer;
	}
	/**Style for Input*/
	.input-common {
		@apply placeholder:text-[16px] placeholder:dark:text-surface-100 placeholder:text-surface-100 placeholder:font-light font-normal border-none dark:bg-surface-800;
		border-bottom: surface-200;
	}

	.input-text {
		@apply mt-1 pl-4 text-[16px] dark:text-surface-200 text-surface-600;
	}

	.border-common {
		@apply border-b-[1px] dark:border-surface-600 border-surface-100;
	}
</style>

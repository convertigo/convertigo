<script>
	import { updateConfiguration } from '../stores/configurationStore';

	export let property;
	export let propertyIndex;
	export let selectedIndex;
	export let hasChanges = false;

	const id = `prop_${selectedIndex}_${propertyIndex}`;
	function onInputChange() {
		hasChanges = true;
	}

	export function handleInputChange(categoryIndex, propertyIndex, event) {
		const newValue = event.target.value;
		console.log(`changes detected ${propertyIndex} dans la catégorie ${categoryIndex}:`, newValue);
		updateConfiguration(categoryIndex, propertyIndex, newValue);
	}
</script>

{#if property['@_type'] == 'Boolean'}
	<div class="flex items-center pb-3 border-b-2 border-surface-500 md:w-[80%] w-[100%]">
		<input
			type="checkbox"
			{id}
			class="text-black mt-5 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px] cursor-pointer"
			bind:value={property['@_value']}
			on:change={onInputChange}
		/>
		<label class="mt-5 ml-5 text-[14px] cursor-pointer" for={id}>{property['@_description']}</label>
	</div>
{/if}
{#if property['@_type'] == 'Text'}
	<div class="flex flex-col justify-center md:w-[80%] w-[100%]">
		<label class="mt-5 text-[14px] cursor-pointer" for={id}>{property['@_description']}</label>
		<input
			type="text"
			{id}
			class="text-black mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
			bind:value={property['@_value']}
			on:input={onInputChange}
		/>
	</div>
{/if}

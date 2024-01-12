<script>
	import { updateConfiguration } from '../stores/configurationStore';

	export let property;
	export let propertyIndex;
	export let selectedIndex;
	export let hasChanges = false;

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
			class="text-black mt-5 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
			bind:value={property['@_value']}	
			on:change={onInputChange}
		/>
		<h2 class="mt-5 ml-5 text-[14px]">{property['@_description']}</h2>
	</div>
{/if}
{#if property['@_type'] == 'Text'}
	<div class="flex flex-col justify-center md:w-[80%] w-[100%]">
		<h2 class="mt-5 text-[14px]">{property['@_description']}</h2>
		<input
			type="text"
			class="text-black mt-2 p-1 text-[14px] placeholder:pl-1 placeholder:text-[12px]"
			bind:value={property['@_value']}
			on:input={onInputChange}
		/>
	</div>
{/if}

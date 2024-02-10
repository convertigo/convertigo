<script>
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';

	export let property;
	export let propertyIndex;
	export let selectedIndex;
	export let id = `property-input-${propertyIndex}`;
	export let hasUnsavedChanges;

	let booleanValue = property['@_value'] === 'true' ? 'true' : 'false';

	function handleBooleanChange(value) {
		property['@_value'] = value;
		hasUnsavedChanges = true;
	}
</script>

{#if property['@_type'] == 'Boolean'}
	<div class="flex items-center">
		<RadioGroup class="text-token" on:change={({ detail }) => handleBooleanChange(detail.value)}>
			<RadioItem bind:group={booleanValue} name={`boolean-${propertyIndex}`} value="true">
				True
			</RadioItem>
			<RadioItem bind:group={booleanValue} name={`boolean-${propertyIndex}`} value="false">
				False
			</RadioItem>
		</RadioGroup>
		<label class="label-common ml-5" for={id}>{property['@_description']}</label>
	</div>
{/if}

{#if property['@_type'] == 'Text'}
	<div class="flex flex-col justify-center border-common">
		<label class="label-common" for={id}>{property['@_description']}</label>
		<input
			type="text"
			{id}
			placeholder="Enter value ..."
			class="input-common input-text placeholder:pl-1"
			bind:value={property['@_value']}
			on:input={() => (hasUnsavedChanges = true)}
		/>
	</div>
{/if}

<style lang="postcss">
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

	/**Style for checkbox*/
	.checkbox-common {
		@apply cursor-pointer;
	}

	.border-common {
		@apply border-b-[1px] dark:border-surface-600 border-surface-100;
	}
</style>

<script>
	export let label = '';
	export let id = '';
	export let name = '';
	export let type = 'text';
	export let value = '';
	export let placeholder = 'Enter value ..';
</script>

<div class="input-field border-common mt-5 ">
	<label for={id} class="label-common">{label}</label>
		<input
        id={id}
        name={name}
        type={type}
        value={value}
        placeholder={placeholder}
        class="input-common input-text"
        />
</div>


<style lang="postcss">
	/**style for label*/
	.label-common {
		@apply text-[14px] cursor-pointer;
	}
	/**Style for Input*/
	.input-common {
		@apply placeholder:text-[16px] placeholder:dark:text-surface-100 placeholder:text-surface-100 placeholder:font-light font-normal border-none dark:bg-surface-800 w-full;
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

	.green{
		background-color: green;
	}
</style>

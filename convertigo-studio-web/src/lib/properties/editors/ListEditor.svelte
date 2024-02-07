<script>
	import { ListBox, ListBoxItem, popup } from '@skeletonlabs/skeleton';
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	export let name;
	export let value;
	export let values = [];
	export let editable = false;

	let input, select;
	let groupValue = value;

	if ($$restProps) {
	}

	function combo(id) {
		return {
			event: 'focus-click',
			target: id,
			placement: 'bottom',
			closeQuery: '.listbox-item'
		};
	}

	function groupChange() {
		input.value = groupValue;
		onChange();
	}

	function onChange() {
		let node = editable ? input : select;
		let val = node.value;
		groupValue = val;

		dispatch('valueChanged', {
			name: name,
			value: val
		});
	}
</script>

<div class="flex">
	{#if editable}
		<input
			bind:this={input}
			type="text"
			autocomplete="off"
			aria-autocomplete="none"
			class="rounded select form-select w-full text-[11.5px] py-0 border-[0.5px]"
			id={name + '-input'}
			{value}
			on:change={onChange}
			use:popup={combo(name + '-popup')}
		/>
		<div
			class="card w-48 shadow-xl py-2 dark:bg-surface-700 bg-surface-200"
			data-popup={name + '-popup'}
		>
			<ListBox
				rounded="rounded-none dark:bg-surface-700 bg-surface-200 text-[11.5px] dark:text-secondary-100 relative z-50"
			>
				{#each values as v}
					<ListBoxItem
						bind:group={groupValue}
						name="medium"
						on:change={groupChange}
						value={v}
						class="border-b dark:border-surface-800 border-surface-300"
						active="dark:text-secondary-300">{v}</ListBoxItem
					>
				{/each}
			</ListBox>
		</div>
	{:else}
		<select
			bind:this={select}
			class="select w-full text-[11.5px] py-0 border-[0.5px] dark:bginput rounded-[4px]"
			id={name + '-select'}
			{value}
			on:change={onChange}
		>
			{#each values as v}
				<option value={v}>{v}</option>
			{/each}
		</select>
	{/if}
</div>

<style lang="postcss">
	.bginput {
		@apply bg-surface-900;
	}
</style>

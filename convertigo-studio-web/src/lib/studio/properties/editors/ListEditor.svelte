<script>
	import { ListBox, ListBoxItem, popup } from '@skeletonlabs/skeleton';
	import { createEventDispatcher } from 'svelte';

	const dispatch = createEventDispatcher();

	/** @type {{Record<string, any>}} */
	let { name, value, values = [], editable = false, ...rest } = $props();

	let input = $state(),
		select = $state();
	let groupValue = $state(value);

	if (rest) {
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
			class="select w-full form-select rounded-sm border-[0.5px] py-0 text-[11.5px]"
			id={name + '-input'}
			{value}
			onchange={onChange}
			use:popup={combo(name + '-popup')}
		/>
		<div
			class="w-48 card bg-surface-200 py-2 shadow-xl dark:bg-surface-700"
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
						class="border-b border-surface-300 dark:border-surface-800"
						active="dark:text-secondary-300">{v}</ListBoxItem
					>
				{/each}
			</ListBox>
		</div>
	{:else}
		<select
			bind:this={select}
			class="dark:bginput select w-full rounded-[4px] border-[0.5px] py-0 text-[11.5px]"
			id={name + '-select'}
			{value}
			onchange={onChange}
		>
			{#each values as v}
				<option value={v}>{v}</option>
			{/each}
		</select>
	{/if}
</div>

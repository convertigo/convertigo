<script module>
	let cpt = 0;
</script>

<script>
	import Ico from '$lib/utils/Ico.svelte';
	import CheckState from './CheckState.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { Segment } from '@skeletonlabs/skeleton-svelte';

	/** @type {{value: string,  label?: string, description?: string, name?: string, item?: any, type?: string, defaultValue?:string, originalValue?:string, loading?:boolean, placeholder?: string}|any} */
	let {
		value = $bindable(),
		label: _label,
		description,
		name,
		item,
		type: _type = 'text',
		defaultValue,
		originalValue,
		loading = false,
		placeholder = 'Enter value …',
		...rest
	} = $props();
	let label = $derived(description ?? _label);
	let restores = $derived.by(() => {
		const r = [];
		if (originalValue != null) {
			r.push({ icon: 'mdi:arrow-u-left-top', val: originalValue, title: 'reset' });
		}
		if (defaultValue != null) {
			r.push({ icon: 'mdi:backup-restore', val: defaultValue, title: 'restore' });
		}
		return r;
	});
	let type = $derived(_type?.toLocaleLowerCase());
	let id = `property-input-${cpt++}`;
</script>

<div class="layout-y-low sm:layout-x-low w-full">
	<div class="max-sm:self-stretch sm:grow">
		{#if type == 'boolean'}
			<CheckState {name} bind:value>{label}</CheckState>
		{:else}
			{@const autocomplete = 'one-time-code'}
			<div class="layout-y-none !items-stretch">
				{#if label}
					<AutoPlaceholder {loading}>
						<label class="label-common" for={id}>{label}</label>
					</AutoPlaceholder>
				{/if}
				{#if type == 'segment'}
					<Segment {...rest} {name} bind:value>
						{#each item as option}
							<Segment.Item value={option.value} stateFocused="preset-filled-surface text-white">
								{option.text ?? option['#text']}
							</Segment.Item>
						{/each}
					</Segment>
				{:else if type == 'combo'}
					<select {name} class="input-common" {id} bind:value>
						{#each item as option}
							<option value={option.value}>{option.text ?? option['#text']}</option>
						{/each}
					</select>
				{:else if type == 'array'}
					<textarea
						{id}
						{name}
						{autocomplete}
						{placeholder}
						class="input-common input-text placeholder:pl-1"
						bind:value
					></textarea>
				{:else}
					<input
						{id}
						{name}
						{autocomplete}
						{placeholder}
						{type}
						disabled={loading}
						class:animate-pulse={loading}
						class="input-common input-text placeholder:pl-1"
						bind:value
					/>
				{/if}
			</div>
		{/if}
	</div>
	{#if restores.length > 0}
		<div class="layout-x-low sm:layout-y-low">
			{#each restores as { icon, val, title }}
				<button
					disabled={value == val}
					onclick={() => {
						value = val;
					}}
					title="{title}:{val}"
					class="btn btn-sm bg-surface-200-800"
				>
					<Ico {icon} />
				</button>
			{/each}
		</div>
	{/if}
</div>

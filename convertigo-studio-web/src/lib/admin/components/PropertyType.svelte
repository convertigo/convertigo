<script module>
	let cpt = 0;
</script>

<script>
	import Ico from '$lib/utils/Ico.svelte';
	import CheckState from './CheckState.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	/** @type {{property: any}} */
	let { property = $bindable() } = $props();
	let { type, description } = $derived(property);
	let id = `property-input-${cpt++}`;

	const buttons = [
		{ icon: 'mdi:arrow-u-left-top', type: 'originalValue', title: 'restore' },
		{ icon: 'mdi:backup-restore', type: 'defaultValue', title: 'reset' }
	];
</script>

<div class="layout-y-low sm:layout-x-low">
	<div class="max-sm:self-stretch sm:grow">
		{#if type == 'Boolean'}
			<CheckState name={id} bind:value={property.value}>{description}</CheckState>
		{:else}
			{@const loading = description == null}
			{@const autocomplete = 'one-time-code'}
			{@const placeholder = 'Enter value ...'}
			<div class="flex-1 flex flex-col justify-center border-common">
				<AutoPlaceholder {loading}>
					<label class="label-common" for={id}>{description}</label>
				</AutoPlaceholder>
				{#if type == 'Combo'}
					<select class="input-common" {id} bind:value={property.value}>
						{#each property.item as option}
							<option value={option.value}>{option['#text']}</option>
						{/each}
					</select>
				{:else if type == 'Array'}
					<textarea
						{id}
						{autocomplete}
						{placeholder}
						class="input-common input-text placeholder:pl-1"
						bind:value={property.value}
					></textarea>
				{:else}
					<input
						{id}
						{autocomplete}
						{placeholder}
						type={type == 'Text' ? 'text' : 'password'}
						disabled={loading}
						class:animate-pulse={loading}
						class="input-common input-text placeholder:pl-1"
						bind:value={property.value}
					/>
				{/if}
			</div>
		{/if}
	</div>
	<div class="layout-x-low sm:layout-y-low">
		{#each buttons as { icon, type, title }}
			<button
				disabled={property.value == property[type]}
				onclick={() => {
					property.value = property[type];
				}}
				title="{title}:{property[type]}"
				class="btn btn-sm bg-surface-200-800"
			>
				<Ico {icon} />
			</button>
		{/each}
	</div>
</div>

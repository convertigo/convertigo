<script module>
	let cpt = 0;
</script>

<script>
	import Ico from '$lib/utils/Ico.svelte';
	import CheckState from './CheckState.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	/** @type {{property: any}} */
	let { property = $bindable() } = $props();

	let id = `property-input-${cpt++}`;

	const buttons = [
		{ icon: 'mdi:arrow-u-left-top', type: '@_originalValue', title: 'restore' },
		{ icon: 'mdi:backup-restore', type: '@_defaultValue', title: 'reset' }
	];
</script>

<div class="layout-y-low sm:layout-x-low">
	<div class="max-sm:self-stretch sm:grow">
		{#if property['@_type'] == 'Boolean'}
			<CheckState name={id} bind:value={property['@_value']}>{property['@_description']}</CheckState
			>
		{:else if property['@_type'] == 'Text'}
			{@const loading = property['@_description'] == null}
			<div class="flex-1 flex flex-col justify-center border-common">
				<AutoPlaceholder {loading}>
					<label class="label-common" for={id}>{property['@_description']}</label>
				</AutoPlaceholder>

				<input
					type="text"
					{id}
					disabled={loading}
					class:animate-pulse={loading}
					placeholder="Enter value ..."
					class="input-common placeholder:pl-1"
					bind:value={property['@_value']}
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
				/>
			</div>
		{:else if property['@_type'] == 'Combo'}
			<div class="flex-1 flex flex-col justify-center border-common">
				<label class="label-common" for={id}>{property['@_description']}</label>

				<select class="input-common" {id} bind:value={property['@_value']}>
					{#each property.item as option}
						<option value={option['@_value']}>{option['#text']}</option>
					{/each}
				</select>
			</div>
		{:else if property['@_type'] == 'Array'}
			<div class="flex-1 flex flex-col justify-center border-common">Not handled</div>
		{/if}
	</div>
	<div class="layout-x-low sm:layout-y-low">
		{#each buttons as { icon, type, title }}
			<button
				disabled={property['@_value'] == property[type]}
				onclick={() => {
					property['@_value'] = property[type];
				}}
				title="{title}:{property[type]}"
				class="btn btn-sm bg-surface-200-800"
			>
				<Ico {icon} />
			</button>
		{/each}
	</div>
</div>

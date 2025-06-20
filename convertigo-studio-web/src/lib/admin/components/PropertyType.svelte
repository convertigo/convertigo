<script module>
	let cpt = 0;
</script>

<script>
	import { Segment, Switch } from '@skeletonlabs/skeleton-svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { checkArray } from '$lib/utils/service';
	import CheckState from './CheckState.svelte';

	/** @type {{value: string, checked?: boolean, label?: string, description?: string, name?: string, item?: any, type?: string, defaultValue?:string, originalValue?:string, loading?:boolean, placeholder?: string, multiple?: boolean}|any} */
	let {
		value = $bindable(''),
		checked = $bindable(false),
		label: _label,
		description,
		name,
		item,
		type: _type = 'text',
		defaultValue,
		originalValue,
		loading = false,
		placeholder = 'Enter value …',
		fit = false,
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

	function handleMultiple(e) {
		e.preventDefault();
		const select = e.target.parentElement;
		value = checkArray(value);
		if (e.target?.selected) {
			value = value.filter((v) => v != e.target?.value);
		} else {
			value.push(e.target?.value);
		}
		const scroll = select.scrollTop;
		setTimeout(() => {
			select.scrollTop = scroll;
		}, 1);
		rest.onchange?.({ target: select });
	}
</script>

<div class="layout-y-low sm:layout-x-low" class:w-fit={fit} class:w-full={!fit}>
	<div class="max-sm:self-stretch sm:grow">
		{#if type == 'boolean'}
			<CheckState {name} {...rest} bind:value>{label}</CheckState>
		{:else if type == 'check'}
			<Switch
				{...rest}
				{name}
				{value}
				controlClasses="min-w-10"
				thumbInactive="bg-white"
				thumbActive="bg-white"
				controlActive="preset-filled-success-200-800"
				controlInactive="preset-filled-error-200-800 motif-error"
				{checked}
				onCheckedChange={(e) => (checked = e.checked)}>{label}</Switch
			>
		{:else}
			{@const autocomplete = 'one-time-code'}
			<div class="layout-y-stretch-none" class:border-common={type != 'segment'}>
				{#if label}
					<AutoPlaceholder {loading}>
						<label class="label-common" for={id}>{label}</label>
					</AutoPlaceholder>
				{/if}
				{#if type == 'segment'}
					<Segment
						{...rest}
						name={name ?? []}
						{value}
						onValueChange={(e) => (value = e.value ?? '')}
						indicatorBg="preset-filled-primary-200-800"
						indicatorText="color-primary-200-800"
						padding="p-0"
					>
						{#each item as option}
							{@const val = option.value ?? option}
							{@const txt = option.text ?? option['#text'] ?? val}
							<Segment.Item value={val}>
								{txt}
							</Segment.Item>
						{/each}
					</Segment>
				{:else if type == 'combo'}
					<select {name} class="select input-common overflow-auto" {id} {...rest} bind:value>
						{#each item as option}
							{@const val = option.value ?? option}
							{@const txt = option.text ?? option['#text'] ?? val}
							{#if rest.multiple ?? true}
								<option class="ig-select" value={val} onmousedown={handleMultiple}>{txt}</option>
							{:else}
								<option class="ig-select" value={val}>{txt}</option>
							{/if}
						{/each}
					</select>
				{:else if type == 'array' || type == 'textarea'}
					<textarea
						{id}
						{name}
						{autocomplete}
						{placeholder}
						{...rest}
						class="input-text input-common placeholder:pl-1"
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
						class="input-text input-common placeholder:pl-1"
						{...rest}
						bind:value
					/>
				{/if}
			</div>
		{/if}
	</div>
	{#if restores.length > 0}
		<div class="layout-x-low justify-around! sm:layout-y-low sm:h-full">
			{#each restores as { icon, val, title }}
				<button
					disabled={value == val}
					type="button"
					onclick={() => (value = val)}
					title="{title}:{val}"
					class="btn bg-surface-200-800 btn-sm"
				>
					<Ico {icon} />
				</button>
			{/each}
		</div>
	{/if}
</div>

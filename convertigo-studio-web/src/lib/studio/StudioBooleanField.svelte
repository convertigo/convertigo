<script>
	import StudioIconButton from './StudioIconButton.svelte';

	/**
	 * A boolean property of the Properties view: a check box and its value, as the property sheet of the
	 * Eclipse Studio shows true or false, with the buttons of the property after them.
	 * @type {{
	 *  value: any,
	 *  onChange: (value: string) => void,
	 *  label?: string,
	 *  disabled?: boolean,
	 *  buttons?: { icon: string, title: string, active?: boolean, ariaExpanded?: boolean, disabled?: boolean, onclick: () => void }[]
	 * }}
	 */
	let { value, onChange, label = '', disabled = false, buttons = [] } = $props();

	let checked = $derived(String(value) === 'true');
</script>

<div class="studio-boolean">
	<label class="studio-boolean__check">
		<input
			type="checkbox"
			{checked}
			{disabled}
			aria-label={label || undefined}
			onchange={(event) => onChange(event.currentTarget.checked ? 'true' : 'false')}
		/>
		<span class="studio-boolean__value" class:studio-boolean__value--off={!checked}
			>{checked ? 'true' : 'false'}</span
		>
	</label>
	{#each buttons as button (button.title)}
		<StudioIconButton
			icon={button.icon}
			size="xs"
			title={button.title}
			ariaLabel={button.title}
			active={button.active}
			aria-expanded={button.ariaExpanded}
			disabled={button.disabled}
			onclick={button.onclick}
		/>
	{/each}
</div>

<style>
	.studio-boolean {
		display: flex;
		min-height: 2rem;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-boolean__check {
		display: inline-flex;
		align-items: center;
		gap: 0.5rem;
		cursor: pointer;
		font-size: 0.8rem;
	}

	.studio-boolean__check input {
		flex: none;
		width: 1rem !important;
		height: 1rem;
		margin: 0;
		cursor: pointer;
	}

	.studio-boolean__value {
		color: var(--studio-text-strong, var(--color-surface-950-50));
	}

	.studio-boolean__value--off {
		color: var(--studio-text-idle, var(--color-surface-600-400));
	}
</style>

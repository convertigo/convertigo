<script>
	import Button from '$lib/admin/components/Button.svelte';

	/** @type {{
	 * icon: string;
	 * title?: string;
	 * ariaLabel?: string;
	 * active?: boolean;
	 * danger?: boolean;
	 * dirty?: boolean;
	 * size?: 'xs' | 'sm' | 'md';
	 * class?: string;
	 * disabled?: boolean;
	 * [key: string]: any;
	 * }} */
	let {
		icon,
		title,
		ariaLabel,
		active = false,
		danger = false,
		dirty = false,
		size = 'sm',
		class: cls = '',
		...rest
	} = $props();

	const sizes = {
		xs: { size: '1.45rem', radius: '0.28rem' },
		sm: { size: '1.75rem', radius: '0.35rem' },
		md: { size: '2.25rem', radius: '0.35rem' }
	};
	let classes = $derived(
		[
			'studio-action-button',
			active && 'studio-action-button--active',
			danger && 'studio-action-button--danger',
			dirty && 'studio-action-button--dirty',
			cls
		]
			.filter(Boolean)
			.join(' ')
	);
	let buttonSize = $derived(sizes[size] ?? sizes.sm);
	let buttonStyle = $derived(
		`--studio-action-size:${buttonSize.size};border-radius:${buttonSize.radius};`
	);
</script>

<Button
	full={false}
	{icon}
	{title}
	ariaLabel={ariaLabel ?? title}
	class={classes}
	style={buttonStyle}
	{...rest}
/>

<style>
	:global(.studio-action-button) {
		--studio-action-size: 2rem;
		display: inline-flex;
		width: var(--studio-action-size) !important;
		height: var(--studio-action-size) !important;
		min-height: var(--studio-action-size) !important;
		flex: 0 0 auto;
		align-items: center;
		justify-content: center;
		gap: 0;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.35rem;
		background: var(--color-surface-50-950);
		color: var(--color-surface-700-300);
		padding: 0 !important;
	}

	:global(.studio-action-button:hover:not(:disabled)),
	:global(.studio-action-button:focus-visible:not(:disabled)),
	:global(.studio-action-button.studio-action-button--active) {
		border-color: color-mix(in oklab, var(--color-primary-500) 45%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	:global(.studio-action-button:disabled) {
		color: var(--color-surface-400-600);
		cursor: not-allowed;
		opacity: 0.6;
	}

	:global(.studio-action-button.studio-action-button--danger:hover:not(:disabled)),
	:global(.studio-action-button.studio-action-button--danger:focus-visible:not(:disabled)) {
		border-color: color-mix(in oklab, var(--color-error-500) 45%, transparent);
		background: color-mix(in oklab, var(--color-error-500) 14%, transparent);
		color: var(--color-error-600-400);
	}

	:global(.studio-action-button.studio-action-button--dirty) {
		position: relative;
		border-color: color-mix(in oklab, var(--color-primary-500) 55%, transparent);
		color: var(--color-primary-600-400);
	}

	:global(.studio-action-button.studio-action-button--dirty::after) {
		position: absolute;
		top: 0.22rem;
		right: 0.22rem;
		width: 0.42rem;
		height: 0.42rem;
		border: 1px solid var(--studio-panel-bg, var(--color-surface-50-950));
		border-radius: 999px;
		background: var(--color-primary-500);
		content: '';
	}
</style>

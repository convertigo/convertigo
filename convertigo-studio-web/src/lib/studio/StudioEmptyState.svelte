<script>
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{
	 * message?: string;
	 * icon?: string;
	 * loading?: boolean;
	 * full?: boolean;
	 * small?: boolean;
	 * class?: string;
	 * children?: import('svelte').Snippet;
	 * }} */
	let {
		message = '',
		icon = '',
		loading = false,
		full = false,
		small = false,
		class: cls = '',
		children
	} = $props();

	let classes = $derived(
		[
			'studio-empty-state',
			full && 'studio-empty-state-full',
			small && 'studio-empty-state-small',
			cls
		]
			.filter(Boolean)
			.join(' ')
	);
</script>

<div
	class={classes}
	role={loading ? 'status' : undefined}
	aria-live={loading ? 'polite' : undefined}
>
	{#if loading}
		<span class="studio-spinner" aria-hidden="true"></span>
	{:else if icon}
		<Ico {icon} size={8} />
	{/if}
	{#if children}
		{@render children()}
	{:else if message}
		<span>{message}</span>
	{/if}
</div>

<style>
	.studio-empty-state {
		display: grid;
		min-height: 7rem;
		place-items: center;
		gap: 0.65rem;
		color: var(--color-surface-600-400);
		padding: 1rem;
		font-size: 0.86rem;
		text-align: center;
	}

	.studio-empty-state-full {
		height: 100%;
		min-height: 0;
		align-content: center;
	}

	.studio-empty-state-small {
		min-height: 3rem;
		padding: 0.5rem;
		font-size: 0.78rem;
	}

	.studio-spinner {
		width: 1.45rem;
		height: 1.45rem;
		border: 2px solid color-mix(in oklab, var(--color-primary-500) 22%, transparent);
		border-top-color: var(--color-primary-500);
		border-radius: 999px;
		animation: studio-empty-spin 0.8s linear infinite;
	}

	@keyframes studio-empty-spin {
		to {
			transform: rotate(360deg);
		}
	}
</style>

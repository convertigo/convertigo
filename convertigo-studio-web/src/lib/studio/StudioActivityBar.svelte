<script>
	import Ico from '$lib/utils/Ico.svelte';

	/**
	 * The activity bar on the left edge of the Studio, as the one of Cursor or Visual Studio Code: each
	 * item shows or hides a panel, the item of a visible panel being lit and marked with the accent.
	 *
	 * @typedef {{ id: string, label: string, icon: string, active?: boolean, href?: string }} ActivityItem
	 */

	/** @type {{
	 * items: ActivityItem[];
	 * footerItems?: ActivityItem[];
	 * onSelect?: (id: string) => void;
	 * }} */
	let { items = [], footerItems = [], onSelect } = $props();
</script>

{#snippet activity(/** @type {ActivityItem} */ item)}
	{#if item.href}
		<a class="studio-activity" href={item.href} title={item.label} aria-label={item.label}>
			<Ico icon={item.icon} size={6} />
		</a>
	{:else}
		<button
			type="button"
			class={['studio-activity', item.active && 'studio-activity--active']}
			title={item.label}
			aria-label={item.label}
			aria-pressed={Boolean(item.active)}
			onclick={() => onSelect?.(item.id)}
		>
			<Ico icon={item.icon} size={6} />
		</button>
	{/if}
{/snippet}

<nav class="studio-activity-bar" aria-label="Studio panels">
	<div class="studio-activity-bar__group">
		{#each items as item (item.id)}
			{@render activity(item)}
		{/each}
	</div>
	<div class="studio-activity-bar__group">
		{#each footerItems as item (item.id)}
			{@render activity(item)}
		{/each}
	</div>
</nav>

<style>
	.studio-activity-bar {
		display: flex;
		height: 100%;
		min-height: 0;
		flex-direction: column;
		justify-content: space-between;
		border-right: 1px solid var(--studio-line);
		background: var(--studio-chrome-bg);
		padding-block: 0.25rem;
	}

	.studio-activity-bar__group {
		display: flex;
		flex-direction: column;
	}

	.studio-activity {
		position: relative;
		display: grid;
		width: 100%;
		height: 3rem;
		place-items: center;
		border: 0;
		background: transparent;
		color: var(--studio-text-idle);
		padding: 0;
		transition: color 0.12s ease;
	}

	.studio-activity:hover,
	.studio-activity:focus-visible {
		color: var(--studio-text);
	}

	.studio-activity--active {
		color: var(--studio-text-strong);
	}

	.studio-activity--active::before {
		position: absolute;
		top: 0.5rem;
		bottom: 0.5rem;
		left: 0;
		width: 2px;
		background: var(--color-primary-500);
		content: '';
	}
</style>

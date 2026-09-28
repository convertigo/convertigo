<script>
	import Ico from '$lib/utils/Ico.svelte';

	/**
	 * The tab of a view of the dock: its icon, its name and the button that closes it.
	 * @type {{ tab: { title: string, icon?: string, onClose: () => void } }}
	 */
	let { tab } = $props();
</script>

<span class="studio-dock-tab layout-x-low">
	{#if tab.icon}
		<Ico icon={tab.icon} size={4} />
	{/if}
	<span class="studio-dock-tab__label">{tab.title}</span>
	<button
		type="button"
		class="studio-dock-tab__close"
		title={`Close ${tab.title}`}
		aria-label={`Close the ${tab.title} view`}
		onpointerdown={(event) => event.stopPropagation()}
		onclick={(event) => {
			event.stopPropagation();
			tab.onClose();
		}}
	>
		<Ico icon="mdi:close" size={3} />
	</button>
</span>

<style>
	/* Flat tabs, as the panel tabs of Cursor: the visible one is underlined with the accent */
	.studio-dock-tab {
		position: relative;
		height: 100%;
		min-width: 0;
		gap: 0.35rem;
		padding: 0 0.15rem 0 0.35rem;
		font-size: 0.72rem;
		font-weight: 600;
		letter-spacing: 0.02em;
		text-transform: uppercase;
		white-space: nowrap;
	}

	.studio-dock-tab__label {
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.studio-dock-tab__close {
		display: grid;
		width: 1.1rem;
		height: 1.1rem;
		place-items: center;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: inherit;
		opacity: 0;
		padding: 0;
	}

	.studio-dock-tab:hover .studio-dock-tab__close,
	.studio-dock-tab__close:focus-visible,
	:global(.dv-active-tab) .studio-dock-tab__close {
		opacity: 0.7;
	}

	.studio-dock-tab__close:hover {
		background: var(--studio-hover-bg);
		opacity: 1;
	}
</style>

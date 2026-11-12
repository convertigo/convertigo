<script>
	import Ico from '#lib/utils/Ico.svelte';

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

	const tooltipId = $props.id();
	/** the name of the item under the pointer or focused, beside it, as the activity bar of Visual Studio Code */
	let hint = $state(/** @type {{ label: string, top: number, left: number } | null} */ (null));
	/** @type {ReturnType<typeof setTimeout> | undefined} */
	let hintTimer;

	/**
	 * @param {HTMLElement} element
	 * @param {string} label
	 * @param {number} delay
	 */
	function showHint(element, label, delay) {
		clearTimeout(hintTimer);
		hintTimer = setTimeout(
			() => {
				const rect = element.getBoundingClientRect();
				hint = { label, top: rect.top + rect.height / 2, left: rect.right + 6 };
			},
			hint ? 0 : delay
		);
	}

	function hideHint() {
		clearTimeout(hintTimer);
		hint = null;
	}
</script>

{#snippet activity(/** @type {ActivityItem} */ item)}
	{#if item.href}
		<!-- a page of its own, as the admin console, leaves the Studio open: a window without address bar
		     (Electron) could not come back -->
		<a
			class="studio-activity"
			href={item.href}
			target="_blank"
			rel="noopener"
			aria-label={item.label}
			aria-describedby={hint?.label === item.label ? tooltipId : undefined}
			onpointerenter={(event) => showHint(event.currentTarget, item.label, 350)}
			onpointerleave={hideHint}
			onfocus={(event) => showHint(event.currentTarget, item.label, 0)}
			onblur={hideHint}
			onclick={hideHint}
		>
			<Ico icon={item.icon} size={6} />
		</a>
	{:else}
		<button
			type="button"
			class={['studio-activity', item.active && 'studio-activity--active']}
			aria-label={item.label}
			aria-describedby={hint?.label === item.label ? tooltipId : undefined}
			aria-pressed={Boolean(item.active)}
			onpointerenter={(event) => showHint(event.currentTarget, item.label, 350)}
			onpointerleave={hideHint}
			onfocus={(event) => showHint(event.currentTarget, item.label, 0)}
			onblur={hideHint}
			onclick={() => {
				hideHint();
				onSelect?.(item.id);
			}}
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
{#if hint}
	<div
		id={tooltipId}
		class="studio-activity-tooltip"
		role="tooltip"
		style:top={`${hint.top}px`}
		style:left={`${hint.left}px`}
	>
		{hint.label}
	</div>
{/if}

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

	/* fixed, out of the panels that would clip it */
	.studio-activity-tooltip {
		position: fixed;
		z-index: 200;
		transform: translateY(-50%);
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 0.4rem 1.2rem color-mix(in oklab, black 25%, transparent);
		color: var(--studio-text-strong);
		font-size: 0.75rem;
		line-height: 1.2;
		padding: 0.3rem 0.55rem;
		pointer-events: none;
		white-space: nowrap;
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

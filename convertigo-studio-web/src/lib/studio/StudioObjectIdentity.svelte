<script>
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';

	/**
	 * @type {{
	 *  item?: { name?: string, classname?: string, id?: string, instanceName?: string, icon?: string } | null,
	 *  compact?: boolean
	 * }}
	 */
	let { item = null, compact = false } = $props();

	let title = $derived(item?.name || item?.classname || 'Component');
	let subtitle = $derived(item?.instanceName || item?.classname || item?.id || '');
	let iconUrl = $derived(iconSource(item?.icon));

	function iconSource(icon) {
		if (!icon) return '';
		return `${getUrl()}studio.dbo.GetIcon?iconPath=${encodeURIComponent(icon)}`;
	}
</script>

{#if item}
	<header
		class="studio-object-identity layout-x-low"
		class:studio-object-identity--compact={compact}
	>
		<span class="studio-object-identity__icon studio-icon-tile">
			{#if iconUrl}
				<AutoSvg
					class="studio-object-identity__icon-image"
					fill="currentColor"
					src={iconUrl}
					alt=""
				/>
			{:else}
				<Ico icon="mdi:cube-outline" size={compact ? 4 : 5} />
			{/if}
		</span>
		<div class="studio-object-identity__title">
			<h2 class="studio-ellipsis">{title}</h2>
			{#if subtitle && subtitle !== title}
				<p class="studio-ellipsis">{subtitle}</p>
			{/if}
		</div>
	</header>
{/if}

<style>
	.studio-object-identity {
		min-width: 0;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		padding: 0.75rem 0.9rem;
	}

	.studio-object-identity--compact {
		padding: 0.52rem 0.65rem;
	}

	.studio-object-identity__icon {
		width: 2.35rem;
		height: 2.35rem;
		background: var(--studio-panel-header-bg, var(--color-surface-100-900));
	}

	.studio-object-identity--compact .studio-object-identity__icon {
		width: 1.8rem;
		height: 1.8rem;
	}

	:global(.studio-object-identity__icon-image) {
		display: block;
		width: 1.55rem;
		height: 1.55rem;
		object-fit: contain;
	}

	.studio-object-identity--compact :global(.studio-object-identity__icon-image) {
		width: 1.15rem;
		height: 1.15rem;
	}

	.studio-object-identity__title {
		min-width: 0;
	}

	.studio-object-identity__title h2 {
		margin: 0;
		color: var(--color-surface-950-50);
		font-size: 1rem;
		font-weight: 750;
		line-height: 1.2;
	}

	.studio-object-identity--compact .studio-object-identity__title h2 {
		font-size: 0.82rem;
	}

	.studio-object-identity__title p {
		margin: 0.18rem 0 0;
		color: var(--color-surface-600-400);
		font-size: 0.74rem;
		line-height: 1.2;
	}

	.studio-object-identity--compact .studio-object-identity__title p {
		font-size: 0.66rem;
	}
</style>

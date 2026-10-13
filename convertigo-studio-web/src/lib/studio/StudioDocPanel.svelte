<script>
	import RichText from '$lib/common/components/RichText.svelte';
	import { documentationBlocks as parseDocumentation } from '$lib/common/docBlocks.js';
	import AutoSvg from '$lib/utils/AutoSvg.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * @typedef {Object} PaletteItem
	 * @property {string=} id
	 * @property {string=} name
	 * @property {string=} classname
	 * @property {string=} description
	 * @property {string=} shortDescriptionHtml
	 * @property {string=} longDescriptionText
	 * @property {string=} longDescriptionHtml
	 * @property {string=} shortDescriptionText
	 * @property {string=} propertiesDescriptionHtml
	 * @property {string=} icon
	 * @property {boolean=} builtin
	 * @property {boolean=} additional
	 */

	/**
	 * @type {{
	 * 	paletteItem?: PaletteItem | null,
	 * 	loading?: boolean,
	 * 	error?: string,
	 * 	emptyMessage?: string
	 * }}
	 */
	let {
		paletteItem = null,
		loading = false,
		error = '',
		emptyMessage = 'Select a palette component to display its documentation.'
	} = $props();

	let displayName = $derived(itemDisplayName(paletteItem));
	let technicalName = $derived(paletteItem?.classname ?? paletteItem?.id ?? '');
	let documentationBlocks = $derived(itemDocumentationBlocks(paletteItem));
	let iconUrl = $derived(iconSource(paletteItem?.icon));

	/**
	 * @param {PaletteItem | null | undefined} item
	 * @returns {string}
	 */
	function itemDisplayName(item) {
		return item?.name || item?.classname || 'Documentation';
	}

	/**
	 * @param {PaletteItem | null | undefined} item
	 * @returns {any[]}
	 */
	function itemDocumentationBlocks(item) {
		if (!item) {
			return [];
		}
		const fallback = splitRawDescription(item.description);
		const short = item.shortDescriptionHtml || item.shortDescriptionText || fallback.shortHtml;
		const long = item.longDescriptionHtml || item.longDescriptionText || fallback.longHtml;
		const blocks = parseDocumentation(short);
		if (blocks[0]?.type === 'paragraph') {
			blocks[0] = { ...blocks[0], className: 'studio-doc__summary' };
		}
		blocks.push(...parseDocumentation(long));
		if (item.propertiesDescriptionHtml) {
			blocks.push({ type: 'heading', segments: [{ type: 'text', text: 'Properties:' }] });
			blocks.push(...parseDocumentation(item.propertiesDescriptionHtml));
		}
		return blocks;
	}

	/**
	 * @param {string | undefined} icon
	 * @returns {string}
	 */
	function iconSource(icon) {
		if (!icon) {
			return '';
		}
		return `${getUrl()}studio.dbo.GetIcon?iconPath=${encodeURIComponent(icon)}`;
	}

	/**
	 * @param {string | undefined} value
	 * @returns {{ shortHtml: string, longHtml: string }}
	 */
	function splitRawDescription(value) {
		const raw = String(value ?? '').trim();
		if (!raw) {
			return { shortHtml: '', longHtml: '' };
		}
		const separator = raw.indexOf('|');
		if (separator < 0) {
			return { shortHtml: raw, longHtml: '' };
		}
		return {
			shortHtml: raw.slice(0, separator).trim(),
			longHtml: raw.slice(separator + 1).trim()
		};
	}
</script>

<section class="studio-doc">
	{#if paletteItem}
		<header class="studio-doc__header layout-x-low">
			<span class="studio-doc__icon studio-icon-tile">
				{#if iconUrl}
					<AutoSvg class="studio-doc__icon-image" fill="currentColor" src={iconUrl} alt="" />
				{:else}
					<Ico icon="mdi:cube-outline" size={5} />
				{/if}
			</span>
			<div class="studio-doc__title">
				<h2 class="studio-ellipsis">{displayName}</h2>
				{#if technicalName}
					<p class="studio-ellipsis">{technicalName}</p>
				{/if}
			</div>
		</header>

		{#if documentationBlocks.length}
			<article class="studio-doc__content">
				<RichText blocks={documentationBlocks} />
			</article>
		{:else}
			<StudioEmptyState message="No documentation available for this component." />
		{/if}
	{:else if loading}
		<StudioEmptyState message="Loading documentation..." loading full />
	{:else if error}
		<StudioEmptyState message={error} icon="mdi:warning-outline" full />
	{:else}
		<StudioEmptyState message={emptyMessage} icon="mdi:book-open-variant" full />
	{/if}
</section>

<style>
	.studio-doc {
		display: grid;
		height: 100%;
		min-width: 0;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
		background: var(--studio-panel-bg, var(--color-surface-50-950));
		color: var(--color-surface-900-100);
	}

	.studio-doc__header {
		min-width: 0;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		padding: 0.75rem 0.9rem;
	}

	.studio-doc__icon {
		width: 2.35rem;
		height: 2.35rem;
		background: var(--studio-panel-header-bg, var(--color-surface-100-900));
	}

	.studio-doc__icon :global(.studio-doc__icon-image) {
		display: block;
		width: 1.55rem;
		height: 1.55rem;
		object-fit: contain;
	}

	.studio-doc__title {
		min-width: 0;
	}

	.studio-doc__title h2 {
		margin: 0;
		color: var(--color-surface-950-50);
		font-size: 1rem;
		font-weight: 750;
		line-height: 1.2;
	}

	.studio-doc__title p {
		margin: 0.18rem 0 0;
		color: var(--color-surface-600-400);
		font-size: 0.74rem;
		line-height: 1.2;
	}

	.studio-doc__content {
		min-width: 0;
		min-height: 0;
		overflow: auto;
		padding: 1rem 1.1rem;
		font-size: 0.9rem;
		line-height: 1.55;
	}
</style>

<script>
	import Ico from '$lib/utils/Ico.svelte';
	import { getUrl } from '$lib/utils/service';

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
	let iconMaskUrl = $derived(cssUrl(iconUrl));

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
		const shortHtml =
			item.shortDescriptionHtml ||
			textToInlineHtml(item.shortDescriptionText) ||
			fallback.shortHtml;
		const longHtml =
			item.longDescriptionHtml || textToBlockHtml(item.longDescriptionText) || fallback.longHtml;
		const propertiesHtml = item.propertiesDescriptionHtml ?? '';
		const blocks = [];
		if (shortHtml) {
			blocks.push({
				type: 'paragraph',
				className: 'studio-doc__summary',
				segments: inlineSegments(`<i>${shortHtml}</i>`)
			});
		}
		if (longHtml) {
			blocks.push(...htmlToBlocks(longHtml));
		}
		if (propertiesHtml) {
			blocks.push({
				type: 'heading',
				segments: [{ type: 'text', text: 'Properties:' }]
			});
			blocks.push(...htmlToBlocks(propertiesHtml));
		}
		return blocks.filter((block) => !isEmptyBlock(block));
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
	 * @param {string} value
	 * @returns {string}
	 */
	function cssUrl(value) {
		return value ? `url("${value.replaceAll('"', '%22')}")` : 'none';
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

	/**
	 * @param {string | undefined} value
	 * @returns {string}
	 */
	function textToInlineHtml(value) {
		const text = String(value ?? '').trim();
		return text ? escapeHtml(text).replace(/\n/g, '<br/>') : '';
	}

	/**
	 * @param {string | undefined} value
	 * @returns {string}
	 */
	function textToBlockHtml(value) {
		const text = String(value ?? '').trim();
		if (!text) {
			return '';
		}
		return text
			.split(/\n{2,}/)
			.map((paragraph) => `<p>${escapeHtml(paragraph).replace(/\n/g, '<br/>')}</p>`)
			.join('');
	}

	/**
	 * @param {string} value
	 * @returns {string}
	 */
	function escapeHtml(value) {
		return value
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#39;');
	}

	/**
	 * @param {string} html
	 * @returns {any[]}
	 */
	function htmlToBlocks(html) {
		const blocks = [];
		const source = String(html ?? '').replace(/\r\n?/g, '\n');
		const blockPattern =
			/<blockquote\b[^>]*class=(?:"|')?doc-note(?:"|')?[^>]*>([\s\S]*?)<\/blockquote>|<h([1-6])\b[^>]*>([\s\S]*?)<\/h\2>|<p\b([^>]*)>([\s\S]*?)<\/p>|<(ul|ol)\b[^>]*>([\s\S]*?)<\/\6>/gi;
		let index = 0;
		let match;
		while ((match = blockPattern.exec(source))) {
			addLooseTextBlock(blocks, source.slice(index, match.index));
			if (match[1] !== undefined) {
				blocks.push({ type: 'note', blocks: htmlToBlocks(match[1]) });
			} else if (match[2] !== undefined) {
				blocks.push({ type: 'heading', segments: inlineSegments(match[3]) });
			} else if (match[5] !== undefined) {
				blocks.push({
					type: 'paragraph',
					className: htmlAttribute(match[4], 'class'),
					segments: inlineSegments(match[5])
				});
			} else if (match[7] !== undefined) {
				blocks.push({
					type: match[6].toLowerCase() === 'ol' ? 'ordered-list' : 'list',
					items: listItems(match[7])
				});
			}
			index = blockPattern.lastIndex;
		}
		addLooseTextBlock(blocks, source.slice(index));
		return blocks.filter((block) => !isEmptyBlock(block));
	}

	/**
	 * @param {any[]} blocks
	 * @param {string} html
	 */
	function addLooseTextBlock(blocks, html) {
		const text = plainText(html);
		if (!text) {
			return;
		}
		blocks.push({ type: 'paragraph', className: '', segments: [{ type: 'text', text }] });
	}

	/**
	 * @param {string} html
	 * @returns {any[]}
	 */
	function listItems(html) {
		const items = [];
		const itemPattern = /<li\b[^>]*>([\s\S]*?)<\/li>/gi;
		let match;
		while ((match = itemPattern.exec(html))) {
			const segments = inlineSegments(match[1]);
			if (segments.length) {
				items.push(segments);
			}
		}
		return items;
	}

	/**
	 * @param {string} html
	 * @returns {any[]}
	 */
	function inlineSegments(html) {
		const segments = [];
		const source = String(html ?? '');
		const inlinePattern = /<br\s*\/?>|<(code|strong|b|em|i)\b[^>]*>([\s\S]*?)<\/\1>/gi;
		let index = 0;
		let match;
		while ((match = inlinePattern.exec(source))) {
			addTextSegment(segments, source.slice(index, match.index));
			if (match[0].match(/^<br/i)) {
				segments.push({ type: 'break' });
			} else {
				const tag = match[1].toLowerCase();
				segments.push({ type: inlineType(tag), text: plainText(match[2]) });
			}
			index = inlinePattern.lastIndex;
		}
		addTextSegment(segments, source.slice(index));
		return segments.filter((segment) => segment.type === 'break' || segment.text);
	}

	/**
	 * @param {any[]} segments
	 * @param {string} html
	 */
	function addTextSegment(segments, html) {
		const text = plainText(html);
		if (text) {
			segments.push({ type: 'text', text });
		}
	}

	/**
	 * @param {string} tag
	 * @returns {string}
	 */
	function inlineType(tag) {
		if (tag === 'code') {
			return 'code';
		}
		if (tag === 'strong' || tag === 'b') {
			return 'strong';
		}
		return 'emphasis';
	}

	/**
	 * @param {string | undefined} attrs
	 * @param {string} name
	 * @returns {string}
	 */
	function htmlAttribute(attrs, name) {
		const match = String(attrs ?? '').match(
			new RegExp(`${name}\\\\s*=\\\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\\\s>]+))`, 'i')
		);
		return match?.[1] ?? match?.[2] ?? match?.[3] ?? '';
	}

	/**
	 * @param {string} html
	 * @returns {string}
	 */
	function plainText(html) {
		return decodeHtmlEntities(
			String(html ?? '')
				.replace(/<br\s*\/?>/gi, '\n')
				.replace(/<[^>]+>/g, '')
				.replace(/[ \t]+\n/g, '\n')
				.replace(/\n{3,}/g, '\n\n')
				.trim()
		);
	}

	/**
	 * @param {string} value
	 * @returns {string}
	 */
	function decodeHtmlEntities(value) {
		return value
			.replace(/&nbsp;/gi, ' ')
			.replace(/&lt;/gi, '<')
			.replace(/&gt;/gi, '>')
			.replace(/&quot;/gi, '"')
			.replace(/&#39;/g, "'")
			.replace(/&amp;/gi, '&');
	}

	/**
	 * @param {any} block
	 * @returns {boolean}
	 */
	function isEmptyBlock(block) {
		if (block.type === 'note') {
			return block.blocks.length === 0;
		}
		if (block.type === 'list' || block.type === 'ordered-list') {
			return block.items.length === 0;
		}
		return (block.segments ?? []).length === 0;
	}
</script>

{#snippet inlineSegment(segment)}
	{#if segment.type === 'break'}
		<br />
	{:else if segment.type === 'code'}
		<code>{segment.text}</code>
	{:else if segment.type === 'strong'}
		<strong>{segment.text}</strong>
	{:else if segment.type === 'emphasis'}
		<em>{segment.text}</em>
	{:else}
		{segment.text}
	{/if}
{/snippet}

{#snippet inlineSegmentsView(segments)}
	{#each segments as segment, index (index)}
		{@render inlineSegment(segment)}
	{/each}
{/snippet}

{#snippet docBlock(block)}
	{#if block.type === 'heading'}
		<h3>{@render inlineSegmentsView(block.segments)}</h3>
	{:else if block.type === 'paragraph'}
		<p class={block.className}>{@render inlineSegmentsView(block.segments)}</p>
	{:else if block.type === 'list'}
		<ul>
			{#each block.items as item, index (index)}
				<li>{@render inlineSegmentsView(item)}</li>
			{/each}
		</ul>
	{:else if block.type === 'ordered-list'}
		<ol>
			{#each block.items as item, index (index)}
				<li>{@render inlineSegmentsView(item)}</li>
			{/each}
		</ol>
	{:else if block.type === 'note'}
		<blockquote class="doc-note">
			{#each block.blocks as child, index (index)}
				{@render docBlock(child)}
			{/each}
		</blockquote>
	{/if}
{/snippet}

<section class="studio-doc">
	{#if paletteItem}
		<header class="studio-doc__header">
			<span class="studio-doc__icon">
				{#if iconUrl}
					<span
						class="studio-doc__icon-mask"
						style:--studio-doc-icon-url={iconMaskUrl}
						aria-hidden="true"
					></span>
				{:else}
					<Ico icon="mdi:cube-outline" size={5} />
				{/if}
			</span>
			<div class="studio-doc__title">
				<h2>{displayName}</h2>
				{#if technicalName}
					<p>{technicalName}</p>
				{/if}
			</div>
		</header>

		{#if documentationBlocks.length}
			<article class="studio-doc__content">
				{#each documentationBlocks as block, index (index)}
					{@render docBlock(block)}
				{/each}
			</article>
		{:else}
			<div class="studio-doc__empty">No documentation available for this component.</div>
		{/if}
	{:else if loading}
		<div class="studio-doc__empty studio-doc__empty--full">
			<span class="studio-doc__loading" aria-label="Loading documentation"></span>
			<span>Loading documentation...</span>
		</div>
	{:else if error}
		<div class="studio-doc__empty studio-doc__empty--full">
			<Ico icon="mdi:warning-outline" size={8} />
			<span>{error}</span>
		</div>
	{:else}
		<div class="studio-doc__empty studio-doc__empty--full">
			<Ico icon="mdi:book-open-variant" size={8} />
			<span>{emptyMessage}</span>
		</div>
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
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.65rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-50-950) 94%, transparent);
		padding: 0.75rem 0.9rem;
	}

	.studio-doc__icon {
		display: grid;
		width: 2.35rem;
		height: 2.35rem;
		flex: 0 0 auto;
		place-items: center;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.45rem;
		background: var(--studio-panel-header-bg, var(--color-surface-100-900));
		color: light-dark(var(--color-primary-600), var(--color-primary-400));
	}

	.studio-doc__icon-mask {
		display: block;
		width: 1.55rem;
		height: 1.55rem;
		background: currentcolor;
		mask: var(--studio-doc-icon-url) center / contain no-repeat;
		-webkit-mask: var(--studio-doc-icon-url) center / contain no-repeat;
	}

	.studio-doc__title {
		min-width: 0;
	}

	.studio-doc__title h2,
	.studio-doc__title p {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
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

	.studio-doc__content :global(p) {
		margin: 0 0 0.8rem;
	}

	.studio-doc__content :global(.studio-doc__summary) {
		color: var(--color-primary-700-300);
		font-size: 0.95rem;
		font-weight: 650;
	}

	.studio-doc__content :global(h3) {
		margin: 1.1rem 0 0.55rem;
		color: var(--color-surface-950-50);
		font-size: 0.86rem;
		font-weight: 800;
	}

	.studio-doc__content :global(ul),
	.studio-doc__content :global(ol) {
		margin: 0 0 0.9rem 1.05rem;
		padding-left: 1.05rem;
	}

	.studio-doc__content :global(li) {
		margin: 0 0 0.72rem;
	}

	.studio-doc__content :global(strong),
	.studio-doc__content :global(b) {
		font-weight: 800;
	}

	.studio-doc__content :global(i),
	.studio-doc__content :global(em) {
		color: var(--color-primary-700-300);
		font-style: italic;
		font-weight: 700;
	}

	.studio-doc__content :global(a) {
		color: var(--color-primary-600-400);
		text-decoration: underline;
		text-underline-offset: 0.18em;
	}

	.studio-doc__content :global(code) {
		border-radius: 0.25rem;
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		padding: 0.08rem 0.25rem;
		font-size: 0.84em;
	}

	.studio-doc__content :global(blockquote.doc-note) {
		margin: 0 0 0.95rem;
		border-left: 4px solid var(--color-primary-500);
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-primary-500) 18%, var(--studio-panel-bg));
		color: var(--color-surface-950-50);
		padding: 0.62rem 0.78rem;
	}

	.studio-doc__content :global(blockquote.doc-note p:last-child) {
		margin-bottom: 0;
	}

	.studio-doc__empty {
		display: grid;
		min-height: 7rem;
		place-items: center;
		color: var(--color-surface-600-400);
		padding: 1rem;
		font-size: 0.86rem;
		text-align: center;
	}

	.studio-doc__empty--full {
		height: 100%;
		gap: 0.65rem;
		align-content: center;
		color: var(--color-surface-500);
	}

	.studio-doc__loading {
		width: 1.45rem;
		height: 1.45rem;
		border: 2px solid color-mix(in oklab, var(--color-primary-500) 22%, transparent);
		border-top-color: var(--color-primary-500);
		border-radius: 999px;
		animation: studio-doc-spin 0.8s linear infinite;
	}

	@keyframes studio-doc-spin {
		to {
			transform: rotate(360deg);
		}
	}
</style>

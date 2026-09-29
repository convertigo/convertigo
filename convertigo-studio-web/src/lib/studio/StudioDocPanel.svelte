<script>
	import { documentationBlocks } from './docBlocks.js';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioObjectIdentity from './StudioObjectIdentity.svelte';

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
	 * @property {{ label: string, description: string }[]=} propertyDocumentation
	 * @property {string=} icon
	 * @property {string=} instanceName
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

	let blocks = $derived(itemDocumentationBlocks(paletteItem));

	/**
	 * @param {PaletteItem | null | undefined} item
	 * @returns {any[]}
	 */
	function itemDocumentationBlocks(item) {
		if (!item) {
			return [];
		}
		const fallback = splitRawDescription(item.description);
		// the text of a comment is Markdown, with the HTML it holds
		const short = item.shortDescriptionHtml || item.shortDescriptionText || fallback.shortHtml;
		const long = item.longDescriptionHtml || item.longDescriptionText || fallback.longHtml;
		const properties = item.propertiesDescriptionHtml ?? '';
		const blocks = [];
		// the summary is the first paragraph of the short documentation, whose Markdown can go on
		const shortBlocks = short ? documentationBlocks(short) : [];
		if (shortBlocks[0]?.type === 'paragraph') {
			shortBlocks[0] = { ...shortBlocks[0], className: 'studio-doc__summary' };
		}
		blocks.push(...shortBlocks);
		if (long) {
			blocks.push(...documentationBlocks(long));
		}
		if (properties) {
			blocks.push({
				type: 'heading',
				segments: [{ type: 'text', text: 'Properties:' }]
			});
			blocks.push(...documentationBlocks(properties));
		}
		if (item.propertyDocumentation?.length) {
			blocks.push({
				type: 'heading',
				segments: [{ type: 'text', text: 'Properties:' }]
			});
			for (const property of item.propertyDocumentation) {
				blocks.push({
					type: 'paragraph',
					className: 'studio-doc__property',
					segments: [
						{ type: 'strong', text: property.label },
						{ type: 'text', text: ` — ${property.description}` }
					]
				});
			}
		}
		return blocks.filter((block) => block.type !== 'paragraph' || block.segments.length);
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

{#snippet inlineSegment(segment)}
	{#if segment.type === 'break'}
		<br />
	{:else if segment.type === 'code'}
		<code>{segment.text}</code>
	{:else if segment.type === 'strong'}
		<strong>{segment.text}</strong>
	{:else if segment.type === 'emphasis'}
		<em>{segment.text}</em>
	{:else if segment.type === 'underline'}
		<u>{segment.text}</u>
	{:else if segment.type === 'strike'}
		<s>{segment.text}</s>
	{:else if segment.type === 'link'}
		<a href={segment.href} target="_blank" rel="noopener noreferrer">{segment.text}</a>
	{:else}
		{segment.text}
	{/if}
{/snippet}

{#snippet inlineSegmentsView(segments)}
	{#each segments as segment, index (index)}
		{@render inlineSegment(segment)}
	{/each}
{/snippet}

{#snippet listItem(item)}
	<li>
		{@render inlineSegmentsView(item.segments)}
		{#each item.blocks as child, index (index)}
			{@render docBlock(child)}
		{/each}
	</li>
{/snippet}

{#snippet docBlock(block)}
	{#if block.type === 'heading'}
		<h3>{@render inlineSegmentsView(block.segments)}</h3>
	{:else if block.type === 'paragraph'}
		<p class={block.className}>{@render inlineSegmentsView(block.segments)}</p>
	{:else if block.type === 'list'}
		<ul>
			{#each block.items as item, index (index)}
				{@render listItem(item)}
			{/each}
		</ul>
	{:else if block.type === 'ordered-list'}
		<ol>
			{#each block.items as item, index (index)}
				{@render listItem(item)}
			{/each}
		</ol>
	{:else if block.type === 'code-block'}
		<pre><code>{block.text}</code></pre>
	{:else if block.type === 'note' || block.type === 'quote'}
		<blockquote class={block.type === 'note' ? 'doc-note' : 'doc-quote'}>
			{#each block.blocks as child, index (index)}
				{@render docBlock(child)}
			{/each}
		</blockquote>
	{:else if block.type === 'table'}
		<div class="studio-doc__table">
			<table>
				<tbody>
					{#each block.rows as row, rowIndex (rowIndex)}
						<tr>
							{#each row.cells as cell, cellIndex (cellIndex)}
								{#if row.header}
									<th>{@render inlineSegmentsView(cell)}</th>
								{:else}
									<td>{@render inlineSegmentsView(cell)}</td>
								{/if}
							{/each}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{:else if block.type === 'rule'}
		<hr />
	{/if}
{/snippet}

<section class="studio-doc">
	{#if paletteItem}
		<StudioObjectIdentity item={paletteItem} />

		{#if blocks.length}
			<article class="studio-doc__content">
				{#each blocks as block, index (index)}
					{@render docBlock(block)}
				{/each}
			</article>
		{:else}
			<StudioEmptyState
				message="No documentation available for this component."
				icon="mdi:book-open-variant"
			/>
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
		font-style: italic;
		font-weight: 650;
	}

	.studio-doc__content :global(pre) {
		margin: 0 0 0.9rem;
		overflow: auto;
		border-radius: 0.35rem;
		background: color-mix(in oklab, var(--color-surface-500) 12%, transparent);
		padding: 0.6rem 0.75rem;
		font-size: 0.8rem;
		line-height: 1.45;
	}

	.studio-doc__content :global(pre code) {
		background: none;
		padding: 0;
	}

	.studio-doc__content :global(blockquote.doc-quote) {
		margin: 0 0 0.9rem;
		border-left: 3px solid var(--color-surface-400-600);
		color: var(--color-surface-700-300);
		padding: 0.1rem 0 0.1rem 0.8rem;
	}

	.studio-doc__table {
		margin: 0 0 0.9rem;
		overflow-x: auto;
	}

	.studio-doc__table table {
		border-collapse: collapse;
		font-size: 0.82rem;
	}

	.studio-doc__table th,
	.studio-doc__table td {
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		padding: 0.3rem 0.55rem;
		text-align: left;
		vertical-align: top;
	}

	.studio-doc__table th {
		background: color-mix(in oklab, var(--color-surface-500) 10%, transparent);
		font-weight: 700;
	}

	.studio-doc__content :global(hr) {
		margin: 0.9rem 0;
		border: 0;
		border-top: 1px solid var(--studio-line, var(--color-surface-200-800));
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
</style>

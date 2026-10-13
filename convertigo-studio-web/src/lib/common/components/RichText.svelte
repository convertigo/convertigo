<script>
	import Button from '$lib/admin/components/Button.svelte';
	import { documentationBlocks } from '$lib/common/docBlocks.js';

	/** @type {{ source?: string | null, blocks?: any[], collapsible?: boolean, collapsedHeight?: number, class?: string }} */
	let {
		source = '',
		blocks,
		collapsible = false,
		collapsedHeight = 96,
		class: cls = ''
	} = $props();
	let content = $derived(blocks ?? documentationBlocks(source ?? ''));
	let expanded = $state(false);
	let contentHeight = $state(0);
	let hasOverflow = $derived(collapsible && contentHeight > collapsedHeight);
	const contentId = $props.id();

	/** @param {PointerEvent} event */
	function toggleText(event) {
		if (!hasOverflow || event.button !== 0 || !event.isPrimary) return;
		if (
			event.target instanceof Element &&
			event.target.closest('a, button, input, textarea, select')
		)
			return;
		if (window.getSelection()?.toString()) return;
		expanded = !expanded;
	}
</script>

{#snippet inline(segments)}
	{#each segments as segment, index (index)}
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
	{/each}
{/snippet}

{#snippet blockView(block)}
	{#if block.type === 'heading'}
		<h3>{@render inline(block.segments)}</h3>
	{:else if block.type === 'paragraph'}
		<p class:summary={block.className === 'studio-doc__summary'}>
			{@render inline(block.segments)}
		</p>
	{:else if block.type === 'list' || block.type === 'ordered-list'}
		<svelte:element this={block.type === 'list' ? 'ul' : 'ol'}>
			{#each block.items as item, index (index)}
				<li>
					{@render inline(item.segments)}
					{#each item.blocks as child, childIndex (childIndex)}
						{@render blockView(child)}
					{/each}
				</li>
			{/each}
		</svelte:element>
	{:else if block.type === 'code-block'}
		<pre><code>{block.text}</code></pre>
	{:else if block.type === 'note' || block.type === 'quote'}
		<blockquote class:note={block.type === 'note'}>
			{#each block.blocks as child, index (index)}
				{@render blockView(child)}
			{/each}
		</blockquote>
	{:else if block.type === 'table'}
		<div class="rich-text__table">
			<table>
				<tbody>
					{#each block.rows as row, rowIndex (rowIndex)}
						<tr>
							{#each row.cells as cell, cellIndex (cellIndex)}
								{#if row.header}
									<th>{@render inline(cell)}</th>
								{:else}
									<td>{@render inline(cell)}</td>
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

<div
	class={['rich-text', cls]}
	class:rich-text--toggle={hasOverflow}
	role="group"
	onpointerup={toggleText}
>
	<div
		class="rich-text__clip"
		style:max-height={collapsible
			? `${expanded ? Math.max(contentHeight, collapsedHeight) : collapsedHeight}px`
			: undefined}
	>
		<div id={contentId} class="rich-text__content" bind:clientHeight={contentHeight}>
			{#each content as block, index (index)}
				{@render blockView(block)}
			{/each}
		</div>
	</div>
	{#if hasOverflow}
		<Button
			type="button"
			label="…"
			ariaLabel={expanded ? 'Show less' : 'Show more'}
			class="mt-1 button-secondary h-6! min-h-6! py-0 px-low text-lg leading-none"
			full={false}
			aria-expanded={expanded}
			aria-controls={contentId}
			onclick={() => (expanded = !expanded)}
		/>
	{/if}
</div>

<style>
	.rich-text {
		min-width: 0;
		line-height: 1.55;
		overflow-wrap: anywhere;
	}
	.rich-text__clip {
		overflow: hidden;
		transition: max-height 200ms ease-in-out;
	}
	.rich-text--toggle .rich-text__content {
		cursor: pointer;
	}
	@media (prefers-reduced-motion: reduce) {
		.rich-text__clip {
			transition: none;
		}
	}
	.rich-text__content {
		display: flow-root;
	}
	.rich-text__content :global(p),
	.rich-text__content :global(ul),
	.rich-text__content :global(ol),
	.rich-text__content :global(pre),
	.rich-text__content :global(blockquote),
	.rich-text__table {
		margin: 0 0 0.75em;
	}
	.rich-text__content :global(:last-child) {
		margin-bottom: 0;
	}
	.rich-text__content :global(h3) {
		margin: 1em 0 0.4em;
		font-weight: 750;
		font-size: 1.1em;
	}
	.rich-text__content :global(h3:first-child) {
		margin-top: 0;
	}
	.rich-text__content :global(ul),
	.rich-text__content :global(ol) {
		padding-left: 1.5em;
	}
	.rich-text__content :global(ul) {
		list-style: disc;
	}
	.rich-text__content :global(ol) {
		list-style: decimal;
	}
	.rich-text__content :global(li) {
		margin: 0.25em 0;
	}
	.rich-text__content :global(strong) {
		font-weight: 750;
	}
	.rich-text__content :global(a) {
		color: var(--color-primary-600-400);
		text-decoration: underline;
	}
	.rich-text__content :global(code) {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
		border-radius: 0.25rem;
		padding: 0.1em 0.25em;
		font-size: 0.9em;
	}
	.rich-text__content :global(pre) {
		overflow: auto;
		padding: 0.75em;
		background: var(--color-surface-100-900);
		border-radius: 0.25rem;
	}
	.rich-text__content :global(pre code) {
		background: none;
		padding: 0;
		white-space: pre;
	}
	.rich-text__content :global(blockquote) {
		border-left: 3px solid var(--color-primary-500);
		padding: 0.5em 0.75em;
	}
	.rich-text__content :global(.note) {
		background: color-mix(in oklab, var(--color-primary-500) 10%, transparent);
	}
	.rich-text__content :global(.summary) {
		color: var(--color-primary-700-300);
		font-weight: 650;
	}
	.rich-text__table {
		overflow-x: auto;
	}
	.rich-text__content :global(table) {
		border-collapse: collapse;
		width: 100%;
	}
	.rich-text__content :global(th),
	.rich-text__content :global(td) {
		border: 1px solid var(--color-surface-300-700);
		padding: 0.4em 0.6em;
		text-align: left;
		vertical-align: top;
	}
	.rich-text__content :global(th) {
		background: var(--color-surface-100-900);
		font-weight: 750;
	}
</style>

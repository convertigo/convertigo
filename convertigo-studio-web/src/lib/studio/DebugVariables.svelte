<script>
	import Ico from '#lib/utils/Ico.svelte';

	/** @typedef {{name: string, value: string, type?: string, ref?: string, expandable?: boolean, fullValue?: boolean, limited?: boolean}} Variable */
	/** @type {{variables?: Variable[], reference?: string, nextOffset?: number, load?: (ref: string, offset: number) => Promise<any>, loadValue?: (ref: string) => Promise<any>}} */
	let { variables = [], reference = '', nextOffset = -1, load, loadValue } = $props();
	let extra = $state(/** @type {Variable[]} */ ([]));
	let rootNext = $state(/** @type {number | null} */ (null));
	let rows = $state(
		/** @type {Record<string, {open: boolean, loading: boolean, children: Variable[], next: number, error?: string}>} */ ({})
	);
	let full = $state(/** @type {{name: string, value: string} | null} */ (null));
	let rootLoading = $state(false);
	let error = $state('');
	let more = $derived(rootNext ?? nextOffset);
	async function expand(variable, key) {
		if (rows[key]) {
			rows[key].open = !rows[key].open;
			return;
		}
		rows[key] = { open: true, loading: true, children: [], next: -1 };
		await fetchPage(variable.ref, key, 0);
	}
	async function fetchPage(ref, key, offset) {
		const row = rows[key];
		row.loading = true;
		try {
			const page = await load?.(ref, offset);
			if (!page) throw new Error('The pause has changed. Refresh the variables.');
			row.children = [...row.children, ...(page.variables ?? [])];
			row.next = page.nextOffset ?? -1;
			row.error = '';
		} catch (cause) {
			row.error = String(cause instanceof Error ? cause.message : cause);
		} finally {
			row.loading = false;
		}
	}
	async function loadMore() {
		rootLoading = true;
		try {
			const page = await load?.(reference, more);
			if (!page) throw new Error('The pause has changed. Refresh the variables.');
			extra = [...extra, ...(page.variables ?? [])];
			rootNext = page.nextOffset ?? -1;
		} catch (cause) {
			error = String(cause instanceof Error ? cause.message : cause);
		} finally {
			rootLoading = false;
		}
	}
	async function showValue(variable) {
		full = { name: variable.name, value: variable.value };
		if (variable.fullValue) {
			try {
				const result = await loadValue?.(variable.ref);
				full = {
					name: variable.name,
					value: result
						? `${result.value}${result.truncated ? '\n[Display limited to 65,536 characters]' : ''}`
						: 'The pause has changed.'
				};
			} catch {
				full = { name: variable.name, value: 'Unable to read this value. Refresh the variables.' };
			}
		}
	}
</script>

{#snippet tree(items, depth = 0, path = '')}
	{#each items as variable, index (`${path}/${index}`)}
		{@const key = `${path}/${index}`}
		{@const row = rows[key]}
		<tr>
			<td title={variable.name}>
				<div class="variable-name" style:padding-left={`${Math.min(depth, 8) * 0.8}rem`}>
					{#if variable.expandable && depth < 32}
						<button
							class="expand"
							aria-label={`${row?.open ? 'Collapse' : 'Expand'} ${variable.name}`}
							aria-expanded={row?.open ?? false}
							onclick={() => expand(variable, key)}
							><Ico icon={row?.open ? 'mdi:chevron-down' : 'mdi:chevron-right'} /></button
						>
					{:else}<span class="spacer"></span>{/if}
					<span class="truncate">{variable.name}</span>
				</div>
			</td>
			<td
				><button
					class="value"
					title={variable.value}
					aria-label={`Show value of ${variable.name}`}
					onclick={() => showValue(variable)}>{variable.value}</button
				></td
			>
			<td class="type" title={variable.type}>{variable.type ?? ''}</td>
		</tr>
		{#if variable.limited}<tr
				><td colspan="3">Variable reference capacity reached. Resume to start a new inspection.</td
				></tr
			>{/if}
		{#if row?.open}
			{@render tree(row.children, depth + 1, key)}
			{#if row.loading}<tr><td colspan="3">Loading properties…</td></tr>{/if}
			{#if row.error}<tr
					><td colspan="3"
						><span role="alert">{row.error}</span><button
							onclick={() => fetchPage(variable.ref, key, row.children.length)}>Retry</button
						></td
					></tr
				>{/if}
			{#if row.next >= 0}<tr
					><td colspan="3"
						><button
							class="more"
							disabled={row.loading}
							onclick={() => fetchPage(variable.ref, key, row.next)}
							>Load next 100 properties of {variable.name}</button
						></td
					></tr
				>{/if}
		{/if}
	{/each}
{/snippet}

<div class="variables">
	<table aria-label="Variables">
		<colgroup><col style="width:38%" /><col style="width:42%" /><col style="width:20%" /></colgroup>
		<thead
			><tr><th scope="col">Name</th><th scope="col">Value</th><th scope="col">Type</th></tr></thead
		>
		<tbody>{@render tree([...variables, ...extra])}</tbody>
	</table>
	{#if !variables.length && !extra.length}<p class="empty">No variables in this scope.</p>{/if}
	{#if more >= 0}<button class="more" disabled={rootLoading} onclick={loadMore}
			>Load next 100 properties</button
		>{/if}
	{#if error}<p role="alert">{error}</p>{/if}
	{#if full}
		<div class="full-value" role="region" aria-label={`Value of ${full.name}`}>
			<div class="full-heading">
				<strong>{full.name}</strong><button
					aria-label="Close full value"
					onclick={() => (full = null)}>×</button
				>
			</div>
			<pre>{full.value}</pre>
		</div>
	{/if}
</div>

<style>
	.variables {
		min-width: 0;
	}
	table {
		width: 100%;
		table-layout: fixed;
		border-collapse: collapse;
		font-size: 0.78rem;
	}
	th {
		text-align: left;
		color: var(--color-surface-600-400);
		font-weight: 500;
		background: var(--color-surface-100-900);
	}
	th,
	td {
		padding: 0.3rem 0.4rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.variable-name {
		display: flex;
		align-items: center;
		min-width: 0;
		gap: 0.15rem;
	}
	.expand,
	.spacer {
		display: inline-flex;
		flex: none;
		width: 1rem;
		height: 1rem;
	}
	.expand {
		align-items: center;
		justify-content: center;
	}
	.value {
		display: block;
		max-width: 100%;
		overflow: hidden;
		text-overflow: ellipsis;
		font-family: monospace;
		text-align: left;
		cursor: zoom-in;
	}
	.type,
	.empty {
		color: var(--color-surface-600-400);
	}
	.more {
		padding: 0.4rem;
		color: var(--color-primary-600-400);
		text-align: left;
		white-space: normal;
	}
	.full-value {
		margin-top: 0.5rem;
		padding: 0.5rem;
		border: 1px solid var(--color-primary-500);
		border-radius: 0.35rem;
	}
	.full-heading {
		display: flex;
		justify-content: space-between;
		gap: 0.5rem;
		overflow-wrap: anywhere;
	}
	pre {
		max-height: 15rem;
		overflow: auto;
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		font-size: 0.78rem;
	}
	button:focus-visible {
		outline: 2px solid var(--color-primary-500);
		outline-offset: 1px;
	}
</style>

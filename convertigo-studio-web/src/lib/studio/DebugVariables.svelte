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
	<ul class="variable-branch">
		{#each items as variable, index (`${path}/${index}`)}
			{@const key = `${path}/${index}`}
			{@const row = rows[key]}
			<li class="variable-item">
				<div class="variable-row" style:padding-left={`${Math.min(depth, 8) * 0.8}rem`}>
					{#if variable.expandable && depth < 32}
						<button
							class="expand"
							aria-label={`${row?.open ? 'Collapse' : 'Expand'} ${variable.name}`}
							aria-expanded={row?.open ?? false}
							onclick={() => expand(variable, key)}
							><Ico icon={row?.open ? 'mdi:chevron-down' : 'mdi:chevron-right'} /></button
						>
					{:else}<span class="spacer"></span>{/if}
					<span class="variable-name" title={variable.name}>{variable.name}</span><span
						class="colon">:</span
					>
					<button
						class="value"
						data-type={variable.type}
						title={`${variable.type ?? ''}: ${variable.value}`}
						aria-label={`Show value of ${variable.name}`}
						onclick={() => showValue(variable)}>{variable.value}</button
					>
					<span class="type">{variable.type ?? ''}</span>
				</div>
				{#if variable.limited}<p class="variable-message">
						Variable reference capacity reached. Resume to start a new inspection.
					</p>{/if}
				{#if row?.open}
					{@render tree(row.children, depth + 1, key)}
					{#if row.loading}<p class="variable-message">Loading properties…</p>{/if}
					{#if row.error}<div class="variable-message">
							<span role="alert">{row.error}</span>
							<button class="more" onclick={() => fetchPage(variable.ref, key, row.children.length)}
								>Retry</button
							>
						</div>{/if}
					{#if row.next >= 0}<button
							class="more"
							disabled={row.loading}
							onclick={() => fetchPage(variable.ref, key, row.next)}
							>Load next 100 properties of {variable.name}</button
						>{/if}
				{/if}
			</li>
		{/each}
	</ul>
{/snippet}

<div class="variables" aria-label="Variables">
	{@render tree([...variables, ...extra])}
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
	.variables,
	.variable-item {
		min-width: 0;
	}
	.variable-branch {
		list-style: none;
		margin: 0;
		padding: 0;
	}
	.variable-row {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		min-width: 0;
		min-height: 1.65rem;
		font: 0.76rem/1.5 monospace;
	}
	.variable-row:hover {
		background: color-mix(in oklab, var(--color-surface-300-700) 25%, transparent);
	}
	.expand,
	.spacer {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		flex: none;
		width: 1rem;
		height: 1.3rem;
	}
	.variable-name {
		flex: 0 1 auto;
		max-width: 45%;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		color: var(--color-primary-600-400);
	}
	.colon {
		flex: none;
		color: var(--color-surface-500);
	}
	.value {
		min-width: 0;
		flex: 1;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		text-align: left;
		cursor: zoom-in;
	}
	.value[data-type='string'] {
		color: var(--color-success-700-300);
	}
	.value[data-type='number'],
	.value[data-type='boolean'] {
		color: var(--color-primary-600-400);
	}
	.type {
		flex: none;
		font: 0.65rem/1.5 sans-serif;
		color: var(--color-surface-600-400);
		padding-right: 0.35rem;
	}
	.empty,
	.variable-message {
		color: var(--color-surface-600-400);
		font-size: 0.75rem;
		padding: 0.35rem 1.25rem;
	}
	.more {
		padding: 0.35rem 1.25rem;
		color: var(--color-primary-600-400);
		text-align: left;
		white-space: normal;
		font-size: 0.75rem;
	}
	.full-value {
		margin: 0.4rem;
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
		outline-offset: -1px;
	}
</style>

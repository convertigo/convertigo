<script>
	import AutoSvg from '#lib/utils/AutoSvg.svelte';
	import { call, getUrl } from '#lib/utils/service.js';
	import { formatDuration, parseSample, rowItems } from './nativeJson.js';
	import StudioJsonTree from './StudioJsonTree.svelte';

	/**
	 * @typedef {{ index: number, stepId: string, stepName: string, icon?: string, variable?: string, count?: number, durationMicros?: number, size?: number, truncated?: boolean, kept?: boolean, note?: string }} NativeRow
	 * @typedef {{ native?: boolean, nativeExecution?: boolean, nativeSteps?: boolean, bridges?: { stepId: string, stepName: string, reads: string[] }[] }} NativeInfo
	 * @typedef {{ key: string, loading: boolean, json?: string, note?: string, truncated?: boolean, kept?: boolean }} NativeDetail
	 */

	/**
	 * The Native JSON steps panel of the sequence editor of the Eclipse Studio: each native step execution of the
	 * last run, with the JSON it produced (no XML is built for these steps), and the XML steps that read the XML
	 * view of a native step.
	 *
	 * @type {{
	 *  token?: string,
	 *  rows?: NativeRow[],
	 *  info?: NativeInfo | null,
	 *  pausedStep?: string,
	 *  onSelectStep?: (id: string) => void
	 * }}
	 */
	let { token = '', rows = [], info = null, pausedStep = '', onSelectStep } = $props();

	/** text above this size is shown as is, not indented */
	const PRETTY_LIMIT = 2 * 1024 * 1024;
	const CUT_AT = 4 * 1024 * 1024;

	let selectedIndex = $state(-1);
	let followLast = $state(true);
	let tab = $state(/** @type {'tree' | 'text'} */ ('tree'));
	let detail = $state(/** @type {NativeDetail | null} */ (null));

	let lastRow = $derived(rows.length ? rows[rows.length - 1] : null);
	let selectedRow = $derived(rows.find((row) => row.index === selectedIndex) ?? null);
	let header = $derived.by(() => {
		let text = !lastRow
			? 'Native JSON steps: execute the sequence to see the JSON each native step produces (no XML is built for them).'
			: `Native JSON steps: ${rows.length} execution${rows.length === 1 ? '' : 's'}, last '${lastRow.stepName}' into '${lastRow.variable ?? ''}'.`;
		if (pausedStep) {
			text = `${text.replace(/\.$/, '')}; paused before '${pausedStep}'.`;
		}
		return text;
	});
	let parsed = $derived(
		detail?.json !== undefined && detail.json !== '' ? parseSample(detail.json) : null
	);
	let prettyText = $derived.by(() => {
		if (!detail?.json) {
			return '';
		}
		if (parsed?.ok && detail.json.length <= PRETTY_LIMIT) {
			return JSON.stringify(parsed.value, null, 2);
		}
		return detail.json;
	});

	$effect(() => {
		// a new run starts empty, each new execution is selected, as in the Eclipse panel
		const last = lastRow;
		if (!last) {
			selectedIndex = -1;
			followLast = true;
			detail = null;
			return;
		}
		if (followLast || selectedIndex < 0) {
			selectedIndex = last.index;
		}
	});

	$effect(() => {
		const row = selectedRow;
		const currentToken = token;
		if (!row || !currentToken) {
			return;
		}
		const key = `${currentToken}\u0000${row.index}`;
		if (detail?.key === key) {
			return;
		}
		detail = { key, loading: true };
		void call('studio.debug.NativeJson', {
			action: 'json',
			token: currentToken,
			index: String(row.index)
		}).then((result) => {
			if (detail?.key !== key) {
				return;
			}
			detail = {
				key,
				loading: false,
				json: String(result?.json ?? ''),
				note: result?.note,
				truncated: result?.truncated === true,
				kept: result?.kept !== false
			};
		});
	});

	/**
	 * @param {NativeRow} row
	 */
	function select(row) {
		selectedIndex = row.index;
		followLast = row.index === lastRow?.index;
	}

	/**
	 * @param {{ stepName: string, reads: string[] }} bridge
	 */
	function bridgeText(bridge) {
		return `${bridge.stepName} reads the XML view of ${bridge.reads.join(', ')}`;
	}
</script>

<section class="studio-native-json" aria-label="Native JSON steps">
	<p class="studio-native-json__header">{header}</p>
	{#if rows.length}
		<div class="studio-native-json__body">
			<div class="studio-native-json__table">
				<table>
					<thead>
						<tr>
							<th>#</th>
							<th>Native step</th>
							<th>Variable</th>
							<th>Items</th>
							<th>Time</th>
						</tr>
					</thead>
					<tbody>
						{#each rows as row (row.index)}
							<tr
								class:studio-native-json__row--selected={row.index === selectedIndex}
								title="Double-click to select the step in the projects tree"
								onclick={() => select(row)}
								ondblclick={() => onSelectStep?.(row.stepId)}
							>
								<td>{row.index + 1}</td>
								<td>
									<span class="studio-native-json__step">
										{#if row.icon}
											<AutoSvg
												class="h-4 w-4"
												fill="currentColor"
												src="{getUrl()}{row.icon}"
												alt=""
											/>
										{/if}
										<button type="button" onclick={() => select(row)}>{row.stepName}</button>
									</span>
								</td>
								<td>{row.variable ?? ''}</td>
								<td>{rowItems(row)}</td>
								<td>{formatDuration(row.durationMicros ?? 0)}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<div class="studio-native-json__detail">
				<div class="studio-native-json__tabs" role="tablist" aria-label="JSON of the step">
					<button
						type="button"
						role="tab"
						aria-selected={tab === 'tree'}
						class:studio-native-json__tab--active={tab === 'tree'}
						onclick={() => (tab = 'tree')}>JSON tree</button
					>
					<button
						type="button"
						role="tab"
						aria-selected={tab === 'text'}
						class:studio-native-json__tab--active={tab === 'text'}
						onclick={() => (tab = 'text')}>JSON text</button
					>
				</div>
				<div class="studio-native-json__view" role="tabpanel">
					{#if !detail || detail.loading}
						<p class="studio-native-json__note">Loading…</p>
					{:else if detail.note === 'stream' && !detail.json}
						<p class="studio-native-json__note">
							'{selectedRow?.stepName}' writes its rows in stream into the response: they are in the
							JSON output of the sequence.
						</p>
					{:else if !detail.kept}
						<p class="studio-native-json__note">
							The JSON of this execution is not kept: the run produced too much JSON.
						</p>
					{:else if tab === 'tree' && parsed?.ok}
						<StudioJsonTree
							value={parsed.value}
							rootLabel={selectedRow?.variable || '$'}
							label={`JSON of ${selectedRow?.stepName ?? 'the step'}`}
						/>
					{:else}
						<pre>{prettyText}</pre>
					{/if}
					{#if detail?.truncated}
						<p class="studio-native-json__note">… (cut at {CUT_AT} characters)</p>
					{/if}
				</div>
			</div>
		</div>
	{/if}
	{#if info}
		<p class="studio-native-json__footer">
			{#if info.bridges?.length}
				XML bridge: {info.bridges.map(bridgeText).join('; ')}. These views are built at their first
				read, for these steps only.
			{:else}
				No XML step reads a native step: no XML is built for the native steps.
			{/if}
		</p>
	{/if}
</section>

<style>
	.studio-native-json {
		display: grid;
		gap: 0.5rem;
		border-top: 1px solid var(--studio-line, var(--color-surface-200-800));
		padding-top: 0.6rem;
		font-size: 0.78rem;
	}

	.studio-native-json__header,
	.studio-native-json__footer,
	.studio-native-json__note {
		margin: 0;
		color: var(--studio-text-idle, var(--color-surface-600-400));
	}

	.studio-native-json__header {
		color: var(--studio-text-strong, inherit);
		font-weight: 600;
	}

	.studio-native-json__body {
		display: grid;
		grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
		gap: 0.6rem;
		min-height: 14rem;
	}

	@container (max-width: 640px) {
		.studio-native-json__body {
			grid-template-columns: minmax(0, 1fr);
		}
	}

	.studio-native-json__table,
	.studio-native-json__view {
		max-height: 22rem;
		overflow: auto;
		border: 1px solid var(--studio-line, var(--color-surface-200-800));
		border-radius: 0.4rem;
	}

	.studio-native-json__table table {
		width: 100%;
		border-collapse: collapse;
	}

	.studio-native-json__table th,
	.studio-native-json__table td {
		padding: 0.25rem 0.45rem;
		text-align: start;
		white-space: nowrap;
	}

	.studio-native-json__table th {
		position: sticky;
		top: 0;
		background: var(--studio-chrome-bg, var(--color-surface-100-900));
		color: var(--studio-text-idle, var(--color-surface-600-400));
		font-weight: 600;
	}

	.studio-native-json__table tbody tr {
		cursor: pointer;
	}

	.studio-native-json__table tbody tr:hover {
		background: var(
			--studio-hover-bg,
			color-mix(in oklab, var(--color-surface-500) 12%, transparent)
		);
	}

	.studio-native-json__table tbody tr.studio-native-json__row--selected {
		background: light-dark(
			color-mix(in oklab, var(--color-primary-500) 16%, white),
			color-mix(in oklab, var(--color-primary-500) 22%, #1c1c1d)
		);
	}

	.studio-native-json__step {
		display: inline-flex;
		align-items: center;
		gap: 0.35rem;
	}

	.studio-native-json__step button {
		border: 0;
		background: none;
		padding: 0;
		color: inherit;
		font: inherit;
		cursor: pointer;
	}

	.studio-native-json__detail {
		display: grid;
		grid-template-rows: auto minmax(0, 1fr);
		gap: 0.3rem;
		min-width: 0;
	}

	.studio-native-json__tabs {
		display: flex;
		gap: 0.25rem;
	}

	.studio-native-json__tabs button {
		border: 1px solid transparent;
		border-radius: 0.35rem;
		background: none;
		padding: 0.15rem 0.55rem;
		color: var(--studio-text-idle, var(--color-surface-600-400));
		font: inherit;
		cursor: pointer;
	}

	.studio-native-json__tabs button.studio-native-json__tab--active {
		border-color: var(--studio-line, var(--color-surface-200-800));
		color: var(--studio-text-strong, inherit);
	}

	.studio-native-json__view {
		padding: 0.35rem;
	}

	.studio-native-json__view pre {
		margin: 0;
		font-size: 0.74rem;
		white-space: pre;
	}
</style>

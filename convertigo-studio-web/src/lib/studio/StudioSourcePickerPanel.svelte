<script>
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{ selectedId?: string, active?: boolean, onSelectObject?: (id: string) => void }} */
	let { selectedId = '', active = true, onSelectObject = () => {} } = $props();

	let linked = $state(true);
	let loading = $state(false);
	let error = $state('');
	let loadSerial = 0;
	let { id, properties, onSelectionChange } = $derived(createDatabaseObjectProperties());

	let objectClass = $derived(getObjectClass(properties));
	let sourceProperty = $derived(findSourceProperty(properties));
	let sourceDefinition = $derived(parseSourceDefinition(sourceProperty?.value));
	let pickerMode = $derived(getPickerMode(selectedId, objectClass, sourceProperty));
	let selectedName = $derived(getSelectionName(selectedId));
	let selectionTrail = $derived(buildSelectionTrail(selectedId));
	let sourceTrail = $derived(buildSelectionTrail(sourceDefinition.target));

	$effect(() => {
		const nextId = selectedId;
		if (!active || !linked || !nextId || nextId === id) {
			return;
		}
		const serial = ++loadSerial;
		void loadSelection(nextId, serial);
	});

	/**
	 * @param {string} nextId
	 * @param {number} serial
	 */
	async function loadSelection(nextId, serial) {
		loading = true;
		error = '';
		try {
			await onSelectionChange({ selectedValue: [nextId] });
		} catch (err) {
			if (serial === loadSerial) {
				error = String(err instanceof Error ? err.message : err);
			}
		} finally {
			if (serial === loadSerial) {
				loading = false;
			}
		}
	}

	/**
	 * @param {string} qname
	 * @returns {string}
	 */
	function getSelectionName(qname) {
		if (!qname) {
			return '';
		}
		return qname.split(/[.:]/).filter(Boolean).at(-1) ?? qname;
	}

	/**
	 * @param {any[]} rows
	 * @returns {string}
	 */
	function getObjectClass(rows) {
		const info = rows.find((row) => {
			const key = `${row.displayName ?? ''} ${row.name ?? ''}`.toLowerCase();
			return key === 'class' || key.includes(' class');
		});
		return String(info?.value ?? '');
	}

	/**
	 * @param {any[]} rows
	 * @returns {any}
	 */
	function findSourceProperty(rows) {
		return rows.find((row) => {
			const key = `${row.displayName ?? ''} ${row.name ?? ''}`.toLowerCase();
			return key.includes('source') && key.includes('definition');
		});
	}

	/**
	 * @param {any} value
	 * @returns {{ target: string, xpath: string, raw: string, empty: boolean }}
	 */
	function parseSourceDefinition(value) {
		if (value == null || value === '') {
			return { target: '', xpath: '', raw: '', empty: true };
		}

		const parts = normalizeSourceParts(value);
		return {
			target: String(parts[0] ?? ''),
			xpath: String(parts[1] ?? ''),
			raw: typeof value === 'string' ? value : JSON.stringify(value),
			empty: parts.length === 0 || parts.every((part) => part == null || part === '')
		};
	}

	/**
	 * @param {any} value
	 * @returns {any[]}
	 */
	function normalizeSourceParts(value) {
		if (Array.isArray(value)) {
			return value;
		}
		if (typeof value !== 'string') {
			return [];
		}
		const trimmed = value.trim();
		if (!trimmed) {
			return [];
		}
		if (trimmed.startsWith('[')) {
			try {
				const parsed = JSON.parse(trimmed);
				return Array.isArray(parsed) ? parsed : [];
			} catch {
				return [trimmed];
			}
		}
		if (trimmed.includes('\n')) {
			return trimmed.split(/\r?\n/).filter(Boolean);
		}
		if (trimmed.includes(',')) {
			return trimmed.split(',').map((part) => part.trim());
		}
		return [trimmed];
	}

	/**
	 * @param {string} qname
	 * @param {string} cls
	 * @param {any} source
	 * @returns {{ label: string, icon: string, kind: string }}
	 */
	function getPickerMode(qname, cls, source) {
		const haystack = `${qname ?? ''} ${cls ?? ''}`.toLowerCase();
		if (source || /\.st:|\.step:|step/.test(haystack)) {
			return { label: 'Step source', icon: 'mdi:source-branch', kind: 'step' };
		}
		if (haystack.includes('.ngx.') || haystack.includes('.mobile.')) {
			return { label: 'Smart source', icon: 'mdi:smartphone-link', kind: 'smart' };
		}
		return { label: 'Object picker', icon: 'mdi:hub', kind: 'object' };
	}

	/**
	 * @param {string} qname
	 * @returns {{ id: string, label: string, type: string }[]}
	 */
	function buildSelectionTrail(qname) {
		if (!qname || qname === 'ROOT') {
			return [];
		}
		let currentId = '';
		return qname
			.split('.')
			.filter(Boolean)
			.map((segment, index) => {
				currentId = currentId ? `${currentId}.${segment}` : segment;
				const match = segment.match(/^([^:]+):(.*)$/);
				return {
					id: currentId,
					label: match?.[2] || segment.replace(/:.*/, ''),
					type: match?.[1] || (index === 0 ? 'project' : 'object')
				};
			});
	}

	/**
	 * @param {string} qname
	 */
	function selectPickerObject(qname) {
		if (qname) {
			onSelectObject(qname);
		}
	}
</script>

<div class="studio-picker">
	<div class="studio-picker__toolbar">
		<button
			type="button"
			class="studio-picker__tool"
			class:studio-picker__tool--active={linked}
			aria-pressed={linked}
			title="Link with the projects tree selection"
			onclick={() => (linked = !linked)}
		>
			<Ico icon="mdi:smartphone-link" size={4} />
		</button>
		<button
			type="button"
			class="studio-picker__tool"
			disabled={!sourceDefinition.target}
			title="Select source object"
			onclick={() => selectPickerObject(sourceDefinition.target)}
		>
			<Ico icon="mdi:search" size={4} />
		</button>
		<button type="button" class="studio-picker__tool" disabled={true} title="Remove source">
			<Ico icon="mdi:delete-outline" size={4} />
		</button>
		<span class="studio-picker__mode">
			<Ico icon={pickerMode.icon} size={4} />
			{pickerMode.label}
		</span>
	</div>

	<div class="studio-picker__body">
		{#if !selectedId}
			<div class="studio-picker__empty">No object selected</div>
		{:else if loading}
			<div class="studio-picker__empty">Loading</div>
		{:else if error}
			<div class="studio-picker__empty">{error}</div>
		{:else}
			<section class="studio-picker__summary">
				<span>{pickerMode.label}</span>
				<strong title={selectedId}>{selectedName}</strong>
				<code title={selectedId}>{selectedId}</code>
				{#if selectionTrail.length > 1}
					<nav class="studio-picker__trail" aria-label="Selection parents">
						{#each selectionTrail as item, index (item.id)}
							{#if index > 0}
								<Ico icon="mdi:chevron-right" size={3} />
							{/if}
							<button
								type="button"
								title={item.id}
								disabled={item.id === selectedId}
								onclick={() => selectPickerObject(item.id)}
							>
								<span>{item.type}</span>
								{item.label}
							</button>
						{/each}
					</nav>
				{/if}
			</section>

			{#if pickerMode.kind === 'step'}
				<section class="studio-picker__section">
					<header>
						<span>Source</span>
						<strong>{sourceDefinition.empty ? 'empty' : 'defined'}</strong>
					</header>
					<div class="studio-picker__source-grid">
						<span>Object</span>
						<code>{sourceDefinition.target || '-'}</code>
						<span>XPath</span>
						<code>{sourceDefinition.xpath || '-'}</code>
					</div>
					{#if sourceTrail.length > 0}
						<nav class="studio-picker__trail studio-picker__trail--source" aria-label="Source path">
							{#each sourceTrail as item, index (item.id)}
								{#if index > 0}
									<Ico icon="mdi:chevron-right" size={3} />
								{/if}
								<button type="button" title={item.id} onclick={() => selectPickerObject(item.id)}>
									<span>{item.type}</span>
									{item.label}
								</button>
							{/each}
						</nav>
					{/if}
				</section>

				<section class="studio-picker__section studio-picker__section--grow">
					<header>
						<span>Preview</span>
						<strong>{sourceProperty?.displayName ?? 'sourceDefinition'}</strong>
					</header>
					<pre>{sourceDefinition.raw || selectedId}</pre>
				</section>
			{:else}
				<section class="studio-picker__section">
					<header>
						<span>Filters</span>
						<strong>{pickerMode.kind === 'smart' ? 'mobile' : 'database'}</strong>
					</header>
					<div class="studio-picker__chips">
						<span>Sequence</span>
						<span>Database</span>
						<span>Iteration</span>
						<span>Form</span>
						<span>Global</span>
						<span>Local</span>
					</div>
				</section>

				<section class="studio-picker__section studio-picker__section--grow">
					<header>
						<span>Selection</span>
						<strong>{objectClass || 'object'}</strong>
					</header>
					<pre>{selectedId}</pre>
				</section>
			{/if}
		{/if}
	</div>
</div>

<style>
	.studio-picker {
		display: flex;
		height: 100%;
		min-height: 0;
		flex-direction: column;
	}

	.studio-picker__toolbar {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.35rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		background: color-mix(in oklab, var(--color-surface-100-900) 82%, transparent);
		padding: 0.45rem;
	}

	.studio-picker__tool {
		display: grid;
		width: 1.85rem;
		height: 1.85rem;
		flex: 0 0 auto;
		place-items: center;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.3rem;
		background: var(--color-surface-50-950);
		color: var(--color-surface-700-300);
	}

	.studio-picker__tool:hover:not(:disabled),
	.studio-picker__tool--active {
		border-color: color-mix(in oklab, var(--color-primary-500) 45%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-picker__tool:disabled {
		color: var(--color-surface-400-600);
		cursor: not-allowed;
		opacity: 0.6;
	}

	.studio-picker__mode {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.35rem;
		margin-left: auto;
		overflow: hidden;
		color: var(--color-surface-700-300);
		font-size: 0.72rem;
		font-weight: 700;
		text-overflow: ellipsis;
		text-transform: uppercase;
		white-space: nowrap;
	}

	.studio-picker__body {
		display: flex;
		min-height: 0;
		flex: 1;
		flex-direction: column;
		gap: 0.5rem;
		overflow: auto;
		padding: 0.55rem;
	}

	.studio-picker__empty {
		display: grid;
		min-height: 8rem;
		place-items: center;
		color: var(--color-surface-600-400);
		font-size: 0.82rem;
	}

	.studio-picker__summary,
	.studio-picker__section {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 0.4rem;
		background: color-mix(in oklab, var(--color-surface-100-900) 55%, transparent);
	}

	.studio-picker__summary {
		display: grid;
		gap: 0.2rem;
		padding: 0.55rem;
	}

	.studio-picker__summary span,
	.studio-picker__section header span,
	.studio-picker__source-grid span {
		color: var(--color-surface-600-400);
		font-size: 0.66rem;
		font-weight: 750;
		text-transform: uppercase;
	}

	.studio-picker__summary strong,
	.studio-picker__summary code {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-picker__summary strong {
		font-size: 0.9rem;
	}

	.studio-picker__summary code,
	.studio-picker__source-grid code,
	.studio-picker__section pre {
		font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
		font-size: 0.68rem;
	}

	.studio-picker__summary code {
		color: var(--color-surface-600-400);
	}

	.studio-picker__trail {
		display: flex;
		min-width: 0;
		flex-wrap: wrap;
		align-items: center;
		gap: 0.18rem;
		padding-top: 0.25rem;
	}

	.studio-picker__trail--source {
		border-top: 1px solid var(--color-surface-200-800);
		padding: 0.45rem 0.55rem;
	}

	.studio-picker__trail button {
		display: inline-flex;
		max-width: 100%;
		min-width: 0;
		align-items: center;
		gap: 0.28rem;
		border: 1px solid var(--color-surface-200-800);
		border-radius: 999px;
		background: var(--color-surface-50-950);
		color: var(--color-surface-800-200);
		padding: 0.18rem 0.45rem;
		font-size: 0.68rem;
		font-weight: 650;
	}

	.studio-picker__trail button:hover:not(:disabled) {
		border-color: color-mix(in oklab, var(--color-primary-500) 45%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-picker__trail button:disabled {
		cursor: default;
		opacity: 0.72;
	}

	.studio-picker__trail button span {
		max-width: 3.5rem;
		overflow: hidden;
		color: var(--color-surface-500-500);
		text-overflow: ellipsis;
		text-transform: uppercase;
		white-space: nowrap;
	}

	.studio-picker__section {
		display: grid;
		min-height: 0;
		overflow: hidden;
	}

	.studio-picker__section--grow {
		flex: 1 1 auto;
	}

	.studio-picker__section header {
		display: flex;
		min-width: 0;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		border-bottom: 1px solid var(--color-surface-200-800);
		padding: 0.45rem 0.55rem;
	}

	.studio-picker__section header strong {
		min-width: 0;
		overflow: hidden;
		color: var(--color-surface-700-300);
		font-size: 0.68rem;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-picker__source-grid {
		display: grid;
		grid-template-columns: 4.5rem minmax(0, 1fr);
		gap: 0.45rem;
		padding: 0.55rem;
	}

	.studio-picker__source-grid code {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-picker__chips {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		padding: 0.55rem;
	}

	.studio-picker__chips span {
		border: 1px solid var(--color-surface-200-800);
		border-radius: 999px;
		background: var(--color-surface-50-950);
		color: var(--color-surface-700-300);
		padding: 0.18rem 0.45rem;
		font-size: 0.68rem;
		font-weight: 650;
	}

	.studio-picker__section pre {
		min-height: 0;
		overflow: auto;
		margin: 0;
		padding: 0.55rem;
		color: var(--color-surface-800-200);
		white-space: pre-wrap;
		word-break: break-word;
	}
</style>

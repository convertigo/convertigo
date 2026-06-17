<script>
	import { createDatabaseObjectProperties } from '$lib/common/DatabaseObjectProperties.svelte.js';
	import Ico from '$lib/utils/Ico.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';
	import StudioIconButton from './StudioIconButton.svelte';

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

<div class="studio-picker layout-y-stretch">
	<div class="studio-picker__toolbar layout-x-low studio-panel-toolbar">
		<StudioIconButton
			icon="mdi:smartphone-link"
			active={linked}
			aria-pressed={linked}
			title="Link with the projects tree selection"
			onclick={() => (linked = !linked)}
		/>
		<StudioIconButton
			icon="mdi:search"
			disabled={!sourceDefinition.target}
			title="Select source object"
			onclick={() => selectPickerObject(sourceDefinition.target)}
		/>
		<StudioIconButton icon="mdi:delete-outline" disabled={true} title="Remove source" />
		<span class="studio-picker__mode layout-x-low studio-ellipsis studio-caption">
			<Ico icon={pickerMode.icon} size={4} />
			{pickerMode.label}
		</span>
	</div>

	<div class="studio-picker__body layout-y-low">
		{#if !selectedId}
			<StudioEmptyState message="No object selected" />
		{:else if loading}
			<StudioEmptyState message="Loading" loading />
		{:else if error}
			<StudioEmptyState message={error} />
		{:else}
			<section class="studio-picker__summary layout-y-low studio-surface">
				<span class="studio-label">{pickerMode.label}</span>
				<strong class="studio-ellipsis" title={selectedId}>{selectedName}</strong>
				<code class="studio-ellipsis" title={selectedId}>{selectedId}</code>
				{#if selectionTrail.length > 1}
					<nav class="studio-picker__trail layout-x-wrap-none" aria-label="Selection parents">
						{#each selectionTrail as item, index (item.id)}
							{#if index > 0}
								<Ico icon="mdi:chevron-right" size={3} />
							{/if}
							<button
								type="button"
								class="studio-picker__trail-button studio-pill"
								title={item.id}
								disabled={item.id === selectedId}
								onclick={() => selectPickerObject(item.id)}
							>
								<span class="studio-ellipsis">{item.type}</span>
								{item.label}
							</button>
						{/each}
					</nav>
				{/if}
			</section>

			{#if pickerMode.kind === 'step'}
				<section class="studio-picker__section studio-surface">
					<header class="layout-x-between-low">
						<span class="studio-label">Source</span>
						<strong class="studio-ellipsis">{sourceDefinition.empty ? 'empty' : 'defined'}</strong>
					</header>
					<div class="studio-picker__source-grid">
						<span class="studio-label">Object</span>
						<code class="studio-ellipsis">{sourceDefinition.target || '-'}</code>
						<span class="studio-label">XPath</span>
						<code class="studio-ellipsis">{sourceDefinition.xpath || '-'}</code>
					</div>
					{#if sourceTrail.length > 0}
						<nav
							class="studio-picker__trail studio-picker__trail--source layout-x-wrap-none"
							aria-label="Source path"
						>
							{#each sourceTrail as item, index (item.id)}
								{#if index > 0}
									<Ico icon="mdi:chevron-right" size={3} />
								{/if}
								<button
									type="button"
									class="studio-picker__trail-button studio-pill"
									title={item.id}
									onclick={() => selectPickerObject(item.id)}
								>
									<span class="studio-ellipsis">{item.type}</span>
									{item.label}
								</button>
							{/each}
						</nav>
					{/if}
				</section>

				<section class="studio-picker__section studio-picker__section--grow studio-surface">
					<header class="layout-x-between-low">
						<span class="studio-label">Preview</span>
						<strong class="studio-ellipsis"
							>{sourceProperty?.displayName ?? 'sourceDefinition'}</strong
						>
					</header>
					<pre>{sourceDefinition.raw || selectedId}</pre>
				</section>
			{:else}
				<section class="studio-picker__section studio-surface">
					<header class="layout-x-between-low">
						<span class="studio-label">Filters</span>
						<strong class="studio-ellipsis"
							>{pickerMode.kind === 'smart' ? 'mobile' : 'database'}</strong
						>
					</header>
					<div class="studio-picker__chips layout-x-wrap-low">
						<span class="studio-pill">Sequence</span>
						<span class="studio-pill">Database</span>
						<span class="studio-pill">Iteration</span>
						<span class="studio-pill">Form</span>
						<span class="studio-pill">Global</span>
						<span class="studio-pill">Local</span>
					</div>
				</section>

				<section class="studio-picker__section studio-picker__section--grow studio-surface">
					<header class="layout-x-between-low">
						<span class="studio-label">Selection</span>
						<strong class="studio-ellipsis">{objectClass || 'object'}</strong>
					</header>
					<pre>{selectedId}</pre>
				</section>
			{/if}
		{/if}
	</div>
</div>

<style>
	.studio-picker {
		height: 100%;
		min-height: 0;
	}

	.studio-picker__toolbar {
		min-width: 0;
		align-items: center;
	}

	.studio-picker__mode {
		margin-left: auto;
	}

	.studio-picker__body {
		min-height: 0;
		flex: 1;
		overflow: auto;
		padding: 0.55rem;
	}

	.studio-picker__summary {
		padding: 0.55rem;
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
		padding-top: 0.25rem;
	}

	.studio-picker__trail--source {
		border-top: 1px solid var(--color-surface-200-800);
		padding: 0.45rem 0.55rem;
	}

	.studio-picker__trail-button:hover:not(:disabled) {
		border-color: color-mix(in oklab, var(--color-primary-500) 45%, transparent);
		background: color-mix(in oklab, var(--color-primary-500) 12%, transparent);
		color: var(--color-primary-600-400);
	}

	.studio-picker__trail-button:disabled {
		cursor: default;
		opacity: 0.72;
	}

	.studio-picker__trail-button span {
		max-width: 3.5rem;
		color: var(--color-surface-500-500);
		text-transform: uppercase;
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
		min-width: 0;
		border-bottom: 1px solid var(--color-surface-200-800);
		padding: 0.45rem 0.55rem;
	}

	.studio-picker__section header strong {
		color: var(--color-surface-700-300);
		font-size: 0.68rem;
	}

	.studio-picker__source-grid {
		display: grid;
		grid-template-columns: 4.5rem minmax(0, 1fr);
		gap: 0.45rem;
		padding: 0.55rem;
	}

	.studio-picker__chips {
		padding: 0.55rem;
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

<script>
	import LightSvelte from '$lib/common/Light.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { call } from '$lib/utils/service';
	import Editor from './editor/Editor.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/**
	 * The XML schema of the selected project, as the Schema view of the Eclipse Studio generates and
	 * validates it, one XSD per namespace.
	 *
	 * @type {{
	 *  selectedId?: string,
	 *  projectName?: string,
	 *  active?: boolean
	 * }}
	 */
	let { selectedId = '', projectName = '', active = false } = $props();

	/** @typedef {{ project: string, schemas: { namespace: string, xsd: string }[], valid: boolean, message: string }} SchemaInfo */
	let schema = $state.raw(/** @type {SchemaInfo | null} */ (null));
	let namespace = $state('');
	let full = $state(false);
	let loading = $state(false);
	let error = $state('');
	let loadedKey = '';
	let serial = 0;
	let current = $derived(
		schema?.schemas.find((candidate) => candidate.namespace === namespace) ?? schema?.schemas[0]
	);
	let theme = $derived(LightSvelte.light ? '' : 'vs-dark');

	$effect(() => {
		const key = `${projectName}\u0000${full}`;
		if (!active || !projectName || !selectedId || key === loadedKey) {
			return;
		}
		loadedKey = key;
		void load(false);
	});

	/**
	 * @param {boolean} refresh
	 */
	async function load(refresh) {
		const request = ++serial;
		loading = true;
		try {
			const response = await call('studio.treeview.Schema', {
				id: selectedId,
				full: String(full),
				refresh: String(refresh)
			});
			if (request !== serial) {
				return;
			}
			schema = Array.isArray(response?.schemas) ? response : null;
			error = schema
				? ''
				: String(
						response?.error?.message ??
							response?.message ??
							`The schema of ${projectName} cannot be generated, see the logs.`
					);
			if (schema && !schema.schemas.some((candidate) => candidate.namespace === namespace)) {
				namespace =
					schema.schemas.find((candidate) => candidate.namespace.includes(projectName))
						?.namespace ??
					schema.schemas[0]?.namespace ??
					'';
			}
		} finally {
			if (request === serial) {
				loading = false;
			}
		}
	}
</script>

<div class="studio-schema">
	{#if !projectName}
		<StudioEmptyState message="No project selected" small />
	{:else}
		<div class="studio-schema__bar">
			<select
				class="select-common"
				bind:value={namespace}
				aria-label="Namespace"
				disabled={!schema?.schemas.length}
			>
				{#each schema?.schemas ?? [] as candidate (candidate.namespace)}
					<option value={candidate.namespace}>{candidate.namespace || '(no namespace)'}</option>
				{/each}
			</select>
			<label class="studio-schema__full">
				<input type="checkbox" bind:checked={full} />
				Full schema
			</label>
			<button
				type="button"
				class="studio-schema__refresh"
				title="Generate the schema again"
				disabled={loading}
				onclick={() => load(true)}
			>
				<Ico icon={loading ? 'mdi:sync' : 'mdi:reload'} size={4} />
			</button>
			{#if schema}
				<span
					class={['studio-schema__message', !schema.valid && 'studio-schema__message--invalid']}
					title={schema.message}
				>
					<Ico icon={schema.valid ? 'mdi:check' : 'mdi:alert-circle-outline'} size={4} />
					{schema.message}
				</span>
			{/if}
		</div>
		<div class="studio-schema__body">
			{#if loading && !schema}
				<StudioEmptyState message={`Generating the ${projectName} schema`} loading small />
			{:else if error}
				<p class="studio-schema__error">{error}</p>
			{:else if current}
				{#key current.namespace}
					<Editor content={current.xsd} language="xml" {theme} readOnly={true} />
				{/key}
			{:else}
				<StudioEmptyState message="No schema" small />
			{/if}
		</div>
	{/if}
</div>

<style>
	.studio-schema {
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template-rows: auto minmax(0, 1fr);
	}

	.studio-schema__bar {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 0.6rem;
		padding: 0.35rem 0.85rem;
		font-size: 0.75rem;
	}

	.studio-schema__bar select {
		height: 1.7rem;
		min-width: 12rem;
		max-width: 24rem;
		padding: 0 0.4rem;
		font-size: 0.75rem;
	}

	.studio-schema__full {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
		white-space: nowrap;
	}

	.studio-schema__refresh {
		display: grid;
		width: 1.7rem;
		height: 1.7rem;
		place-items: center;
		border: 0;
		border-radius: 0.3rem;
		background: transparent;
		color: var(--studio-text-idle);
	}

	.studio-schema__refresh:hover {
		background: var(--studio-hover-bg);
		color: var(--studio-text-strong);
	}

	.studio-schema__message {
		display: inline-flex;
		min-width: 0;
		align-items: center;
		gap: 0.3rem;
		overflow: hidden;
		color: var(--color-success-600-400);
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-schema__message--invalid {
		color: var(--color-error-600-400);
	}

	.studio-schema__error {
		margin: 0;
		color: var(--color-error-600-400);
		padding: 0.4rem 0.85rem;
		font-size: 0.75rem;
	}

	.studio-schema__body {
		min-height: 0;
	}
</style>

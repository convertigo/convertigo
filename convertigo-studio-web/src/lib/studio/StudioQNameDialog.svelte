<script>
	import { call } from '#lib/utils/service.js';

	/**
	 * Chooses a type or an element of the schemas of the project for a qualified name property, as the
	 * QName editor of the Eclipse Studio: an existing one, a dynamic one to create in the namespace of the
	 * project, or none.
	 *
	 * @type {{
	 *  id: string,
	 *  property: string,
	 *  label?: string,
	 *  value?: string,
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { id, property, label = property, value = '', onApply, onClose } = $props();

	/** @typedef {{ qname: string, namespace: string, name: string, kind: string, dynamic: boolean, readOnly: boolean }} QNameItem */

	/** @type {QNameItem[]} */
	let items = $state([]);
	let kind = $state('type');
	let projectNamespace = $state('');
	let filter = $state('');
	let namespace = $state('');
	let name = $state('');

	$effect.pre(() => {
		const match = /^\{(.*)\}(.*)$/.exec(value);
		namespace = match ? match[1] : '';
		name = match ? match[2] : value;
	});

	$effect(() => {
		void call('studio.properties.QNames', { id, property }).then((result) => {
			items = Array.isArray(result?.items) ? result.items : [];
			kind = String(result?.kind ?? 'type');
			projectNamespace = String(result?.namespace ?? '');
		});
	});

	let what = $derived(kind === 'element' ? 'element' : 'type');
	// a name without namespace has no braces, as QName.toString() gives it
	let qname = $derived(name ? (namespace ? `{${namespace}}${name}` : name) : '');
	let selected = $derived(items.find((item) => item.qname === qname));
	let shown = $derived.by(() => {
		const text = filter.trim().toLowerCase();
		const matching = text
			? items.filter((item) => `${item.name} ${item.namespace}`.toLowerCase().includes(text))
			: items;
		/** @type {Map<string, QNameItem[]>} */
		const groups = new Map();
		for (const item of matching) {
			groups.set(item.namespace, [...(groups.get(item.namespace) ?? []), item]);
		}
		return [...groups];
	});
	let summary = $derived(
		!name
			? `No ${what} set.`
			: selected
				? `Use the ${selected.dynamic ? (selected.readOnly ? 'dynamic read-only' : 'dynamic') : 'static'} ${what} ${name}.`
				: `Create the dynamic ${what} ${name} in ${namespace}.`
	);

	/**
	 * @param {string} next
	 */
	function typeName(next) {
		name = next.replace(/\s/g, '');
		namespace = projectNamespace;
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-qname-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-qname-title">{label}</strong>
		</header>
		<form
			class="studio-dialog__form"
			onsubmit={(event) => {
				event.preventDefault();
				onApply?.(qname);
			}}
		>
			<div class="studio-dialog__body">
				<input
					class="input-common"
					type="search"
					placeholder={`Filter the existing ${what}s`}
					aria-label="Filter"
					bind:value={filter}
				/>
				<div class="studio-qname__list" role="listbox" aria-label={`Existing ${what}s`}>
					{#each shown as [group, groupItems] (group)}
						<div class="studio-qname__namespace" title={group}>{group || 'No namespace'}</div>
						{#each groupItems as item (item.qname)}
							<button
								type="button"
								role="option"
								aria-selected={item.qname === qname}
								class={[
									'studio-qname__item',
									item.qname === qname && 'studio-qname__item--selected'
								]}
								onclick={() => {
									namespace = item.namespace;
									name = item.name;
								}}
							>
								<span>{item.name}</span>
								{#if item.dynamic}
									<small>{item.readOnly ? 'dynamic, read-only' : 'dynamic'}</small>
								{/if}
							</button>
						{/each}
					{:else}
						<p class="studio-qname__empty">No {what} found.</p>
					{/each}
				</div>
				<label class="studio-dialog__field">
					<span>Namespace</span>
					<input class="input-common" value={namespace} readonly />
				</label>
				<label class="studio-dialog__field">
					<span>Local name</span>
					<input
						class="input-common"
						value={name}
						disabled={kind === 'simpleType'}
						oninput={(event) => typeName(event.currentTarget.value)}
					/>
				</label>
				<p class="studio-qname__summary" role="status">{summary}</p>
			</div>
			<footer class="studio-dialog__footer">
				<button
					type="button"
					class="button-secondary"
					disabled={kind === 'simpleType'}
					onclick={() => {
						name = '';
						namespace = '';
					}}>No {what}</button
				>
				<span class="studio-qname__spacer"></span>
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary">Apply</button>
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-dialog {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-dialog__box {
		display: grid;
		width: min(34rem, 100%);
		max-height: min(40rem, 100%);
		grid-template-rows: auto minmax(0, 1fr);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: grid;
		min-height: 0;
		grid-template-rows: minmax(0, 1fr) auto;
	}

	.studio-dialog__header,
	.studio-dialog__footer {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
	}

	.studio-dialog__header {
		justify-content: flex-start;
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__footer {
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__body {
		display: grid;
		min-height: 0;
		grid-template-rows: auto minmax(8rem, 1fr) auto auto auto;
		gap: 0.6rem;
		overflow: hidden;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__body input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-qname__list {
		min-height: 0;
		overflow-y: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.25rem;
	}

	.studio-qname__namespace {
		overflow: hidden;
		padding: 0.35rem 0.4rem 0.15rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		font-weight: 600;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-qname__item {
		display: flex;
		width: 100%;
		align-items: baseline;
		justify-content: space-between;
		gap: 0.5rem;
		border: 0;
		border-radius: 0.25rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.25rem 0.5rem;
		text-align: left;
	}

	.studio-qname__item:hover {
		background: var(--studio-hover-bg);
	}

	.studio-qname__item--selected {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-qname__item small,
	.studio-qname__empty {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-qname__empty {
		margin: 0.5rem;
	}

	.studio-qname__summary {
		margin: 0;
		color: var(--studio-text-idle);
	}

	.studio-qname__spacer {
		flex: 1;
	}
</style>

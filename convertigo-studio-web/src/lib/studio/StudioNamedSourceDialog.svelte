<script>
	import { call } from '$lib/utils/service';

	/**
	 * Chooses the object a property names, as the named source selector of the Eclipse Studio: a
	 * requestable, a page, a shared component, a view… among the ones the property can name, grouped
	 * by project, or none.
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

	/** @typedef {{ name: string, label: string, type: string, project: string }} NamedSource */

	/** @type {NamedSource[]} */
	let items = $state([]);
	let loaded = $state(false);
	let filter = $state('');
	let chosen = $state('');

	$effect.pre(() => {
		chosen = value;
	});

	$effect(() => {
		void call('studio.properties.NamedSources', { id, property }).then((result) => {
			items = Array.isArray(result?.items) ? result.items : [];
			loaded = true;
		});
	});

	let groups = $derived.by(() => {
		const text = filter.trim().toLowerCase();
		/** @type {Map<string, NamedSource[]>} */
		const byProject = new Map();
		for (const item of items) {
			if (text && !`${item.name} ${item.type}`.toLowerCase().includes(text)) {
				continue;
			}
			byProject.set(item.project, [...(byProject.get(item.project) ?? []), item]);
		}
		return [...byProject];
	});

	/**
	 * @param {NamedSource} item
	 * @returns {string}
	 */
	function path(item) {
		// the path of the object in its project, without the project
		return item.name.startsWith(`${item.project}.`)
			? item.name.slice(item.project.length + 1)
			: item.name;
	}
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-named-source-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-named-source-title">{label}</strong>
		</header>
		<form
			class="studio-dialog__form"
			onsubmit={(event) => {
				event.preventDefault();
				onApply?.(chosen);
			}}
		>
			<div class="studio-dialog__body">
				<input
					class="input-common"
					type="search"
					placeholder="Filter"
					aria-label="Filter"
					bind:value={filter}
				/>
				<div class="studio-named__list" role="listbox" aria-label={label}>
					{#each groups as [project, projectItems] (project)}
						<div class="studio-named__project">{project}</div>
						{#each projectItems as item (item.name)}
							<button
								type="button"
								role="option"
								aria-selected={item.name === chosen}
								title={item.name}
								class={[
									'studio-named__item',
									item.name === chosen && 'studio-named__item--selected'
								]}
								onclick={() => (chosen = item.name)}
								ondblclick={() => onApply?.(item.name)}
							>
								<span class="studio-ellipsis">{path(item)}</span>
								<small>{item.type}</small>
							</button>
						{/each}
					{:else}
						<p class="studio-named__empty">
							{loaded ? 'No object this property can name.' : 'Loading…'}
						</p>
					{/each}
				</div>
				<p class="studio-named__summary studio-ellipsis" role="status" title={chosen}>
					{chosen || 'No object named.'}
				</p>
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => (chosen = '')}>None</button>
				<span class="studio-named__spacer"></span>
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
		width: min(36rem, 100%);
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
		grid-template-rows: auto minmax(8rem, 1fr) auto;
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

	.studio-named__list {
		min-height: 0;
		overflow-y: auto;
		border: 1px solid var(--studio-line);
		border-radius: 0.35rem;
		padding: 0.25rem;
	}

	.studio-named__project {
		padding: 0.35rem 0.4rem 0.15rem;
		color: var(--studio-text-idle);
		font-size: 0.7rem;
		font-weight: 600;
	}

	.studio-named__item {
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

	.studio-named__item:hover {
		background: var(--studio-hover-bg);
	}

	.studio-named__item--selected {
		background: var(--studio-selection-bg);
		color: var(--studio-text-strong);
	}

	.studio-named__item small,
	.studio-named__empty,
	.studio-named__summary {
		color: var(--studio-text-idle);
		font-size: 0.7rem;
	}

	.studio-named__empty,
	.studio-named__summary {
		margin: 0;
	}

	.studio-named__spacer {
		flex: 1;
	}
</style>

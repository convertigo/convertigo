<script>
	import { Dialog } from '@skeletonlabs/skeleton-svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import ModalDynamic from '$lib/common/components/ModalDynamic.svelte';
	import ModalYesNo from '$lib/common/components/ModalYesNo.svelte';
	import { call } from '$lib/utils/service';
	import StudioOrderedList from './StudioOrderedList.svelte';
	import StudioReferenceList from './StudioReferenceList.svelte';

	/** @type {{ onChanged?: (result: any) => Promise<void> | void }} */
	let { onChanged } = $props();
	let modal;
	let confirmation;
	async function confirm(message) {
		return await confirmation.open({ title: 'Confirm tag change', message });
	}
	let scope = $state('projectObjects');
	let project = $state('');
	let targets = $state(/** @type {string[]} */ ([]));
	let snapshot = $state(/** @type {any} */ (null));
	let selected = $state('');
	let referenceProject = $state('');
	let definition = $state(/** @type {any} */ ({ label: '', description: '', metadata: {} }));
	let search = $state('');
	let busy = $state(false);
	let error = $state('');
	let orderTarget = $state('');
	let orderedTagIds = $derived(
		/** @type {string[]} */ (snapshot?.assignments?.[orderTarget] ?? [])
	);
	let definitions = $derived(
		Object.entries(snapshot?.tags ?? {})
			.map(([id, tag]) => ({ id, .../** @type {any} */ (tag) }))
			.filter((tag) => tag.label.toLocaleLowerCase().includes(search.toLocaleLowerCase()))
			.sort((a, b) => a.label.localeCompare(b.label) || a.id.localeCompare(b.id))
	);
	let members = $derived(
		!selected && referenceProject
			? (snapshot?.referenceTargets ?? [])
			: Object.entries(snapshot?.assignments ?? {})
					.filter(([, ids]) => /** @type {string[]} */ (ids).includes(selected))
					.map(([target]) => target)
	);
	let availableTargets = $derived(
		[
			...new Set(
				scope === 'workspaceProjects'
					? (snapshot?.projects ?? [])
					: (snapshot?.targets ?? [...targets, ...Object.keys(snapshot?.assignments ?? {})])
			)
		].sort()
	);
	let publication = $derived(
		(snapshot?.publication ?? []).filter((item) => item.tagId === selected)
	);
	let conflict = $derived((snapshot?.conflicts ?? []).find((item) => item.tagId === selected));

	/** @param {string} nextScope @param {string} name @param {string[]} selectedTargets @param {string} tagId @param {string} fromReferences */
	export async function open(
		nextScope,
		name,
		selectedTargets = [],
		tagId = '',
		fromReferences = ''
	) {
		scope = nextScope;
		project = name;
		targets = [...new Set(selectedTargets)];
		orderTarget = targets[0] ?? '';
		search = '';
		selected = '';
		referenceProject = fromReferences;
		definition = { label: referenceProject, description: '', metadata: {} };
		await refresh();
		if (!availableTargets.includes(orderTarget)) orderTarget = availableTargets[0] ?? '';
		const first =
			tagId ||
			selectedTargets.flatMap((target) => snapshot?.assignments?.[target] ?? [])[0] ||
			definitions[0]?.id;
		if (!referenceProject && first && snapshot?.tags?.[first]) edit(first);
		return modal.open({ scope });
	}

	async function refresh() {
		error = '';
		snapshot = await call(
			'studio.tags.Get',
			{ scope, project, referenceProject },
			{ silentError: () => true }
		);
		if (snapshot?.isError || !snapshot?.revision) {
			error = String(
				snapshot?.error?.message ??
					snapshot?.message ??
					snapshot?.error ??
					'Unable to read tags. Refresh to try again.'
			);
			snapshot = { ...snapshot, readOnly: true };
		}
	}

	/** @param {string} id */
	function edit(id) {
		selected = id;
		referenceProject = '';
		definition = JSON.parse(JSON.stringify(snapshot.tags[id]));
		definition.metadata ??= {};
		definition.description ??= '';
	}

	/** @param {string} action @param {any} input */
	async function apply(action, input) {
		busy = true;
		error = '';
		try {
			const result = await call(
				'studio.tags.Apply',
				{
					scope,
					project,
					revision: snapshot.revision,
					action,
					input: JSON.stringify(input)
				},
				{ silentError: () => true }
			);
			if (!result?.done) {
				error = String(
					result?.error?.message ??
						result?.message ??
						'The change was refused. Refresh before trying again.'
				);
				return null;
			}
			snapshot = { ...result, suggestions: result.suggestions ?? snapshot?.suggestions };
			await onChanged?.(result);
			if (selected && snapshot.tags[selected] && !['assign', 'remove', 'reorder'].includes(action))
				edit(selected);
			return result;
		} catch (cause) {
			error = cause instanceof Error ? cause.message : String(cause);
			return null;
		} finally {
			busy = false;
		}
	}

	async function saveDefinition() {
		const result = await apply(
			selected ? 'update' : referenceProject ? 'createFromReferences' : 'create',
			{ id: selected, definition, project: referenceProject }
		);
		if (result?.id) {
			edit(result.id);
		}
	}

	async function deleteDefinition() {
		if (
			!(await confirm(
				`Delete “${definition.label}” and its ${members.length} membership(s)? Objects will remain in the project.`
			))
		)
			return;
		await apply('delete', { id: selected, memberCount: members.length, confirmed: true });
		selected = '';
		definition = { label: '', description: '', metadata: {} };
	}

	/** @param {string} target @param {Event & { currentTarget: HTMLInputElement }} event */
	async function changeMembership(target, event) {
		orderTarget = target;
		const checkbox = event.currentTarget;
		const result = await apply(checkbox.checked ? 'assign' : 'remove', {
			targets: [target],
			tagIds: [selected]
		});
		if (!result) checkbox.checked = Boolean(snapshot?.assignments?.[target]?.includes(selected));
	}
	/** @param {string} target */
	function targetLabel(target) {
		return scope === 'workspaceProjects' ? target : target.replace(/^.*\.sq[:~]/, '');
	}
	/** @param {number} index @param {number} offset */
	async function moveTag(index, offset) {
		if (busy || snapshot?.readOnly || index + offset < 0 || index + offset >= orderedTagIds.length)
			return;
		const tagIds = [...orderedTagIds];
		[tagIds[index], tagIds[index + offset]] = [tagIds[index + offset], tagIds[index]];
		await apply('reorder', { targets: [orderTarget], tagIds });
	}
	function newTag() {
		if (busy) return;
		selected = '';
		referenceProject = '';
		definition = { label: '', description: '', metadata: {} };
	}

	async function resolve(fromProject = '', alignSources = false) {
		if (
			!(await confirm(
				alignSources
					? 'Prepare alignment of all known member sources to the chosen local presentation? Save each project explicitly afterward.'
					: `Use ${fromProject || 'the current workspace'} presentation for this shared tag? Portable sources will keep their definitions.`
			))
		)
			return;
		await apply('resolve', { id: selected, fromProject, alignSources, confirmed: true });
	}
	async function share() {
		if (
			!(await confirm(
				`Prepare ${definition.shared ? 'removal of portable declarations' : 'sharing'} for: ${members.join(', ')}. Save each project explicitly to publish this change.`
			))
		)
			return;
		await apply('share', { id: selected, shared: !definition.shared, confirmed: true });
	}
	async function republish() {
		if (await confirm(`Republish to ${members.join(', ')}? Save each project afterward.`))
			await apply('republish', { id: selected, confirmed: true });
	}

	/** @param {string} namespace @param {string} field @param {any} value */
	function setMetadata(namespace, field, value) {
		definition.metadata[namespace] ??= {};
		if (value === undefined) delete definition.metadata[namespace][field];
		else definition.metadata[namespace][field] = value;
	}
</script>

<ModalDynamic bind:this={modal} class="w-full max-w-4xl p">
	{#snippet children({ close })}
		<div class="studio-tags">
			<header class="layout-x-between-low">
				<Dialog.Title class="h4"
					>{scope === 'workspaceProjects'
						? 'Project tags'
						: `Sequence tags — ${project}`}</Dialog.Title
				>
				<Button label="Close" cls="button-secondary" full={false} onclick={() => close()} />
			</header>
			<Dialog.Description class="studio-tags__hint"
				>Select a tag, then check the {scope === 'workspaceProjects' ? 'projects' : 'sequences'} that
				belong to it.</Dialog.Description
			>
			{#if referenceProject}
				<p class="studio-tags__hint">
					Create a local tag for {referenceProject} and its direct and indirect project references. Review
					the checked projects, then choose Create tag. Only projects present in this workspace are included;
					unavailable references are listed below. Memberships remain editable afterward; they do not
					track future reference changes.
				</p>
			{/if}
			{#if error}<p role="alert">{error}</p>{/if}
			{#each snapshot?.diagnostics ?? [] as message (message)}<p role="alert">{message}</p>{/each}
			<div class="studio-tags__body">
				<aside class="studio-tags__list" aria-label="Tags">
					<label class="studio-tags__field"
						>Find a tag<input
							class="input"
							type="search"
							bind:value={search}
							placeholder="Search"
						/></label
					>
					<div class="studio-tags__choices">
						{#each definitions as tag (tag.id)}
							<button
								type="button"
								class="studio-tags__choice"
								class:studio-tags__choice--selected={selected === tag.id}
								aria-pressed={selected === tag.id}
								disabled={busy}
								onclick={() => edit(tag.id)}
							>
								<span
									class="studio-tag-color"
									style:background={tag.presentation?.color ?? 'currentColor'}
								></span>
								<span>{tag.label}</span>
							</button>
						{:else}<p class="studio-tags__hint">No tags yet.</p>{/each}
					</div>
					<Button
						label="New tag"
						icon="mdi:plus"
						cls="button-secondary"
						disabled={busy || snapshot?.readOnly}
						onclick={newTag}
					/>
				</aside>
				<section class="studio-tags__editor" aria-label="Tag details">
					<form
						class="studio-tags__form"
						onsubmit={(event) => {
							event.preventDefault();
							void saveDefinition();
						}}
					>
						<fieldset class="studio-tags__form" disabled={busy || snapshot?.readOnly}>
							<div class="studio-tags__identity">
								<label class="studio-tags__field"
									>Label<input
										class="input"
										required
										maxlength="256"
										bind:value={definition.label}
									/></label
								>
								<label class="studio-tags__field"
									>Color<input
										type="color"
										value={definition.presentation?.color ?? '#2563eb'}
										oninput={(event) => {
											definition.presentation = {
												...definition.presentation,
												color: event.currentTarget.value
											};
										}}
									/></label
								>
							</div>
							<label class="studio-tags__field"
								>Description<textarea
									class="textarea"
									rows="2"
									maxlength="4096"
									bind:value={definition.description}></textarea></label
							>
						</fieldset>
						<div class="layout-x-start-low">
							<Button
								type="submit"
								label={selected ? 'Update tag' : 'Create tag'}
								cls="button-primary"
								full={false}
								disabled={busy || snapshot?.readOnly || !definition.label.trim()}
							/>
						</div>
						{#if selected || referenceProject}
							<fieldset
								class="studio-tags__members"
								disabled={busy || snapshot?.readOnly || !selected}
							>
								<legend
									>{scope === 'workspaceProjects' ? 'Projects' : 'Sequences'}
									<span class="studio-tags__hint">({members.length})</span></legend
								>
								<div class="studio-tags__member-list">
									{#each availableTargets as target (target)}
										<label class="studio-tags__member" title={target}>
											<input
												type="checkbox"
												aria-label={`Include ${targetLabel(target)}`}
												checked={members.includes(target)}
												onchange={(event) => changeMembership(target, event)}
											/>
											<span>{targetLabel(target)}</span>
										</label>
									{:else}<p class="studio-tags__hint">
											No {scope === 'workspaceProjects' ? 'projects' : 'sequences'} available.
										</p>{/each}
								</div>
							</fieldset>
						{/if}
						{#if selected}
							<fieldset class="layout-y-low" disabled={busy || snapshot?.readOnly}>
								<legend>Tag order</legend>
								<label class="studio-tags__field"
									>{scope === 'workspaceProjects' ? 'Project' : 'Sequence'}
									<select class="select" bind:value={orderTarget}>
										{#each availableTargets as target (target)}
											<option value={target}>{targetLabel(target)}</option>
										{/each}
									</select>
								</label>
								<p class="studio-tags__hint">
									Applied from top to bottom. Later tags take precedence when an extension combines
									values.
								</p>
								<StudioOrderedList
									values={orderedTagIds}
									labelFor={(id) => snapshot?.tags?.[id]?.label ?? id}
									disabled={busy || snapshot?.readOnly}
									onMove={moveTag}
								/>
							</fieldset>
							{#if scope === 'workspaceProjects'}
								<details class="studio-tags__advanced">
									<summary>Sharing</summary>
									<div class="studio-tags__form">
										<label class="studio-tags__member"
											><input
												type="checkbox"
												checked={definition.shared ?? false}
												disabled={busy || snapshot?.readOnly}
												onchange={async (event) => {
													const checkbox = event.currentTarget;
													await share();
													checkbox.checked = definition.shared ?? false;
												}}
											/>Share with other workspaces</label
										>
										<p class="studio-tags__hint">
											Shared tags travel with their projects. Save each modified project to publish
											changes.
										</p>
										{#each publication as item (item.project)}<p>
												{item.project}: {item.status === 'pendingSave'
													? 'Save required'
													: item.status}{!item.open ? ' — closed' : ''}
											</p>{/each}
										{#if definition.shared}<Button
												label="Prepare republication"
												cls="button-secondary"
												full={false}
												disabled={busy || snapshot?.readOnly}
												onclick={republish}
											/>{/if}
										{#if conflict}
											<p role="alert">
												Shared definitions differ. Choose which presentation to use here.
											</p>
											<Button
												label="Keep current local presentation"
												cls="button-secondary"
												disabled={busy || snapshot?.readOnly}
												onclick={() => resolve()}
											/>
											{#each Object.entries(conflict.sources) as [name, source] (name)}<Button
													label={`Use ${source.label} from ${name}`}
													cls="button-secondary"
													disabled={busy || snapshot?.readOnly}
													onclick={() => resolve(name)}
												/>{/each}
											<Button
												label="Prepare alignment of member sources"
												cls="button-secondary"
												disabled={busy || snapshot?.readOnly}
												onclick={() => resolve('', true)}
											/>
										{/if}
									</div>
								</details>
							{/if}
						{/if}
						<details class="studio-tags__advanced">
							<summary>Advanced</summary>
							<fieldset class="studio-tags__form" disabled={busy || snapshot?.readOnly}>
								{#each Object.entries(snapshot?.contributions ?? {}) as [namespace, contribution] (namespace)}
									<fieldset class="layout-y-low">
										<legend>{contribution.label}</legend>
										{#each Object.entries(contribution.fields) as [field, schema] (field)}
											{#if schema.type === 'array'}
												<StudioReferenceList
													label={schema.label}
													values={definition.metadata?.[namespace]?.[field] ?? []}
													choices={schema.items.enum}
													disabled={busy || snapshot?.readOnly}
													onChange={(values) => setMetadata(namespace, field, values)}
												/>
												{#if schema.description}<p class="studio-tags__hint">
														{schema.description}
													</p>{/if}
											{:else}
												<label class="layout-y-low"
													>{schema.label}
													{#if schema.type === 'boolean'}
														<input
															type="checkbox"
															checked={definition.metadata?.[namespace]?.[field] ?? false}
															onchange={(event) =>
																setMetadata(namespace, field, event.currentTarget.checked)}
														/>
													{:else if schema.enum}
														<select
															class="select"
															value={definition.metadata?.[namespace]?.[field] ?? ''}
															onchange={(event) =>
																setMetadata(
																	namespace,
																	field,
																	schema.enum.find(
																		(value) => String(value) === event.currentTarget.value
																	)
																)}
															><option value="">Choose…</option
															>{#each schema.enum as value (value)}<option value={String(value)}
																	>{String(value)}</option
																>{/each}</select
														>
													{:else}
														<input
															class="input"
															type={schema.type === 'string' ? 'text' : 'number'}
															step={schema.type === 'integer' ? '1' : 'any'}
															value={definition.metadata?.[namespace]?.[field] ?? ''}
															oninput={(event) =>
																setMetadata(
																	namespace,
																	field,
																	schema.type === 'string'
																		? event.currentTarget.value
																		: event.currentTarget.value === ''
																			? undefined
																			: Number(event.currentTarget.value)
																)}
														/>
													{/if}
												</label>
											{/if}
										{/each}
									</fieldset>
								{/each}
								{#each Object.entries(definition.metadata ?? {}).filter(([namespace]) => !snapshot?.contributions?.[namespace]) as [namespace, values] (namespace)}
									<details>
										<summary>{namespace} — extension unavailable (read only)</summary>
										<pre>{JSON.stringify(values, null, 2)}</pre>
									</details>
								{/each}
							</fieldset>
							{#if selected}<p class="studio-tags__hint">ID: {selected}</p>
								<Button
									label="Delete tag…"
									cls="button-secondary"
									full={false}
									disabled={busy || snapshot?.readOnly}
									onclick={deleteDefinition}
								/>{/if}
							{#if scope === 'projectObjects' && snapshot?.suggestions?.length}
								<p class="studio-tags__hint">Copy a tag from another open project:</p>
								{#each snapshot.suggestions as suggestion (`${suggestion.project}:${suggestion.sourceId}`)}<Button
										label={`Copy ${suggestion.definition.label} from ${suggestion.project}`}
										cls="button-secondary"
										disabled={busy || snapshot?.readOnly}
										onclick={async () => {
											const result = await apply('create', { definition: suggestion.definition });
											if (result?.id) edit(result.id);
										}}
									/>{/each}
							{/if}
						</details>
					</form>
				</section>
			</div>
			<footer class="layout-x-between-low">
				<p class="studio-tags__hint" role="status">
					{scope === 'workspaceProjects'
						? 'Memberships are saved in this workspace.'
						: snapshot?.dirty
							? 'Modified — save the project to keep these tags.'
							: 'Sequence tags are saved with the project.'}
				</p>
				<Button
					label="Refresh tags"
					icon="mdi:refresh"
					cls="button-secondary"
					full={false}
					disabled={busy}
					onclick={async () => {
						await refresh();
						if (selected && snapshot?.tags?.[selected]) edit(selected);
					}}
				/>
			</footer>
		</div>
	{/snippet}
</ModalDynamic>
<ModalYesNo bind:this={confirmation} />

<style>
	.studio-tags {
		display: grid;
		gap: 1rem;
		width: 100%;
		text-align: left;
	}
	.studio-tags__body {
		display: grid;
		grid-template-columns: minmax(11rem, 0.75fr) minmax(0, 1.75fr);
		gap: 1.25rem;
		min-height: 23rem;
	}
	.studio-tags__list,
	.studio-tags__editor,
	.studio-tags__form,
	.studio-tags__field {
		display: grid;
		align-content: start;
		gap: 0.65rem;
		min-width: 0;
	}
	.studio-tags__list {
		grid-template-rows: auto minmax(10rem, 1fr) auto;
		border-right: 1px solid var(--color-surface-200-800);
		padding-right: 1.25rem;
	}
	.studio-tags__choices {
		overflow: auto;
	}
	.studio-tags__choice {
		display: flex;
		width: 100%;
		gap: 0.55rem;
		align-items: center;
		text-align: left;
		padding: 0.6rem;
		border-radius: var(--radius-base);
	}
	.studio-tags__choice:hover,
	.studio-tags__choice--selected {
		background: color-mix(in oklab, var(--color-primary-500) 16%, transparent);
	}
	.studio-tags__choice--selected {
		box-shadow: inset 3px 0 var(--color-primary-500);
	}
	.studio-tags__identity {
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		gap: 0.75rem;
		align-items: end;
	}
	.studio-tags__identity input[type='color'] {
		width: 3rem;
		height: 2.4rem;
	}
	.studio-tags__members {
		border-top: 1px solid var(--color-surface-200-800);
		padding-top: 0.65rem;
	}
	.studio-tags__members legend {
		font-weight: 600;
	}
	.studio-tags__member-list {
		max-height: 12rem;
		overflow: auto;
		padding-top: 0.4rem;
	}
	.studio-tags__member {
		display: flex;
		align-items: center;
		gap: 0.55rem;
		padding: 0.35rem 0;
	}
	.studio-tags__advanced {
		border-top: 1px solid var(--color-surface-200-800);
		padding-top: 0.65rem;
	}
	.studio-tags__advanced > summary {
		cursor: pointer;
		font-weight: 600;
		margin-bottom: 0.5rem;
	}
	.studio-tags__hint {
		font-size: 0.8rem;
		opacity: 0.7;
	}
	.studio-tags :global(button) {
		height: auto;
	}
	.studio-tag-color {
		border-radius: 50%;
		width: 0.55rem;
		height: 0.55rem;
		flex-shrink: 0;
	}
	@media (max-width: 640px) {
		.studio-tags__body {
			grid-template-columns: 1fr;
		}
		.studio-tags__list {
			grid-template-rows: auto auto auto;
			border-right: 0;
			border-bottom: 1px solid var(--color-surface-200-800);
			padding: 0 0 1rem;
		}
		.studio-tags__choices {
			max-height: 8rem;
		}
	}
</style>

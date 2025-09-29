<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import RequestableVariables from '$lib/admin/components/RequestableVariables.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import LightSvelte from '$lib/common/Light.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { callRequestable } from '$lib/utils/service';
	import { decode } from 'html-entities';
	import { marked } from 'marked';
	import { flip } from 'svelte/animate';
	import { fly } from 'svelte/transition';

	let project = $state(TestPlatform(page.params.project));
	let searchQuery = $state('');

	const modes = ['JSON', 'XML', 'BIN', 'CXML'];
	let mode = $state(modes[0]);

	const accessibilities = $state({
		Private: {
			accent: 'preset-filled-success-200-800',
			chip: 'preset-filled-success-50-950 odd:preset-filled-success-100-900',
			soft: 'preset-outlined-success-50-950',
			tone: 'text-success-600-300',
			icon: 'mdi:lock',
			enabled: true
		},
		Hidden: {
			accent: 'preset-filled-warning-200-800',
			chip: 'preset-filled-warning-50-950 odd:preset-filled-warning-100-900',
			soft: 'preset-outlined-warning-50-950',
			tone: 'text-warning-600-300',
			icon: 'mdi:eye-off',
			enabled: true
		},
		Public: {
			accent: 'preset-filled-error-200-800',
			chip: 'preset-filled-error-50-950 odd:preset-filled-error-100-900',
			soft: 'preset-outlined-error-50-950',
			tone: 'text-error-600-300',
			icon: 'mdi:lock-open-variant',
			enabled: true
		}
	});

	/**
	 * @param {string} markdown
	 */
	function convertMarkdownToHtml(markdown) {
		const cleanedMarkdown = decode(markdown);
		return marked(cleanedMarkdown);
	}

	async function run(requestable, event) {
		event.preventDefault?.();

		if (event.submitter.textContent == 'Clear') {
			requestable.response = '';
			return;
		}
		requestable.loading = true;
		requestable.response = 'Loading …';
		const fd = new FormData(event.target);
		if (event.submitter.value) {
			fd.append('__testcase', event.submitter.value);
			for (const key of [...fd.keys()]) {
				if (!key.startsWith('__')) {
					fd.delete(key);
				}
			}
		} else {
			for (const variable of requestable.variable) {
				if (variable.send == 'false') {
					fd.delete(variable.name);
				}
			}
		}
		const data = await callRequestable(mode, project.name, fd);
		requestable.response = await data.text();
		requestable.language = data.headers.get('Content-Type')?.includes('json') ? 'json' : 'xml';
		requestable.loading = false;
	}

	let parts = $state([]);
	let partsOpened = $state(['Sequences']);
	$effect(() => {
		const _parts = [
			{ name: 'Sequences', requestables: project.sequence, comment: 'high level requestables' }
		];
		for (let connector of project.connector) {
			_parts.push({
				name: connector.name,
				comment: connector.comment,
				requestables: connector.transaction
			});
		}
		parts = _parts
			.map((part) => ({
				...part,
				requestables: part.requestables.filter(
					({ accessibility, name }) =>
						accessibilities[accessibility].enabled &&
						name?.toLowerCase().includes(searchQuery.toLowerCase())
				)
			}))
			.filter((part) => part.requestables.length > 0);
	});
	const [duration, y] = [200, -50];
</script>

<Card title={project?.name ?? null}>
	{#snippet cornerOption()}
		<InputGroup
			id="search"
			type="search"
			placeholder="Search requestable..."
			class="bg-surface-200-800"
			actionsClass="pr-1"
			icon="mdi:magnify"
			bind:value={searchQuery}
		>
			{#snippet actions()}
				{#each Object.values(accessibilities) as accessibility, idx}
					<button
						type="button"
						class="flex h-full items-center rounded-none p-2 text-[11px] font-semibold tracking-wide uppercase transition-all duration-200 {accessibility.chip}"
						class:rounded-l={idx === 0}
						class:rounded-r={idx === 2}
						class:opacity-50={!accessibility.enabled}
						onclick={() => {
							accessibility.enabled = !accessibility.enabled;
						}}
					>
						<Ico icon={accessibility.icon} size={3} class={accessibility.tone} />
					</button>
				{/each}
			{/snippet}
		</InputGroup>
	{/snippet}
	<AutoPlaceholder loading={!project}>
		{@html convertMarkdownToHtml(project.comment)}
	</AutoPlaceholder>

	<Accordion
		multiple
		classes="space-y-4"
		width=""
		value={searchQuery.length ? parts.map(({ name }) => name) : partsOpened}
		onValueChange={(e) => {
			partsOpened = e.value;
		}}
	>
		{#each parts as part, partIdx (part.name)}
			{@const { name, requestables, comment } = part}
			<div transition:fly={{ duration, y }}>
				<Accordion.Item
					value={part.name}
					classes="rounded-2xl border border-surface-200-800/40 bg-surface-50-950/70 shadow-sm shadow-surface-900/5"
					controlClasses="group flex w-full items-center justify-between gap-3 rounded-2xl px-3 py-3 text-left transition-colors duration-200 hover:bg-surface-100/60 dark:hover:bg-surface-800/40"
					controlPadding="p-0"
					panelPadding="px-2 pb-4"
					panelClasses="bg-transparent"
				>
					{#snippet control()}
						<div class="flex w-full flex-wrap items-center justify-between gap-3">
							<div class="flex min-w-0 flex-col">
								<span
									class="text-base leading-tight font-semibold text-surface-900 dark:text-surface-50"
									>{name}</span
								>
								{#if comment?.length}
									<span class="text-surface-500-300 text-xs">{comment}</span>
								{/if}
							</div>
							<span
								class="text-surface-500-300 rounded-full border border-dashed border-surface-400-600/60 px-2 py-1 text-[11px] font-semibold tracking-wide uppercase"
							>
								{requestables.length} item{requestables.length > 1 ? 's' : ''}
							</span>
						</div>
					{/snippet}
					{#snippet panel()}
						<div class="space-y-3">
							<Accordion multiple>
								{#each requestables as requestable, requestableIdx (requestable.name)}
									{@const { name, accessibility, comment } = requestable}
									<div
										animate:flip={{ duration }}
										transition:fly={{ duration, y }}
										class="group/requestable"
									>
										<Accordion.Item
											value={`${part.name}.${name}`}
											classes="group relative overflow-hidden rounded-xl border border-surface-200-800/40 bg-surface-50-950/60 shadow-sm shadow-surface-900/5 transition-colors duration-200 data-[state=open]:border-surface-300-700"
											controlClasses="group w-full rounded-xl text-left"
											controlPadding="p-0"
											panelPadding="p-0"
											panelClasses="bg-transparent"
										>
											{#snippet control()}
												<div
													class="flex w-full items-stretch gap-3 rounded-xl px-3 py-3 transition-colors duration-200 group-hover:bg-surface-100/70 group-data-[state=open]:bg-surface-100/60 dark:group-hover:bg-surface-800/40 dark:group-data-[state=open]:bg-surface-900/40"
												>
													<span
														aria-hidden
														class={`hidden w-1.5 shrink-0 rounded-full ${accessibilities[accessibility].accent} sm:block`}
													></span>
													<span
														class={`grid h-10 w-10 shrink-0 place-content-center rounded-full border border-surface-200-800/50 bg-surface-100-900 shadow-sm ${accessibilities[accessibility].chip}`}
													>
														<Ico
															icon={accessibilities[accessibility].icon}
															size={4}
															class={accessibilities[accessibility].tone}
														/>
													</span>
													<div class="flex min-w-0 flex-1 flex-col justify-center gap-1 text-left">
														<span
															class="text-sm leading-tight font-semibold text-surface-900 dark:text-surface-50"
															>{name}</span
														>
														{#if comment?.length}
															<span
																class="text-surface-500-300 truncate text-xs transition-opacity duration-150 group-data-[state=open]:opacity-70"
																>{comment}</span
															>
														{/if}
													</div>
													<span
														class={`hidden items-center gap-1 rounded-full px-2 py-1 text-[11px] font-semibold tracking-wide uppercase ${accessibilities[accessibility].soft} sm:flex`}
													>
														<Ico
															icon={accessibilities[accessibility].icon}
															size={3}
															class={accessibilities[accessibility].tone}
														/>
														<span>{accessibility}</span>
													</span>
												</div>
											{/snippet}
											{#snippet panel()}
												<form
													onsubmit={async (e) => {
														run(requestable, e);
													}}
													class="layout-y-stretch-low gap-4 px-3 pt-3 pb-4"
												>
													{#if part.name == 'Sequences'}
														<input type="hidden" name="__sequence" value={name} />
													{:else}
														<input type="hidden" name="__connector" value={part.name} />
														<input type="hidden" name="__transaction" value={name} />
													{/if}
													{#if comment.length}
														<p
															class="text-surface-500-300 rounded-md border border-dashed border-surface-200-800/60 bg-surface-50-950/70 px-3 py-2 text-sm"
														>
															{comment}
														</p>
													{/if}
													{#if requestable.variable?.length > 0}
														<RequestableVariables
															bind:requestable={parts[partIdx].requestables[requestableIdx]}
														/>
													{/if}
													{#if requestable.testcase.length > 0}
														<Accordion
															collapsible
															base="rounded-lg border border-surface-200-800/40 bg-surface-50-950/50"
														>
															<Accordion.Item
																value={`${requestableIdx}`}
																classes="rounded-lg"
																controlClasses="group flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-xs font-semibold uppercase tracking-wide text-surface-500-300"
																controlPadding="p-0"
																panelPadding="px-3 pb-3 pt-0"
																panelClasses="bg-transparent"
															>
																{#snippet control()}
																	<div
																		class="flex w-full flex-wrap items-center justify-between gap-2"
																	>
																		<div class="flex items-center gap-2">
																			<span
																				aria-hidden
																				class={`h-2 w-2 rounded-full ${accessibilities[accessibility].accent}`}
																			></span>
																			<span>
																				{requestable.testcase.length} Test Case{requestable.testcase
																					.length > 1
																					? 's'
																					: ''} available
																			</span>
																		</div>
																		<span
																			class={`rounded-full px-2 py-0.5 text-[10px] font-semibold tracking-wide ${accessibilities[accessibility].soft}`}
																		>
																			{accessibility}
																		</span>
																	</div>
																{/snippet}
																{#snippet panel()}
																	<div class="grid gap-3 sm:grid-cols-2">
																		{#each requestable.testcase as testcase}
																			<Card title={testcase.name} bg="bg-surface-50-950/70">
																				{#snippet cornerOption()}
																					<ResponsiveButtons
																						buttons={[
																							{
																								label: 'Execute',
																								type: 'submit',
																								value: testcase.name,
																								class: 'button-success',
																								icon: 'mdi:play-circle-outline'
																							},
																							{
																								label: 'Edit',
																								class: 'button-tertiary',
																								icon: 'mdi:edit-outline',
																								onclick: () => {
																									requestable.tc = { ...testcase };
																								}
																							}
																						]}
																					/>
																				{/snippet}
																				<TableAutoCard
																					showHeaders={false}
																					definition={[
																						{ key: 'name', class: 'font-medium' },
																						{ key: 'value' }
																					]}
																					data={testcase.variable}
																				/>
																			</Card>
																		{/each}
																	</div>
																{/snippet}
															</Accordion.Item>
														</Accordion>
													{/if}
													<div
														class="flex flex-col gap-3 rounded-lg border border-dashed border-surface-200-800/60 bg-surface-50-950/60 p-3 md:flex-row md:items-center md:justify-between"
													>
														<PropertyType
															type="segment"
															bind:value={mode}
															item={modes}
															fit={true}
														/>
														<div class="flex flex-wrap items-center justify-end gap-2">
															<Button
																label="Execute"
																type="submit"
																class="button-success"
																icon="mdi:play-circle-outline"
															/>
															{#if requestable.response?.length > 0}
																<Button
																	label="Clear"
																	type="submit"
																	class="button-error"
																	icon="mdi:broom"
																/>
															{/if}
														</div>
													</div>
													{#if requestable.response?.length > 0}
														<div
															class="h-[480px] overflow-hidden rounded-lg border border-surface-200-800/60 bg-surface-50-950 shadow-inner"
															class:animate-pulse={requestable.loading}
															transition:fly={{ duration, y }}
														>
															<Editor
																content={requestable.response}
																language={requestable.language}
																theme={LightSvelte.light ? '' : 'vs-dark'}
															/>
														</div>
													{/if}
												</form>
											{/snippet}
										</Accordion.Item>
									</div>
								{/each}
							</Accordion>
						</div>
					{/snippet}
				</Accordion.Item>
			</div>
		{/each}
	</Accordion>
</Card>

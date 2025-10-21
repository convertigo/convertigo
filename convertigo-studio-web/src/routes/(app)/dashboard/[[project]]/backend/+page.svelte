<script lang="ts">
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import RequestableVariables from '$lib/admin/components/RequestableVariables.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
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
	import type { PageProps } from './$types';

	let { params }: PageProps = $props();
	let project = $state(TestPlatform(params.project));

	$effect(() => {
		project = TestPlatform(params.project);
	});

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

	let parts = $derived.by(() => {
		if (!project) {
			return [];
		}
		const baseParts = [
			{
				name: 'Sequences',
				requestables: project.sequence ?? [],
				comment: 'high level requestables'
			},
			...(project.connector ?? []).map((connector) => ({
				name: connector.name,
				comment: connector.comment,
				requestables: connector.transaction ?? []
			}))
		];
		const query = searchQuery.toLowerCase();
		return baseParts
			.map((part) => ({
				...part,
				requestables: part.requestables.filter(({ accessibility, name }) => {
					const status = accessibilities[accessibility];
					return status?.enabled && name?.toLowerCase().includes(query);
				})
			}))
			.filter((part) => part.requestables.length > 0);
	});
	let partsOpened = $state(['Sequences']);
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
						class="layout-x h-full gap-none rounded-none p-2 text-[11px] font-semibold tracking-wide uppercase transition-all duration-200 {accessibility.chip}"
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

	<AccordionGroup
		class="-mx-low"
		multiple
		value={searchQuery.length ? parts.map(({ name }) => name) : partsOpened}
		onValueChange={({ value }) => {
			if (!searchQuery.length) {
				partsOpened = value;
			}
		}}
	>
		{#each parts as part, partIdx (part.name)}
			{@const { name, requestables, comment } = part}
			<div transition:fly={{ duration, y }}>
				<AccordionSection
					value={part.name}
					class="rounded-container bg-surface-100-900 shadow-follow"
					triggerClass="layout-x-between w-full rounded-2xl px-3 py-3 text-left transition-colors duration-200 hover:bg-surface-100/60 dark:hover:bg-surface-800/40"
					panelClass="px-low pb-low"
				>
					{#snippet control()}
						<div class="layout-x-wrap w-full justify-between">
							<div class="layout-y-start-low min-w-0">
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
							<AccordionGroup multiple>
								{#each requestables as requestable, requestableIdx (requestable.name)}
									{@const { name, accessibility, comment } = requestable}
									<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
										<AccordionSection
											value={`${part.name}.${name}`}
											class="relative overflow-hidden rounded-xl border border-surface-200-800/40 bg-surface-50-950/60 shadow-sm shadow-surface-900/5 transition-colors duration-200 data-[state=open]:border-surface-300-700"
											triggerClass="w-full rounded-xl text-left px-0 py-0"
											panelClass="px-0 py-0 bg-transparent"
										>
											{#snippet control()}
												<div
													class="layout-x-stretch w-full rounded-xl px-3 py-3 transition-colors duration-200 group-hover:bg-surface-100/70 group-data-[state=open]:bg-surface-100/60 dark:group-hover:bg-surface-800/40"
												>
													<span
														aria-hidden="true"
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
													<div class="layout-y-start-low min-w-0 flex-1 justify-center text-left">
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
														class={`hidden rounded-full px-2 py-1 text-[11px] font-semibold tracking-wide uppercase ${accessibilities[accessibility].soft} sm:layout-x-low sm:items-center`}
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
													onsubmit={(e) => {
														run(requestable, e);
													}}
													class="layout-y-stretch px-3 pt-3 pb-4"
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
														<AccordionGroup
															collapsible
															base="rounded-lg border border-surface-200-800/40 bg-surface-50-950/50"
														>
															<AccordionSection
																value={`${requestableIdx}`}
																class="rounded-lg"
																triggerClass="group layout-x-low w-full justify-between rounded-lg px-3 py-2 text-xs font-semibold uppercase tracking-wide text-surface-500-300"
																panelClass="px-3 pb-3 pt-0 bg-transparent"
															>
																{#snippet control()}
																	<div class="layout-x-wrap-low w-full justify-between">
																		<div class="layout-x-low">
																			<span
																				aria-hidden="true"
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
																	<div class="layout-grid-[240px] sm:layout-grid-[280px]">
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
															</AccordionSection>
														</AccordionGroup>
													{/if}
													<div
														class="layout-y-low rounded-lg border border-dashed border-surface-200-800/60 bg-surface-50-950/60 p-3 md:layout-x-low md:items-center md:justify-between"
													>
														<PropertyType
															type="segment"
															bind:value={mode}
															item={modes}
															fit={true}
														/>
														<div class="layout-x-wrap-low justify-end">
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
										</AccordionSection>
									</div>
								{/each}
							</AccordionGroup>
						</div>
					{/snippet}
				</AccordionSection>
			</div>
		{/each}
	</AccordionGroup>
</Card>

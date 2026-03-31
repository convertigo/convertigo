<script lang="ts">
	import ActionBar from '$lib/admin/components/ActionBar.svelte';
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
	import { flip } from 'svelte/animate';
	import { fly } from 'svelte/transition';
	import type { PageProps } from './$types';

	let { params }: PageProps = $props();
	let project = $derived.by(() => TestPlatform(params.project));

	let searchQuery = $state('');

	const modes = ['JSON', 'XML', 'BIN', 'CXML'];
	let mode = $state(modes[0]);

	const accessibilities = $state({
		Private: {
			accent: 'preset-filled-success-200-800',
			chip: 'preset-filled-success-50-950 odd:preset-filled-success-100-900',
			filterChip: 'preset-filled-success-100-900',
			soft: 'preset-outlined-success-50-950',
			tone: 'text-success-600-400',
			icon: 'mdi:lock',
			enabled: true
		},
		Hidden: {
			accent: 'preset-filled-warning-200-800',
			chip: 'preset-filled-warning-50-950 odd:preset-filled-warning-100-900',
			filterChip: 'preset-filled-warning-100-900',
			soft: 'preset-outlined-warning-50-950',
			tone: 'text-warning-600-400',
			icon: 'mdi:eye-off',
			enabled: true
		},
		Public: {
			accent: 'preset-filled-error-200-800',
			chip: 'preset-filled-error-50-950 odd:preset-filled-error-100-900',
			filterChip: 'preset-filled-error-100-900',
			soft: 'preset-outlined-error-50-950',
			tone: 'text-error-600-400',
			icon: 'mdi:lock-open-variant',
			enabled: true
		}
	});

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

<Card title={project?.name ?? null} cornerOptionClass="flex-1 min-w-[18rem] max-w-[72rem]">
	{#snippet cornerOption()}
		<InputGroup
			id="search"
			type="search"
			placeholder="Search requestable..."
			autofocus
			class="w-full"
			actionsClass="pr-1"
			icon="mdi:magnify"
			bind:value={searchQuery}
		>
			{#snippet actions()}
				{#each Object.entries(accessibilities) as [name, accessibility], idx (name)}
					<Button
						full={false}
						type="button"
						size={3}
						icon={accessibility.icon}
						class={`layout-x h-full gap-none rounded-none p-2 text-[11px] font-semibold tracking-wide uppercase transition-all duration-200 ${accessibility.filterChip} ${accessibility.tone} ${
							idx === 0 ? 'rounded-l' : ''
						} ${idx === 2 ? 'rounded-r' : ''} ${!accessibility.enabled ? 'opacity-50' : ''}`}
						title={`${accessibility.enabled ? 'Hide' : 'Show'} ${name} requestables`}
						ariaLabel={`${accessibility.enabled ? 'Hide' : 'Show'} ${name} requestables`}
						onclick={() => {
							accessibility.enabled = !accessibility.enabled;
						}}
					/>
				{/each}
			{/snippet}
		</InputGroup>
	{/snippet}
	<AutoPlaceholder loading={!project}>
		<div class="whitespace-pre-wrap">{project?.comment}</div>
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
					class="accordion-item"
					triggerClass="accordion-trigger"
					panelClass="accordion-panel"
					title={name}
					subtitle={comment}
					count={requestables.length}
				>
					{#snippet panel()}
						<div class="space-y-3">
							<AccordionGroup multiple>
								{#each requestables as requestable, requestableIdx (requestable.name)}
									{@const { name, accessibility, comment } = requestable}
									<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
										<AccordionSection
											value={`${part.name}.${name}`}
											class="relative overflow-hidden rounded-xl border border-surface-200-800/40 bg-surface-50-950/60 shadow-sm shadow-surface-900/5 transition-surface data-[state=open]:border-surface-300-700"
											triggerClass="w-full rounded-xl text-left px-0 py-0"
											panelClass="px-0 py-0 bg-transparent"
										>
											{#snippet control()}
												<div
													class="layout-x-stretch w-full rounded-xl px-3 py-3 transition-soft group-hover:bg-surface-100-900/70 group-data-[state=open]:bg-surface-100-900/60"
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
														<span class="text-sm leading-tight font-semibold text-strong"
															>{name}</span
														>
														{#if comment?.length}
															<span
																class="truncate text-xs text-surface-600-400 transition-opacity duration-150 group-data-[state=open]:opacity-70"
																>{comment}</span
															>
														{/if}
													</div>
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
															class="rounded-md border border-dashed border-surface-200-800/60 bg-surface-50-950/70 px-3 py-2 text-sm text-surface-600-400"
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
															value={requestable.testCasesOpened ? [`${requestableIdx}`] : []}
															onValueChange={({ value }) => {
																requestable.testCasesOpened = (value ?? []).includes(
																	`${requestableIdx}`
																);
															}}
															class="rounded-md border border-surface-200-800/40 bg-surface-50-950/45"
														>
															<AccordionSection
																value={`${requestableIdx}`}
																class="rounded-md"
																title={`Test cases (${requestable.testcase.length})`}
																triggerClass="group w-full rounded-md px-2.5 py-2"
																titleClass="text-sm font-semibold"
																panelClass="bg-transparent px-2.5 pb-2 pt-0"
															>
																{#snippet panel()}
																	<div class="layout-grid-[220px] gap-2 sm:layout-grid-[260px]">
																		{#each requestable.testcase as testcase (testcase.name)}
																			{@const testcaseValuesOpen = testcase.valuesOpened ?? false}
																			<Card
																				title={testcase.name}
																				bg="bg-surface-50-950/70"
																				class="gap-2 p-low"
																			>
																				{#snippet cornerOption()}
																					<ResponsiveButtons
																						class="max-w-none"
																						buttons={[
																							{
																								label: 'Execute',
																								type: 'submit',
																								value: testcase.name,
																								class: 'button-primary',
																								icon: 'mdi:play-circle-outline'
																							},
																							{
																								label: 'Edit',
																								class: 'button-secondary',
																								icon: 'mdi:edit-outline',
																								onclick: () => {
																									requestable.tc = { ...testcase };
																								}
																							}
																						]}
																					/>
																				{/snippet}
																				<AccordionGroup
																					collapsible
																					value={testcaseValuesOpen ? ['values'] : []}
																					onValueChange={({ value }) => {
																						testcase.valuesOpened = (value ?? []).includes(
																							'values'
																						);
																					}}
																					class="rounded-md border border-surface-200-800/40 bg-surface-100-900/25"
																				>
																					<AccordionSection
																						value="values"
																						class="rounded-md"
																						title={`Preset values (${testcase.variable.length})`}
																						triggerClass="w-full rounded-md px-2.5 py-2"
																						titleClass="text-sm font-semibold text-strong"
																						panelClass="bg-transparent px-2 pb-2 pt-0"
																					>
																						{#snippet panel()}
																							{#each testcase.variable as variable (variable.name)}
																								<div
																									class="border-b border-surface-200-800/30 pb-2 last:border-b-0 last:pb-0"
																								>
																									<div class="text-sm font-medium text-strong">
																										{variable.name}
																									</div>
																									<div
																										class="text-xs break-words whitespace-pre-wrap text-surface-600-400"
																									>
																										{variable.value}
																									</div>
																								</div>
																							{/each}
																						{/snippet}
																					</AccordionSection>
																				</AccordionGroup>
																			</Card>
																		{/each}
																	</div>
																{/snippet}
															</AccordionSection>
														</AccordionGroup>
													{/if}
													<div
														class="sticky bottom-3 z-10 layout-y-low rounded-lg border border-dashed border-surface-200-800/60 bg-surface-50-950/88 p-3 shadow-lg shadow-surface-900/10 backdrop-blur-sm md:layout-x-low md:items-center md:justify-between"
													>
														<PropertyType
															type="segment"
															bind:value={mode}
															item={modes}
															fit={true}
														/>
														<ActionBar wrap full={false}>
															<Button
																label="Execute"
																type="submit"
																class="button-primary"
																icon="mdi:play-circle-outline"
															/>
															{#if requestable.response?.length > 0}
																<Button
																	label="Clear"
																	type="submit"
																	class="button-secondary"
																	icon="mdi:broom"
																/>
															{/if}
														</ActionBar>
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

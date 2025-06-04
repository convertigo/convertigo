<script>
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import RequestableVariables from '$lib/admin/components/RequestableVariables.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
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
			bg: 'bg-success-200! dark:bg-success-600!',
			icon: 'mdi:lock',
			enabled: true
		},
		Hidden: {
			bg: 'bg-warning-200! dark:bg-warning-600!',
			icon: 'mdi:eye-off',
			enabled: true
		},
		Public: {
			bg: 'bg-error-200! dark:bg-error-600!',
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
	const [duration, y, opacity] = [200, -50, 1];
</script>

<Card title={project?.name ?? null}>
	{#snippet cornerOption()}
		<div
			class="input-group w-full grid-cols-[auto_1fr_auto] divide-x divide-surface-700-300 preset-outlined-surface-700-300 bg-surface-200-800"
		>
			<label for="search" class="ig-cell"><Ico icon="mdi:magnify" /></label>
			<input
				id="search"
				class="ig-input placeholder:text-surface-500"
				type="search"
				placeholder="Search requestable..."
				bind:value={searchQuery}
			/>
			<span class="layout-x-none gap-[1px]! pr-[1px]">
				{#each Object.values(accessibilities) as accessibility}
					<button
						class="btn h-full rounded-none p-1 {accessibility.bg}"
						class:opacity-50={!accessibility.enabled}
						onclick={() => {
							accessibility.enabled = !accessibility.enabled;
						}}
					>
						<Ico icon={accessibility.icon} size="nav" />
					</button>
				{/each}
			</span>
		</div>
	{/snippet}
	<AutoPlaceholder loading={!project}>
		{@html convertMarkdownToHtml(project.comment)}
	</AutoPlaceholder>

	<Accordion
		multiple
		classes="-mx"
		width=""
		value={searchQuery.length ? parts.map(({ name }) => name) : ['Sequences']}
	>
		{#each parts as part, partIdx (part.name)}
			{@const { name, requestables, comment } = part}
			<div transition:fly={{ duration, y }}>
				<Accordion.Item value={part.name} controlPadding="py-1 px-2" panelPadding="p-1">
					{#snippet control()}
						<div class="layout-x justify-between border-b-[0.5px]">
							<span class="text-lg font-semibold">{name}</span><span class="truncate text-xs"
								>{comment}</span
							>
						</div>
					{/snippet}
					{#snippet panel()}
						<Accordion multiple>
							{#each requestables as requestable, requestableIdx (requestable.name)}
								{@const { name, accessibility, comment } = requestable}
								<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
									<Accordion.Item
										value="{parts[partIdx]}.{name}"
										classes="rounded-sm {accessibilities[accessibility].bg}"
										controlPadding="py-1 px-2"
										panelPadding="p-1"
									>
										{#snippet control()}
											<div class="layout-x justify-between">
												<div class="layout-x">
													<Ico icon={accessibilities[accessibility].icon} /><span
														class="text text-[14px] font-bold">{name}</span
													>
												</div>
												{#if !requestable.open}
													<span
														transition:fly={{ duration, y: 20 }}
														class="color-grey absolute left-[50%] w-[50%] truncate text-xs"
														>{comment}</span
													>
												{/if}
											</div>
										{/snippet}
										{#snippet panel()}
											<form
												onsubmit={async (e) => {
													run(requestable, e);
												}}
												class="layout-y-stretch-low"
											>
												{#if part.name == 'Sequences'}
													<input type="hidden" name="__sequence" value={name} />
												{:else}
													<input type="hidden" name="__connector" value={part.name} />
													<input type="hidden" name="__transaction" value={name} />
												{/if}
												{#if comment.length}
													<p class="p">{comment}</p>
												{/if}
												{#if requestable.variable?.length > 0}
													<RequestableVariables
														bind:requestable={parts[partIdx].requestables[requestableIdx]}
													/>
												{/if}
												{#if requestable.testcase.length > 0}
													<Accordion collapsible>
														<Accordion.Item
															value={`${requestableIdx}`}
															classes={accessibilities[accessibility].bg}
															controlPadding="py-1 px-2"
															panelPadding="px-0"
														>
															{#snippet control()}
																<div>
																	{requestable.testcase.length} Test Case{requestable.testcase
																		.length > 1
																		? 's'
																		: ''} available
																</div>
															{/snippet}
															{#snippet panel()}
																<div class="layout-grid-low-60">
																	{#each requestable.testcase as testcase}
																		<Card title={testcase.name}>
																			{#snippet cornerOption()}
																				<ResponsiveButtons
																					buttons={[
																						{
																							label: 'Execute',
																							type: 'submit',
																							value: testcase.name,
																							class: 'button-primary'
																						},
																						{
																							label: 'Edit',
																							class: 'button-tertiary',
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
																					{ key: 'name', class: 'font-bold' },
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
												<Card class="layout-y p-low! md:layout-x">
													<PropertyType type="segment" bind:value={mode} item={modes} fit={true} />
													<Button label="Execute" type="submit" class="button-primary" />
													{#if part.name == 'Sequences'}
														<Button
															label="View flow"
															class="button-tertiary max-w-24"
															href={name}
														/>
													{/if}
													{#if requestable.response?.length > 0}
														<Button label="Clear" type="submit" class="button-error" />
													{/if}
												</Card>
												{#if requestable.response?.length > 0}
													<div
														class="h-[480px]"
														class:animate-pulse={requestable.loading}
														transition:fly={{ duration, y }}
													>
														<Editor
															content={requestable.response}
															language={requestable.language}
															theme={false ? '' : 'vs-dark'}
														/>
													</div>
												{/if}
											</form>
										{/snippet}
									</Accordion.Item>
								</div>
							{/each}
						</Accordion>
					{/snippet}
				</Accordion.Item>
			</div>
		{/each}
	</Accordion>
</Card>

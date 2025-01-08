<script>
	import { page } from '$app/state';
	import { decode } from 'html-entities';
	import { marked } from 'marked';
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { fly } from 'svelte/transition';
	import { flip } from 'svelte/animate';
	import { callRequestable } from '$lib/utils/service';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import PropertyType from '$lib/admin/components/PropertyType.svelte';
	import Button from '$lib/admin/components/Button.svelte';
	import RequestableVariables from '$lib/admin/components/RequestableVariables.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import ResponsiveButtons from '$lib/admin/components/ResponsiveButtons.svelte';

	let project = $state(TestPlatform(page.params.project));
	let searchQuery = $state('');

	const modes = ['JSON', 'XML', 'BIN', 'CXML'];
	let mode = $state(modes[0]);

	const accessibilities = $state({
		Private: {
			bg: '!bg-success-200 dark:!bg-success-600',
			icon: 'mdi:lock',
			enabled: true
		},
		Hidden: {
			bg: '!bg-warning-200 dark:!bg-warning-600',
			icon: 'mdi:eye-off',
			enabled: true
		},
		Public: {
			bg: '!bg-error-200 dark:!bg-error-600',
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
		const parts = [
			{ name: 'Sequences', requestables: project.sequence, comment: 'high level requestables' }
		];
		for (let connector of project.connector) {
			parts.push({
				name: connector.name,
				comment: connector.comment,
				requestables: connector.transaction
			});
		}
		return parts
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
			class="w-full input-group bg-surface-200-800 divide-surface-700-300 preset-outlined-surface-700-300 divide-x grid-cols-[auto_1fr_auto]"
		>
			<div class="input-group-cell"><Ico icon="mdi:magnify" /></div>
			<input type="search" placeholder="Search requestable..." bind:value={searchQuery} />
			<span class="layout-x-none !gap-[1px] pr-[1px]">
				{#each Object.values(accessibilities) as accessibility}
					<button
						class="btn rounded-none p-1 {accessibility.bg}"
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

	<Accordion multiple classes="-mx" width="" value={['n0']}>
		{#each parts as part, index (part.name)}
			{@const { name, requestables, comment } = part}
			<div>
				<!-- <div animate:flip={{ duration }} transition:fly={{ duration, y }}> -->
				<Accordion.Item value="n{index}" controlPadding="py-1 px-2" panelPadding="p-1">
					<!-- <Accordion.Item open={index == 0 || searchQuery.length > 0}> -->
					{#snippet control()}
						<div class="border-b-[0.5px] layout-x justify-between">
							<span class="text-lg font-semibold">{name}</span><span class="text-xs truncate"
								>{comment}</span
							>
						</div>
					{/snippet}
					{#snippet panel()}
						<Accordion multiple>
							{#each requestables as requestable, index (requestable.name)}
								{@const { name, accessibility, comment } = requestable}
								<!-- <div animate:flip={{ duration }} transition:fly={{ duration, y }}> -->
								<div animate:flip={{ duration }}>
									<Accordion.Item
										value={`${index}`}
										classes="rounded {accessibilities[accessibility].bg}"
										controlPadding="py-1 px-2"
										panelPadding="p-1"
									>
										<!-- <Accordion.Item
											ontoggle={(e) => (requestable.open = e.detail?.open)}
											open={requestable.open}
										> -->
										{#snippet control()}
											<div class="layout-x justify-between">
												<div class="layout-x">
													<Ico icon={accessibilities[accessibility].icon} /><span
														class="text-[14px] text font-bold">{name}</span
													>
												</div>
												{#if !requestable.open}
													<span
														transition:fly={{ duration, y: 20 }}
														class="absolute left-[50%] w-[50%] text-xs color-grey truncate"
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
													<RequestableVariables {requestable} />
												{/if}
												{#if requestable.testcase.length > 0}
													<Accordion collapsible>
														<Accordion.Item
															value={`${index}`}
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
																<div class="layout-y-stretch-low">
																	{#each requestable.testcase as testcase}
																		<Card title={testcase.name}>
																			{#snippet cornerOption()}
																				<ResponsiveButtons
																					buttons={[
																						{
																							label: 'Execute',
																							type: 'submit',
																							value: testcase.name,
																							class: 'basic-button'
																						},
																						{
																							label: 'Edit',
																							class: 'yellow-button',
																							onclick: () => {}
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
												<Card class="layout-y md:layout-x !p-low">
													<PropertyType type="segment" bind:value={mode} item={modes} fit={true} />
													<Button label="Execute" type="submit" class="basic-button" />
													{#if part.name == 'Sequences'}
														<Button label="View flow" class="yellow-button max-w-24" href={name} />
													{/if}
													{#if requestable.response?.length > 0}
														<Button label="Clear" type="submit" class="cancel-button" />
													{/if}
												</Card>
												{#if requestable.response?.length > 0}
													<div class="h-[480px]" class:animate-pulse={requestable.loading}>
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

<style lang="postcss">
	:global(.accordion-summary) {
		@apply overflow-hidden;
	}
	.sticky {
		position: -webkit-sticky; /* For Safari */
		position: sticky;
		top: 40px;
		height: calc(100vh - 40px); /* Adjust height to account for the top offset */
		overflow-y: auto; /* Allow scrolling if content overflows */
	}
	.qr-code {
		width: 200px; /* Adjust the size as needed */
		height: 200px; /* Adjust the size as needed */
	}

	.marked-p-class {
		color: aqua;
	}
</style>

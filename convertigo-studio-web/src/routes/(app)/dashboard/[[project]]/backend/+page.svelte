<script>
	import { page } from '$app/stores';
	import Table from '$lib/dashboard/components/Table.svelte';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import { decode } from 'html-entities';
	import { onMount } from 'svelte';
	import { marked } from 'marked';
	import { Switch, Accordion } from '@skeletonlabs/skeleton-svelte';
	import Ico from '$lib/utils/Ico.svelte';
	import { blur, fly } from 'svelte/transition';
	import { flip } from 'svelte/animate';
	import { callRequestable } from '$lib/utils/service';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';

	let project = $state();
	let _parts = $state([]);
	let searchQuery = $state('');

	const modes = ['JSON', 'XML', 'BIN', 'CXML'];
	let mode = $state(modes[0]);

	const bgColors = [
		'bg-pale-violet border-[1px] border-pale-violet',
		'bg-pale-blue border-[1px] border-pale-blue',
		'bg-pale-green border-[1px] border-pale-green',
		'bg-pale-pink border-[1px] border-pale-pink'
	];

	/**
	 * @param {string} markdown
	 */
	function convertMarkdownToHtml(markdown) {
		const cleanedMarkdown = decode(markdown);
		return marked(cleanedMarkdown);
	}

	onMount(() => {
		const unsubscribe = page.subscribe(($page) => {
			const projectName = $page.params.project;
			checkTestPlatform(projectName).then(() => {
				project = $testPlatformStore[projectName];
				_parts = [{ name: 'Sequences', requestables: Object.values(project.sequence || {}) }];
				for (let connector of Object.values(project.connector || {})) {
					_parts.push({
						name: connector.name,
						requestables: Object.values(connector.transaction || {})
					});
				}
				_parts = _parts.filter((part) => part.requestables.length > 0);
			});
		});
		return () => unsubscribe();
	});

	async function run(requestable, event) {
		event.preventDefault();
		if (event.submitter.textContent == 'Clear') {
			requestable.response = '';
			_parts = _parts;
			return;
		}
		requestable.loading = true;
		requestable.response = 'Loading …';
		_parts = _parts;
		const data = await callRequestable(mode, project.name, new FormData(event.target));
		requestable.response = await data.text();
		requestable.language = data.headers.get('Content-Type')?.includes('json') ? 'json' : 'xml';
		requestable.loading = false;
		_parts = _parts;
	}

	/**
	 * @param {{ variables: { [s: string]: any; } | ArrayLike<any>; }} testcase
	 */
	function copyToInputs(testcase) {
		Object.values(testcase.variables).forEach(({ name, value }) => {
			const inputElement = document.querySelector(`input[name="${name}"]`);
			if (inputElement) {
				//@ts-ignore
				inputElement.value = value;
			}
		});
	}
	let columns = ['Name', 'Value'];

	let parts = $derived(
		_parts
			.map((part) => ({
				...part,
				requestables: part.requestables.filter(({ name }) =>
					name?.toLowerCase().includes(searchQuery.toLowerCase())
				)
			}))
			.filter((part) => part.requestables.length > 0)
	);

	const [duration, y, opacity] = [200, -50, 1];
</script>

<Card title={project?.name ?? null} class="!items-stretch">
	{#snippet cornerOption()}
		<div
			class="w-full input-group bg-surface-200-800 divide-surface-700-300 preset-outlined-surface-700-300 divide-x grid-cols-[auto_1fr_auto]"
		>
			<div class="input-group-cell"><Ico icon="mdi:magnify" /></div>
			<input type="search" placeholder="Search requestable..." bind:value={searchQuery} />
		</div>
	{/snippet}
	<AutoPlaceholder loading={!project}>
		{@html convertMarkdownToHtml(project.comment)}
	</AutoPlaceholder>
	{#each parts as part, index (part.name)}
		{@const { name, requestables } = part}
		<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
			<Accordion collapsible value={['n0']}>
				<Accordion.Item value="n{index}">
					<!-- <Accordion.Item open={index == 0 || searchQuery.length > 0}> -->
					{#snippet control()}
						<p class="text-lg font-semibold pb-2 border-b-[0.5px]">{name}</p>
					{/snippet}
					{#snippet panel()}
						{#each requestables as requestable, index (requestable.name)}
							{@const { name, comment } = requestable}
							<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
								<Accordion
									collapsible
									padding="p-low"
									classes="rounded bg-opacity-20 {bgColors[index % bgColors.length]}"
								>
									<Accordion.Item value="ok">
										<!-- <Accordion.Item
											ontoggle={(e) => (requestable.open = e.detail?.open)}
											open={requestable.open}
										> -->
										{#snippet control()}
											<div class="flex items-center justify-between relative">
												<span class="text-[14px] text font-bold">{name}</span>
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
												class="flex flex-col gap-3"
											>
												{#if part.name == 'Sequences'}
													<input type="hidden" name="__sequence" value={name} />
													<a href={name} class="yellow-button">View flow</a>
												{:else}
													<input type="hidden" name="__connector" value={name} />
													<input type="hidden" name="__transaction" value={name} />
												{/if}
												<span>{comment}</span>
												<div class="p-3 font-semiBold bg-surface-100 dark:bg-surface-800">
													<p>Parameters</p>
												</div>
												<div class="grid grid-cols-2 p-5 gap-10">
													<div class="col-span-1">
														{#each Object.values(requestable.variable ?? {}) as variable}
															{@const { checked, name, required, value } = variable}
															<label class="label-common">
																<p class="font-semibold mb-2">{name}</p>
																<div class="flex items-center gap-3">
																	{#if checked}
																		<input
																			class="input-common"
																			{required}
																			{name}
																			{value}
																			in:blur={{ duration, opacity }}
																		/>
																	{:else}
																		<input
																			class="input-common"
																			style="color: grey;"
																			{value}
																			readonly={true}
																			in:blur={{ duration, opacity }}
																			onclick={() => {
																				variable.checked = true;
																			}}
																		/>
																	{/if}
																	<Switch
																		controlActive="activeSlideToggle"
																		controlInactive="unActiveSlideToggle"
																		name=""
																		{checked}
																	/>
																	<!-- <Switch
																			controlActive="activeSlideToggle"
																			controlInactive="unActiveSlideToggle"
																			size="sm"
																			name=""
																			{checked}
																			onchange={() => {
																				variable.checked = !checked;
																			}}
																		/> -->
																</div>
															</label>
														{/each}
													</div>
													<div class="col-span-1">
														{#if requestable.testcases && Object.keys(requestable.testcases).length > 0}
															{#each Object.values(requestable.testcases) as testcase}
																<p class="font-semibold mb-4">{testcase.name}</p>

																{#if testcase.variables && Object.keys(testcase.variables).length > 0}
																	{@const data = Object.values(testcase.variables).map(
																		({ name, value }) => [name, convertMarkdownToHtml(value)]
																	)}
																	<div class="table-container flex flex-col mb-5">
																		<Table {columns} {data} />
																		<button
																			class="basic-button mt-5"
																			onclick={() => copyToInputs(testcase)}>Copy</button
																		>
																	</div>
																{:else}
																	<p>No variables available in this testcase</p>
																{/if}
															{/each}
														{:else}
															<p>No test cases available in this sequence</p>
														{/if}
													</div>
												</div>
												<div class="flex flex-row gap-5">
													<button class="basic-button flex-1">Execute</button>
													{#if requestable.response?.length > 0}
														<button class="cancel-button flex-1" in:fly={{ duration, x: -50 }}
															>Clear</button
														>
													{/if}
												</div>
												<div
													class="p-3 font-semiBold bg-surface-100 dark:bg-surface-800 flex items-center justify-between"
												>
													<strong>Response</strong>
													<span
														>Response type&nbsp;
														<select class="select w-fit" bind:value={mode}>
															{#each modes as mode}
																<option>{mode}</option>
															{/each}
														</select></span
													>
												</div>
												{#if 'loading' in requestable}
													<div
														class="h-[480px]"
														class:animate-pulse={requestable.loading}
														transition:fly={{ duration, y: -100 }}
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
								</Accordion>
							</div>
						{/each}
					{/snippet}
				</Accordion.Item>
			</Accordion>
		</div>
	{/each}
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

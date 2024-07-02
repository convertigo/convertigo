<script>
	import { page } from '$app/stores';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import Table from '$lib/dashboard/components/Table.svelte';
	import {
		checkTestPlatform,
		connectorsStore,
		sequencesStore,
		testPlatformStore
	} from '$lib/dashboard/stores/testPlatform';
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { marked } from 'marked';
	import { SlideToggle } from '@skeletonlabs/skeleton';
	// import ButtonsContainer from '$lib/admin/components/ButtonsContainer.svelte';

	let project;
	let enableInputVar = {};

	const colors = ['bg-pale-violet', 'bg-pale-blue', 'bg-pale-green', 'bg-pale-pink'];
	onMount(async () => {
		checkTestPlatform(project);
	});

	$: {
		page.subscribe(($page) => {
			project = $page.params.project;
		});
	}

	function convertMarkdownToHtml(markdown) {
		const entityMap = {
			'&quot;': '"',
			'&#10;': '\n',
			'&#9;': '\t',
			'&amp;': '&',
			'&lt;': '<',
			'&gt;': '>',
			'&nbsp;': ' ',
			'&apos;': "'"
		};

		const cleanedMarkdown = markdown.replace(
			/&quot;|&#10;|&#9;|&amp;|&lt;|&gt;|&nbsp;|&apos;/g,
			function (match) {
				return entityMap[match];
			}
		);

		return marked(cleanedMarkdown);
	}

	function copyToInputs(testcase) {
		Object.values(testcase.variables).forEach((variable) => {
			const inputElement = document.querySelector(`input[name="${variable['@_name']}"]`);
			if (inputElement) {
				//@ts-ignore
				inputElement.value = variable['@_value'];
			}
		});
	}
<<<<<<< HEAD
	let columns = ['Name', 'Value'];
=======
>>>>>>> branch 'develop' of git@github.com:convertigo/convertigo.git

	// Subscribe to sequencesStore to log its value
	sequencesStore.subscribe((value) => {
		console.log('Sequences Store Data:', value);
	});

	connectorsStore.subscribe((value) => {
		console.log('Connectors Store Data:', value);
	});

	testPlatformStore.subscribe((value) => {
		console.log('TestPlaroform proejct:', value);
	});
</script>

<main class="gap-5 flex flex-col">
	<CardD>
		<div class="grid grid-cols-2">
			<div class="col-span-1">
				<!-- <h1>Project: {project}</h1> -->
				{#each $testPlatformStore as project}
					{project['@_name']}
					<!-- {project['@_comment']} -->
					<div class="mb-5" />
					{@html convertMarkdownToHtml(project['@_comment'])}
				{/each}
			</div>
			<div class="col-span-1"></div>
		</div>
	</CardD>

	<div class="col-span-1">
		<CardD class="gap-2">
			<Accordion padding="p-4" class="">
				<AccordionItem>
					<svelte:fragment slot="lead"></svelte:fragment>
					<svelte:fragment slot="summary">
						<p class="text-[14px] text-token">Sequences</p>
					</svelte:fragment>
					<svelte:fragment slot="content">
						{#if $sequencesStore && $sequencesStore.length > 0}
							{#each $sequencesStore as sequence, index}
								<Accordion padding="p-4" class="dark:bg-surface-600 bg-surface-50 text-token">
									<AccordionItem close>
										<svelte:fragment slot="lead"></svelte:fragment>
										<svelte:fragment slot="summary">
											<p class="text-[14px] text-token">{sequence['@_name']}</p>
										</svelte:fragment>
										<svelte:fragment slot="content">
											<div
												class="p-3 font-semiBold bg-surface-100 flex items-center justify-between"
											>
												<p>Parameters</p>
												<button class="basic-button">Try sequence</button>
											</div>
											<div class="grid grid-cols-2 gap-10">
												<div class="col-span-1">
													{#if sequence.variables && Object.keys(sequence.variables).length > 0}
														<form>
															{#each Object.values(sequence.variables) as variable}
																<div class=" py-2 flex-col items-center">
																	<p class="font-semibold mb-2">{variable['@_name']}</p>
																	<div class="flex items-center gap-3">
																		<label class="label-common w-full">
																			<input
																				class="input-common"
																				disabled={!enableInputVar[variable['@_name']]}
																				required={variable['@_required']}
																				name={variable['@_name']}
																				value={variable['@_value']}
																			/>
																			<!-- <label>
																				<input type="checkbox" bind:checked={enableInputVar} />
																				Enable Input
																			</label> -->
																		</label>
																		<SlideToggle
																			active="activeSlideToggle"
																			background="unActiveSlideToggle"
																			size="sm"
																			name="slide"
																			bind:checked={enableInputVar[variable['@_name']]}
																		/>
																	</div>
																</div>
															{/each}
														</form>
													{:else}
														<p>No variables available in this sequence</p>
													{/if}
												</div>
												<div class="col-span-1">
													{#if sequence.testcases && Object.keys(sequence.testcases).length > 0}
														{#each Object.values(sequence.testcases) as testcase}
<<<<<<< HEAD
															<p class="font-semibold mb-4">{testcase['@_name']}</p>

															{#if testcase.variables && Object.keys(testcase.variables).length > 0}
																{@const data = Object.values(testcase.variables).map((variable) => [
																	variable['@_name'],
																	convertMarkdownToHtml(variable['@_value'])
																])}
																<div class="table-container flex flex-col mb-5">
																	<Table {columns} {data} />
																	<button
																		class="basic-button mt-5"
																		on:click={() => copyToInputs(testcase)}>Copy</button
																	>
																</div>
															{:else}
																<p>No variables available in this testcase</p>
															{/if}
=======
															<AccordionItem close>
																<svelte:fragment slot="lead"></svelte:fragment>
																<svelte:fragment slot="summary">
																	<p class="font-semibold">{testcase['@_name']}</p>
																</svelte:fragment>
																<svelte:fragment slot="content">
																	{#if testcase.variables && Object.keys(testcase.variables).length > 0}
																		<Accordion
																			class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
																		>
																			<div class="table-container p-5 flex flex-col gap-2">
																				{#each Object.values(testcase.variables) as variable}
																					<table class="table-auto w-full">
																						<tbody>
																							<tr>
																								<td class="p-2">{variable['@_name']}</td>
																								<td class="p-2">
																									<div>
																										{@html convertMarkdownToHtml(
																											variable['@_value']
																										)}
																									</div>
																								</td>
																							</tr>
																						</tbody>
																					</table>
																				{/each}
																				<button
																					class="basic-button"
																					on:click={() => copyToInputs(testcase)}>Copy</button
																				>
																			</div>
																		</Accordion>
																	{:else}
																		<p>No variables available in this testcase</p>
																	{/if}
																</svelte:fragment>
															</AccordionItem>
>>>>>>> branch 'develop' of git@github.com:convertigo/convertigo.git
														{/each}
													{:else}
														<p>No test cases available in this sequence</p>
													{/if}
												</div>
											</div>
											<div class="p-3 font-semiBold bg-surface-100">Response</div>
										</svelte:fragment>
									</AccordionItem>
								</Accordion>
							{/each}
						{:else}
							<p>No sequences available</p>
						{/if}
					</svelte:fragment>
				</AccordionItem>
			</Accordion>

			{#if $connectorsStore && $connectorsStore.length > 0}
				{#each $connectorsStore as connector, index}
					<Accordion
						padding="0"
						class={`rounded-token bg-opacity-50 ${colors[index % colors.length]}`}
					>
						<AccordionItem close>
							<svelte:fragment slot="lead"></svelte:fragment>
							<svelte:fragment slot="summary">
								<p class="text-[14px]">{connector['@_name']}</p>
							</svelte:fragment>
							<svelte:fragment slot="content">
								{#if connector.variables && Object.keys(connector.variables).length > 0}
									<Accordion
										class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
									>
										<form>
											{#each Object.values(connector.variables) as variable}
												<div class="p-5">
													<label class="label-common">
														<p class="font-semibold">{variable['@_name']}</p>
														<input class="input-common" />
													</label>
												</div>
											{/each}
										</form>
									</Accordion>
								{:else}
									<p>No variables available in this connector</p>
								{/if}

								{#if connector.transactions && Object.keys(connector.transactions).length > 0}
									<Accordion
										class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
									>
										{#each Object.values(connector.transactions) as transaction}
											<AccordionItem close>
												<svelte:fragment slot="lead"></svelte:fragment>
												<svelte:fragment slot="summary">
													<p class="font-semibold">{transaction['@_name']}</p>
												</svelte:fragment>
												<svelte:fragment slot="content">
													{#if transaction.variables && Object.keys(transaction.variables).length > 0}
														<Accordion
															class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
														>
															{transaction['@_comment']}
															{#each Object.values(transaction.variables) as variable}
																<div class="p-5">
																	<label class="label-common">
																		<p class="font-semibold">{variable['@_name']}</p>
																		<input class="input-common" />
																	</label>
																</div>
															{/each}
														</Accordion>
													{:else}
														<p>No variables available in this transaction</p>
													{/if}
												</svelte:fragment>
											</AccordionItem>
										{/each}
									</Accordion>
								{:else}
									<p>No transactions available in this connector</p>
								{/if}
							</svelte:fragment>
						</AccordionItem>
					</Accordion>
				{/each}
			{:else}
				<p>No connectors available</p>
			{/if}
		</CardD>
	</div>
</main>

<style lang="postcss">
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
</style>

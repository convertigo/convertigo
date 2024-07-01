<script>
	import { page } from '$app/stores';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import {
		checkTestPlatform,
		connectorsStore,
		sequencesStore,
		testPlatformStore
	} from '$lib/dashboard/stores/testPlatform';
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { marked } from 'marked';

	let project;
	const colors = ['bg-pale-violet', 'bg-pale-blue', 'bg-pale-green', 'bg-pale-pink'];
	onMount(async () => {
		checkTestPlatform(project);
	});

	function endsWithAny(str, suffixes) {
		return suffixes.some((suffix) => str.endsWith(suffix));
	}

	$: {
		page.subscribe(($page) => {
			project = $page.params.project;
		});
	}

	function convertMarkdownToHtml(markdown) {
		// Convert `&#10;` to newline character
		const cleanedMarkdown = markdown.replace(/&#10;/g, '\n');
		// Use `marked` to convert Markdown to HTML
		return marked(cleanedMarkdown);
	}
	// console.log($connectorsStore.)

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

	<CardD class="gap-2">
		<Accordion padding="0" class="bg-pale-pink bg-opacity-50">
			<AccordionItem>
				<svelte:fragment slot="lead"></svelte:fragment>
				<svelte:fragment slot="summary">
					<p class="text-[14px]">Sequences</p>
				</svelte:fragment>
				<svelte:fragment slot="content">
					{#if $sequencesStore && $sequencesStore.length > 0}
						{#each $sequencesStore as sequence, index}
							<Accordion
								padding="0"
								class={`rounded-token bg-opacity-0 ${colors[index % colors.length]}`}
							>
								<AccordionItem close>
									<svelte:fragment slot="lead"></svelte:fragment>
									<svelte:fragment slot="summary">
										<p class="text-[14px]">{sequence['@_name']}</p>
									</svelte:fragment>
									<svelte:fragment slot="content">
										{#if sequence.variables && Object.keys(sequence.variables).length > 0}
											<Accordion
												class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
											>
												<form>
													{#each Object.values(sequence.variables) as variable}
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
											<p>No variables available in this sequence</p>
										{/if}

										{#if sequence.testcases && Object.keys(sequence.testcases).length > 0}
											<Accordion
												padding="px-5"
												class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
											>
												{#each Object.values(sequence.testcases) as testcase}
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
																	{#each Object.values(testcase.variables) as variable}
																		<div class="p-5">
																			<label class="label-common">
																				<p class="font-semibold">{variable['@_name']}</p>
																				<input class="input-common" />
																			</label>
																		</div>
																	{/each}
																</Accordion>
															{:else}
																<p>No variables available in this testcase</p>
															{/if}
														</svelte:fragment>
													</AccordionItem>
												{/each}
											</Accordion>
										{:else}
											<p>No test cases available in this sequence</p>
										{/if}
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
</main>

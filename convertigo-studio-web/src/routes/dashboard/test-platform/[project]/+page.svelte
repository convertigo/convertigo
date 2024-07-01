<script>
	import { page } from '$app/stores';
	import { environmentVariables } from '$lib/admin/stores/symbolsStore';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import {
		checkTestPlatform,
		connectorsStore,
		sequencesStore
	} from '$lib/dashboard/stores/testPlatform';
	import { Accordion, AccordionItem } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';

	let project;
	const colors = ['bg-pale-violet', 'bg-pale-blue', 'bg-pale-green', 'bg-pale-pink'];
	const colorsDp = ['bg-pale-violet', 'bg-pale-blue', 'bg-pale-green', 'bg-pale-pink'];
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
	// console.log($connectorsStore.)

	// Subscribe to sequencesStore to log its value
	sequencesStore.subscribe((value) => {
		console.log('Sequences Store Data:', value);
	});

	connectorsStore.subscribe((value) => {
		console.log('Connectors Store Data:', value);
	});
</script>

<main>
	<h1 class="mb-5">Project: {project}</h1>
	<CardD class="gap-2">
		<Accordion padding="0">
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
								class={`rounded-token bg-opacity-50 ${colors[index % colors.length]}`}
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
												{#each Object.values(sequence.variables) as variable}
													<div class="p-5">
														<label class="label-common">
															<p class="font-semibold">{variable['@_name']}</p>
															<input class="input-common" />
														</label>
													</div>
												{/each}
											</Accordion>
										{:else}
											<p>No variables available in this sequence</p>
										{/if}

										{#if sequence.testcases && Object.keys(sequence.testcases).length > 0}
											<Accordion
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

		{#each $connectorsStore as connector, index}
			<Accordion padding="0" class={`rounded-token bg-opacity-50 ${colors[index % colors.length]}`}>
				<AccordionItem close>
					<svelte:fragment slot="lead"></svelte:fragment>
					<svelte:fragment slot="summary">
						<p class="text-[14px]">{connector['@_name']}</p>
					</svelte:fragment>
					<svelte:fragment slot="content">
						<Accordion class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}>
							{#each Object.values(connector.transactions) as transaction}
								<AccordionItem close>
									<svelte:fragment slot="lead"></svelte:fragment>
									<svelte:fragment slot="summary">
										<p class="font-semibold">{transaction['@_name']}</p>
									</svelte:fragment>
									<svelte:fragment slot="content">
										{transaction['@_comment']}

										{#if connector.transaction?.variables && Object.keys(connector.variables).length > 0}
											<Accordion
												class={`rounded-token bg-opacity-5 gap-2 ${colors[index % colors.length]}`}
											>
												{#each Object.values(connector.transaction?.variables) as variable}
													<div class="p-5">
														<label class="label-common">
															<p class="font-semibold">{variable['@_name']}</p>
															<input class="input-common" />
														</label>
													</div>
												{/each}
											</Accordion>
										{:else}
											<p class="font-bold mt-5">No variables available in this sequence</p>
										{/if}
									</svelte:fragment>
								</AccordionItem>
							{/each}
						</Accordion>
					</svelte:fragment>
				</AccordionItem>
			</Accordion>
		{/each}
	</CardD>
</main>

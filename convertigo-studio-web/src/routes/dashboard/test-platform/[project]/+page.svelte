<script>
	import { page } from '$app/stores';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import {
		checkTestPlatform,
		connectorsStore,
		transactionStore
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
</script>

<main>
	<h1 class="mb-5">Project: {project}</h1>
	<CardD class="gap-2">
		{#each $connectorsStore as connector, index}
			<Accordion padding="0" class={`rounded-token bg-opacity-50 ${colors[index % colors.length]}`}>
				<AccordionItem close>
					<svelte:fragment slot="lead"></svelte:fragment>
					<svelte:fragment slot="summary">
						<p class="text-[14px]">{connector['@_name']}</p>
					</svelte:fragment>
					<svelte:fragment slot="content">
						<Accordion class={`rounded-token bg-opacity-10 ${colorsDp[index % colorsDp.length]}`}>
							{#each Object.values(connector.transaction) as transaction}
								<AccordionItem close>
									<svelte:fragment slot="lead"></svelte:fragment>
									<svelte:fragment slot="summary">
										<p class="font-semibold">{transaction['@_name']}</p>
									</svelte:fragment>
									<svelte:fragment slot="content">
										<div class="rounded-token bg-opacity-40 p-2">
											{#if transaction['@_comment']}
												<p>{transaction['@_comment']}</p>
											{:else}
												<p>No comment</p>
											{/if}
										</div>
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

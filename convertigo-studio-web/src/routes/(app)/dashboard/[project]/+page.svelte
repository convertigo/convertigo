<script>
	import { page } from '$app/stores';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import Table from '$lib/dashboard/components/Table.svelte';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import { Accordion, AccordionItem, getModalStore } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { marked } from 'marked';
	import { SlideToggle } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import { fade, fly } from 'svelte/transition';

	const modalStore = getModalStore();
	let project;
	let _parts = [];
	let searchQuery = '';
	let enableInputVar = {};

	const bgColors = [
		'bg-pale-violet border-[1px] border-pale-violet',
		'bg-pale-blue border-[1px] border-pale-blue',
		'bg-pale-green border-[1px] border-pale-green',
		'bg-pale-pink border-[1px] border-pale-pink'
	];

	onMount(() => {
		checkTestPlatform($page.params.project).then(() => {
			project = $testPlatformStore[$page.params.project];
			_parts = [{ name: 'Sequences', requestables: Object.values(project.sequence || {}) }];
			for (let connector of Object.values(project.connector || {})) {
				_parts.push({
					name: connector['@_name'],
					requestables: Object.values(connector.transaction || {})
				});
			}
			_parts = _parts.filter((part) => part.requestables.length > 0);
		});
	});

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
	let columns = ['Name', 'Value'];

	function openModalInfo(mode) {
		modalStore.trigger({
			type: 'component',
			component: 'modalInfo',
			meta: { mode }
		});
	}

	$: parts = _parts
		.map((part) => ({
			...part,
			requestables: part.requestables.filter((requestable) =>
				requestable['@_name'].toLowerCase().includes(searchQuery.toLowerCase())
			)
		}))
		.filter((part) => part.requestables.length > 0);
</script>

<div class="input-group input-group-divider grid-cols-[auto_1fr_auto]">
	<div class="input-group-shim"><Ico icon="mdi:magnify" /></div>
	<input type="search" placeholder="Search requestable..." bind:value={searchQuery} />
</div>
{#if project}
	<CardD>
		<div class="grid grid-cols-2">
			<div class="col-span-1">
				{project['@_name']}
				<div class="mb-5" />
				{@html convertMarkdownToHtml(project['@_comment'])}
			</div>
			<div class="col-span-1"></div>
		</div>
	</CardD>
	<CardD class="gap-2">
		{#each parts as { name, requestables }, index}
			<Accordion padding="p-4">
				<AccordionItem open={index == 0}>
					<svelte:fragment slot="lead"></svelte:fragment>
					<svelte:fragment slot="summary">
						<p class="text-[18px] font-semibold text-token pb-4 px-2">{name}</p>
						<div class="bottom-0 h-[0.5px] bg-surface-300"></div>
					</svelte:fragment>
					<svelte:fragment slot="content">
						{#each requestables as requestable, index}
							<Accordion
								caretOpen="rotate-0"
								caretClosed="-rotate-90"
								padding="p-4"
								class="rounded-token bg-opacity-20 {bgColors[index % bgColors.length]} border-2"
							>
								<AccordionItem on:toggle={(e) => (requestable.open = e.detail?.open)}>
									<svelte:fragment slot="lead"></svelte:fragment>
									<svelte:fragment slot="summary">
										<div class="flex items-center justify-between relative">
											<span class="text-[14px] text-token font-bold">{requestable['@_name']}</span>
											{#if !requestable.open}
												<span
													transition:fade={{ duration: 200 }}
													class="absolute left-[50%] w-[50%] text-xs color-grey truncate"
													>{requestable['@_comment']}</span
												>
											{/if}
										</div>
									</svelte:fragment>
									<svelte:fragment slot="content">
										<form on:submit|preventDefault>
											<span>{requestable['@_comment']}</span>
											<div
												class="p-3 font-semiBold bg-surface-100 dark:bg-surface-800 flex items-center justify-between"
											>
												<p>Parameters</p>
												<button class="basic-button">Run</button>
											</div>
											<div class="grid grid-cols-2 p-5 gap-10">
												<div class="col-span-1">
													{#each Object.values(requestable.variable ?? {}) as variable}
														{@const { checked } = variable}
														<p class="font-semibold mb-2">{variable['@_name']}</p>
														<div class="flex items-center gap-3">
															<label class="label-common w-full">
																<input
																	class="input-common"
																	disabled={!checked}
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
																{checked}
																on:change={() => (variable.checked = !checked)}
															/>
														</div>
													{/each}
												</div>
												<div class="col-span-1">
													{#if requestable.testcases && Object.keys(requestable.testcases).length > 0}
														{#each Object.values(requestable.testcases) as testcase}
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
														{/each}
													{:else}
														<p>No test cases available in this sequence</p>
													{/if}
												</div>
											</div>
											<div
												class="p-3 font-semiBold bg-surface-100 dark:bg-surface-800 flex items-center justify-between"
											>
												Response
											</div>
										</form>
									</svelte:fragment>
								</AccordionItem>
							</Accordion>
						{/each}
					</svelte:fragment>
				</AccordionItem>
			</Accordion>
		{/each}
	</CardD>
{:else}
	Loading ...
{/if}

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
</style>

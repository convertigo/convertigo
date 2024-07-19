<script>
	import { page } from '$app/stores';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import Table from '$lib/dashboard/components/Table.svelte';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import {
		Accordion,
		AccordionItem,
		getModalStore,
		modeCurrent,
		TabGroup,
		Tab
	} from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import { marked } from 'marked';
	import { SlideToggle } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import { blur, fly } from 'svelte/transition';
	import { flip } from 'svelte/animate';
	import { callRequestable, getUrl } from '$lib/utils/service';
	import Editor from '$lib/studio/editor/Editor.svelte';
	import he from 'he';
	import { RadioGroup, RadioItem } from '@skeletonlabs/skeleton';
	import 'react-device-frameset/styles/marvel-devices.min.css';
	import Icon from '@iconify/svelte';
	import { DeviceMockup } from 'svelte-device-mockups';

	const modalStore = getModalStore();
	let project;
	let _parts = [];
	let searchQuery = '';

	const modes = ['JSON', 'XML', 'BIN', 'CXML'];
	let mode = modes[0];

	let tabSet = 0;
	let deviceVal = 0;
	let appUrl = '';
	let qrCodeUrl = '';

	let landscape;
	let selectedPhoneType = 'iPhone X';

	const bgColors = [
		'bg-pale-violet border-[1px] border-pale-violet',
		'bg-pale-blue border-[1px] border-pale-blue',
		'bg-pale-green border-[1px] border-pale-green',
		'bg-pale-pink border-[1px] border-pale-pink'
	];
	const devices = [
		{ name: 'MacBook Pro', color: 'gold', scale: 5 },
		{ name: 'iPad', color: 'gold', scale: 5 },
		{ name: 'iPhone X', color: 'gold', scale: 5 },
		{ name: 'iPhone 5c', color: 'yellow', scale: 5 }
	];

	const phones = [
		{ name: 'iPhone X', color: 'gold', scale: 5 },
		{ name: 'iPhone 5c', color: 'yellow', scale: 5 },
		{ name: 'iPhone 5s', color: 'silver', scale: 5 }
	];

	function toggleLandscape() {
		landscape = !landscape;
	}
	function handlePhoneTypeChange(event) {
		selectedPhoneType = event.target.value;
	}

	function generateAppUrl(projectName) {
		let href = `/projects/${projectName}/DisplayObjects/mobile/index.html`;
		const fullUrl = getUrl(href);
		console.log(`Generated App URL: ${fullUrl}`);
		return fullUrl;
	}

	function generateQRCodeUrl(projectName) {
		let targetUrl = generateAppUrl(projectName);

		const qrUrl = `${getUrl('/qrcode')}?${new URLSearchParams({
			o: 'image/png',
			e: 'L',
			s: '2',
			d: targetUrl
		}).toString()}`;
		console.log(`Generated QR Code URL: ${qrUrl}`);
		return qrUrl;
	}

	function updateUrls() {
		const projectName = $page.params.project;
		appUrl = generateAppUrl(projectName);
		qrCodeUrl = generateQRCodeUrl(projectName);
		console.log('App URL', appUrl);
		console.log('QR Code URL', qrCodeUrl);
	}
	// // Create a custom renderer
	// const renderer = new Renderer();

	// // Customizing paragraph rendering
	// renderer.paragraph = (text) => {
	// 	return `<p class="custom-paragraph">${text}</p>`;
	// };

	// // Customizing heading rendering
	// renderer.heading = (text, level) => {
	// 	return `<h${level} class="custom-heading">${text}</h${level}>`;
	// };

	/**
	 * @param {string} markdown
	 */
	function convertMarkdownToHtml(markdown) {
		const cleanedMarkdown = he.decode(markdown);
		return marked(cleanedMarkdown);
	}

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
			updateUrls();
		});
	});

	async function run(requestable, event) {
		if (event.submitter.textContent == 'Clear') {
			requestable.response = '';
			_parts = _parts;
			return;
		}
		requestable.loading = true;
		requestable.response = 'Loading …';
		_parts = _parts;
		const data = await callRequestable(mode, project['@_name'], new FormData(event.target));
		requestable.response = await data.text();
		requestable.language = data.headers.get('Content-Type')?.includes('json') ? 'json' : 'xml';
		requestable.loading = false;
		_parts = _parts;
	}

	/**
	 * @param {{ variables: { [s: string]: any; } | ArrayLike<any>; }} testcase
	 */
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

	$: selectedPhone = phones.find((phone) => phone.name === selectedPhoneType) || {
		name: 'iPhone X',
		color: 'gold',
		scale: 5
	};

	$: parts = _parts
		.map((part) => ({
			...part,
			requestables: part.requestables.filter(
				(/** @type {{ [x: string]: string; }} */ requestable) =>
					requestable['@_name'].toLowerCase().includes(searchQuery.toLowerCase())
			)
		}))
		.filter((part) => part.requestables.length > 0);
</script>

{#if project}
	{@const [duration, y, opacity] = [200, -50, 1]}

	{#if tabSet === 0}
		<CardD>
			<div class="input-group input-group-divider grid-cols-[auto_1fr_auto] w-96">
				<div class="input-group-shim"><Ico icon="mdi:magnify" /></div>
				<input type="search" placeholder="Search requestable..." bind:value={searchQuery} />
			</div>

			<div class="grid grid-cols-2 mt-5">
				<div class="col-span-1">
					{project['@_name']}
					<div class="mb-5" />
					{@html convertMarkdownToHtml(project['@_comment'])}
				</div>
				<div class="col-span-1"></div>
			</div>
		</CardD>
	{/if}
	<!-- {#if tabSet === 1}
			<TabGroup rounded="rounded-none">
				<Tab bind:group={tabSet} name="tab1" value={0} class="w-[50%] bg-surface-700">
					<span>Backend</span>
				</Tab>
				<Tab bind:group={tabSet} name="tab2" value={1} class="w-[50%] bg-surface-700">
					<span>Frontend</span>
				</Tab>
			</TabGroup>
		{/if} -->

	<TabGroup rounded="rounded-none" border="border-none">
		{#if tabSet === 0}
			<Tab bind:group={tabSet} name="tab1" value={0} class="w-[50%] bg-surface-700">
				<span>Backend</span>
			</Tab>
			<Tab bind:group={tabSet} name="tab2" value={1} class="w-[50%] bg-surface-700">
				<span>Frontend</span>
			</Tab>
		{/if}
		<svelte:fragment slot="panel">
			{#if tabSet === 0}
				{#each parts as { name, requestables }, index (name)}
					<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
						<Accordion caretOpen="rotate-0" caretClosed="-rotate-90" padding="p-4">
							<AccordionItem open={index == 0 || searchQuery.length > 0}>
								<svelte:fragment slot="lead"></svelte:fragment>
								<svelte:fragment slot="summary">
									<p class="text-[18px] font-semibold text-token pb-4 px-2">{name}</p>
									<div class="bottom-0 h-[0.5px] bg-surface-300"></div>
								</svelte:fragment>
								<svelte:fragment slot="content">
									{#each requestables as requestable, index (requestable['@_name'])}
										<div animate:flip={{ duration }} transition:fly={{ duration, y }}>
											<Accordion
												caretOpen="rotate-0"
												caretClosed="-rotate-90"
												padding="p-4"
												class="rounded-token bg-opacity-20 {bgColors[
													index % bgColors.length
												]} border-2"
											>
												<AccordionItem
													on:toggle={(e) => (requestable.open = e.detail?.open)}
													open={requestable.open}
												>
													<svelte:fragment slot="lead"></svelte:fragment>
													<svelte:fragment slot="summary">
														<div class="flex items-center justify-between relative">
															<span class="text-[14px] text-token font-bold"
																>{requestable['@_name']}</span
															>
															{#if !requestable.open}
																<span
																	transition:fly={{ duration, y: 20 }}
																	class="absolute left-[50%] w-[50%] text-xs color-grey truncate"
																	>{requestable['@_comment']}</span
																>
															{/if}
														</div>
													</svelte:fragment>
													<svelte:fragment slot="content">
														<form
															on:submit|preventDefault={async (e) => {
																run(requestable, e);
															}}
															class="flex flex-col gap-3"
														>
															{#if name == 'Sequences'}
																<input
																	type="hidden"
																	name="__sequence"
																	value={requestable['@_name']}
																/>
															{:else}
																<input type="hidden" name="__connector" value={name} />
																<input
																	type="hidden"
																	name="__transaction"
																	value={requestable['@_name']}
																/>
															{/if}
															<span>{requestable['@_comment']}</span>
															<div class="p-3 font-semiBold bg-surface-100 dark:bg-surface-800">
																<p>Parameters</p>
															</div>
															<div class="grid grid-cols-2 p-5 gap-10">
																<div class="col-span-1">
																	{#each Object.values(requestable.variable ?? {}) as variable}
																		{@const { checked } = variable}
																		<label class="label-common">
																			<p class="font-semibold mb-2">{variable['@_name']}</p>
																			<div class="flex items-center gap-3">
																				{#if checked}
																					<input
																						class="input-common"
																						required={variable['@_required']}
																						name={variable['@_name']}
																						value={variable['@_value']}
																						in:blur={{ duration, opacity }}
																					/>
																				{:else}
																					<input
																						class="input-common"
																						style="color: grey;"
																						value={variable['@_value']}
																						readonly={true}
																						in:blur={{ duration, opacity }}
																						on:click={() => {
																							variable.checked = true;
																						}}
																					/>
																				{/if}
																				<SlideToggle
																					active="activeSlideToggle"
																					background="unActiveSlideToggle"
																					size="sm"
																					name=""
																					{checked}
																					on:change={() => {
																						variable.checked = !checked;
																					}}
																				/>
																			</div>
																		</label>
																	{/each}
																</div>
																<div class="col-span-1">
																	{#if requestable.testcases && Object.keys(requestable.testcases).length > 0}
																		{#each Object.values(requestable.testcases) as testcase}
																			<p class="font-semibold mb-4">{testcase['@_name']}</p>

																			{#if testcase.variables && Object.keys(testcase.variables).length > 0}
																				{@const data = Object.values(testcase.variables).map(
																					(variable) => [
																						variable['@_name'],
																						convertMarkdownToHtml(variable['@_value'])
																					]
																				)}
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
																		theme={$modeCurrent ? '' : 'vs-dark'}
																	/>
																</div>
															{/if}
														</form>
													</svelte:fragment>
												</AccordionItem>
											</Accordion>
										</div>
									{/each}
								</svelte:fragment>
							</AccordionItem>
						</Accordion>
					</div>
				{/each}
			{:else if tabSet === 1}
				<div class="grid grid-cols-3">
					<div class="col-span-1">
						<TabGroup rounded="rounded-none" border="border-none" class="">
							<Tab bind:group={tabSet} name="tab1" value={0} class="bg-surface-700 rounded-token">
								<span class="text-[12px] flex items-center gap-2"
									><Ico icon="ion:return-down-back-sharp" /><span class="text-[12px]"
										>Return to Backend</span
									></span
								>
							</Tab>
						</TabGroup>
					</div>
					<div class="col-span-1 flex items-center justify-center gap-5">
						<RadioGroup active="bg-surface-800" class="">
							<RadioItem bind:group={deviceVal} name="justify" value={0}
								><Icon icon="fluent:laptop-20-regular" style="color: white" /></RadioItem
							>
							<RadioItem bind:group={deviceVal} name="justify" value={1}
								><Icon icon="fluent:tablet-48-regular" style="color: white" /></RadioItem
							>
							<RadioItem bind:group={deviceVal} name="justify" value={2}
								><Icon icon="fluent:phone-20-regular" style="color: white" /></RadioItem
							>
						</RadioGroup>
						<button class="basic-button" on:click={() => toggleLandscape()}>Landscape</button>
						{#if deviceVal == 2}
							<select on:change={handlePhoneTypeChange} class="select-common text-[12px]">
								{#each phones as phone}
									<option value={phone.name}><span>{phone.name}</span></option>
								{/each}
							</select>
						{/if}
					</div>
					<div class="col-span-1 flex items-center justify-end gap-2"></div>
				</div>
				<div class="flex flex-col items-center justify-start mt-5">
					{#if deviceVal >= 0 && deviceVal < devices.length}
						<DeviceMockup
							device={deviceVal == 2 ? selectedPhone.name : devices[deviceVal].name}
							scale={deviceVal == 2 ? selectedPhone.scale : devices[deviceVal].scale}
							{landscape}
							deviceColor={deviceVal == 2 ? selectedPhone.color : devices[deviceVal].color}
							src={appUrl}
						/>
					{/if}
				</div>

				{#if qrCodeUrl}
					<a href={appUrl} target="_blank">
						<img src={qrCodeUrl} alt="QR code" title="QR code" />
					</a>
				{/if}
				<!-- <iframe
						src="http://localhost:18080/convertigo/projects/QuestHunter/DisplayObjects/mobile/index.html"
						class="h-[700px] w-full"
					></iframe> -->
			{/if}
		</svelte:fragment>
	</TabGroup>
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

	.marked-p-class {
		color: aqua;
	}
</style>

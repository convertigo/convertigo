<script>
	import { projectsCheck, projectsStore } from '$lib/admin/stores/projectsStore';
	import { onMount } from 'svelte';
	import { Accordion, AccordionItem, getModalStore, popup } from '@skeletonlabs/skeleton';
	import Ico from '$lib/utils/Ico.svelte';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import { flip } from 'svelte/animate';
	import { fade } from 'svelte/transition';

	onMount(() => {
		projectsCheck();
	});

	const modalStore = getModalStore();

	// Create a writable store for the search query
	let searchQuery = '';

	const settingsPopup = [
		{ icon: 'mdi:edit-outline', title: 'Rename Project' },
		{ icon: 'fluent:open-24-filled', title: 'Open In New Tab' },
		{ icon: 'fad:duplicate', title: 'Duplicate Project' },
		{ icon: 'grommet-icons:add', title: 'Add Tag' },
		{ icon: 'mingcute:delete-line', title: 'Delete Project' }
	];

	/** @type {import('@skeletonlabs/skeleton').PopupSettings } */
	const popupClick = {
		event: 'click',
		target: 'popupClick',
		placement: 'bottom'
	};

	function openModalSettings(mode, project) {
		modalStore.trigger({
			type: 'component',
			component: 'modalSettingsProject',
			meta: { mode, project }
		});
	}

	function showQRCode() {
		let href = 'qrcode/' + $projectsStore['@_name'] + '/DisplayObjects/mobile/index.html';
	}

	$: filteredProjects = $projectsStore.filter((project) =>
		project['@_name'].toLowerCase().includes(searchQuery.toLowerCase())
	);
</script>

<CardD class="gap-5">
	<div class="input-group input-group-divider grid-cols-[auto_1fr_auto]">
		<div class="input-group-shim"><Ico icon="mdi:magnify" /></div>
		<input type="search" placeholder="Search projects..." bind:value={searchQuery} />
	</div>
	<div class="grid gap-5 grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
		{#each filteredProjects as project (project['@_name'])}
			<div class="flex flex-col" animate:flip={{ duration: 500 }} transition:fade>
				<div
					class="flex gap-3 justify-between items-center p-2 border-[1px] rounded-t-md dark:border-surface-700 border-surface-200"
				>
					<span class="text-md font-semibold">{project['@_name']}</span>
					<div class="flex gap-3">
						<button class="basic-button">Back</button>
						<button class="violet-button">Front</button>
						<button use:popup={popupClick}>
							<Ico icon="mdi:dots-vertical" />
						</button>
						<div class="px-2 py-3 z-10 bg-surface-600 rounded-token gap-2" data-popup="popupClick">
							{#each settingsPopup as setting}
								<button
									on:click={() => {
										if (setting.title === 'Delete Project') {
											console.log('Project name:', project['@_name']);
										} else {
											openModalSettings(setting.title);
										}
									}}
									class="flex text-light text-[12px] justify-start w-full px-3 hover:bg-surface-400"
								>
									<Ico icon={setting.icon} />
									<p>{setting.title}</p>
								</button>
							{/each}
							<div class="arrow bg-surface-600" />
						</div>
					</div>
				</div>
				<a href="{project['@_name']}/">
					<img
						src="https://www.impactplus.com/hubfs/Fensea.jpg"
						class="object-cover border-[1px] border-t-0 rounded-b-md dark:border-surface-700 border-surface-200 dark:opacity-70"
						alt="project"
					/>
				</a>
				<Accordion>
					<AccordionItem close>
						<svelte:fragment slot="summary"
							>{project['@_comment'].match(/(\w+\s*){1,5}/)[0] + '...'}</svelte:fragment
						>
						<svelte:fragment slot="content">{project['@_comment']}</svelte:fragment>
					</AccordionItem>
				</Accordion>
			</div>
		{/each}
	</div>
</CardD>

<style lang="postcss">
	.search-bar {
		@apply placeholder:text-[16px] placeholder:dark:text-surface-500 placeholder:text-surface-200 text-token placeholder:font-light font-normal border-none dark:bg-surface-800 rounded-token;
		border-bottom: surface-200;
	}
</style>

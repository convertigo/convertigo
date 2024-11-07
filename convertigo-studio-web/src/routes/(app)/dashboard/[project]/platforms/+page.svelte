<script>
	import { page } from '$app/stores';
	import CardD from '$lib/dashboard/components/Card-D.svelte';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import { Accordion, AccordionItem, getModalStore, modeCurrent } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import 'react-device-frameset/styles/marvel-devices.min.css';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { call, copyObj, getQuery, getUrl } from '$lib/utils/service';
	import Ico from '$lib/utils/Ico.svelte';
	import QrCode from '$lib/common/components/QrCode.svelte';

	const modalStore = getModalStore();
	let app;

	let data = $state([
		{ Name: 'Mobile Project Name', Attr: '@_mobileProjectName' },
		{ Name: 'Endpoint', Attr: '@_endpoint' },
		{ Name: 'Application Id', Attr: '@_applicationID' },
		{ Name: 'Version', Attr: '@_applicationVersion' }
	]);

	const dataPlatform = [
		{ Name: 'Display Name', Attr: '@_displayName' },
		{ Name: 'Package Type', Attr: '@_packageType' },
		{ Name: 'Local Revision' },
		{ Name: 'Built Revision' },
		{ Name: 'Built Application Version' },
		{ Name: 'Cordova Version' }
	];

	let platforms = $state([]);

	onMount(() => {
		const unsubscribe = page.subscribe(($page) => {
			const projectName = $page.params.project;
			checkTestPlatform(projectName).then(() => {
				app = Object.values($testPlatformStore[projectName].mobileapplication)[0];
				for (let i = 0; i < data.length; i++) {
					data[i].Value = app[data[i].Attr];
				}
				for (let platform of Object.values(app.mobileplatform)) {
					platform.data = copyObj(dataPlatform);
					for (let i = 0; i < dataPlatform.length; i++) {
						platform.data[i].Value = platform[dataPlatform[i].Attr ?? 0];
					}
					platforms.push(platform);
					call('mobiles.GetLocalRevision', {
						project: projectName,
						platform: platform['@_name']
					}).then((res) => {
						platform.data[2].Value = res.admin?.revision;
						platforms = platforms;
					});
					getBuildStatus(platform);
				}
				platforms = platforms;
			});
		});
		return () => unsubscribe();
	});

	async function getBuildStatus(platform) {
		const res = await call('mobiles.GetBuildStatus', {
			project: $page.params.project,
			platform: platform['@_name']
		});
		platform.data[3].Value = res.admin?.build?.['@_revision'];
		platform.data[4].Value = res.admin?.build?.['@_version'];
		platform.data[5].Value = res.admin?.build?.['@_phonegap_version'];
		platform.status = res.admin?.build?.['@_status'];
		if (platform.status == 'pending') {
			setTimeout(() => getBuildStatus(platform), 2500);
		}
		platforms = platforms;
	}

	async function build(platform) {
		delete platform.status;
		const res = await call('mobiles.LaunchBuild', {
			project: $page.params.project,
			platform: platform['@_name']
		});
		getBuildStatus(platform);
	}
</script>

<CardD>
	<TableAutoCard
		showHeaders={false}
		definition={[
			{ key: 'Name', custom: true },
			{ key: 'Value', custom: true }
		]}
		{data}
	>
		{#snippet children(row, def)}
			{#if def.key === 'Name'}
				<span class="font-normal">{row.Name}</span>
			{:else}
				<AutoPlaceholder loading={row[def.key] == null}>{row[def.key] ?? ''}</AutoPlaceholder>
			{/if}
		{/snippet}
	</TableAutoCard>
</CardD>

<CardD>
	<Accordion caretOpen="rotate-0" caretClosed="-rotate-90" class="" autocollapse>
		{#each platforms as platform, i}
			<AccordionItem open={i == 0} class="preset-ghost-surface rounded">
				<svelte:fragment slot="lead">{platform['@_displayName']}</svelte:fragment>
				<svelte:fragment slot="summary"
					><AutoPlaceholder loading={!platform.status}
						><span
							class:text-success-500={platform.status}
							class:text-error-500={platform.status != 'complete'}
							class:text-warning-500={platform.status == 'pending'}
							class:animate-pulse={platform.status == 'pending'}>⬤</span
						></AutoPlaceholder
					></svelte:fragment
				>
				<svelte:fragment slot="content">
					<div class="flex flex-wrap justify-center gap-2">
						<TableAutoCard
							class="max-w-fit"
							showHeaders={false}
							definition={[
								{ key: 'Name', custom: true },
								{ key: 'Value', custom: true }
							]}
							data={platform.data}
						>
							{#snippet children(row, def)}
								{#if def.key === 'Name'}
									<span class="font-normal">{row.Name}</span>
								{:else}
									<AutoPlaceholder loading={row[def.key] == null}
										>{row[def.key] ?? ''}</AutoPlaceholder
									>
								{/if}
							{/snippet}
						</TableAutoCard>
						{#if (platform.status ?? 'none') != 'none'}
							<CardD class="flex min-w-48 justify-center">
								{#if platform.status == 'complete'}
									<QrCode
										class="max-w-48"
										href={getUrl() +
											'mobiles.GetPackage' +
											getQuery({ project: $page.params.project, platform: platform['@_name'] })}
									/>
								{:else if platform.status == 'pending'}
									<div class="text-warning-500 animate-pulse">Building...</div>
								{:else if platform.status == 'error'}
									<div class="text-error">Error</div>
								{/if}
							</CardD>
						{/if}
					</div>
					<div class="flex flex-wrap justify-center gap-2">
						<button onclick={() => build(platform)} class="btn preset-filled-primary"
							><span><Ico icon="mdi:briefcase-upload-outline" /></span><span
								>Build Mobile Platform</span
							></button
						>
						<a
							href="{getUrl()}mobiles.GetSourcePackage?project={window.encodeURIComponent(
								$page.params.project
							)}&platform={window.encodeURIComponent(platform['@_name'])}"
							class="btn preset-filled-tertiary"
							><span><Ico icon="mdi:file-download-outline" /></span><span>Get Source Package</span
							></a
						>
					</div>
				</svelte:fragment>
			</AccordionItem>
		{/each}
	</Accordion>
</CardD>

<style lang="postcss">
</style>

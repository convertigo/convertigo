<script>
	import { page } from '$app/stores';
	import { checkTestPlatform, testPlatformStore } from '$lib/common/stores/testPlatform';
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import { onMount } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { call, copyObj, getQuery, getUrl } from '$lib/utils/service';
	import Ico from '$lib/utils/Ico.svelte';
	import QrCode from '$lib/common/components/QrCode.svelte';
	import Card from '$lib/admin/components/Card.svelte';

	let app;

	let data = $state([
		{ Name: 'Mobile Project Name', Attr: 'mobileProjectName' },
		{ Name: 'Endpoint', Attr: 'endpoint' },
		{ Name: 'Application Id', Attr: 'applicationID' },
		{ Name: 'Version', Attr: 'applicationVersion' }
	]);

	const dataPlatform = [
		{ Name: 'Display Name', Attr: 'displayName' },
		{ Name: 'Package Type', Attr: 'packageType' },
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
						platform: platform.name
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
			platform: platform.name
		});
		platform.data[3].Value = res.admin?.build?.revision;
		platform.data[4].Value = res.admin?.build?.version;
		platform.data[5].Value = res.admin?.build?.phonegap_version;
		platform.status = res.admin?.build?.status;
		if (platform.status == 'pending') {
			setTimeout(() => getBuildStatus(platform), 2500);
		}
		platforms = platforms;
	}

	async function build(platform) {
		delete platform.status;
		const res = await call('mobiles.LaunchBuild', {
			project: $page.params.project,
			platform: platform.name
		});
		getBuildStatus(platform);
	}
</script>

<Card>
	<TableAutoCard
		showHeaders={false}
		definition={[
			{ key: 'Name', custom: true },
			{ key: 'Value', custom: true }
		]}
		{data}
	>
		{#snippet children({ row, def })}
			{#if def.key === 'Name'}
				<span class="font-normal">{row.Name}</span>
			{:else}
				<AutoPlaceholder loading={row[def.key] == null}>{row[def.key] ?? ''}</AutoPlaceholder>
			{/if}
		{/snippet}
	</TableAutoCard>
</Card>

<Card>
	<Accordion>
		{#each platforms as platform, i}
			<!-- open={i == 0} -->
			<Accordion.Item classes="preset-ghost-surface rounded" value="ok">
				{#snippet control()}{platform.displayName}
					<AutoPlaceholder loading={!platform.status}
						><span
							class:text-success-500={platform.status}
							class:text-error-500={platform.status != 'complete'}
							class:text-warning-500={platform.status == 'pending'}
							class:animate-pulse={platform.status == 'pending'}>⬤</span
						></AutoPlaceholder
					>{/snippet}
				{#snippet panel()}
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
							{#snippet children({ row, def })}
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
											getQuery({ project: $page.params.project, platform: platform.name })}
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
							)}&platform={window.encodeURIComponent(platform.name)}"
							class="btn preset-filled-tertiary"
							><span><Ico icon="mdi:file-download-outline" /></span><span>Get Source Package</span
							></a
						>
					</div>
				{/snippet}
			</Accordion.Item>
		{/each}
	</Accordion>
</Card>

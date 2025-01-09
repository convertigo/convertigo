<script>
	import { page } from '$app/state';
	import { Accordion } from '@skeletonlabs/skeleton-svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { call, getQuery, getUrl } from '$lib/utils/service';
	import Ico from '$lib/utils/Ico.svelte';
	import QrCode from '$lib/common/components/QrCode.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';

	let project = $state(TestPlatform(page.params.project));
	let app = $derived(project?.mobileapplication);
	let platforms = $derived(app?.mobileplatform ?? []);

	const dataPlatform = [
		{ Name: 'Display Name', Attr: 'displayName' },
		{ Name: 'Package Type', Attr: 'packageType' },
		{ Name: 'Local Revision' },
		{ Name: 'Built Revision' },
		{ Name: 'Built Application Version' },
		{ Name: 'Cordova Version' }
	];

	// $effect(() => {
	// for (let platform of app.mobileplatform) {
	// 	platform.data = copyObj(dataPlatform);
	// 	for (let i = 0; i < dataPlatform.length; i++) {
	// 		platform.data[i].Value = platform[dataPlatform[i].Attr ?? 0];
	// 	}
	// 	platforms.push(platform);
	// 	call('mobiles.GetLocalRevision', {
	// 		project: project.name,
	// 		platform: platform.name
	// 	}).then((res) => {
	// 		platform.data[2].Value = res.admin?.revision;
	// 		platforms = platforms;
	// 	});
	// 	getBuildStatus(platform);
	// }
	// platforms = platforms;
	// });

	async function getBuildStatus(platform) {
		const res = await call('mobiles.GetBuildStatus', {
			project: page.params.project,
			platform: platform.name
		});
		platform.data[3].Value = res.admin?.build?.revision;
		platform.data[4].Value = res.admin?.build?.version;
		platform.data[5].Value = res.admin?.build?.phonegap_version;
		platform.status = res.admin?.build?.status;
		if (platform.status == 'pending') {
			setTimeout(() => getBuildStatus(platform), 2500);
		}
	}

	async function build(platform) {
		delete platform.status;
		const res = await call('mobiles.LaunchBuild', {
			project: page.params.project,
			platform: platform.name
		});
		getBuildStatus(platform);
	}
</script>

<Card>
	<TableAutoCard
		showHeaders={false}
		definition={[{ key: 'key', class: 'font-bold w-0 text-nowrap' }, { key: 'val' }]}
		data={[
			{ key: 'Mobile Project Name', val: app?.mobileProjectName },
			{ key: 'Endpoint', val: app?.endpoint },
			{ key: 'Application Id', val: app?.applicationID },
			{ key: 'Version', val: app?.applicationVersion }
		]}
	/>
	<Accordion value={[platforms[0]?.name ?? '']}>
		{#each platforms as platform, i}
			<Accordion.Item classes="preset-ghost-surface rounded" value={platform.name}>
				{#snippet control()}{platform.displayName}
					<!-- <AutoPlaceholder loading={!platform.status}
						><span
							class:text-success-500={platform.status}
							class:text-error-500={platform.status != 'complete'}
							class:text-warning-500={platform.status == 'pending'}
							class:animate-pulse={platform.status == 'pending'}>⬤</span
						></AutoPlaceholder> -->
				{/snippet}
				{#snippet panel()}
					<div class="layout-y-stretch">
						<TableAutoCard
							showHeaders={false}
							definition={[{ key: 'key', class: 'font-bold w-0 text-nowrap' }, { key: 'val' }]}
							data={[
								{ key: 'Comment', val: platform.comment },
								{ key: 'Package Type', val: platform.packageType },
								{ key: 'Version', val: platform.version },
								{ key: 'Local Revision', val: platform.revision },
								{ key: 'Built Revision', val: platform.built },
								{ key: 'Built Application Version', val: platform.built },
								{ key: 'Cordova Version', val: platform.cordova }
							]}
						/>
						<!-- {#if (platform.status ?? 'none') != 'none'}
							<Card class="flex min-w-48 justify-center">
								{#if platform.status == 'complete'}
									<QrCode
										class="max-w-48"
										href={getUrl() +
											'mobiles.GetPackage' +
											getQuery({ project: page.params.project, platform: platform.name })}
									/>
								{:else if platform.status == 'pending'}
									<div class="text-warning-500 animate-pulse">Building...</div>
								{:else if platform.status == 'error'}
									<div class="text-error">Error</div>
								{/if}
							</Card>
						{/if} -->
					</div>
					<!-- <div class="flex flex-wrap justify-center gap-2">
						<button onclick={() => build(platform)} class="btn preset-filled-primary"
							><span><Ico icon="mdi:briefcase-upload-outline" /></span><span
								>Build Mobile Platform</span
							></button
						>
						<a
							href="{getUrl()}mobiles.GetSourcePackage?project={window.encodeURIComponent(
								page.params.project
							)}&platform={window.encodeURIComponent(platform.name)}"
							class="btn preset-filled-tertiary"
							><span><Ico icon="mdi:file-download-outline" /></span><span>Get Source Package</span
							></a
						>
					</div> -->
				{/snippet}
			</Accordion.Item>
		{/each}
	</Accordion>
</Card>

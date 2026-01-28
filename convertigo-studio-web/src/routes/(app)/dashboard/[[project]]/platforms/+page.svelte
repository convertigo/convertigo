<script>
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import AccordionGroup from '$lib/common/components/AccordionGroup.svelte';
	import AccordionSection from '$lib/common/components/AccordionSection.svelte';
	import QrCode from '$lib/common/components/QrCode.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { getQuery, getUrl } from '$lib/utils/service';
	import { getContext, onMount } from 'svelte';

	let modalAlert = getContext('modalAlert');
	let project = $state(TestPlatform(page.params.project));
	let app = $derived(project?.mobileapplication);
	let platforms = $derived(app?.mobileplatform ?? []);
	let openedPlatform = $state([]);
</script>

<Card title={project?.name ?? null}>
	<TableAutoCard
		showHeaders={false}
		definition={[{ key: 'key', class: 'font-medium w-0 text-nowrap' }, { key: 'val' }]}
		data={[
			{ key: 'Mobile Project Name', val: app?.mobileProjectName },
			{ key: 'Endpoint', val: app?.endpoint },
			{ key: 'Application Id', val: app?.applicationID },
			{ key: 'Version', val: app?.applicationVersion }
		]}
	/>
	<AccordionGroup
		bind:value={
			() => (openedPlatform.length ? openedPlatform : [platforms[0]?.name]),
			(v) => (openedPlatform = v)
		}
	>
		{#each platforms as { name, displayName, packageType, local, built: { status, revision, version, phonegap_version, endpoint, waiting, error }, classname, comment, build } (name)}
			<AccordionSection
				value={name}
				triggerClass="border-b-[0.5px] px-low py-low text-left"
				title={displayName}
			>
				{#snippet lead()}
					<span class="inline-flex items-center text-lg font-semibold">
						<span
							class:text-success-500={status == 'complete'}
							class:text-error-500={status == 'error'}
							class:text-warning-500={status == 'pending' || status == 'none'}
							class:animate-pulse={!status || status == 'pending'}
						>
							⬤
						</span>
					</span>
				{/snippet}
				{#snippet trail()}
					{#if comment}
						<span class="max-w-52 truncate text-xs text-surface-600-400">{comment}</span>
					{/if}
				{/snippet}
				{#snippet panel()}
					<div class="layout-y-stretch md:layout-x-stretch">
						<TableAutoCard
							showHeaders={false}
							definition={[{ key: 'key', class: 'font-medium w-0 text-nowrap' }, { key: 'val' }]}
							data={[
								{ key: 'Platform', val: `${classname} (.${packageType})` },
								{ key: 'Local Revision', val: local.revision },
								{ key: 'Built Revision', val: revision },
								{ key: 'Built Application Version', val: version },
								{ key: 'Built Cordova Version', val: phonegap_version },
								{ key: 'Built Endpoint', val: endpoint }
							]}
						/>
						<div class="layout-x md:layout-y">
							<div class="layout-y h-full w-full justify-center">
								{#if status == 'complete'}
									<QrCode
										class="w-full max-w-96"
										href={getUrl() +
											'mobiles.GetPackage' +
											getQuery({ project: page.params.project, platform: name })}
									/>
								{:else if status == 'pending'}
									<div class="animate-pulse text-warning-500">Building...</div>
								{:else if status == 'error'}
									<div class="text-error-500">
										<strong>Build Error</strong>
										<pre class="text-wrap">{error.substring(0, 200)}</pre>
									</div>
								{:else if status == 'none'}
									<div class="text-warning-500">Not Built</div>
								{:else}
									<AutoPlaceholder loading={true} />
								{/if}
							</div>
							<div class="layout-y md:layout-x-stretch">
								{#if status == 'error'}
									<Button
										label="Show Error"
										icon="mdi:briefcase-upload-outline"
										class="button-secondary"
										onclick={() =>
											modalAlert.open({
												message: 'Build Error',
												stacktrace: error,
												showStack: true
											})}
									/>
								{/if}
								<Button
									label="Source Package"
									icon="mdi:briefcase-upload-outline"
									class="button-secondary"
									rel="external"
									href={getUrl() +
										'mobiles.GetSourcePackage' +
										getQuery({ project: project.name, platform: name })}
								/>
								<Button
									label="Build Platform"
									icon="mdi:briefcase-upload-outline"
									class="button-primary"
									disabled={waiting}
									onclick={build}
								/>
							</div>
						</div>
					</div>
				{/snippet}
			</AccordionSection>
		{/each}
	</AccordionGroup>
</Card>

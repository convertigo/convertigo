<script>
	import { browser } from '$app/environment';
	import { beforeNavigate } from '$app/navigation';
	import { page } from '$app/state';
	import Button from '$lib/admin/components/Button.svelte';
	import Card from '$lib/admin/components/Card.svelte';
	import Authentication from '$lib/common/Authentication.svelte';
	import Projects from '$lib/common/Projects.svelte';
	import { stopTestPlatform } from '$lib/common/TestPlatform.svelte';
	import PagesRail from '$lib/dashboard/PagesRail.svelte';
	import Project from '$lib/dashboard/Project.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { resolve } from '$lib/utils/route';
	import { onDestroy } from 'svelte';

	let { children } = $props();
	const getSwaggerProjectUrl = (project) =>
		`/convertigo/swagger/dist/index.html?url=${encodeURIComponent(
			`/convertigo/openapi?YAML&__project=${project}`
		)}`;
	$effect(() => {
		Project.page = page;
	});

	let canEditInAdmin = $derived(
		Authentication.canAccessAdmin ||
			Authentication.hasRole('PROJECT_DBO_VIEW') ||
			Authentication.hasRole('PROJECT_DBO_CONFIG')
	);
	let projectName = $derived(page.params?.project);
	let currentProject = $derived.by(() =>
		projectName ? Projects.projects.find(({ name }) => name == projectName) : null
	);
	let projectLookupReady = $derived(!projectName || Projects.init);
	let missingProject = $derived(Boolean(projectName) && projectLookupReady && !currentProject);

	$effect(() => {
		const extras = [];
		const project = projectName;
		if (project && !missingProject) {
			if (Project.hasRef) {
				extras.push({
					title: 'References',
					icon: 'mdi:power-plug',
					page: '/(app)/dashboard/[[project]]',
					params: { project }
				});
			}
			extras.push({
				title: 'Backend',
				icon: 'mdi:cog',
				page: '/(app)/dashboard/[[project]]/backend',
				id: '/(app)/dashboard/[[project]]/backend/[sequence]',
				params: { project }
			});
			if (canEditInAdmin) {
				extras.push({
					title: 'Edit',
					icon: 'mdi:edit-outline',
					page: '/(app)/admin/projects/[project]',
					params: { project }
				});
			}
			if (Project.hasFrontend) {
				extras.push({
					title: 'Frontend',
					icon: 'mdi:smartphone-link',
					page: '/(app)/dashboard/[[project]]/frontend',
					id: '/(app)/dashboard/[[project]]/frontend/[model]',
					params: { project }
				});
			}
			if (Project.hasPlatforms) {
				extras.push({
					title: 'Platforms',
					icon: 'mdi:package-variant-closed',
					page: '/(app)/dashboard/[[project]]/platforms',
					params: { project }
				});
			}
			extras.push({
				title: 'Swagger',
				icon: 'mdi:swagger',
				url: getSwaggerProjectUrl(project),
				external: true
			});
		}
		PagesRail.extras = extras;
	});
	beforeNavigate(({ to }) => {
		if (!to?.params?.project) {
			PagesRail.extras = [];
		}
	});
	if (browser) {
		onDestroy(() => {
			Projects.stop();
			const project = page.params?.project;
			if (project) {
				stopTestPlatform(project);
			}
		});
	}
</script>

{#if projectName && !projectLookupReady}
	<div class="grid min-h-[40vh] place-items-center">
		<AutoPlaceholder loading={true} />
	</div>
{:else if missingProject}
	<div class="grid min-h-[40vh] place-items-center">
		<Card title="Project not found" class="max-w-xl">
			<p class="text-sm text-surface-700-300">
				The project <strong>{projectName}</strong> does not exist or is no longer available.
			</p>
			<div class="mt-4 layout-x justify-end">
				<Button
					href={resolve('/(app)/dashboard')}
					class="button-primary"
					label="Back to Projects"
					icon="mdi:arrow-left"
					full={false}
				/>
			</div>
		</Card>
	</div>
{:else}
	{@render children?.()}
{/if}

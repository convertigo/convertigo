<script>
	import { browser } from '$app/environment';
	import { beforeNavigate } from '$app/navigation';
	import { page } from '$app/state';
	import Authentication from '$lib/common/Authentication.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import PagesRail from '$lib/dashboard/PagesRail.svelte';
	import Project from '$lib/dashboard/Project.svelte';
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

	$effect(() => {
		const extras = [];
		const project = page.params?.project;
		if (project) {
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
			const project = page.params?.project;
			if (project) {
				TestPlatform(project).stop();
			}
		});
	}
</script>

{@render children?.()}

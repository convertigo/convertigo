<script>
	import { beforeNavigate } from '$app/navigation';
	import { page } from '$app/stores';
	import PagesRail from '$lib/dashboard/PagesRail.svelte';
	import Project from '$lib/dashboard/Project.svelte';

	let { children } = $props();
	$effect(() => {
		Project.page = $page;
	});

	$effect(() => {
		const extras = [];
		const project = $page.params?.project;
		if (project) {
			if (Project.hasRef) {
				extras.push({
					title: 'References',
					icon: 'ph:plugs-connected-thin',
					page: '/(app)/dashboard/[[project]]',
					params: { project }
				});
			}
			extras.push({
				title: 'Backend',
				icon: 'ph:gear-six-thin',
				page: '/(app)/dashboard/[[project]]/backend',
				id: '/(app)/dashboard/[[project]]/backend/[sequence]',
				params: { project }
			});
			if (Project.hasFrontend) {
				extras.push({
					title: 'Frontend',
					icon: 'ph:video-thin',
					page: '/(app)/dashboard/[[project]]/frontend',
					params: { project }
				});
			}
			if (Project.hasPlatforms) {
				extras.push({
					title: 'Platforms',
					icon: 'ph:package-thin',
					page: '/(app)/dashboard/[[project]]/platforms',
					params: { project }
				});
			}
		}
		PagesRail.extras = extras;
	});
	beforeNavigate(({ to }) => {
		if (!to?.params?.project) {
			PagesRail.extras = [];
		}
	});
</script>

{@render children?.()}

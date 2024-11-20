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
		const name = $page.params?.project;
		if (name) {
			if (Project.hasRef) {
				extras.push({
					title: 'References',
					icon: 'ph:plugs-connected-thin',
					url: `${name}/`
				});
			}
			extras.push({
				title: 'Backend',
				icon: 'ph:gear-six-thin',
				url: `${name}/backend/`
			});
			if (Project.hasFrontend) {
				extras.push({
					title: 'Frontend',
					icon: 'ph:video-thin',
					url: `${name}/frontend/`
				});
			}
			if (Project.hasPlatforms) {
				extras.push({
					title: 'Platforms',
					icon: 'ph:package-thin',
					url: `${name}/platforms/`
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

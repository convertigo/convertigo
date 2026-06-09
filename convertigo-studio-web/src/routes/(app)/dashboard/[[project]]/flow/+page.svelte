<script>
	import Card from '$lib/admin/components/Card.svelte';
	import TestPlatform from '$lib/common/TestPlatform.svelte';
	import FlowViewer from '$lib/dashboard/flow/FlowViewer.svelte';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';

	let { params } = $props();
	let projectName = $derived(params.project ?? '');
	let project = $derived.by(() => (projectName ? TestPlatform(projectName) : null));
	let sequences = $derived(project?.sequence?.filter((sequence) => sequence.name) ?? []);
</script>

<Card title={project?.name ?? null} cornerOptionClass="flex-1 min-w-[18rem] max-w-[72rem]">
	<AutoPlaceholder loading={!project}>
		<FlowViewer {projectName} {sequences} />
	</AutoPlaceholder>
</Card>

<script>
	import Card from '$lib/admin/components/Card.svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { onMount } from 'svelte';
	import {
		statusCheck,
		product,
		javaVersion,
		javaClassVersion,
		beans,
		licenceType,
		licenceNumber,
		licenceEnd
	} from '$lib/admin/stores/statusStore';
	import { projectsStore, projectsCheck } from '$lib/admin/stores/projectsStore';

	let data;

	onMount(() => {
		statusCheck();
		projectsCheck();
	});

	$: if ($product && $javaVersion && $javaClassVersion && $beans) {
		data = [
			{
				'Convertigo version': $product,
				'Java version': $javaVersion,
				'Classes version': $javaClassVersion,
				'License Type': $licenceType,
				'License Number': $licenceNumber,
				'License End': $licenceEnd
			}
		];
	}
</script>

<Card title="Test Platform"></Card>

<Card title="Projects" class="mt-5">
	<TableAutoCard
		definition={[
			{ name: 'Project name', key: '@_name' },
			{ name: 'Comment', key: '@_comment' },
			{ name: 'Deployment Date', key: '@_deployDate' },
			{ name: 'Test Platform', custom: true },
			{ name: 'Web-service definition', custom: true }
		]}
		data={$projectsStore}
		let:row
		let:def
	>
		{#if def.name === 'Test Platform'}{:else if def.name === 'Web-service definition'}{/if}
	</TableAutoCard>
</Card>

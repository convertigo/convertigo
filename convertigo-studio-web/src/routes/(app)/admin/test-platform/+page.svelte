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
	import Icon from '@iconify/svelte';

	let data;

	onMount(() => {
		statusCheck();
		projectsCheck();
	});

	$: data = [
		{
			'Convertigo version': $product,
			'Java version': $javaVersion,
			'Classes version': $javaClassVersion,
			'License Type': $licenceType,
			'License Number': $licenceNumber,
			'License End': $licenceEnd
		}
	];
</script>

<Card title="Test Platform">
	<TableAutoCard
		definition={[
			{ name: 'Convertigo version', key: 'Convertigo version' },
			{ name: 'Java version', key: 'Java version' },
			{ name: 'Classes version', key: 'Classes version' },
			{ name: 'License Type', key: 'License Type' },
			{ name: 'License Number', key: 'License Number' },
			{ name: 'License End', key: 'License End' }
		]}
		{data}
	/>
</Card>

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
		{#if def.name === 'Test Platform'}
			<button class="shadow-md">
				<Icon icon="ic:round-play-arrow" class="w-7 h-7" style="color: #35b13d" />
			</button>
		{:else if def.name === 'Web-service definition'}
			<button class="shadow-md">
				<Icon icon="ic:round-play-arrow" class="w-7 h-7" style="color: #f14b04" />
			</button>
		{/if}
	</TableAutoCard>
</Card>

<script>
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AutoPlaceholder from '$lib/utils/AutoPlaceholder.svelte';
	import { resolve } from '$lib/utils/route';
	import { onMount } from 'svelte';
	import Last from './Last.svelte';

	onMount(() => {
		goto(
			resolve('/(app)/dashboard/[[project]]/frontend/[model]', {
				...page.params,
				model: `${Last.model}_${Last.orientation}`
			}),
			{ replaceState: true }
		);
	});
</script>

<div class="layout-y-center gap-4 py-10">
	<AutoPlaceholder class="w-72" />
	<AutoPlaceholder class="h-4 w-48" />
</div>

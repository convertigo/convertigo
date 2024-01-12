<script>
	import { AppBar } from '@skeletonlabs/skeleton';
	import { onMount } from 'svelte';
	import Drawer from '../admin-components/Drawer.svelte';
	import Icon from '@iconify/svelte';
	import { goto } from '$app/navigation';

	onMount(() => {
	});

	function navigateToStudio() {
		goto('/ui/dev')
	}
</script>

<AppBar background="bg-surface-800" class="flex w-full justify-between border-b-[0.5px] border-surface-500" padding='p-0'>
	<svelte:fragment slot="lead">
		<Drawer />
	</svelte:fragment>

	
	<svelte:fragment slot="trail">
		<div class="pr-4 p-1 flex items-center">
			<button type="button" class="btn  mr-10 p-1 pl-5 pr-5 bg-surface-500" on:click={navigateToStudio}>
				<span><Icon icon="material-symbols-light:design-services-outline-sharp" /></span>
				<span class="text-[13px] font-light">Go to studio</span>
			</button>
			<Icon icon="ph:gear-six-thin" class="w-6 h-6" />
		</div>
		
	</svelte:fragment>

</AppBar>

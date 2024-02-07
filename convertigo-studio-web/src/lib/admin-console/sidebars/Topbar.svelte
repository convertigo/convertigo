<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';
	import { goto } from '$app/navigation';
	import PagesRailToggle from '../admin-components/PagesRailToggle.svelte';
</script>

<AppBar
	background="bg-surface-800"
	class="flex w-full justify-between border-b-[0.5px] border-surface-500"
	padding="p-0"
>
	<svelte:fragment slot="lead">
		<PagesRailToggle />
	</svelte:fragment>

	<svelte:fragment slot="trail">
		<div class="pr-4 p-1 flex items-center space-x-5">
			<button
				type="button"
				class="btn p-1 pl-5 pr-5 bg-surface-500"
				on:click={() => goto('/ui/dev')}
			>
				<span><Icon icon="material-symbols-light:design-services-outline-sharp" /></span>
				<span class="text-[13px] font-light">Go to studio</span>
			</button>
			<LightSwitch />
			<Icon icon="ph:gear-six-thin" class="w-6 h-6" />
		</div>
	</svelte:fragment>
</AppBar>

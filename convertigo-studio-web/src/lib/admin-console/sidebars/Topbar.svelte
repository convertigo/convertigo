<script>
	import { AppBar, LightSwitch } from '@skeletonlabs/skeleton';
	import Icon from '@iconify/svelte';
	import { goto } from '$app/navigation';
	import PagesRailToggle from '../admin-components/PagesRailToggle.svelte';
</script>

<AppBar
	background="dark:bg-surface-800 bg-white"
	class="flex w-full justify-between border-b-[0.5px] dark:border-surface-500 border-surface-50"
	padding="p-3"
>
	<svelte:fragment slot="lead">
		<PagesRailToggle />
		<img src="/logo.png" alt="logo convertigo" class="w-10 h-full mr-5 ml-3" />
		<h1 class="font-normal dark:text-surface-200 text-surface-800 hidden md:block">
			Convertigo Admin Console
		</h1>
	</svelte:fragment>

	<svelte:fragment slot="trail">
		<a
			href="https://github.com/convertigo/convertigo"
			target="_blank"
			class="flex items-center btn font-normal"
		>
			<p class="">Star us on</p>
			<Icon icon="logos:github" />
		</a>

		<div class="pr-4 p-1 flex items-center space-x-5">
			<button
				type="button"
				class="btn p-1 pl-5 pr-5 text-white bg-buttons font-normal rounded-full font-medium"
				on:click={() => goto('/ui/dev')}
			>
				<Icon icon="arcticons:gearup-booster" class="w-7 h-7" />
				<span class="text-[13px] font-medium">Go to studio</span>
			</button>
			<LightSwitch />
			<Icon icon="ph:gear-six-thin" class="w-6 h-6" />
		</div>
	</svelte:fragment>
</AppBar>

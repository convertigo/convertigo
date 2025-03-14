<script>
	// @ts-ignore
	import Icon from '@iconify/svelte';
	import { AppBar, AppShell, LightSwitch } from '@skeletonlabs/skeleton';
	import { base } from '$app/paths';
	import { loading } from '$lib/utils/loadingStore';
</script>

<AppShell>
	<svelte:fragment slot="header">
		<AppBar
			gridColumns="grid-cols-3"
			slotDefault="place-self-center"
			slotTrail="place-content-end"
			padding="p-1"
			background="dark:bg-surface-900 bg-surface-200"
			class="border-1 border-b border-surface-300 dark:border-surface-500"
		>
			<svelte:fragment slot="lead"
				><div class={$loading > 0 ? 'rotate' : ''} style="margin-left:-4px">
					<Icon icon="material-symbols-light:cloud-outline" class="h-8 w-8 " />
				</div></svelte:fragment
			>

			<svelte:fragment slot="trail">
				<div class=" mr-7 preset-filled bg-surface-600 pr-3 pl-3">
					<a href="{base}/admin/" class="flex items-center"
						><p class="mr-2 text-[12px] font-light">Admin-consoleugugu</p>
						<Icon icon="arcticons:google-admin" class="h-8 w-8 " /></a
					>
				</div>
				<LightSwitch /></svelte:fragment
			>
		</AppBar>
	</svelte:fragment>
</AppShell>

<script>
	import { AppBar, AppShell, LightSwitch } from '@skeletonlabs/skeleton';
	import { loading } from '$lib/utils/loadingStore';
	// @ts-ignore
	import Icon from '@iconify/svelte';
	import { base } from '$app/paths';
</script>

<AppShell>
	<svelte:fragment slot="header">
		<AppBar
			gridColumns="grid-cols-3"
			slotDefault="place-self-center"
			slotTrail="place-content-end"
			padding="p-1"
			background="dark:bg-surface-900 bg-surface-200"
			class="border-1 border-b dark:border-surface-500 border-surface-300"
		>
			<svelte:fragment slot="lead"
				><div class={$loading > 0 ? 'rotate' : ''} style="margin-left:-4px">
					<Icon icon="material-symbols-light:cloud-outline" class="w-8 h-8 " />
				</div></svelte:fragment
			>

			<svelte:fragment slot="trail">
				<div class=" bg-surface-600 mr-7 pr-3 pl-3 variant-filled">
					<a href="{base}/admin/" class="flex items-center"
						><p class="mr-2 text-[12px] font-light">Admin-console</p>
						<Icon icon="arcticons:google-admin" class="w-8 h-8 " /></a
					>
				</div>
				<LightSwitch /></svelte:fragment
			>
		</AppBar>
	</svelte:fragment>
</AppShell>

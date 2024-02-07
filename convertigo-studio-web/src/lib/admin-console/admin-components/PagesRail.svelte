<script>
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailAnchor } from '@skeletonlabs/skeleton';
	import parts from './PagesRail.json';
	import { page } from '$app/stores';

	$: isRoot = $page.route.id == '/admin-console';
</script>

<AppRail
	width="w-auto"
	height=""
	background="bg-surface-800"
	class="border-r-[0.5px] border-surface-500"
	active="bg-surface-900"
	hover="hover:bg-surface-900"
>
	<div class="grid grid-cols-2">
		{#each parts as tiles, i}
			{#each tiles as tile}
				{@const url = tile.url.length ? `${tile.url}/` : ''}
				<AppRailAnchor
					href={`${isRoot ? '' : '../'}${url}`}
					selected={url == '' ? isRoot : $page.url.pathname.endsWith(url)}
					name={tile.title.toLowerCase()}
					title={tile.title}
					class="border border-surface-900"
				>
					<svelte:fragment slot="lead">
						<Icon icon={tile.icon} width={30} height={30} />
					</svelte:fragment>
					<span class="font-extralight text-[10px]">{tile.title}</span>
				</AppRailAnchor>
			{/each}
			{#if i < parts.length - 1}
				<div class="col-span-2 flex justify-center my-5">
					<separator class="border-[1px] flex w-[70%] border-surface-100" />
				</div>
			{/if}
		{/each}
	</div>
</AppRail>

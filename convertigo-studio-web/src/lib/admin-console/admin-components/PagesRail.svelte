<script>
	import Icon from '@iconify/svelte';
	import { AppRail, AppRailAnchor } from '@skeletonlabs/skeleton';
	import parts from './PagesRail.json';
	import { page } from '$app/stores';

	function isSelected(url) {
		if (isRoot) {
			return url === '';
		} else {
			const basePath = $page.url.pathname.split('/')[1];
			console.log(basePath);
			return url.includes(basePath);
		}
	}

	$: isRoot = $page.route.id == '/admin-console';
</script>

<AppRail
	width="w-auto"
	height="h-[100%]"
	background="dark:bg-surface-800 bg-white"
	class="border-r-[0.5px] dark:border-surface-500 border-surface-50 p-4"
	active="dark:bg-surface-900"
	hover="hover:bg-surface-900"
>
	<!-- <div class="grid grid-cols-2">
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
					<span class="font-extralight font-normal">{tile.title}</span>
				</AppRailAnchor>
			{/each}
			{#if i < parts.length - 1}
				<div class="col-span-2 flex justify-center my-5">
					<separator class="border-[1px] flex w-[70%] border-surface-100" />
				</div>
			{/if}
		{/each}
	</div>-->

	{#each parts as tiles, i}
		{#each tiles as tile}
			{@const url = tile.url.length ? `${tile.url}/` : ''}
			<a
				href={`${isRoot ? '' : '../'}${url}`}
				class="flex p-2 items-center dark:hover:bg-surface-500 hover:bg-surface-50 rounded-xl {isSelected(
					url
				)
					? 'bg'
					: ''}"
			>
				<Icon icon={tile.icon} width={25} height={25} />
				<span
					class="font-extralight font-normal ml-3 text-[14px] dark:text-surface-200 text-surface-800"
					>{tile.title}</span
				>
			</a>
		{/each}
		{#if i < parts.length - 1}
			<div class="col-span-2 flex justify-center my-5">
				<separator class="border-[1px] flex w-[70%] border-surface-100" />
			</div>
		{/if}
	{/each}
</AppRail>

<style lang="postcss">
	.bg {
		@apply dark:bg-surface-500;
	}
</style>
